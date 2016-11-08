package com.openmp;

import mpi.MPI;

public class HelloWorld {

	public static void main(String[] args) {
		
		MPI.Init(args);
		
		int me = MPI.COMM_WORLD.Rank();
		int size = MPI.COMM_WORLD.Size();
		
		//System.out.println("Size is : "+size);
		
		System.out.println("Hi from <" + me + "> with total size : " + size);
		
		if (me == 0) {
			System.out.println("This statement is executed by the processor of rank <" + me + ">");
		}
		
		MPI.Finalize();
		
	}
}
