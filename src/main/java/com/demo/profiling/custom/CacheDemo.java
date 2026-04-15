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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.FlightRecorder;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.Period;
import jdk.jfr.StackTrace;
import jdk.jfr.consumer.EventStream;


/**
 * We show how to create events when something happens, we create events at fixed intervals too.
 * Also, we use the event stream and use it to push extra alerts.
 */
public class CacheDemo 
{
	private static final int CACHESIZE = 1000;
	private final LRUCache<String, String> cache = new LRUCache<>(CACHESIZE);

	public static void main(String[] args) throws InterruptedException, IOException 
	{
		System.out.println("Starting Custom JFR Cache Demo. Press Ctrl+C to stop.");
		System.out.println("Run with:");
		System.out.println("java -XX:StartFlightRecording=filename=cache.jfr com.demo.profiling.custom.CacheDemo");

		System.out.println("Starting JFR Event Streaming for CacheStatistics...");

		var rs = EventStream.openRepository(); 
		{
			rs.onEvent("custom.CacheStatistics", event -> {
				var hitRate = event.getDouble("rate");
				if (hitRate <= 0.75)
				{
					// you must fire it from another thread then the even listener,
					// java prevents recursion but suppressing events from here
					CompletableFuture.runAsync(() -> {
				        var e = new LowCacheRateEvent();
				        e.rate = hitRate;
				        e.commit();
				    });
				}
				System.out.println(event.toString());
			});
			rs.onEvent(event -> {
				if (event.getEventType().getName().equals("custom.LowCacheRateEvent"))
				{
					System.out.println(event.toString());
				}
			});
			rs.startAsync();
		} 

		System.out.println("Playing with the cache...");

		CacheDemo cd = new CacheDemo();
		cd.run();
	}

	/**
	 * Our main method
	 * @throws InterruptedException 
	 */
	public void run() throws InterruptedException
	{
		final Random r = new Random();
		long lastCacheKill = System.currentTimeMillis();

		// register periodic timer
		System.out.println("Registering cache statistics...");
		FlightRecorder.addPeriodicEvent(CacheStatistics.class, new CacheStatisticsReporter(this.cache));

		// ok, we need more data then CACHESIZE
		final List<String> goodData = new ArrayList<>();
		for (int i = 0; i < CACHESIZE; i++)
		{
			var s = "GoodData-" + Math.abs(r.nextInt());
			goodData.add(s);

			// fill cache
			cache.put(s, s);
		}

		// do cache fun
		double cacheTrigger = 1.00d;

		while (true)
		{
			// good or bad
			final String key;
			if (r.nextDouble() > cacheTrigger)
			{
				// bad
				key = "BadData-" + r.nextInt();
				cacheTrigger = cacheTrigger >= 1.00 ? cacheTrigger + 0.1d : 1.00d;
			}
			else
			{
				// good
				key = goodData.get(r.nextInt(goodData.size()));
				cacheTrigger -= 0.01d;
			}

			var v = cache.get(key);
			if (v == null)
			{
				// burn time and cache
				switch (r.nextInt(3))
				{
				case 0 : burnWithSorting(); break;
				case 1 : burnWithMath(); break;
				case 2 : burnWithStringChurn(); break;
				}
				cache.put(key, "");
			}

			// pretend to be a normal application
			if (r.nextDouble() < 0.01)
			{
				Thread.sleep(1);
			}

			// clear the cache randomly
			if (lastCacheKill + 60 * 1000 < System.currentTimeMillis())
			{
				cache.clear();
				cacheTrigger = 1.00d;
				lastCacheKill = System.currentTimeMillis();
			}
		}
	}

	public static class LRUCache<K, V> extends LinkedHashMap<K, V> 
	{
		private final int capacity;
		private AtomicLong hits = new AtomicLong(0);
		private AtomicLong misses = new AtomicLong(0);

		public LRUCache(int capacity) 
		{
			super(capacity, 0.75f, true);
			this.capacity = capacity;
		}

		@Override
		public V get(Object key)
		{
			final var e = new CacheEvent();
			e.key = key.toString();

			e.begin();
			var v = super.get(key);
			e.end();

			if (v != null)
			{
				e.hit = true;
				hits.incrementAndGet();
			}
			else
			{
				e.hit = false;
				misses.incrementAndGet();
			}

			e.commit();

			return v;
		}

		@Override
		public void clear()
		{
			super.clear();
			
			this.hits.set(0);
			this.misses.set(0);
		}
		
		@Override
		protected boolean removeEldestEntry(Map.Entry<K, V> eldest) 
		{
			var b = size() > this.capacity;

			if (b)
			{
				var e = new CacheLRUEvent();
				e.key = eldest.getKey().toString();
				e.commit();
			}

			return b;
		}
	}

	@Name("custom.CacheEvent")
	@Label("Cache Hit")
	@Description("Records when a cache lookup occurs and whether it was a hit.")
	@Category({"Application", "Cache"})
	@StackTrace(false)
	static class CacheEvent extends Event 
	{
		@Label("Cache Key")
		String key;

		@Label("Cache Hit")
		boolean hit;
	}

	@Name("custom.CacheLRUEvent")
	@Label("Cache LRU Event")
	@Description("Records cache misses and from where and what data was involved.")
	@Category({"Application", "Cache"})
	@StackTrace(true)
	static class CacheLRUEvent extends Event 
	{
		@Label("Cache Key")
		String key;
	}

	@Name("custom.CacheStatistics")
	@Label("Cache Statistics")
	@Category({"Application", "Cache"})
	@Period("1s")
	static class CacheStatistics extends Event 
	{
		@Label("Capacity")
		int capacity;

		@Label("Size")
		int size;

		@Label("Hits")
		long hits;

		@Label("Misses")
		long misses;

		@Label("HitRate")
		double rate;

		@Label("Utilization")
		double utilization;
	}

	@Name("custom.LowCacheRateEvent")
	@Label("Low Cache Rate Alarm")
	@Description("Fired when the cache rate is below 0.75 for a while")
	@Category({"Application", "Cache"})
	@StackTrace(false)
	static class LowCacheRateEvent extends Event 
	{
		@Label("HitRate")
		double rate;
	}

	public static class CacheStatisticsReporter implements Runnable
	{
		private final LRUCache<?, ?> cache;

		public CacheStatisticsReporter(LRUCache<?, ?> cache)
		{
			this.cache = cache;
		}

		@Override
		public void run() 
		{
			var e = new CacheStatistics();
			e.hits = cache.hits.get();
			e.misses = cache.misses.get();
			e.size = cache.size();
			e.capacity = cache.capacity;

			double total = e.hits + e.misses;
			e.rate = total > 0 ? Math.floor(((double)e.hits / total) * 100) / 100 : 0.00d;

			e.utilization = Math.floor(((double)e.size / (double)e.capacity) * 100) / 100; 

			e.commit();
		}
	}


	// ---------------------------------------------------------------
	// Method 2 — Heavy trigonometric math chain
	// ---------------------------------------------------------------
	private static void burnWithMath()
	{
		double result = 0;
		for (int i = 0; i < 2_000_000; i++)
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
	// Method 4 — String concatenation & hashing churn (GC pressure)
	// ---------------------------------------------------------------
	private static void burnWithStringChurn()
	{
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < 500_000; i++)
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
	// Method 5 — Repeated random array sorting (with custom objects)
	// ---------------------------------------------------------------
	private static void burnWithSorting()
	{
		Random rng = new Random(42);
		for (int round = 0; round < 50; round++)
		{
			List<SortContainer> list = new ArrayList<>(100_000);
			for (int i = 0; i < 10_000; i++)
			{
				list.add(new SortContainer("Item-" + rng.nextInt()));
			}
			Collections.sort(list);
		}
	}
}
