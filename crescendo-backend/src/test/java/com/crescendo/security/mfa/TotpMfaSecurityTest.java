package com.crescendo.security.mfa;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class TotpMfaSecurityTest {

    @Test
    void generateSecret_returns32CharacterBase32String() {
        String secret = TOTPUtil.generateSecret();

        assertThat(secret).isNotNull();
        assertThat(secret).hasSize(32);
        assertThat(secret).matches("^[ABCDEFGHIJKLMNOPQRSTUVWXYZ234567]+$");
    }

    @Test
    void generateAndVerifyCurrentCode_validatesSuccessfullyWithinTimeWindow() {
        String secret = TOTPUtil.generateSecret();
        Instant now = Instant.now();

        int currentCode = TOTPUtil.generateCurrentCode(secret, now);
        boolean isValid = TOTPUtil.verifyCode(secret, currentCode, 1);

        assertThat(isValid).isTrue();
    }

    @Test
    void verifyCode_invalidOrIncorrectCode_returnsFalse() {
        String secret = TOTPUtil.generateSecret();
        
        // Use an obviously invalid code
        boolean isValid = TOTPUtil.verifyCode(secret, 9999999, 1);

        assertThat(isValid).isFalse();
    }

    @Test
    void verifyCode_expiredTimeStepOutsideWindowSkew_failsVerification() {
        String secret = TOTPUtil.generateSecret();
        // Go 90 seconds in the past (3 time steps ago)
        Instant past = Instant.now().minusSeconds(90);

        int oldCode = TOTPUtil.generateCurrentCode(secret, past);
        // With windowSkew = 1 (±30 seconds), an old code from 90s ago must fail
        boolean isValid = TOTPUtil.verifyCode(secret, oldCode, 1);

        assertThat(isValid).isFalse();
    }

    @Test
    void verifyCode_acceptsCodesWithinOneWindowSkew() {
        String secret = TOTPUtil.generateSecret();
        // Go 25 seconds in the past (still within current or adjacent window)
        Instant slightlyPast = Instant.now().minusSeconds(25);

        int code = TOTPUtil.generateCurrentCode(secret, slightlyPast);
        boolean isValid = TOTPUtil.verifyCode(secret, code, 1);

        assertThat(isValid).isTrue();
    }
}
