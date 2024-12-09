/**
 * This program is a rudimentary demonstration of Swing GUI programming.
 * Note, the default layout manager for JFrames is the border layout. This
 * enables us to position containers using the coordinates South and Center.
 *
 * Usage:
 *	java ChatScreen
 *
 * When the user enters text in the textfield, it is displayed backwards 
 * in the display area.
 */
package com.example;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.*;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import javax.swing.text.*;

public class ChatScreen extends JFrame implements ActionListener, KeyListener
{

	private CardLayout cardLayout;
	private JPanel mainPanel;

	// Declaring login screen components
	private JPanel loginPanel;
	private JTextField usernameField;
	private JButton joinButton;

	// Declaring chat screen components
	private JPanel chatPanel;
	private JButton sendButton;
	private JTextField sendText;
	private JPanel displayArea;
	private JButton userBoardButton;
	private JButton userStatusButton;
	private JButton leaveButton;


	//Declaring socket components
	private Socket socket = null;
    private BufferedWriter toServer;
	private static String hostname;
	private String username;

	public static final int PORT = 8000;
	public String jsonMessage;

	Color purple = new Color(151,153,186);
	Color lightpink = new Color(249,225,224);
	Color lightpurple = new Color(188,133,163);
	Color redish = new Color(149,20,29);


	public ChatScreen() {
		/**
		 * a panel used for placing components
		 */
		// Set up CardLayout
		cardLayout = new CardLayout();
		mainPanel = new JPanel(cardLayout);

		// Create Login Panel
		createLoginPanel();

		// Create Chat Panel
		createChatPanel();

		// Add panels to mainPanel
		mainPanel.add(loginPanel, "Login");
		mainPanel.add(chatPanel, "Chat");

		// Add mainPanel to JFrame
		getContentPane().add(mainPanel);
		setTitle("The Cool Chatroom");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		pack();
		setLocationRelativeTo(null);
		setVisible(true);
	}


	private void createLoginPanel() {

		loginPanel = new JPanel(new GridBagLayout());
		loginPanel.setBackground(purple);

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(5, 5, 5, 5);

		JLabel usernameLabel = new JLabel("Enter your username to join the chatroom:");
		usernameLabel.setFont(new Font("SansSerif", Font.PLAIN, 16));

		usernameField = new JTextField(20);
		joinButton = new JButton("Join");
		joinButton.addActionListener(this);


		// Add components to loginPanel
		gbc.gridx = 0;
		gbc.gridy = 0;
		loginPanel.add(usernameLabel, gbc);

		gbc.gridy = 1;
		loginPanel.add(usernameField, gbc);

		gbc.gridy = 2;
		gbc.anchor = GridBagConstraints.EAST;
		loginPanel.add(joinButton, gbc);
	}


	private void createChatPanel() {
		chatPanel = new JPanel(new BorderLayout());
		chatPanel.setPreferredSize(new Dimension(800, 600)); // Set the preferred size

		// Create the North panel for buttons
		JPanel northPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		userBoardButton = new JButton("User Board");
		userStatusButton = new JButton("User Status");
		leaveButton = new JButton("Leave");
		northPanel.setBackground(lightpurple);


		JPanel southPanel = new JPanel();
		Border etched = BorderFactory.createEtchedBorder();
		Border titled = BorderFactory.createTitledBorder(etched, "Enter Message Here ...");
		southPanel.setBorder(titled);

		/**
		* set up all the components
		*/
		sendText = new JTextField(30);
		sendButton = new JButton("Send");

		// Limit to 500 characters
		((AbstractDocument) sendText.getDocument()).setDocumentFilter(new LengthFilter(500));

		/**
		* register the listeners for the different button clicks
		*/
		sendText.addKeyListener(this);
		sendButton.addActionListener(this);
		userBoardButton.addActionListener(this);
		userStatusButton.addActionListener(this);
		leaveButton.addActionListener(this);

		userBoardButton.setBackground(lightpink);
		userBoardButton.setOpaque(true);
		userBoardButton.setBorderPainted(false);
		userStatusButton.setBackground(lightpink);
		userStatusButton.setOpaque(true);
		userStatusButton.setBorderPainted(false);
		leaveButton.setBackground(redish);
		leaveButton.setOpaque(true);
		leaveButton.setBorderPainted(false);
		leaveButton.setForeground(Color.WHITE);


		sendButton.setBackground(lightpurple);
		sendButton.setOpaque(true);
		sendButton.setBorderPainted(false);

		/**
		* add the components to the panel
		*/
		southPanel.add(sendText);
		southPanel.add(sendButton);
		northPanel.add(userBoardButton);
		northPanel.add(userStatusButton);
		northPanel.add(leaveButton);

		/**
		* set the title and size of the frame
		*/
		displayArea = new JPanel();
		displayArea.setLayout(new BoxLayout(displayArea, BoxLayout.Y_AXIS));

		//displayArea.setBackground(Color.WHITE);

		JScrollPane scrollPane = new JScrollPane(displayArea);
		chatPanel.add(scrollPane, BorderLayout.CENTER);
		// Add panels to the chatPanel
		chatPanel.add(northPanel, BorderLayout.NORTH);
		chatPanel.add(southPanel, BorderLayout.SOUTH);


		/** anonymous inner class to handle window closing events */
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent evt) {
				System.exit(0);
			}
		});
	}

	/**
	 * Displays a message
	 */
	public void displayMessage(String message) {
		// Create a JPanel to act as the message box
		JPanel messageBox = new JPanel();
		messageBox.setLayout(new GridBagLayout());

		String username = getUsername();
		// Extract the sender's name from the message
		String senderName = message.contains(" ") ? message.split(" ")[0].trim() : "";
		System.out.println("senderName " + senderName);
		// Check if the sender's name matches the username
		boolean isOwnMessage = senderName.equals(username);

		messageBox.setOpaque(true);
		if (isOwnMessage) {
			messageBox.setBackground(purple); // Light purple for own messages
		} else {
			messageBox.setBackground(new Color(220, 220, 220)); // Light gray for others
		}
		messageBox.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(Color.GRAY, 1), // Outer line border
				BorderFactory.createEmptyBorder(2, 2, 2, 2) // Padding inside the panel
		));

		// Create a JLabel for the text
		JLabel messageLabel = new JLabel(message);
		messageLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
		messageLabel.setHorizontalAlignment(SwingConstants.CENTER);

		// Add the JLabel to the message box
		messageBox.add(messageLabel);

		messageBox.setPreferredSize(new Dimension(messageLabel.getPreferredSize().width + 15, messageLabel.getPreferredSize().height + 15));

		// Wrap the messageBox in a JPanel to use FlowLayout
		JPanel wrapperPanel = new JPanel(new FlowLayout(isOwnMessage ? FlowLayout.RIGHT : FlowLayout.LEFT, 0, 0));
		wrapperPanel.setBackground(Color.WHITE); // Match the background of the display area

		//if the message starts with the username, flowlayout.right and change color to lightpurple

		wrapperPanel.add(messageBox);

		// Add the wrapper panel to the display panel
		displayArea.add(wrapperPanel);
		displayArea.add(Box.createVerticalStrut(5));

		// Repaint and revalidate to update the display
		displayArea.revalidate();
		displayArea.repaint();
	}


	/**
	 * This method responds to action events .... i.e. button clicks
	 * and fulfills the contract of the ActionListener interface.
	 */
	public void actionPerformed(ActionEvent evt) {
		Object source = evt.getSource();

		if (source == joinButton) {
			username = usernameField.getText();

			// Validate username
			if (validateUsername(username)) {
				// If valid, connect to server, send join request and switch to chat panel
				if (connectToServer()) {
                    sendJoinRequest(username);
                    cardLayout.show(mainPanel, "Chat");
					mainPanel.revalidate();
					mainPanel.repaint();
                }
			}
			else {
				usernameField.setText("");
			}
		} else if (source == sendButton) {
			String message = sendText.getText().trim();
			//send this to server
			// Split the input into recipients and message
			SplitMessage splitMessage = splitMessage(message);

			if (splitMessage != null) {
				String header = splitMessage.getHeader();
				String newMessage = splitMessage.getMessage();
				String username = getUsername();

				if (newMessage == null) {
					JOptionPane.showMessageDialog(this, "Message cannot be blank.", "Message Not Found", JOptionPane.ERROR_MESSAGE);
				}

				// Create JSON message
				jsonMessage = createMessageJson(username, header, newMessage);

				// Send the message to the server
				sendToServer(jsonMessage);
				System.out.println("What is being sent: " + jsonMessage);

				sendText.setText("");
				sendText.requestFocus();
			} else {
				JOptionPane.showMessageDialog(this, "Invalid format. Message cannot be blank.", "Message Not Found", JOptionPane.ERROR_MESSAGE);
			}
		} else if (source == leaveButton) {
			leaveRequest();
		}
		else if(source == userBoardButton) {
			getUserBoard();
		}
		else if (source == userStatusButton) {

		}
	}

	public SplitMessage splitMessage(String input) {
		// Initialize variables to hold recipients and message
		List<String> header = new ArrayList<>();
		String message = "";

		// Split the string by spaces
		String[] parts = input.trim().split("\\s+"); // Trim input and split by whitespace

		boolean hasAtSymbol = false;

		for (String part : parts) {
			if (part.startsWith("@")) {
				// Add all words starting with '@' to the recipients list
				header.add(part);
				hasAtSymbol = true;
			} else {
				// The rest is considered the message
				break;
			}
		}

		// If no @recipients were found, default to "@all"
		if (!hasAtSymbol) {
			header.add("@all");
			message = input.trim(); // Entire input is treated as the message
		} else {
			try{
				// Reconstruct the message part
				int startIndex = input.indexOf(parts[header.size()]);
				message = input.substring(startIndex).trim();
			}
			catch (ArrayIndexOutOfBoundsException e){
				JOptionPane.showMessageDialog(this, "Invalid format. Message cannot be blank.", "Message Not Found", JOptionPane.ERROR_MESSAGE);
			}

		}

		// Convert the header list to a single string of recipients
		String headerString = String.join(" ", header); // Joins the recipients with a space

		if (message.isEmpty()) {
			return null; // Return null if message is empty
		}

		return new SplitMessage(headerString, message);
	}

	// Helper class to hold the recipients and message together
	static class SplitMessage {
		private String header;
		private String message;

		public SplitMessage(String header, String message) {
			this.header = header;
			this.message = message;
		}

		public String getHeader() {
			return header;
		}

		public String getMessage() {
			return message;
		}
	}

	public static String createMessageJson(String sender, String header, String message) {
		// Create an object to hold the message details
		String timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
		Message messageObj = new Message(sender, header, timestamp, message);

		// Use Jackson to convert the object to a JSON string
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			return objectMapper.writeValueAsString(messageObj);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	// Message class to hold the message details
	static class Message {

		private String sender;
		private String header;
		private String message;
		private String timestamp;

		public Message(String sender, String header, String timestamp, String message) {
			this.sender = sender;
			this.header = header;
			this.message = message;
			this.timestamp = timestamp;
		}

		// Getters and Setters (optional, for Jackson)
		public String getSender() {
			return sender;
		}

		public void setSender(String sender) {
			this.sender = sender;
		}

		public String getHeader() {
			return header;
		}

		public void setHeader(String header) {
			this.header = header;
		}

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}

		public String getTimestamp() {
			return timestamp;
		}

		public void setTimestamp(String timestamp) {
			this.timestamp = timestamp;
		}
	}

	private boolean validateUsername(String username) {
		if (username.length() < 3 || username.length() > 30) {
			JOptionPane.showMessageDialog(this, "Username must be between 3 and 30 characters.", "Invalid Username", JOptionPane.ERROR_MESSAGE);
			return false;
		}

		if (!username.matches("[a-zA-Z0-9]+")) {
			JOptionPane.showMessageDialog(this, "Username must be alphanumeric and contain no spaces.", "Invalid Username", JOptionPane.ERROR_MESSAGE);
			return false;
		}

		if (username.equalsIgnoreCase("all")) {
			JOptionPane.showMessageDialog(this, "Username cannot be 'all'.", "Invalid Username", JOptionPane.ERROR_MESSAGE);
			return false;
		}

		return true;
	}

	public String getUsername() {
		return username;
	}

	private boolean connectToServer() {
		if (socket != null && !socket.isClosed()) {
			displayMessage("Already connected to server!");
			return true;
		}
		try {
            socket = new Socket(hostname, PORT);
            toServer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            displayMessage("Connected to server!");

			// Start ReaderThread immediately after successful connection
			Thread ReaderThread = new Thread(new ReaderThread(socket, this));
			ReaderThread.start();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to connect to server.", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
	}

	private void sendJoinRequest(String username) {
        try {
            toServer.write("JOIN " + username + "\n");
            toServer.flush();
			this.username = username;

        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to join the chat room.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

	private void leaveRequest() {
        try {
            toServer.write("LEAVE\n");
            toServer.flush();
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to leave the chat room.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

	private void sendToServer(String jsonMessage) {
		try {
			toServer.write("SEND " + jsonMessage + "\n");
			toServer.flush();
		} catch (IOException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, "Failed to send a message.", "Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void getUserBoard() {
		try {
			toServer.write("USERBOARD\n");
			toServer.flush();
			this.username = username;

		} catch (IOException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, "Failed to send request for the user board list.", "Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void getUserStatus() {
		//not implementing it currently
	}

	// Custom DocumentFilter to limit input length
	static class LengthFilter extends DocumentFilter {
		private final int maxLength;

		public LengthFilter(int maxLength) {
			this.maxLength = maxLength;
		}

		@Override
		public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
			if (fb.getDocument().getLength() + string.length() <= maxLength) {
				super.insertString(fb, offset, string, attr);
			} else {
				// Optionally, play a beep or show a warning
				Toolkit.getDefaultToolkit().beep();
			}
		}

		@Override
		public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
			if (fb.getDocument().getLength() - length + text.length() <= maxLength) {
				super.replace(fb, offset, length, text, attrs);
			} else {
				// Optionally, play a beep or show a warning
				Toolkit.getDefaultToolkit().beep();
			}
		}
	}

	/**
	 * These methods responds to keystroke events and fulfills
	 * the contract of the KeyListener interface.
	 */

	/**
	 * This is invoked when the user presses
	 * the ENTER key.
	 */
	public void keyPressed(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_ENTER) {
			sendButton.doClick();
		}
	}

	/** Not implemented */
	public void keyReleased(KeyEvent e) { }

	/** Not implemented */
	public void keyTyped(KeyEvent e) {  }

	public static void main(String[] args) {
		if (args.length != 1) {
			System.out.println("Usage: java ChatScreen <hostname>");
			System.exit(1);
		}
		hostname = args[0];

		ChatScreen win = new ChatScreen();
	}
}
