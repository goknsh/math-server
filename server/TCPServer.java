package server;

import lib.Protocol;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Class/Thread to handle an individual client's requests after initial client hello.
 */
class TCPServerThread extends Thread {
    private Socket client;
    private String name;
    private boolean keepAlive;
    private Date connectionEstablished;

    /**
     * Constructor for this class, initially assigns keepAlive to true to have an infinite loop and stores the connection established date.
     * @param client Socket this thread will be responding to.
     * @param name Name the client provided upon initial connect.
     */
    public TCPServerThread(Socket client, String name) {
        this.connectionEstablished = new Date();
        this.client = client;
        this.name = name;
        this.keepAlive = true; // TODO: Design decision: keep this? Since it is only used in run()
    }

    /**
     * An infinite loop (as long as keepAlive is true) that responds to client's math commands.
     * This function will also gracefully close the socket and exit if the client sends the exit command.
     */
    public void run() {
        try {
            BufferedReader inFromClient = new BufferedReader(new InputStreamReader(client.getInputStream()));
            DataOutputStream outToClient = new DataOutputStream(client.getOutputStream());
            Map<String, String> input;
            while (this.keepAlive) {
                input = Protocol.unmarshal(inFromClient.readLine());
                if (Objects.equals(input.get("cmd"), "math")) {
                    // TODO: Log command
                    outToClient.writeBytes(buildServerResponse(evaluateEquation(input.get("eq"))));
                }
                if (Objects.equals(input.get("cmd"), "exit")) {
                    // TODO: Log exit and dates connection started and ended
                    this.keepAlive = false;
                    outToClient.writeBytes(buildClientExitACK());
                    this.client.close();
                    System.out.println("Client has left: " + this.name);
                    Date connectionClosed = new Date();
                }
            }
        } catch (IOException e) {
            // TODO: Decide what happens here. Close socket? Send error back to client?
            System.out.println("IOException for client with name: " + this.name + " (" + e.getMessage() + ")");
            e.printStackTrace();
        }
    }

    public boolean isDigit(char d)
    {
        if(d >= 48 && d <= 57){ return true; }
        return false;
    }

    public boolean isOperation(char op)
    {
        if(op == '*' || op == '/' || op == '%' || op == '!' || op == '^' || op == '+' || op == '-') { return true; }
        return false;
    }

    // TODO: Waiting on response from professor to check if a "global" evaluator needs to be implemented to satisfy requirement #5 of the server application
    public String evaluateEquation(String equation) {
        String arg1 = "null";
        char operator = '#';
        String arg2 = "null";
        String result = "null";
        
        int nextIndex = 0;
        boolean foundDecimal = false;
        String errorMessage = "";

        // Parsing first argument and the operator
        for (int i = 0; i < equation.length(); i++) {
            if(isDigit(equation.charAt(i)))
            {
                arg1+=equation.charAt(i);               // Append digit to first argument
            }
            else if(equation.charAt(i)=='.')
            {
                if(foundDecimal==false)
                {
                    foundDecimal = true;
                    arg1+=equation.charAt(i);           // Appends decimal to argument, but only if it's the first decimal found in this number
                }
                else{
                    arg1+=equation.charAt(i);
                    errorMessage = "Error - Too many decimal points in first argument. {" + arg1 + "}";
                }
            }
            else if(isOperation(equation.charAt(i)))
            {
                operator = equation.charAt(i);
            }
            else{
                errorMessage = "Error - Unrecognized symbol. {"+ equation.charAt(i) + "} Please use only digits, operators, and decimal points.";
            }

            if(errorMessage!="" || operator != '#')     // If operator is found or an error occured, exit the loop.
            {
                nextIndex = i+1;
                break;
            }
        }

        /* Commented out for testing. Return to actual code later
        if(errorMessage!="")
        {
            return(errorMessage); // If error occured, return it instead of result
        }
        */

        foundDecimal = false;   // Resets count for the second argument
        
        // Parsing second argument
        for (int i = nextIndex; i < equation.length(); i++)
        {
            if(isDigit(equation.charAt(i)))
            {
                arg2+=equation.charAt(i);
            }
            else if(equation.charAt(i)=='.')
            {
                if(foundDecimal==false)
                {
                    foundDecimal = true;
                    arg2+=equation.charAt(i);
                }
                else{
                    arg2+=equation.charAt(i);
                    errorMessage = "Error - Too many decimal points in second argument. {" + arg2 + "}";
                }
            }
            else if(isOperation(equation.charAt(i)))
            {
                errorMessage = "Error - Too many operators. {" + operator + ", " + equation.charAt(i) + "}";
            }
            else{
                errorMessage = "Error - Unrecognized symbol. {"+ equation.charAt(i) + "} Please use only digits, operators, and decimal points.";
            }

            // This loop will stop when all of the digits are successfully processed or an error occurs.
            if(errorMessage!="")
            {
                break;
            }
        }
        
        /* Commented out for testing. Return to actual code later
        if(errorMessage!="")
        {
            return(errorMessage); // If error occured, return it instead of result
        }
        */

        return("1st arg: " + arg1 + ", Operator: " + operator + ", 2nd arg: " + arg2 + ", Error msg: " + errorMessage);         // DEBUG

        // TODO : ACTUAL CALCULATION PART
    }

    /**
     * Builds a marshalled response to a client's exit command.
     * @return Marshalled exit response, ready to be directly sent.
     */
    public String buildClientExitACK() {
        Map<String, String> mapResponse = new HashMap<>();
        mapResponse.put("resp", "Bye, " + this.name);
        return Protocol.marshal(mapResponse);
    }

    /**
     * Builds a marshalled response to a client's math command.
     * @param response Result after evaluating the math expression to be sent to the client.
     * @return Marshalled math response, ready to be directly sent.
     */
    public String buildServerResponse(String response) {
        Map<String, String> mapResponse = new HashMap<>();
        mapResponse.put("resp", response);
        return Protocol.marshal(mapResponse);
    }
}

/**
 * Class to accept and ACK client hellos. This class also creates threads to handle all future client requests.
 */
public class TCPServer {
    /**
     * Constructor for this class, starts a TCP server, then creates an infinite loop to listen and ACK for client hellos, then creates a thread to handle all future client requests.
     * @param port Port on which the server listens to incoming requests.
     * @throws Exception If a client leaves abruptly and the server cannot read or send messages anymore.
     */
    public TCPServer(Integer port) throws Exception {
        ServerSocket welcomeSocket = new ServerSocket(port);
        System.out.println("Server listening to connections on port " + port);

        while (true) {
            // Accept connection and ACK
            Socket clientSocket = welcomeSocket.accept();

            BufferedReader inFromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            DataOutputStream outToClient = new DataOutputStream(clientSocket.getOutputStream());

            // TODO: Log connection
            Map<String, String> clientHello = Protocol.unmarshal(inFromClient.readLine());
            outToClient.writeBytes(buildClientHelloACK(clientHello.get("name")));

            // Create new thread to handle future client requests
            System.out.println("Client has joined: " + clientHello.get("name"));
            new TCPServerThread(clientSocket, clientHello.get("name")).start();
            // TODO: Maybe implement another shutdown hook to cleanly close all sockets on Ctrl+C
        }
    }

    /**
     * Builds a marshalled response to ACK a client's hello.
     * @param name Name of the client to send back.
     * @return Marshalled ACK response, ready to be directly sent.
     */
    public String buildClientHelloACK(String name) {
        Map<String, String> mapResponse = new HashMap<>();
        mapResponse.put("resp", "Hello, " + name);
        return Protocol.marshal(mapResponse);
    }
}
