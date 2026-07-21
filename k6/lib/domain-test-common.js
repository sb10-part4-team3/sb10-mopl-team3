import http from 'k6/http';
import { check, fail } from 'k6';

export const BASE_URL = (__ENV.BASE_URL || 'http://localhost:8080').replace(/\/$/, '');
export const USER_COUNT = Number(__ENV.USER_COUNT || 60);
export const PASSWORD = __ENV.TEST_USER_PASSWORD || 'LoadTest1!';

export function loadOptions(domain) {
    const vus = Number(__ENV.VUS || 30);
    if (USER_COUNT < vus) {
        throw new Error(
            `USER_COUNT (${USER_COUNT}) must be greater than or equal to VUS (${vus}) `
            + 'so that each VU has a dedicated test account'
        );
    }
    return {
        setupTimeout: __ENV.SETUP_TIMEOUT || '5m',
        scenarios: {
            [`${domain}_load`]: {
                executor: 'ramping-vus',
                stages: [
                    { duration: __ENV.RAMP_UP || '1m', target: vus },
                    { duration: __ENV.DURATION || '3m', target: vus },
                    { duration: __ENV.RAMP_DOWN || '1m', target: 0 },
                ],
                gracefulRampDown: '20s',
            },
        },
        thresholds: {
            http_req_failed: [__ENV.ERROR_THRESHOLD || 'rate<0.05'],
            http_req_duration: [__ENV.LATENCY_THRESHOLD || 'p(95)<1500'],
            checks: ['rate>0.95'],
        },
        tags: { domain },
    };
}

export function csvEnv(name, required = true) {
    const values = (__ENV[name] || '').split(',').map((value) => value.trim()).filter(Boolean);
    if (required && values.length === 0) {
        throw new Error(`${name} must contain at least one value`);
    }
    return values;
}

export function valueForVu(values) {
    return values[(__VU - 1) % values.length];
}

function formEncode(data) {
    return Object.entries(data)
        .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(value)}`)
        .join('&');
}

function csrfToken() {
    const response = http.get(`${BASE_URL}/api/auth/csrf-token`, {
        tags: { endpoint: 'csrf' },
    });
    const cookies = response.cookies['XSRF-TOKEN'];
    const token = cookies && cookies[0] && cookies[0].value;
    check(response, {
        'csrf token issued': (result) => result.status === 204 && Boolean(token),
    });
    return token;
}

export function setupUsers(prefix) {
    const users = [];
    for (let index = 1; index <= USER_COUNT; index += 1) {
        const email = `${prefix}-${index}@mopl.test`;
        const token = csrfToken();
        const response = http.post(
            `${BASE_URL}/api/users`,
            JSON.stringify({ name: `${prefix}-${index}`, email, password: PASSWORD }),
            {
                headers: {
                    'Content-Type': 'application/json',
                    'X-XSRF-TOKEN': token,
                    Cookie: `XSRF-TOKEN=${token}`,
                },
                tags: { endpoint: 'signup_setup' },
                responseCallback: http.expectedStatuses(201, 409),
            }
        );
        check(response, {
            'test user ready': (result) => result.status === 201 || result.status === 409,
        });
        if (response.status === 201 || response.status === 409) {
            users.push({ email, password: PASSWORD });
        }
    }
    const vus = Number(__ENV.VUS || 30);
    if (users.length < vus) {
        fail(
            `Only ${users.length} load-test users are available for ${vus} VUs; `
            + 'each VU requires a dedicated account'
        );
    }
    return { users };
}

export function authForVu(data) {
    if (globalThis.domainAuth) return globalThis.domainAuth;
    const user = data.users[(__VU - 1) % data.users.length];
    const token = csrfToken();
    const response = http.post(
        `${BASE_URL}/api/auth/sign-in`,
        formEncode({ username: user.email, password: user.password }),
        {
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
                'X-XSRF-TOKEN': token,
                Cookie: `XSRF-TOKEN=${token}`,
            },
            tags: { endpoint: 'signin' },
        }
    );
    const signinSucceeded = check(response, {
        'signin success': (result) => result.status === 200,
    });
    if (!signinSucceeded) return null;

    let accessToken = null;
    try {
        accessToken = response.json('accessToken');
    } catch (error) {
        accessToken = null;
    }
    const tokenExists = check(response, {
        'access token exists': () => Boolean(accessToken),
    });
    if (!tokenExists) return null;

    globalThis.domainAuth = { accessToken, csrfToken: token };
    return globalThis.domainAuth;
}

export function clearAuthOnUnauthorized(response) {
    if (response.status !== 401) return false;
    globalThis.domainAuth = null;
    return true;
}

export function requestParams(auth, endpoint, json = false) {
    const headers = {
        Authorization: `Bearer ${auth.accessToken}`,
        'X-XSRF-TOKEN': auth.csrfToken,
        Cookie: `XSRF-TOKEN=${auth.csrfToken}`,
    };
    if (json) headers['Content-Type'] = 'application/json';
    return { headers, tags: { endpoint } };
}
