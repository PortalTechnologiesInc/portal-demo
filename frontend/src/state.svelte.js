import { writable } from 'svelte/store';

export const count = writable(0);

count.subscribe((value) => {
	console.log(value);
}); // logs '0'

count.set(1); // logs '1'

count.update((n) => n + 1); // logs '2'



export const loggedIn = writable(false);
export const profile = writable(null);