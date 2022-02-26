import java.util.ArrayList;

/**
 * Node.java
 * @author Glen
 * May 9, 2019
 * Node stores moves in a tree to evaluate move playability.
 */

public class Node implements Comparable<Node> {
	public Move move;
	public double value;
	// public int frequency;
	public ArrayList<Node> children;

	/**
	 * Node constructor
	 * @param move
	 * @param value
	 */
	public Node(Move move, double value) {
		this.move = move;
		this.value = value;
		this.children = new ArrayList<Node>();
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(Node other) {
		return (int)Math.ceil(this.value - other.value);
	}
}
