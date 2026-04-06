#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$DIR/.."

# Ensure compiled
if [ ! -d "target/classes" ]; then
    echo "Building project..."
    mvn clean package -DskipTests
fi

# Run
echo "Starting High Allocation Rate Demo..."
java -XX:StartFlightRecording=dumponexit=true,filename=target/high-allocation.jfr -cp "target/classes:target/lib/*" com.demo.profiling.memory.HighAllocationRate
