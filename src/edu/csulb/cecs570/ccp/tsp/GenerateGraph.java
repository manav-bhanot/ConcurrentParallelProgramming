package edu.csulb.cecs570.ccp.tsp;

import java.util.Random;

public class GenerateGraph {
	
	private static Random random;
	private static int[][] graph;
	
	private static int n = 15;

	public static void main(String[] args) {
		
		random = new Random();
		graph = new int[n][n];
		int min = 1;
		
		for (int i=0; i < n; i++) {
			for (int j = i+1; j < n; j++) {
				graph[i][j] = graph[j][i] = random.nextInt(9) + min;				
			}
		}
		for (int i=0; i < n; i++) {
			for (int j=0; j < n; j++) {
				System.out.print(graph[i][j] + " ");				
			}
			System.out.println();
		}
	}
}
