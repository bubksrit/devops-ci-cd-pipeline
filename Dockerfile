FROM eclipse-temurin:17-jre

WORKDIR /app

COPY target/*.jar app.jar

ENV SERVER_PORT=8080
EXPOSE 8080

USER 10001

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
