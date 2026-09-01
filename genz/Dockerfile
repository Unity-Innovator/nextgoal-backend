FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:resolve -B -Dmaven.wagon.http.retryHandler.count=3 -Dmaven.wagon.httpconnectionManager.ttlSeconds=60

COPY src ./src
RUN mvn clean package -B

FROM eclipse-temurin:21-jre

WORKDIR /app

# Create a secure non-root user (Debian syntax)
RUN addgroup --system spring && adduser --system --ingroup spring spring
USER spring:spring

# Copy the compiled JAR from the builder stage
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENV JAVA_OPTS="-XX:+UseG1GC -XX:+UseContainerSupport -XX:MaxRAMPercentage=70.0 -XX:InitialRAMPercentage=25.0 -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
