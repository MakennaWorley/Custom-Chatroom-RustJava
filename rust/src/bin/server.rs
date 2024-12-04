use serde_json::Value;
use std::collections::HashMap;
use std::io::{Read, Write};
use std::net::{TcpListener, TcpStream};
use std::sync::{Arc, Mutex};
use std::thread;

type SharedState = Arc<Mutex<HashMap<String, String>>>;

fn main() -> std::io::Result<()> {
    let listener = TcpListener::bind("127.0.0.1:8000")?;
    println!("Server started on 127.0.0.1:8000");

    let state: SharedState = Arc::new(Mutex::new(HashMap::new()));

    for stream in listener.incoming() {
        match stream {
            Ok(stream) => {
                let state = state.clone();
                thread::spawn(move || {
                    if let Err(e) = handle_client(stream, state) {
                        eprintln!("Error handling client: {}", e);
                    }
                });
            }
            Err(e) => {
                eprintln!("Connection failed: {}", e);
            }
        }
    }

    Ok(())
}

fn handle_client(mut stream: TcpStream, state: SharedState) -> std::io::Result<()> {
    let mut buffer = [0; 1024];

    loop {
        let size = stream.read(&mut buffer)?;

        if size == 0 {
            println!("Client disconnected");
            return Ok(());
        }

        let raw_message = String::from_utf8_lossy(&buffer[..size]).trim().to_string();
        let (command, message) = raw_message.split_once(' ').unwrap_or((raw_message.as_str(), ""));

        println!("Command: {}, Message: {}", command, message);

        let mut response = "500 SERVER ERROR\n".to_string();

        match command {
            "JOIN" => {
                if is_valid_username(&message, &state) {
                    {
                        let mut state = state.lock().unwrap();
                        state.insert(stream.peer_addr()?.to_string(), message.to_string());
                    }
                    println!("{} has joined from {}", message, stream.peer_addr()?);
                    response = "200 OK\n".to_string();
                } else {
                    response = "400 INVALID USERNAME\n".to_string();
                }
            }
            "LEAVE" => {
                if let Some(parsed_message) = parse_message(message) {
                    println!("Processing LEAVE with message: {:?}", parsed_message);
                    response = format!("200 OK\n");
                }
            }
            "SEND" => {
                if let Some(parsed_message) = parse_message(message) {
                    println!("Processing SEND with message: {:?}", parsed_message);
                    response = format!("200 OK\n");
                }
            }
            _ => {
                response = "500 SERVER ERROR\n".to_string();
            }
        }

        stream.write_all(response.as_bytes())?;
    }
}

fn parse_message(message: &str) -> Option<Value> {
    if let Ok(json_value) = serde_json::from_str::<Value>(message) {
        Some(json_value)
    } else {
        // Treat as plain text wrapped as a JSON string
        Some(Value::String(message.to_string()))
    }
}

fn is_valid_username(username: &str, state: &SharedState) -> bool {
    if !username.chars().all(|c| c.is_alphanumeric()) {
        return false;
    }

    if username.len() < 3 || username.len() > 30 {
        return false;
    }

    if username == "everyone" {
        return false;
    }

    let state = state.lock().unwrap();
    if state.values().any(|v| v == username) {
        return false;
    }

    true
}
