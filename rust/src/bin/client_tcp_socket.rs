use tokio::{
    io::{self, AsyncReadExt, AsyncWriteExt},
    net::TcpStream,
};

#[tokio::main]
async fn main() -> io::Result<()> {
    let mut socket = TcpStream::connect("127.0.0.1:8000").await?;
    println!("Connected to chat server on 127.0.0.1:8000");

    let mut input = String::new();
    let mut stdin = io::stdin();
    let mut stdout = io::stdout();

    // Get username
    loop {
        print!("Enter username: ");
        stdout.flush().await?;
        input.clear();
        stdin.read_line(&mut input).await?;
        let username = input.trim();

        socket.write_all(username.as_bytes()).await?;

        let mut buffer = [0; 1024];
        let size = socket.read(&mut buffer).await?;
        let response = String::from_utf8_lossy(&buffer[..size]);

        if response == "USERNAME ACCEPTED" {
            println!("Welcome, {}!", username);
            break;
        } else {
            println!("{}", response);
        }
    }

    Ok(())
}
