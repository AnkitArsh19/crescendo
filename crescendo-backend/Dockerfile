# ==============================================================================
# Stage 1: Build Application JAR using Maven Wrapper & Amazon Corretto 25
# ==============================================================================
FROM amazoncorretto:25 AS builder

WORKDIR /build

# Install essential utilities for maven wrapper (tar, gzip, unzip)
RUN dnf install -y tar gzip unzip findutils && dnf clean all

# Copy Maven wrapper configuration and descriptor first for optimal layer caching
COPY .mvn .mvn
COPY mvnw pom.xml ./

# Ensure correct line endings and executable permissions for Linux environment
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw

# Pre-fetch project dependencies into cached layer
RUN ./mvnw dependency:go-offline -B || true

# Copy application source code
COPY src src

# Ensure properties templates are present in the build if omitted by .gitignore
RUN [ -f src/main/resources/application-docker.properties ] || cp src/main/resources/application-docker.properties.example src/main/resources/application-docker.properties
RUN [ -f src/main/resources/application.properties ] || cp src/main/resources/application.properties.example src/main/resources/application.properties

# Build executable production JAR without running unit tests during Docker image build
RUN ./mvnw clean package -DskipTests -B

# ==============================================================================
# Stage 2: Lightweight Production Runtime Image
# ==============================================================================
FROM amazoncorretto:25 AS runtime

# Install curl for container health check endpoint
RUN dnf install -y curl && dnf clean all || true

WORKDIR /app

# Copy the compiled production JAR from builder stage
COPY --from=builder /build/target/crescendo-backend.jar /app/crescendo-backend.jar

EXPOSE 8080

# Configure container memory limits (-Xms256m -Xmx768m) and G1GC for production stability
ENV JAVA_OPTS="-Xms256m -Xmx768m -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar crescendo-backend.jar"]
