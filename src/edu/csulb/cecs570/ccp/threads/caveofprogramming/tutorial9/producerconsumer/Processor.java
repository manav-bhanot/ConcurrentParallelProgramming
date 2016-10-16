package edu.csulb.cecs570.ccp.threads.caveofprogramming.tutorial9.producerconsumer;

import java.util.LinkedList;

public class Processor {
	
	private LinkedList<Integer> list = new LinkedList<Integer>();
	
	// Size of the buffer which is a linkedlist here
	private final int LIMIT = 10;
	
	private Object lock = new Object();
	
	// Adds item to the LinkedList
	public void produce() throws InterruptedException {		
		System.out.println("Producer thread running ....");
		int value = 0;
		while (true) {
			synchronized (lock) {
				while (list.size() == 10) {
					lock.wait();
				}
				list.add(value++);
			}
		}
	}
	
	public void consume() throws InterruptedException {
		while (true) {
			System.out.println("Consumer thread running ....");
			synchronized (lock) {
				System.out.println("List size is : " + list.size());
				int value = list.removeFirst();
				System.out.println("; value is : " + value);
				lock.notify();
			}
		}
	}
}
