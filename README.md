# Custom Chatroom
Homework 4/Final for CMPT 352 Networks

This chat room mainly uses a Java client for the interface to the Rust server which follows the protocol listed as a pdf on this repo which was designed by Jesse Melanson, Makenna Worley, Scott Ruiz Gomez, and Shreeya Maskey.

Makenna created both the Rust server and a barebones Rust client and Shreeya built the Java client. The Rust client requires you to write ```JOIN Makenna``` and ```SEND {"header": "@all", "sender": "Makenna", "message": "Hi"}```. Therefore not recommended for most users just wanting to use this as a chatroom.

To run the server:

Make sure you have Rust installed on your device (https://doc.rust-lang.org/beta/book/ch01-01-installation.html)

Pull down the lastest changes from the main branch.

Then cd into the rust directory from this repo, run ```cargo build``` and then run from that same location ```cargo run --bin server```.


To run the client:

Make sure you have Java installed on your device (https://www.java.com/en/download/) and maven (https://maven.apache.org/download.cgi)

cd into java and run ```mvn clean install```

java ChatScreen <hostname>