package edu.csulb.cecs570.ccp.mpjexpress;

import java.security.SecureRandom;

import mpi.MPI;
import mpi.MPIException;

public class CMPI {
	static int m_row = 4;
	static int m_column = 4;
	static int zed = 30;
	static int matrix[][] = new int[4][4];
	int visited[] = new int[4];
	static int best_path[] = new int[4];
	static int best_cost = 9999999;
	static int size = 4;

	static void dfs(int city, int[] visited_in, int[] path_in, int path_i_in, int cost_in) {
		if (cost_in < best_cost) {
			int visited[] = new int[size + 1];
			int path[] = new int[size + 1];
			int path_i = path_i_in, cost = cost_in, i;
			for (i = 0; i < size; i++) {
				visited[i] = visited_in[i];
				path[i] = path_in[i];
			}
			visited[city] = 1;
			path[path_i] = city;
			path_i++;
			int leaf = 0;
			for (i = 0; i < size; i++) {
				if (visited[i] == 0) {
					leaf++;
					dfs(i, visited, path, path_i, cost + matrix[city][i]);
				}
			}
			if (leaf == 0) {
				cost += matrix[city][0];
				path[path_i] = 0;
				path_i++;
				if (cost < best_cost) {
					// System.out.println(“Found new best cost: %i\n”, cost);
					best_cost = cost;
					for (i = 0; i < size; i++)
						best_path[i] = path[i];
				}
			}
		}
	}

	static public void main(String[] args) throws MPIException {
		{
			int rank, size, p = 4;
			MPI.Init(args);
			size = MPI.COMM_WORLD.Size();
			rank = MPI.COMM_WORLD.Rank();
			SecureRandom s = new SecureRandom();
			if (rank == 0) {
				int i, j;
				for (i = 0; i < m_row; i++)
					for (j = 0; j < m_column; j++)
						matrix[i][j] = 0;
				for (i = 0; i < m_row; i++) {
					for (j = 0; j < i; j++) {
						if (i != j) {
							int temp = (s.nextInt(zed)) + 1;
							matrix[i][j] = temp;
							matrix[j][i] = temp;
						}
					}
				}
				for (i = 1; i < p; i++)
					MPI.COMM_WORLD.Send(matrix[i], 0, size, MPI.INT, i, 0);
				System.out.println("Matrix[" + m_row + "][" + m_column + "], Max Int: " + zed + "\n");
				for (i = 0; i < m_row; i++) {
					for (j = 0; j < m_column; j++)
						System.out.print(matrix[i][j] + "\t");
					System.out.println("\n");
					// Flush(NULL);
				}
				System.out.println("\n");
				int winner = 0;
				int node_array[] = new int[p - 1];
				int node_array_i = 0;
				for (i = 0; i < p - 1; i++)
					node_array[i] = i + 1;
				for (i = 1; i < size; i++) {
					int temp_best_cost = Integer.MAX_VALUE, node;
					node = node_array[node_array_i];
					if (node_array_i < p - 2)
						node_array_i++;
					else
						node_array_i = 0;
					int temp_best_path[] = new int[size + 1];
					MPI.COMM_WORLD.Recv(new int[]{temp_best_cost}, 0, 1, MPI.INT, node, 0);
					MPI.COMM_WORLD.Recv(temp_best_path, 0, size + 1, MPI.INT, node, 0);
					if (temp_best_cost < best_cost) {
						winner = node;
						best_cost = temp_best_cost;
						for (j = 0; j < size + 1; j++)
							best_path[j] = temp_best_path[j];
					}
					MPI.COMM_WORLD.Send(new int[]{best_cost}, 0, 1, MPI.INT, node, 0);
				}
				System.out.println("Best Path Found by node" + winner + ":\n");
				System.out.println(best_path[0]);
				for (i = 1; i < size; i++)
					System.out.println(" –> " + best_path[i]);
				System.out.println("\nBest Cost Found: " + best_cost + "\n");
			} else {
				MPI.COMM_WORLD.Recv(matrix[0], 0, m_row * m_column, MPI.INT, 0, 0);
				int i;
				for (i = rank; i < size; i += (p - 1)) {
					int[] visited = new int[size + 1];
					int path[] = new int[size + 1];
					int cost = matrix[0][i], path_i = 1;
					path[0] = 0;
					visited[0] = 1;
					dfs(i, visited, path, path_i, cost);
					MPI.COMM_WORLD.Send(new int[]{best_cost}, 0, 1, MPI.INT, 0, 0);
					MPI.COMM_WORLD.Send(best_path, 0, size, MPI.INT, 0, 0);
					MPI.COMM_WORLD.Recv(new int[]{best_cost}, 0, 1, MPI.INT, 0, 0);
				}
			}
			MPI.Finalize();
		}

	}
}