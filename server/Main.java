package server;

/**
 * Creates the server at port 6789.
 */
public class Main {
    public static void main(String[] args) throws Exception {
        TCPServer server = new TCPServer(6789);
    }
}
