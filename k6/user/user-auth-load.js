import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const USER_COUNT = Number(__ENV.USER_COUNT || 150);
const PASSWORD = __ENV.TEST_USER_PASSWORD || 'LoadTest1!';

export const options = {
    scenarios: {
        user_auth_realistic: {
            executor: 'ramping-vus',
            stages: [
                { duration: '1m', target: 20 },
                { duration: '2m', target: 20 },
                { duration: '1m', target: 50 },
                { duration: '3m', target: 50 },
                { duration: '1m', target: 0 },
            ],
            gracefulRampDown: '20s',
        },
    },
    thresholds: {
        http_req_failed: ['rate<0.05'],
        http_req_duration: ['p(95)<1500'],
        'http_req_duration{endpoint:signin_once}': ['p(95)<2000'],
        'http_req_duration{endpoint:user_profile}': ['p(95)<1000'],
        'http_req_duration{endpoint:refresh}': ['p(95)<1500'],
    },
};

function formEncode(data) {
    return Object.entries(data)
        .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(value)}`)
        .join('&');
}

function getCookieValue(response, name) {
    return response.cookies[name]?.[0]?.value;
}

function getCsrfToken() {
    const res = http.get(`${BASE_URL}/api/auth/csrf-token`, {
        tags: { endpoint: 'csrf' },
    });

    const token = getCookieValue(res, 'XSRF-TOKEN');

    check(res, {
        'csrf token issued': (r) => r.status === 204 && Boolean(token),
    });

    return token;
}

function signUpUser(email, password, index) {
    const csrfToken = getCsrfToken();

    const res = http.post(
        `${BASE_URL}/api/users`,
        JSON.stringify({
            name: `realistic-user-${index}`,
            email,
            password,
        }),
        {
            headers: {
                'Content-Type': 'application/json',
                'X-XSRF-TOKEN': csrfToken,
                Cookie: `XSRF-TOKEN=${csrfToken}`,
            },
            tags: { endpoint: 'signup_setup' },
        }
    );

    check(res, {
        'signup created or already exists': (r) => r.status === 201 || r.status === 409,
    });
}

function signin(user) {
    const csrfToken = getCsrfToken();

    const res = http.post(
        `${BASE_URL}/api/auth/sign-in`,
        formEncode({
            username: user.email,
            password: user.password,
        }),
        {
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
                'X-XSRF-TOKEN': csrfToken,
                Cookie: `XSRF-TOKEN=${csrfToken}`,
            },
            tags: { endpoint: 'signin_once' },
        }
    );

    const ok = check(res, {
        'signin success': (r) => r.status === 200,
        'access token exists': (r) => r.status === 200 && Boolean(r.json('accessToken')),
        'user id exists': (r) => r.status === 200 && Boolean(r.json('userDto.id')),
    });

    if (!ok) {
        return null;
    }

    return {
        accessToken: res.json('accessToken'),
        userId: res.json('userDto.id'),
        refreshToken: getCookieValue(res, 'REFRESH_TOKEN'),
    };
}

function refreshToken(authState) {
    const csrfToken = getCsrfToken();

    const cookieHeader = [
        `XSRF-TOKEN=${csrfToken}`,
        authState.refreshToken ? `REFRESH_TOKEN=${authState.refreshToken}` : null,
    ].filter(Boolean).join('; ');

    const res = http.post(`${BASE_URL}/api/auth/refresh`, null, {
        headers: {
            'X-XSRF-TOKEN': csrfToken,
            Cookie: cookieHeader,
        },
        tags: { endpoint: 'refresh' },
    });

    check(res, {
        'refresh success': (r) => r.status === 200,
        'new access token exists': (r) => r.status === 200 && Boolean(r.json('accessToken')),
    });

    if (res.status !== 200) {
        if (__VU === 1) {
            console.log(`refresh failed: status=${res.status}, body=${res.body}`);
        }
        return null;
    }

    return {
        accessToken: res.json('accessToken'),
        refreshToken: getCookieValue(res, 'REFRESH_TOKEN') || authState.refreshToken,
    };
}

export function setup() {
    const users = [];

    for (let i = 1; i <= USER_COUNT; i += 1) {
        const email = `realistic-user-${i}@mopl.test`;

        signUpUser(email, PASSWORD, i);

        users.push({
            email,
            password: PASSWORD,
        });
    }

    return { users };
}

export default function (data) {
    const user = data.users[(__VU - 1) % data.users.length];

    if (!globalThis.authState) {
        globalThis.authState = signin(user);
    }

    if (!globalThis.authState) {
        sleep(1);
        return;
    }

    const profileRes = http.get(`${BASE_URL}/api/users/${globalThis.authState.userId}`, {
        headers: {
            Authorization: `Bearer ${globalThis.authState.accessToken}`,
        },
        tags: { endpoint: 'user_profile' },
    });

    check(profileRes, {
        'profile success': (r) => r.status === 200,
    });

    if (__ITER > 0 && __ITER % 10 === 0) {
        const refreshed = refreshToken(globalThis.authState);

        if (refreshed) {
            globalThis.authState.accessToken = refreshed.accessToken;
            globalThis.authState.refreshToken = refreshed.refreshToken;
        }
    }

    sleep(1);
}