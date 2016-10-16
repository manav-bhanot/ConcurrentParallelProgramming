package edu.csulb.cecs570.ccp.threads;

public class SleepMessages {

	public static void main(String[] args) throws InterruptedException {

		String[] importantInfo = { "Mares eat oats", "Does eat oats", "Little lambs eat ivy",
				"A kid will eat ivy too" };
		
		for (int i=0; i < importantInfo.length; i++) {
			
			// Pause for 4 seconds
			/**
			 * Thread.sleep causes the current thread to suspend execution for a specified period. 
			 *  This is an efficient means of making processor time available to the other threads of an 
			 *  application or other applications that might be running on a computer system. 
			 *  
			 *  Two overloaded versions of sleep are provided: one that specifies the sleep time to the millisecond 
			 *  and one that specifies the sleep time to the nanosecond. 
			 *  However, these sleep times are not guaranteed to be precise, because they are limited by the 
			 *  facilities provided by the underlying OS. Also, the sleep period can be terminated by interrupts, 
			 *  as we'll see in a later section. 
			 *  In any case, you cannot assume that invoking sleep will suspend the thread for precisely the 
			 *  time period specified.
			 *  
			 *  
			 *  InterruptedException : This is an exception that sleep throws when another thread interrupts 
			 *  the current thread while sleep is active. Since this application has not defined another thread to 
			 *  cause the interrupt, it doesn't bother to catch InterruptedException.
			 */
			
			try {
				Thread.sleep(4000);
			} catch (InterruptedException e) {
				// We'he been interrupted.
				// No more messages
				e.printStackTrace();
				return;
			}
			
			
			// Prints a message
			System.out.println(importantInfo[i]);
		}
	}

}
