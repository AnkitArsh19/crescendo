import { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { HiOutlineDownload, HiOutlineExternalLink } from 'react-icons/hi';
import { FaWindows, FaApple, FaLinux } from 'react-icons/fa';
import './DownloadsSection.css';

const RELEASE_VERSION = '1.0.2';
const RELEASE_TAG = `v${RELEASE_VERSION}`;
const RELEASE_BASE = `https://github.com/AnkitArsh19/crescendo/releases/download/${RELEASE_TAG}`;
const RELEASE_PAGE = `https://github.com/AnkitArsh19/crescendo/releases/tag/${RELEASE_TAG}`;

const platforms = [
  {
    id: 'windows',
    name: 'Windows',
    icon: <FaWindows />,
    arch: 'x64 • Windows 10 / 11',
    desc: 'Native desktop installer with automatic background updates and system tray controls.',
    primaryLabel: 'Download for Windows',
    primaryUrl: `${RELEASE_BASE}/Crescendo_${RELEASE_VERSION}_x64-setup.exe`,
    secondary: [
      { label: '.msi package', url: `${RELEASE_BASE}/Crescendo_${RELEASE_VERSION}_x64_en-US.msi` },
    ],
  },
  {
    id: 'macos',
    name: 'macOS',
    icon: <FaApple />,
    arch: 'Universal • Apple Silicon & Intel',
    desc: 'Optimized universal build with native menu bar integration and deep-link protocol routing.',
    primaryLabel: 'Download for macOS',
    primaryUrl: `${RELEASE_BASE}/Crescendo_${RELEASE_VERSION}_universal.dmg`,
    secondary: [
      { label: '.tar.gz', url: `${RELEASE_BASE}/Crescendo_universal.app.tar.gz` },
    ],
  },
  {
    id: 'linux',
    name: 'Linux',
    icon: <FaLinux />,
    arch: 'x86_64 • AppImage, deb, rpm',
    desc: 'Portable standalone executable and distribution packages for Ubuntu, Debian, and Fedora.',
    primaryLabel: 'Download .AppImage',
    primaryUrl: `${RELEASE_BASE}/Crescendo_${RELEASE_VERSION}_amd64.AppImage`,
    secondary: [
      { label: '.deb', url: `${RELEASE_BASE}/Crescendo_${RELEASE_VERSION}_amd64.deb` },
      { label: '.rpm', url: `${RELEASE_BASE}/Crescendo-${RELEASE_VERSION}-1.x86_64.rpm` },
    ],
  },
];

const containerVariants = {
  hidden: {},
  visible: {
    transition: {
      staggerChildren: 0.1,
      delayChildren: 0.1,
    },
  },
};

const cardVariants = {
  hidden: { opacity: 0, y: 28 },
  visible: {
    opacity: 1,
    y: 0,
    transition: { duration: 0.6, ease: [0.22, 1, 0.36, 1] },
  },
};

export default function DownloadsSection() {
  const [detectedOs, setDetectedOs] = useState(null);

  useEffect(() => {
    if (typeof window === 'undefined') return;
    const ua = window.navigator.userAgent.toLowerCase();
    if (ua.includes('win')) {
      setDetectedOs('windows');
    } else if (ua.includes('mac')) {
      setDetectedOs('macos');
    } else if (ua.includes('linux')) {
      setDetectedOs('linux');
    }
  }, []);

  return (
    <section className="downloads-section" id="downloads">
      <div className="downloads-header">
        <motion.p
          className="section-label"
          initial={{ opacity: 0, y: 12 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: false, amount: 0.3 }}
          transition={{ duration: 0.5 }}
        >
          Desktop Client
        </motion.p>
        <motion.h2
          className="section-title"
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: false, amount: 0.3 }}
          transition={{ duration: 0.6, delay: 0.08 }}
        >
          Automation on your <span className="font-serif" style={{ fontStyle: 'italic' }}>desktop</span>
        </motion.h2>
        <motion.p
          className="section-subtitle"
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: false, amount: 0.3 }}
          transition={{ duration: 0.6, delay: 0.16 }}
        >
          Experience lightning-fast workflow orchestration directly from your system tray. Built with Tauri v2 for near-zero memory footprint.
        </motion.p>
      </div>

      <motion.div
        className="downloads-grid"
        variants={containerVariants}
        initial="hidden"
        whileInView="visible"
        viewport={{ once: false, amount: 0.2 }}
      >
        {platforms.map((p) => {
          const isDetected = detectedOs === p.id;
          return (
            <motion.div
              key={p.id}
              className={`download-card ${isDetected ? 'is-detected' : ''}`}
              variants={cardVariants}
            >
              {isDetected && (
                <div className="download-badge">Your OS</div>
              )}

              <div className="download-card-top">
                <div className="download-icon-box">
                  {p.icon}
                </div>
                <div>
                  <h3 className="download-platform-name">{p.name}</h3>
                  <span className="download-platform-arch">{p.arch}</span>
                </div>
              </div>

              <p className="download-desc">{p.desc}</p>

              <a
                href={p.primaryUrl}
                className="download-btn-main"
                download
              >
                <HiOutlineDownload className="download-btn-icon" />
                <span>{p.primaryLabel}</span>
              </a>

              {p.secondary && p.secondary.length > 0 && (
                <div className="download-sublinks">
                  <span className="download-sublinks-label">Also available:</span>
                  {p.secondary.map((sec, idx) => (
                    <a
                      key={idx}
                      href={sec.url}
                      className="download-sublink"
                      download
                    >
                      {sec.label}
                    </a>
                  ))}
                </div>
              )}
            </motion.div>
          );
        })}
      </motion.div>

      <div className="downloads-footer">
        <span>Release {RELEASE_TAG}</span>
        <span className="downloads-sep">•</span>
        <a
          href={RELEASE_PAGE}
          target="_blank"
          rel="noopener noreferrer"
          className="downloads-github"
        >
          View all release assets on GitHub <HiOutlineExternalLink />
        </a>
      </div>
    </section>
  );
}
