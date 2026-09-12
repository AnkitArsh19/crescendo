import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    host: true, // Listen on all network interfaces (needed for tunnels)
    port: 5173,
    strictPort: true,
    allowedHosts: ['app.crescendo.run'] // Allow access from your custom domain
  },
  // ─── Prevent the "Invalid hook call / two React copies" error ────────────────
  // Root cause: @xyflow/react bundles its own zustand@4.5.7 alongside our
  // zustand@5.0.14. Without dedup, Vite optimises them in separate passes,
  // giving each a different browserHash (?v=...). Since both carry their own
  // copy of React, react-dom initialises hooks on one copy while zustand reads
  // from the other — producing the "Cannot read null (reading 'useCallback')"
  // crash at App.jsx:99.
  //
  // resolve.dedupe forces ALL imports of these packages (regardless of which
  // nested node_modules folder they live in) to the single top-level copy.
  // optimizeDeps.include forces Vite to bundle them all in ONE pre-build pass,
  // so every file carries the same browserHash and the same React instance.
  resolve: {
    dedupe: ['react', 'react-dom', 'zustand']
  },
  optimizeDeps: {
    include: [
      'react',
      'react-dom',
      'react-dom/client',
      'react/jsx-runtime',
      'react/jsx-dev-runtime',
      'zustand',
      '@xyflow/react'
    ]
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/setupTests.js'],
    globals: true,
    exclude: ['**/node_modules/**', '**/dist/**', 'tests/*.spec.js']
  }
})
