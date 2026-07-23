import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL, authForVu, clearAuthOnUnauthorized, csvEnv, loadOptions, requestParams, setupUsers, valueForVu } from '../lib/domain-test-common.js';

const contentIds = csvEnv('CONTENT_IDS');
export const options = loadOptions('content_detail');
export function setup() { return setupUsers('content-detail-load-user'); }

export default function (data) {
    const auth = authForVu(data);
    if (!auth) { sleep(1); return; }

    const contentId = valueForVu(contentIds);
    const detail = http.get(`${BASE_URL}/api/contents/${contentId}`, requestParams(auth, 'content_detail'));
    check(detail, { 'content detail success': (response) => response.status === 200 });
    clearAuthOnUnauthorized(detail);

    sleep(Number(__ENV.THINK_TIME || 1));
}
