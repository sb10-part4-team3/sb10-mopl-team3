import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const USER_COUNT = Number(__ENV.USER_COUNT || 150);
const PASSWORD = __ENV.TEST_USER_PASSWORD || 'LoadTest1!';
const ADMIN_EMAIL = __ENV.LOAD_TEST_ADMIN_EMAIL || __ENV.ADMIN_EMAIL || 'admin@mopl.com';
const ADMIN_PASSWORD = __ENV.LOAD_TEST_ADMIN_PASSWORD || __ENV.ADMIN_PASSWORD;
const MAX_VUS = Number(__ENV.MAX_VUS || 50);
const WARM_VUS = Math.max(1, Math.floor(MAX_VUS * 0.4));

http.setResponseCallback(http.expectedStatuses({ min: 200, max: 399 }));

const signupExpectedStatuses = http.expectedStatuses(201, 409);

export const options = {
    setupTimeout: '5m',
    scenarios: {
        user_admin_search_load: {
            executor: 'ramping-vus',
            stages: [
                { duration: '1m', target: WARM_VUS },
                { duration: '2m', target: WARM_VUS },
                { duration: '1m', target: MAX_VUS },
                { duration: '3m', target: MAX_VUS },
                { duration: '1m', target: 0 },
            ],
            gracefulRampDown: '20s',
        },
    },
    thresholds: {
        http_req_failed: ['rate<0.01'],
        http_req_duration: ['p(95)<1500'],
        'http_req_duration{endpoint:admin_users_list}': ['p(95)<1000'],
        'http_req_duration{endpoint:admin_users_search}': ['p(95)<1200'],
        'http_req_duration{endpoint:admin_users_filter}': ['p(95)<1200'],
    },
};

function encodeParams(data) {
    return Object.entries(data)
        .filter(([, value]) => value !== undefined && value !== null && value !== '')
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

function signUpUser(index) {
    const csrfToken = getCsrfToken();
    const email = `admin-search-user-${index}@mopl.test`;
    const name = index % 2 === 0
        ? `검색사용자${index}`
        : `admin-search-user-${index}`;

    const res = http.post(
        `${BASE_URL}/api/users`,
        JSON.stringify({
            name,
            email,
            password: PASSWORD,
        }),
        {
            headers: {
                'Content-Type': 'application/json',
                'X-XSRF-TOKEN': csrfToken,
                Cookie: `XSRF-TOKEN=${csrfToken}`,
            },
            tags: { endpoint: 'signup_setup' },
            responseCallback: signupExpectedStatuses,
        }
    );

    const ok = check(res, {
        'signup created or already exists': (r) => r.status === 201 || r.status === 409,
    });

    if (!ok) {
        console.log(`signup failed: email=${email}, status=${res.status}`);
    }

    return { email, ok };
}

function signinAdmin() {
    const csrfToken = getCsrfToken();

    const res = http.post(
        `${BASE_URL}/api/auth/sign-in`,
        encodeParams({
            username: ADMIN_EMAIL,
            password: ADMIN_PASSWORD,
        }),
        {
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
                'X-XSRF-TOKEN': csrfToken,
                Cookie: `XSRF-TOKEN=${csrfToken}`,
            },
            tags: { endpoint: 'signin_admin' },
        }
    );

    const ok = check(res, {
        'admin signin success': (r) => r.status === 200,
        'admin access token exists': (r) => r.status === 200 && Boolean(r.json('accessToken')),
    });

    if (!ok) {
        console.log(`admin signin failed: email=${ADMIN_EMAIL}, status=${res.status}`);
        return null;
    }

    return res.json('accessToken');
}

function getUsers(accessToken, params, endpoint) {
    const query = encodeParams(params);
    const url = query ? `${BASE_URL}/api/users?${query}` : `${BASE_URL}/api/users`;

    const res = http.get(url, {
        headers: {
            Authorization: `Bearer ${accessToken}`,
        },
        tags: { endpoint },
    });

    check(res, {
        [`${endpoint} success`]: (r) => r.status === 200,
        [`${endpoint} has data array`]: (r) => r.status === 200 && Array.isArray(r.json('data')),
        [`${endpoint} has total count`]: (r) => r.status === 200 && Number(r.json('totalCount')) >= 0,
    });

    return res;
}

export function setup() {
    if (!ADMIN_PASSWORD) {
        throw new Error('Set LOAD_TEST_ADMIN_PASSWORD or ADMIN_PASSWORD before running this script');
    }

    const seedResults = [];

    for (let i = 1; i <= USER_COUNT; i += 1) {
        seedResults.push(signUpUser(i));
    }

    const failedSeedResults = seedResults.filter((result) => !result.ok);
    if (failedSeedResults.length > 0) {
        const failedEmails = failedSeedResults
            .slice(0, 5)
            .map((result) => result.email)
            .join(', ');
        throw new Error(`seed user setup failed: count=${failedSeedResults.length}, emails=${failedEmails}`);
    }

    const adminAccessToken = signinAdmin();

    if (!adminAccessToken) {
        throw new Error('admin signin failed during setup');
    }

    return {
        adminAccessToken,
        keywords: ['admin-search-user', '검색사용자', 'mopl.test', 'user-1'],
    };
}

export default function (data) {
    const keyword = data.keywords[__ITER % data.keywords.length];
    const scenario = __ITER % 3;

    if (scenario === 0) {
        getUsers(
            data.adminAccessToken,
            {
                limit: 20,
                sortBy: 'createdAt',
                sortDirection: 'DESCENDING',
            },
            'admin_users_list'
        );
    } else if (scenario === 1) {
        getUsers(
            data.adminAccessToken,
            {
                emailLike: keyword,
                limit: 20,
                sortBy: 'name',
                sortDirection: 'ASCENDING',
            },
            'admin_users_search'
        );
    } else {
        getUsers(
            data.adminAccessToken,
            {
                roleEqual: 'USER',
                isLocked: false,
                limit: 50,
                sortBy: 'email',
                sortDirection: 'ASCENDING',
            },
            'admin_users_filter'
        );
    }

    sleep(1);
}
