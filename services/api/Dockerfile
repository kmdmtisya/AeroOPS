# Multi-stage build so no local JDK/Maven install is required to build or run the API.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Import any locally-exported corporate TLS-inspection CA certs (see certs/README.md)
# into the JDK trust store, so Maven can reach Maven Central behind such a proxy.
# The wildcard copy + shopt keeps this a no-op when certs/ is empty (open network, CI).
COPY certs/ /tmp/certs/
RUN if [ -n "$(ls -A /tmp/certs 2>/dev/null)" ]; then \
      for cert in /tmp/certs/*.crt; do \
        name=$(basename "$cert" .crt); \
        keytool -importcert -noprompt -trustcacerts \
          -alias "$name" -file "$cert" \
          -keystore "$JAVA_HOME/lib/security/cacerts" -storepass changeit; \
      done; \
    fi

COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/target/api-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
