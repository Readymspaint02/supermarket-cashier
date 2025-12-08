# 本地 HTTPS 证书说明

此目录用于存放本地开发的 https 证书：

- `localhost-key.pem`
- `localhost.pem`

推荐使用 [mkcert](https://github.com/FiloSottile/mkcert) 生成证书：

```bash
mkcert -install
mkcert localhost
```

如果你希望使用自定义路径，可在启动前设置环境变量：

- `VITE_DEV_SSL_KEY`：私钥文件路径
- `VITE_DEV_SSL_CERT`：证书文件路径
- `VITE_DEV_HTTPS=false`：临时禁用 https（恢复 http）

将生成的文件复制到本目录后，重新执行 `npm run dev`，即可通过 `https://localhost:5173` 访问并启用摄像头。
