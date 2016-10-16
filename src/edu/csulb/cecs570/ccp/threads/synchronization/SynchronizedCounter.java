package edu.csulb.cecs570.ccp.threads.synchronization;

public class SynchronizedCounter {
	private int c = 0;

	public synchronized void increment() {
		c++;
	}

	public synchronized void decrement() {
		c--;
	}

	public synchronized int getCounter() {
		return c;
	}
}
