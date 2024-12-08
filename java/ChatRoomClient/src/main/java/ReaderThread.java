/**
 * This thread is passed a socket that it reads from. Whenever it gets input
 * it writes it to the ChatScreen text area using the displayMessage() method.
 */

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class ReaderThread implements Runnable
{
	Socket server;
	BufferedReader fromServer;
	ChatScreen screen;

	public ReaderThread(Socket server, ChatScreen screen) {
		this.server = server;
		this.screen = screen;
	}

	public void run() {
		
		try {
			fromServer = new BufferedReader(new InputStreamReader(server.getInputStream()));
			System.out.println("Socket successfully passed over: " + server);

			while (true) {
				System.out.println("Waiting for message from server...");
				String message = fromServer.readLine();
				if (message == null) {
					System.out.println("Server connection closed.");
					break;
				}
				// now display it on the display area
				// Parse the message
				String[] parts = message.split(" ", 2); // Split into command and the rest of the message
				String command = parts[0]; // The first part is the command
				String payload = parts.length > 1 ? parts[1] : ""; // The second part is the payload (if any)

				// Handle commands
				switch (command) {
					case "200":
						if (payload.startsWith("OK")) {
							String username = screen.getUsername();
							screen.displayMessage("Joined chatroom as " + username);
							System.out.println("User joined: " + username);
						} else if (payload.startsWith("BYE")) {
							JOptionPane.showMessageDialog(null, "Disconnected from the server.", "Disconnected", JOptionPane.INFORMATION_MESSAGE);
							System.out.println("Server requested disconnection.");
							System.exit(0); // Close the application
						} else if (payload.startsWith("BOARD")) {

						}
						break;

					default:
						System.out.println("Unknown command from server: " + command);
						break;
				}

		// 		//displayMessage("Joined chat as " + username);

		// 		//so continually be reading from the server and do statments
		// 		//if message is 200 OK -> display "joinned chatroom!"
		// 		//if message is SEND JSON file -> display "message from:_ and message:_"
			}
		}
		catch (IOException ioe) { 
			System.out.println("Error reading from server: " + ioe.getMessage());
		}



	}
}
