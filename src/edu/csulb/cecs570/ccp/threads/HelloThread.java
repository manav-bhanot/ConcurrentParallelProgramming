package edu.csulb.cecs570.ccp.threads;

public class HelloThread extends Thread {
	
	@Override
	public void run() {
		System.out.println("Hello from a Thread!");
	}
	
	public static void main(String[] args) {
		HelloThread t = new HelloThread();
		t.start();
	}
	

}
