# Custom Chatroom
Homework 4 for CMPT 352 Networks

A Java client and Rust server for a chatroom following the pdf file included on this repo.

Makenna worked on the server (Rust) and Shreeya worked on the client (Java).

To run the server, make sure you have Rust installed on your device (https://doc.rust-lang.org/beta/book/ch01-01-installation.html)

Then cd into the rust directory from this repo, run ```cargo build``` and then run from that same location ```cargo run --bin server```. If you want to run the barebones rust client, which requires you know the protocol and sending messages like ```JOIN <username>\n``` or ```SEND {"header": "@all", "sender": "Hello", "message": "Hi"}\n```. Run ```cargo run --bin client```.