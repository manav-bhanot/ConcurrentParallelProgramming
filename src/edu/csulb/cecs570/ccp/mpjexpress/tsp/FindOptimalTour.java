/**
 * 
 */
package edu.csulb.cecs570.ccp.mpjexpress.tsp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;

import mpi.MPI;

/**
 * @author Manav
 *
 */
public class FindOptimalTour {
	
	private static Random random;
	private int noOfCities;
	
	// To check if the verbose mode is on or off
	private boolean isVersboseModeOn = true;
	
	// List to store all the cities entered by a user
	private List<String> cities;
	
	// Adjacency matrix to store the weight/cost of the edges between each pair of vertices
	private int[][] adjacencyMatrix;
	
	// Stores the total number of cities
	// private int noOfCities;

	private int longestCityName = 0;

	// Checks if a route containing all the nodes is found or not.
	private boolean isRouteFound = false;
	
	// Stores the cost of the minimum cost route found
	private int minimumCost = Integer.MAX_VALUE;

	// Stores the node that contains the route having the minimum cost
	private Node minCostNode;
	
	// Keeps a count of the number of processors
	private int noOfProcessors;
	
	// Keeps track of the rank of this processor
	private int myRank;

	// Keeps the nodes that are created during the process of finding an optimal route
	// Priority is given to those nodes having the minimum of all the lower bounds
	private PriorityQueue<Node> pQueue = new PriorityQueue<Node>();
	
	private long startTime;
	private long endTime;
	
	private Node[] nodeBuffer = new Node[1];
	
	
	/**
	 * Start the main program
	 * @param args
	 */
	public static void main(String[] args) {
		
		FindOptimalTour g = new FindOptimalTour();
		
		// Initialize the OpenMP framework
		MPI.Init(args);
		
		g.myRank = MPI.COMM_WORLD.Rank();
		g.noOfProcessors = MPI.COMM_WORLD.Size();
		
		/*// Initialize the cities array size
		g.cities = new ArrayList<String>();
		// Add cities in the cities array.. starting from A
		for (int b = 0; b < g.noOfCities; b++) {
			g.cities.add((char) (b + 65) + "");
		}*/
		
		// Process 0 is the master process
		// It generates till the number of nodes in the priority queue is equal to the number of processes available to process them
		if (g.myRank == 0) {
			
			// Generate a random matrix
			// g.generateMatrix();
			
			// Reads the input from a file
			g.readInputGraphFromFile();
			
			g.startTime = System.currentTimeMillis();
			System.out.println("Start time is : "+g.startTime);
			
			// An edgeMatrix to keep track of the included and excluded edges
			// This matrix is re-calculated for each node. Each node has its own
			// instance of this matrix in order to
			// efficiently track the inclusion/exclusion of edges at each node.
			int[][] edgeMatrix = new int[g.noOfCities][g.noOfCities];
			
			// Initializing edge matrix to have -1 between those vertices for which no edge exists
			/*for (int i=0; i<g.noOfCities; i++) {
				for (int j=0; j<g.noOfCities; j++) {
					if (g.adjacencyMatrix[i][j] == -1) edgeMatrix[i][j] = -1;
				}
			}*/

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
			
			// Generates the child nodes and puts them in the Priority Queue until the size of the queue is equal to the number of processes
			while (g.pQueue.size() < g.noOfProcessors - 1) {
				g.findOptimalTour();
			}
			
			// Process 0 has generated nodes = number of slave processes available
			// Now send these nodes one by one to the processes
			
			for (int i=1; i < g.noOfProcessors; i++) {
				
				Node n = g.pQueue.poll();
				
				try {
					// Converting the node to byte array
					ByteArrayOutputStream bos = new ByteArrayOutputStream();
					ObjectOutput out = new ObjectOutputStream(bos);
					out.writeObject(n);
					byte[] nodeBytes = bos.toByteArray();
					
					// Converting the adjacency matrix to byte array
					bos = new ByteArrayOutputStream();
					out = new ObjectOutputStream(bos);
					out.writeObject(g.adjacencyMatrix);
					byte[] adjacencyMatrixBytes = bos.toByteArray();
					
					// MPI send the adjacency matrix to each of the slave processors
					MPI.COMM_WORLD.Isend(adjacencyMatrixBytes, 0, adjacencyMatrixBytes.length, MPI.BYTE, i, i);
					
					// MPI send command to send the node n to process i
					MPI.COMM_WORLD.Isend(nodeBytes, 0, nodeBytes.length, MPI.BYTE, i, i);
					
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
			
			System.out.println("Master process has send the nodes in the queue to all the processors");
			
			// Master should now wait until all the slaves have finished processing and return their respective results to the master
			for (int i=1; i < g.noOfProcessors; i++) {
				Node minCostNodeReceived = null;
				byte minCostNodes[] = new byte[5000];
				
				MPI.COMM_WORLD.Recv(minCostNodes, 0, 5000, MPI.BYTE, i, i);
				
				ByteArrayInputStream bis = new ByteArrayInputStream(minCostNodes);
				ObjectInput in = null;
				try {
					in = new ObjectInputStream(bis);
					Object obj = in.readObject();
					minCostNodeReceived = (Node) obj;
					
					if (g.minimumCost > minCostNodeReceived.value.lowerBound) {
						g.minimumCost = (int) minCostNodeReceived.value.lowerBound;
						g.minCostNode = minCostNodeReceived;
					}
					
				} catch (IOException ex) {
					ex.printStackTrace();
				} catch (ClassNotFoundException cnf) {
					cnf.printStackTrace();
				}				
			}
					
			g.printOptimalTour(g.minCostNode);
			
			g.endTime = System.currentTimeMillis();
			System.out.println("End time is : "+g.endTime);
			
			System.out.println("Total time taken is : " + (g.endTime - g.startTime));
		} else {
			
			// Other processors tend to keep listening to the parent processor if it sends any new node
			// Upon receiving the node, they will calculate the lower bound of the node. 
			// Once the lower bound of the node is calculated, they must check that if the lower bound of the new node is greater than the min cost found
			// If yes, this node must not be sent back to the parent process for addition to PQ
			// If not, then send this to parent process so that it can be added to PQ
			
			// PriorityQueue<Node> pQueue = new PriorityQueue<Node>();
			
			byte[] nodeBytes = new byte[5000];
			byte[] adjacencyMatrixBytes = new byte[5000];
			Node nodeReceived = null;
			
			// Each process should receive a node from the parent process
			// After receiving the node, it casts the buffer contents back to a Node class
			// It then adds the node to its own local Priority Queue
			// Then it starts processing the node i.e. finding the subtree rooted at this node.
			for (int i=1; i < g.noOfProcessors; i++) {
				if (g.myRank == i) {
					
					//System.out.println("Before Receive : "+g.myRank);
					
					// Receives the adjacency matrix from the parent process
					MPI.COMM_WORLD.Recv(adjacencyMatrixBytes, 0, 5000, MPI.BYTE, 0, i);
					
					// Receives the node from the parent processor
					MPI.COMM_WORLD.Recv(nodeBytes, 0, 5000, MPI.BYTE, 0, i);
					
					//System.out.println("After Receive : "+g.myRank);
					
					ByteArrayInputStream bis1 = new ByteArrayInputStream(adjacencyMatrixBytes);
					ObjectInput in1 = null;
					try {
						in1 = new ObjectInputStream(bis1);
						Object obj = in1.readObject();
						g.adjacencyMatrix = (int[][]) obj;
					} catch (IOException ex) {
						ex.printStackTrace();
					} catch (ClassNotFoundException cnf) {
						cnf.printStackTrace();
					}
					
					ByteArrayInputStream bis2 = new ByteArrayInputStream(nodeBytes);
					ObjectInput in2 = null;
					try {
						in2 = new ObjectInputStream(bis2);
						Object obj = in2.readObject();
						nodeReceived = (Node) obj;
					} catch (IOException ex) {
						ex.printStackTrace();
					} catch (ClassNotFoundException cnf) {
						cnf.printStackTrace();
					}
					
					g.noOfCities = nodeReceived.value.edgeMatrix[0].length;
					// Initialize the cities array size
					g.cities = new ArrayList<String>();
					
					// Add cities in the cities array.. starting from A
					for (int b = 0; b < g.noOfCities; b++) {
						g.cities.add((char)(b+65) + "");
					}
					g.pQueue.add(nodeReceived);
				}
			}
			
			
			// By now the node has been received, so this process will start processing the node
			// The process will calculate the subtree rooted at this node and then return the minimum cost of the route found in this branch of
			// the main tree
			
			while (!g.pQueue.isEmpty()) {
				g.findOptimalTour();
			}			
			try {
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				ObjectOutput out = null;
				out = new ObjectOutputStream(bos);
				out.writeObject(g.minCostNode);
				byte[] minCostNodeBytes = bos.toByteArray();
				
				// MPI send command to send the node n to process i
				MPI.COMM_WORLD.Isend(minCostNodeBytes, 0, minCostNodeBytes.length, MPI.BYTE, 0, g.myRank);
				
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		// MPI.COMM_WORLD.Barrier();
		MPI.Finalize();
	}
	
	private void generateMatrix() {		
		random = new Random();
		this.adjacencyMatrix = new int[this.noOfCities][this.noOfCities];
		int min = 1;

		for (int i = 0; i < this.noOfCities; i++) {
			for (int j = i + 1; j < this.noOfCities; j++) {
				this.adjacencyMatrix[i][j] = this.adjacencyMatrix[j][i] = random.nextInt(9) + min;
			}
		}
		for (int i = 0; i < this.noOfCities; i++) {
			for (int j = 0; j < this.noOfCities; j++) {
				System.out.print(this.adjacencyMatrix[i][j] + " ");
			}
			System.out.println();
		}
	}

	/**
	 * 
	 * @param i
	 * @param j
	 * @throws InterruptedException 
	 */
	private void findOptimalTour()  {
		
/**********************   START of SEQUENTIAL CODE   *************************/
		
		// Checks if the queue still has some nodes to be processed
		//while (!this.pQueue.isEmpty()) {
			
			// Gets the i,j vertices whose edge is to be processed in the next
			// iteration
			int i=0; int j=0;
			boolean check = false;
			Node topNodeInPQueue = this.pQueue.peek();
			
			// System.out.println("Process " + this.myRank + " is going to print its edge matrix");
			// this.displayMatrix(topNodeInPQueue.value.edgeMatrix);
			
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
			
			Node node = this.pQueue.remove();		
			
			// Prints the node info which includes the edgeMatrix showing edges included and excluded on calculation of the lower bound 
			// of this node
			//printNodeInfo(node);			
			
			if (this.isRouteFound && node.value.lowerBound > minimumCost) {
				System.out.println("\nNode with lower bound " + node.value.lowerBound + " is pruned");
				return;
			}
			// Check if the left child of this node is null. If it is then create the left child
			if (node.left == null) {				
				generateChildNode(node, i, j, true);
			}
			
			// Check if the right child of this node is null. If yes, then create the right child
			if (node.right == null) {				
				generateChildNode(node, i, j, false);				
			}
			
			//System.out.println("PQueue size : "+this.pQueue.size());
		//}
		
		/**********************   END of SEQUENTIAL CODE   *************************/
		
		/**********************   START of MULTI-THREADED CODE   *************************/
		
		/*while (count < totalThreads) {
			
			Node topNodeInPQueue = null;
			
			synchronized (lock1) {
				while (this.pQueue.isEmpty()) {
					if (count >= totalThreads) {
						lock1.notifyAll();
						return;
					} else {
						count++;
						System.out.println("Number of threads waiting is : " + count);
						if (count >= totalThreads) {
							lock1.notifyAll();
							return;
						}
						lock1.wait();
					}
					
					System.out.println("Number of threads waiting is : "+count);
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
		}	*/	
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
		
		System.out.println("i = " + i);
		System.out.println("j = " + j);

		// Considering the edge i,j and updating the state of the edge matrix
		if (isEdgeIncluded) edgeMatrix[i][j] = edgeMatrix[j][i] = 1;
		else edgeMatrix[i][j] = edgeMatrix[j][i] = -1;
		
		boolean shouldBePruned =  updateEdgeMatrix(edgeMatrix, verticesToReprocess);
		
		System.out.println("ShoudBePruned : " + shouldBePruned);
		if (shouldBePruned) return;

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
		
		if (routeFound && routeCost < this.minimumCost) {
			this.minimumCost = (int) routeCost;
			this.minCostNode = node;
			this.isRouteFound = routeFound;
			
			// Broadcast the minimum cost found till now to all the processing nodes
			/*for (int rank=1; rank < this.noOfProcessors; rank++) {
				if (rank == this.myRank) {
					continue;
				}
				MPI.COMM_WORLD.Isend(new int[]{this.minimumCost}, 0, 1, MPI.INT, rank, 0);
			}	*/	
		} else if (routeCost < minimumCost) {
			// Adds this node in the queue since no route is found till now and
			// so this node is a potential candidate for
			// future processing.
			// However, this node should only be added if it has a lower bound <
			// cost of the mincost route found till now
			this.pQueue.add(node);
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
	private boolean updateEdgeMatrix(int[][] edgeMatrix, Set<Integer> s) {

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
			
			if (edgesConsidered > 2 || (edgesConsidered + remainingEdges) < 2) {
				return true;
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
		return false;
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
		
		System.out.println("Printing the edge matrix representing the graph.\n");
		
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
		displayMatrix(node.value.edgeMatrix);
		System.out.println("\nOptimal Route Cost is : "+node.value.lowerBound);
	}
	
	@SuppressWarnings("resource")
	private void readInputGraphFromFile() {
		/*Scanner scan = new Scanner(System.in);
		System.out.println("Enter the number of cities");
		this.noOfCities = scan.nextInt();*/
		
		String graph = "tsp_graph_02.txt";
		File file = null;
		Scanner readGraph = null;
		try {
			URL url = getClass().getResource(graph);
			file = new File(url.getPath());
			readGraph = new Scanner(file);
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		String row = readGraph.nextLine();
		
		this.noOfCities = row.split(" ").length;

		// Initialize the cities array size
		this.cities = new ArrayList<String>();
		
		// Add cities in the cities array.. starting from A
		for (int b = 0; b < this.noOfCities; b++) {
			this.cities.add((char)(b+65) + "");
		}
		
		this.adjacencyMatrix = new int[this.noOfCities][this.noOfCities];
		
		// boolean isValidPath = false;
		
		
		/*while (!isValidPath) {
			System.out.println("Enter the path to the input text file containing the graph represented as an adjacency matrix");
			graph = scan.next().trim();
			if (graph.contains(".txt")) {
				isValidPath = true;
			} else {
				System.out.println("Invalid path");
			}
		}*/

		try {
			readGraph = new Scanner(file);
			int i=0;
			int j=0;
			while (readGraph.hasNextLine()) {
				row = readGraph.nextLine();
				String[] cols = row.split(" ");
				
				for (String col : cols) {
					int cost = Integer.parseInt(col.trim());
					this.adjacencyMatrix[i][j] = cost;
					j++;
				}
				//i = (i+1) % (this.cities.size()-1);
				i++;
				j = 0;
			}
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}	
		
		for (String name : this.cities) {
			this.longestCityName = Math.max(this.longestCityName, name.length());
		}

		/*for (int i = 0; i < this.noOfCities; i++) {
			for (int j = i + 1; j < this.noOfCities; j++) {
				System.out.println("Enter the distance between " + cities.get(i) + " and " + cities.get(j)
						+ ". Enter -1 if there is no edge between them");
				int dist = scan.nextInt();
				adjacencyMatrix[i][j] = dist;
				adjacencyMatrix[j][i] = dist;
			}
		}*/
		
		// System.out.println("Do you want to switch the Verbose mode on (Type Y for yes and N for No) : ");
		// isVersboseModeOn = scan.next().equalsIgnoreCase("y") ? true : false;
		
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
class NodeData implements Serializable {
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
class Node implements Comparable<Node>, Serializable {
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
