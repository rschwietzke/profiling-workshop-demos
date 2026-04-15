# Stage 1: Build
FROM maven:3.9-eclipse-temurin-25 AS builder

WORKDIR /app
COPY pom.xml .

# Download dependencies
RUN mvn dependency:go-offline

COPY src ./src
# Build and copy dependencies to target/lib
RUN mvn clean package -DskipTests

# Stage 2: Run
FROM eclipse-temurin:25-jdk-jammy
WORKDIR /app

# Copy classes and dependency jars
COPY --from=builder /app/target/classes /app/classes
COPY --from=builder /app/target/lib /app/lib

# The generic entrypoint. We use an environment variable DEMO_CLASS to switch.
ENV DEMO_CLASS="com.demo.profiling.cpu.CpuSpike"

## Expose the internal port
# JMX
EXPOSE 7091

COPY libasyncProfiler.so /usr/local/lib/
ENV LD_LIBRARY_PATH="/usr/local/lib:$LD_LIBRARY_PATH"

# Optional: Make sure it's executable if required
RUN chmod 755 /usr/local/lib/libasyncProfiler.so

# Keep the JVM flags flexible
ENV JAVA_OPTS=""
ENV JAVA_OPTS="$JAVA_OPTS -XX:+UnlockDiagnosticVMOptions"
ENV JAVA_OPTS="$JAVA_OPTS -XX:+DebugNonSafepoints"
ENV JAVA_OPTS="$JAVA_OPTS -Dcom.sun.management.jmxremote.port=7091"
ENV JAVA_OPTS="$JAVA_OPTS -Dcom.sun.management.jmxremote.rmi.port=7091"
ENV JAVA_OPTS="$JAVA_OPTS -Dcom.sun.management.jmxremote.authenticate=false"
ENV JAVA_OPTS="$JAVA_OPTS -Dcom.sun.management.jmxremote.ssl=false"
ENV JAVA_OPTS="$JAVA_OPTS -Dcom.sun.management.jmxremote"
ENV JAVA_OPTS="$JAVA_OPTS -Djava.rmi.server.hostname=localhost"

CMD java ${JAVA_OPTS} -cp "classes:lib/*" ${DEMO_CLASS}
