package edu.csulb.cecs570.ccp.threads.synchronization;

public class AccessSynchronizedCounter {
	// Display a message preceded by the name of the current thread
	static void printCounter(int val) {
		String threadName = Thread.currentThread().getName();
		System.out.format("%s: %s%n", threadName, val + "");
	}

	public static void main(String[] args) {
		SynchronizedCounter c = new SynchronizedCounter();

		Thread t1 = new Thread(new SyncThread1(c));
		Thread t2 = new Thread(new SyncThread2(c));

		t1.start();
		t2.start();
	}

}

class SyncThread1 implements Runnable {

	SynchronizedCounter c;

	public SyncThread1(SynchronizedCounter c) {
		this.c = c;
	}

	@Override
	public void run() {
		// Counter c = new Counter();
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

class SyncThread2 implements Runnable {

	SynchronizedCounter c;

	public SyncThread2(SynchronizedCounter c) {
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