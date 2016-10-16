package edu.csulb.cecs570.ccp.threads;

public class HelloRunnable implements Runnable {

	@Override
	public void run() {
		System.out.println("Hello from a Thread!");		
	}
	
	public static void main(String[] args) {
		Thread t = new Thread(new HelloRunnable());
		t.start();
	}

}
