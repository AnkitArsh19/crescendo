import Modal from './Modal';
import './ConfirmModal.css';

export default function ConfirmModal({ open, onClose, title, description, onConfirm, confirmText = 'Confirm', cancelText = 'Cancel', isDestructive = false }) {
    return (
        <Modal open={open} onClose={onClose} title={title} description={description}>
            <div className="confirm-modal-actions">
                <button className="confirm-modal-cancel" onClick={onClose}>
                    {cancelText}
                </button>
                <button 
                    className={`confirm-modal-confirm ${isDestructive ? 'destructive' : ''}`} 
                    onClick={onConfirm}
                >
                    {confirmText}
                </button>
            </div>
        </Modal>
    );
}
