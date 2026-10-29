FROM eclipse-temurin:25-jre

ARG JAR_FILE=build/libs/simple-plugin-registry-0.0.1-SNAPSHOT.jar

WORKDIR /app
COPY ${JAR_FILE} application.jar

USER 1000:1000
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/application.jar"]
