import { describe, it, expect, beforeEach, vi } from 'vitest';
import useWorkflowStore from '../../../src/store/workflowStore';
import { workflowApi } from '../../../src/api/workflowApi';
import useToastStore from '../../../src/store/toastStore';

// Mock workflowApi
vi.mock('../../../src/api/workflowApi', () => ({
  workflowApi: {
    list: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    delete: vi.fn(),
    activate: vi.fn(),
    deactivate: vi.fn(),
    bulkActivate: vi.fn(),
    bulkDeactivate: vi.fn(),
  }
}));

// Mock toastStore
vi.mock('../../../src/store/toastStore', () => ({
  default: {
    getState: vi.fn(() => ({
      addToast: vi.fn()
    }))
  }
}));

describe('workflowStore', () => {
  beforeEach(() => {
    // Reset store state
    useWorkflowStore.setState({
      workflows: [],
      isLoading: false,
      error: null,
    });
    vi.clearAllMocks();
  });

  describe('fetchWorkflows', () => {
    it('fetches workflows successfully', async () => {
      const mockWorkflows = [{ id: '1', name: 'Flow 1' }, { id: '2', name: 'Flow 2' }];
      workflowApi.list.mockResolvedValueOnce(mockWorkflows);

      await useWorkflowStore.getState().fetchWorkflows();
      
      const state = useWorkflowStore.getState();
      expect(state.isLoading).toBe(false);
      expect(state.error).toBeNull();
      expect(state.workflows).toEqual(mockWorkflows);
      expect(workflowApi.list).toHaveBeenCalled();
    });

    it('handles fetch error', async () => {
      workflowApi.list.mockRejectedValueOnce({
        response: { data: { message: 'Network error' } }
      });

      await useWorkflowStore.getState().fetchWorkflows();
      
      const state = useWorkflowStore.getState();
      expect(state.isLoading).toBe(false);
      expect(state.error).toBe('Network error');
      expect(state.workflows).toEqual([]);
    });
  });

  describe('createWorkflow', () => {
    it('creates a workflow and updates state', async () => {
      const newWorkflow = { id: '3', name: 'New Flow', description: 'Desc' };
      workflowApi.create.mockResolvedValueOnce(newWorkflow);
      
      // Seed initial state
      useWorkflowStore.setState({ workflows: [{ id: '1', name: 'Flow 1' }] });

      const result = await useWorkflowStore.getState().createWorkflow('New Flow', 'Desc');
      
      expect(result).toEqual(newWorkflow);
      const state = useWorkflowStore.getState();
      expect(state.workflows).toHaveLength(2);
      expect(state.workflows[0]).toEqual(newWorkflow); // should be unshifted
      expect(workflowApi.create).toHaveBeenCalledWith({ name: 'New Flow', description: 'Desc' });
    });
  });

  describe('activateWorkflow (Optimistic UI)', () => {
    it('optimistically updates to active, then reconciles on success', async () => {
      const initialFlow = { id: '1', name: 'Flow 1', isActive: false, status: 'INACTIVE' };
      useWorkflowStore.setState({ workflows: [initialFlow] });
      
      workflowApi.activate.mockResolvedValueOnce({});
      workflowApi.list.mockResolvedValueOnce([{ ...initialFlow, isActive: true, status: 'ACTIVE' }]);

      const promise = useWorkflowStore.getState().activateWorkflow('1');
      
      // Before promise resolves, state should be optimistically updated
      let state = useWorkflowStore.getState();
      expect(state.workflows[0].isActive).toBe(true);
      expect(state.workflows[0].status).toBe('ACTIVE');

      await promise;
      
      // Reconciled
      expect(workflowApi.activate).toHaveBeenCalledWith('1');
      expect(workflowApi.list).toHaveBeenCalled();
    });

    it('rolls back optimistic update on failure', async () => {
      const initialFlow = { id: '1', name: 'Flow 1', isActive: false, status: 'INACTIVE' };
      useWorkflowStore.setState({ workflows: [initialFlow] });
      
      workflowApi.activate.mockRejectedValueOnce(new Error('Failed'));

      await expect(useWorkflowStore.getState().activateWorkflow('1')).rejects.toThrow('Failed');
      
      // Should roll back to INACTIVE
      const state = useWorkflowStore.getState();
      expect(state.workflows[0].isActive).toBe(false);
      expect(state.workflows[0].status).toBe('INACTIVE');
    });
  });
});
