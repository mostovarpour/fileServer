import java.io.*;
import java.net.*;
import java.util.StringTokenizer;

public class FileServer_Client {

	// Creating input and output stream references
	static InputStream fromServer = null;
	static DataOutputStream toServer = null;

	// Creating variables to store IP address and port
	static InetAddress ipAddress = null;
	static int port;

	// Creating the socket reference for connecting to server
	static Socket theSocket = null;


	// Main function, client starts running here
	public static void main(String[] args) throws IOException {

		// Keep track if actively connected to the server
		boolean connected = false;

		// Use a buffered reader to get the user's command input
		BufferedReader userCommand = new BufferedReader(new InputStreamReader(System.in));

		// Display a text menu with the possible commands
		System.out.println("Available Commands:");
		System.out.println("  OPEN <server IP> <server port>");
		System.out.println("  GET <server file path> <local path for file>");
		System.out.println("  CLOSE");
		System.out.println("  QUIT");

		// Keep getting user commands until they request QUIT
		while(true){

			// Small prompt character before the cursor
			System.out.print(" > ");

			// Temporarily store the line entered by user
			String theCommand = userCommand.readLine();

			// Use a tokenizer to parse through the command
			StringTokenizer commandParse = new StringTokenizer(theCommand);

			// Get the first token (aka the command: OPEN, GET, CLOSE, QUIT)
			String command = commandParse.nextToken();

			// Need to take different action for each command
			switch (command) {

			// User did not input valid input command! Show menu again
			default:
				System.out.println("\nAvailable Commands:");
				System.out.println("  OPEN <server IP> <server port>");
				System.out.println("  GET <server file path> <local path for file>");
				System.out.println("  CLOSE");
				System.out.println("  QUIT");
				break;

				//If command is QUIT
			case "QUIT":

				// User wants to exit out of client
				System.exit(0);

				// For the OPEN command
			case "OPEN":

				// OPEN requires the hostname of server and the port
				// There should be two remaining tokens 
				if(commandParse.countTokens() < 2){

					// User did not call OPEN correctly, show message hint
					System.out.println("Invalid use of OPEN.");
					System.out.println("\tOPEN <servername> <port>");
					break;
				}

				// If already connected, no point in trying to connect again
				if(connected == true){
					System.out.println("Already connected to server.");
					break;
				}

				// Now ready to parse the hostname and port of server

				// Get the hostname from the next token
				String serverName = commandParse.nextToken().trim();

				// Convert it to an IP address 
				ipAddress = InetAddress.getByName(serverName);

				// Set the port to the one specified by user from next token
				port = Integer.parseInt(commandParse.nextToken().trim());

				try {
					// Attempt to create the connection to server
					createConnection();

					// Display to user that connection was made
					System.out.println("Successfully connected to server.");

					// Update the connected flag to true
					connected = true;

				} catch (IOException e) {

					// Display error message
					System.err.println("Error connecting to server!");

				}

				break;
				
				// CLOSE connection command
			case "CLOSE":

				// Cannot close a connection that does not exist
				if(connected == false){

					// Give user helpeful information
					System.out.println("No connection ongoing.");
					break;
				}

				// If the connection is valid, time to close it
				closeConnection();

				// And update the variable so client knows connection was closed
				connected = false;
				break;

				// Request for GET 
			case "GET":

				// There should be two more tokens for the server file and local file strings
				if(commandParse.countTokens() < 2){

					// User did not call command correctly
					System.out.println("Invalid use of GET command.");
					break;
				}

				// Does not make sense to attempt to get a file if not connected
				if(connected == false){

					// Inform user to connect first
					System.out.println("Connection to server is required for GET.");
					break;
				}

				// The server file desired comes first, local file name second
				String serverPath = commandParse.nextToken().trim();
				String localPath = commandParse.nextToken().trim();

				// Call the code that receives the actual file
				getFile(serverPath, localPath);

				// Go back and wait for more commands
				break;

			}
		}
	}


	// Using references set up so far, attempt to create the connection
	public static void createConnection() throws IOException{

		// Create a new socket
		theSocket = new Socket(ipAddress, port);

		// Get the output stream to be able to send data to server
		toServer = new DataOutputStream(theSocket.getOutputStream());

		// Get the input stream to receive data from server
		fromServer = theSocket.getInputStream();

	}


	// 
	public static void getFile(String serverPath, String localPath) throws IOException {

		OutputStream outputFile = null;

		//Request file transfer
		toServer.writeBytes("GET " + serverPath + "\r\n");

		// Create a buffer to receive the file in chunks
		byte[] buffer = new byte[1000];

		// Track how many bytes received for each read
		int count;

		// Wait for the first chunk of data to come from the server
		count = fromServer.read(buffer);

		// Make a copy of the first 50 bytes of the buffer to parse
		// The first part of data is text, parse to see if file was found
		String fileStatus = new String(buffer, 0, 50, "UTF-8");

		// If the initial string/message is DATA 404, file was not found 
		if(fileStatus.startsWith("DATA 404")){

			// Message will only contain the not found string, no data after
			// Print a message to user then return for new command parsing
			System.out.println("Server reply: " + fileStatus);
			return;
		}

		// Here means that the file was found and is being sent over now
		// Create a file object with the path to get ready for writing
		File incomingFile = new File(localPath);

		// Check if the file exists, overwrite existing file with server's copy
		if(incomingFile.exists()) {

			// Delete it to get latest copy from server
			incomingFile.delete();

			// Print message out to inform user
			System.out.println("Local file already existed. Deleted local copy.");
		}

		// File should not exist now, create the empty file
		incomingFile.createNewFile();

		// Attempt to make the output stream to the local destination file
		try {
			outputFile = new FileOutputStream(incomingFile);
		} catch (FileNotFoundException ex) {

			// Something went wrong
			System.out.println("Problem creating output file.");
		}

		// Create a string tokenizer to extract the content length string/value 
		StringTokenizer token = new StringTokenizer(fileStatus, "\r\n");

		// Copy the status portion of the string
		String stringToken = token.nextToken();

		// Display the information to the user
		System.out.println("Server reply:\n" + stringToken);

		// Know that the initial message before data ends in \r\n
		// add two bytes to compensate for tokenizer removing them 
		int initialLength = stringToken.length()+2;

		// Get the next token which contains the content length value
		stringToken = token.nextToken();

		// Again, add two extra bytes to compensate for what tokenizer removes
		// Need to know exactly where in the buffer the file begins
		initialLength += stringToken.length()+2;

		// Store the file size for the remote file for comparisons
		int fileLength = 0;

		// The token with the content length still has a space, so split it up into substrings 
		// with space character as the delimiter
		String num[] = stringToken.split("\\s");

		// The number will be the second string after the split
		// Trim just to make sure blank characters in memory do not cause problems
		fileLength = Integer.parseInt(num[1].trim());

		// Display the content length to the user
		System.out.println("Content length: "+fileLength);

		// Keep track of bytes written to local file
		int bytesWritten = 0;

		// Write to the local file the initial data sent after the message (beginning of file)
		// Skip the message sent by the server, copy remaining bytes 
		outputFile.write(buffer, initialLength, count - initialLength);

		// Some bytes have been written
		bytesWritten += count - initialLength;

		// Keep looping while missing bytes in the local file
		while (bytesWritten < fileLength) {

			// Wait for server to send next chunk
			count = fromServer.read(buffer);

			// Write the new chunk from the buffer to the local file
			outputFile.write(buffer, 0, count);

			// Track latest bytes written
			bytesWritten += count;
		}

		// If all the bytes have been written to local file, done!
		if(bytesWritten == fileLength){
			System.out.println("File successfully transfered");
		}

	}


	// Close the connection that was created
	public static void closeConnection() throws IOException{
		//Request to close the connection
		try{

			// Server needs to get the CLOSE message!
			toServer.writeBytes("CLOSE\r\n");

			// Print something for local side to know connection is closed
			System.out.println("Connection to server successfully closed.");
			
		} catch (IOException ex){

			// Something went wrong!
			System.out.println("Problem closing the output stream.");
		}

		// Close the input/output streams from the connection to server
		toServer.close();
		fromServer.close();

	}

}
