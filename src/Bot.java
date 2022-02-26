import java.awt.Point;

/**
 * Bot.java
 * @author Anthony, Glen
 * May 9, 2019
 * Bot uses State to calculate a move and returns it to the Client.
 */

public class Bot {
	// current board information
	private State root;

	private double CENTROID_BIAS = 40.0;
	
	private int turn = 1;
	private Point piece;
	private Point lastSpace;
	
	/**
	 * Bot constructor
	 * @param centroidBias
	 */
	public Bot(double centroidBias) {
		this.CENTROID_BIAS = centroidBias;
	}

	/**
	 * updateState
	 * This method will update the state's board.
	 * @param board
	 */
	public void updateState(int[][] board) {
		this.root = new State(board);
	}

	/**
	 * getMove
	 * This method will determine and perform the best way to get the best move.
	 * @return Move
	 */
	public Move getMove() {
		Move bestMove;
		if (lastSpaceStatus() == true) { // fill last space
			System.out.println("Filling in the last space...");
			bestMove = fillLastSpace();
		} else if (turn > 2) { // normal move
			System.out.println("Calculating the best move...");
			bestMove = getMoveMethod();
		} else { // hard coded start
			System.out.println("Performing the hard-coded start...");
			bestMove = hardCodedStart();
		}
		root.apply(bestMove);
		bestMove.output();
		return bestMove;
	}
	
	/**
	 * getMoveMethod
	 * This method will get the best move.
	 * @return Move
	 */
	private Move getMoveMethod() {
		root.analyze(CENTROID_BIAS); // move analyze method to bot to customize centroid bias
		for (Move move : root.moves) {
			root.addChild(move);
		}
		Move bestMove = root.moves.get(0);
		double bestScore = Double.NEGATIVE_INFINITY;
		for (State child : root.children) {
			child.analyze(CENTROID_BIAS); // reminder to take top x% moves, rounded up
			if (child.score > bestScore) {
				bestScore = child.score;
				bestMove = child.previousMove;
			}
		}
		// playability is how likely it is that the board will be in a state such that 
		// we will have a good move next turn
		System.out.println("Playability of next turn's good move (relatively high num): " + bestScore);
		return bestMove;
	}
	
	/**
	 * hardCodedStart
	 * This method will get a move according to a predetermined hard-coded start.
	 * @return Move
	 */
	private Move hardCodedStart() {
		Move bestMove;
		if (turn == 1) { // first turn
			bestMove = new Move(new Point(12, 5));
			bestMove.addJump(new Point(13, 6));
		} else { // second turn
			bestMove = new Move(new Point(12, 8));
			bestMove.addJump(new Point(13, 8));
		}
		turn++;
		if (isMoveValid(bestMove) == false) { // if not valid
			turn = 9;
			bestMove = getMoveMethod();
		}
		return bestMove;
	}
	
	/**
	 * fillLastSpace
	 * This method will hard-code the last space case.
	 * @return Move
	 */
	private Move fillLastSpace() {
		Move bestMove;
		// if piece is near lastSpace
		// lastSpace = (x, y) piece = (x-1, y or y-1)
		// directly move it in
		
		// else
		// move it closer
		
		if (piece.y == lastSpace.y || (piece.y+1) == lastSpace.y) {
			bestMove = new Move(piece);
			bestMove.addJump(lastSpace);
		} else {
			bestMove = new Move(piece);
			Point jumpPoint;
			if (piece.y > lastSpace.y) { // from right to left
				jumpPoint = new Point(piece.x, piece.y-1);
				bestMove.addJump(jumpPoint);
			} else { // from left to right
				jumpPoint = new Point(piece.x, piece.y+1);
				bestMove.addJump(jumpPoint);
			}
		}

		if (isMoveValid(bestMove) == false) { // if not valid
			bestMove = getMoveMethod();
		}
		return bestMove;
	}
	
	/**
	 * lastSpaceStatus
	 * This method will check if a certain case that requires hard-coding is true.
	 * @return boolean
	 */
	private boolean lastSpaceStatus() {
		// loop through triangle
		// if top row
		// if only one space is empty
		// that is marked as the lastSpace
		
		// loop through above row
		// if a space is occupied
		// that is marked as the piece to move
		
		boolean meetSpace = false;
		for (int x = 0; x >= -3; x--) {
			for (int r = 25 + x; r >= 25 + x; r--) {
				for (int c = 13 + x; c <= 13; c++) {
					if (r == 22) { // if top row of triangle
						if (root.board[r][c] == 0) { // if empty space
							if (meetSpace == false) { // if only empty space
								this.lastSpace = new Point(r, c);
								meetSpace = true;
							} else {
								return false;
							}
						}
					} else { // other rows of triangle
						if (root.board[r][c] == 0) { // if not valid
							return false;
						}
					}
				}
			}
		}
		if (meetSpace == false) {
			return false;
		}
		int r = 21;
		for (int c = 9; c <= 13; c++) {
			if (root.board[r][c] == 1) { // if piece
				this.piece = new Point(r, c);
				return true;
			}
		}
		return false;
	}
	
	/**
	 * isMoveValid
	 * This method will return true if a move is valid.
	 * @param move
	 * @return boolean
	 */
	private boolean isMoveValid(Move move) {
		Point point = move.getLastJump();
		if (root.board[point.x][point.y] == 0) {
			return true;
		} else {
			return false;
		}
	}
}