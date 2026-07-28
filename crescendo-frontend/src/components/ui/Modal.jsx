import { motion, AnimatePresence } from 'framer-motion';
import { HiX } from 'react-icons/hi';
import { BorderBeam } from './BorderBeam';
import './Modal.css';

export default function Modal({ open, onClose, title, description, children, noBorderBeam = false }) {
    return (
        <AnimatePresence>
            {open && (
                <motion.div
                    className="modal-overlay"
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    exit={{ opacity: 0 }}
                    transition={{ duration: 0.2 }}
                    onClick={onClose}
                >
                    <motion.div
                        className="modal-card"
                        initial={{ opacity: 0, scale: 0.65, y: 40 }}
                        animate={{ opacity: 1, scale: 1, y: 0 }}
                        exit={{ opacity: 0, scale: 0.75, y: 20 }}
                        transition={{ type: "spring", stiffness: 380, damping: 18, mass: 0.8 }}
                        onClick={(e) => e.stopPropagation()}
                    >
                        {!noBorderBeam && <BorderBeam duration={8} borderWidth={2} />}
                        <button className="modal-close" onClick={onClose}>
                            <HiX />
                        </button>
                        {title && <h3 className="modal-title">{title}</h3>}
                        {description && <p className="modal-desc">{description}</p>}
                        {children}
                    </motion.div>
                </motion.div>
            )}
        </AnimatePresence>
    );
}
