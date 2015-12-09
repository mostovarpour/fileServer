/*
 * Matthew Ostovarpour
 * Daniel Durazo
 * 12/10/15
 * File Server Client in Java
 */

import java.net.*;
import java.io.*;
import java.util.logging.*;

public class Client implements Runnable{
    private Socket clientSocket;
    private BufferedReader in = null;

    public CLIENTConnection(Socket client){
        this.clientSocket = client;
    }
    public void run(){
        try{
            in = new BufferedReader(new InputStreamReader(
                        clientSocket.getInputStream()));
            String clientSelection;
            while ((clientSelection = in.readLine()) != null){
                switch(clientSelection){
                    case "1":
                        receiveFile();
                        break;
                    case "2":
                        String outgoingFilename;
                        while ((outgoingFilename = in.readLine()) != null){
                            sendFile(outgoingFilename);
                        }
                        break;
                    default:
                        System.out.println("Incorrect command received.");
                        break;
                }
                in.close();
                break;
            }
        } catch (IOException ex){
            Logger.getLogger(CLIENTConnection.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void receiveFile(){
        try{
            int bytesRead;
            DataInputStream clientData = new DataInputStream(clientSocket.getInputStream());
            String filename = clientData.readUTF();
            OutputStream output = new FileOutputStream(("Received from client: " + filename));
            long size = clientData.readLong();
            byte[] buffer = new byte[1024];
            while (size > 0 && (bytesRead = clientData.read(buffer, 0, (int) Math.min(buffer.length, size))) != -1){
                output.write(buffer, 0, bytesRead);
                size -= bytesRead;
            }
            output.close();
            clientData.close();

            System.out.println("File " + filename + " received from the client.");
        } catch (IOException ex){
            System.err.println("Client error, closing connection.");
        }
    }

    public void sendFile(String filename){
        try{
            //handle the reading of the file
            File myFile = new File(filename);
            byte[] byteArr = new byte[(int) myFile.length()];

            FileInputStream fis = new FileInputStream(myFile);
            BufferedInputStream bis = new BufferedInputStream(fis);

            DataInputStream dis = new DataInputStream(bis);
            dis.readFully(byteArr, 0, byteArr.length);

            //handle file send over socket
            OutputStream os = clientSocket.getOutputStream();

            //Sending file name and file size to le server
            DataOutputStream dos = new DataOutputStream(os);
            dos.writeUTF(myFile.getName());
            dos.writeLong(byteArr.length);
            dos.write(byteArr, 0, byteArr.length);
            dos.flush();
            System.out.println("File " + filename + " sent to the client.");
        } catch (IOException e){
            System.err.println("File does not exist!!!");
        }
    }
}
