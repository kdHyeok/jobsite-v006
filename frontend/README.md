# JobSight Vue Frontend

Vue 3.5, Vite 8, TypeScript 기반 기업 정보 CRUD UI입니다.

```bash
npm ci
npm run type-check
npm test
npm run build
docker build -t jobsite-company-frontend:test .
```

운영 image는 Nginx non-root user로 8080에서 실행하며 `/api/` 요청을 `backend:8080`으로 전달합니다.
