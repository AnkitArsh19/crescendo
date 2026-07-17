package com.crescendo.security.mfa;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.List;

/**
 * All request and response DTOs for the MFA endpoints, grouped in one file.
 * Using nested records keeps MFA-related API shapes co-located and avoids polluting the package
 * with many small single-purpose files.
 */
public class MFADto {
    /// Returned from POST /mfa/enroll/start.
    /// secret      — the raw Base32 TOTP secret (show to user as manual entry fallback)
    /// otpAuthUrl  — the full otpauth:// URI (scanned by the authenticator app)
    /// qrImageDataUri — base64 PNG data URI of the QR code, ready for <img src="..."> in the frontend
    public record MfaEnrollStartResponse(
            String secret,
            String otpAuthUrl,
            String qrImageDataUri
    ){}

    /// Sent to POST /mfa/enroll/confirm.
    /// The user enters the 6-digit code shown by their authenticator app to prove setup succeeded.
    public record MfaEnrollConfirmRequest(
            @NotNull Integer code
    ) {}

    /// Sent to PATCH /mfa/toggle to enable or disable MFA without re-enrolling.
    /// MFA must have been verified at least once before it can be enabled.
    public record MfaToggleRequest(
            @NotNull Boolean enabled
    ) {}

    /// Sent to POST /mfa/challenge after a successful password login that detected MFA is enabled.
    /// email — identifies the user (same email submitted at /auth/login)
    /// code  — the 6-digit TOTP code from the authenticator app (validated as exactly 6 digits)
    public record MfaLoginChallengeRequest(
            @NotBlank String email,
            @Pattern(regexp="^[0-9]{6}$") String code,
            String deviceId,
            String deviceLabel
    ) {}

    /// Returned from POST /mfa/challenge.
    /// On success: success=true and both tokens are populated (refresh also set as HttpOnly cookie).
    /// On failure: success=false, tokens are null — client should display an error.
    public record MfaLoginChallengeResponse(
            boolean success,
            String accessToken,
            String refreshToken
    ) {}

    /// Returned from POST /mfa/backup-codes/regenerate.
    /// backupCodesPlain — the 10 plain-text backup codes, shown ONCE to the user.
    /// After this response is received, the codes cannot be retrieved again — only hashes are stored.
    /// The frontend should instruct the user to save or print these immediately.
    public record MfaBackupCodesResponse(
            List<String> backupCodesPlain
    ) {}

    /// Sent to POST /mfa/backup-code when the user cannot use their authenticator app.
    /// backupCode — the plain XXXX-XXXX-XXXX-XXXX code from the user's saved list.
    public record MfaUseBackupCodeRequest(
            @NotBlank String email,
            @NotBlank String backupCode,
            String deviceId,
            String deviceLabel
    ) {}

    /// Returned from POST /mfa/backup-code.
    /// remainingBackupCodes — how many unused codes are left, so the UI can warn
    /// the user if they are running low and should regenerate their backup codes.
    public record MfaUseBackupCodeResponse(
            boolean success,
            String accessToken,
            String refreshToken,
            int remainingBackupCodes
    ) {}
}
