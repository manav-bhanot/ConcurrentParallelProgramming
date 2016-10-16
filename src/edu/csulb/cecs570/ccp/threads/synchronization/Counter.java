package edu.csulb.cecs570.ccp.threads.synchronization;

public class Counter {
	
	private int c = 0;
	
	public void increment() {
		c++;
	}
	
	public void decrement() {
		c--;
	}
	
	public int getCounter() {
		return c;
	}

}
