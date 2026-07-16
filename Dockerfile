# ---- Etapa 1: build ----
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app

# Copiamos primero solo lo necesario para resolver dependencias (mejor cache de Docker)
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline

# Ahora sí el código fuente y el build
COPY src ./src
RUN ./mvnw -B clean package -DskipTests

# ---- Etapa 2: runtime (imagen final, sin Maven ni JDK completo) ----
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Usuario sin privilegios (no correr la app como root dentro del contenedor)
RUN addgroup --system spring && adduser --system --ingroup spring spring
USER spring:spring

COPY --from=build /app/target/erp-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
