import java.awt.Point;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

/**
 * Client.java
 * @author Anthony
 * May 9, 2019
 * Client communicates with the server.
 */

public class Client {
	private Socket socket;
	private BufferedReader input;
	private PrintWriter output;
	private boolean running = true;

	private String room = "room";
	private String name = "admin";
	private int port = 6666;

	private int active;
	@SuppressWarnings("unused")
	private int inactive;
	
	private int CENTROID_BIAS;

	// FORMAT
	// PLAYER, PIECE, XY COORDS
	private int[][][] activePositions;

	private int[][] board;
	private Bot bot;

	/**
	 * go
	 * This method will start up the client.
	 */
	public void go() {
		System.out.println("CLIENT SIDE\n");
		// connect("127.0.0.1", port); // my computer
		
		connect("10.242.177.108", port); // omg
		
		// connect("192.168.1.100", port); // server's computer
		// connect("10.242.161.92", port); // server's computer 2
		// connect("10.242.181.151", port); // will's computer
		// connect("10.242.161.232", port); // will's computer 2
		// connect("10.242.171.120", port); // artem
		// connect("10.242.174.127", port); // jonathan
		// connect("10.242.184.39", port); // brian
		
		play();
	}

	/**
	 * connect
	 * This method will connect to a server.
	 * @param address
	 * @param port
	 * @return Socket
	 */
	public Socket connect(String address, int port) {
		try {
			socket = new Socket(address, port);
			InputStreamReader stream = new InputStreamReader(socket.getInputStream());
			input = new BufferedReader(stream);
			output = new PrintWriter(socket.getOutputStream());
		} catch (IOException e) {
			System.out.println("Connection to server failed.");
		}
		return socket;
	}

	/**
	 * play
	 * This method will start a game of Chinese Checkers.
	 */
	public void play() {
		join();
		Eval.computeDist();
		this.bot = new Bot(CENTROID_BIAS);

		while (running) {
			try {
				if (input.ready()) {
					String message = input.readLine();
					System.out.println("SERVER MESSAGE:\n"+message);
					message = message.trim();
					if (message.charAt(0) == 'B') { // BOARD
						getData(message);
						bot.updateState(board);
						Move move = bot.getMove();
						move(move);
					} else if (message.charAt(0) == 'E') { // ERROR
						System.exit(0);
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		close();
	}

	/**
	 * join
	 * This method will handle joining.
	 */
	public void join() {
		Scanner scanner = new Scanner(System.in);
		System.out.println("Enter the room name:");
		this.room = scanner.nextLine();
		System.out.println("Enter your username:");
		this.name = scanner.nextLine();
		System.out.println("Enter the centroid bias: (best is 12 from experimentation)");
		this.CENTROID_BIAS = Integer.parseInt(scanner.nextLine());
		output.println("JOINROOM "+room);
		output.flush();
		output.println("CHOOSENAME "+name);
		output.flush();
		boolean error = true;
		do {
			try {
				if (input.ready()) {
					String message = input.readLine();
					message = message.trim();

					if (message.charAt(0) == 'E') { // ERROR
						System.out.println(message);
						System.out.println("Enter the room name:");
						this.room = scanner.nextLine();
						System.out.println("Enter your username:");
						this.name = scanner.nextLine();
						output.println("JOINROOM "+room);
						output.flush();
						output.println("CHOOSENAME "+name);
						output.flush();
					} else { // OK
						error = false;
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		} while (error == true);
		scanner.close();
	}

	/**
	 * getData
	 * This method will parse data from a board message.
	 * @param boardMessage
	 */
	public void getData(String boardMessage) {
		// BOARD #ACTIVENUM #INACTIVENUM #ACTIVEPIECES #INACTIVEPIECES
		boardMessage = boardMessage.substring(6);
		this.active = Integer.parseInt(boardMessage.substring(0, boardMessage.indexOf(' ')));
		boardMessage = boardMessage.substring(boardMessage.indexOf(' ')+1);
		this.inactive = Integer.parseInt(boardMessage.substring(0, boardMessage.indexOf(' ')));
		boardMessage = boardMessage.substring(boardMessage.indexOf(' ')+1);

		this.activePositions = new int[active][10][2];
		boardMessage = boardMessage.replaceAll(",", " ");
		boardMessage = boardMessage.replaceAll("\\p{P}","");

		// get the active pieces
		int currentIndex = 0;
		int i = 0;
		while (i < active) {
			int j = 0;
			while (j < 10) {
				int k = 0;
				while (k < 2) {
					int endIndex = boardMessage.indexOf(' ', currentIndex);
					String data;
					if (endIndex != -1) {
						data = boardMessage.substring(currentIndex, endIndex);
					} else { // if reach the end
						data = boardMessage.substring(currentIndex, boardMessage.length());
					}
					activePositions[i][j][k] = Integer.parseInt(data);
					currentIndex = endIndex + 1;
					k++;
				}
				j++;
			}
			i++;
		}
		updateBoard();
		outputBoard();
		/* FOR DEBUG PURPOSES */
//		System.out.println("ACTIVE: "+active);
//		System.out.println("INACTIVE: "+inactive);
//		System.out.println();
//
//		System.out.println("ACTIVE POSITIONS:");
//		outputPositionsArray(activePositions);
//		System.out.println();
	}

	/**
	 * updateBoard
	 * This method will update the board according to active positions.
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
	}

	/**
	 * move
	 * This method will use a bot to make a move and send it to the server.
	 */
	public void move(Move move) {
		int[] position = new int[] {move.piece.x, move.piece.y};
		ArrayList<int[]> jumps = new ArrayList<int[]>();

		for (int i = 0; i < move.jumpPath.size(); i++) {
			Point jumpPoint = move.jumpPath.get(i);
			int[] jump = new int[] {jumpPoint.x, jumpPoint.y};
			jumps.add(jump);
		}

		int index = find(position, activePositions[0]);
		this.activePositions[0][index] = jumps.get(jumps.size()-1);
		updateBoard();
		outputBoard();
		sendMove(position, jumps);
	}

	/**
	 * moveScanner
	 * This method will listen to the user to make a move.
	 */
	public void moveScanner(Scanner scanner) {
		// use algorithm
		// update activePositions
		// get move info
		// display statistics, decisions, and outcomes
		System.out.println("ENTER POSITION"); // input 11, 5 for testing
		int[] position = new int[2];
		System.out.println("X = ");
		position[0] = Integer.parseInt(scanner.nextLine());
		System.out.println("Y = ");
		position[1] = Integer.parseInt(scanner.nextLine());

		System.out.println("ENTER JUMPS"); // input 13, 5 for testing
		ArrayList<int[]> jumps = new ArrayList<int[]>();

		String input;
		do {
			int[] jump = new int[2];
			System.out.println("X = ");
			jump[0] = Integer.parseInt(scanner.nextLine());
			System.out.println("Y = ");
			jump[1] = Integer.parseInt(scanner.nextLine());
			jumps.add(jump);
			System.out.println("Enter '0' to enter another jump. Enter '1' to finish.");
			input = scanner.nextLine();
		} while (input.equals("0"));

		int index = find(position, activePositions[0]);
		this.activePositions[0][index] = jumps.get(jumps.size()-1);
		updateBoard();
		outputBoard();
		sendMove(position, jumps);
	}

	/**
	 * sendMove
	 * This method will generate a MOVE message and send it to the server.
	 * @param position
	 * @param jumps
	 */
	public void sendMove(int[] position, ArrayList<int[]> jumps) {
		// MOVE #POSITION #FIRSTJUMP ... #LASTJUMP
		String message = "MOVE ("+position[0]+","+position[1]+") ";
		for (int i = 0; i < jumps.size(); i++) {
			for (int j = 0; j < 2; j++) {
				if (j == 0) {
					message += "("+jumps.get(i)[j]+",";
				} else {
					message += jumps.get(i)[j]+") ";
				}
			}
		}
		output.println(message);
		output.flush();
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

	/**
	 * outputPositionsArray
	 * This method will output a positions array.
	 * @param array
	 */
	public void outputPositionsArray(int[][][] array) {
		for (int a = 0; a < array.length; a++) {
			int numPlayer = a + 1;
			System.out.println("PLAYER "+numPlayer+": ");
			for (int b = 0; b < array[0].length; b++) {
				System.out.print("(");
				for (int c = 0; c < array[0][0].length; c++) {
					System.out.print(array[a][b][c]);
					if (c == 0) {
						System.out.print(",");
					}
				}
				System.out.print(") ");
			}
			System.out.println();
		}
	}

	/**
	 * close
	 * This method will close all connection sockets.
	 */
	public void close() {
		try {
			socket.close();
			input.close();
			output.close();
		} catch (IOException e) {
			System.out.println("Failed to close socket.");
		}
	}
}
