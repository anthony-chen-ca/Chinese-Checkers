import java.util.ArrayList;
import java.awt.Point;

/**
 * Move.java
 * @author Glen, Anthony
 * May 9, 2019
 * Stores all information related to a move.
 */

public class Move implements Comparable<Move> {
	public Point piece;
	public ArrayList<Point> jumpPath; // includes target
	public double value;
	public double distance;
	public double multiplier;

	/**
	 * Move constructor
	 * @param piece
	 */
	public Move(Point piece) {
		this.piece = piece;
		this.jumpPath = new ArrayList<Point>();
	}

	/**
	 * Move constructor
	 * @param piece
	 * @param jumpPath
	 */
	public Move(Point piece, ArrayList<Point> jumpPath) {
		this.piece = piece;
		this.jumpPath = jumpPath;
	}

	/**
	 * addJump
	 * This method will add a jump to a move.
	 * @param jump
	 */
	public void addJump(Point jump) {
		this.jumpPath.add(jump);
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(Move other) {
		//System.out.println("Comparing Values: " + (int)(this.value - other.value));
		return (int)(this.value) - (int)(other.value);
	}

	/**
	 * getLastJump
	 * This method will return the last jump of a move's jump path.
	 * @return Point
	 */
	public Point getLastJump() {
		return jumpPath.get(jumpPath.size()-1);
	}

	/**
	 * copy
	 * This method will create a copy of a move.
	 * @return Move
	 */
	public Move copy() {
		Point newPiece = new Point(piece.x, piece.y);
		ArrayList<Point> newJumpPath = new ArrayList<Point>();
		for (int i = 0; i < jumpPath.size(); i++) {
			int x = jumpPath.get(i).x;
			int y = jumpPath.get(i).y;
			Point jumpPathPoint = new Point(x, y);
			newJumpPath.add(jumpPathPoint);
		}
		return new Move(newPiece, newJumpPath);
	}

	/**
	 * output
	 * This method will output the move to the console.
	 */
	public void output() {
		System.out.print("Move coordinates: ");
		System.out.print("("+piece.x+","+piece.y+") --> ");
		for (int i = 0; i < jumpPath.size(); i++) {
			Point point = jumpPath.get(i);
			System.out.print("("+point.x+","+point.y+") ");
			if (i < jumpPath.size() - 1) {
				System.out.print("--> ");
			}
		}
		System.out.println();
		System.out.println("Move Distance: "+distance);
		System.out.println("Centroid Bias Multiplier: "+multiplier);
		System.out.println("Total Move Value (Distance * Multiplier): " + value);
		System.out.println("Board after move: ");
	}
}
