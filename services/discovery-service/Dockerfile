# =================
# 1단계: 프로젝트 빌드 (JDK가 포함된 이미지 사용)
# =================
FROM openjdk:17-jdk-slim AS builder

# 작업 디렉토리 설정
WORKDIR /app

# 프로젝트 파일 전체 복사
COPY . .

# Gradle을 사용하여 프로젝트를 빌드 (jar 파일 생성)
# 만약 Maven을 사용하신다면 RUN ./mvnw package 로 변경하세요.
RUN ./gradlew bootJar

# =================
# 2단계: 실행 환경 구성 (JRE만 포함된 가벼운 이미지 사용)
# =================
FROM openjdk:17-slim

# 작업 디렉토리 설정
WORKDIR /app

# 빌드 단계(builder)에서 생성된 jar 파일만 복사
# Gradle 경로: build/libs/*.jar
# Maven 경로: target/*.jar
COPY --from=builder /app/build/libs/*.jar ./app.jar

# 컨테이너 실행 시 이 명령어가 실행됨
ENTRYPOINT ["java", "-jar", "app.jar"]

# 서비스가 사용하는 포트 명시 (문서화 목적)
# 각 서비스의 실제 포트로 변경해주세요 (예: Gateway는 8000, User-service는 8081 등)
EXPOSE 8761