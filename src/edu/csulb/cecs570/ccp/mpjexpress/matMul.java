
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
// HOW TO RUN
// mpjrun.sh -cp .:$JCUDA_HOME/jcuda-0.2.3.jar:$MPJ_HOME/lib/mpj.jar matMul 512 g

package edu.csulb.cecs570.ccp.mpjexpress;

import mpi.*;

public class matMul {

	static { // Change this if different location
		// System.load("/usr/local/cuda/lib64/libcudart.so.2");
	}

	/**
	 * @param args
	 *            the command line arguments
	 */
	public static void main(String[] args) {
		// TODO code application logic here

		MPI.Init(args);

		long start_MPI[] = new long[1];
		start_MPI[0] = System.nanoTime();

		int rank = MPI.COMM_WORLD.Rank();
		int size = MPI.COMM_WORLD.Size();
		
		// Number of processors which should run the this problem
		int N = 50;
		String mode = "c";
		
		int root = 0;

		float A[] = null;
		float B[] = null;
		float C[] = null;

		float Asub[] = null;
		float Csub[] = null;

		if (rank == root) {
			A = new float[N * N];
			C = new float[N * N];
		}

		B = new float[N * N];

		if (rank == root) {
			for (int i = 0; i < N; i++) {
				for (int j = 0; j < N; j++) {
					A[i * N + j] = 1;
					B[i * N + j] = 1;
				}
			}
		}

		int chunkSize = N / size;
		
		System.out.println("Chunk Size : "+chunkSize);

		Asub = new float[chunkSize * N];
		Csub = new float[chunkSize * N];

		// Broad Cast B
		MPI.COMM_WORLD.Bcast(B, 0, N * N, MPI.FLOAT, root);
		// System.out.println("Bcast B ");
		// scatteer A
		MPI.COMM_WORLD.Scatter(A, 0, chunkSize * N, MPI.FLOAT, Asub, 0, chunkSize * N, MPI.FLOAT, root);
		// System.out.println("Scater A");
		// Find Csub

		// Matix Multiplication
		long start_processing[] = new long[1];
		start_processing[0] = System.nanoTime();
		if (mode.equals("c")) {
			//// CPU ///////////////////
			//////////////////////////////////////////////////////////
			for (int i = 0; i < chunkSize; i++) {

				for (int j = 0; j < N; j++) {
					Csub[N * i + j] = 0;
					for (int k = 0; k < N; k++) {
						Csub[N * i + j] += Asub[i * N + k] * B[k * N + j];

					}

				}
			}
			///////////////////////////////////////////////////////

		} // CPU ENDS
		else if (mode.equals("g")) { // GPU CUDA

			/*
			 * final int numDev = 2; int myGPU = rank%numDev;
			 * 
			 * Csub = cudaMatMul.cuMatrixMul(Asub,B,chunkSize, N, N,myGPU);
			 */

		}
		////////// END MUTIPLICATION /////////////////////////
		long stop_processing[] = new long[1];
		stop_processing[0] = System.nanoTime() - start_processing[0];
		// Now Gather Csub at root

		MPI.COMM_WORLD.Gather(Csub, 0, chunkSize * N, MPI.FLOAT, C, 0, chunkSize * N, MPI.FLOAT, root);

		// here is C
		/*
		 * if(rank == root){ for(int i = N-1; i < N ; i ++){
		 * 
		 * for(int j = 0; j < 20 ; j ++){
		 * 
		 * System.out.print(C[i*N+j]+"  ");
		 * 
		 * 
		 * } System.out.println("");
		 * 
		 * } }
		 */

		long stop_MPI[] = new long[1];
		stop_MPI[0] = System.nanoTime() - start_MPI[0];
		long avg_MPI[] = new long[1];
		long avg_Processing[] = new long[1];

		// System.out.println(" MPI TIME = "+stop_MPI[0]+" Rank = "+rank);
		// System.out.println("proce TIME = "+stop_processing[0]+" Rank =
		// "+rank);

		MPI.COMM_WORLD.Reduce(stop_MPI, 0, avg_MPI, 0, 1, MPI.LONG, MPI.SUM, root);
		MPI.COMM_WORLD.Reduce(stop_processing, 0, avg_Processing, 0, 1, MPI.LONG, MPI.SUM, root);

		MPI.Finalize();

		if (rank == root) {

			// System.out.println("SUM MPI TIME = "+avg_MPI[0]);

			avg_MPI[0] = avg_MPI[0] / size;
			avg_Processing[0] = avg_Processing[0] / size;
			System.out.println(" ======================= STATISTICS =========================");
			System.out.println(" MATIX SIZE = " + N + " X " + N);
			System.out.println(" MPJ PROCESSES COUNT = " + size);
			System.out.println(" COMPUTATION DONE ON " + mode);
			System.out.println(" PROGRAM TOTAL TIME " + avg_MPI[0] / (1000 * 1000 * 1000.0));
			System.out.println(" PROGRAM COMPUTATION TIME " + avg_Processing[0] / (1000 * 1000 * 1000.0));
			System.out.println(" ================ FINISHED - BY BIBRAK QAMAR =================");

		}

	} // main ends

}
