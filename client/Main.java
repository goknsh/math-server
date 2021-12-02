package client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * Invokes TCPClient to establish a connection to the server, then creates an infinite loop, accepting and forwarding math commands to the TCPClient class.
 */
public class Main {
    private static final String SERVER_IP = "127.0.0.1";
    private static final int PORT_NUMBER = 6789;

    public static void main(String[] args) {
        Scanner reader = new Scanner(System.in);
        System.out.println("                __  .__                                                      \n" +
                "  _____ _____ _/  |_|  |__             ______ ______________  __ ___________ \n" +
                " /     \\\\__  \\\\   __\\  |  \\   ______  /  ___// __ \\_  __ \\  \\/ // __ \\_  __ \\\n" +
                "|  Y Y  \\/ __ \\|  | |   Y  \\ /_____/  \\___ \\\\  ___/|  | \\/\\   /\\  ___/|  | \\/\n" +
                "|__|_|  (____  /__| |___|  /         /____  >\\___  >__|    \\_/  \\___  >__|   \n" +
                "      \\/     \\/          \\/               \\/     \\/                 \\/       \n\n" +
                "A single-threaded server that evaluates mathematical expressions.\n\n");
        System.out.print("Provide your name to connect to the server: ");
        String name = reader.nextLine();

        System.out.println("Establishing connection...");
        // Creates TCP client that connects to the SERVER_IP at PORT_NUMBER
        TCPClient client = new TCPClient(name, SERVER_IP, PORT_NUMBER);
        System.out.println("Connection established. Hello, " + name);

        while (true) {
            System.out.println("\nEnter a simple equation. (Operators: *, /, +, -, ^, %, !)");
            System.out.println("You may send 'quit' or press Ctrl+C to quit.");
            System.out.print(client.getName() + "@math-server> ");

            // Handle edge case where Java looks at reading input where none exists while running the shutdown hook if this line were not present
            while (!reader.hasNextLine()) {
            }

            String command = reader.nextLine().trim();
            if ((command.getBytes(StandardCharsets.UTF_8)).length < 1024) {
                switch (command) {
                    case "exit":
                    case "e":
                    case "quit":
                    case "q": {
                        // Closes the connection with client
                        reader.close();
                        System.exit(0);
                        break;
                    }
                    case "help":
                    case "h":
                    case "usage": {
                        // Help message
                        System.out.println("exit or quit to close the program\n" +
                                "Any other unrecognized command is considered a mathematical expression and sent to the server\n" +
                                "help or usage to print this message\n");
                        break;
                    }
                    default: {
                        // Any other unrecognized command is considered a mathematical expression and sent to the server
                        try {
                            System.out.println("Response: " + client.buildAndSendMathCommand(command));
                            break;
                        } catch (IOException e) {
                            System.out.println("Could not read/write to server. This typically occurs if the server has been shut down. Try again later.");
                            Runtime.getRuntime().halt(0);
                        }
                    }
                }
            } else {
                System.out.println("Error: Command cannot exceed 1024 bytes in length.");
            }
        }
    }
}
