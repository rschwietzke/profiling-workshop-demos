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

import java.util.ArrayList;
import java.util.List;

public class MemoryLeak {
    
    // A static list that holds references indefinitely
    private static final List<byte[]> LEAKED_LIST = new ArrayList<>();

    public static void main(String[] args) throws InterruptedException {
        System.out.println("Starting Memory Leak demo. Monitor heap usage until OOM.");
        
        while (true) {
            System.out.println("Allocating 10MB chunk...");
            // Add a 10MB byte array to the list
            byte[] chunk = new byte[10 * 1024 * 1024];
            LEAKED_LIST.add(chunk);
            
            // Sleep to let us observe the heap growing step by step
            Thread.sleep(500);
        }
    }
}
