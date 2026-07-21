import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL, authForVu, clearAuthOnUnauthorized, csvEnv, loadOptions, requestParams, setupUsers, valueForVu } from '../lib/domain-test-common.js';

const contentIds = csvEnv('CONTENT_IDS');
const mutations = __ENV.MUTATIONS === 'true';
export const options = loadOptions('review');
export function setup() { return setupUsers('review-load-user'); }

export default function (data) {
    const auth = authForVu(data);
    if (!auth) { sleep(1); return; }
    const contentId = valueForVu(contentIds);
    const list = http.get(
        `${BASE_URL}/api/reviews?contentId=${contentId}&limit=20&sortDirection=DESCENDING&sortBy=createdAt`,
        requestParams(auth, 'review_list')
    );
    check(list, { 'review list success': (response) => response.status === 200 });
    if (clearAuthOnUnauthorized(list)) { sleep(1); return; }

    if (mutations) {
        const created = http.post(
            `${BASE_URL}/api/reviews`,
            JSON.stringify({ contentId, text: `k6 review ${__VU}-${__ITER}`, rating: 4.0 }),
            requestParams(auth, 'review_create', true)
        );
        if (clearAuthOnUnauthorized(created)) { sleep(1); return; }
        if (check(created, { 'review create success': (response) => response.status === 201 })) {
            const removed = http.del(
                `${BASE_URL}/api/reviews/${created.json('id')}`,
                null,
                requestParams(auth, 'review_delete')
            );
            clearAuthOnUnauthorized(removed);
            check(removed, { 'review delete success': (response) => response.status === 200 });
        }
    }
    sleep(Number(__ENV.THINK_TIME || 1));
}
