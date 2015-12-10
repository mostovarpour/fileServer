/* Daniel Durazo
 * Matthew Ostovarpour
 * 12/10/2015
 * 
 * Java File Server: Server Code
 */

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.StringTokenizer;


public class FileServer_Server {

	// Port passed in as the first argument overwrites this
	// Default value for port
	static int port = 23657;
    
	// Create references needed for connections
	static ServerSocket serverSocket;
    static Socket client;
    
    // This output stream will send messages/data to the connected client
    static DataOutputStream toClient;
    
    // Prefix directory of "public" files that may be requested
    // On a web server, do not want users to access the root directory!
    static String allowedSubDir = "public/";

    

    // Running the File Server will begin here
    public static void main(String[] args) {

    	// Check if there was a port number passed in
    	if (args.length > 0) {
    		
    		try {
    			// Attempt to parse the first argument as an integer
    			port = Integer.parseInt(args[0].trim());
    			
    		} catch (NumberFormatException e) {
    			
    			// Inform user they did not pass in a valid port number
    			System.err.println("Invalid port! Defaulting to: " + port);
    		}
    	}
    	
        try {
        	// Attempt to set up the socket to use the requested port 
			serverSocket = new ServerSocket(port);

        } catch (IOException e) {
			
        	// Print out error message & stack trace to inform user
        	System.err.println("Error creating socket on port: " + port + "\n");
        	e.printStackTrace();
        	
        	// Exit with an error status
        	System.exit(-1);
		}

        // The server loop (described below)
        while (true) {
        	
        	try {
        		// Block/wait until a client connects
				client = serverSocket.accept();
				
			} catch (IOException e) {
				
				// Display error message & trace to inform user
				System.err.println("Error on socket accept\n");
				e.printStackTrace();
			}
            
        	// Display that client has successfully connected 
            System.out.println("Client successfully connected.");
            
            try {
            	
            	// Handle the client's requests
				handleClient();
				
			} catch (IOException e) {
				
				// Serious error when handling client, show info before exit
				System.err.println("Error while client connected!\n");
				e.printStackTrace();
				System.exit(-2);
			}
            
            // End of loop, go back and wait for next client
        }
        
    }
    
    
    // When this method is called, client has successfully connected
    public static void handleClient() throws IOException{
        
    	// Create the output stream for data for the client
    	toClient = new DataOutputStream(client.getOutputStream());
        
    	// Keep taking commands from this client until CLOSE command 
        while(true){
        	
            // Store commands from client in a buffered reader for parsing
        	// Each command from client explicitly contains "\r\n" characters
        	BufferedReader fromClient = new BufferedReader(new InputStreamReader(client.getInputStream()));
        
        	// Essentially block until a command comes in from the client
        	// (readLine() reads until \r\n is hit) 
        	// Store in a string to use a tokenizer for parsing
            String clientCommand = fromClient.readLine();
            
            // Create a tokenizer 
            StringTokenizer parseCommand = new StringTokenizer(clientCommand);
            
            // The actual command is the first word/token
            String theCommand = parseCommand.nextToken();
            
            // Check for the possible command cases: GET or CLOSE
            
            // Client requests to get a file 
            if(theCommand.equals("GET")){
            	
            	// The next token is the name of file requested
                String fileName = parseCommand.nextToken();

                // Now run the code that sends the file back to client
                sendFile(fileName);

            }else if(theCommand.equals("CLOSE")){
                
            	// Client has requested to close the connection
            	// Exiting the while loop closes the connection
            	break;
                
            }else{
            	// Something was sent incorrectly
                System.out.println("ERROR: Unrecognized Command!");
            }
        } 
        // Breaking out of loop means client has requested to CLOSE
        
        // Calling close() flushes the output stream then closes the stream
        toClient.close();
        
        // Close the connected client socket
        client.close();

        // Display server-side info about client disconnect
        System.out.println("Client has requested to CLOSE the connection.");

    }
    
    
    //Send requested file to the client socket
    public static void sendFile(String sourcePath) throws IOException {
        
    	// Create file object to allow accessing/checking files
    	// Ensure that file path begins at the "allowed" directory
        File theFile = new File(allowedSubDir + sourcePath);
        
        // If the file does NOT exist: 
        if (!theFile.exists()){
            
        	// Send the 404 not found message to client
            toClient.write(("DATA 404 Not found\r\n").getBytes());
            
            // Also print to the server output
            System.out.println("Client requested \'" + theFile.getPath() + "\' which does not exist.");
            
            // No file to send, so exit this method
            return;
        }
        
        // File DOES exist, so get ready to send it over to client, quick display message
        System.out.println("Client requested \'" + theFile.getPath() + "\'. Preparing to send file!");
        
        // First get the length of the file
        long fileLength = theFile.length();
        
        // Set up the beginning of message to client as specified by requirements
        String fileMessage = "DATA 200 OK\r\ncontent-length: " + fileLength + "\r\n";
        
        // Create a buffer of 1000 bytes (to split up and send large file in "chunks").
        // Copy the file message to the beginning of buffer for sending.
        // By using Arrays, can copy a range and fill the rest of desired buffer size with zeros
        // copyOfRange(byte[], start, end)
        byte[] sendBuffer = Arrays.copyOfRange(fileMessage.getBytes(), 0, 1000);
        
        // Know the file exists, so create a stream to read from the file
        FileInputStream fileStream = new FileInputStream(theFile);
        
        // Keep track of how many bytes stored in buffer to be written to client
        // Start with the length of the file message first
        int bufferIndex = fileMessage.length();
        
        //Fill the rest of the buffer with beginning of file
        // read(byte[], offset index, number of bytes to read)
        
        // Read the beginning of file into the remaining buffer space available:
        // read(byte[], start offset, # bytes to read), this returns an int of bytes read
        // If end of file reached before filling buffer, the index keeps track of last byte to send
        bufferIndex += fileStream.read(sendBuffer, bufferIndex, sendBuffer.length - bufferIndex);
        
        // The bufferIndex will be pointing one byte ahead, due to zero based array addressing
        // For example: 1000 bytes -> byte array offsets 0 to 999 INCLUSIVE!
        // So the index will equal 1000 when the buffer has been filled
        // When writing, use the current value of index to specify how many bytes to write, starting at
        // sendBuffer[0]
        
        // Now the message and first part of the file are ready to send
        // Send this initial "chunk" now, then loop again if needed until 
        // entire file has been sent
        do{
            toClient.write(sendBuffer, 0, bufferIndex);
        }
        while ((bufferIndex = fileStream.read(sendBuffer)) > 0);
        
        // Calling read(buffer) is the same as read(buffer, start offset, buffer.length)
        // which is attempting to read enough bytes to fill the entire buffer (1000 bytes)
        
        // Now the file has been completely sent to the client, quick status message, then close
        System.out.println("File sent successfully to client.");
        
        // Make sure everything buffered gets flushed out to client
        toClient.flush();
        
        // Close file stream as we are done with the file
        fileStream.close();
    }
}

