import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL, authForVu, csvEnv, loadOptions, requestParams, setupUsers, valueForVu } from '../lib/domain-test-common.js';

const followeeIds = csvEnv('FOLLOWEE_IDS');
const mutations = __ENV.MUTATIONS === 'true';
export const options = loadOptions('follow');
export function setup() { return setupUsers('follow-load-user'); }

export default function (data) {
    const auth = authForVu(data);
    if (!auth) { sleep(1); return; }
    const followeeId = valueForVu(followeeIds);
    const count = http.get(`${BASE_URL}/api/follows/count?followeeId=${followeeId}`, requestParams(auth, 'follow_count'));
    check(count, { 'follower count success': (response) => response.status === 200 });
    const statusParams = requestParams(auth, 'follow_status');
    statusParams.responseCallback = http.expectedStatuses(200, 404);
    const status = http.get(`${BASE_URL}/api/follows/followed-by-me?followeeId=${followeeId}`, statusParams);
    check(status, { 'follow status success': (response) => response.status === 200 || response.status === 404 });

    if (mutations) {
        const created = http.post(
            `${BASE_URL}/api/follows`,
            JSON.stringify({ followeeId }),
            requestParams(auth, 'follow_create', true)
        );
        if (check(created, { 'follow create success': (response) => response.status === 200 || response.status === 201 })) {
            const removed = http.del(
                `${BASE_URL}/api/follows/${created.json('id')}`,
                null,
                requestParams(auth, 'follow_delete')
            );
            check(removed, { 'follow delete success': (response) => response.status === 204 });
        }
    }
    sleep(Number(__ENV.THINK_TIME || 1));
}
