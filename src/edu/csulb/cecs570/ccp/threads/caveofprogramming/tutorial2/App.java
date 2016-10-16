/**
 * Basic Thread synchronization
 * There are 2 problems encountered  if you have more than one thread sharing the same data
 * 
 * 1. Data being cached.. 
 * Since threads have their own execution stack, so in some systems, they cache the values of the variables associated
 * with their instance and hence do not pick up the latest changes made to them by other threads.
 * Here the thread might decide to cache the value of the variable running when its start and never see the changes made to it to the same 
 * variable. Hence the changes to start may not be visible to Processor thread
 * To make that make that variable volatile
 * 
 * Solution -> volatile keyword
 * By using volatile -> it prevents thread from caching the variables so that the changes made by other threads to the shared variable can be
 * seen by the current thread
 * 
 * 
 * 
 * 2. Threads Interleaving - discussed in tutorial 3
 */

package edu.csulb.cecs570.ccp.threads.caveofprogramming.tutorial2;

import java.util.Scanner;

class Processor extends Thread {
	
	private volatile boolean running = true;
	
	public void run() {
		while (running) {
			System.out.println("Hello");
			
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public void shutdown() {
		running = false;
	}
}

public class App {
	
	public static void main(String[] args) {
		
		Processor proc1 = new Processor();
		proc1.start();
		
		System.out.println("Press Enter to stop the thread");
		Scanner scan = new Scanner(System.in);
		scan.nextLine();
		
		proc1.shutdown();
	}
}
