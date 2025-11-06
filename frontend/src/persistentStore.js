import { writable } from 'svelte/store';

export function persistentStore(key, initialValue) {
  const storedValue = localStorage.getItem(key);
  const parsed = storedValue ? JSON.parse(storedValue) : initialValue;

  const store = writable(parsed);

  store.subscribe(value => {
    localStorage.setItem(key, JSON.stringify(value));
  });

  return store;
}
