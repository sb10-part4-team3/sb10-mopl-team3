import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const USER_COUNT = Number(__ENV.USER_COUNT || 20);
const PASSWORD = __ENV.TEST_USER_PASSWORD || 'LoadTest1!';

export const options = {
    setupTimeout: '2m',
    scenarios: {
        user_auth_smoke: {
            executor: 'ramping-vus',
            stages: [
                { duration: '30s', target: 5 },
                { duration: '1m', target: 5 },
                { duration: '30s', target: 0 },
            ],
            gracefulRampDown: '10s',
        },
    },
    thresholds: {
        http_req_failed: ['rate<0.05'],
        http_req_duration: ['p(95)<3000'],
        'http_req_duration{endpoint:signin}': ['p(95)<3000'],
        'http_req_duration{endpoint:user_profile}': ['p(95)<800'],
    },
};

function formEncode(data) {
    return Object.entries(data)
        .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(value)}`)
        .join('&');
}

function getCsrfToken() {
    const res = http.get(`${BASE_URL}/api/auth/csrf-token`, {
        tags: { endpoint: 'csrf' },
    });

    check(res, {
        'csrf token issued': (r) =>
            r.status === 204 && Boolean(r.cookies['XSRF-TOKEN']?.[0]?.value),
    });

    return res.cookies['XSRF-TOKEN']?.[0]?.value;
}

export function setup() {
    const users = [];

    for (let i = 1; i <= USER_COUNT; i += 1) {
        const email = `load-user-${i}@mopl.test`;
        const csrfToken = getCsrfToken();

        const res = http.post(
            `${BASE_URL}/api/users`,
            JSON.stringify({
                name: `load-user-${i}`,
                email,
                password: PASSWORD,
            }),
            {
                headers: {
                    'Content-Type': 'application/json',
                    'X-XSRF-TOKEN': csrfToken,
                },
                tags: { endpoint: 'signup_setup' },
            }
        );

        check(res, {
            'signup created or already exists': (r) => r.status === 201 || r.status === 409,
        });

        if (res.status !== 201 && res.status !== 409) {
            console.log(`signup failed for ${email}: status=${res.status}, body=${res.body}`);
            continue;
        }

        users.push({ email, password: PASSWORD });
    }

    if (users.length === 0) {
        throw new Error('setup did not create or find any smoke test users');
    }

    return { users };
}

export default function (data) {
    if (!data.users || data.users.length === 0) {
        throw new Error('no smoke test users available');
    }

    const user = data.users[(__VU - 1) % data.users.length];

    const signInCsrfToken = getCsrfToken();

    const signInRes = http.post(
        `${BASE_URL}/api/auth/sign-in`,
        formEncode({
            username: user.email,
            password: user.password,
        }),
        {
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
                'X-XSRF-TOKEN': signInCsrfToken,
            },
            tags: { endpoint: 'signin' },
        }
    );

    const signInOk = check(signInRes, {
        'signin success': (r) => r.status === 200,
        'access token exists': (r) => Boolean(r.json('accessToken')),
        'user id exists': (r) => Boolean(r.json('userDto.id')),
    });

    if (!signInOk) {
        return;
    }

    const accessToken = signInRes.json('accessToken');
    const userId = signInRes.json('userDto.id');

    const profileRes = http.get(`${BASE_URL}/api/users/${userId}`, {
        headers: {
            Authorization: `Bearer ${accessToken}`,
        },
        tags: { endpoint: 'user_profile' },
    });

    check(profileRes, {
        'profile success': (r) => r.status === 200,
    });

    const refreshCsrfToken = getCsrfToken();

    const refreshRes = http.post(`${BASE_URL}/api/auth/refresh`, null, {
        headers: {
            'X-XSRF-TOKEN': refreshCsrfToken,
        },
        tags: { endpoint: 'refresh' },
    });

    check(refreshRes, {
        'refresh success': (r) => r.status === 200,
        'new access token exists': (r) => Boolean(r.json('accessToken')),
    });

    const latestAccessToken = refreshRes.status === 200
        ? refreshRes.json('accessToken')
        : accessToken;

    const signOutCsrfToken = getCsrfToken();

    const signOutRes = http.post(`${BASE_URL}/api/auth/sign-out`, null, {
        headers: {
            Authorization: `Bearer ${latestAccessToken}`,
            'X-XSRF-TOKEN': signOutCsrfToken,
        },
        tags: { endpoint: 'signout' },
    });

    check(signOutRes, {
        'signout success': (r) => r.status === 204,
    });

    sleep(1);
}
