# 1) Build Stage
FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /app

# Gradle Wrapper / settings / root build
COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle settings.gradle ./

RUN chmod +x ./gradlew

# 의존성 사전 해석 (캐시 활용)
RUN ./gradlew dependencies --no-daemon || true

# 소스 전체 복사 후 빌드
COPY . .
RUN ./gradlew bootJar -x test --no-daemon


# 2) Run Stage
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

RUN mkdir -p /app/logs

EXPOSE 8080
# 힙은 런타임 env(JVM_HEAP)로 오버라이드 가능. 기본 512m 유지(단일호스트 배포=app+redis+nginx 한 대라 OOM 방지).
# 스케일아웃 App EC2(앱 컨테이너만 실행, 2GB)에서는 compose 로 JVM_HEAP=1g 등으로 상향 → 수천 WS 세션 대비.
ENV JVM_HEAP="512m"
ENV JAVA_OPTS="\
  -XX:MaxMetaspaceSize=192m \
  -Xss512k \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:G1HeapRegionSize=4m \
  -XX:InitiatingHeapOccupancyPercent=45 \
  -XX:+UseStringDeduplication \
  -XX:+UseContainerSupport \
  -Xlog:gc*:file=/app/logs/gc.log:time,uptime,level,tags:filecount=5,filesize=10m"

ENTRYPOINT ["sh", "-c", "java -Xms$JVM_HEAP -Xmx$JVM_HEAP $JAVA_OPTS -jar app.jar"]
