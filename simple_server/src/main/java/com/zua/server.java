package main.java.com.zua;

import java.net.ServerSocket;
import java.net.Socket;

public class server {
    public static void main(String[] args){
        // Create ServerSocket on LocalHost, port 6789
        ServerSocket serverSocket = new ServerSocket(8080);
        System.out.println("Listening for connections on port 8080...\r\n");
        // Listen for new client connections
        while(true) {
            // Accept new client connection
            Socket connectionSocket = serverSocket.accept();
            // Create new thread to handle client request
            Thread connectionThread = new Thread(new Connection(connectionSocket));
            // Start the connection thread
            connectionThread.start();
            System.out.println("New connection on port 8080...\r\n");
        }
    }
}
