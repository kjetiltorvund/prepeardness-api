FROM eclipse-temurin:21-jre-alpine

# Sikkerhet: Lag en ikke-root-bruker
RUN apk add --no-cache wget \
    && addgroup -S spring \
    && adduser -S spring -G spring

WORKDIR /app

# Kopier filen som actions/download-artifact la i target-mappen
COPY target/*.jar app.jar

# Gi 'spring'-brukeren eierskap til filene
RUN chown -R spring:spring /app

USER spring

EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1
CMD ["java", "-jar", "app.jar"]
