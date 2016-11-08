package com.openmp;

import java.security.SecureRandom;

import mpi.*;

class StatusCheck {
	static public void main(String[] args) throws MPIException {
		// Initialize the MPI environment
		MPI.Init(args);

		// Find out rank, size
		int world_rank = MPI.COMM_WORLD.Rank();
		int world_size = MPI.COMM_WORLD.Size();
		if (world_size != 2) {
			System.out.println("World size must be two for " + args[0] + "\n");
			MPI.COMM_WORLD.Abort(1);
		}
		int MAX_NUMBERS = 100;
		int[] numbers = new int[MAX_NUMBERS];
		int number_amount;
		if (world_rank == 0) {
			// Pick a random amount of integers to send to process one
			SecureRandom s = new SecureRandom();
			number_amount = s.nextInt(50);

			// Send the amount of integers to process one
			MPI.COMM_WORLD.Send(numbers, 0, number_amount, MPI.INT, 1, 5);
			System.out.println("0 sent " + number_amount + " numbers to 1\n");

		} else if (world_rank == 1) {
			// Receive at most MAX_NUMBERS from process zero
			Status status = MPI.COMM_WORLD.Recv(numbers, 0, MAX_NUMBERS, MPI.INT, MPI.ANY_SOURCE, MPI.ANY_TAG);

			// After receiving the message, check the status to determine how
			// many numbers were actually received
			number_amount = status.Get_count(MPI.INT);

			// Print off the amount of numbers, and also print additional
			// information in the status object
			System.out.println("1 received " + number_amount + " numbers from 0. Message source = " + status.source
					+ ", tag = " + status.tag + "\n");
		}
		MPI.COMM_WORLD.Barrier();
		MPI.Finalize();
	}
}