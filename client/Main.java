package client;

import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * Invokes TCPClient to establish a connection to the server, then creates an infinite loop, accepting and forwarding math commands to the TCPClient class.
 */
public class Main {
    public static void main(String[] args) throws Exception {
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
        TCPClient client = new TCPClient(name, "127.0.0.1", 6789);
        System.out.println("Connection established. Hello, " + name);

        while (true) {
            System.out.println("\nPress Ctrl+C or send 'quit' to quit");
            System.out.print(client.getName() + "@math-server> ");
            if (reader.hasNextLine()) {
                String command = reader.nextLine();
                if ((command.getBytes(StandardCharsets.UTF_8)).length < 1024) {
                    switch (command) {
                        case "exit":
                        case "e":
                        case "quit":
                        case "q": {
                            System.exit(0);
                            break;
                        }
                        case "help":
                        case "h":
                        case "usage": {
                            System.out.println("exit or quit to close the program\n" +
                                    "any other unrecognized command is considered a mathematical expression and sent to the server\n" +
                                    "help or usage to print this message\n");
                            break;
                        }
                        default: {
                            System.out.println("Response: " + client.buildAndSendMathCommand(command));
                            break;

                        }
                    }
                } else {
                    System.out.println("Error: Command cannot exceed 1024 bytes in length.");
                }
            }
        }
    }
}
