import "franken-ui/js/core.iife";
import "franken-ui/js/icon.iife";

import { mount } from 'svelte'
import './app.css'
import App from './App.svelte'

const app = mount(App, {
  target: document.getElementById('app'),
})

export default app
