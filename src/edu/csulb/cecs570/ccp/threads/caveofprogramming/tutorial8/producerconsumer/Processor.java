package edu.csulb.cecs570.ccp.threads.caveofprogramming.tutorial8.producerconsumer;

import java.util.Scanner;

public class Processor {
	
	public void produce() throws InterruptedException {
		
		synchronized (this) {
			System.out.println("Producer thread running ....");
			
			// Every object has a wait() method
			// You can only call wait from a synchronized scope
			// It waits such that a lot of system resources are not consumed
			// Very resource efficient
			// Hands over the control of the lock held by the synchronized block
			// After executing wait, this thread will not resume until 2 things happen
			// 1. It can regain control as there is still more code to execute after wait
			// 2. Another thread who has taken a lock on this object calls the notify metho
			wait();
			
			System.out.println("Resumed execution");
		}
		
	}
	
	public void consume() throws InterruptedException {
		
		Scanner scanner = new Scanner(System.in);		
		Thread.sleep(2000);
		
		synchronized (this) {
			System.out.println("Waiting for return key");
			scanner.nextLine();
			System.out.println("Return key pressed");
			notify(); //notifyAll() 
			// Notify does not relinquish control of this lock. So it should be the last statement in
			// method block
		}
	}

}
