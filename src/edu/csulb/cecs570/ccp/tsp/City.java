/**
 * 
 */
package edu.csulb.cecs570.ccp.tsp;

import java.util.Map;

/**
 * @author Manav
 *
 */
public class City {

	private String name;
	private Map<String, Integer> neighbors;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Map<String, Integer> getNeighbors() {
		return neighbors;
	}

	public void setNeighbors(Map<String, Integer> neighbors) {
		this.neighbors = neighbors;
	}
}
