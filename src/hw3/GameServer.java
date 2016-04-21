package hw3;


import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by GleasonK on 2/26/16.
 */
public class GameServer {
    public static final String SERVER_IP = "localhost";
    private static final String MULTICAST_BASE = "231.0.8."; // Add a number after
    private static final String KIBITZ_BASE = "231.0.9."; // Add a number after
    public static final int DEFAULT_PORT = 60100;
    public static final int BUF_LENGTH = 2048;

    private Set<RoomInfo> games;
    private RoomInfo room;
    private DatagramSocket sock;
    private DatagramPacket packet, replyPacket;
    private byte[] recBuf, repBuf;

    public GameServer() {
        this.games = new HashSet<RoomInfo>();
        this.room = new RoomInfo(getMulticastAddress(), getMulticastPort(), getKibitzAddress(), getMulticastPort());
        this.games.add(this.room);
        try {
            this.sock = new DatagramSocket(DEFAULT_PORT);
            this.recBuf = new byte[BUF_LENGTH];
            System.out.println("Listening on port " + DEFAULT_PORT + " for connections...");
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public void runServer() throws IOException, ClassNotFoundException {
        while (true) {
            System.out.println("Waiting for data...");
            this.packet = new DatagramPacket(this.recBuf, this.recBuf.length);
            this.sock.receive(this.packet);
            Thread t = new Thread(new ServerThread(this.packet));
            t.start();
        }
    }

    private String getMulticastAddress() {
        return MULTICAST_BASE + games.size();
    }

    private String getKibitzAddress() {
        return KIBITZ_BASE + games.size();
    }

    private int getMulticastPort() {
        return 8000 + (int)(Math.random() * 2000);
//        return (int)(Math.random() * 1000);
    }


    class ServerThread implements Runnable {
        private ClientInfo client;
        private DatagramSocket sock;
        private DatagramPacket packet, replyPacket;
        private byte[] recBuf, repBuf;
        private DatagramPacket jPacket;

        public ServerThread(DatagramPacket packet) {
            this.jPacket = packet;
            try {
                this.sock = new DatagramSocket();
                this.recBuf = new byte[BUF_LENGTH];
                System.out.println("Listening on port " + DEFAULT_PORT + " for connections...");
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                ByteArrayInputStream bais = new ByteArrayInputStream(this.jPacket.getData());
                ObjectInputStream ois = new ObjectInputStream(bais);
                PlayerData data = (PlayerData) ois.readObject();
                ois.close();
                bais.close();

                ByteArrayOutputStream baos = new ByteArrayOutputStream(BUF_LENGTH);
                ObjectOutputStream oos = new ObjectOutputStream(baos);

//                switch (data.getType()) {
//                    case PlayerData.PLAYER_TYPE:
                if (data.getType().equals(PlayerData.PLAYER_TYPE)) {
                    System.out.println("Player Join!");
                    if (room.player1 == null) {
                        room.setPlayer1(data);
                    } else if (room.player2 == null) {
                        room.setPlayer2(data);
                    } else {
                        room = new RoomInfo(getMulticastAddress(), getMulticastPort(), getKibitzAddress(), getMulticastPort());
                        room.setPlayer1(data);
                        games.add(room);
                    }
                } // Else Kibitzer, just put in first open room
                oos.writeObject(room); // If player 2 null, you are player 1.
                this.repBuf = baos.toByteArray();
                System.out.println(this.jPacket.getAddress() + " " + this.jPacket.getPort());
                this.replyPacket = new DatagramPacket(repBuf, repBuf.length, this.jPacket.getAddress(), this.jPacket.getPort());
                this.sock.send(this.replyPacket);
                oos.close();
                baos.close();
            } catch (IOException e){
                e.printStackTrace();
            } catch(ClassNotFoundException e){
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        try {
            new GameServer().runServer();
        } catch (IOException e){
            e.printStackTrace();
        } catch(ClassNotFoundException e){
            e.printStackTrace();
        }
    }
}
