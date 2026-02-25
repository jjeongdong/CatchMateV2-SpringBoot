# 1) Build Stage
FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /app

# Gradle Wrapper / settings / root build
COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle settings.gradle ./

# (캐시 핵심) 멀티모듈의 build.gradle만 먼저 복사
COPY catchmate-common/build.gradle          catchmate-common/
COPY catchmate-domain/build.gradle          catchmate-domain/
COPY catchmate-infrastructure/build.gradle  catchmate-infrastructure/
COPY catchmate-application/build.gradle     catchmate-application/
COPY catchmate-orchestration/build.gradle   catchmate-orchestration/
COPY catchmate-api/build.gradle             catchmate-api/
COPY catchmate-authorization/build.gradle   catchmate-authorization/
COPY catchmate-mcp/build.gradle             catchmate-mcp/
COPY catchmate-boot/build.gradle            catchmate-boot/

RUN chmod +x ./gradlew

# (캐시 활용) 전체 dependencies 대신, 실제 컨테이너 빌드 타깃 모듈만 의존성 해석
RUN ./gradlew :catchmate-boot:dependencies --no-daemon

# 소스 전체 복사 후 빌드
COPY . .
RUN ./gradlew :catchmate-boot:bootJar -x test --no-daemon


# 2) Run Stage
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

COPY --from=builder /app/catchmate-boot/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
