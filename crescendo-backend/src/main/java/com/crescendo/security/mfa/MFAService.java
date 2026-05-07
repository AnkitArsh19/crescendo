package com.crescendo.security.mfa;

import com.crescendo.security.AppUserDetails;
import com.crescendo.security.JWTService;
import com.crescendo.security.TokenPair;
import com.crescendo.shared.domain.event.DomainEventPublisher;
import com.crescendo.user.domain_event.MFADisabledEvent;
import com.crescendo.user.domain_event.MFAEnabledEvent;
import com.crescendo.user.user_command.User_command;
import com.crescendo.user.user_command.User_commandRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.common.BitMatrix;

import static com.crescendo.security.mfa.TOTPUtil.*;

/**
 * Orchestrates the full MFA lifecycle:
 *   - Enrollment  : generate secret → build QR code → confirm with first TOTP code → issue backup codes
 *   - Login        : verify TOTP code mid-login, then issue a full token pair
 *   - Backup codes : one-time fallback codes for when the user loses their authenticator device
 *   - Management   : toggle MFA on/off, regenerate backup codes
 */
@Service
public class MFAService {

    private final UserMFASettingRepository mfaRepo;
    private final User_commandRepository userRepo;
    private final JWTService jwtService;
    private final UserMFABackupCodeRepository backupRepo;
    private final DomainEventPublisher eventPublisher;
    private final SecureRandom secureRandom = new SecureRandom();

    public MFAService(UserMFASettingRepository mfaRepo, User_commandRepository userRepo, JWTService jwtService,
                      UserMFABackupCodeRepository backupRepo, DomainEventPublisher eventPublisher) {
        this.mfaRepo = mfaRepo;
        this.userRepo = userRepo;
        this.jwtService = jwtService;
        this.backupRepo = backupRepo;
        this.eventPublisher = eventPublisher;
    }

    /// Step 1 of MFA setup: generate a fresh TOTP secret, persist it (unverified/disabled),
    /// and return the secret + an otpauth:// URL + a QR code data URI for the authenticator app to scan.
    @Transactional
    public MFADto.MfaEnrollStartResponse startEnrollment(UUID userId, String issuer, String accountLabel) {
        User_command user = userRepo.findById(userId).orElseThrow();
        String secret = generateSecret();
        UserMFASetting setting = mfaRepo.findByUser_Id(userId)
                .orElse(new UserMFASetting(UUID.randomUUID(), user, secret));
        setting.setSecret(secret);
        setting.setVerified(false);
        setting.setEnabled(false);
        mfaRepo.save(setting);
        String otpauth = buildOtpAuthUrl(issuer, accountLabel, secret);
        String qr = buildQrDataUri(otpauth);
        return new MFADto.MfaEnrollStartResponse(secret, otpauth, qr);
    }

    /// Step 2 of MFA setup: user scanned the QR and enters their first TOTP code to prove
    /// they set up their authenticator correctly. Only on success do we mark MFA as verified+enabled
    /// and generate the 10 one-time backup codes.
    /// Returns the 10 plain backup codes on success, or an empty Optional if the code is wrong.
    @Transactional
    public Optional<MFADto.MfaBackupCodesResponse> confirmEnrollment(UUID userId, int code) {
        UserMFASetting setting = mfaRepo.findByUser_Id(userId).orElseThrow();
        if (verifyCode(setting.getSecret(), code, 1)) {
            setting.setVerified(true);
            setting.setEnabled(true);
            eventPublisher.publish(new MFAEnabledEvent(userId));
            // Generate backup codes on first successful verification and return them immediately.
            // They are only available now — only hashes are kept in the DB after this point.
            List<String> plain = generateAndStoreBackupCodes(userId, setting.getUser());
            return Optional.of(new MFADto.MfaBackupCodesResponse(plain));
        }
        return Optional.empty();
    }

    /// Enables or disables MFA for the user.
    /// Guard: you cannot enable MFA if the secret was never verified — that would lock the user out
    /// because unverified means their authenticator app was never successfully configured.
    @Transactional
    public void toggle(UUID userId, boolean enabled) {
        UserMFASetting setting = mfaRepo.findByUser_Id(userId).orElseThrow();
        if (!setting.isVerified() && enabled) throw new IllegalStateException("MFA not verified");
        setting.setEnabled(enabled);
        eventPublisher.publish(enabled ? new MFAEnabledEvent(userId) : new MFADisabledEvent(userId));
    }

    /// Called during the MFA login challenge: the user already passed password auth and now
    /// provides their 6-digit TOTP code. Returns a full TokenPair on success, empty on failure.
    /// Returns empty (not an exception) so the auth controller can return a clean 401 without
    /// leaking whether the failure was a wrong code vs no MFA configured.
    public Optional<TokenPair> completeLoginIfValid(User_command user, int code, String userAgent) {
        Optional<UserMFASetting> settingOpt = mfaRepo.findByUser_Id(user.getId());
        if (settingOpt.isEmpty()) return Optional.empty();
        UserMFASetting setting = settingOpt.get();
        if (!setting.isEnabled() || !setting.isVerified()) return Optional.empty();
        if (verifyCode(setting.getSecret(), code, 1)) {
            AppUserDetails principal = AppUserDetails.from(user, Optional.empty());
            return Optional.of(jwtService.issueTokenPair(user, principal, userAgent));
        }
        return Optional.empty();
    }

    /// Deletes all existing backup codes for the user and generates a fresh set of 10.
    /// Used when the user suspects their backup codes were exposed or has used several.
    /// The plain codes are returned exactly once here — they are never retrievable again
    /// because only hashes are stored in the DB.
    @Transactional
    public MFADto.MfaBackupCodesResponse regenerateBackupCodes(UUID userId) {
        User_command user = userRepo.findById(userId).orElseThrow();
        backupRepo.deleteAllByUserId(userId);
        return new MFADto.MfaBackupCodesResponse(generateAndStoreBackupCodes(userId, user));
    }

    /// Quick check used by the auth controller during password login to decide whether
    /// to issue tokens immediately or respond with an MFA challenge instead.
    /// Returns false if no MFA setting exists at all (user never enrolled).
    public boolean hasEnabledMfa(UUID userId) {
        return mfaRepo.findByUser_Id(userId).map(UserMFASetting::isEnabled).orElse(false);
    }

    /// Returns the count of unused backup codes remaining for a user.
    /// Used by the /mfa/backup-code endpoint response to warn the UI when codes are running low.
    public int countRemainingBackupCodes(UUID userId) {
        return backupRepo.findAllByUser_IdAndUsedAtIsNull(userId).size();
    }

    /// Authenticates using a one-time backup code instead of a TOTP code.
    /// The presented raw code is hashed and looked up against unused (usedAt IS NULL) codes.
    /// On match: mark the code as consumed (sets usedAt) and issue a full token pair.
    /// The code becomes permanently unusable after this call — even if the user logs out and tries again.
    @Transactional
    public Optional<TokenPair> useBackupCode(User_command user, String rawCode, String userAgent) {
        String hash = hashBackupCode(rawCode);
        Optional<UserMFABackupCode> opt = backupRepo.findByUser_IdAndCodeHashAndUsedAtIsNull(user.getId(), hash);
        if (opt.isEmpty()) return Optional.empty();
        UserMFABackupCode code = opt.get();
        code.setUsedAt(Instant.now()); // marks code as consumed — single-use enforced
        AppUserDetails principal = AppUserDetails.from(user, Optional.empty());
        return Optional.of(jwtService.issueTokenPair(user, principal, userAgent));
    }

    /// Builds the standard otpauth:// URI consumed by authenticator apps (Google Authenticator, Authy etc.).
    /// Format: otpauth://totp/<issuer>:<account>?secret=<BASE32>&issuer=<issuer>&digits=6&period=30
    /// The issuer appears as the app name in the authenticator, accountLabel as the account identifier.
    private String buildOtpAuthUrl(String issuer, String accountLabel, String secret) {
        return "otpauth://totp/" + url(issuer) + ":" + url(accountLabel) + "?secret=" + secret + "&issuer=" + url(issuer) + "&digits=6&period=30";
    }

    /// Encodes the otpauth:// URL into a 240×240 QR code PNG, then returns it as a
    /// base64 data URI ("data:image/png;base64,...") so the frontend can render it
    /// directly in an <img> tag without a separate image endpoint.
    /// Uses the ZXing (Zebra Crossing) library for QR code generation.
    private String buildQrDataUri(String otpauth) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(otpauth, BarcodeFormat.QR_CODE, 240, 240);
            ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", outputStream);
            String b64 = Base64.getEncoder().encodeToString(outputStream.toByteArray());
            return "data:image/png;base64," + b64;
        } catch (WriterException | java.io.IOException e) {
            throw new IllegalStateException("QR generation failed", e);
        }
    }

    /// URL-encodes a string for safe inclusion in the otpauth:// URI.
    /// Without this, spaces or special characters in the issuer name would break the URI.
    private String url(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    /// Generates 10 backup codes, stores only their SHA-256 hashes in the DB,
    /// and returns the plain codes to the caller (who passes them back to the user, once only).
    /// After this method returns, the plain codes are gone — not recoverable from the DB.
    private List<String> generateAndStoreBackupCodes(UUID userId, User_command user) {
        List<String> plain = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            String code = randomBackupCode();
            plain.add(code);
            backupRepo.save(new UserMFABackupCode(UUID.randomUUID(), user, hashBackupCode(code)));
        }
        return plain;
    }

    /// Generates a human-readable backup code in XXXX-XXXX-XXXX-XXXX format.
    /// Ambiguous characters (0, O, 1, I, L) are excluded to prevent user confusion.
    private String randomBackupCode() {
        // pattern: 4 groups of 4 alphanumeric chars
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // avoid ambiguous
        StringBuilder sb = new StringBuilder();
        for (int g = 0; g < 4; g++) {
            if (g > 0) sb.append('-');
            for (int i = 0; i < 4; i++) {
                sb.append(chars.charAt(secureRandom.nextInt(chars.length())));
            }
        }
        return sb.toString();
    }

    /// SHA-256 hashes a raw backup code and returns it as a lowercase hex string.
    /// Hex (not Base64) is used here so the stored hash is exactly 64 printable characters
    /// with no padding characters, matching the column length constraint of 64.
    private String hashBackupCode(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot hash backup code", e);
        }
    }
}
