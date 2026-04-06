# Profiling Demos

This project provides a set of intentionally unoptimized, problematic Java 21 programs designed for demonstrating profiling techniques. It includes scenarios for exploring high CPU utilization, memory leaks, high object allocation rates, and thread concurrency issues (like deadlocks and lock contention).

## Supported Platforms

> **Note:** This project currently only supports **Linux and macOS** out of the box via the provided Bash starter scripts. Windows `.bat` or PowerShell scripts are not included. If you are on Windows, you can either run the Docker Compose environment or run the `java` commands manually.

## Features

- **CPU Spike (`CpuSpike.java`)**: Simulates a CPU-bound workload, perfect for exploring CPU profiling.
- **Memory Leak (`MemoryLeak.java`)**: Gradually escapes memory to cause an OutOfMemoryError, ideal for Heap Dump analysis.
- **High Allocation (`HighAllocationRate.java`)**: Fast-paces object creation to trigger intense Garbage Collection activity.
- **Deadlock (`DeadlockDemo.java`)**: Generates a classic thread lock order issue resulting in a hang.
- **Lock Contention (`LockContention.java`)**: Creates extreme synchronization bottlenecks across multiple threads.

## Usage

You can launch any of these demos directly via their bash scripts (Linux/macOS):

```bash
# Example:
./bin/run-memory-leak.sh
```

All runner scripts are configured with Java Flight Recorder (JFR) support out-of-the-box. Upon exiting the script/program, a profiling recording will be dumped into the `target/` directory (e.g. `target/memory-leak.jfr`).

You can also run everything concurrently using Docker Compose:

```bash
docker-compose up --build
```

## Disclaimer

This codebase was proudly built with the support of **Antigravity** (an Advanced Agentic Coding AI). 

## License

This project is licensed under the [Apache License 2.0](LICENSE).
