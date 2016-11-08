package com.openmp;

import mpi.MPI;

public class PingPong {
	public static void main(String[] args) {
		int PING_PONG_LIMIT = 10;

		// Initialize the MPI environment
		MPI.Init(args);

		// Find out rank, size
		int world_rank = MPI.COMM_WORLD.Rank();
		int world_size = MPI.COMM_WORLD.Size();

		// We are assuming at least 2 processes for this task

		if (world_size != 2) {
			System.out.println("World size must be two for " + args[0] + "\n");
			MPI.COMM_WORLD.Abort(1);
		}
		int[] ping_pong_count = new int[] { 0 };
		int partner_rank = (world_rank + 1) % 2;
		while (ping_pong_count[0] < PING_PONG_LIMIT) {
			if (world_rank == ping_pong_count[0] % 2) {

				// Increment the ping pong count before you send it
				ping_pong_count[0]++;
				MPI.COMM_WORLD.Send(ping_pong_count, 0, 1, MPI.INT, partner_rank, 0);
				System.out.println(world_rank + " sent an incremented ping_pong_count " + ping_pong_count[0] + " to "
						+ partner_rank + "\n");
			} else {
				MPI.COMM_WORLD.Recv(ping_pong_count, 0, 1, MPI.INT, partner_rank, 0);
				System.out.println(world_rank + " received ping_pong_count " + ping_pong_count[0] + " from "
						+ partner_rank + "\n");
			}
		}
		MPI.Finalize();
	}
}