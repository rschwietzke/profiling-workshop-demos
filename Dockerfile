# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
# Download dependencies
RUN mvn dependency:go-offline

COPY src ./src
# Build and copy dependencies to target/lib
RUN mvn clean package -DskipTests

# Stage 2: Run
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Copy classes and dependency jars
COPY --from=builder /app/target/classes /app/classes
COPY --from=builder /app/target/lib /app/lib

# The generic entrypoint. We use an environment variable DEMO_CLASS to switch.
ENV DEMO_CLASS="com.demo.profiling.cpu.CpuSpike"

# Keep the JVM flags flexible
ENV JAVA_OPTS=""

CMD java ${JAVA_OPTS} -cp "classes:lib/*" ${DEMO_CLASS}
