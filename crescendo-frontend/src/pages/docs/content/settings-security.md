# Settings, Security & Workspace Administration

The Settings & Security console empowers workspace owners and system administrators to manage personal user profiles, configure robust passwordless authentication credentials, generate programmatic API keys, and organize team collaboration access.

## WebAuthn & Passkey Biometric Authentication

Crescendo integrates natively with contemporary FIDO2 and WebAuthn security specifications, enabling passwordless authentication across mobile devices and secure modern desktop web browsers.

### Setting Up Passwordless Passkeys
1. Navigate to **Settings > Security & Authentication** within your user dashboard menu.
2. Under the **Passkeys (WebAuthn)** interface, click **Register New Passkey**.
3. Your native operating system security prompt will activate:
   * **Windows/macOS:** Scan your fingerprint (Touch ID / Windows Hello) or authenticate via facial recognition camera sensor.
   * **Hardware Tokens:** Insert your USB security key (e.g., YubiKey) and touch the physical validation sensor.
   * **Mobile Authentication:** Scan the generated QR code utilizing an authenticated iOS or Android cellular device.
4. Assign an identifiable alias name to the credential (e.g., `MacBook Pro TouchID` or `YubiKey 5C NFC`).

### Passwordless Login Benefits
Once registered, you can log directly into your Crescendo workspace by selecting **Sign in with Passkey** on the primary login screen. Passkeys eliminate vulnerability to credential stuffing attacks and completely bypass risks associated with traditional phishing interception.

## Developer API Key Administration

For automated systems communicating directly with Crescendo backend microservices over REST HTTPS endpoints, administrators can generate scoped programmatic authentication credentials under **Settings > Developer API**.

### Creating Scoped API Keys
When generating a new programmatic secret key, select explicit role-based access scopes to adhere to zero-trust principle security architecture:
* `workflows:read` - Allows read-only visibility into saved graph configurations and node structures.
* `workflows:write` - Permits programmatic creation, editing, and architectural modification of workflow steps.
* `runs:read` - Grants access to historical workflow execution logs and diagnostic run telemetry.
* `runs:execute` - Enables remote applications to trigger workflow execution routines via API endpoint invocation.

> [!CAUTION]
> Treat API keys with maximum confidentiality. Never embed API secrets directly inside frontend browser JavaScript code or public GitHub repositories. Always proxy automated integration requests through authenticated backend corporate architectures.

## Workspace Preferences & Team Access

Workspace administrative configuration controls ensure standardized collaboration across engineering and marketing personnel:
* **Timezone Configuration:** Specify your local enterprise operational timezone (e.g., `Asia/Kolkata` or `America/New_York`). All scheduled campaign dispatches and timestamped execution logs align automatically with your selected regional standard time.
* **Default Notification Routing:** Configure primary administrator email addresses for automated service incident warnings, usage quota exhaustion alerts, and security audit log deliveries.
