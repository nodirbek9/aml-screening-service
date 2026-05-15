FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY build/libs/screening-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 7777
ENTRYPOINT ["java", "-jar", "app.jar"]
