use std::net::UdpSocket;
use std::collections::HashMap;
use std::sync::{Arc, Mutex};

type SharedState = Arc<Mutex<HashMap<String, String>>>;

fn main() -> std::io::Result<()> {
    let socket = UdpSocket::bind("127.0.0.1:8000")?;
    println!("Server started on 127.0.0.1:8000");

    let state: SharedState = Arc::new(Mutex::new(HashMap::new()));
    let mut buffer = [0; 1024];

    loop {
        let (size, addr) = match socket.recv_from(&mut buffer) {
            Ok(result) => result,
            Err(e) => {
                println!("Error receiving data: {}", e);
                continue;
            }
        };
    
        println!("Received {} bytes from {}", size, addr);
    
        let message = String::from_utf8_lossy(&buffer[..size]).trim().to_string();
        let mut response = "500 CONNECTION ERROR\n";
    
        if is_valid_username(&message, &state) {
            {
                let mut state = state.lock().unwrap();
                state.insert(addr.to_string(), message.clone());
            }
            println!("{} has joined from {}", message, addr);
            response = "200 OK\n";
        } else {
            response = "400 INVALID USERNAME\n";
        }
    
        if let Err(e) = socket.send_to(response.as_bytes(), addr) {
            eprintln!("Error sending response to {}: {}", addr, e);
        }
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
    if state.contains_key(username) {
        return false;
    }

    true
}
