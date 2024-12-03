use std::net::TcpStream;
use std::io::{self, Write, Read};

fn main() -> io::Result<()> {
    let mut stream = TcpStream::connect("127.0.0.1:8000")?;
    println!("Requesting access to chat room. Please select a username that has between 3 and 30 characters and only alphanumeric symbols.");

    let mut input = String::new();
    let mut stdout = io::stdout();

    loop {
        print!("Enter username: ");
        stdout.flush()?;
        input.clear();
        io::stdin().read_line(&mut input)?;
        let username = input.trim();

        if !username.is_empty() {
            stream.write_all(username.as_bytes())?;

            let mut buffer = [0; 1024];
            let size = stream.read(&mut buffer)?;
            let response = String::from_utf8_lossy(&buffer[..size]);
            let response_trimmed = response.trim();

            match response_trimmed {
                "200 OK" => {
                    println!("Username accepted. You have joined the chatroom!");
                    break;
                }
                "400 INVALID USERNAME" => {
                    println!("Invalid username. Please try again.");
                }
                _ => {
                    println!("Unexpected response from server: {}", response_trimmed);
                }
            }
        }
    }

    println!("You are now connected.");

    loop {}

}
