package com.crescendo.emailservice;

/**
 * Notification abstraction for password reset and email verification flows.
 */
public interface NotificationService {

    void sendPasswordResetToken(String email, String plainToken);

    void sendEmailVerificationToken(String email, String plainToken);

    void sendPasskeyRecoveryLink(String email, String recoveryToken);

    void sendPasswordlessSignupOtp(String email, String otp);

    /** Sent when an existing account's email is used in the passwordless sign-up flow.
     *  The HTTP response to the caller is identical to the success case to prevent enumeration;
     *  only the inbox content differs. */
    void sendAccountExistsEmail(String email);

    void sendWelcomeEmail(String email, String name);

    void sendDeleteAccountEmail(String email);



    void sendPasswordChangedEmail(String email);

    void sendTotpEnabledEmail(String email);

    void sendTotpDisabledEmail(String email);

    void sendSmartLoginAlertEmail(String email, String device, String location, String country, String revokeUrl);

    void sendSuspiciousActivityEmail(String email, String originalIp, String newIp, String revokeUrl);

    void sendPasskeyAddedEmail(String email, String passkeyName);
}
