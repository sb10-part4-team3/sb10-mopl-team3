import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL, authForVu, clearAuthOnUnauthorized, loadOptions, requestParams } from '../lib/domain-test-common.js';

const PASSWORD = __ENV.TEST_USER_PASSWORD || 'LoadTest1!';
const USER_COUNT = Number(__ENV.USER_COUNT || 10);
const VUS = Number(__ENV.VUS || 10);
const ADMIN_EMAIL = __ENV.ADMIN_EMAIL;
const ADMIN_PASSWORD = __ENV.ADMIN_PASSWORD;

export const options = loadOptions('content_admin_crud');

function formEncode(data) {
    return Object.entries(data)
        .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(value)}`)
        .join('&');
}

function csrfToken() {
    const response = http.get(`${BASE_URL}/api/auth/csrf-token`, { tags: { endpoint: 'csrf' } });
    const cookies = response.cookies['XSRF-TOKEN'];
    const token = cookies && cookies[0] && cookies[0].value;
    check(response, { 'csrf token issued': (result) => result.status === 204 && Boolean(token) });
    return token;
}

// Each test VU needs its own dedicated ADMIN account (not the shared bootstrap admin):
// signing in revokes any other active session for that user, so N concurrent VUs
// sharing one admin login would keep logging each other out.
export function setup() {
    if (USER_COUNT < VUS) {
        throw new Error(`USER_COUNT (${USER_COUNT}) must be >= VUS (${VUS}) so each VU has a dedicated admin account`);
    }
    if (!ADMIN_EMAIL || !ADMIN_PASSWORD) {
        throw new Error('ADMIN_EMAIL and ADMIN_PASSWORD env vars are required to grant admin role to load-test accounts');
    }

    const bootstrapToken = csrfToken();
    const bootstrapSignin = http.post(
        `${BASE_URL}/api/auth/sign-in`,
        formEncode({ username: ADMIN_EMAIL, password: ADMIN_PASSWORD }),
        {
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
                'X-XSRF-TOKEN': bootstrapToken,
                Cookie: `XSRF-TOKEN=${bootstrapToken}`,
            },
            tags: { endpoint: 'bootstrap_admin_signin' },
        }
    );
    check(bootstrapSignin, { 'bootstrap admin signin success': (r) => r.status === 200 });
    const bootstrapAccessToken = bootstrapSignin.json('accessToken');

    const users = [];
    for (let i = 1; i <= USER_COUNT; i += 1) {
        const email = `content-admin-load-user-${i}@mopl.test`;
        const signupToken = csrfToken();
        const signupRes = http.post(
            `${BASE_URL}/api/users`,
            JSON.stringify({ name: `content-admin-load-user-${i}`, email, password: PASSWORD }),
            {
                headers: {
                    'Content-Type': 'application/json',
                    'X-XSRF-TOKEN': signupToken,
                    Cookie: `XSRF-TOKEN=${signupToken}`,
                },
                tags: { endpoint: 'signup_setup' },
                responseCallback: http.expectedStatuses(201, 409),
            }
        );
        check(signupRes, { 'test user ready': (r) => r.status === 201 || r.status === 409 });

        if (signupRes.status === 201) {
            const userId = signupRes.json('id');
            const roleToken = csrfToken();
            const roleRes = http.patch(
                `${BASE_URL}/api/users/${userId}/role`,
                JSON.stringify({ role: 'ADMIN' }),
                {
                    headers: {
                        Authorization: `Bearer ${bootstrapAccessToken}`,
                        'Content-Type': 'application/json',
                        'X-XSRF-TOKEN': roleToken,
                        Cookie: `XSRF-TOKEN=${roleToken}`,
                    },
                    tags: { endpoint: 'grant_admin_setup' },
                }
            );
            check(roleRes, { 'granted admin role': (r) => r.status === 200 });
        }
        users.push({ email, password: PASSWORD });
    }

    if (users.length < VUS) {
        throw new Error(`Only ${users.length} admin load-test accounts are ready for ${VUS} VUs`);
    }
    return { users };
}

export default function (data) {
    const auth = authForVu(data);
    if (!auth) { sleep(1); return; }

    const createBody = JSON.stringify({
        type: 'movie',
        title: `k6 admin content ${__VU}-${__ITER}`,
        description: 'k6 admin crud load test',
        tags: ['k6'],
    });
    const created = http.post(
        `${BASE_URL}/api/contents`,
        { request: http.file(createBody, 'request.json', 'application/json') },
        requestParams(auth, 'content_create')
    );
    if (clearAuthOnUnauthorized(created)) { sleep(1); return; }
    if (!check(created, { 'content create success': (response) => response.status === 201 })) { sleep(1); return; }
    const contentId = created.json('id');

    const updateBody = JSON.stringify({
        type: 'movie',
        title: `k6 admin content updated ${__VU}-${__ITER}`,
        description: 'k6 admin crud load test (updated)',
        tags: ['k6'],
    });
    const updated = http.patch(
        `${BASE_URL}/api/contents/${contentId}`,
        { request: http.file(updateBody, 'request.json', 'application/json') },
        requestParams(auth, 'content_update')
    );
    clearAuthOnUnauthorized(updated);
    check(updated, { 'content update success': (response) => response.status === 200 });

    const removed = http.del(
        `${BASE_URL}/api/contents/${contentId}`,
        null,
        requestParams(auth, 'content_delete')
    );
    clearAuthOnUnauthorized(removed);
    check(removed, { 'content delete success': (response) => response.status === 200 });

    sleep(Number(__ENV.THINK_TIME || 1));
}
