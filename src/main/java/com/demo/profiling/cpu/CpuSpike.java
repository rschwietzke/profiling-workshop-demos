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
package com.demo.profiling.cpu;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class CpuSpike {
    public static void main(String[] args) {
        System.out.println("Starting CPU Spike demo. Press Ctrl+C to stop.");
        
        int numThreads = Runtime.getRuntime().availableProcessors();
        for (int i = 0; i < numThreads; i++) {
            Thread t = new Thread(() -> {
                while (true) {
                    burnCpu();
                }
            }, "CpuBurnerThread-" + i);
            t.setDaemon(false);
            t.start();
        }
    }

    private static void burnCpu() {
        // Expensive regex calculation typical of ReDoS vulnerabilities
        String regex = "^(a+)+$";
        String testString = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaab";
        
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(testString);
        
        // This will block and consume CPU for a while
        matcher.matches();
        
        // Alternative math burn
        double result = 0;
        for (int i = 0; i < 100000; i++) {
            result += Math.tan(Math.atan(Math.tan(Math.atan(Math.tan(Math.atan(i))))));
        }
    }
}
