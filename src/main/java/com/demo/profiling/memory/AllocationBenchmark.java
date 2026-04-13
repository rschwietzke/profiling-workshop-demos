package com.demo.profiling.memory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class AllocationBenchmark 
{

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        // Parse thread count from command line, default to 1
        int numThreads = 1;
        if (args.length > 0) {
            try {
                numThreads = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid thread count. Using default: 1");
            }
        }
        else
        {
            System.err.println("Invalid thread count. Just say 1 or 2 or ...");
            System.exit(-1);
        }

        int durationSeconds = 5;
        int arraySizeBytes = 1000 * 1024; // 1000 kb
        int batchSize = 5000;   // Batch to avoid calling System.nanoTime() too often

        System.out.printf("Running with %d threads...%n", numThreads);

        System.out.println("Warming up JIT compiler (2 seconds)...");
        runBenchmark(numThreads, 2, arraySizeBytes, batchSize);

        System.out.println("Starting actual benchmark...");
        long totalAllocatedBytes = runBenchmark(numThreads, durationSeconds, arraySizeBytes, batchSize);

        // Calculate and format results
        double allocatedGb = totalAllocatedBytes / (1024.0 * 1024.0 * 1024.0);
        double gbPerSecond = allocatedGb / durationSeconds;

        System.out.printf("Total Allocated: %.2f GB in %d seconds%n", allocatedGb, durationSeconds);
        System.out.printf("Allocation Rate: %.2f GB/sec%n", gbPerSecond);
    }

    private static long runBenchmark(int numThreads, int seconds, int arraySize, int batchSize) 
            throws InterruptedException, ExecutionException {
        
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        List<Callable<Long>> tasks = new ArrayList<>();
        
        for (int i = 0; i < numThreads; i++) {
            tasks.add(new AllocationTask(seconds, arraySize, batchSize));
        }

        long totalBytes = 0;
        // invokeAll blocks until all threads finish their duration
        List<Future<Long>> results = executor.invokeAll(tasks); 
        for (Future<Long> result : results) {
            totalBytes += result.get();
        }
        
        executor.shutdown();
        return totalBytes;
    }

    // A Callable task so each thread can return its individual allocation total
    static class AllocationTask implements Callable<Long> {
        
        // CRITICAL: Each thread gets its own volatile sink. 
        // If they shared a static sink, cache line contention would destroy the benchmark.
        public volatile Object threadSink;
        
        private final int seconds;
        private final int arraySize;
        private final int batchSize;

        public AllocationTask(int seconds, int arraySize, int batchSize) {
            this.seconds = seconds;
            this.arraySize = arraySize;
            this.batchSize = batchSize;
        }

        @Override
        public Long call() {
            long startNanos = System.nanoTime();
            long durationNanos = seconds * 1_000_000_000L;
            long allocatedBytes = 0;

            while (System.nanoTime() - startNanos < durationNanos) {
                for (int i = 0; i < batchSize; i++) {
                    // Forces the JVM to materialize the array on the heap
                    threadSink = new byte[arraySize];
                }
                allocatedBytes += (long) batchSize * arraySize;
            }
            return allocatedBytes;
        }
    }
}