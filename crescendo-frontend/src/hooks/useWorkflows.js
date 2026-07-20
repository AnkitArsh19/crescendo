import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { workflowClient } from '../api/workflowClient';

const workflowKeys = {
    all: ['workflows'],
    detail: (id) => ['workflow', id],
};

const updateListItem = (workflows, id, update) => (workflows || []).map((workflow) => (
    workflow.id === id ? { ...workflow, ...update } : workflow
));

export function useWorkflowList() {
    return useQuery({
        queryKey: workflowKeys.all,
        queryFn: workflowClient.list,
        staleTime: 30_000,
        refetchOnWindowFocus: 'always',
    });
}

export function useWorkflowDetail(workflowId) {
    return useQuery({
        queryKey: workflowKeys.detail(workflowId),
        queryFn: () => workflowClient.get(workflowId),
        staleTime: Infinity,
        refetchOnWindowFocus: 'always',
        enabled: Boolean(workflowId),
    });
}

export function useCreateWorkflow() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: workflowClient.create,
        onSuccess: () => queryClient.invalidateQueries({ queryKey: workflowKeys.all }),
    });
}

export function useUpdateWorkflow() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: ({ id, data }) => workflowClient.update(id, data),
        onMutate: async ({ id, data }) => {
            await queryClient.cancelQueries({ queryKey: workflowKeys.all });
            const previous = queryClient.getQueryData(workflowKeys.all);
            queryClient.setQueryData(workflowKeys.all, (workflows) => updateListItem(workflows, id, data));
            return { previous };
        },
        onError: (_error, _variables, context) => queryClient.setQueryData(workflowKeys.all, context?.previous),
        onSettled: (_data, _error, { id }) => Promise.all([
            queryClient.invalidateQueries({ queryKey: workflowKeys.all }),
            queryClient.invalidateQueries({ queryKey: workflowKeys.detail(id) }),
        ]),
    });
}

export function useDeleteWorkflow() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: workflowClient.delete,
        onMutate: async (id) => {
            await queryClient.cancelQueries({ queryKey: workflowKeys.all });
            const previous = queryClient.getQueryData(workflowKeys.all);
            queryClient.setQueryData(workflowKeys.all, (workflows) => (workflows || []).filter((workflow) => workflow.id !== id));
            return { previous };
        },
        onError: (_error, _id, context) => queryClient.setQueryData(workflowKeys.all, context?.previous),
        onSettled: () => queryClient.invalidateQueries({ queryKey: workflowKeys.all }),
    });
}

function useWorkflowStateMutation(mutationFn, active) {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn,
        onMutate: async (id) => {
            await queryClient.cancelQueries({ queryKey: workflowKeys.all });
            const previous = queryClient.getQueryData(workflowKeys.all);
            queryClient.setQueryData(workflowKeys.all, (workflows) => updateListItem(workflows, id, {
                isActive: active,
                status: active ? 'ACTIVE' : 'INACTIVE',
            }));
            return { previous };
        },
        onError: (_error, _id, context) => queryClient.setQueryData(workflowKeys.all, context?.previous),
        onSettled: (_data, _error, id) => Promise.all([
            queryClient.invalidateQueries({ queryKey: workflowKeys.all }),
            queryClient.invalidateQueries({ queryKey: workflowKeys.detail(id) }),
        ]),
    });
}

export function useActivateWorkflow() {
    return useWorkflowStateMutation(workflowClient.activate, true);
}

export function useDeactivateWorkflow() {
    return useWorkflowStateMutation(workflowClient.deactivate, false);
}

export function useSaveWorkflowGraph() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: ({ id, data }) => workflowClient.updateGraph(id, data),
        onSettled: (_data, _error, { id }) => Promise.all([
            queryClient.invalidateQueries({ queryKey: workflowKeys.detail(id) }),
            queryClient.invalidateQueries({ queryKey: workflowKeys.all }),
        ]),
    });
}

export { workflowKeys };
