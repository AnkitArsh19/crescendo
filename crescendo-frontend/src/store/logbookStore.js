import { create } from 'zustand';
import { allRunsApi, workflowRunApi } from '../api/logbookApi';


const useLogbookStore = create((set) => ({
  // All-runs view (cross-workflow)
  runs: [],
  page: null,
  isLoading: false,
  error: null,

  // Single run detail
  runDetail: null,
  isLoadingDetail: false,
  detailError: null,

  // Workflow-scoped stats
  stats: null,

  // ─── Cross-workflow runs ───────────────────────────────────────────────────

  fetchAllRuns: async (page = 0, size = 20) => {
    set({ isLoading: true, error: null });
    try {
      const data = await allRunsApi.list(page, size);
      set({ runs: data.content, page: data, isLoading: false });
    } catch (err) {
      set({
        error: err.response?.data?.message || 'Failed to load runs',
        isLoading: false,
      });
    }
  },

  // ─── Workflow-scoped runs ──────────────────────────────────────────────────

  fetchWorkflowRuns: async (workflowId, page = 0, size = 20) => {
    set({ isLoading: true, error: null });
    try {
      const data = await workflowRunApi.list(workflowId, page, size);
      set({ runs: data.content, page: data, isLoading: false });
    } catch (err) {
      set({
        error: err.response?.data?.message || 'Failed to load runs',
        isLoading: false,
      });
    }
  },

  // ─── Run detail ────────────────────────────────────────────────────────────

  fetchRunDetail: async (workflowId, runId) => {
    set({ isLoadingDetail: true, detailError: null });
    try {
      const data = await workflowRunApi.get(workflowId, runId);
      set({ runDetail: data, isLoadingDetail: false });
    } catch (err) {
      set({
        detailError: err.response?.data?.message || 'Failed to load run detail',
        isLoadingDetail: false,
      });
    }
  },

  clearRunDetail: () => set({ runDetail: null, detailError: null }),

  // ─── Actions ───────────────────────────────────────────────────────────────

  cancelRun: async (workflowId, runId) => {
    await workflowRunApi.cancel(workflowId, runId);
    set((state) => ({
      runs: state.runs.map((r) =>
        r.id === runId ? { ...r, status: 'FAILED', errorMessage: 'Cancelled by user' } : r
      ),
      runDetail: state.runDetail?.id === runId
        ? { ...state.runDetail, status: 'FAILED', errorMessage: 'Cancelled by user' }
        : state.runDetail,
    }));
  },

  // ─── Stats ─────────────────────────────────────────────────────────────────

  fetchStats: async (workflowId) => {
    try {
      const data = await workflowRunApi.stats(workflowId);
      set({ stats: data });
    } catch {
      // stats are non-critical
    }
  },
}));

export default useLogbookStore;
