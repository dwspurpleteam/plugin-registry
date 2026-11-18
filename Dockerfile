FROM eclipse-temurin:25-jre

RUN apt-get update && apt-get install -y --no-install-recommends wget && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY build/libs/simple-plugin-registry-0.0.1-SNAPSHOT.jar application.jar

USER 1000:1000

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/application.jar"]
