# 빌드 단계 --------------------------------------------------------------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# 의존성만 먼저 받아 두면 소스만 바뀔 때 이 레이어가 캐시된다.
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

COPY src src
# 테스트는 CI 에서 이미 실행한다. 이미지 빌드에서는 건너뛴다.
RUN ./gradlew --no-daemon clean bootJar -x test

# 실행 단계 --------------------------------------------------------------
FROM eclipse-temurin:21-jre

# JDK 는 자체 시간대 DB(tzdb.dat)를 갖고 있어 OS 에 tzdata 를 설치하지 않아도
# -Duser.timezone 으로 한국 시간이 적용된다. TZ 는 OS 도구용으로 함께 둔다.
ENV TZ=Asia/Seoul
ENV JAVA_OPTS=""

# 헬스체크용. 이미지에 기본 포함되지 않으므로 명시적으로 설치한다.
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/*

# root 로 실행하지 않는다.
RUN useradd --system --create-home --shell /usr/sbin/nologin writegrow

WORKDIR /app
COPY --from=build --chown=writegrow:writegrow /workspace/build/libs/*.jar app.jar

USER writegrow
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Duser.timezone=Asia/Seoul -jar /app/app.jar"]
