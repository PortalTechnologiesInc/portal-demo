import { writable } from 'svelte/store';

export const connected = writable(false);
export const messages = writable([]);
export const errors = writable([]);

const apiUrl = __VITE_BACKEND_API_WS__;
console.log('apiUrl', apiUrl);
const ws = new WebSocket(apiUrl);

// Quando la connessione si apre
ws.onopen = () => {
    console.log('✅ WebSocket connected');
    connected.set(true);

    // every x seconds send a ping message to the server
    setInterval(() => {
        ws.send('PING');
    }, 10000);
};

// Quando ricevi un messaggio
ws.onmessage = (event) => {

    let data = event.data;
    if (data === 'PONG') {
        return;
    }

    console.log('🔴 Message received', event.data);

    let json = JSON.parse(data);
    if (json.type === 'error') {
        errors.update(prev => [...prev, json.message]);
        return;
    }

    messages.update(prev => [...prev, json]);

};

// Quando la connessione si chiude
ws.onclose = () => {
    console.log('❌ WebSocket disconnected');
    connected.set(false);
};

// Se vuoi gestire errori
ws.onerror = (err) => {
    console.error('⚠️ WebSocket error', err);
    connected.set(false);
};

export { ws };
