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
package com.demo.profiling.custom;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class CacheMissDemo {

    @Name("com.demo.profiling.custom.CacheOperation")
    @Label("Cache Operation")
    @Description("Records when a cache lookup occurs and whether it was a hit or miss.")
    @Category({"Application", "Cache"})
    @StackTrace(true)
    public static class CacheOperationEvent extends Event {
        @Label("Cache Key")
        String key;

        @Label("Cache Hit")
        boolean hit;
    }

    private static final Map<String, String> cache = new HashMap<>();
    private static final Random random = new Random();
    
    // Simulating a set of keys our app frequently accesses
    private static final String[] KEYS = {
        "user:101", "user:102", "user:103", "user:104", 
        "user:105", "config:main", "config:db", "session:abc"
    };

    public static void main(String[] args) throws InterruptedException {
        System.out.println("Starting Custom JFR Cache Miss Demo. Press Ctrl+C to stop.");
        System.out.println("Run with:");
        System.out.println("java -XX:StartFlightRecording=duration=60s,filename=cache.jfr -cp target/classes com.demo.profiling.custom.CacheMissDemo");

        // Pre-populate some keys to ensure we get some hits
        cache.put("config:main", "value-main");
        cache.put("config:db", "value-db");

        while (true) {
            String targetKey = KEYS[random.nextInt(KEYS.length)];
            
            CacheOperationEvent event = new CacheOperationEvent();
            event.begin();
            
            boolean hit = checkCache(targetKey);
            
            event.key = targetKey;
            event.hit = hit;
            event.commit();

            // Randomly evict cache to keep things dynamic and cause fresh misses
            if (random.nextDouble() > 0.95) {
                cache.clear();
            }

            // Small delay to simulate general application processing throughput
            Thread.sleep(random.nextInt(5, 55)); 
        }
    }

    private static boolean checkCache(String key) {
        if (cache.containsKey(key)) {
            // Hit is super fast natively
            return true;
        } else {
            // Miss requires a slow simulated DB fetch
            simulateDatabaseFetch();
            cache.put(key, "data-for-" + key);
            return false;
        }
    }

    private static void simulateDatabaseFetch() {
        try {
            // Simulate 50ms - 150ms delay for DB query
            Thread.sleep(random.nextInt(25, 75));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
