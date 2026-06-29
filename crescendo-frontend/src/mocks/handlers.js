import { http, HttpResponse } from 'msw';

export const handlers = [
  http.get('http://localhost:8080/auth/login', () => {
    return HttpResponse.json({ accessToken: 'mock-token' });
  }),
  // More handlers can be added here
];
