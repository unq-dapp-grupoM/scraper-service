# --- Build Stage ---
FROM gradle:8.8-jdk21-jammy AS build

WORKDIR /app
COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle
RUN chmod +x gradlew
RUN ./gradlew build -x test --no-daemon --stacktrace || true

COPY src ./src
RUN ./gradlew build -x test --no-daemon

# --- Runtime Stage ---
FROM mcr.microsoft.com/playwright/java:v1.45.0-jammy

WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]