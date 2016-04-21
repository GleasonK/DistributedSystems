package hw1;

import java.io.*;
import java.net.*;


public class DGClient {
	private DatagramSocket sock;
    private String message;
    private DatagramPacket packet;
    private InetAddress address;
    private byte[] rbuff;
    private int serverPort;

	public  DGClient(String addr, int port){
		try {
			this.serverPort = port;
			this.sock = new DatagramSocket();
			this.address = InetAddress.getByName(addr);
			this.rbuff = new byte[HiLoServer.BUF_LENGTH]; // TODO: DGServer.BUF_LENGTH
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0); // Quit on error
		}
	}

	public boolean sendName(String name){
		try {
			byte[] nameBytes = name.getBytes();
			this.packet = new DatagramPacket(nameBytes, nameBytes.length, this.address, this.serverPort);
			this.sock.send(this.packet);
			return true;
		} catch (IOException e){
			e.printStackTrace();
			return false;
		}
	}

	public boolean sendGuess(int guess){
		DataOutputStream dout;
		try {
			ByteArrayOutputStream baos=new ByteArrayOutputStream();
        	DataOutputStream daos=new DataOutputStream(baos);
			daos.writeInt(guess);
			daos.close();
			byte[] guessBytes = baos.toByteArray();
			this.packet = new DatagramPacket(guessBytes, guessBytes.length, this.address, this.serverPort);
			this.sock.send(this.packet);
			return true;
		} catch (IOException e){
			e.printStackTrace();
			return false;
		}
	}

	public boolean receiveResult(){
		try {
			this.packet = new DatagramPacket(this.rbuff, rbuff.length);
			this.sock.receive(this.packet);
			return true;
		} catch (IOException e){
			e.printStackTrace();
			return false;
		}
	}

	public DatagramPacket getPacket(){
		return this.packet;
	}

	public void close(){
		this.sock.close();
	}

}