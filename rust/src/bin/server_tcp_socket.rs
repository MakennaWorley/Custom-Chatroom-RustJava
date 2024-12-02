use tokio::{
    io::{AsyncReadExt, AsyncWriteExt},
    net::{TcpListener, TcpStream},
    sync::Mutex,
};
use serde_json::Value;
use std::collections::HashMap;
use std::sync::Arc;

type SharedState = Arc<Mutex<HashMap<String, TcpStream>>>;

#[tokio::main]
async fn main() -> tokio::io::Result<()> {
    let listener = TcpListener::bind("127.0.0.1:8000").await?;
    let state: SharedState = Arc::new(Mutex::new(HashMap::new()));

    println!("Server started on 127.0.0.1:8000");

    loop {
        let (socket, _) = listener.accept().await?;
        let state = Arc::clone(&state);

        tokio::spawn(async move {
            if let Err(e) = send_error_response(socket, b"500 CONNECTION ERROR\n").await {
                eprintln!("Failed to send error response to client socket: {}", e);
            }
        });
    }
}

async fn send_error_response(mut socket: TcpStream, message: &[u8]) -> tokio::io::Result<()> {
    socket.write_all(message).await
}

async fn handle_client(mut socket: TcpStream, state: SharedState) -> tokio::io::Result<()> {
    let mut buffer = [0; 1024];
    let size = socket.read(&mut buffer).await?;
    let username = String::from_utf8_lossy(&buffer[..size]).trim().to_string();

    if !is_valid_username(&username, &state).await {
        socket.write_all(b"400 INVALID USERNAME\n").await?;
        return Err(tokio::io::Error::new(tokio::io::ErrorKind::InvalidInput, "Invalid username"));
    }

    let mut state = state.lock().await;
    state.insert(username.clone(), socket.try_clone()?);
    socket.write_all(b"200 OK\n").await?;
    println!("{} has joined", username);

    Ok(())
}

async fn is_valid_username(username: &str, state: &SharedState) -> bool {
    if !username.chars().all(|c| c.is_alphanumeric()) {
        return false;
    }
    
    if username.len() < 3 || username.len() > 30 {
        return false;
    }
    
    if username == "everyone" {
        return false;
    }

    let state = state.lock().await;
    if state.contains_key(username) {
        return false;
    }

    return true;
}