package hw1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;

public class ClientApp {
    private DGClient client;
    private DatagramPacket rpacket;
    private String response;

    public ClientApp(){
        this.client = new DGClient("localhost", HiLoServer.DEFAULT_PORT);
    }

    public void playGame(){
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        System.out.print("Enter Name: ");
        try {
            String name = reader.readLine();
            if (!this.client.sendName(name)) System.err.println("Error sending packet");
            if (!this.client.receiveResult()) System.err.println("Error receiving packet.");
            this.rpacket  = this.client.getPacket();
            this.response = new String(this.rpacket.getData(), 0, this.rpacket.getLength());
            System.out.println("Server: " + this.response);
            while (true) {
                System.out.print("Enter Guess: ");
                String line = reader.readLine();
                if (line.equals("done")) break;
                try {
                    int guess = Integer.parseInt(line);
                    if (!this.client.sendGuess(guess)) System.err.println("Error sending packet");
                    if (!this.client.receiveResult()) System.err.println("Error receiving packet.");
                    this.rpacket  = this.client.getPacket();
                    this.response = new String(this.rpacket.getData(), 0, this.rpacket.getLength());
                    System.out.println("Server: " + this.response);
                    if (this.response.startsWith(HiLoServer.CORRECT)) return;
                } catch (NumberFormatException e){
                    System.out.println("Enter a valid number...");
                    continue;
                }
            }
        } catch(IOException e){
            System.err.println(e.toString());
        } finally {
            this.client.close();
        }

    }

    public static void main(String[] args) {
        new ClientApp().playGame();
    }
}