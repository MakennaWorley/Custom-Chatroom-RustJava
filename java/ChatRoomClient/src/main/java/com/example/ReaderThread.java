/**
 * This thread is passed a socket that it reads from. Whenever it gets input
 * it writes it to the ChatScreen text area using the displayMessage() method.
 */
package com.example;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.HashMap;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;



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

			while (true) {
				String message = fromServer.readLine();
				if (message == null) {
					System.out.println("Server connection closed.");
					break;
				}

				System.out.println("Message received: " + message);

				// Handle JSON or plain commands
				if (isJSONValid(message)) {
					processJsonMessage(message);
				} else {
					processCommand(message);
				}

			}
		}
		catch(IOException ioe){
				System.out.println("Error reading from server: " + ioe.getMessage());
		}
	}

	private void processJsonMessage(String message) {
		try {
			// Parse the JSON object
			ObjectMapper mapper = new ObjectMapper();
			JsonNode jsonNode = mapper.readTree(message);

			// Extract relevant fields
			String header = jsonNode.get("header").asText().trim();
			String clientMessage = jsonNode.get("message").asText();
			String sender = jsonNode.get("sender").asText();
			String timestamp = jsonNode.get("timestamp").asText();

			// Handle based on the header type
			if ("@all".equals(header)) {
				screen.displayMessage(sender + " : " + clientMessage + "\n\n\n" + timestamp);
			} else {
				screen.displayMessage(sender + " [Private]: " + clientMessage + "\n\n\n" + timestamp);
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Failed to parse JSON payload: " + message);
		}
	}

	private void processCommand(String message) {
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
						screen.displayMessage("Welcome to the chatroom " + username + "!\n");
					} else if (payload.startsWith("BYE")) {
						JOptionPane.showMessageDialog(null, "Disconnected from the server.", "Disconnected", JOptionPane.INFORMATION_MESSAGE);
						System.out.println("Server requested disconnection.");
						System.exit(0); // Close the application
					} else if (payload.startsWith("BOARD")) {
						// Extract JSON from payload (after "BOARD ")
						String json = payload.substring(6).trim();

						try {
							// Parse JSON into a HashMap
							ObjectMapper mapper = new ObjectMapper();
							HashMap<String, String> userBoard = mapper.readValue(json, HashMap.class);

							// Build and display the user board
							StringBuilder boardDisplay = new StringBuilder("User Board:\n\n");
							userBoard.forEach((username, status) ->
									boardDisplay.append(username).append(": ").append(status).append("\n")
							);

							JOptionPane.showMessageDialog(null, boardDisplay.toString(), "User Board", JOptionPane.INFORMATION_MESSAGE);
						} catch (IOException e) {
							System.err.println("Failed to parse user board JSON: " + json);
							e.printStackTrace();
						}
						}
					else if (payload.startsWith("SENT")) {
						processJsonMessage(screen.jsonMessage);
					}
					break;
				case "400":
					if (payload.startsWith("MESSAGE")) {
						JOptionPane.showMessageDialog(screen, "Message failed to be sent to the server.", "Message Failed", JOptionPane.ERROR_MESSAGE);
					}
					else if (payload.startsWith("INVALID")) {
						JOptionPane.showMessageDialog(screen, "Invalid message format. Message failed to be sent to the server.", "Message Failed", JOptionPane.ERROR_MESSAGE);
					}
					break;
				case "500":
					JOptionPane.showMessageDialog(screen, "Server error.", "Server Error", JOptionPane.ERROR_MESSAGE);

				default:
					System.out.println("Unknown command from server: " + command);
					break;
			}
	}


	private boolean isJSONValid(String message) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			JsonNode jsonNode = mapper.readTree(message); // Attempt to parse as JSON

			// Check for specific keys to validate the structure
			return jsonNode.has("header") &&
					jsonNode.has("message") &&
					jsonNode.has("sender") &&
					jsonNode.has("timestamp");
		} catch (IOException e) {
			return false; // Not valid JSON
		}
	}
}

