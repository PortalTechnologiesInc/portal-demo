import { writable } from 'svelte/store';

import { persistentStore } from './persistentStore.js';

export const loggedIn = persistentStore('loggedIn', false);
export const profile = persistentStore('state', null);
export const sessionToken = persistentStore('sessionToken', null);
export const pubkey = persistentStore('pubkey', null);