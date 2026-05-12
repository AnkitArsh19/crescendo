import { create } from 'zustand';
import { contactsApi } from '../api/contactsApi';

const useContactStore = create((set, get) => ({
  contacts: [],
  loading: false,

  fetchContacts: async () => {
    set({ loading: true });
    try {
      const data = await contactsApi.list();
      set({ contacts: Array.isArray(data) ? data : [], loading: false });
    } catch {
      set({ loading: false });
    }
  },

  createContact: async (contact) => {
    const data = await contactsApi.create(contact);
    set({ contacts: [data, ...get().contacts] });
    return data;
  },

  updateContact: async (id, updates) => {
    const data = await contactsApi.update(id, updates);
    set({ contacts: get().contacts.map(c => c.id === id ? data : c) });
    return data;
  },

  deleteContact: async (id) => {
    await contactsApi.delete(id);
    set({ contacts: get().contacts.filter(c => c.id !== id) });
  },
}));

export default useContactStore;
