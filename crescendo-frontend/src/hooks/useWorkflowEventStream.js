import { useEffect } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import useAuthStore from '../store/authStore';
import { workflowKeys } from './useWorkflows';

const eventUrl = `${import.meta.env.VITE_API_URL || 'https://api.crescendo.run'}/workflows/events`;

export default function useWorkflowEventStream() {
    const queryClient = useQueryClient();
    const isGuest = useAuthStore((state) => state.isGuest);
    const accessToken = useAuthStore((state) => state.accessToken);

    useEffect(() => {
        // Native EventSource cannot attach the Bearer header used by this app.
        // The short-lived access token is passed as an SSE-only query parameter
        // and validated by the controller before opening the stream.
        if (isGuest || !accessToken) return undefined;

        const source = new EventSource(`${eventUrl}?access_token=${encodeURIComponent(accessToken)}`, { withCredentials: true });
        const onWorkflowChanged = (event) => {
            try {
                const { workflowId } = JSON.parse(event.data);
                if (workflowId) queryClient.invalidateQueries({ queryKey: workflowKeys.detail(workflowId) });
                queryClient.invalidateQueries({ queryKey: workflowKeys.all });
            } catch {
                queryClient.invalidateQueries({ queryKey: workflowKeys.all });
            }
        };
        source.addEventListener('workflow-changed', onWorkflowChanged);
        return () => source.close();
    }, [accessToken, isGuest, queryClient]);
}
