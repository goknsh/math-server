package client;

import lib.Protocol;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Class (shutdown hook) to gracefully send the server a client exit command and quit.
 */
class ShutdownHook extends Thread {
    private TCPClient client;

    /**
     * Constructor for this class, simply stores the TCPClient class so that it can send and receive a response to the client exit command.
     * @param client Client on behalf of whom an exit command will be sent.
     */
    public ShutdownHook(TCPClient client) {
        this.client = client;
    }

    /**
     * Send a client exit command, and receive the response to it.
     */
    public void run() {
        try {
            System.out.println("\n\nSending quit to server...");
            client.outToServer.writeBytes(client.buildClientExitCommand());
            Map<String, String> clientExitACK = Protocol.unmarshal(client.inFromServer.readLine());

            if (Objects.equals(clientExitACK.get("resp"), "Bye, " + client.getName())) {
                System.out.println("Quit successfully. Server says: " + clientExitACK.get("resp"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

/**
 * Class to establish a connection and communicate with the server.
 */
public class TCPClient {
    private Socket server;
    private String name;
    public DataOutputStream outToServer;
    public BufferedReader inFromServer;

    /**
     * Creates and sends a client hello, then validates the server's response to the client hello.
     * @param name Name of the client.
     * @param host Host of the server to communicate with.
     * @param port Port of the server to communicate with.
     * @throws Exception If the server leaves abruptly and the client cannot read or send messages anymore.
     */
    public TCPClient(String name, String host, Integer port) throws Exception {
        Map<String, String> clientHelloACK;
        Socket serverSocket = new Socket(host, port);

        DataOutputStream outToServer = new DataOutputStream(serverSocket.getOutputStream());
        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));

        outToServer.writeBytes(buildClientHello(name));
        clientHelloACK = Protocol.unmarshal(inFromServer.readLine());

        if (Objects.equals(clientHelloACK.get("resp"), "Hello, " + name)) {
            this.server = serverSocket;
            this.name = name;
            this.outToServer = outToServer;
            this.inFromServer = inFromServer;

            Runtime.getRuntime().addShutdownHook(new ShutdownHook(this));
        } else {
            serverSocket.close();
            throw new Exception("Server sent incorrect acknowledgement");
        }
    }

    // TODO: Implement
    public String buildAndSendMathCommand(String rawInput) {
        return "NOT_YET_IMPLEMENTED";
    }

    /**
     * Builds a marshalled client hello.
     * @param name Name of the client to send to the server.
     * @return Marshalled client hello, ready to be directly sent.
     */
    public String buildClientHello(String name) {
        Map<String, String> mapResponse = new HashMap<>();
        mapResponse.put("cmd", "hello");
        mapResponse.put("name", name);
        return Protocol.marshal(mapResponse);
    }

    /**
     * Builds a marshalled math command.
     * @param eq Equation the client wants the server to evaluate.
     * @return Marshalled math command, ready to be directly sent.
     */
    public String buildClientMathCommand(String eq) {
        Map<String, String> mapResponse = new HashMap<>();
        mapResponse.put("cmd", "math");
        mapResponse.put("eq", eq);
        return Protocol.marshal(mapResponse);
    }

    /**
     * Builds a marshalled exit command.
     * @return Marshalled exit command, ready to be directly sent.
     */
    public String buildClientExitCommand() {
        Map<String, String> mapResponse = new HashMap<>();
        mapResponse.put("cmd", "exit");
        mapResponse.put("name", this.name);
        return Protocol.marshal(mapResponse);
    }

    /**
     * Get the privately stored name of this client.
     * @return Name of this client.
     */
    public String getName() {
        return this.name;
    }
}
