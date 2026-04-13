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

import java.util.Random;

public class MemoryAllocator 
{
	private long lastSwitch = System.currentTimeMillis();
	private Random random = new Random(4219876781L);
	private int LENGTH_MS = 60 * 1000;

	private final int MAX_SIZE = 800_000_000;

	private Object[] data;

	private void run() 
	{
		int what = 1;

		while (true)
		{
			switch ((what++) % 2)
			{
				case 0: increasing(); break;
				case 1: random(); break;
			}
			lastSwitch = System.currentTimeMillis();
		}
	}

	private void increasing()
	{
		int SLOTS = 100_000;
		data = new Object[SLOTS];
		int avgSlotSize = MAX_SIZE / SLOTS;
		int maxSlotSize = 2 * avgSlotSize;
		double increment = (double)maxSlotSize / (double)SLOTS;
		int currentSlot = 0;
		int currentSlotSize = 0;
		int total = 0;

		int slotsPerSecond = SLOTS / (LENGTH_MS / 1000);
		long time = System.currentTimeMillis();
		long lastSec = time / 1000;

		while (lastSwitch + LENGTH_MS > (time = System.currentTimeMillis()))
		{
			if (lastSec != (time / 1000))
			{
				lastSec = (time / 1000);

				for (int i = 0; i < slotsPerSecond; i++)
				{
					currentSlot = currentSlot % SLOTS;
					currentSlotSize = (int) ((currentSlot * increment) % maxSlotSize); 
					total += currentSlotSize;
					data[currentSlot] = new byte[currentSlotSize];

					// System.out.format("%d = %d (%,d)%n", currentSlot, currentSlotSize, total);
					currentSlot++;
				}
			}
		}
	}

	private void random()
	{
		int SLOTS = 10_000;
		data = new Object[SLOTS];
		int avgSlotSize = MAX_SIZE / SLOTS;
		int maxSlotSize = 2 * avgSlotSize;
		
		long time = System.currentTimeMillis();
		
		while (lastSwitch + LENGTH_MS > (time = System.currentTimeMillis()))
		{
			// draw a slot
			int slot = random.nextInt(SLOTS);
			// random size
			int size = random.nextInt(maxSlotSize);
			data[slot] = new byte[size];
		}
	}

	public static void main(String[] args) {
		System.out.println("Starting Memory Allocator demo. Watch the GC activity!");

		new MemoryAllocator().run();
	}
}
