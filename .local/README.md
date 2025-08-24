# Solo E2E (내 PC 전용) 초간단 가이드

**목표:** Config/Eureka 없이 `Front → Gateway → NewsService → FlaskAPI` 를 **항상 MySQL**로 실행 (스위치 없음).  
**원칙:** 공통은 스냅샷 재사용, DB/비밀은 **내 로컬 시크릿 파일**로만 관리.

---

## 0) 필수 파일 (모두 `<repo>/.local/config-files/` 안)
- `application-solo.yml` — Config/Discovery 끄기
- `gateway-service-solo.yml` — 정적 라우트(`/api/news/** → http://localhost:8082`)
- `news-service-solo.yml` — 포트/Flask URL만 (DB 설정 없음)
- *(선택)* `user-service-solo.yml` — 포트만 (DB 설정 없음)
- **`news-service-secret.yml` / `user-service-secret.yml`** — **MySQL 연결값**(URL/USER/PASS, `ddl-auto=validate`)

> 예) `news-service-secret.yml`
> ```yaml
> spring:
>   datasource:
>     url: jdbc:mysql://localhost:3306/news?serverTimezone=Asia/Seoul&useUnicode=true&characterEncoding=UTF-8
>     username: devuser
>     password: devpass
>   jpa.hibernate.ddl-auto: validate
> ```

---

## 1) 실행 (IntelliJ Spring Boot Run 권장)
**공통 설정**
- **Active profiles**: `solo,secret`
- **Program arguments**:
  ```
  --spring.config.additional-location=optional:file:$PROJECT_DIR$/.local/config-files/
  ```
- **Working directory**: `$PROJECT_DIR$`
- **Environment variables**: 비움 (예전 `SPRING_CONFIG_IMPORT`/`EUREKA_*` 제거)

**순서**
1) Flask (`flaskapi` 폴더): `FLASK_APP=app`, `FLASK_ENV=development`, `flask run -p 5000`
2) NewsService: 위 설정으로 Run
3) Gateway: 위 설정으로 Run
4) Front `.env.local`:
   ```
   NEXT_PUBLIC_API_URL=http://localhost:8000
   ```

---

## 2) 상태 확인
- News:  `http://localhost:8082/actuator/health`
- Gateway:`http://localhost:8000/actuator/health`
- 적용 검증: `http://localhost:8082/actuator/env` → `spring.datasource.url`이 **MySQL**인지, `propertySources`에 `news-service-secret.yml` 표시 확인

---

## 3) 꼭 기억
- `.local/`·`*-solo.yml`·`*-secret.yml`은 **커밋/도커 컨텍스트 제외**  
  (루트 `.gitignore` / `.dockerignore` 템플릿을 참고하여 추가)
- `ddl-auto=validate` 권장(빈 DB라면 1회만 `update` → 다시 `validate`)
- Run 구성은 **Store as project file** 끄기(개인 전용)
