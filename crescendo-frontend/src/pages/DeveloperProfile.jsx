import React from 'react';
import { motion } from 'framer-motion';
import DotCanvas from '../components/DotCanvas';
import Navbar from '../components/Navbar';
import Footer from '../components/Footer';
import { FiBook, FiCpu, FiHash, FiMapPin, FiLayers, FiCode } from 'react-icons/fi';

const DeveloperProfile = () => {
  return (
    <>
      <DotCanvas />
      <div style={{ position: 'relative', zIndex: 1, minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
        <Navbar />

        <main style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '120px 20px 80px', position: 'relative' }}>
          {/* Subtle Background Ornaments to enhance the card */}
          <div
            style={{
              position: 'absolute',
              top: '10%',
              left: '20%',
              width: '400px',
              height: '400px',
              background: 'radial-gradient(circle, rgba(147,51,234,0.05) 0%, transparent 60%)',
              borderRadius: '50%',
              filter: 'blur(80px)',
              pointerEvents: 'none'
            }}
          />
          <div
            style={{
              position: 'absolute',
              bottom: '10%',
              right: '20%',
              width: '400px',
              height: '400px',
              background: 'radial-gradient(circle, rgba(59,130,246,0.05) 0%, transparent 60%)',
              borderRadius: '50%',
              filter: 'blur(80px)',
              pointerEvents: 'none'
            }}
          />

          <motion.div
            initial={{ opacity: 0, y: 30 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6, ease: [0.22, 1, 0.36, 1] }}
            style={{
              position: 'relative',
              zIndex: 10,
              width: '100%',
              maxWidth: '900px', /* Wider card */
              background: 'var(--bg-glass)',
              backdropFilter: 'blur(24px)',
              WebkitBackdropFilter: 'blur(24px)',
              border: '1px solid var(--border-subtle)',
              borderRadius: 'var(--radius-xl)',
              boxShadow: 'var(--shadow-3d)',
              overflow: 'hidden',
              display: 'flex',
              flexDirection: 'column',
              padding: 'clamp(32px, 5vw, 48px)',
              gap: '32px'
            }}
          >
            {/* Profile Header Section */}
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '32px',
                flexWrap: 'wrap'
              }}
            >
              <motion.div
                initial={{ scale: 0.8, opacity: 0 }}
                animate={{ scale: 1, opacity: 1 }}
                transition={{ delay: 0.1, duration: 0.5, type: 'spring', bounce: 0.4 }}
                style={{
                  width: '120px', /* Smaller image */
                  height: '120px',
                  flexShrink: 0,
                  position: 'relative',
                }}
              >
                <div
                  style={{
                    position: 'absolute',
                    inset: 0,
                    borderRadius: '50%',
                    background: 'var(--gradient-shine)',
                    padding: '2px',
                    boxShadow: 'var(--shadow-glow)'
                  }}
                >
                  <img
                    src="/developer.jpg"
                    alt="Ankit Arsh"
                    style={{
                      width: '100%',
                      height: '100%',
                      objectFit: 'cover',
                      borderRadius: '50%',
                      border: '2px solid var(--bg-card)'
                    }}
                  />
                </div>
              </motion.div>

              <div style={{ flex: 1, minWidth: '240px' }}>
                <motion.div
                  initial={{ opacity: 0, x: -10 }}
                  animate={{ opacity: 1, x: 0 }}
                  transition={{ delay: 0.2, duration: 0.4 }}
                  style={{
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: '8px',
                    padding: '6px 12px',
                    borderRadius: '100px',
                    background: 'var(--bg-elevated)',
                    border: '1px solid var(--border-subtle)',
                    marginBottom: '12px'
                  }}
                >
                  <div style={{ width: '6px', height: '6px', borderRadius: '50%', backgroundColor: '#10b981' }} />
                  <span
                    style={{
                      fontSize: '0.75rem',
                      fontWeight: 600,
                      textTransform: 'uppercase',
                      letterSpacing: '1px',
                      color: 'var(--text-secondary)'
                    }}
                  >
                    Developer Profile
                  </span>
                </motion.div>

                <motion.h1
                  initial={{ opacity: 0, x: -10 }}
                  animate={{ opacity: 1, x: 0 }}
                  transition={{ delay: 0.3, duration: 0.4 }}
                  style={{
                    fontSize: '2rem', /* Much smaller font size */
                    fontWeight: 800,
                    color: 'var(--text-accent)',
                    letterSpacing: '-0.5px',
                    lineHeight: '1.2',
                    marginBottom: '8px'
                  }}
                >
                  Ankit Arsh
                </motion.h1>
                
                <motion.p
                  initial={{ opacity: 0, x: -10 }}
                  animate={{ opacity: 1, x: 0 }}
                  transition={{ delay: 0.4, duration: 0.4 }}
                  style={{
                    fontSize: '0.95rem',
                    color: 'var(--text-secondary)',
                    maxWidth: '500px',
                    lineHeight: '1.6'
                  }}
                >
                  Information Technology Student at KIIT University. Passionate about building scalable software solutions.
                </motion.p>
              </div>
            </div>
            
            <div style={{ height: '1px', background: 'var(--border-subtle)', width: '100%' }} />

            {/* Data Grid Section */}
            <div
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))',
                gap: '24px'
              }}
            >
              <InfoItem label="University" value="KIIT University" icon={<FiMapPin />} delay={0.4} />
              <InfoItem label="School" value="School of Computer Engineering" icon={<FiBook />} delay={0.45} />
              <InfoItem label="Branch" value="Information Technology" icon={<FiCpu />} delay={0.5} />
              <InfoItem label="Roll Number" value="2306184" icon={<FiHash />} delay={0.55} />
              <InfoItem label="Semester" value="6th" icon={<FiLayers />} delay={0.6} />
              <InfoItem label="Section" value="IT02" icon={<FiCode />} delay={0.65} />
            </div>
          </motion.div>
        </main>

        <Footer />
      </div>
    </>
  );
};

const InfoItem = ({ label, value, icon, delay }) => (
  <motion.div
    initial={{ opacity: 0, y: 15 }}
    animate={{ opacity: 1, y: 0 }}
    transition={{ delay, duration: 0.5, ease: [0.22, 1, 0.36, 1] }}
    style={{
      display: 'flex',
      alignItems: 'center',
      gap: '16px',
      padding: '16px',
      borderRadius: 'var(--radius-md)',
      backgroundColor: 'transparent',
      border: '1px solid transparent',
      transition: 'all var(--transition-fast)',
    }}
    onMouseOver={(e) => {
      e.currentTarget.style.backgroundColor = 'var(--bg-card)';
      e.currentTarget.style.borderColor = 'var(--border-subtle)';
    }}
    onMouseOut={(e) => {
      e.currentTarget.style.backgroundColor = 'transparent';
      e.currentTarget.style.borderColor = 'transparent';
    }}
  >
    <div 
      style={{ 
        fontSize: '18px',
        color: 'var(--text-tertiary)',
        width: '36px',
        height: '36px',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        background: 'var(--bg-elevated)',
        borderRadius: '10px',
        border: '1px solid var(--border-subtle)',
        flexShrink: 0
      }}
    >
      {icon}
    </div>
    <div>
      <p
        style={{
          fontSize: '0.75rem',
          textTransform: 'uppercase',
          letterSpacing: '0.5px',
          fontWeight: 600,
          color: 'var(--text-tertiary)',
          marginBottom: '2px'
        }}
      >
        {label}
      </p>
      <p
        style={{
          fontSize: '1rem',
          fontWeight: 500,
          color: 'var(--text-primary)',
          letterSpacing: '0.1px',
          lineHeight: '1.2'
        }}
      >
        {value}
      </p>
    </div>
  </motion.div>
);

export default DeveloperProfile;
