#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$DIR/.."

# Ensure compiled
if [ ! -d "target/classes" ]; then
    echo "Building project..."
    mvn clean package -DskipTests
fi

# Run
echo "Starting CPU Spike Demo..."
java -XX:StartFlightRecording=dumponexit=true,filename=target/cpu-spike.jfr -cp "target/classes:target/lib/*" com.demo.profiling.cpu.CpuSpike
