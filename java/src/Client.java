import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client {
    public static void main(String[] args) {
        String serverAddress = "127.0.0.1";
        int port = 8000;

        try (Socket socket = new Socket(serverAddress, port);
            BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
            PrintWriter serverWriter = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader serverReader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            System.out.println("Requesting access to chat room. Please select a username that has between 3 and 30 characters and only alphanumeric symbols.");

            String username;
            while (true) {
                System.out.print("Enter username: ");
                username = consoleReader.readLine();

                if (username == null || username.length() < 3 || username.length() > 30 || !username.matches("[a-zA-Z0-9]+")) {
                    System.out.println("Invalid username. Try again.");
                } else {
                    break;
                }
            }

            serverWriter.println(username);

            System.out.println("Connected to server. You can start sending messages:");
            String message;
            while ((message = consoleReader.readLine()) != null) {
                serverWriter.println(message);

                // Reading response from server
                String response = serverReader.readLine();
                if (response != null) {
                    System.out.println("Server: " + response);
                }
            }

        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
