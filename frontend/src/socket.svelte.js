import { writable } from 'svelte/store';

const ws = new WebSocket('ws://localhost:3030');

if (ws.readyState === WebSocket.OPEN) {
    console.log('WebSocket is open');
} else {
    console.log('WebSocket is not open');
}

ws.onmessage = function(event) {
  messages.update(prev => [...prev, event.data]);
};

let connected = ws.readyState === WebSocket.OPEN;

export {
    connected
}
