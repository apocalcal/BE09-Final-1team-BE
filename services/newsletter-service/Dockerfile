FROM openjdk:17-jdk-slim

WORKDIR /app

# Gradle wrapper와 build.gradle 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle .

# 소스 코드 복사
COPY src src

# Gradle 빌드 실행
RUN chmod +x ./gradlew
RUN ./gradlew build -x test

# JAR 파일을 app.jar로 복사
RUN cp build/libs/*.jar app.jar

# 포트 노출
EXPOSE 8085

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"]
