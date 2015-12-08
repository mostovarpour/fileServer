public class FileClient{
    private static Socket sock;
    private static String filename;
    private static BufferedReader stdin;
    private static PrintStream os;

    public static void main(String[] args) throws IOException{
        try{
            sock = new Socket("localhost", 4444);
            stdin = new BufferedReader(new InputStreamReader(System.in))'
        } catch (Exception e){
            System.err.println("Cannot connect to the server, try again.");
            System.exit(1);
        }

        os = new PrintStream(sock.getOutputStream());
        try{
            switch(Integer.parseInt(selecAction())){
                case 1:
                    os.println("1");
                    sendFile();
                    break;
                case 2:
                    os.println("2");
                    System.err.print("Enter file name: ");
                    filename = stdin.readLine();
                    os.println(filename);
                    receiveFile(filename);
                    break;
            }
        } catch (Exception e){
            System.err.println("That is not valid input!");
        }
        sock.close();
    }

    public static String selectAction() throws IOException{
        System.out.println("====================");
        System.out.println("1. Send file.");
        System.out.println("2. Receive file.");
        System.out.print("\nMake selection: ");

        return stdin.readLine();
    }

    public static void sendFile(String filename){
        try{
            System.err.print("Enter file name: ");
            filename = stdin.readLine();

            File myFile = new File(filename);
            byte[] byteArr = new byte[(int) myFile.length()];

            FileInputStream fis = new FileInputStream(myFile);
            BufferedInputStream bis = new BufferedInputStream(fis);

            DataInputStream dis = new DataInputStream(bis);
            dis.readFully(byteArr, 0, byteArr.length);

            OutputStream os = sock.getOutputStream();

            //Sending the file name and file size to the server
            DataOutputStream dos = new DataOutputStream(os);
            dos.writeUTF(myFile.getName());
            dos.writeLong(byteArr, 0, byteArr.length);
            dos.flush();
            System.out.println("File "+filename+" sent to the server.");
        } catch (Exception e){
            System.err.println("File does not exist!!!");
        }
    }

    public static void receiveFile(String filename){
        try{
            int bytesRead;
            InputStream in = sock.getInputStream();
            DataOutputStream clientData = new DataInputStream(in);
            filename = clientData.readUTF();
            OutputStream output = new FileOutputStream(("Received from server "+filename));
            long size = clientData.readLong();
            byte[] buffer = new bute[1024];
            while (size > 0 && (bytesRead = clientData.read(buffer, 0, (int) Math.min(buffer.length, size))) != -1){
                output.write(buffer, 0, bytesRead);
                size -= bytesRead;
            }

            output.close();
            in.close();
            System.out.println("File "+filename+" received from the server.");
        } catch (IOException ex){
            Logger.getLogger(CLIENTConnection.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
