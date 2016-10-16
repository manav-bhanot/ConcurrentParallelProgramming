/**
 * 
 */
package edu.csulb.cecs570.ccp;

import java.io.File;
import java.io.FileNotFoundException;
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
public class FindOptimalTour {
	
	// To check if the verbose mode is on or off
	private boolean isVersboseModeOn = true;
	
	// List to store all the cities entered by a user
	private List<String> cities;
	
	// Adjacency matrix to store the weight/cost of the edges between each pair of vertices
	private int[][] adjacencyMatrix;
	
	// Stores the total number of cities
	private int noOfCities;

	private int longestCityName = 0;

	// Checks if a route containing all the nodes is found or not.
	private volatile boolean isRouteFound = false;
	
	// Stores the cost of the minimum cost route found
	private volatile int minimumCost = Integer.MAX_VALUE;

	// Stores the node that contains the route having the minimum cost
	private volatile Node minCostNode;

	// Keeps the nodes that are created during the process of finding an optimal route
	// Priority is given to those nodes having the minimum of all the lower bounds
	private PriorityQueue<Node> pQueue = new PriorityQueue<Node>();

	// Creating a lock to provide mutually exclusive access the share data structures
	private Object lock1 = new Object();
	private Object lock2 = new Object();
	
	// A counter object that keeps track of the number of waiting threads
	private volatile int count;
	
	// Variable that stores the total number of threads started by the system
	private int totalThreads;
	
	
	/**
	 * Start the main program
	 * @param args
	 */
	public static void main(String[] args) {
		
		FindOptimalTour g = new FindOptimalTour();
		
		// Reads the input from a file
		g.readInputGraphFromFile();
		
		// An edgeMatrix to keep track of the included and excluded edges
		// This matrix is re-calculated for each node. Each node has its own
		// instance of this matrix in order to
		// efficiently track the inclusion/exclusion of edges at each node.
		int[][] edgeMatrix = new int[g.noOfCities][g.noOfCities];
		
		// Initializing edge matrix to have -1 between those vertices for which no edge exists
		for (int i=0; i<g.noOfCities; i++) {
			for (int j=0; j<g.noOfCities; j++) {
				if (g.adjacencyMatrix[i][j] == -1) edgeMatrix[i][j] = -1;
			}
		}

		// Display the adjacency matrix
		if (g.isVersboseModeOn) g.displayMatrix(g.adjacencyMatrix);		
		
		// Finding the lower bound at the root node
		double cost = 0.0;
		for (int v = 0; v < g.noOfCities; v++) {
			cost += g.findSumOfTwoMinimumCostEdges(v, edgeMatrix);
		}
		
		// Creating the root node that considers all the possible routes
		NodeData value = new NodeData(edgeMatrix, cost/2);
		Node root = new Node(value, null, null, null, false);

		System.out.println("Estimated lower bound cost at root node is : " + root.value.lowerBound);

		// Adding the root node to the queue
		g.pQueue.add(root);	
		
		// Create the desired number of threads as entered by the user
		Thread th[] = new Thread[g.totalThreads];
		for (int t=0; t < g.totalThreads; t++) {
			th[t] = new Thread(new Runnable() {			
				@Override
				public void run() {
					try {
						g.findOptimalTour();
					} catch (InterruptedException e) {
						System.out.println("Thread is interrupted");
					}				
				}
			});
		}
		
		try {
			// Start the threads
			for (int t=0; t < g.totalThreads; t++) {
				th[t].start();
				th[t].join();
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		g.printOptimalTour(g.minCostNode);
	}
	
	/**
	 * 
	 * @param i
	 * @param j
	 * @throws InterruptedException 
	 */
	private void findOptimalTour() throws InterruptedException {
		
		/**********************   START of MULTI-THREADED CODE   *************************/
		
		while (count < totalThreads) {
			
			Node topNodeInPQueue = null;
			
			synchronized (lock1) {
				while (this.pQueue.isEmpty()) {
					count++;
					if (count >= totalThreads) {
						lock1.notifyAll();
						return;
					}
					lock1.wait();
				}	
				
				//System.out.println("Decremented Count is : "+count);
				topNodeInPQueue = this.pQueue.remove();
				
				// Now that it has popped the node from the shared PQueue, it will notify any waiting threads to access PQ now.
				// But the lock will be released only after the synchronized block ends its execution
				lock1.notifyAll();
			}
			
			if (this.isRouteFound && topNodeInPQueue.value.lowerBound > this.minimumCost) {
				System.out.println("\nNode with lower bound " + topNodeInPQueue.value.lowerBound + " is pruned");
				continue;
			}
			
			// Gets the edge to be processed based on the edge matrix stored in this node
			int i=0; int j=0;
			boolean check = false;
			for (int v = 0; v < this.noOfCities; v++) {
				if (!isVertexFullyProcessed(topNodeInPQueue.value.edgeMatrix, v)) {
					i = v;
					for (int col = i + 1; col < this.noOfCities; col++) {
						if (topNodeInPQueue.value.edgeMatrix[i][col] == 0) {
							j = col;
							check = true;
							break;
						}
					}
				}
				if (check)
					break;
			}
			
			// Check if the left child of this node is null. If it is then create the left child
			if (topNodeInPQueue.left == null) {		
				generateChildNode(topNodeInPQueue, i, j, true);
			}
			
			// Check if the right child of this node is null. If yes, then create the right child
			if (topNodeInPQueue.right == null) {				
				generateChildNode(topNodeInPQueue, i, j, false);				
			}
		}		
		/**********************   END of MULTI-THREADED CODE   *************************/
	}
	
	private void generateChildNode(Node parentNode, int i, int j, boolean isEdgeIncluded) {
		// Create the new edge matrix for the left child
		int[][] edgeMatrix = new int[this.noOfCities][this.noOfCities];		

		// Keeping track of vertices whose edges are implicitly included/excluded due to the inclusion/exclusion of edge of other vertices
		Set<Integer> verticesToReprocess = new HashSet<Integer>();
		
		// Copies the edge matrix from the current node to the left child node
		for (int loop = 0; loop < this.noOfCities; loop++) {
			edgeMatrix[loop] = Arrays.copyOf(parentNode.value.edgeMatrix[loop], this.noOfCities);
		}

		// Considering the edge i,j and updating the state of the edge matrix
		if (isEdgeIncluded) edgeMatrix[i][j] = edgeMatrix[j][i] = 1;
		else edgeMatrix[i][j] = edgeMatrix[j][i] = -1;
		
		updateEdgeMatrix(edgeMatrix, verticesToReprocess);

		// Updating the edge matrix for the edges present in the verticesToReprocess set
		for (int v : verticesToReprocess) {
			updateEdgeMatrix(edgeMatrix, v);
		}

		// Clearing the set as all the vertices are now reprocessed
		verticesToReprocess.clear();

		// Check if the route is found at this node
		boolean routeFound = isRouteFound(edgeMatrix);

		double routeCost = 0;

		// Update the minimum cost if route is found else find the lower bound and update it in the node
		if (routeFound) {
			routeCost = findRouteCost(edgeMatrix);
		} else {
			for (int v = 0; v < this.noOfCities; v++) {
				routeCost += this.findSumOfTwoMinimumCostEdges(v, edgeMatrix);
			}
			routeCost = routeCost / 2.0;
		}
		NodeData value = new NodeData(edgeMatrix, routeCost);
		Node node = new Node(value, null, null, null, routeFound);
		
		// Appends this node to the left or right of the parent node depending on whether an edge was considered or not
		if (isEdgeIncluded) parentNode.left = node;
		else parentNode.right = node;

		// Prints all the edges considered and not considered for calculation of the lower bound on this node
		if (isVersboseModeOn) {
			System.out.println("\n");
			System.out.println("Computing the "+ (isEdgeIncluded ? "left" : "right") + " node");
			printEdgesConsideredForThisNode(edgeMatrix);
			displayMatrix(edgeMatrix);
			if (routeFound) System.out.println("Cost of the route found : "+routeCost);
			else System.out.println("Lower bound of this node : "+routeCost);
		}

		// If a route is found with the minimum cost, update the minimum cost and store this node
		
		if (routeFound && routeCost < minimumCost) {
			synchronized (lock2) {
				this.minimumCost = (int) routeCost;
				this.minCostNode = node;
				this.isRouteFound = routeFound;
			}
		} else if (routeCost < minimumCost) {
			// Adds this node in the queue since no route is found till now and
			// so this node is a potential candidate for
			// future processing.
			// However, this node should only be added if it has a lower bound <
			// cost of the mincost route found till now
			synchronized (lock1) {
				this.pQueue.add(node);
				/*count--;
				System.out.println("Decremented Count is : "+count);*/
				lock1.notifyAll();				
			}
		}
	}
	
	/**
	 * Checks if this vertex has been fully processed
	 * A vertex is fully processed if we have considered all the possibilities of finding a route passing through that vertex
	 * Returns true if the vertex is fully processed
	 * 
	 * @param edgeMatrix
	 * @param i
	 * @return
	 */
	private boolean isVertexFullyProcessed(int[][] edgeMatrix, int i) {

		int remainingEdges = 0;
		int edgesConsidered = 0;
		int edgesNotConsidered = 0;

		for (int j = 0; j < this.noOfCities; j++) {
			if (i != j && edgeMatrix[i][j] == 0) {
				remainingEdges++;
			} else if (i != j && edgeMatrix[i][j] == -1) {
				edgesNotConsidered++;
			} else if (i != j && edgeMatrix[i][j] == 1) {
				edgesConsidered++;
			}
		}
		if (remainingEdges == 0 && edgesNotConsidered == this.noOfCities - 2 && edgesConsidered == 2) {
			return true;
		}
		return false;
	}

	/**
	 * Finds the cost of the route found
	 * @param edgeMatrix
	 * @return
	 */
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
	 * This also helps us to detect partial cycles in the graph
	 * 
	 * @param edgeMatrix
	 * @return
	 */
	private boolean isRouteFound(int[][] edgeMatrix) {

		for (int i = 0; i < this.noOfCities; i++) {

			// Tracks the total number of edges that should be incident with
			// this vertex i
			int remainingEdges = 0;
			int edgesConsidered = 0;
			int edgesNotConsidered = 0;

			for (int j = 0; j < this.noOfCities; j++) {
				if (i != j && edgeMatrix[i][j] == 0) {
					remainingEdges++;
				} else if (i != j && edgeMatrix[i][j] == -1) {
					edgesNotConsidered++;
				} else if (i != j && edgeMatrix[i][j] == 1) {
					edgesConsidered++;
				}
			}
			
			if (remainingEdges != 0 || edgesNotConsidered != this.noOfCities - 3 || edgesConsidered != 2) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Updates the state of all the edges incident with all the vertices in the graph.
	 * edgeMatrix[i,j] == -1 => this edge is not considered in the tour
	 * edgeMatrix[i,j] == 1 => this edge must be considered in the final tour
	 * Also takes care of the implicit exclusion inclusion of edges as a result of inclusion and exclusion of edge 
	 * under consideration
	 * 
	 * @param edgeMatrix
	 * @param s
	 */
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

	/**
	 * Updates the edge inclusion/exclusion flag for all the edges incident with this vertex (v)
	 * @param edgeMatrix
	 * @param v
	 */
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
		return firstMin + secondMin;
	}
	
	/**
	 * Prints all the edges that are considered along with the ones that are not considered for calculation of lower bound on this node
	 * @param edgeMatrix
	 */
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
		System.out.println("Edges considered :" + edgesConsidered.toString());
		System.out.println("Edges Not considered :" + edgesNotConsidered.toString());
	}

	/**
	 * Displays the adjacency matrix representing the graph
	 */
	private void displayMatrix(int[][] matrix) {
		
		System.out.println("Printing the adjacency matrix representing the graph.\n");
		
		printSpaces(this.longestCityName + 2);
		for (int i = 0; i < this.cities.size(); i++) {
			System.out.print(this.cities.get(i) + " ");
		}

		System.out.println();

		for (int i = 0; i < this.cities.size(); i++) {
			System.out.print(this.cities.get(i));
			printSpaces(this.longestCityName - cities.get(i).length() + 2);
			
			for (int j = 0; j < this.cities.size(); j++) {
				System.out.print(matrix[i][j]);
				printSpaces(this.cities.get(i).length());
			}
			System.out.println();
		}
		System.out.println("\n");

	}
	
	@SuppressWarnings("unused")
	private void printNodeInfo(Node node) {
		System.out.println("\nBelow node is now popped from the pQueue");
		displayMatrix(node.value.edgeMatrix);
		printEdgesConsideredForThisNode(node.value.edgeMatrix);
		System.out.println("Lower bound of this node is : "+node.value.lowerBound);
		System.out.println("Does this node has a left child? : "+(node.left != null));
		System.out.println("Does this node has a right child? : "+(node.right != null));
		System.out.println();
	}
	
	/**
	 * Prints the minimum cost route on the console along with its cost
	 * @param minCostNode2
	 */
	private void printOptimalTour(Node node) {
		System.out.println("\n\nThe optimal path will include the below edges of the graph");
		List<String> edgesInvolvedInOptimalTour = new ArrayList<String>();
		for (int i=0; i < node.value.edgeMatrix[0].length; i++) {
			for (int j=i+1; j < node.value.edgeMatrix[i].length; j++) {
				if (node.value.edgeMatrix[i][j] == 1) {
					edgesInvolvedInOptimalTour.add(this.cities.get(i) + this.cities.get(j));
					System.out.print(" "+this.cities.get(i) + this.cities.get(j));
				}
			}
		}
		System.out.println("\nOptimal Route Cost is : "+node.value.lowerBound);
	}
	
	@SuppressWarnings("resource")
	private void readInputGraphFromFile() {
		Scanner scan = new Scanner(System.in);
		System.out.println("Enter the number of cities");
		this.noOfCities = scan.nextInt();

		// Initialize the cities array size
		this.cities = new ArrayList<String>();
		
		// Add cities in the cities array.. starting from A
		for (int b = 0; b < this.noOfCities; b++) {
			this.cities.add((char)(b+65) + "");
		}
		
		this.adjacencyMatrix = new int[this.noOfCities][this.noOfCities];
		
		boolean isValidPath = false;
		String graph = "";
		
		while (!isValidPath) {
			System.out.println("Enter the path to the input text file containing the graph represented as an adjacency matrix");
			graph = scan.next().trim();
			if (graph.contains(".txt")) {
				isValidPath = true;
			} else {
				System.out.println("Invalid path");
			}
		}

		try {
			Scanner readGraph = new Scanner(new File(graph));
			int i=0;
			int j=0;
			while (readGraph.hasNextLine()) {
				String row = readGraph.nextLine();
				String[] cols = row.split(" ");
				
				for (String col : cols) {
					int cost = Integer.parseInt(col.trim());
					this.adjacencyMatrix[i][j] = cost;
					j++;
				}
				i++;
				j = 0;
			}
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}	
		
		for (String name : this.cities) {
			this.longestCityName = Math.max(this.longestCityName, name.length());
		}
		
		System.out.println("Enter the total number of threads that you want ? ");
		this.totalThreads = Integer.parseInt(scan.next());
		
		System.out.println("Do you want to switch the Verbose mode on (Type Y for yes and N for No) : ");
		isVersboseModeOn = scan.next().equalsIgnoreCase("y") ? true : false;
		
	}

	private void printSpaces(int s) {
		for (int i = 0; i < s; i++) {
			System.out.print(" ");
		}
	}
}

/**
 * Stores the edgeMatrix and the lower bound calculated at each of the node
 * @author Manav
 *
 */
class NodeData {
	int[][] edgeMatrix;
	double lowerBound;

	public NodeData(int[][] edgeMatrix, double cost) {
		super();
		this.edgeMatrix = edgeMatrix;
		this.lowerBound = cost;
	}
}

/**
 * Represents the current node and its left and right children
 * @author Manav
 *
 */
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
		if (this.value.lowerBound < node.value.lowerBound) {
			return -1;
		} else if (this.value.lowerBound > node.value.lowerBound) {
			return 1;
		} else {
			return 0;
		}
	}
}
