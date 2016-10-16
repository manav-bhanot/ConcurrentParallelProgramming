package edu.csulb.cecs570.ccp.threads.caveofprogramming.tutorial7.producerconsumer;

import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * 
 * @author Manav
 *
 */
public class App {
	
	// Holds data items of type Integer
	// Add and remove items.. FIFO queue
	// These are thread safe classes
	// You do not need to worry about thread synchronization
	private static BlockingQueue<Integer> queue = new ArrayBlockingQueue<Integer>(10);
	
	public static void main(String[] args) {
		
		// t1 is the producer
		Thread t1 = new Thread(new Runnable() {			
			@Override
			public void run() {
				producer();				
			}
		});
		
		// t2 is the consumer
		Thread t2 = new Thread(new Runnable() {			
			@Override
			public void run() {
				consumer();				
			}
		});
		
		// Starts the producer
		t1.start();
		// Starts the consumer
		t2.start();
		
		try {
			t1.join();
			t2.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	// Generates random numbers
	// Put those random numbers in the queue
	// Producer waits if the queue is full. This wait is handled by java itself
	// Programmer does not have to do anything to handle that
	private static void producer() {		
		Random random = new Random();		
		while(true) {
			try {
				queue.put(random.nextInt(100));
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	// Consumer is slower in consuming the values from the queue
	// We are putting the consumer on sleep for 1 ms everytime
	// Also the consumer only takes a value from the queue when a random number generator generates a 0 
	// Consumer will also wait if there is nothing inside the queue to be taken.
	private static void consumer() {		
		Random random = new Random();		
		while(true) {
			try {
				Thread.sleep(100);				
				if (random.nextInt(10) == 0) {
					Integer value = queue.take();
					System.out.println("Taken value : "+value+"; Queue size is : "+queue.size());
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}
