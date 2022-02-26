import java.awt.Point;

/**
 * Eval.java
 * @author Eric
 * May 9, 2019
 * Contains methods used in various calculations.
 */

public class Eval {
	//variable declaration
	private static int[][] board = new int[26][18];
	private static int[][] dist;

	/**
	 * getDist
	 * This method will get the distance of a point from the goal.
	 * @param piece
	 * @return int
	 */
	public static int getDist(Point piece) {
		return dist[piece.x][piece.y];
	}

	/**
	 * computeDist
	 * This method will pre-compute the distance of all points on the board.
	 */
	public static void computeDist() {
		//initialize dist array
		dist = new int [board.length][board[0].length];
		//calculate the distance of all points on the board
		for(int i = 9; i<board.length; i++) {
			for(int j = 0; j<board[i].length; j++){
				if (board[i][j] != -1){
					dist[i][j] = calcDist(new Point(i, j));
				}
			}
		}
	}

	/**
	 * calcDist
	 * This method will calculate the distance of a point on the board.
	 * @param piece
	 * @return int variable, the distance from the bottom point of the board
	 */
	private static int calcDist(Point piece){
		//variable declaration
		boolean found = false;
		int currX = piece.x;
		int currY = piece.y;
		int distance = 0;
		//while loop that runs until point is found
		while (!found) {
			//if point is found set variable and exit loop
			if ((currX == 25) && (currY == 13)) {
				found = true;
			}
			//if point is not found
			if (!found) {
				//the method will attempt to find a path towards the bottom using the following steps:
				//figure if on left or right of midline (middle of the actual board) (y = 2x or currY = currX/2 on the skewed board)
				//try to move down (diagonally towards the midline)
				//else move towards midline
				if ((currX/2)<currY) {//if right of midline
					if ((currX + 1 < board.length) && (board[currX+1][currY]!=-1)) { //if the piece can move down, move down
						currX++;
						distance++;
					} else{ //else move sideways towards midline
						currY--;
						distance++;
					}
				} else { //if left of midline
					if((currX+1 < board.length) && (currY+1 < board[currX].length) && (board[currX+1][currY+1]!=1)){ //if the piece can move down, move down
						currX++;
						currY++;
						distance++;
					} else { //else move sideways towards midline
						currY++;
						distance++;
					}
				}
			}
		}
		return distance; //return the accumulated distance
	}

	/**
	 * updateBoard
	 * This method will update the board stored in this variable for access by the other methods
	 * @param newboard
	 */
	public static void updateBoard(int [][] newboard){
		board = newboard;
	}

	/**
	 * distCentroid
	 * This method calculates centroid of all current allied pieces and calculates the distance from a point to the centroid
	 * @return int value representing the distance from point to centroid
	 */
	public static int distCentroid(Point piece){
		//variable declaration
		int x = piece.x;
		int y = piece.y;
		double avgX = 0;
		double avgY = 0;
		double count = 0;
		//loop through the board
		for (int i = 8; i<board.length; i++) {
			for (int j = 0; j<board[i].length; j++) {
				if (board[i][j] == 1) { //if an allied piece is found
					//increment the x and y totals based on it's location
					avgX+=i;
					avgY+=(j+0.5*(board.length-i));
					count++;
				}
			}
		}
		//get the average by dividing the total by the amount
		//these 2 points form the centroid
		avgX = Math.round(avgX/count);
		avgY = Math.round((avgY/count)-(0.5*(board.length-avgX)));

		//use a variant of the pathfinding algorithm used in calcDist to get the distance from a point to the centroid
		int dist = 0;
		boolean found = false;
		while (!found){
			if ((x == avgX) && (y == avgY)) { //check if the centroid is found
				found = true;
			}
			if (!found) {
				//if above the centroid
				if (x<avgX) {
					if ((y+0.5*(board.length-x)>avgY+0.5*(board.length-avgX))) {//if right of target
						if (board[x+1][y]!=-1) { //move down and left if possible
							x++;
							dist++;
						} else if((x<board.length-1) && (y<board[x].length-1) && (board[x+1][y+1]!=-1)){ //otherwise move down and right
							x++;
							y++;
							dist++;
						} else { //if both are impossible move sideways left
							y--;
							dist++;
						}
					} else { //if left of target
						if (board[x+1][y+1]!=-1) { //move down and right if possible
							x++;
							y++;
							dist++;
						} else if ((x<board.length-1) && (board[x+1][y]!=-1)) { //otherwise move down and left
							x++;
							dist++;
						} else { //if both are impossible move sideways right
							y++;
							dist++;
						}
					}
				} else if (x>avgX) { //if below the centroid
					//repeat similar process to above, except moving up
					if ((y+0.5*(board.length-x)>avgY+0.5*(board.length-avgX))) {//if right of target
						if (board[x-1][y-1]!=-1) {
							x--;
							y--;
							dist++;
						} else if ((x>0) && (board[x-1][y]!=-1)) {
							x--;
							dist++;
						} else {
							y--;
							dist++;
						}
					} else {
						if (board[x-1][y]!=-1) {
							x--;
							dist++;
						} else if ((x>0) && (y>0) && (board[x-1][y-1]!=-1)) {
							x--;
							y--;
							dist++;
						} else {
							y++;
							dist++;
						}
					}
				} else { //if on the same plane vertically as centroid
					if (y+0.5*(board.length-x)>avgY+0.5*(board.length-avgX)) {//if right of target, move left
						y--;
						dist++;
					} else { //if left of target move right
						y++;
						dist++;
					}
				}
			}
		}
		return dist; //return distance
	}

	/**
	 * isBehindCentroid
	 * This method will check if a piece is behind the centroid.
	 * The centroid is the group center.
	 * A piece is behind the centroid if it is not located in a cone shaped area below the centroid.
	 * @param piece
	 * @return boolean, true if behind false if not
	 */
	public static boolean isBehindCentroid(Point piece){
		//variable declaration
		int x = piece.x;
		int y = piece.y;
		double avgX = 0;
		double avgY = 0;
		double count = 0;
		//loop through the board and calculate the centroid
		for (int i = 8; i<board.length; i++) {
			for (int j = 0; j<board[i].length; j++) {
				if (board[i][j] == 1) {
					avgX += i;
					avgY += j+0.5*(board.length-i);
					count++;
				}
			}
		}
		avgX = avgX/count;
		avgY = (avgY/count)-(0.5*(board.length-avgX));
		//if the point is above the centroid or on either side of the cone extending below the centroid
		if ((x < avgX) || (y < avgY) || (y > avgY + x - avgX)) {
			return true; //the point is behind the centroid
		} else {
			return false; //otherwise the point is not behind the centroid
		}
	}
}