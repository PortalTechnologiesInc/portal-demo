import { writable } from 'svelte/store';
import { daemonVersion } from './state.svelte.js';

export const connected = writable(false);
export const messages = writable([]);
export const errors = writable([]);


const apiUrl = __VITE_BACKEND_API_WS__;
console.log('apiUrl', apiUrl);
let ws;
function connect() {
   ws = new WebSocket(apiUrl);

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

        if (json.cmd === 'DaemonVersion') {
            daemonVersion.set({ version: json.version, git_commit: json.git_commit });
            return;
        }

        messages.update(prev => [...prev, json]);

    };

    // Quando la connessione si chiude
    ws.onclose = () => {
        console.log('❌ WebSocket disconnected');
        connected.set(false);

        let interval;
        interval = setInterval(() => {
            clearInterval(interval);
            connect();

        }, 1000 * 5);
    };

    // Se vuoi gestire errori
    ws.onerror = (err) => {
        console.error('⚠️ WebSocket error', err);
        connected.set(false);
    };

}

function sendWsMessage(message) {
    ws.send(message);
}


connect();

export { sendWsMessage };