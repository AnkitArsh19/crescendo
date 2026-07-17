import api from './axios';

export const sessionsApi = {
    getSessions: async () => {
        const response = await api.get('/auth/sessions');
        return response.data;
    },
    
    revokeSession: async (sessionId) => {
        const response = await api.delete(`/auth/sessions/${sessionId}`);
        return response.data;
    },
    
    revokeAllSessions: async () => {
        const response = await api.delete('/auth/sessions');
        return response.data;
    },

    revokeByToken: async (token) => {
        const response = await api.post('/auth/sessions/revoke-by-token', `token=${encodeURIComponent(token)}`, {
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
        });
        return response.data;
    },
    
    getRevokeConfirmTarget: async (token) => {
        const response = await api.get(`/auth/sessions/revoke-confirm?token=${encodeURIComponent(token)}`);
        return response.data;
    }
};
