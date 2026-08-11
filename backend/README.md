# Company API

Spring Boot 4.1.0, Java 21, PostgreSQL 기반 기업 CRUD API입니다.

## 환경 변수

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`

## 검증

```bash
./gradlew test
docker build -t jobsite-company-api:test .
```

API base path는 `/api/companies`, health endpoint는 `/actuator/health`입니다.
