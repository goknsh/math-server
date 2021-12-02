package server;

/**
 * Creates the server at port 6789.
 */
public class Main {
    private static final int PORT_NUMBER = 6789;
    public static void main(String[] args) throws Exception {
        TCPServer server = new TCPServer(PORT_NUMBER);
    }
}