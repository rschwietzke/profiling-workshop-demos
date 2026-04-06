/*
 * Copyright 2026 rschwietzke
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.demo.profiling.concurrency;

public class LockContention {

    private static final Object SHARED_LOCK = new Object();
    private static volatile long counter = 0;

    public static void main(String[] args) {
        System.out.println("Starting Lock Contention demo.");

        int numThreads = 10;
        for (int i = 0; i < numThreads; i++) {
            new Thread(() -> {
                while (true) {
                    doWork();
                }
            }, "WorkerThread-" + i).start();
        }
    }

    private static void doWork() {
        // High contention: many threads trying to enter this small synchronized block
        synchronized (SHARED_LOCK) {
            counter++;
            // Simulate a tiny amount of work inside the lock and delay so others wait
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
