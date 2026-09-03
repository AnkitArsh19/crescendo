import http from 'k6/http';
import { check } from 'k6';

export const options = {
    vus: 50,
    duration: '10s',
};

const BASE_URL = __ENV.TARGET_URL || 'http://host.docker.internal:8000';

export default function () {
    const res = http.get(`${BASE_URL}/`);
    check(res, {
        'status is 200': (r) => r.status === 200,
        'has correct service': (r) => r.json('service') === 'crescendo-aiml',
    });
}
