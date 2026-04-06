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
package com.demo.profiling.memory;

import java.util.UUID;

public class HighAllocationRate {

    public static void main(String[] args) {
        System.out.println("Starting High Allocation Rate demo. Watch the GC activity!");
        
        int numThreads = 4;
        for (int i = 0; i < numThreads; i++) {
            new Thread(() -> {
                while (true) {
                    generateGarbage();
                }
            }, "AllocatorThread-" + i).start();
        }
    }

    private static void generateGarbage() {
        // Allocate a batch of short-lived Strings and objects
        for(int i = 0; i < 10000; i++) {
            String garbage = UUID.randomUUID().toString() + "-" + System.currentTimeMillis();
            
            // Do dummy operation so JIT doesn't fully optimize it away
            if (garbage.hashCode() == 0) {
                System.out.println("Rare event!");
            }
        }
        
        try {
            // Small pause to yield and not 100% just one core, let GC catch up marginally
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
