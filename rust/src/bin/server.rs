use dashmap::DashMap;
use local_ip_address::local_ip;
use serde_json::Value;
use std::collections::HashMap;
use std::io::{Read, Write};
use std::net::{TcpListener, TcpStream};
use std::sync::{Arc, RwLock};
use std::thread;

type SharedState = Arc<RwLock<HashMap<String, (String, String)>>>;
type StreamMap = Arc<DashMap<String, TcpStream>>;

fn main() -> std::io::Result<()> {
    let local_ip = local_ip().expect("Could not get local IP");
    let address = format!("{}:{}", local_ip, 8000);

    let state: SharedState = Arc::new(RwLock::new(HashMap::new()));
    let streams: StreamMap = Arc::new(DashMap::new());

    let listener = TcpListener::bind(address.clone())?;
    println!("[SERVER] Server running on {}", address);

    for stream in listener.incoming() {
        let stream = stream?;
        let state_clone = Arc::clone(&state);
        let streams_clone = Arc::clone(&streams);

        thread::spawn(move || {
            let _ = handle_client(stream, state_clone, streams_clone);
        });
    }

    Ok(())
}

fn handle_client(mut stream: TcpStream, state: SharedState, streams: StreamMap) -> std::io::Result<()> {
    let mut buffer = [0; 1024];
    let peer_addr = stream.peer_addr()?.to_string();

    println!("[SERVER] New connection from {}", peer_addr);
    //let testing = "100 TESTING\n";
    //stream.write_all(testing.as_bytes())?;

    streams.insert(peer_addr.clone(), stream.try_clone()?);

    loop {
        let size = match stream.read(&mut buffer) {
            Ok(size) if size == 0 => {
                println!("[SERVER] Client {} disconnected", peer_addr);
                cleanup_user(&peer_addr, &state, &streams);
                return Ok(());
            }
            Ok(size) => size,
            Err(e) => {
                eprintln!("[SERVER ERROR] Error reading from client {}: {}", peer_addr, e);
                cleanup_user(&peer_addr, &state, &streams);
                return Err(e);
            }
        };

        let raw_message = String::from_utf8_lossy(&buffer[..size])
            .trim()
            .to_string();
        println!("[SERVER] Received message from {}: {}", peer_addr, raw_message);

        let (command, message) = raw_message.split_once(' ').unwrap_or((raw_message.as_str(), ""));
        println!("[SERVER] Parsed command: {}, message: {}", command, message);

        let mut response= "500 SERVER ERROR\n".to_string();

        match command {
            "JOIN" => {
                if is_valid_username(&message, &state) {
                    state.write().unwrap().insert(peer_addr.clone(), (message.to_string(), "ONLINE".to_string()));
                    println!("[SERVER] {} joined from {}", message, peer_addr);
                    response = "200 OK\n".to_string();
                } else {
                    println!("[SERVER] Invalid username from {}: {}", peer_addr, message);
                    response = "400 INVALID USERNAME\n".to_string();
                }
            }
            "LEAVE" => {
                cleanup_user(&peer_addr, &state, &streams);
                response = "200 BYE\n".to_string();
            }
            "SEND" => {
                if let Ok(parsed_message) = serde_json::from_str::<Value>(message) {
                    // Check if the content field exists and is valid
                    if let Some(content) = parsed_message["message"].as_str() {
                        let trimmed_content = content.trim();
                        if !(1..=500).contains(&trimmed_content.len()) {
                            eprintln!("[SERVER ERROR] Message content length invalid: {}", trimmed_content.len());
                            response = "400 MESSAGE FAILED\n".to_string();
                        } else if parsed_message["header"].as_str() == Some("@all") {
                            broadcast_message(&streams, &parsed_message, Some(&peer_addr))?;
                            response = "200 SENT\n".to_string();
                        } else if let Some(header) = parsed_message["header"].as_str() {
                            let mut all_sent = false;
                            let recipients: Vec<&str> = header
                                .split_whitespace()
                                .filter(|word| word.starts_with('@'))
                                .map(|user| user.trim_start_matches('@'))
                                .collect();

                            if !recipients.is_empty() {
                                let state = state.read().unwrap();
                                for recipient in recipients {
                                    println!("[SERVER] Finding {}", recipient);

                                    if let Some((ip, _)) = state.iter().find(|(_, (name, _))| name == recipient) {
                                        if let Some(user_stream) = streams.get(ip) {
                                            if let Err(e) = send_to_user(&user_stream, &parsed_message) {
                                                eprintln!("[SERVER ERROR] Failed to send message to {}: {}", recipient, e);
                                                all_sent = false;
                                            } else {
                                                println!("[SERVER] Message sent to {}", recipient);
                                                all_sent = true;
                                            }
                                        } else {
                                            eprintln!("[SERVER] No active stream for recipient {}", recipient);
                                            all_sent = false;
                                        }
                                    } else {
                                        eprintln!("[SERVER ERROR] Recipient {} not found in state", recipient);
                                        all_sent = false;
                                    }
                                }
                                if all_sent {
                                    response = "200 SENT\n".to_string();
                                } else {
                                    response = "400 MESSAGE FAILED\n".to_string();
                                }
                            } else {
                                response = "400 MESSAGE FAILED\n".to_string();
                            }
                        } else {
                            response = "400 MESSAGE FAILED\n".to_string();
                        }
                    } else {
                        eprintln!("[SERVER ERROR] Missing or invalid content field in message from {}", peer_addr);
                        response = "400 INVALID MESSAGE FORMAT\n".to_string();
                    }
                } else {
                    eprintln!("[SERVER ERROR] Invalid JSON message from {}: {}", peer_addr, message);
                    response = "400 INVALID MESSAGE FORMAT\n".to_string();
                }
            }
            "USERBOARD" => {
                println!("[SERVER] User is requesting the userboard");
                response = user_board(&state);
            }
            "USERSTATUS" => {
                println!("[SERVER] User is requesting to change their status");
                response = user_status_update(message, &state);
            }
            _ => {
                eprintln!("[SERVER ERROR] Unknown command from {}: {}", peer_addr, command);
                response = "500 SERVER ERROR\n".to_string();
            }
        }

        stream.write_all(response.as_bytes())?;
    }
}

fn is_valid_username(username: &str, state: &SharedState) -> bool {
    if !(username.chars().all(|c| c.is_alphanumeric())) {
        return false;
    }

    if username.len() < 3 && username.len() > 30 {
        return false;
    }

    if username == "all" {
        return false;
    }

    let state = state.read().unwrap();
    if state.values().any(|v| v.0 == username) {
        return false;
    }

    true
}

fn cleanup_user(peer_addr: &str, state: &SharedState, streams: &StreamMap) {
    state.write().unwrap().remove(peer_addr);
    streams.remove(peer_addr);
    println!("[SERVER] Cleaned up user and stream for {}", peer_addr);
}

fn user_board(state: &SharedState) -> String {
    let state = state.read().unwrap();

    let userboard: HashMap<String, String> = state
        .values()
        .map(|(username, status)| (username.clone(), status.clone()))
        .collect();

    match serde_json::to_string(&userboard) {
        Ok(json) => format!("200 BOARD {}\n", json),
        Err(_) => "500 SERVER ERROR\n".to_string(),
    }
}

fn user_status_update(message: &str, state: &SharedState) -> String {
    let parts: Vec<&str> = message.split_whitespace().collect();
    if parts.len() != 2 {
        return "400 INVALID REQUEST\n".to_string();
    }

    let username = parts[0];
    let new_status = parts[1];

    let valid_statuses = vec!["ONLINE", "OFFLINE", "DO_NOT_DISTURB"];
    if !valid_statuses.contains(&new_status) {
        return "400 INVALID REQUEST\n".to_string();
    }

    let mut state = state.write().unwrap();
    for (_, (user, status)) in state.iter_mut() {
        if user == username {
            *status = new_status.to_string();
            return "200 USERSTATUS UPDATED\n".to_string();
        }
    }

    "400 INVALID REQUEST\n".to_string()
}

fn broadcast_message(streams: &StreamMap, message: &Value, exclude_addr: Option<&str>) -> std::io::Result<()> {
    let message_string = serde_json::to_string(message)?;
    println!("[SERVER] Broadcasting {}", message_string);
    for entry in streams.iter() {
        let (addr, mut stream) = entry.pair();
        if Some(addr.as_str()) == exclude_addr {
            continue;
        }
        if let Err(e) = stream.write_all(format!("{}\n", message_string).as_bytes()) {
            eprintln!("[SERVER ERROR] Failed to send message to {}: {}", addr, e);
        }
    }
    Ok(())
}

fn send_to_user(mut stream: &TcpStream, json_message: &Value) -> std::io::Result<()> {
    let json_string = serde_json::to_string(json_message)?;
    println!("[SERVER] Sending private message {}", json_string);
    stream.write_all(format!("{}\n", json_string).as_bytes())
}
