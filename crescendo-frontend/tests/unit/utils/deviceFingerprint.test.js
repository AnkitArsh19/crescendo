import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { getDeviceId, getDeviceLabel, getDeviceMetadata } from '../../../src/utils/deviceFingerprint';

describe('deviceFingerprint', () => {
  beforeEach(() => {
    // Clear localStorage before each test
    localStorage.clear();
    // Reset navigator userAgent mock
    vi.stubGlobal('navigator', { userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36' });
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  describe('getDeviceId', () => {
    it('generates and stores a new device ID if none exists', () => {
      expect(localStorage.getItem('crescendo_device_id')).toBeNull();
      
      const id1 = getDeviceId();
      expect(id1).toBeDefined();
      expect(typeof id1).toBe('string');
      expect(localStorage.getItem('crescendo_device_id')).toBe(id1);
    });

    it('returns the existing device ID if it is already in localStorage', () => {
      localStorage.setItem('crescendo_device_id', 'test-uuid-1234');
      
      const id = getDeviceId();
      expect(id).toBe('test-uuid-1234');
    });
  });

  describe('getDeviceLabel', () => {
    it('detects Chrome on Windows correctly', () => {
      vi.stubGlobal('navigator', { userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36' });
      expect(getDeviceLabel()).toBe('Chrome on Windows');
    });

    it('detects Safari on macOS correctly', () => {
      vi.stubGlobal('navigator', { userAgent: 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2.1 Safari/605.1.15' });
      expect(getDeviceLabel()).toBe('Safari on macOS');
    });

    it('detects Firefox on Linux correctly', () => {
      vi.stubGlobal('navigator', { userAgent: 'Mozilla/5.0 (X11; Linux x86_64; rv:121.0) Gecko/20100101 Firefox/121.0' });
      expect(getDeviceLabel()).toBe('Firefox on Linux');
    });

    it('detects Edge correctly', () => {
      vi.stubGlobal('navigator', { userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 Edg/120.0.0.0' });
      expect(getDeviceLabel()).toBe('Edge on Windows');
    });
  });

  describe('getDeviceMetadata', () => {
    it('returns both deviceId and deviceLabel', () => {
      localStorage.setItem('crescendo_device_id', 'test-uuid-1234');
      const metadata = getDeviceMetadata();
      
      expect(metadata).toEqual({
        deviceId: 'test-uuid-1234',
        deviceLabel: 'Chrome on Windows'
      });
    });
  });
});
