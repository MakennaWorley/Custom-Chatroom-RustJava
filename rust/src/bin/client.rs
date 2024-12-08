use std::net::TcpStream;
use std::io::{self, Write, Read, stdin};
use std::sync::{Arc, Mutex};
use std::thread;
use std::collections::VecDeque;
use serde_json::Value;
use chrono::Utc;
use std::time::Duration;

fn main() -> io::Result<()> {
    let local_ip = local_ip_address::local_ip().expect("Could not get local IP");
    let address = format!("{}:{}", local_ip, 8000);
    let stream = TcpStream::connect(&address).expect("Could not connect to server");

    let sender_stream = Arc::new(Mutex::new(stream.try_clone().expect("Failed to clone stream for sender")));
    let receiver_stream = Arc::new(Mutex::new(stream));

    println!("Connected to server at {}", address);

    // Spawning thread for sending messages to server
    let sender_thread = {
        let mut input = String::new();
        let mut stdout = io::stdout();

        let sender_stream = Arc::clone(&sender_stream);
        thread::spawn(move || {

            loop {
                input.clear();
                print!("Enter a command: ");
                stdout.flush().expect("Failed to flush stdout");
                stdin().read_line(&mut input).expect("Failed to read input");

                let message = input.trim();
                println!("[CLIENT] Message is: '{}'", message);
                if message.is_empty() {
                    println!("[CLIENT] Empty message. Skipping writing out to server.");
                    continue;
                }

                println!("Attempting to lock stream_sender in thread: {:?}", std::thread::current().id());
                let mut stream = match sender_stream.lock() {
                    Ok(stream) => {
                        println!("Lock acquired by thread: {:?}", std::thread::current().id());
                        stream
                    }
                    Err(e) => {
                        eprintln!("Failed to lock stream in thread {:?}: {}", std::thread::current().id(), e);
                        break;
                    }
                };
                println!("stream was locked");

                if message.starts_with("SEND") {
                    println!("[CLIENT] Processing SEND message.");
                    if let Some(json_message) = process_send_message(&message["SEND".len()..].trim()) {
                        println!("[CLIENT] Sending JSON message: {}", json_message);
                        if let Err(e) = stream.write_all(format!("SEND {}\n", json_message).as_bytes()) {
                            eprintln!("[CLIENT ERROR] Failed to send message: {}", e);
                            break;
                        }
                        println!("[CLIENT] Message successfully sent to server.");

                        if let Ok(parsed_json) = serde_json::from_str::<Value>(&message) {
                            if let Some(message_content) = parsed_json["message"].as_str() {
                                println!("Message from you: {}", message_content);
                            } else {
                                println!("[CLIENT] JSON sent, but no 'message' field found.");
                            }
                        } else {
                            println!("[CLIENT] Sent raw message: {}", message);
                        }
                    } else {
                        println!("[CLIENT ERROR] Invalid JSON for SEND message.");
                    }
                } else {
                    println!("[CLIENT] Sending raw message: {}", message);
                    if let Err(e) = stream.write_all(format!("{}\n", message).as_bytes()) {
                        eprintln!("[CLIENT ERROR] Failed to send raw message: {}", e);
                        break;
                    }
                    println!("[CLIENT] Message successfully sent to server.");
                }
            }
        })
    };

    // Spawning thread for receiving messages from server
    let receiver_thread = {
        let receiver_stream = Arc::clone(&receiver_stream);
        thread::spawn(move || {
            let mut buffer = String::new();
            let mut queue: VecDeque<String> = VecDeque::new();
            let mut temp_buffer = [0; 1024];

            loop {
                let mut stream = match receiver_stream.lock() {
                    Ok(stream) => stream,
                    Err(e) => {
                        eprintln!("Failed to lock stream: {}", e);
                        break;
                    }
                };

                match stream.read(&mut temp_buffer) {
                    Ok(0) => {
                        println!("[CLIENT] Connection closed by server.");
                        break;
                    }
                    Ok(size) => {
                        let raw_message = String::from_utf8_lossy(&temp_buffer[..size]);
                        buffer.push_str(&raw_message);
                    }
                    Err(ref e) if e.kind() == io::ErrorKind::WouldBlock => {
                        thread::sleep(Duration::from_millis(100));
                        continue;
                    }
                    Err(e) => {
                        eprintln!("[CLIENT ERROR] Failed to read from server: {}", e);
                        break;
                    }
                }

                while let Some(position) = buffer.find('\n') {
                    let message = buffer.drain(..=position).collect::<String>().trim().to_string();
                    queue.push_back(message);
                }

                // Process all messages in the queue
                while let Some(message) = queue.pop_front() {
                    println!("[CLIENT] Received message: {}", message);
                    process_server_response(&message);
                }
            }
        })
    };

    if let Err(err) = sender_thread.join() {
        eprintln!("Error in sender thread: {:?}", err);
    }

    if let Err(err) = receiver_thread.join() {
        eprintln!("Error in receiver thread: {:?}", err);
    }

    println!("[CLIENT] Threads successfully shut down.");
    Ok(())
}

fn process_server_response(response_trimmed: &str) {
    if response_trimmed.starts_with("200 BOARD") {
        let json_data = response_trimmed.strip_prefix("200 BOARD").unwrap_or("").trim();
        if let Ok(parsed) = serde_json::from_str::<serde_json::Value>(json_data) {
            println!("Current userboard:");
            if let Some(user_map) = parsed.as_object() {
                for (user, status) in user_map {
                    println!("{}: {}", user, status);
                }
            }
        } else {
            println!("Unexpected format for userboard response.");
        }
    } else if response_trimmed.starts_with("200 SEND") {
        let json_part = response_trimmed.trim_start_matches("200 SEND").trim();
        match serde_json::from_str::<serde_json::Value>(json_part) {
            Ok(json_response) => {
                if let Some(sender) = json_response["sender"].as_str() {
                    if let Some(message) = json_response["message"].as_str() {
                        println!("Message from {}: {}", sender, message);
                    }
                } else {
                    println!("Unexpected JSON format: {}", json_response);
                }
            }
            Err(e) => {
                println!("Failed to parse JSON: {}", json_part);
                println!("Error: {}", e);
            }
        }
    } else {
        match response_trimmed {
            "200 OK" => {
                println!("Username was accepted");
            }
            "400 INVALID USERNAME" => {
                println!("Invalid username. Please try again.");
            }
            "200 BYE" => {
                println!("Leaving the chatroom");
                std::process::exit(0);
            }
            "200 SENT" => {
                println!("Message in queue to be sent");
            }
            "200 USERSTATUS UPDATED" => {
                println!("Accepted user status change command");
            }
            "500 SERVER ERROR" => {
                println!("Server error");
            }
            "100 TESTING" => {
                println!("Testing message received");
            }
            _ => {
                println!("Unexpected response from server: {}", response_trimmed);
            }
        }
    }
}

fn process_send_message(input: &str) -> Option<String> {
    println!("process_send_message called");
    match serde_json::from_str::<Value>(input) {
        Ok(mut json_obj) => {
            if let Some(obj) = json_obj.as_object_mut() {
                obj.insert("timestamp".to_string(), Value::String(Utc::now().to_rfc3339()));
                return Some(json_obj.to_string());
            } else {
                println!("Invalid SEND command: JSON must be an object.");
            }
        }
        Err(e) => {
            println!("Failed to parse JSON: {}", e);
        }
    }
    None
}