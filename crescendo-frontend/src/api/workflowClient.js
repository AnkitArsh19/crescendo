import useAuthStore from '../store/authStore';
import { workflowApi, guestWorkflowApi, stepApi, guestStepApi } from './workflowApi';

/**
 * workflowClient intelligently routes API calls to either the authenticated endpoints
 * (/workflows/...) or the guest endpoints (/guest/workflows/...) based on the
 * current state of the authStore.
 */

function getSession() {
  return useAuthStore.getState().getGuestSessionId();
}

function isGuest() {
  return useAuthStore.getState().isGuest || !useAuthStore.getState().isAuthenticated;
}

export const workflowClient = {
  // Workflow CRUD
  list: () => isGuest() ? guestWorkflowApi.list(getSession()) : workflowApi.list(),
  get: (id) => isGuest() ? guestWorkflowApi.get(getSession(), id) : workflowApi.get(id),
  create: (data) => isGuest() ? guestWorkflowApi.create(getSession(), data) : workflowApi.create(data),
  update: (id, data) => isGuest() ? guestWorkflowApi.update(getSession(), id, data) : workflowApi.update(id, data),
  updateGraph: (id, data) => isGuest() ? guestWorkflowApi.updateGraph(getSession(), id, data) : workflowApi.updateGraph(id, data),
  delete: (id) => isGuest() ? guestWorkflowApi.delete(getSession(), id) : workflowApi.delete(id),

  // Step CRUD
  steps: {
    list: (workflowId) => isGuest() ? guestStepApi.list(getSession(), workflowId) : stepApi.list(workflowId),
    add: (workflowId, data) => isGuest() ? guestStepApi.add(getSession(), workflowId, data) : stepApi.add(workflowId, data),
    update: (workflowId, stepId, data) => isGuest() ? guestStepApi.update(getSession(), workflowId, stepId, data) : stepApi.update(workflowId, stepId, data),
    delete: (workflowId, stepId) => isGuest() ? guestStepApi.delete(getSession(), workflowId, stepId) : stepApi.delete(workflowId, stepId),
    reorder: (workflowId, stepId, newOrder) => isGuest() ? guestStepApi.reorder(getSession(), workflowId, stepId, newOrder) : stepApi.reorder(workflowId, stepId, newOrder),
  }
};
