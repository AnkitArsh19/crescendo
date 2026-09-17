import { create } from 'zustand';

let toastId = 0;

const useToastStore = create((set) => ({
  toasts: [],

  addToast: (message, type = 'success', duration = 3500) => {
    let msg = message;
    let t = type;
    let d = duration;

    if (typeof message === 'object' && message !== null) {
      if (message instanceof Error) {
        msg = message.message || 'An error occurred';
        t = 'error';
      } else {
        msg = message.message || JSON.stringify(message);
        if (message.type) t = message.type;
        if (message.duration) d = message.duration;
      }
    } else if (typeof msg !== 'string') {
      msg = String(msg ?? '');
    }

    const id = ++toastId;
    set((state) => ({
      toasts: [...state.toasts, { id, message: msg, type: t, duration: d }],
    }));
    setTimeout(() => {
      set((state) => ({
        toasts: state.toasts.filter((t) => t.id !== id),
      }));
    }, d);
  },

  removeToast: (id) => {
    set((state) => ({
      toasts: state.toasts.filter((t) => t.id !== id),
    }));
  },
}));

export default useToastStore;
