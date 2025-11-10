import { defineConfig } from 'vite'
import { svelte } from '@sveltejs/vite-plugin-svelte'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    svelte(),
  ],
  define: {
    __VITE_BACKEND_API_WS__: JSON.stringify(process.env.VITE_BACKEND_API_WS),
  }
})
