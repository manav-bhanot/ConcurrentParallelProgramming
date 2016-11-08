package com.openmp;

import mpi.*;

class Ring {
	static public void main(String[] args) throws MPIException {
		// Initialize the MPI environment
		MPI.Init(args);

		// Find out rank, size
		int world_rank = MPI.COMM_WORLD.Rank();
		int world_size = MPI.COMM_WORLD.Size();

		int[] token = new int[1];
		/*
		 * Receive from the lower process and send to the higher process. Take
		 * care of the special case when you are the first process to prevent
		 * deadlock
		 */
		if (world_rank != 0) {
			MPI.COMM_WORLD.Recv(token, 0, 1, MPI.INT, world_rank - 1, 0);
			System.out.println("Process " + world_rank + " received token " + token[0] + " from process "+(world_rank - 1)+"\n");
		} else {
			// Set the token's value if you are process 0
			token[0] = -1;
		}
		MPI.COMM_WORLD.Send(token, 0, 1, MPI.INT, (world_rank + 1) % world_size, 0);

		/*
		 * Now process 0 can receive from the last process. This makes sure that
		 * at least one MPI_Send is initialized before all MPI_Recvs (again, to
		 * prevent deadlock)
		 */
		if (world_rank == 0) {
			MPI.COMM_WORLD.Recv(token, 0, 1, MPI.INT, world_size - 1, 0);
			System.out.println("Process " + world_rank + " received token " + token[0] + "from process "
					+ (world_size - 1) + "\n");
		}
		MPI.Finalize();
	}
}