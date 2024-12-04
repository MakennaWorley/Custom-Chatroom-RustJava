use std::net::TcpStream;
use std::io::{self, Write, Read};

fn main() -> io::Result<()> {
    let mut stream = TcpStream::connect("127.0.0.1:8000")?;
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
                    println!("Command was accepted, you should see something posted on the server side or get a message from the server");
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
