#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$DIR/.."

# Ensure compiled
if [ ! -d "target/classes" ]; then
    echo "Building project..."
    mvn clean package -DskipTests
fi

# Run with small heap and dump on OOM
echo "Starting Memory Leak Demo..."
java -Xmx64m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=target/heapdump.hprof -XX:StartFlightRecording=dumponexit=true,filename=target/memory-leak.jfr -cp "target/classes:target/lib/*" com.demo.profiling.memory.MemoryLeak
