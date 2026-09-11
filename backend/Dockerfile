FROM maven:3.9.11-eclipse-temurin-17 AS build

WORKDIR /build
COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN chmod +x mvnw
RUN ./mvnw -q -DskipTests dependency:go-offline

COPY src src
RUN ./mvnw -q -DskipTests package

FROM eclipse-temurin:17-jre

WORKDIR /app
RUN useradd --system --create-home --uid 10001 appuser
COPY --from=build /build/target/backend-*.jar app.jar
USER appuser

EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
