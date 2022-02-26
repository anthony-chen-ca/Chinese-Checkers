import java.util.ArrayList;
import java.util.Random;
import java.util.Collections;
import java.awt.Point;

/**
 * State.java
 * @author Glen, Eric, Anthony
 * May 9, 2019
 * State stores the game board and methods to evaluate it.
 */

public class State implements Comparable<State> {
	public int[][] board = new int[26][18];
	public double score;
	public ArrayList<Move> moves = new ArrayList<Move>();
	public Move previousMove;
	public ArrayList<State> children = new ArrayList<State>();
	private boolean[][] illegalGoalBoard = new boolean[26][18];
	private Random rnd = new Random();
	// private int player;

	/**
	 * State constructor
	 * @param board
	 */
	public State(int[][] board) {
		System.arraycopy(board, 0, this.board, 0, board.length);
		fillIllegalBoard();
	}

	/**
	 * State constructor
	 * @param oState
	 */
	public State(State oState) { // cloning constructor bc object.clone sucks
		System.arraycopy(oState.board, 0, this.board, 0, oState.board.length);
		fillIllegalBoard();
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(State other) {
		return (int)(this.score - other.score);
	}

	/**
	 * apply
	 * This method will apply a move to the board.
	 * @param move
	 */
	public void apply(Move move) {
		Point lastPoint = move.getLastJump();
		int target = board[lastPoint.x][lastPoint.y];
		board[lastPoint.x][lastPoint.y] = board[move.piece.x][move.piece.y];
		board[move.piece.x][move.piece.y] = target;
		return;
	}

	/**
	 * addChild
	 * This method will add a child to the state's children.
	 * @param move
	 */
	public void addChild(Move move) {
		State child = new State(this);
		child.apply(move);
		child.previousMove = move;
		children.add(child);
	}

	/**
	 * analyze
	 * This method will analyze the total score.
	 */
	public void analyze(double centroidBias) {
		getMoves();

		for (Move move : moves) {
			// here is where moves get evaluated
			Eval.updateBoard(board);
			// value starts off equal to distance to goal
			move.distance = (Eval.getDist(move.piece) - Eval.getDist(move.getLastJump()));
			move.value = move.distance;
			if (Eval.isBehindCentroid(move.piece)) {
				// if piece is behind the group center, it gets a multiplier proportional to distance to centroid
				move.multiplier = Math.sqrt(Eval.distCentroid(move.piece)) * centroidBias; 
				move.value *= move.multiplier;
				//System.out.print(" BEHIND CENTROID BY: " + Eval.distCentroid(move.piece));
			}
			// move.value *= 100;
			// move.output(); // output moves
		}
		Collections.sort(moves, Collections.reverseOrder()); // absolute power move
		// choose a random best move; choice is stored as the first in list
		if (moves.size() > 0) {
			double bestValue = moves.get(0).value;
			int i = 0;
			while (i < moves.size() - 1 &&  moves.get(i).value == bestValue) {
				++i;
			}
			int randomBestMove = (int)(rnd.nextDouble() * i);
			Collections.swap(moves, 0, randomBestMove);
		}

		score = 0.0;
		for (Move move : moves) {
			score += move.value;
		}


	}

	/**
	 * getMoves
	 * This method will get all possible moves from the board, evaluate them, and sort them.
	 * @param moves
	 */
	private void getMoves() {
		moves.clear();
		Point piece;
		for (int i = 0; i < board.length; ++i) {
			for (int j = 0; j < board[0].length; ++j) {
				if (board[i][j] == 1) {
					piece = new Point(i, j);
					getSteps(piece);
					getJumps(piece, null, new boolean[26][18]);
				}
			}
		}
	}

	/**
	 * getSteps
	 * This method will get all possible steps.
	 * @param moves
	 * @param piece
	 */
	private void getSteps(Point piece) {
		// loop around the point and add all steps
		// if step is valid, add to moves array

		for (int i = 0; i < 2; i++) {
			for (int j = -1 + i; j < 2; j++) {
				Point potentialPoint = new Point(piece.x+i, piece.y+j);
				if (inBounds(potentialPoint)) {
					if (board[potentialPoint.x][potentialPoint.y] == 0) {
						Move move = new Move(piece);
						move.addJump(potentialPoint);
						if (checkIllegalGoal(move.getLastJump()) == false) {
							moves.add(move);
						}
					}
				}
			}
		}
		return;
	}

	/**
	 * getJumps
	 * This method will use recursion to get all possible jumps.
	 * @param piece
	 * @param move
	 * @param moves
	 * @param visited
	 */
	private void getJumps(Point piece, Move move, boolean[][] visited) {
		// if move is null
		// create a new move

		// mark current space as visited
		// check through all jumpable points
		// if jump is valid and not visited yet
		// update move jumpPath
		// recursion getJumps for that jumpable point

		// if move has a jumpPath and is not illegal, add to array
		// return

		Point currentPoint;
		if (move == null) {
			move = new Move(piece);
			currentPoint = piece;
		} else {
			currentPoint = move.getLastJump();
		}
		visited[currentPoint.x][currentPoint.y] = true;
		for (int i = 0; i < 2; i++) {
			for (int j = -1 + i; j < 2; j++) {
				Point potentialPoint = new Point(currentPoint.x+(i*2), currentPoint.y+(j*2));
				Point jumpPoint = new Point(currentPoint.x+i, currentPoint.y+j);
				if (isJumpValid(potentialPoint, jumpPoint)) {
					if (visited[potentialPoint.x][potentialPoint.y] == false) {
						Move newMove = move.copy();
						newMove.addJump(potentialPoint);
						boolean[][] newVisited = new boolean[26][18];
						System.arraycopy(visited, 0, newVisited, 0, visited.length);
						getJumps(piece, newMove, newVisited);
					}
				}
			}
		}
		if (move.jumpPath.size() > 0 && checkIllegalGoal(move.getLastJump()) == false) {
			moves.add(move);
		}
		return;
	}

	/**
	 * isJumpValid
	 * This method will return true if a jump is valid.
	 * @param point
	 * @param jumpPoint
	 * @return boolean
	 */
	private boolean isJumpValid(Point point, Point jumpPoint) {
		if (inBounds(point) && inBounds(jumpPoint)) { // if in bounds
			if (board[point.x][point.y] == 0) { // if target is empty
				int jumpSpaceValue = board[jumpPoint.x][jumpPoint.y];
				if (jumpSpaceValue != -1 && jumpSpaceValue != 0) { // if jumped space is occupied
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * checkIllegalGoal
	 * This method will return true if a space is illegal.
	 * @param point
	 * @return boolean
	 */
	private boolean checkIllegalGoal(Point point) {
		return illegalGoalBoard[point.x][point.y];
	}

	/**
	 * fillIllegalBoard
	 * This method will fill the illegal board.
	 */
	private void fillIllegalBoard() {
		setUpTriangle(new int[] {16,4}, true);
		setUpTriangle(new int[] {16,13}, true);
		setUpTriangle(new int[] {18,5}, false);
		setUpTriangle(new int[] {18,14}, false);
	}

	/**
	 * setUpTriangle
	 * This method will set up a triangle to be used in the illegal goal board.
	 * @param peak
	 * @param upsideDown
	 */
	public void setUpTriangle(int[] peak, boolean upsideDown) {
		if (upsideDown == false) {
			for (int x = 0; x <= 3; x++) {
				for (int r = peak[0] + x; r <= peak[0] + x; r++) {
					for (int c = peak[1]; c <= peak[1] + x; c++) {
						this.illegalGoalBoard[r][c] = true;
					}
				}
			}
		} else {
			for (int x = 0; x >= -3; x--) {
				for (int r = peak[0] + x; r >= peak[0] + x; r--) {
					for (int c = peak[1] + x; c <= peak[1]; c++) {
						this.illegalGoalBoard[r][c] = true;
					}
				}
			}
		}
	}

	/**
	 * This method will return true if a point is on the board.
	 * @param point
	 * @return boolean
	 */
	private boolean inBounds(Point point) {
		if (point.x >= 0 && point.x <= 25 && point.y >= 0 && point.y <= 17) {
			return true;
		}
		return false;
	}

	/**
	 * checkWin
	 * This method will return true if the player has won.
	 * @return boolean
	 */
	@SuppressWarnings("unused")
	private boolean checkWin() {
		for (int i = 21; i<25; i++){
			for (int j = 9 + (i-21); j <= 12; j++){
				if (board[i][j] == 0){
					return false;
				}
			}
		}
		return true;
	}
}
