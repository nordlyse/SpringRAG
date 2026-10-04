import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

const api = process.env.SPRING_RAG_API || 'http://localhost:8080'

export default defineConfig({
  plugins: [react()],
  server: {
    host: '0.0.0.0',
    port: 5173,
    proxy: {
      '/documents': api,
      '/chat': {
        target: api,
        configure(proxy) {
          proxy.on('proxyRes', (_proxyRes, _req, res) => {
            res.setHeader('Cache-Control', 'no-cache')
            res.setHeader('X-Accel-Buffering', 'no')
          })
        },
      },
    },
    watch: {
      usePolling: process.env.CHOKIDAR_USEPOLLING === 'true',
    },
  },
})
