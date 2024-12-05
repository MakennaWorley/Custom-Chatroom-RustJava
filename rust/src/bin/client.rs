use std::net::TcpStream;
use std::io::{self, Write, Read};

fn main() -> io::Result<()> {
    let mut stream = TcpStream::connect("146.86.114.51:8000")?;
    println!("Requesting access to chat room. Please select a username that has between 3 and 30 characters and only alphanumeric symbols.");

    let mut input = String::new();
    let mut stdout = io::stdout();

    loop {
        print!("Enter a command: ");
        stdout.flush()?;
        input.clear();
        io::stdin().read_line(&mut input)?;
        let message = input.trim();

        if !message.is_empty() {
            stream.write_all(message.as_bytes())?;

            let mut buffer = [0; 1024];
            let size = stream.read(&mut buffer)?;
            let response = String::from_utf8_lossy(&buffer[..size]);
            let response_trimmed = response.trim();

            match response_trimmed {
                "200 OK" => {
                    println!("Username was accepted");
                }
                "400 INVALID USERNAME" => {
                    println!("Invalid username. Please try again.");
                }

                "200 BYE" => {
                    println!("Leaving the chatroom");
                    return Ok(())
                }

                "200 SEND" => {
                    println!("Accepted send command");
                }

                "500 SERVER ERROR\n" => {
                    println!("Server error");
                }
                _ => {
                    println!("Unexpected response from server: {}", response_trimmed);
                }
            }
        }
    }

}
