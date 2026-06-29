/**
 * Utility for generating and retrieving client-side device metadata.
 * 
 * The Device ID is a persistent UUID stored in localStorage to track a physical device
 * across sessions, even if the IP address changes.
 * 
 * The Device Label is a human-readable string (e.g. "Chrome on Windows") parsed from the User-Agent,
 * used in the user's active sessions dashboard.
 */

const DEVICE_ID_KEY = 'crescendo_device_id';

export const getDeviceId = () => {
  let deviceId = localStorage.getItem(DEVICE_ID_KEY);
  if (!deviceId) {
    deviceId = crypto.randomUUID();
    localStorage.setItem(DEVICE_ID_KEY, deviceId);
  }
  return deviceId;
};

export const getDeviceLabel = () => {
  const ua = navigator.userAgent;
  let browser = 'Unknown Browser';
  let os = 'Unknown OS';

  // Basic Browser Detection
  if (ua.includes('Firefox')) browser = 'Firefox';
  else if (ua.includes('SamsungBrowser')) browser = 'Samsung Internet';
  else if (ua.includes('Opera') || ua.includes('OPR')) browser = 'Opera';
  else if (ua.includes('Trident')) browser = 'Internet Explorer';
  else if (ua.includes('Edge') || ua.includes('Edg/')) browser = 'Edge';
  else if (ua.includes('Chrome')) browser = 'Chrome';
  else if (ua.includes('Safari')) browser = 'Safari';

  // Basic OS Detection
  if (ua.includes('Win')) os = 'Windows';
  else if (ua.includes('Mac')) os = 'macOS';
  else if (ua.includes('Linux')) os = 'Linux';
  else if (ua.includes('Android')) os = 'Android';
  else if (ua.includes('like Mac')) os = 'iOS';

  return `${browser} on ${os}`;
};

export const getDeviceMetadata = () => ({
  deviceId: getDeviceId(),
  deviceLabel: getDeviceLabel(),
});
