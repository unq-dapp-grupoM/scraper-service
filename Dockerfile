# --- Etapa 1: Construcción (Build Stage) ---
# Usamos una imagen de Gradle con JDK 21 para compilar la aplicación.
FROM gradle:8.8-jdk21-jammy AS builder

WORKDIR /app
COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle

# Damos permisos de ejecución al wrapper de Gradle
RUN chmod +x gradlew

RUN ./gradlew dependencies --no-daemon

COPY src ./src
RUN ./gradlew bootJar --no-daemon

# --- Etapa 2: Ejecución (Runtime Stage) ---
# Usamos una imagen de Java 21 optimizada y ligera.
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]