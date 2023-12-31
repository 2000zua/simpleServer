package main.java.com.zua;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.util.HashMap;

public class conn implements Runnable {
    // The socket used to connect to the server
    private Socket connectionSocket;
    // A HashMap to store the keys/values inside of the client request
    private HashMap<String, String> request;
    // A HashMap that contains keys that need to be redirected to the given value
    private HashMap<String, String> redirect;

    public conn(Socket connectionSocket){
        this.setConnectionSocket(connectionSocket);

        this.request = new HashMap<>();
        this.redirect = new HashMap<>();
        // Add key/value pairs to the redirect HashMap
        // The key represents the URL value used by the user and the value is the URL value to redirect to
        redirect.put("/", "/index.html");
        redirect.put("/index.htm", "/index.html");
        redirect.put("/index", "/index.html");
    }

    public Socket getConnectionSocket(){
        return this.connectionSocket;
    }

    public void setConnectionSocket(Socket consocket){
        this.connectionSocket = consocket;
    }


    private void parseRequest() throws IOException {

        // Connect a BufferedReader to the client socket's input stream and read it its request data
        BufferedReader connectionReader = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));

        // Read in the top client request line. ex: GET /index.html HTTP/1.1
        String requestLine = connectionReader.readLine();

        // If the request line exists we begin to parse the request, otherwise it is not a proper request
        // and we do nothing
        if (requestLine != null) {

            // The top line of a client request is formatted differently from the rest of the request so
            // we get the values of the top line of the client request first
            String[] requestLineParams = requestLine.split(" ");

            // Extract the relevant information from the top line of the request. Method, Resource URL, and Protocol
            String requestMethod = requestLineParams[0];
            String requestResource = requestLineParams[1];
            String requestProtocol = requestLineParams[2];

            // Add the Method, Resource, and Protocol to the request HashMap
            request.put("Method", requestMethod);
            request.put("Resource", requestResource);
            request.put("Protocol", requestProtocol);

            // Read the next line of the client request header
            String headerLine = connectionReader.readLine();

            // While the request header still has lines to read we continue reading and
            // storing the values of each request field into the request HashMap
            while (!headerLine.isEmpty()) {

                // Splits the request field into its key and value pair
                String[] requestParams = headerLine.split(":", 2);

                // Put the request field key and value into the request HashMap
                request.put(requestParams[0], requestParams[1].replaceFirst(" ", ""));

                // Read the next header line of the request
                headerLine = connectionReader.readLine();
            }
        }
    }

    private void sendResponse() throws IOException {

        // Create an DataOutputStream, outStream, to be able to send information out to the client connection
        DataOutputStream outStream = new DataOutputStream(connectionSocket.getOutputStream());

        // Get the file path of the file requested by the client connection and open the requested file
        String resourcePath = request.get("Resource").toString();
        File file = new File("." + resourcePath);

        // If the file requested by the client is in the redirect HashMap then send the client a
        // HTTP 301 response and redirect the client to the new file address
        if (redirect.get(resourcePath) != null) {

            // Send the client a HTTP 301 request and redirect to new address
            outStream.writeBytes("HTTP/1.1 301 Moved Permanently\n" +
                    "Location: " + redirect.get(resourcePath));
        }

        // If the file requested does not exist send the client a HTTP 404 response with a webpage
        // that tells the client that the file was not found
        else if (!file.exists()) {

            // Create HTTP 404 response with a basic webpage
            String http404Response = "HTTP/1.1 404 Not Found\r\n\r\n" + "<!DOCTYPE html>\n" +
                    "<html>\n" +
                    "\n" +
                    "<head>\n" +
                    "    <title>Maurice Harris - Network Project 1</title>\n" +
                    "</head>\n" +
                    "\n" +
                    "<body><h1>\n" +
                    "404 Error: Page Not Found\n" +
                    "</h1></body>\n" +
                    "\n" +
                    "</html>";

            // Send the HTTP 404 response to the client using the UTF-8 encoding
            outStream.write(http404Response.getBytes("UTF-8"));
        }

        // If the file is not in the redirect HashMap and it exists then we send the client
        // a HTTP 200 response and serve the file
        else {

            // Open a file input stream to read data from the file
            FileInputStream fileStream = new FileInputStream(file);

            // Get the MIME file type of the file that the client is requesting
            String contentType = Files.probeContentType(file.toPath());

            // Create a BufferedInputStream to read data from the fileStream
            BufferedInputStream bufInputStream = new BufferedInputStream(fileStream);

            // Create an array of bytes the same length of the file requested to hold the file data bytes
            byte[] bytes = new byte[(int) file.length()];

            // Send the header of the HTTP 200 response
            outStream.writeBytes("HTTP/1.1 200 OK\r\nContent-Type: " + contentType + "\r\n\r\n");

            // Read in the data from the file requested into the bytes array
            bufInputStream.read(bytes);

            // Send the data contained by the bytes array to the client connection and flush the output stream
            outStream.write(bytes);
            outStream.flush();

            // Close the input stream
            bufInputStream.close();
        }
        // Close the output stream
        outStream.close();
    }

    @Override
    public void run() {
        try {
            // Parse the client request and store the request field keys/values inside of the request HashMap
            parseRequest();

            // Send an appropriate response to the client based on the request received by the server
            sendResponse();

            // Close the client connection
            this.connectionSocket.close();
        } catch (IOException ex) {
            // If an IOException is caught print out the stack of commands that leads to the error
            ex.printStackTrace();
        }
    }
}
