import {
  browserSupportsWebAuthn,
  browserSupportsWebAuthnAutofill,
  startAuthentication,
  startRegistration,
  WebAuthnAbortService,
} from '@simplewebauthn/browser';
import api from './axios';
import useAuthStore from '../store/authStore';
import { getDeviceMetadata } from '../utils/deviceFingerprint';

const unavailableMessage = 'Passkeys are not supported by this browser or device.';

function readableError(error, fallback) {
  if (error?.name === 'NotAllowedError') return 'Passkey request was cancelled or timed out.';
  if (error?.name === 'InvalidStateError') return 'This passkey is already registered on this device.';
  return error?.response?.data?.message || error?.message || fallback;
}

export function passkeysSupported() {
  return browserSupportsWebAuthn();
}

function isCancellation(error) {
  return error?.name === 'NotAllowedError' || error?.name === 'AbortError';
}

async function finishAuthentication(options, authentication) {
  const { deviceId, deviceLabel } = getDeviceMetadata();
  const { data: tokens } = await api.post('/auth/webauthn/login/finish', {
    ...authentication,
    transactionId: options.transactionId,
  }, {
    headers: {
      'X-Device-Id': deviceId,
      'X-Device-Label': deviceLabel,
    }
  });

  useAuthStore.getState().setTokens(
    tokens.accessToken,
    tokens.accessExpiresAt,
    tokens.refreshToken,
    tokens.refreshExpiresAt,
  );
  await useAuthStore.getState().checkAuth();
}

export function cancelPasskeyRequest() {
  WebAuthnAbortService.cancelCeremony();
}

// Conditional UI is intentionally silent until a user focuses an input with
// autocomplete="username webauthn". It lets supported browsers place a
// passkey beside saved passwords in their familiar autofill sheet.
export async function beginPasskeyAutofill() {
  if (!passkeysSupported() || !(await browserSupportsWebAuthnAutofill())) return false;

  try {
    const { data: options } = await api.post('/auth/webauthn/login/start', {});
    const authentication = await startAuthentication({
      optionsJSON: options,
      useBrowserAutofill: true,
    });
    await finishAuthentication(options, authentication);
    return true;
  } catch (error) {
    // Starting the explicit passkey flow or leaving this page cancels the
    // pending conditional request. That is expected, not an error for users.
    if (isCancellation(error)) return false;
    throw new Error(readableError(error, 'Could not sign in with this passkey.'));
  }
}

export async function registerPasskey(credentialName = 'Passkey', elevatedToken) {
  if (!passkeysSupported()) throw new Error(unavailableMessage);
  // Cancel any in-progress ceremony (e.g. a previous attempt that was abandoned)
  // to avoid the "A request is already pending" browser error.
  WebAuthnAbortService.cancelCeremony();
  try {
    const { data: options } = await api.post(
      '/auth/webauthn/register/start',
      { credentialName },
      { headers: { 'X-Elevated-Token': elevatedToken } },
    );
    const registration = await startRegistration({ optionsJSON: options });
    await api.post('/auth/webauthn/register/finish', {
      ...registration,
      transactionId: options.transactionId,
      credentialName,
    });
  } catch (error) {
    throw new Error(readableError(error, 'Could not add this passkey.'));
  }
}

export async function loginWithPasskey(email = '') {
  if (!passkeysSupported()) throw new Error(unavailableMessage);
  try {
    const { data: options } = await api.post('/auth/webauthn/login/start', { email: email.trim() || undefined });
    const authentication = await startAuthentication({ optionsJSON: options });
    await finishAuthentication(options, authentication);
  } catch (error) {
    throw new Error(readableError(error, 'Could not sign in with this passkey.'));
  }
}
