package edu.csulb.cecs570.ccp.threads.caveofprogramming.tutorial4;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Worker does some processing which can be divided into 2 stages - stage 1 and stage 2
 * 
 * the process methods first runs the stageOne and then runs the stageTwo
 * 
 * Observe carefully, a single thread will take roughly 2 seconds to complete the process
 * 
 * If you make two threads to do the same process,
 * 
 * If the stageOne and stageTwo methods are not synchronized, then thread interleaving occurs which may lead to unexpected results
 * but the overall time taken to run the process is roughly 2 secs by each thread. Why? because they are running parallerly
 * 
 * If the stageOne and stageTwo methods are synchronized, then it will take roughly 4 seconds to complete the process
 * Why ? -> because when the first thread starts executing the process which is inside a loop of 1000, it acquires the intrinsic lock
 * on the Worker object. Thus at that point when you start the other thread to execute the same process, it will wait for the first
 * thread to release the lock on the worker object. Both the threads need to acquire the same lock on the worker object. Once acquired,
 * a thread won't release that lock until the loop in which process is being called finishes
 * 
 * But there is one more issue. When the first thread is running stageOne, then the second thread can actually run stageTwo because
 * both these stages are independent (they are modifying different lists). 
 * 
 * 
 * Solution : use different lock objects
 * 
 * You can also lock on the list object. But that is not a good practice
 * 
 * @author Manav
 *
 */
public class Worker {
	
	private Random random = new Random();
	
	private List<Integer> list1 = new ArrayList<Integer>();
	private List<Integer> list2 = new ArrayList<Integer>();
	
	private Object lock1 = new Object();
	private Object lock2 = new Object();
	
	public synchronized void stageOne() {
		synchronized (lock1) {
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			list1.add(random.nextInt(100));
		}			
	}
	
	public void stageTwo() {
		
		synchronized (lock2) {
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}			
			list2.add(random.nextInt(100));		
		}		
	}
	
	public void process() {
		for (int i=0; i < 1000; i++) {
			stageOne();
			stageTwo();
		}
	}
	
	public void main() {
		System.out.println("Starting....");
		
		long start = System.currentTimeMillis();
		
		Thread t1 = new Thread(new Runnable() {			
			@Override
			public void run() {
				process();
			}
		});
		
		Thread t2 = new Thread(new Runnable() {			
			@Override
			public void run() {
				process();
			}
		});
		
		t1.start();
		t2.start();		
		
		try {
			t1.join();
			t2.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		long end = System.currentTimeMillis();
		
		System.out.println("Time take = "+(end - start));
		System.out.println("List1 : " + list1.size() + "; List2 : " + list2.size());
	}

}
