import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL, authForVu, csvEnv, loadOptions, requestParams, setupUsers, valueForVu } from '../lib/domain-test-common.js';

const playlistIds = csvEnv('PLAYLIST_IDS', false);
const mutations = __ENV.MUTATIONS === 'true';
export const options = loadOptions('playlist');
export function setup() { return setupUsers('playlist-load-user'); }

export default function (data) {
    const auth = authForVu(data);
    if (!auth) { sleep(1); return; }
    const list = http.get(
        `${BASE_URL}/api/playlists?limit=20&sortDirection=DESCENDING&sortBy=updatedAt`,
        requestParams(auth, 'playlist_list')
    );
    check(list, { 'playlist list success': (response) => response.status === 200 });

    if (playlistIds.length > 0) {
        const detail = http.get(
            `${BASE_URL}/api/playlists/${valueForVu(playlistIds)}`,
            requestParams(auth, 'playlist_detail')
        );
        check(detail, { 'playlist detail success': (response) => response.status === 200 });
    }

    if (mutations) {
        const created = http.post(
            `${BASE_URL}/api/playlists`,
            JSON.stringify({ title: `k6 playlist ${__VU}-${__ITER}`, description: 'k6 load test' }),
            requestParams(auth, 'playlist_create', true)
        );
        if (check(created, { 'playlist create success': (response) => response.status === 201 })) {
            const playlistId = created.json('id');
            const updated = http.patch(
                `${BASE_URL}/api/playlists/${playlistId}`,
                JSON.stringify({ title: `k6 updated ${__VU}-${__ITER}`, description: 'k6 load test updated' }),
                requestParams(auth, 'playlist_update', true)
            );
            check(updated, { 'playlist update success': (response) => response.status === 200 });
            const removed = http.del(`${BASE_URL}/api/playlists/${playlistId}`, null, requestParams(auth, 'playlist_delete'));
            check(removed, { 'playlist delete success': (response) => response.status === 200 });
        }
    }
    sleep(Number(__ENV.THINK_TIME || 1));
}

