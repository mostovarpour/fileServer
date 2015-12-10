import java.io.*;
import java.net.*;
import java.util.StringTokenizer;

public class FileServer_Client {

	static InputStream in = null;
    static DataOutputStream out = null;

    public static void main(String[] args) throws IOException {
        boolean connection = false;
        Socket clientSocket = null;
          
        BufferedReader commandLineIn = new BufferedReader(new InputStreamReader(System.in));
        
        //System.out.println("");
        //System.out.println("");
        //System.out.println("");
        System.out.println("Commands are: \n1). OPEN <servername> <port>\n"
                + "2). GET <source path> <destination path>\n3). CLOSE\n4). QUIT");
        
        options: while(true){
            System.out.print("> ");
            String commandLine = commandLineIn.readLine();
            StringTokenizer token = new StringTokenizer(commandLine);
            
            String command = token.nextToken();
            
            switch (command) {
                //If command is OPEN
                case "OPEN":
                    //Should be two remaining tokens
                    if(token.countTokens() < 2){
                        System.out.println("Invalid use of OPEN command.");
                        break;
                    }
                    if(connection == true){
                        System.out.println("Connection already opened.");
                        break;
                    }
                    String serverName = token.nextToken();
                    InetAddress IPAddress = InetAddress.getByName(serverName);
                    int port = Integer.parseInt(token.nextToken().trim());
                    clientSocket = createConnection(IPAddress, port);
                    System.out.println("Started the connection.");
                    connection = true;
                    
                    break;
                //If command is GET
                case "GET":
                    //Should be two remaining tokens
                    if(token.countTokens() < 2){
                        System.out.println("Invalid use of GET command.");
                        break;
                    }
                    if(connection == false){
                        System.out.println("You must start a connection first.");
                        break;
                    }
                    String sourcePath = token.nextToken();
                    String destPath = token.nextToken();
                    getFile(clientSocket, sourcePath, destPath);
                    break;
                //If command is close
                case "CLOSE":
                    if(connection == false){
                        System.out.println("No connection ongoing.");
                        break;
                    }
                    closeConnection(clientSocket);
                    connection = false;
                    break;
                //If command is QUIT
                case "QUIT":
                    break options;
                default:
                    System.out.println("Commands are: \nOPEN <servername> <port>\n"
                            + "GET <source path> <destination path>\nCLOSE");
                    break;
            }
        }
    }
    //Create connection to server here and return object
    public static Socket createConnection(InetAddress IPAddress, int port) throws IOException{
        
        Socket clientSocket = new Socket(IPAddress, port);
        
        out = new DataOutputStream(clientSocket.getOutputStream());
        
        //try to create input stream
        try{
           in = clientSocket.getInputStream();
        } catch (IOException ex){
            System.out.println("Can't get socket input stream.");
        }
        
        return clientSocket;
        
    }
    //Function to get a file
    public static void getFile(Socket clientSocket, String sourcePath, String destPath) throws IOException {
        
        OutputStream outFile = null;

        //Request file transfer
        out.writeBytes("GET "+sourcePath+"\r\n");
        
        
        byte[] bytes = new byte[1024];
        int count;
        
        count = in.read(bytes);
        
        //Convert first 50 bytes of data into string
        //This is more than enough to check the feedback
        String copy = new String(bytes, 0, 50, "UTF-8");
        
        if(copy.startsWith("DATA 404")){
            //Message must only include the 404 message no file bytes included
            System.out.println("Server reply: "+new String(bytes));
            return;
        }
        
        File file = new File(destPath);
        if(file.exists()){
            file.delete();
            System.out.println("File already was created, deleteing");
        }
        
        file.createNewFile();
        
        //try to create the file output stream using the destination path
        try {
            outFile = new FileOutputStream(file);
        } catch (FileNotFoundException ex) {
            System.out.println("File not found.");
        }
        
        
        StringTokenizer token = new StringTokenizer(copy, "\r\n");
        String content = token.nextToken();
        System.out.println("Server reply: "+content);
        //keep track of number of bytes in the command section
        //adding 2 to the length because tokenizer will remove the \r\n bytes
        int preambleLength = content.length()+2;
        
        //Get the total length of the file
        content = token.nextToken();
        preambleLength += content.length()+2;
        int fileLength = 0;
        String num[] = content.split("\\s");
        
        //Number should be the second string on split
        fileLength = Integer.parseInt(num[1].trim());
        System.out.println("Content length: "+fileLength);
        
        int bytesWritten = 0;
        
        outFile.write(bytes, preambleLength, count - preambleLength);
        bytesWritten += count - preambleLength;
        
        if(bytesWritten == fileLength){
            System.out.println("File successfully transfered");
        }
        while (bytesWritten < fileLength) {
            count = in.read(bytes);
            outFile.write(bytes, 0, count);
            bytesWritten += count;
        }
        
        //outFile.close();
        //out.flush();
    }
    //Close an ongoing function here, return true on successful closure
    public static void closeConnection(Socket clientSocket) throws IOException{
    //Request to close the connection
        try{
            //out = new DataOutputStream(clientSocket.getOutputStream());
            out.writeBytes("CLOSE\r\n");
            System.out.println("Closed the connection.");
        } catch (IOException ex){
            System.out.println("Can't get socket data output stream.");
        }
        out.close();
        in.close();
        
    }

}
