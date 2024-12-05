use std::net::TcpStream;
use local_ip_address::local_ip;
use std::io::{self, Write, Read};
use serde_json::Value;

fn main() -> io::Result<()> {
    let local_ip = local_ip().expect("Could not get local IP");
    let address = format!("{}:{}", local_ip, 8000);

    let mut stream = TcpStream::connect(&address).expect("Could not bind to address");
    println!("Requesting access to chat room.");

    let mut input = String::new();
    let mut stdout = io::stdout();

    loop {
        print!("Enter a command: ");
        stdout.flush()?;
        input.clear();
        io::stdin().read_line(&mut input)?;
        let message = input.trim();

        if !message.is_empty() {
            stream.write_all(format!("{}\n", message).as_bytes())?;

            let mut buffer = [0; 1024];
            let size = stream.read(&mut buffer)?;
            let response = String::from_utf8_lossy(&buffer[..size]);
            let response_trimmed = response.trim();

            if response_trimmed.starts_with("200 BOARD") {
                let json_data = response_trimmed.strip_prefix("200 BOARD").unwrap_or("").trim();
                if let Ok(parsed) = serde_json::from_str::<Value>(json_data) {
                    println!("Current userboard:");
                    if let Some(user_map) = parsed.as_object() {
                        for (user, status) in user_map {
                            println!("{}: {}", user, status);
                        }
                    }
                } else {
                    println!("Unexpected format for userboard response.");
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
                        return Ok(());
                    }
                    "200 SEND" => {
                        println!("Accepted send command");
                    }
                    "200 USERSTATUS UPDATED" => {
                        println!("Accepted user status change command");
                    }
                    "500 SERVER ERROR" => {
                        println!("Server error");
                    }
                    _ => {
                        println!("Unexpected response from server: {}", response_trimmed);
                    }
                }
            }
        }
    }

}
