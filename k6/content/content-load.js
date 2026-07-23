import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL, authForVu, clearAuthOnUnauthorized, loadOptions, requestParams, setupUsers } from '../lib/domain-test-common.js';

const scenarios = [
    { typeEqual: 'movie', keywordLike: '', tagsIn: [] },
    { typeEqual: 'tvSeries', keywordLike: '', tagsIn: [] },
    { typeEqual: 'sport', keywordLike: '', tagsIn: [] },
    { typeEqual: '', keywordLike: '오', tagsIn: [] },
    { typeEqual: '', keywordLike: '', tagsIn: ['액션'] },
    { typeEqual: '', keywordLike: '', tagsIn: ['드라마'] },
    { typeEqual: '', keywordLike: '', tagsIn: [] },
];

export const options = loadOptions('content');
export function setup() { return setupUsers('content-load-user'); }

function buildQuery(scenario) {
    const parts = [];
    if (scenario.typeEqual) parts.push(`typeEqual=${encodeURIComponent(scenario.typeEqual)}`);
    if (scenario.keywordLike) parts.push(`keywordLike=${encodeURIComponent(scenario.keywordLike)}`);
    scenario.tagsIn.forEach((tag) => parts.push(`tagsIn=${encodeURIComponent(tag)}`));
    parts.push('limit=20');
    parts.push('sortBy=createdAt');
    parts.push('sortDirection=DESCENDING');
    return parts.join('&');
}

export default function (data) {
    const auth = authForVu(data);
    if (!auth) { sleep(1); return; }

    const scenario = scenarios[(__VU - 1) % scenarios.length];
    const list = http.get(
        `${BASE_URL}/api/contents?${buildQuery(scenario)}`,
        requestParams(auth, 'content_list')
    );
    check(list, { 'content list success': (response) => response.status === 200 });
    clearAuthOnUnauthorized(list);

    sleep(Number(__ENV.THINK_TIME || 1));
}
