import { create } from 'zustand';
import { workflowApi } from '../api/workflowApi';
import { workflowClient } from '../api/workflowClient';
import useToastStore from './toastStore';

const useWorkflowStore = create((set, get) => ({
  workflows: [],
  isLoading: false,
  error: null,

  fetchWorkflows: async () => {
    set({ isLoading: true, error: null });
    try {
      const data = await workflowClient.list();
      set({ workflows: data, isLoading: false });
    } catch (err) {
      set({
        error: err.response?.data?.message || 'Failed to load workflows',
        isLoading: false,
      });
    }
  },

  createWorkflow: async (name, description = '') => {
    const workflow = await workflowClient.create({ name, description });
    set((state) => ({ workflows: [workflow, ...state.workflows] }));
    useToastStore.getState().addToast('Workflow created', 'success');
    return workflow;
  },

  updateWorkflow: async (id, data) => {
    await workflowClient.update(id, data);
    set((state) => ({
      workflows: state.workflows.map((w) =>
        w.id === id ? { ...w, ...data } : w
      ),
    }));
  },

  deleteWorkflow: async (id) => {
    await workflowClient.delete(id);
    set((state) => ({
      workflows: state.workflows.filter((w) => w.id !== id),
    }));
    useToastStore.getState().addToast('Workflow deleted', 'info');
  },

  activateWorkflow: async (id) => {
    // Optimistic update
    set((state) => ({
      workflows: state.workflows.map((w) =>
        w.id === id ? { ...w, isActive: true, status: 'ACTIVE' } : w
      ),
    }));
    try {
      await workflowApi.activate(id);
      useToastStore.getState().addToast('Workflow activated', 'success');
      // Re-fetch to reconcile server state
      const data = await workflowApi.list();
      set({ workflows: data });
    } catch (err) {
      // Rollback on failure
      set((state) => ({
        workflows: state.workflows.map((w) =>
          w.id === id ? { ...w, isActive: false, status: 'INACTIVE' } : w
        ),
      }));
      useToastStore.getState().addToast('Failed to activate workflow', 'error');
      throw err;
    }
  },

  deactivateWorkflow: async (id) => {
    // Optimistic update
    set((state) => ({
      workflows: state.workflows.map((w) =>
        w.id === id ? { ...w, isActive: false, status: 'INACTIVE' } : w
      ),
    }));
    try {
      await workflowApi.deactivate(id);
      useToastStore.getState().addToast('Workflow deactivated', 'info');
      // Re-fetch to reconcile server state
      const data = await workflowApi.list();
      set({ workflows: data });
    } catch (err) {
      // Rollback on failure
      set((state) => ({
        workflows: state.workflows.map((w) =>
          w.id === id ? { ...w, isActive: true, status: 'ACTIVE' } : w
        ),
      }));
      useToastStore.getState().addToast('Failed to deactivate workflow', 'error');
      throw err;
    }
  },

  bulkActivate: async (ids) => {
    await workflowApi.bulkActivate(ids);
    set((state) => ({
      workflows: state.workflows.map((w) =>
        ids.includes(w.id) ? { ...w, isActive: true, status: 'ACTIVE' } : w
      ),
    }));
    useToastStore.getState().addToast(`${ids.length} workflow(s) activated`, 'success');
  },

  bulkDeactivate: async (ids) => {
    await workflowApi.bulkDeactivate(ids);
    set((state) => ({
      workflows: state.workflows.map((w) =>
        ids.includes(w.id) ? { ...w, isActive: false, status: 'INACTIVE' } : w
      ),
    }));
    useToastStore.getState().addToast(`${ids.length} workflow(s) deactivated`, 'info');
  },
}));

export default useWorkflowStore;
