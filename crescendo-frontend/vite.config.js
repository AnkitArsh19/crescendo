import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    host: true, // Listen on all network interfaces (needed for tunnels)
    allowedHosts: ['app.crescendo.run'] // Allow access from your custom domain
  }
})
