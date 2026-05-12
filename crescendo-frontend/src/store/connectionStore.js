import { create } from 'zustand';
import { connectionsApi } from '../api/connectionsApi';

const useConnectionStore = create((set, get) => ({
  connections: [],
  isLoading: false,
  error: null,

  fetchConnections: async () => {
    set({ isLoading: true, error: null });
    try {
      const data = await connectionsApi.list();
      set({ connections: data, isLoading: false });
    } catch (err) {
      set({ error: err.response?.data?.message || 'Failed to load connections', isLoading: false });
    }
  },

  createConnection: async (payload) => {
    const data = await connectionsApi.create(payload);
    set({ connections: [...get().connections, data] });
    return data;
  },

  updateConnection: async (id, payload) => {
    await connectionsApi.update(id, payload);
    // Re-fetch to get updated data
    await get().fetchConnections();
  },

  deleteConnection: async (id) => {
    await connectionsApi.delete(id);
    set({ connections: get().connections.filter((c) => c.id !== id) });
  },
}));

export default useConnectionStore;
