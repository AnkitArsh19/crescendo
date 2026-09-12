import { useEffect } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import useAuthStore from '../store/authStore';
import { workflowKeys } from './useWorkflows';
import api from '../api/axios';

const eventUrl = `${import.meta.env.VITE_API_URL || 'https://api.crescendo.run'}/workflows/events`;

function isTokenExpired(token) {
    if (!token) return true;
    try {
        const base64Url = token.split('.')[1];
        if (!base64Url) return false;
        const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
        const payload = JSON.parse(window.atob(base64));
        return payload.exp ? Date.now() >= (payload.exp - 5) * 1000 : false;
    } catch {
        return false;
    }
}

export default function useWorkflowEventStream() {
    const queryClient = useQueryClient();
    const isGuest = useAuthStore((state) => state.isGuest);
    const accessToken = useAuthStore((state) => state.accessToken);

    useEffect(() => {
        // Native EventSource cannot attach the Bearer header used by this app.
        // The short-lived access token is passed as an SSE-only query parameter
        // and validated by the controller before opening the stream.
        if (isGuest || !accessToken) return undefined;

        // If token is already expired, proactively refresh it via axios instead of opening dead stream
        if (isTokenExpired(accessToken)) {
            api.get('/users/me').catch(() => {});
            return undefined;
        }

        let isSubscribed = true;
        let retryTimer = null;
        let source = null;

        const connect = () => {
            if (!isSubscribed) return;

            // Do NOT set withCredentials: true since token is in the query string and
            // cross-origin cookies trigger Edge Tracking Prevention warnings.
            source = new EventSource(`${eventUrl}?access_token=${encodeURIComponent(accessToken)}`);

            source.addEventListener('workflow-changed', (event) => {
                try {
                    const { workflowId } = JSON.parse(event.data);
                    if (workflowId) queryClient.invalidateQueries({ queryKey: workflowKeys.detail(workflowId) });
                    queryClient.invalidateQueries({ queryKey: workflowKeys.all });
                } catch {
                    queryClient.invalidateQueries({ queryKey: workflowKeys.all });
                }
            });

            source.onerror = () => {
                // Immediately close the dead source to abort browser's 3-second retry loop
                if (source) {
                    source.close();
                    source = null;
                }

                if (!isSubscribed) return;

                // Trigger token refresh via axios interceptor if the token was expired
                api.get('/users/me').catch(() => {});

                // Backoff reconnect after 10s if the token was not updated by refresh
                retryTimer = setTimeout(() => {
                    if (isSubscribed) connect();
                }, 10000);
            };
        };

        connect();

        return () => {
            isSubscribed = false;
            if (retryTimer) clearTimeout(retryTimer);
            if (source) source.close();
        };
    }, [accessToken, isGuest, queryClient]);
}

