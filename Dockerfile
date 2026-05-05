FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build

COPY pom.xml .
COPY .mvn/ .mvn/
COPY mvnw .
RUN chmod +x mvnw && ./mvnw dependency:resolve dependency:resolve-plugins -q

COPY src/ src/
RUN ./mvnw package -DskipTests

FROM gcr.io/distroless/java21-debian12 AS runtime
WORKDIR /app
EXPOSE 8080 8081
COPY --from=builder /build/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
