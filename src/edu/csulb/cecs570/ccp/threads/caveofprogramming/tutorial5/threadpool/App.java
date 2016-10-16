package edu.csulb.cecs570.ccp.threads.caveofprogramming.tutorial5.threadpool;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * A ThreadPool is like having a number of workers in the factory
 * 
 * @author Manav
 *
 */

class Processor implements Runnable {
	
	private int id;
	
	public Processor(int id) {
		this.id = id;
	}

	@Override
	public void run() {
		System.out.println("Starting : "+id);
		
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println("Completed : "+id);
	}
}

public class App {
	public static void main(String[] args) {
		
		ExecutorService executor = Executors.newFixedThreadPool(2);
		
		for (int i=0; i < 5; i++) {
			executor.submit(new Processor(i));
		}		
		System.out.println("All tasks submitted");
		
		// Stop accepting new tasks and shutdown itself when all the tasks are finished
		// It will not shutdown the exeuctor service immediately but it will wait until all the workers have
		// completed the tasks assigned to them
		executor.shutdown();
		
		// The below statement will give you an error because we have told executor to stop accepting new tasks.
		//executor.submit(new Processor(19));
		
		try {
			executor.awaitTermination(1, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println("All tasks completed");
		
	}
}
