use std::collections::{HashMap, VecDeque};
use std::io::{Read, Write};
use std::net::{TcpListener, TcpStream};
use std::sync::{Arc, Condvar, Mutex};
use std::thread;
use local_ip_address::local_ip;
use serde_json::Value;

type SharedState = Arc<Mutex<HashMap<String, (String, String)>>>;
type StreamMap = Arc<Mutex<HashMap<String, TcpStream>>>;

fn main() -> std::io::Result<()> {
    let local_ip = local_ip().expect("Could not get local IP");
    let address = format!("{}:{}", local_ip, 8000);

    let state: SharedState = Arc::new(Mutex::new(HashMap::new()));
    let streams: StreamMap = Arc::new(Mutex::new(HashMap::new()));

    let listener = TcpListener::bind(address.clone())?;
    println!("Server running on {}", address);

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

fn handle_client(
    mut stream: TcpStream,
    state: SharedState,
    streams: StreamMap,
) -> std::io::Result<()> {
    let mut buffer = [0; 1024];
    let peer_addr = stream.peer_addr()?.to_string();

    {
        let mut streams = streams.lock().unwrap();
        streams.insert(peer_addr.clone(), stream.try_clone()?);
    }

    loop {
        let size = match stream.read(&mut buffer) {
            Ok(size) if size == 0 => {
                cleanup_user(&peer_addr, &state, &streams);
                println!("Client {} disconnected", peer_addr);
                return Ok(());
            }
            Ok(size) => size,
            Err(e) => {
                cleanup_user(&peer_addr, &state, &streams);
                eprintln!("Error reading from client {}: {}", peer_addr, e);
                return Err(e);
            }
        };

        let raw_message = String::from_utf8_lossy(&buffer[..size])
            .trim()
            .trim_end_matches('\n')
            .to_string();
        let (command, message) = raw_message.split_once(' ').unwrap_or((raw_message.as_str(), ""));

        println!("Command: {}, Message: {}", command, message);

        let mut response;

        match command {
            "JOIN" => {
                if is_valid_username(&message, &state) {
                    {
                        let mut state = state.lock().unwrap();
                        state.insert(peer_addr.clone(), (message.to_string(), "online".to_string()));
                    }
                    println!("{} has joined from {}", message, peer_addr);
                    response = "200 OK\n".to_string();
                } else {
                    response = "400 INVALID USERNAME\n".to_string();
                }
            }
            "LEAVE" => {
                cleanup_user(&peer_addr, &state, &streams);
                response = "200 BYE\n".to_string();
            }
            "SEND" => {
                if let Ok(parsed_message) = serde_json::from_str::<Value>(message) {
                    // Forward the JSON object to all recipients
                    let streams = streams.lock().unwrap();
                    if let Some(header) = parsed_message["header"].as_str() {
                        if header == "@all" {
                            for (_addr, stream) in streams.iter() {
                                let _ = send_to_user(stream, &parsed_message);
                            }
                            response = "200 SENT\n".to_string();
                        } else {
                            // Handle sending to specific users if required
                            let recipients: Vec<&str> = header.split_whitespace().collect();
                            for recipient in recipients {
                                let recipient_username = recipient.strip_prefix("@").unwrap_or("");
                                if let Some((_peer_addr, (_username, _status))) = state
                                    .lock()
                                    .unwrap()
                                    .iter()
                                    .find(|(_, (u, _))| u == recipient_username)
                                {
                                    if let Some(stream) = streams.get(_peer_addr) {
                                        let _ = send_to_user(stream, &parsed_message);
                                    }
                                }
                            }
                            response = "200 SENT\n".to_string();
                        }
                    } else {
                        response = "400 MESSAGE FAILED\n".to_string();
                    }
                } else {
                    response = "400 MESSAGE FAILED\n".to_string();
                }
            }
            "USERBOARD" => {
                response = user_board(&state);
            }
            "USERSTATUS" => {
                response = user_status_update(message, &state);
            }
            _ => {
                response = "500 SERVER ERROR\n".to_string();
            }
        }

        stream.write_all(response.as_bytes())?;
    }
}

fn is_valid_username(username: &str, state: &SharedState) -> bool {
    if !username.chars().all(|c| c.is_alphanumeric()) {
        return false;
    }

    if username.len() < 3 || username.len() > 30 {
        return false;
    }

    if username == "all" {
        return false;
    }

    let state = state.lock().unwrap();
    if state.values().any(|v| v.0 == username) {
        return false;
    }

    true
}

fn cleanup_user(peer_addr: &str, state: &SharedState, streams: &StreamMap) {
    {
        let mut state = state.lock().unwrap();
        state.remove(peer_addr);
    }
    {
        let mut streams = streams.lock().unwrap();
        streams.remove(peer_addr);
    }
    println!("Cleaned up user and stream for {}", peer_addr);
}

fn user_board(state: &SharedState) -> String {
    let state = state.lock().unwrap();

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

    let mut state = state.lock().unwrap();
    for (_, (user, status)) in state.iter_mut() {
        if user == username {
            *status = new_status.to_string();
            return "200 USERSTATUS UPDATED\n".to_string();
        }
    }

    "400 INVALID REQUEST\n".to_string()
}

fn send_to_user(mut stream: &TcpStream, json_message: &Value) -> std::io::Result<()> {
    // Convert the JSON object back to a string and send it
    let json_string = serde_json::to_string(json_message)?;
    stream.write_all(format!("200 SEND {}\n", json_string).as_bytes())
}
