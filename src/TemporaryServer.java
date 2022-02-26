import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * TemporaryServer.java
 * @author Anthony
 * May 9, 2019
 * TemporaryServer is used for debugging. Otherwise, use the official server.
 */

public class TemporaryServer {
	private ServerSocket serverSocket;
	private int port = 6666;
	private boolean running = true;

	private int active;
	private int inactive;

	// FORMAT
	// PLAYER, PIECE, XY COORDS
	private int[][][] activePositions;

	private int[][] board;

	/**
	 * Main method
	 * @param args
	 */
	public static void main(String[] args) {
		new TemporaryServer().go();
	}

	/**
	 * go
	 * This method will initialize the temporary server.
	 */
	public void go() {
		// initialization
		this.active = 1;
		this.inactive = 0;

		System.out.println("SERVER SIDE");
		Socket client = null;

		try {
			serverSocket = new ServerSocket(port);
		} catch (IOException e) {
			e.printStackTrace();
		}

		while (running) {
			try {
				client = serverSocket.accept();
				Thread t = new Thread(new ConnectionHandler(client));
				t.start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * ConnectionHandler.java
	 * @author Anthony
	 * May 9, 2019
	 * The connection handler will handle all the connections.
	 */
	
	private class ConnectionHandler implements Runnable {
		private Socket client;
		private BufferedReader input;
		private PrintWriter output;
		private boolean running;

		/**
		 * ConnectionHandler constructor
		 * @param client
		 */
		public ConnectionHandler(Socket client) {
			this.client = client;
			try {
				this.output = new PrintWriter(client.getOutputStream());
				InputStreamReader stream = new InputStreamReader(client.getInputStream());
				this.input = new BufferedReader(stream);
			} catch (IOException e) {
				e.printStackTrace();
			}
			this.running = true;
		}

		/* (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		public void run() {
			setUpBoard();
			sendMessage("OK"); // send OK
			sendMessage("BOARD"); // send board message

			// send and get messages from client
			while (running) {
				try {
					if (input.ready()) {
						String message = input.readLine();
						System.out.println("Client Message: "+message);
						message = message.trim();
						if ((message.substring(0, 4)).equals("MOVE")) {
							getData(message);
							updateBoard();
							if (checkWin() == true) {
								sendMessage("WIN");
								running = false;
								System.exit(0);
							} else {
								sendMessage("BOARD");
							}
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			// close
			close();
		}

		/**
		 * sendMessage
		 * This method will send a message to the client.
		 * @param type
		 */
		public void sendMessage(String type) {
			String message = "ERROR";

			if (type.equals("BOARD")) {
				String message1 = "BOARD "+active+" "+inactive+" ";
				String message2 = "";

				for (int i = 0; i < activePositions.length; i++) { // player
					for (int j = 0; j < 10; j++) { // piece number
						for (int k = 0; k < 2; k++) { // x y
							if (k == 0) {
								message2 += "(" + activePositions[i][j][k] + ",";
							} else {
								message2 += activePositions[i][j][k] + ") ";
							}
						}
					}
				}
				message = message1 + message2;
			} else if (type.equals("OK")) {
				message = "OK";
			} else if (type.equals("WIN")) {
				message = "WIN";
			} else if (type.equals("EXIT")) {
				message = "EXIT";
			}
			output.println(message);
			output.flush();
		}

		/**
		 * close
		 * This method will close all connection sockets.
		 */
		public void close() {
			try {
				client.close();
				input.close();
				output.close();
			} catch (Exception e) {
				System.out.println("Failed to close socket.");
			}
		}
	}

	/**
	 * getData
	 * This method will parse data from MOVE message.
	 * @param message
	 */
	public void getData(String message) {
		// MOVE #POSITION #FIRSTJUMP ... #LASTJUMP
		message = message.substring(5);
		message = message.replaceAll(",", " ");
		message = message.replaceAll("\\p{P}","");

		int[] position = new int[2];
		position[0] = Integer.parseInt(message.substring(0, message.indexOf(' ')));
		message = message.substring(message.indexOf(' ')+1);
		position[1] = Integer.parseInt(message.substring(0, message.indexOf(' ')));
		message = message.substring(message.indexOf(' ')+1);

		ArrayList<int[]> jumps = new ArrayList<int[]>();

		int currentIndex = 0;
		while (currentIndex < message.length()) {
			int[] data = new int[2];
			for (int i = 0; i < 2; i++) {
				int endIndex = message.indexOf(' ', currentIndex);
				if (endIndex != -1) {
					data[i] = Integer.parseInt(message.substring(currentIndex, endIndex));
					currentIndex = endIndex + 1;
				} else { // if reach the end
					data[i] = Integer.parseInt(message.substring(currentIndex, message.length()));
					currentIndex = message.length();
				}
			}
			jumps.add(data);
		}

		int index = find(position, activePositions[0]);
		this.activePositions[0][index] = jumps.get(jumps.size()-1);

		/* FOR DEBUG PURPOSES */
		System.out.println("POSITION: "+"("+position[0]+","+position[1]+")");
		System.out.print("JUMPS: ");
		for (int i = 0; i < jumps.size(); i++) {
			System.out.print("("+jumps.get(i)[0]+","+jumps.get(i)[1]+") ");
		}
		System.out.println();
	}

	/**
	 * find
	 * This method will find a value and return the index.
	 * @param value
	 * @return int
	 */
	public int find(int[] value, int[][] array) {
		for (int i = 0; i < array.length; i++) {
			if (Arrays.equals(array[i], value)) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * checkWin
	 * This method will return true if player 1 has won.
	 * @return boolean
	 */
	public boolean checkWin() {
		for (int i = 21; i < 25; i++){
			for (int j = 9 + (i-21); j <= 12; j++){
				if (board[i][j] != 1){
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * setUpBoard
	 * This method will set up the initial board.
	 */
	public void setUpBoard() {
		active = 1;
		inactive = 1;
		activePositions = new int[active][10][2];

		setUpTriangle(new int[] {9, 5}, 1, false); // top triangle
		// setUpTriangle(new int[] {25, 13}, 2, activePositions, true); // bottom triangle
		updateBoard();
	}

	/**
	 * setUpTriangle
	 * This method will set up a triangle to be used in setUpBoard.
	 * @param peak
	 * @param playerNum
	 * @param positions
	 * @param upsideDown
	 */
	public void setUpTriangle(int[] peak, int playerNum, boolean upsideDown) {
		if (upsideDown == false) {
			int i = 0;
			for (int x = 0; x <= 3; x++) {
				for (int r = peak[0] + x; r <= peak[0] + x; r++) {
					for (int c = peak[1]; c <= peak[1] + x; c++) {
						for (int j = 0; j < 2; j++) {
							if (j == 0) {
								activePositions[playerNum-1][i][j] = r;
							} else {
								activePositions[playerNum-1][i][j] = c;
							}
						}
						i++;
					}
				}
			}
		} else {
			int i = 0;
			for (int x = 0; x >= -3; x--) {
				for (int r = peak[0] + x; r >= peak[0] + x; r--) {
					for (int c = peak[1] + x; c <= peak[1]; c++) {
						for (int j = 0; j < 2; j++) {
							if (j == 0) {
								activePositions[playerNum-1][i][j] = r;
							} else {
								activePositions[playerNum-1][i][j] = c;
							}
						}
						i++;
					}
				}
			}
		}
	}

	/**
	 * updateBoard
	 * This method will update the board according to active/inactive positions.
	 */
	public void updateBoard() {
		resetBoard();

		// fill active positions
		for (int i = 0; i < activePositions.length; i++) {
			for (int j = 0; j < activePositions[0].length; j++) {
				int x = activePositions[i][j][0];
				int y = activePositions[i][j][1];
				board[x][y] = i+1;
			}
		}
		outputBoard();
	}

	/**
	 * resetBoard
	 * This method will reset the board.
	 */
	public void resetBoard() {
		int[][] board = {
				{-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
				{-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
				{-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
				{-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
				{-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
				{-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
				{-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
				{-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
				{-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
				{-1, -1, -1, -1, -1,  0, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
				{-1, -1, -1, -1, -1,  0,  0, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
				{-1, -1, -1, -1, -1,  0,  0,  0, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
				{-1, -1, -1, -1, -1,  0,  0,  0,  0, -1, -1, -1, -1, -1, -1, -1, -1, -1},
				{-1,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0, -1, -1, -1, -1},
				{-1, -1,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0, -1, -1, -1, -1},
				{-1, -1, -1,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0, -1, -1, -1, -1},
				{-1, -1, -1, -1,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0, -1, -1, -1, -1},
				{-1, -1, -1, -1, -1,  0,  0,  0,  0,  0,  0,  0,  0,  0, -1, -1, -1, -1},
				{-1, -1, -1, -1, -1,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0, -1, -1, -1},
				{-1, -1, -1, -1, -1,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0, -1, -1},
				{-1, -1, -1, -1, -1,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0, -1},
				{-1, -1, -1, -1, -1,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0},
				{-1, -1, -1, -1, -1, -1, -1, -1, -1, -1,  0,  0,  0,  0, -1, -1, -1, -1},
				{-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,  0,  0,  0, -1, -1, -1, -1},
				{-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,  0,  0, -1, -1, -1, -1},
				{-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,  0, -1, -1, -1, -1}};
		this.board = board;
		this.board = board;
	}

	/**
	 * outputBoard
	 * This method will output the board to the console.
	 */
	public void outputBoard() {
		System.out.println("BOARD:");
		for (int i = 9; i < board.length; i++){
			for (int k = 0; k < board.length-i-1; k++){
				System.out.print(" ");
			}

			for (int j = 0; j < board[i].length; j++){
				if (board[i][j] == -1){
					System.out.print(" "+' ');
				} else {
					System.out.print(" "+board[i][j]);
				}
			}
			System.out.println();
		}
	}
}
