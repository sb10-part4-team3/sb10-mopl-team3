FROM amazoncorretto:17 AS builder

WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

RUN chmod +x gradlew

COPY src src

RUN ./gradlew clean bootJar -x test --no-daemon \
    && find build/libs -maxdepth 1 -type f -name "*.jar" ! -name "*-plain.jar" \
       -exec cp {} /app/app.jar \;

FROM amazoncorretto:17-alpine

WORKDIR /app

COPY --from=builder /app/app.jar app.jar

EXPOSE 8080

ENV JVM_OPTS=""

ENTRYPOINT ["sh", "-c", "exec java $JVM_OPTS -jar app.jar"]
