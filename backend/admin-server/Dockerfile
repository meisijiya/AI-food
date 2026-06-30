FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/admin-server.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar", "--spring.profiles.active=prod"]