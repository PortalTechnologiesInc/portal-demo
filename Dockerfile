# --- FRONTEND BUILD STAGE ---
FROM node:20 AS frontend
WORKDIR /app/frontend
COPY frontend/ .

ARG VITE_BACKEND_API_WS
ENV VITE_BACKEND_API_WS=$VITE_BACKEND_API_WS

RUN npm ci
RUN npm run build

# --- BACKEND BUILD STAGE ---
FROM maven:3.9.9-eclipse-temurin-17 AS backend
WORKDIR /app/backend
COPY . .
COPY --from=frontend /app/frontend/dist /app/backend/src/main/resources/static
RUN mvn clean package -DskipTests

# --- RUNTIME STAGE ---
FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY --from=backend /app/backend/target/portal-demo-*.jar app.jar
EXPOSE 7070
ENTRYPOINT ["java", "-jar", "app.jar"]