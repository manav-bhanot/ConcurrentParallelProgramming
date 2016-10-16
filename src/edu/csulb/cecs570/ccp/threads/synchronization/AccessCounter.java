package edu.csulb.cecs570.ccp.threads.synchronization;

public class AccessCounter {

	// Display a message preceded by the name of the current thread
	static void printCounter(int val) {
		String threadName = Thread.currentThread().getName();
		System.out.format("%s: %s%n", threadName, val+"");
	}
	
	public static void main(String[] args) {
		Counter c = new Counter();
		
		Thread t1 = new Thread(new Thread1(c));
		Thread t2 = new Thread(new Thread2(c));
		
		t1.start();
		t2.start();
	}

}

class Thread1 implements Runnable {
	
	Counter c;
	
	public Thread1(Counter c) {
		this.c = c;
	}

	@Override
	public void run() {
		//Counter c = new Counter();
		c.increment();
		AccessCounter.printCounter(c.getCounter());
		
		c.increment();
		AccessCounter.printCounter(c.getCounter());
		
		c.increment();
		AccessCounter.printCounter(c.getCounter());
		
		c.increment();
		AccessCounter.printCounter(c.getCounter());
		
		
	}
}

class Thread2 implements Runnable {
	
	Counter c;
	
	public Thread2(Counter c) {
		this.c = c;
	}
	@Override
	public void run() {		
		c.decrement();
		AccessCounter.printCounter(c.getCounter());
		
		c.decrement();
		AccessCounter.printCounter(c.getCounter());
		
		c.decrement();
		AccessCounter.printCounter(c.getCounter());
		
		c.decrement();
		AccessCounter.printCounter(c.getCounter());
	}
}
