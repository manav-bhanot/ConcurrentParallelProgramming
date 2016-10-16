/**
 * 
 */
package edu.csulb.cecs570.ccp.tsp.recursive;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Scanner;
import java.util.Set;

/**
 * @author Manav
 *
 */
public class Graph {
	private List<String> cities;
	private int[][] adjacencyMatrix;
	private int noOfCities;

	private boolean[] visitedVertices;

	private int longestCityName = 0;

	private boolean isRouteFound = false;
	
	// Stores the cost of the minimum cost route found
	private int minimumCost = Integer.MAX_VALUE;

	// Stores the node that contains the route having the minimum cost
	private Node minCostNode;

	private PriorityQueue<Node> pQueue = new PriorityQueue<Node>();

	public static void main(String[] args) {
		Graph g = new Graph();
		// g.getUserInput();

		g.adjacencyMatrix = new int[][] { { 0, 3, 4, 2, 7 }, { 3, 0, 4, 6, 3 }, { 4, 4, 0, 5, 8 }, { 2, 6, 5, 0, 6 },
				{ 7, 3, 8, 6, 0 } };
		g.cities = new ArrayList<String>();
		g.cities.add("A");
		g.cities.add("B");
		g.cities.add("C");
		g.cities.add("D");
		g.cities.add("E");

		g.noOfCities = g.cities.size();

		g.displayMatrix();
		// g.dfs(0);

		/*Scanner scan = new Scanner(System.in);
		System.out.println("Enter the source city from the options :");
		for (String city : g.cities) {
			System.out.print(" " + city);
		}
		System.out.println();
		String source = scan.next();*/

		// An edgeMatrix to keep track of the included and excluded edges
		int[][] edgeMatrix = new int[g.noOfCities][g.noOfCities];
		
		double cost = 0.0;
		for (int v = 0; v < g.noOfCities; v++) {
			cost += g.findSumOfTwoMinimumCostEdges(v, edgeMatrix);
		}
		NodeData value = new NodeData(edgeMatrix, cost/2);
		Node root = new Node(value, null, null, null, false);

		System.out.println("Estimated lower bound cost at root node is : " + root.value.cost);

		// Adding the root node to the queue
		g.pQueue.add(root);

		g.findOptimalTour(0, 1);

		System.out.println("Minimum route cost is : " + g.minCostNode.value.cost);
	}

	private void findOptimalTour(int i, int j) {
		// check the cost of its sibling
		// if cost of its sibling is less than its own cost then continue and do
		// not expand the tree

		// check the number of incident edges considered till now
		// i represents the node index which should be checked for the incident
		// edges
		// if the number of incident edges = 2 then do not expand this node
		// further
		// set isLeafNode = true

		// if no edges have been considered till now incident to i, and if there
		// are only 2 edges left to be considered,
		// and the cost computed is less than its sibling, then
		// increment i, i.e. consider the next city now and repeat the same
		// process
		
		while (!this.pQueue.isEmpty()) {
			
			Node node = this.pQueue.remove();
			
			if (isRouteFound && node.value.cost > minimumCost) {
				continue;
			}
			
			if (i > this.noOfCities - 1 || j > this.noOfCities - 1) continue;
			
			Set<Integer> verticesToReprocess = new HashSet<Integer>();
			
			// Check if the left child of this node is null. If it is then create the left child
			if (node.left == null) {
				
				// Create the new edge matrix for the left child
				int[][] edgeMatrix = new int[this.noOfCities][this.noOfCities];
				
				// Copies the edge matrix from the current node to the left child node
				for (int loop = 0; loop < this.noOfCities; loop++) {
					edgeMatrix[loop] = Arrays.copyOf(node.value.edgeMatrix[loop], this.noOfCities);
				}

				// Considering the edge i,j and udpdating the state if the edges considered matrix
				edgeMatrix[i][j] = edgeMatrix[j][i] = 1;
				updateEdgeMatrix(edgeMatrix, verticesToReprocess);

				for (int v : verticesToReprocess) {
					updateEdgeMatrix(edgeMatrix, v);
				}

				// Clearing the set as all the vertices are now reprocessed
				verticesToReprocess.clear();

				// Check if the route is found at this node
				boolean routeFound = isRouteFound(edgeMatrix);

				double cost = 0;

				// Update the minimum cost if route is found else find the lower bound and update it in the node
				if (routeFound) {
					cost = findRouteCost(edgeMatrix);
				} else {
					for (int v = 0; v < this.noOfCities; v++) {
						cost += this.findSumOfTwoMinimumCostEdges(v, edgeMatrix);
					}
					cost = cost / 2.0;
				}
				NodeData value = new NodeData(edgeMatrix, cost);
				node.left = new Node(value, null, null, null, routeFound);

				printEdgesConsideredForThisNode(edgeMatrix);

				// If a route is found with the minimum cost, update the minimum cost and store this node
				if (routeFound && cost < minimumCost) {
					this.minimumCost = (int) cost;
					System.out.println("Minimum Cost :: left node :: " + minimumCost);
					minCostNode = node.left;
				} else {
					this.pQueue.add(node.left);
				}
				System.out.println("Cost of this node : " + cost);
			}
			
			// Check if the right child of this node is null. If yes, then create the right child
			if (node.right == null) {
				// Creating the right node
				int[][] edgeMatrix = new int[this.noOfCities][this.noOfCities];
				for (int loop = 0; loop < this.noOfCities; loop++) {
					edgeMatrix[loop] = Arrays.copyOf(node.value.edgeMatrix[loop], this.noOfCities);
				}

				// Not considering the edge i,j and creating the right node of the
				// parent
				edgeMatrix[i][j] = edgeMatrix[j][i] = -1;

				updateEdgeMatrix(edgeMatrix, verticesToReprocess);

				for (int v : verticesToReprocess) {
					updateEdgeMatrix(edgeMatrix, v);
				}

				// Clearing the set as all the vertices are now reprocessed
				verticesToReprocess.clear();

				// Check if the route is found at this node
				boolean routeFound = isRouteFound(edgeMatrix);

				double cost = 0.0;

				// Update the minimum cost if route is found
				if (routeFound) {
					cost = findRouteCost(edgeMatrix);
				} else {
					for (int v = 0; v < this.noOfCities; v++) {
						cost += this.findSumOfTwoMinimumCostEdges(v, edgeMatrix);
					}
					cost = cost / 2.0;
				}

				NodeData value = new NodeData(edgeMatrix, cost);
				node.right = new Node(value, null, null, null, routeFound);

				printEdgesConsideredForThisNode(edgeMatrix);
				if (routeFound && cost < minimumCost) {
					minimumCost = (int) cost;
					System.out.println("Minimum Cost :: right node :: " + minimumCost);
					this.minCostNode = node.right;
				} else {
					this.pQueue.add(node.right);
				}
				System.out.println("Cost of this node : " + cost + "\n");
			}
			
			if (isVertexFullyProcessed(pQueue.peek().value.edgeMatrix, i)) {
				i = i + 1;
				j = i + 1;
			} else {
				j++;
			}
			
			// Branch and Bound
			/*if (node.left.value.cost < node.right.value.cost) {
				if (isVertexFullyProcessed(node.left.value.edgeMatrix, i)) {
					i = i + 1;
					j = i;
				}

				// check if the cost of the right branch is still minimum than the
				// minimum route cost found till now
				if (node.right.value.cost > minimumCost) {
					node.right.isLeafNode = true;
				} else {
					findOptimalTour(parent.right, null, null, i, j + 1);
				}
			} else {
				if (isVertexFullyProcessed(node.right.value.edgeMatrix, i)) {
					i = i + 1;
					j = i;
				}
				// Expand the right branch of the tree by recursively calling the
				// findOptimalTour function
				//findOptimalTour(parent.right, null, null, i, j + 1);

				// check if the cost of the right branch is still minimum than the
				// minimum route cost found till now
				if (parent.left.value.cost > minimumCost) {
					parent.left.isLeafNode = true;
				} else {
					findOptimalTour(parent.left, null, null, i, j + 1);
				}
			}*/
		}
		
		// Obsolete Code
		/*System.out.println("" + i + j);

		if (i >= this.noOfCities || j >= this.noOfCities) {
			return;
		}

		Set<Integer> verticesToReprocess = new HashSet<Integer>();

		if (!parent.isLeafNode && left == null) {
			// Creating the left node
			int[][] edgeMatrix = new int[this.noOfCities][this.noOfCities];
			for (int loop = 0; loop < this.noOfCities; loop++) {
				edgeMatrix[loop] = Arrays.copyOf(parent.value.edgeMatrix[loop], this.noOfCities);
			}

			// Considering the edge i,j and creating the left node of the parent
			edgeMatrix[i][j] = edgeMatrix[j][i] = 1;

			updateEdgeMatrix(edgeMatrix, verticesToReprocess);

			for (int v : verticesToReprocess) {
				updateEdgeMatrix(edgeMatrix, v);
			}

			// Clearing the set as all the vertices are now reprocessed
			verticesToReprocess.clear();

			// Check if the route is found at this node
			boolean routeFound = isRouteFound(edgeMatrix);

			int cost = 0;

			// Update the minimum cost if route is found
			if (routeFound) {
				cost = findRouteCost(edgeMatrix);
			} else {
				for (int v = 0; v < this.noOfCities; v++) {
					cost += this.findSumOfTwoMinimumCostEdges(v, edgeMatrix);
				}
				cost = (int) Math.ceil(cost / 2.0);
			}
			NodeData value = new NodeData(edgeMatrix, cost, 0);
			parent.left = new Node(value, null, null, null, routeFound);

			printEdgesConsideredForThisNode(edgeMatrix);

			if (routeFound && cost < minimumCost) {
				this.minimumCost = cost;
				System.out.println("Minimum Cost :: left node :: " + minimumCost);
				minCostNode = parent.left;
			}
			System.out.println("Cost of this node : " + cost);
		}
		if (!parent.isLeafNode && right == null) {
			// Creating the right node
			int[][] edgeMatrix = new int[this.noOfCities][this.noOfCities];
			for (int loop = 0; loop < this.noOfCities; loop++) {
				edgeMatrix[loop] = Arrays.copyOf(parent.value.edgeMatrix[loop], this.noOfCities);
			}

			// Not considering the edge i,j and creating the right node of the
			// parent
			edgeMatrix[i][j] = edgeMatrix[j][i] = -1;

			updateEdgeMatrix(edgeMatrix, verticesToReprocess);

			for (int v : verticesToReprocess) {
				updateEdgeMatrix(edgeMatrix, v);
			}

			// Clearing the set as all the vertices are now reprocessed
			verticesToReprocess.clear();

			// Check if the route is found at this node
			boolean routeFound = isRouteFound(edgeMatrix);

			int cost = 0;

			// Update the minimum cost if route is found
			if (routeFound) {
				cost = findRouteCost(edgeMatrix);
			} else {
				for (int v = 0; v < this.noOfCities; v++) {
					cost += this.findSumOfTwoMinimumCostEdges(v, edgeMatrix);
				}
				cost = (int) Math.ceil(cost / 2.0);
			}

			NodeData value = new NodeData(edgeMatrix, cost, 0);
			parent.right = new Node(value, null, null, null, routeFound);

			printEdgesConsideredForThisNode(edgeMatrix);
			if (routeFound && cost < minimumCost) {
				minimumCost = cost;
				System.out.println("Minimum Cost :: right node :: " + minimumCost);
				this.minCostNode = parent.right;
			}
			System.out.println("Cost of this node : " + cost + "\n");
		}

		// Branch and Bound
		if (parent.left.value.cost < parent.right.value.cost) {
			if (parent.left.isLeafNode)
				return;
			if (isVertexFullyProcessed(parent.left.value.edgeMatrix, i)) {
				i = i + 1;
				j = i;
			}
			// Expand the left branch of the tree by recursively calling the
			// findOptimalTour function
			findOptimalTour(parent.left, null, null, i, j + 1);

			// check if the cost of the right branch is still minimum than the
			// minimum route cost found till now
			if (parent.right.value.cost > minimumCost) {
				parent.right.isLeafNode = true;
			} else {
				findOptimalTour(parent.right, null, null, i, j + 1);
			}
		} else {
			if (parent.right.isLeafNode)
				return;
			if (isVertexFullyProcessed(parent.right.value.edgeMatrix, i)) {
				i = i + 1;
				j = i;
			}
			// Expand the right branch of the tree by recursively calling the
			// findOptimalTour function
			findOptimalTour(parent.right, null, null, i, j + 1);

			// check if the cost of the right branch is still minimum than the
			// minimum route cost found till now
			if (parent.left.value.cost > minimumCost) {
				parent.left.isLeafNode = true;
			} else {
				findOptimalTour(parent.left, null, null, i, j + 1);
			}
		}*/
	}

	private void printEdgesConsideredForThisNode(int[][] edgeMatrix) {
		StringBuilder edgesConsidered = new StringBuilder();
		StringBuilder edgesNotConsidered = new StringBuilder();
		for (int i = 0; i < this.noOfCities; i++) {
			for (int j = i; j < this.noOfCities; j++) {
				if (edgeMatrix[i][j] == 1) {
					edgesConsidered.append(" (" + this.cities.get(i) + "-" + this.cities.get(j) + ")");
				} else if (edgeMatrix[i][j] == -1) {
					edgesNotConsidered.append(" (" + this.cities.get(i) + "-" + this.cities.get(j) + ")");
				}
			}
		}
		System.out.println("Edges considered : " + edgesConsidered.toString());
		System.out.println("Edges Not considered : " + edgesNotConsidered.toString());
	}

	/**
	 * Checks if this vertex has been fully processed
	 * A vertex is fully processed if we have considered all the possibilites of finding a route passing through that vertex
	 * Returns true if the vertex is fully processed
	 * 
	 * @param edgeMatrix
	 * @param i
	 * @return
	 */
	private boolean isVertexFullyProcessed(int[][] edgeMatrix, int i) {

		int edgesConsidered = 0;

		for (int j = 0; j < this.noOfCities; j++) {
			if (i != j && edgeMatrix[i][j] == 1) {
				edgesConsidered++;
			}
		}
		if (edgesConsidered == 2) {
			return true;
		}
		return false;
	}

	private int findRouteCost(int[][] edgeMatrix) {
		int cost = 0;
		for (int i = 0; i < this.noOfCities; i++) {
			for (int j = i; j < this.noOfCities; j++) {
				if (edgeMatrix[i][j] == 1) {
					cost += adjacencyMatrix[i][j];
				}
			}
		}
		return cost;
	}

	/**
	 * Checks if a route has been found at this node We have found a route if
	 * every vertex has exactly 2 edges incident with it
	 * 
	 * @param edgeMatrix
	 * @return
	 */
	private boolean isRouteFound(int[][] edgeMatrix) {

		for (int i = 0; i < this.noOfCities; i++) {

			// Tracks the total number of edges that should be incident with
			// this vertex i
			int edgesConsidered = 0;

			for (int j = 0; j < this.noOfCities; j++) {
				if (i != j && edgeMatrix[i][j] == 1) {
					edgesConsidered++;
				}
			}
			if (edgesConsidered != 2) {
				return false;
			}
		}
		return true;
	}

	private void updateEdgeMatrix(int[][] edgeMatrix, Set<Integer> s) {

		for (int i = 0; i < this.noOfCities; i++) {
			// Tracks the total number of edges that should be incident with
			// this vertex i
			int edgesConsidered = 0;

			// Tracks the remaining edges to be considered
			int remainingEdges = 0;

			for (int j = 0; j < this.noOfCities; j++) {
				if (i != j && edgeMatrix[i][j] == 1) {
					edgesConsidered++;
				} else if (i != j && edgeMatrix[i][j] == 0) {
					remainingEdges++;
				}
			}
			if (edgesConsidered < 2) {
				if ((remainingEdges == 2 && edgesConsidered == 0) || (remainingEdges == 1 && edgesConsidered == 1)) {
					for (int j = 0; j < this.noOfCities; j++) {
						if (i != j && edgeMatrix[i][j] == 0) {
							edgeMatrix[i][j] = 1;
							edgeMatrix[j][i] = 1;
							s.add(j);
						}
					}
				}
			} else {
				for (int j = 0; j < this.noOfCities; j++) {
					if (i != j && edgeMatrix[i][j] == 0) {
						edgeMatrix[i][j] = -1;
						edgeMatrix[j][i] = -1;
						s.add(j);
					}
				}
			}
		}
	}

	private void updateEdgeMatrix(int[][] edgeMatrix, int v) {

		// Tracks the total number of edges that should be incident with
		// this vertex i
		int edgesConsidered = 0;

		// Tracks the remaining edges to be considered
		int remainingEdges = 0;

		for (int j = 0; j < this.noOfCities; j++) {
			if (v != j && edgeMatrix[v][j] == 1) {
				edgesConsidered++;
			} else if (v != j && edgeMatrix[v][j] == 0) {
				remainingEdges++;
			}
		}
		if (edgesConsidered < 2) {
			if ((remainingEdges == 2 && edgesConsidered == 0) || (remainingEdges == 1 && edgesConsidered == 1)) {
				for (int j = 0; j < this.noOfCities; j++) {
					if (v != j && edgeMatrix[v][j] == 0) {
						edgeMatrix[v][j] = 1;
						edgeMatrix[j][v] = 1;
					}
				}
			}
		} else {
			for (int j = 0; j < this.noOfCities; j++) {
				if (v != j && edgeMatrix[v][j] == 0) {
					edgeMatrix[v][j] = -1;
					edgeMatrix[j][v] = -1;
				}
			}
		}
	}

	// Check whether this node has to be pruned
	private boolean isCandidateForPruning(Node node, int[][] edgeMatrix, int i) {
		int edgesConsidered = 0;
		int remainingEdges = 0;
		for (int loop = 0; loop < this.noOfCities; loop++) {
			if (i != loop && edgeMatrix[i][loop] == 1) {
				edgesConsidered++;
			} else if (i != loop && edgeMatrix[i][loop] == 0) {
				remainingEdges++;
			}
		}

		if (edgesConsidered < 2) {
			if ((remainingEdges == 2 && edgesConsidered == 0) || (remainingEdges == 1 && edgesConsidered == 1)) {
				for (int loop = 0; loop < this.noOfCities; loop++) {
					if (i != loop && edgeMatrix[i][loop] == 0) {
						edgeMatrix[i][loop] = 1;
						edgeMatrix[loop][i] = 1;
					}
					return true;
				}
			}
		} else {
			for (int loop = 0; loop < this.noOfCities; loop++) {
				if (i != loop && edgeMatrix[i][loop] == 0) {
					edgeMatrix[i][loop] = -1;
					edgeMatrix[loop][i] = -1;
				}
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns the sum of the cost of two minimum cost edges incident with the
	 * vertex v. The selection of minimum cost edges is based on a certain
	 * heuristic. If we have already selected an edge to be included in the
	 * final tour, then that edge is always considered, regardless of whether it
	 * has a minimum cost or not.
	 * 
	 * @param v
	 * @return
	 */
	private int findSumOfTwoMinimumCostEdges(int v, int[][] edgeMatrix) {

		int firstMin = Integer.MAX_VALUE;
		int secondMin = Integer.MAX_VALUE;

		boolean isFirstMinFound = false;
		boolean isSecondMinFound = false;

		for (int i = 0; i < this.noOfCities; i++) {
			if (i != v && adjacencyMatrix[v][i] != -1 && edgeMatrix[v][i] != -1) {
				if (isFirstMinFound && isSecondMinFound) {
					return firstMin + secondMin;
				}
				if (!isFirstMinFound && edgeMatrix[v][i] == 1) {
					firstMin = adjacencyMatrix[v][i];
					isFirstMinFound = true;
					continue;
				} else if (!isSecondMinFound && edgeMatrix[v][i] == 1) {
					secondMin = adjacencyMatrix[v][i];
					isSecondMinFound = true;
					continue;
				}

				if (!isFirstMinFound && adjacencyMatrix[v][i] < firstMin) {
					secondMin = firstMin;
					firstMin = adjacencyMatrix[v][i];
				} else if (!isSecondMinFound && adjacencyMatrix[v][i] < secondMin) {
					secondMin = adjacencyMatrix[v][i];
				}
			}
		}

		// System.out.println("For vertex : "+this.cities.get(v)+"--
		// firstMin="+firstMin+": secondMin="+secondMin+": cost is="+(firstMin +
		// secondMin));
		return firstMin + secondMin;
	}

	private void dfs(int i) {

		System.out.println(" " + (cities.get(i)));
		visitedVertices[i] = true;

		for (int j = 0; j < visitedVertices.length; j++) {
			if (!visitedVertices[j] && adjacencyMatrix[i][j] > -1) {
				dfs(j);
			}
		}
	}

	private void getUserInput() {
		Scanner scan = new Scanner(System.in);
		System.out.println("Enter the number of cities");
		this.noOfCities = scan.nextInt();

		// Initialize the cities array size
		cities = new ArrayList<String>();
		visitedVertices = new boolean[this.noOfCities];
		adjacencyMatrix = new int[this.noOfCities][this.noOfCities];

		for (int i = 0; i < this.noOfCities; i++) {
			System.out.println("Enter the name of the city");
			String name = scan.next();
			cities.add(name);
			longestCityName = Math.max(longestCityName, name.length());
		}

		for (int i = 0; i < this.noOfCities; i++) {
			for (int j = i + 1; j < this.noOfCities; j++) {
				System.out.println("Enter the distance between " + cities.get(i) + " and " + cities.get(j)
						+ "Enter -1 if there is no edge");
				int dist = scan.nextInt();
				adjacencyMatrix[i][j] = dist;
				adjacencyMatrix[j][i] = dist;
			}
		}
	}

	private void displayMatrix() {
		printSpaces(longestCityName + 1);
		for (int i = 0; i < cities.size(); i++) {
			System.out.print(cities.get(i) + " ");
		}

		System.out.println();

		for (int i = 0; i < cities.size(); i++) {
			System.out.print(cities.get(i));
			printSpaces(longestCityName - cities.get(i).length() + 1);
			for (int j = 0; j < cities.size(); j++) {
				System.out.print(adjacencyMatrix[i][j]);
				printSpaces(cities.get(i).length());
			}
			System.out.println();
		}

	}

	private void printSpaces(int s) {
		for (int i = 0; i < s; i++) {
			System.out.print(" ");
		}
	}
}

class NodeData {
	int[][] edgeMatrix;
	double cost;
	int incidentEdges;

	public NodeData(int[][] edgeMatrix, double cost) {
		super();
		this.edgeMatrix = edgeMatrix;
		this.cost = cost;
	}
}

class Node implements Comparable<Node> {
	NodeData value;
	Node left;
	Node right;
	Node parent;
	boolean isLeafNode;

	public Node() {
	}

	public Node(NodeData value, Node left, Node right, Node parent, boolean isLeafNode) {
		super();
		this.value = value;
		this.left = left;
		this.right = right;
		this.parent = parent;
		this.isLeafNode = isLeafNode;
	}

	@Override
	public int compareTo(Node node) {
		if (this.value.cost < node.value.cost) {
			return -1;
		} else if (this.value.cost > node.value.cost) {
			return 1;
		} else {
			return 0;
		}
	}
}
