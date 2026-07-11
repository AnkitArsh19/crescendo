import { describe, it, expect, beforeEach, vi } from 'vitest';
import { createSaveCoordinator } from '../../../src/workflow/workflowSaveCoordinator';
import { server } from '../../../src/mocks/server';
import { http, HttpResponse } from 'msw';

// Mock dependencies
vi.mock('../../../src/api/appCatalogApi', () => ({
  appCatalogApi: {
    get: vi.fn().mockResolvedValue({})
  }
}));

vi.mock('../../../src/workflow/workflowGraphSerializer', () => ({
  orderedNodesFromGraph: vi.fn((nodes) => nodes),
  validateGraphForSave: vi.fn(() => null),
  edgesToPayload: vi.fn(() => []),
  validateNodeForSave: vi.fn(() => null), // Always valid
  nodeToStepPayload: vi.fn((node) => ({
    backendId: node.data?._backendId || null,
    type: 'ACTION',
    name: 'Test Step',
    actionKey: 'testAction',
    appKey: 'testApp',
    connectionId: null,
    configuration: {}
  }))
}));

vi.mock('../../../src/store/toastStore', () => ({
  default: {
    getState: vi.fn(() => ({
      addToast: vi.fn()
    }))
  }
}));

describe('workflowSaveCoordinator', () => {
  let callbacks;
  let draftMock;
  let saveCoordinator;
  let nodes;

  beforeEach(() => {
    vi.clearAllMocks();

    draftMock = {
      getDelta: vi.fn(() => ({ deletedBackendIds: new Set(), serverRevision: 1 })),
      markSaved: vi.fn(),
      reset: vi.fn(),
      isDirty: vi.fn(() => true)
    };

    nodes = [
      { id: 'node-1', type: 'trigger', data: { appKey: 'testApp' } },
      { id: 'node-2', type: 'action', data: { appKey: 'testApp' } },
    ];

    callbacks = {
      getNodes: vi.fn(() => nodes),
      getEdges: vi.fn(() => [{ id: 'edge-1', source: 'node-1', target: 'node-2' }]),
      getWorkflowId: vi.fn(() => 'test-workflow-id'),
      getWorkflowName: vi.fn(() => 'Test Workflow'),
      getCatalogApps: vi.fn(() => []),
      getAppDetailsByKey: vi.fn(() => ({})),
      draft: draftMock,
      onWorkflowCreated: vi.fn(),
      onNodeSaved: vi.fn(),
      onSaveStart: vi.fn(),
      onSaveSuccess: vi.fn(),
      onSaveError: vi.fn(),
      onDirtyChange: vi.fn(),
    };

    saveCoordinator = createSaveCoordinator(callbacks);
  });

  describe('saveManual', () => {
    it('performs a complete validated save successfully', async () => {
      server.use(
        http.patch('https://api.crescendo.run/guest/workflows/test-workflow-id', () => {
          return HttpResponse.json({ id: 'test-workflow-id' });
        }),
        http.put('https://api.crescendo.run/guest/workflows/test-workflow-id/graph', () => {
          return HttpResponse.json({
            revision: 2,
            savedSteps: [{ clientId: 'node-1', backendId: 'backend-1' }]
          });
        })
      );

      const result = await saveCoordinator.saveManual();

      expect(result).toBe('test-workflow-id');
      expect(callbacks.onSaveStart).toHaveBeenCalled();
      expect(callbacks.onSaveSuccess).toHaveBeenCalled();
      expect(callbacks.onNodeSaved).toHaveBeenCalledWith('node-1', 'backend-1');
      expect(draftMock.reset).toHaveBeenCalledWith(2);
      expect(callbacks.onDirtyChange).toHaveBeenCalledWith(false);
    });

    it('handles 500 API errors and triggers onSaveError', async () => {
      server.use(
        http.patch('https://api.crescendo.run/guest/workflows/test-workflow-id', () => {
          return HttpResponse.json({ id: 'test-workflow-id' });
        }),
        http.put('https://api.crescendo.run/guest/workflows/test-workflow-id/graph', () => {
          return new HttpResponse(null, { status: 500, statusText: 'Internal Server Error' });
        })
      );

      const result = await saveCoordinator.saveManual();

      expect(result).toBeNull();
      expect(callbacks.onSaveError).toHaveBeenCalled();
      expect(callbacks.onSaveSuccess).not.toHaveBeenCalled();
    });
  });

  describe('saveAuto', () => {
    it('debounces and saves successfully', async () => {
      vi.useFakeTimers();

      server.use(
        http.patch('https://api.crescendo.run/guest/workflows/test-workflow-id', () => {
          return HttpResponse.json({ id: 'test-workflow-id' });
        }),
        http.put('https://api.crescendo.run/guest/workflows/test-workflow-id/graph', () => {
          return HttpResponse.json({
            revision: 2,
            savedSteps: []
          });
        })
      );

      // Call it twice rapidly to test debounce
      saveCoordinator.saveAuto();
      saveCoordinator.saveAuto();

      // Fast-forward debounce timer (800ms)
      await vi.advanceTimersByTimeAsync(800);

      expect(callbacks.onSaveSuccess).toHaveBeenCalled();
      vi.useRealTimers();
    });
  });
});
