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

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import java.io.*;
import java.net.*;


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
	private JButton exitButton;
	private JTextField sendText;
	private JTextArea displayArea;

	Color purple = new Color(151,153,186); // Purple color


	public static final int PORT = 8000;
	private Socket socket; //not sure about this one ------------------------


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

		JPanel southPanel = new JPanel();
		Border etched = BorderFactory.createEtchedBorder();
		Border titled = BorderFactory.createTitledBorder(etched, "Enter Message Here ...");
		southPanel.setBorder(titled);

		sendText = new JTextField(30);
		sendButton = new JButton("Send");
		exitButton = new JButton("Exit");

		sendText.addKeyListener(this);
		sendButton.addActionListener(this);
		exitButton.addActionListener(this);

		southPanel.add(sendText);
		southPanel.add(sendButton);
		southPanel.add(exitButton);

		displayArea = new JTextArea(15, 40);
		displayArea.setEditable(false);
		displayArea.setFont(new Font("SansSerif", Font.PLAIN, 14));

		JScrollPane scrollPane = new JScrollPane(displayArea);
		chatPanel.add(scrollPane, BorderLayout.CENTER);
		chatPanel.add(southPanel, BorderLayout.SOUTH);

		/** anonymous inner class to handle window closing events */
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent evt) {
				System.exit(0);
			}
		});
	}
//		/**
//		 * set up all the components
//		 */
//		sendText = new JTextField(30);
//		sendButton = new JButton("Send");
//		exitButton = new JButton("Exit");
//
//		/**
//		 * register the listeners for the different button clicks
//		 */
//		sendText.addKeyListener(this);
//		sendButton.addActionListener(this);
//		exitButton.addActionListener(this);
//
//		/**
//		 * add the components to the panel
//		 */
//		p.add(sendText);
//		p.add(sendButton);
//		p.add(exitButton);
//
//		/**
//		 * add the panel to the "south" end of the container
//		 */
//		getContentPane().add(p,"South");
//
//		/**
//		 * add the text area for displaying output. Associate
//		 * a scrollbar with this text area. Note we add the scrollpane
//		 * to the container, not the text area
//		 */
//		/**
//		 * set the title and size of the frame
//		 */
//		displayArea = new JTextArea(15, 40);
//		displayArea.setEditable(false);
//		displayArea.setFont(new Font("SansSerif", Font.PLAIN, 14));
//
//		JScrollPane scrollPane = new JScrollPane(displayArea);
//		getContentPane().add(scrollPane,"Center");
//
//		setTitle("The Cool Chatroom");
//		pack();
//
//		setVisible(true);
//		sendText.requestFocus();
//
//		/** anonymous inner class to handle window closing events */
//		addWindowListener(new WindowAdapter() {
//			public void windowClosing(WindowEvent evt) {
//				System.exit(0);
//			}
//		} );



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
			String username = usernameField.getText();

			// Validate username
			if (validateUsername(username)) {
				// If valid, connect to server and switch to chat panel
				connectToServer(username);
				cardLayout.show(mainPanel, "Chat");
			}
			else {
				usernameField.setText("");
			}
		} else if (source == sendButton) {
			String message = sendText.getText().trim();
			displayMessage(message);
			sendText.setText("");
			sendText.requestFocus();
		} else if (source == exitButton) {
			System.exit(0);
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

	private void connectToServer(String username) {
		/**
		try {
			socket = new Socket("localhost", PORT); // Replace with server address
			displayMessage("Connected as: " + username);

			// Start ReaderThread for incoming messages
			Thread readerThread = new Thread(new ReaderThread(socket, this));
			readerThread.start();
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this, "Failed to connect to server.", "Connection Error", JOptionPane.ERROR_MESSAGE);
		}
	 	**/
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
		try {
			Socket annoying = new Socket(args[0], PORT);
			ChatScreen win = new ChatScreen();

			String username = win.usernameField.getText();
			win.displayMessage("Welcome to the chatroom " + username);

			BufferedWriter toServer = new BufferedWriter(new OutputStreamWriter(annoying.getOutputStream()));
			toServer.write("JOIN " + username + "\n");
			toServer.flush();

			Thread ReaderThread = new Thread(new ReaderThread(annoying, win));


			ReaderThread.start();
		}
		catch (UnknownHostException uhe) { System.out.println(uhe); }
		catch (IOException ioe) { System.out.println(ioe); }

		//SwingUtilities.invokeLater(ChatScreen::new);

	}
}
