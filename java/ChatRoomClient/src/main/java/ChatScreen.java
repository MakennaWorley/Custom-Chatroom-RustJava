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

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.*;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;


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
	private JTextArea displayArea;
	private JButton userBoardButton;
	private JButton userStatusButton;
	private JButton leaveButton;


	//Declaring socket components
	private Socket socket = null;
    private BufferedWriter toServer;
	private static String hostname;
	private String username;

	public static final int PORT = 8000;

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

		JPanel westPanel = new JPanel();

		JPanel southPanel = new JPanel();
		Border etched = BorderFactory.createEtchedBorder();
		Border titled = BorderFactory.createTitledBorder(etched, "Enter Message Here ...");
		southPanel.setBorder(titled);

		/**
		* set up all the components
		*/
		sendText = new JTextField(30);
		sendButton = new JButton("Send");

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
		displayArea = new JTextArea(15, 40);
		displayArea.setEditable(false);
		displayArea.setFont(new Font("SansSerif", Font.PLAIN, 14));

		JScrollPane scrollPane = new JScrollPane(displayArea);
		chatPanel.add(scrollPane, BorderLayout.CENTER);
		// Add panels to the chatPanel
		chatPanel.add(northPanel, BorderLayout.NORTH);
		chatPanel.add(southPanel, BorderLayout.SOUTH);
		chatPanel.add(westPanel, BorderLayout.WEST);

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
		displayArea.append(message + "\n");
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
			displayMessage(message);
			sendText.setText("");
			sendText.requestFocus();
		} else if (source == leaveButton) {
			leaveRequest();
		}
		else if(source == userBoardButton) {

		}
		else if (source == userStatusButton) {

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
		if (e.getKeyCode() == KeyEvent.VK_ENTER)
			joinButton.addActionListener(this);
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
