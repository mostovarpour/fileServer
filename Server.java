/*
 * Matthew Ostovarpour
 * Daniel Durazo
 * 12/10/15
 * File Server Server in Java
 */

import java.net.*;
import java.io.*;

public class Server{
    private static ServerSocket serverSocket;
    private static Socket clientSocket = null;

    public static main void(String[] args) throws IOException{
        try{
            serverSocket = new ServerSocket(4444);
            System.out.println("Server Started.");
        } catch (Exception e){
            System.err.println("Port already in use.");
            System.exit(1);
        }
        while (true){
            try{
                clientSocket = serverSocket.accept();
                System.out.println("Accepted connection: " + clientSocket);
                Thread t = new Thread(new CLIENTConnection(clientSocket));
                t.start();
            } catch (Exception e){
                System.err.println("Error in connection attempt.");
            }
        }
    }
}
