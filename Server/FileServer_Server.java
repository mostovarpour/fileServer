import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.StringTokenizer;



public class FileServer_Server {

	protected static int port = 23657;
    protected ServerSocket serverSocket;
    protected Socket clientSocket;
    protected static OutputStream out;
    protected static String publicPath = "public_files/";
    

//    public JavaFileServerServer(Socket clientSocket) {
//        this.clientSocket = clientSocket;
//    }

    public static void main(String[] args) throws IOException {
        //create and connect to a socket
        ServerSocket serverSocket = new ServerSocket(port);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("Connected");
            handleClient(clientSocket);
            
            
        }
    } //end main
    
    public static void handleClient(Socket clientSocket) throws IOException{
        out = new DataOutputStream(clientSocket.getOutputStream());
        
        while(true){
            BufferedReader inFromClient =
                new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        
            String commandLine;
            commandLine = inFromClient.readLine();
            
            StringTokenizer token = new StringTokenizer(commandLine);
            
            String command = token.nextToken();
            if(command.equals("GET")){
                String sourcePath = token.nextToken();
                sendFile(clientSocket, sourcePath);
            }else if(command.equals("CLOSE")){
                break;
                
            }else{
                System.out.println("Unrecognizable command.");
            }
        } //end while
        closeConnection(clientSocket);
    }
    
    //Send requested file to the client socket
    public static void sendFile(Socket clientSocket, String sourcePath) throws IOException{
        //Create file object
        File file = new File(publicPath+sourcePath);
        
        //Check to see if the requested file does not exist respond with a 404 message
        if (file.exists() == false){
            
            out.write(("DATA 404 Not found\r\n").getBytes());
            return;
        }
        //Get length of the file
        long fileLength = file.length();
        
        String goodFile = "DATA 200 OK\r\ncontent-length: "+fileLength+"\r\n";
        //Fill beggining of buffer with file avaliable message
        byte[] bytes = Arrays.copyOfRange(goodFile.getBytes(), 0, 1024);
        
        InputStream in = new FileInputStream(file);
        
        int count = goodFile.length();
        
        //Fill the rest of the buffer with beginning of file
        // read(byte[], offset index, number of bytes to read)
        count += in.read(bytes, goodFile.length(), bytes.length - goodFile.length());
        
        //Send the first filled buffer, then if there is anymore to write fill 
        // send and loop until there is nothing left to send
        do{
            out.write(bytes, 0, count);
        }
        while ((count = in.read(bytes)) > 0);
        
        //Close data input and output streams
        //out.flush();
        in.close();
    }
    
    //function to close connection to the socket
    public static void closeConnection(Socket clientSocket) throws IOException{
        try {
            clientSocket.close();
            System.out.println("Closed the connection.");
        } catch (IOException ex) {
            System.out.println("Could not close connection.");
        }
        out.close();
    }

}

