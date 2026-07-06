FROM amazoncorretto:17 AS builder

WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

RUN chmod +x gradlew

# Cache dependency resolution separately from application source changes.
RUN ./gradlew dependencies --no-daemon

COPY src src

RUN ./gradlew clean bootJar -x test --no-daemon \
    && find build/libs -maxdepth 1 -type f -name "*.jar" ! -name "*-plain.jar" \
       -exec cp {} /app/app.jar \;

FROM amazoncorretto:17-alpine

WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=builder --chown=spring:spring /app/app.jar app.jar

EXPOSE 8080

ENV JVM_OPTS=""

USER spring

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JVM_OPTS -jar app.jar"]
