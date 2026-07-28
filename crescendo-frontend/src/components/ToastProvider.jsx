import { AnimatePresence, motion } from 'framer-motion';
import { HiOutlineCheckCircle, HiOutlineXCircle, HiOutlineInformationCircle, HiOutlineExclamation, HiX } from 'react-icons/hi';
import useToastStore from '../store/toastStore';
import './ToastProvider.css';

const iconMap = {
  success: <HiOutlineCheckCircle />,
  error: <HiOutlineXCircle />,
  info: <HiOutlineInformationCircle />,
  warning: <HiOutlineExclamation />,
};

export default function ToastProvider() {
  const { toasts, removeToast } = useToastStore();

  return (
    <div className="toast-container">
      <AnimatePresence mode="popLayout">
        {toasts.map((toast) => (
          <motion.div
            key={toast.id}
            layout
            className={`toast-item ${toast.type}`}
            initial={{ opacity: 0, y: 35, scale: 0.88, filter: 'blur(8px)' }}
            animate={{ opacity: 1, y: 0, scale: 1, filter: 'blur(0px)' }}
            exit={{ opacity: 0, scale: 0.85, filter: 'blur(8px)', transition: { duration: 0.2, ease: 'easeIn' } }}
            transition={{ 
              type: 'spring', 
              damping: 24, 
              stiffness: 320, 
              mass: 0.8,
              layout: { duration: 0.25, ease: [0.22, 1, 0.36, 1] }
            }}
            onClick={() => removeToast(toast.id)}
            whileHover={{ scale: 1.02, y: -2 }}
          >
            <motion.span 
              className="toast-icon"
              initial={{ scale: 0, rotate: -30 }}
              animate={{ scale: 1, rotate: 0 }}
              transition={{ delay: 0.1, type: 'spring', stiffness: 400 }}
            >
              {iconMap[toast.type] || iconMap.info}
            </motion.span>
            <span className="toast-message">{toast.message}</span>
            <button className="toast-dismiss" onClick={(e) => { e.stopPropagation(); removeToast(toast.id); }}>
              <HiX />
            </button>
            
            {/* Animated countdown progress bar */}
            <motion.div
              className="toast-progress-bar"
              initial={{ width: '100%' }}
              animate={{ width: '0%' }}
              transition={{ duration: (toast.duration || 3500) / 1000, ease: 'linear' }}
            />
          </motion.div>
        ))}
      </AnimatePresence>
    </div>
  );
}
