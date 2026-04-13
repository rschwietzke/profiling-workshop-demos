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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Demonstrates 6 different CPU-intensive workloads that threads cycle through
 * round-robin style. Each method shows up as a distinct hotspot in a profiler.
 */
public class CpuSpike
{

    /** Shared counter so every thread picks the next method in sequence. */
    private static volatile int methodCounter = 0;

    public static void main(String[] args)
    {
        System.out.println("Starting CPU Spike demo — cycling through 6 burn methods.");
        System.out.println("Press Ctrl+C to stop.");

        // fix up threads but use at last one
        final int numThreads = Math.max(1, Runtime.getRuntime().availableProcessors() / 2);
        
        for (int i = 0; i < numThreads; i++)
        {
            Thread t = new Thread(() ->
            {
                while (true)
                {
                    // Protect against negative modulo when methodCounter overflows
                    int method = (methodCounter++ & Integer.MAX_VALUE) % 6;
                    switch (method)
                    {
                        case 0 -> burnWithRegex();
                        case 1 -> burnWithMath();
                        case 2 -> burnWithPrimes();
                        case 3 -> burnWithStringChurn();
                        case 4 -> burnWithSorting();
                        case 5 -> burnWithDeepCalls();
                    }
                }
            }, "CpuBurnerThread-" + i);
            t.setDaemon(false);
            t.start();
        }
    }

    // ---------------------------------------------------------------
    // Method 1 — ReDoS-style catastrophic backtracking regex
    // ---------------------------------------------------------------
    private static void burnWithRegex()
    {
        String regex = "^(a+)+$";
        // 30 a's + trailing b forces exponential backtracking
        String input = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaab";

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);
        matcher.matches(); // consumes CPU during backtracking
    }

    // ---------------------------------------------------------------
    // Method 2 — Heavy trigonometric math chain
    // ---------------------------------------------------------------
    private static void burnWithMath()
    {
        double result = 0;
        for (int i = 0; i < 200_000; i++)
        {
            result += Math.tan(Math.atan(Math.tan(Math.atan(Math.tan(Math.atan(i))))));
        }
        
        // prevent dead-code elimination
        if (result == Double.MIN_VALUE)
        {
            System.out.print("");
        }
    }

    // ---------------------------------------------------------------
    // Method 3 — Brute-force prime number search (trial division)
    // ---------------------------------------------------------------
    private static int burnWithPrimes()
    {
        int count = 0;
        for (int n = 2; count < 10_000; n++)
        {
            if (isPrime(n))
            {
                count++;
            }
        }
        
        return count;
    }

    private static boolean isPrime(int n)
    {
        if (n < 2)
        {
            return false;
        }
        
        for (int i = 2; i * i <= n; i++)
        {
            if (n % i == 0)
            {
                return false;
            }
        }
        return true;
    }

    // ---------------------------------------------------------------
    // Method 4 — String concatenation & hashing churn (GC pressure)
    // ---------------------------------------------------------------
    private static void burnWithStringChurn()
    {
        StringBuilder sb = new StringBuilder();
        
        for (int i = 0; i < 50_000; i++)
        {
            sb.append("burn").append(i);
            if (i % 500 == 0)
            {
                // force hash computation and throw away the builder
                sb.toString().hashCode();
                
                // don't set empty, really let it go
                sb = new StringBuilder();
            }
        }
    }

    // ---------------------------------------------------------------
    // Method 5 — Repeated random array sorting (with custom objects)
    // ---------------------------------------------------------------
    private static void burnWithSorting()
    {
        Random rng = new Random(42);
        for (int round = 0; round < 50; round++)
        {
            List<SortContainer> list = new ArrayList<>(10_000);
            for (int i = 0; i < 10_000; i++)
            {
                list.add(new SortContainer("Item-" + rng.nextInt()));
            }
            Collections.sort(list);
        }
    }

    /**
     * Custom wrapper to create recognizable objects in allocation profiles.
     */
    static class SortContainer implements Comparable<SortContainer>
    {
        private final String value;

        public SortContainer(String value)
        {
            this.value = value;
        }

        @Override
        public int compareTo(SortContainer other)
        {
            return this.value.compareTo(other.value);
        }
    }

    // ---------------------------------------------------------------
    // Method 6 — Deep call hierarchy with polymorphic dispatch
    // ---------------------------------------------------------------
    private static void burnWithDeepCalls()
    {
        // Generates a deep flamechart with and without inlined calls
        // Depth 14 generates a substantial tree mapping nicely to the CPU profile
        Node root = buildTree(14);
        long result = evaluateTree(root);
        if (result == -1)
        {
            System.out.print("");
        }
    }

    interface Node
    {
        long compute();
    }

    static class AddNode implements Node
    {
        Node left, right;

        AddNode(Node l, Node r)
        {
            left = l;
            right = r;
        }

        public long compute()
        {
            return left.compute() + right.compute() + inlineMeIfYouCan();
        }

        // Small method, highly likely to be inlined
        private long inlineMeIfYouCan()
        {
            return 1;
        }
    }

    static class MulNode implements Node
    {
        Node left, right;

        MulNode(Node l, Node r)
        {
            left = l;
            right = r;
        }

        public long compute()
        {
            return left.compute() * right.compute() + doNotInlineMe();
        }

        // Large complex method, less likely to be inlined cleanly
        private long doNotInlineMe()
        {
            long sum = 0;
            for (int i = 0; i < 5; i++)
            {
                sum += (long) Math.acos(Math.sin(i));
            }
            return sum;
        }
    }

    static class ValueNode implements Node
    {
        long val;

        ValueNode(long v)
        {
            val = v;
        }

        // Trivial method, guaranteed inline
        public long compute()
        {
            return val;
        }
    }

    private static Node buildTree(int depth)
    {
        if (depth == 0)
        {
            return new ValueNode(2);
        }
        if (depth % 2 == 0)
        {
            return new AddNode(buildTree(depth - 1), buildTree(depth - 1));
        }
        return new MulNode(buildTree(depth - 1), buildTree(depth - 1));
    }

    private static long evaluateTree(Node root)
    {
        return root.compute();
    }
}
