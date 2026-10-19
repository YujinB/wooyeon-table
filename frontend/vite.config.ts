import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// 개발 중에는 /api를 로컬 백엔드(8080)로 넘긴다. 배포에서는 nginx가 같은 일을 한다
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
});
