import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'
import path from 'path'
import fs from 'fs'

/**
 * 尝试加载本地 https 证书，若不存在则自动回落为 http。
 * 证书路径可通过环境变量 VITE_DEV_SSL_KEY / VITE_DEV_SSL_CERT 指定，
 * 默认读取项目内 certs/localhost-key.pem 与 certs/localhost.pem。
 */
const resolveCertFile = (customPath) => {
  if (!customPath) {
    return null
  }
  const absolutePath = path.isAbsolute(customPath)
    ? customPath
    : path.resolve(__dirname, customPath)
  return fs.existsSync(absolutePath) ? absolutePath : null
}

const loadHttpsOptions = () => {
  if (process.env.VITE_DEV_HTTPS === 'false') {
    return false
  }

  const keyPath =
    resolveCertFile(process.env.VITE_DEV_SSL_KEY) ??
    resolveCertFile('./certs/localhost-key.pem')
  const certPath =
    resolveCertFile(process.env.VITE_DEV_SSL_CERT) ??
    resolveCertFile('./certs/localhost.pem')

  if (keyPath && certPath) {
    return {
      key: fs.readFileSync(keyPath),
      cert: fs.readFileSync(certPath),
    }
  }
  return false
}

const httpsOptions = loadHttpsOptions()

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue(), tailwindcss()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    host: '0.0.0.0',
    port: 5173,
    https: httpsOptions || undefined,
    proxy: {
      // API接口代理
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        //rewrite: (path) => path.replace(/^\/api/, '/api'),
      },
      // 静态资源代理（上传的图片）
      '/uploads': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/uploads/, '/api/uploads'),
      },
    },
  },
})
