package com.openmp;

import java.util.Scanner;

import mpi.MPI;

public class HelloWorldGAP {

	public static void main(String[] args) {
		// Initialize the MPI environment
		MPI.Init(args);

		// Get the number of processes
		int world_size = MPI.COMM_WORLD.Size();

		// Get the rank of the process
		int world_rank = MPI.COMM_WORLD.Rank();

		// Get the name of the processor
		String processor_name = MPI.Get_processor_name();

		// Print off a hello world message
		System.out.println("Hello world from processor " + processor_name + ", rank " + world_rank + " out of "
				+ world_size + " processors\n");
		int[] number = new int[1];
		if (world_rank == 0) {
			Scanner scan = new Scanner(System.in);
			number[0] = scan.nextInt();
			MPI.COMM_WORLD.Send(number, 0, 1, MPI.INT, 1, 0);
			number[0]++;
			MPI.COMM_WORLD.Send(number, 0, 1, MPI.INT, 1, 1);
		} else if (world_rank == 1) {
			MPI.COMM_WORLD.Recv(number, 0, 1, MPI.INT, 0, MPI.ANY_TAG);
			System.out.println("Process 1 received number " + number[0] + " from process 0\n");
		}
		MPI.Finalize();
	}
}