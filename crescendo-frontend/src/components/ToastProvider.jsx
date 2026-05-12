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
      <AnimatePresence>
        {toasts.map((toast) => (
          <motion.div
            key={toast.id}
            className={`toast-item ${toast.type}`}
            initial={{ opacity: 0, x: 80, scale: 0.95 }}
            animate={{ opacity: 1, x: 0, scale: 1 }}
            exit={{ opacity: 0, x: 80, scale: 0.95 }}
            transition={{ duration: 0.25, ease: [0.22, 1, 0.36, 1] }}
            onClick={() => removeToast(toast.id)}
          >
            <span className="toast-icon">{iconMap[toast.type] || iconMap.info}</span>
            <span className="toast-message">{toast.message}</span>
            <button className="toast-dismiss" onClick={(e) => { e.stopPropagation(); removeToast(toast.id); }}>
              <HiX />
            </button>
          </motion.div>
        ))}
      </AnimatePresence>
    </div>
  );
}
