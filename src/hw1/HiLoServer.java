package hw1;

import java.net.*;
import java.io.*;


public class HiLoServer {
	public static final int DEFAULT_PORT = 60001;
    public static final int BUF_LENGTH = 1024;
	public static final String CORRECT = "Correct!";

	private int secretNumber;
	private int guesses;
	private String player;

    private DatagramSocket sock;
    private DatagramPacket packet;
    private DatagramPacket replyPacket;
    private byte[] recBuf;
    private byte[] repBuf;
    private String message;
    
    public HiLoServer(){
		this.secretNumber = (int)(Math.random()*100);
		this.guesses = 0;
    	try {
    		this.sock = new DatagramSocket(DEFAULT_PORT);
	    	System.out.println("Listening on port "+DEFAULT_PORT+" for connections...");
	    	this.recBuf = new byte[BUF_LENGTH];
    	} catch (SocketException e){
    		e.printStackTrace();
    		System.exit(0);
    	}
    }

	public void runServer(){
		try {
			this.packet = new DatagramPacket(this.recBuf, this.recBuf.length);
			this.sock.receive(this.packet);
			this.player = new String(this.recBuf);
			System.out.println("Player connected - " + this.player); // Debug
			String message = "Welcome " + this.player + ", let's play...";
			this.repBuf = message.getBytes();
			this.replyPacket = new DatagramPacket(repBuf, repBuf.length, this.packet.getAddress(), this.packet.getPort());
			this.sock.send(this.replyPacket);
			playGame();
			System.out.println("GAME OVER!");
		} catch (IOException e){
			e.printStackTrace();
		}
	}

    public void playGame() throws IOException{
		System.out.println("Secret number is " + this.secretNumber);
		while(true) { // Listen for guesses
			this.packet = new DatagramPacket(this.recBuf, this.recBuf.length);
			this.sock.receive(this.packet);
			ByteArrayInputStream bais = new ByteArrayInputStream(this.recBuf);
			DataInputStream dais = new DataInputStream(bais);
			int guess = dais.readInt();
			dais.close();
			System.out.println(this.player + " guessed " + guess);
			String reply = processGuess(guess);
			this.repBuf = reply.getBytes();
			this.replyPacket = new DatagramPacket(repBuf, repBuf.length, this.packet.getAddress(), this.packet.getPort());
			this.sock.send(this.replyPacket);
			if (guess == this.secretNumber) return;
		}
    }

    public String processGuess(int guess){
		this.guesses++;
    	if (guess > this.secretNumber){
			return "Guess too high!";
		} else if (guess < this.secretNumber){
			return "Guess too low!";
		} else {
			return CORRECT + " - took you " + this.guesses + " guesses.";
		}

    }

	public static void main(String[] args) {
		new HiLoServer().runServer();
	}

}