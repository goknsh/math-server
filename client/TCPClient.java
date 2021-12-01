package client;

import lib.Protocol;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Class (shutdown hook) to gracefully send the server a client exit command and quit.
 */
class ShutdownHook extends Thread {
    /**
     * The client on which the shutdown hook is to be executed.
     */
    private TCPClient client;

    /**
     * Constructor for this class, simply stores the TCPClient class so that it can send and receive a response to the client exit command.
     *
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
    /**
     * Data stream to write to the server.
     */
    public DataOutputStream outToServer;
    /**
     * Data stream to receive from the server.
     */
    public BufferedReader inFromServer;
    /**
     * Name of this client, as specified by the user.
     */
    private String name;

    /**
     * Creates and sends a client hello, then validates the server's response to the client hello.
     *
     * @param name Name of the client.
     * @param host Host of the server to communicate with.
     * @param port Port of the server to communicate with.
     */
    public TCPClient(String name, String host, Integer port) {
        try {
            Map<String, String> clientHelloACK;
            Socket serverSocket = new Socket(host, port);

            DataOutputStream outToServer = new DataOutputStream(serverSocket.getOutputStream());
            BufferedReader inFromServer = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));

            outToServer.writeBytes(buildClientHello(name));
            clientHelloACK = Protocol.unmarshal(inFromServer.readLine());

            if (Objects.equals(clientHelloACK.get("resp"), "Hello, " + name)) {
                this.name = name;
                this.outToServer = outToServer;
                this.inFromServer = inFromServer;

                Runtime.getRuntime().addShutdownHook(new ShutdownHook(this));
            } else {
                serverSocket.close();
                System.out.println("Server sent incorrect acknowledgement. Try again later.");
                Runtime.getRuntime().halt(1);
            }
        } catch (ConnectException e) {
            System.out.println("Could not connect to server. This typically occurs because the connection was refused remotely (e.g., no process is listening on the remote address/port).");
            Runtime.getRuntime().halt(1);
        } catch (IOException e) {
            System.out.println("Could not read/write to server. This typically occurs if the server has been shut down. Try again later.");
            Runtime.getRuntime().halt(1);
        }
    }

    /**
     * Builds and sends a math command to the server. Returns the server's response.
     *
     * @param input Math expression for the server to evaluate.
     * @return Server's response to the math command.
     * @throws IOException In case we cannot read a line from the server.
     */
    public String buildAndSendMathCommand(String input) throws IOException {
        this.outToServer.writeBytes(buildClientMathCommand(input));
        return Protocol.unmarshal(this.inFromServer.readLine()).get("resp");
    }

    /**
     * Builds a marshalled client hello.
     *
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
     *
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
     *
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
     *
     * @return Name of this client.
     */
    public String getName() {
        return this.name;
    }
}
