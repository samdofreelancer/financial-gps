FROM maven:3.9.9-eclipse-temurin-21 AS source

WORKDIR /workspace/backend
COPY .mvn .mvn
COPY mvnw pom.xml ./
# Strip CR so the build works from CRLF checkouts (Windows) too —
# same pattern as compose.override.yaml's dev-watch.sh handling.
RUN tr -d '\r' < mvnw > /tmp/mvnw && mv /tmp/mvnw mvnw && chmod +x mvnw
COPY src src

FROM source AS test
CMD ["./mvnw", "-B", "test"]

FROM source AS package
RUN ./mvnw -B -DskipTests package

FROM eclipse-temurin:21-jre-jammy AS runtime

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=package /workspace/backend/target/backend-*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-Duser.timezone=UTC", "-jar", "/app/app.jar"]
