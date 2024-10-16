import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {
    private static Set<PrintWriter> clientWriters = new HashSet<>();

    public static void main(String[] args) {
        System.out.println("Chat server started...");
        int port = 12345; // You can change the port number

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            // Start a thread to handle server input
            new Thread(new ServerInputHandler()).start();

            while (true) {
                // Accept clients
                new ClientHandler(serverSocket.accept()).start();
            }
        } catch (IOException e) {
            System.out.println("Error starting the server: " + e.getMessage());
        }
    }

    // Handles each client in a new thread
    private static class ClientHandler extends Thread {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                synchronized (clientWriters) {
                    clientWriters.add(out);
                }

                String message;
                while ((message = in.readLine()) != null) {
                    System.out.println("Received: " + message);
                    broadcastMessage(message);
                }
            } catch (IOException e) {
                System.out.println("Error handling client: " + e.getMessage());
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                synchronized (clientWriters) {
                    clientWriters.remove(out);
                }
            }
        }

        // Broadcasts the message to all clients
        private void broadcastMessage(String message) {
            synchronized (clientWriters) {
                for (PrintWriter writer : clientWriters) {
                    writer.println(message);
                }
            }
        }
    }

    // Handles server console input
    private static class ServerInputHandler implements Runnable {
        private BufferedReader consoleInput;

        public ServerInputHandler() {
            consoleInput = new BufferedReader(new InputStreamReader(System.in));
        }

        public void run() {
            String serverMessage;
            try {
                while (true) { // Keep running to read input
                    serverMessage = consoleInput.readLine(); // Wait for user input
                    if (serverMessage != null && !serverMessage.isEmpty()) {
                        broadcastServerMessage(serverMessage);
                    }
                }
            } catch (IOException e) {
                System.out.println("Error reading server input: " + e.getMessage());
            }
        }

        // Broadcasts messages entered from the server console
        private void broadcastServerMessage(String message) {
            synchronized (clientWriters) {
                for (PrintWriter writer : clientWriters) {
                    writer.println("Server: " + message);
                }
            }
        }
    }
}
