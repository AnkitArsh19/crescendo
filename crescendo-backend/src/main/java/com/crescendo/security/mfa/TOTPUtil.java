package com.crescendo.security.mfa;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.time.Instant;

/**
 * Minimal TOTP (RFC 6238) utility (SHA1 / 30s time-step / 6 digits) for MFA.
 * We intentionally keep deps minimal; secret is generated as 20 random bytes then Base32 encoded.
 */
public final class TOTPUtil {

    private static final SecureRandom RNG = new SecureRandom();
    /// RFC 4648 Base32 alphabet — 26 uppercase letters + digits 2–7 (avoids 0 and 1 which look like O and I).
    private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    private TOTPUtil() {}

    /// Generates a new TOTP secret: 20 random bytes (160 bits) encoded as Base32.
    /// 20 bytes matches the SHA-1 output size and is the standard Google Authenticator key length.
    /// Base32 is used because it is case-insensitive and alphanumeric — safe for display and manual entry.
    public static String generateSecret() {
        byte[] buf = new byte[20];
        RNG.nextBytes(buf);
        return base32Encode(buf);
    }

    /// Generates the TOTP code for a specific instant in time.
    /// Primarily used in tests to generate codes at a known timestamp for deterministic assertions.
    /// In production, use verifyCode() which checks the current time automatically.
    public static int generateCurrentCode(String base32Secret, Instant instant) {
        long timeWindow = instant.getEpochSecond() / 30L; // 30s steps
        return generateCode(base32Secret, timeWindow);
    }

    /// Verifies a TOTP code allowing for ±windowSkew time-step tolerance.
    /// windowSkew=1 means we check the previous, current, and next 30-second window.
    /// This handles clock drift between the user's authenticator app and the server (up to ±30s).
    public static boolean verifyCode(String base32Secret, int code, int windowSkew) {
        long current = Instant.now().getEpochSecond() / 30L;
        for (int i = -windowSkew; i <= windowSkew; i++) {
            if (generateCode(base32Secret, current + i) == code) return true;
        }
        return false;
    }

    /// Core RFC 6238 / HOTP (RFC 4226) algorithm:
    /// 1. Encode the time window counter as a big-endian 8-byte array.
    /// 2. HMAC-SHA1 sign it with the user's secret key.
    /// 3. Dynamic truncation: use the last nibble of the HMAC as an offset.
    /// 4. Extract 4 bytes from that offset, mask the sign bit, then mod 1,000,000 → 6-digit code.
    private static int generateCode(String base32Secret, long timeWindow) {
        try {
            byte[] key = base32Decode(base32Secret);
            byte[] msg = new byte[8];
            for (int i = 7; i >= 0; i--) {
                msg[i] = (byte) (timeWindow & 0xFF);
                timeWindow >>= 8;
            }
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hmac = mac.doFinal(msg);
            int offset = hmac[hmac.length - 1] & 0x0F;  // dynamic truncation offset
            int binary = ((hmac[offset] & 0x7F) << 24) |  // 0x7F masks the sign bit
                    ((hmac[offset + 1] & 0xFF) << 16) |
                    ((hmac[offset + 2] & 0xFF) << 8) |
                    (hmac[offset + 3] & 0xFF);
            int otp = binary % 1_000_000;  // truncate to 6 digits
            return otp;
        } catch (Exception e) {
            throw new IllegalStateException("TOTP generation failed", e);
        }
    }

    /// Encodes raw bytes into a Base32 string (no padding).
    /// Works as a bit-stream: accumulates 8 bits from each byte into a buffer,
    /// then drains 5 bits at a time to produce each Base32 character.
    /// (8 bits per byte → 5 bits per Base32 char → ceil(20*8/5) = 32 chars for a 20-byte secret)
    private static String base32Encode(byte[] data) {
        StringBuilder sb = new StringBuilder((data.length * 8 + 4) / 5);
        int buffer = 0;
        int bitsLeft = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF); // load next 8 bits into buffer
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                sb.append(BASE32_ALPHABET.charAt((buffer >> (bitsLeft - 5)) & 0x1F)); // take top 5 bits
                bitsLeft -= 5;
            }
        }
        if (bitsLeft > 0) {
            sb.append(BASE32_ALPHABET.charAt((buffer << (5 - bitsLeft)) & 0x1F)); // flush remaining bits
        }
        return sb.toString();
    }

    /// Decodes a Base32 string back into raw bytes — the inverse of base32Encode.
    /// Non-alphabet characters (spaces, dashes, padding '=') are silently skipped,
    /// which makes it tolerant of user-formatted secrets like "JBSW Y3DP" vs "JBSWY3DP".
    /// Accumulates 5-bit Base32 values into a buffer, then drains 8 bits at a time as bytes.
    private static byte[] base32Decode(String s) {
        int buffer = 0;
        int bitsLeft = 0;
        ByteArrayOutputStreamEx out = new ByteArrayOutputStreamEx();
        for (char c : s.toCharArray()) {
            int val = BASE32_ALPHABET.indexOf(Character.toUpperCase(c));
            if (val < 0) continue; // skip padding, spaces, dashes
            buffer = (buffer << 5) | val; // load next 5 bits into buffer
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                out.write((buffer >> (bitsLeft - 8)) & 0xFF); // drain 8 bits as a byte
                bitsLeft -= 8;
            }
        }
        return out.toByteArray();
    }

    /// Minimal resizable byte buffer used in base32Decode to avoid a dependency on
    /// java.io.ByteArrayOutputStream (which throws checked IOException on write, requiring
    /// extra try-catch noise in the decode loop). Doubles capacity when full.
    private static class ByteArrayOutputStreamEx {
        private byte[] buf = new byte[32];
        private int count = 0;
        void write(int b) {
            if (count == buf.length) { // grow buffer when full
                byte[] bigger = new byte[buf.length * 2];
                System.arraycopy(buf, 0, bigger, 0, buf.length);
                buf = bigger;
            }
            buf[count++] = (byte) b;
        }
        byte[] toByteArray() {
            byte[] out = new byte[count];
            System.arraycopy(buf, 0, out, 0, count);
            return out;
        }
    }
}
