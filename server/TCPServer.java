package server;

import lib.Protocol;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class to store data about clients and unfinished commands from clients.
 */
class ClientStore {
    /**
     * <code>SocketAddress</code>es are mapped to their unfinished commands, stored as <code>String</code>s.
     */
    private Map<SocketAddress, String> commands;

    /**
     * <code>SocketAddress</code>es are mapped to their client's name, stored as <code>String</code>s.
     */
    private Map<SocketAddress, String> names;

    /**
     * <code>SocketAddress</code>es are mapped to their <code>SocketChannel</code>, used in the ShutdownHook to cleanly close client connections.
     */

    public Map<SocketAddress, SocketChannel> clients;

    /**
     * Constructor for this class, simply creates a new <code>HashMap</code> to store <code>SocketAddress</code> mapped to a <code>String</code>.
     */
    public ClientStore() {
        this.commands = new HashMap<>();
        this.names = new HashMap<>();
        this.clients = new HashMap<>();
    }

    /**
     * Adds a client to the store. Stores the <code>SocketChannel</code> and the client's name.
     *
     * @param key  Client to which the name and <code>SocketChannel</code> is associated.
     * @param name Name of the client.
     * @throws IOException If the socket is closed.
     */
    public void addClient(SocketChannel key, String name) throws IOException {
        this.clients.put(key.getLocalAddress(), key);
        this.names.put(key.getLocalAddress(), name);
    }

    /**
     * Removes a client from all the store.
     *
     * @param key  Client to remove.
     * @throws IOException If the socket is closed.
     */
    public void removeClient(SocketAddress key) throws IOException {
        this.clients.remove(key);
        this.names.remove(key);
        this.commands.remove(key);
    }

    /**
     * @param key Client whose command you would like to retrieve.
     * @return The Name of the client.
     * @throws IOException If the socket is closed.
     */
    public String getName(SocketChannel key) throws IOException {
        return this.names.get(key.getLocalAddress());
    }

    /**
     * Creates/Updates a client's command string.
     *
     * @param key   Client to which the command is associated.
     * @param value Command which is client is sending.
     * @throws IOException If the socket is closed.
     */
    public void addCommand(SocketChannel key, String value) throws IOException {
        this.commands.merge(key.getLocalAddress(), value, (a, b) -> a + b);
    }

    /**
     * Removes and returns the command associated with a client.
     *
     * @param key Client whose command you would like to retrieve and remove.
     * @return The command that was just removed.
     * @throws IOException If the socket is closed.
     */
    public String removeCommand(SocketChannel key) throws IOException {
        return this.commands.remove(key.getLocalAddress());
    }

    /**
     * Checks if a client's command has fully arrived by seeing if the command ends with <code>\n</code>, as the protocol defines that all messages end with <code>\n</code>.
     *
     * @param key Client whose command you would like to check if it has fully arrived.
     * @return Whether the full command has arrived or not.
     * @throws IOException If the socket is closed.
     */
    public Boolean isCommandComplete(SocketChannel key) throws IOException {
        return this.commands.get(key.getLocalAddress()).endsWith("\n");
    }
}

/**
 * Class to accept connections and service future client requests.
 */
public class TCPServer {
    private static final Logger serverLogger = Logger.getLogger(TCPServer.class.getName());

    /**
     * Constructor for this class, starts a TCP server, then creates an infinite loop to listen and respond to client messages.
     *
     * @param port Port on which the server listens to incoming requests.
     * @throws Exception If a client leaves abruptly and the server cannot read or send messages anymore.
     */
    public TCPServer(Integer port) throws Exception {
        // Creating handler for server logging, then adding it to the logger
        Handler fileHandler = new FileHandler("./TCPServer.log", true);
        serverLogger.addHandler(fileHandler);
        fileHandler.setLevel(Level.ALL);
        serverLogger.setLevel(Level.ALL);
        serverLogger.setUseParentHandlers(false);

        Selector selector = Selector.open();
        ClientStore clients = new ClientStore();
        ServerSocketChannel server = ServerSocketChannel.open();
        server.bind(new InetSocketAddress(port));
        server.configureBlocking(false);
        server.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("Server listening to connections on port " + port);

        while (true) {
            selector.select();
            Set<SelectionKey> keys = selector.selectedKeys();
            Iterator<SelectionKey> i = keys.iterator();
            while (i.hasNext()) {
                SelectionKey key = i.next();

                if (key.isAcceptable()) {
                    SocketChannel client = ((ServerSocketChannel) key.channel()).accept();
                    client.configureBlocking(false);
                    client.register(selector, SelectionKey.OP_READ);
                }
                if (key.isReadable()) {
                    SocketChannel client = (SocketChannel) key.channel();
                    String command = this.readFromBuffer(client, 2048);
                    clients.addCommand(client, command);

                    if (clients.isCommandComplete(client)) {
                        Map<String, String> request = Protocol.unmarshal(clients.removeCommand(client));
                        switch (request.get("cmd")) {
                            case "hello": {
                                client.write(buildClientHelloACK(request.get("name")));
                                System.out.println("Client has connected: " + request.get("name"));
                                clients.addClient(client, request.get("name"));
                                serverLogger.log(Level.INFO, "Client joined. Name: " + request.get("name"));
                                break;
                            }
                            case "math": {
                                String equationResponse = evaluateEquation(request.get("eq"));
                                client.write(buildServerResponse(equationResponse));
                                serverLogger.log(Level.INFO, "Client \"" + clients.getName(client) + "\" entered equation : " + request.get("eq") + ". Response : " + equationResponse);
                                break;
                            }
                            case "exit": {
                                client.write(buildClientExitACK(request.get("name")));
                                String name = clients.getName(client);
                                SocketAddress clientSocketAddress = client.getLocalAddress();
                                client.close();
                                clients.removeClient(clientSocketAddress);
                                System.out.println("Client has left: " + name);
                                serverLogger.log(Level.INFO, "Client disconnected. Name: " + name);
                                break;
                            }
                            default: {
                                client.write(buildServerResponse("Unknown command"));
                                serverLogger.log(Level.INFO, "Client \"" + clients.getName(client) + "\" entered unknown command: " + request.get("cmd"));
                                break;
                            }
                        }
                    }
                }
                i.remove();
            }
        }
    }

    /**
     * Read bytes from a SocketChannel into a buffer, then converts and returns a UTF-8 string
     *
     * @param client Client from which bytes must be read.
     * @param size   Size of the buffer into which all available bytes will be read.
     * @return Converted string from the buffer.
     * @throws IOException If the socket can no longer be read from.
     */
    private String readFromBuffer(SocketChannel client, int size) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(size);
        client.read(buffer);
        buffer.flip();
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
        return (decoder.decode(buffer)).toString();
    }

    /**
     * Builds a marshalled response to ACK a client's hello.
     *
     * @param name Name of the client to send back.
     * @return Marshalled ACK response, ready to be directly sent.
     */
    public ByteBuffer buildClientHelloACK(String name) {
        Map<String, String> mapResponse = new HashMap<>();
        mapResponse.put("resp", "Hello, " + name);
        return ByteBuffer.wrap(Protocol.marshal(mapResponse).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Builds a marshalled response to a client's exit command.
     *
     * @param name Name of the client to send back.
     * @return Marshalled exit response, ready to be directly sent.
     */
    public ByteBuffer buildClientExitACK(String name) {
        Map<String, String> mapResponse = new HashMap<>();
        mapResponse.put("resp", "Bye, " + name);
        return ByteBuffer.wrap(Protocol.marshal(mapResponse).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Builds a marshalled response to a client's math command.
     *
     * @param response Result after evaluating the math expression to be sent to the client.
     * @return Marshalled math response, ready to be directly sent.
     */
    public ByteBuffer buildServerResponse(String response) {
        Map<String, String> mapResponse = new HashMap<>();
        mapResponse.put("resp", response);
        return ByteBuffer.wrap(Protocol.marshal(mapResponse).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Determines if a given character is a numerical digit. Used in parsing an input from client.
     *
     * @param d Character to be analyzed.
     * @return True if the character is a digit, false otherwise.
     */
    public boolean isDigit(char d) {
        return d >= 48 && d <= 57;
    }

    /**
     * Determines if a given character is an operation symbol supported by the program.
     * The supported operations are multiplication, division, addition, subtraction, exponentiation, modulo, and factorial.
     * 
     * @param op Character to be analyzed.
     * @return True if the character is a supported operation symbol, false otherwise.
     */
    public boolean isOperation(char op) {
        return op == '*' || op == '/' || op == '+' || op == '-' || op == '%' || op == '^' || op == '!';
    }

    /**
     * Takes in an equation command from the client and formulates a response. It will attempt to parse the equation and return the result, or an error message if unsuccessful.
     *
     * @param equation  A string input from the client to be parsed and solved.
     * @return  The result of the equation, or an error message if the equation couldn't be parsed or solved.
     */
    public String evaluateEquation(String equation) {
        String arg1 = "";
        char operator = '#';
        String arg2 = "";

        int nextIndex = 0;
        boolean foundDecimal = false;
        String errorMessage = "";

        // Parsing first argument and the operator
        for (int i = 0; i < equation.length(); i++) {
            if (isDigit(equation.charAt(i))) {
                arg1 += equation.charAt(i);               // Append digit to first argument
            } else if (equation.charAt(i) == '.') {
                if (foundDecimal == false) {
                    foundDecimal = true;
                    arg1 += equation.charAt(i);           // Appends decimal to argument, but only if it's the first decimal found in this number
                } else {
                    arg1 += equation.charAt(i);
                    errorMessage = "Error - Too many decimal points in first argument. {" + arg1 + "}";
                }
            } else if (isOperation(equation.charAt(i))) {
                operator = equation.charAt(i);
            } else {
                errorMessage = "Error - Unrecognized symbol. {" + equation.charAt(i) + "} Please use only digits, operators, and decimal points.";
            }

            if (errorMessage != "" || operator != '#')     // If operator is found or an error occured, exit the loop.
            {
                nextIndex = i + 1;
                break;
            }
        }

        if(errorMessage!="")
        {
            return(errorMessage);   // If error occured, return it instead of result
        }

        foundDecimal = false;       // Resets count for the second argument

        // Parsing second argument
        for (int i = nextIndex; i < equation.length(); i++) {
            if (isDigit(equation.charAt(i))) {
                arg2 += equation.charAt(i);
            } else if (equation.charAt(i) == '.') {
                if (foundDecimal == false) {
                    foundDecimal = true;
                    arg2 += equation.charAt(i);
                } else {
                    arg2 += equation.charAt(i);
                    errorMessage = "Error - Too many decimal points in second argument. {" + arg2 + "}";
                }
            } else if (isOperation(equation.charAt(i))) {
                errorMessage = "Error - Too many operators. {" + operator + ", " + equation.charAt(i) + "}";
            } else {
                errorMessage = "Error - Unrecognized symbol. {" + equation.charAt(i) + "} Please use only digits, operators, and decimal points.";
            }

            // This loop will stop when all of the digits are successfully processed or an error occurs.
            if (errorMessage != "") {
                break;
            }
        }

        if(operator == '!' && arg2 != "") {
            errorMessage = "Error - Found second argument for factorial. {" + arg2 + "} Please use only one integer argument, followed by '!'.";
        }


        if(errorMessage!="") {
            return(errorMessage); // If error occured, return it instead of result
        }

        // Try to parse floats from the arguments
        float argf1, argf2;

        try {
            argf1 = Float.parseFloat(arg1);
        } catch (Exception e) {
            errorMessage = "Error - Failed to parse float from arg1. {" + e + "}";
            return errorMessage;
        }

        if(operator != '!') {
            try {
                argf2 = Float.parseFloat(arg2);
            } catch (Exception e) {
                errorMessage = "Error - Failed to parse float from arg2. {" + e + "}";
                return errorMessage;
            }
        }
        else{
            argf2 = 0f;
        }

        float resultf = 0;

        // Perform operation based on operator
        switch (operator) {
            case '*':
                resultf = argf1 * argf2;
                break;
            case '/':
                resultf = argf1 / argf2;
                break;
            case '%':
                if(argf2 == 0f)
                {
                    return "Undefined";
                }
                else{
                    resultf = argf1 % argf2;
                    break;
                }
            case '^':
                resultf = (float) Math.pow(argf1, argf2);
                break;
            case '+':
                resultf = argf1 + argf2;
                break;
            case '-':
                resultf = argf1 - argf2;
                break;
            case '!':
                if(argf1 % 1 == 0 && argf1 >= 0){
                    resultf = 1.0f;
                    for(int i = 1; i <= (int) argf1; i++){
                        resultf = Math.round(resultf*i);
                    }
                    break;
                }
                else{
                    errorMessage = "Error - The argument for factorial must be a positive integer >= 0. {" + argf1 + "}";
                    return errorMessage;
                }
            default:
                errorMessage = "Error - Unrecognized operator in evaluation step. {" + operator + "}";
                return errorMessage;
        }
        return Float.toString(resultf);
    }

}
