FROM amazoncorretto:25

# Install curl for container health checks
RUN yum install -y curl || dnf install -y curl || true

WORKDIR /app

# Copy the pre-built application JAR
COPY target/crescendo-backend.jar /app/crescendo-backend.jar

EXPOSE 8080

# Configure container memory limits (-Xmx768m for 4 GiB EC2 host) and G1GC
ENV JAVA_OPTS="-Xms256m -Xmx768m -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar crescendo-backend.jar"]
