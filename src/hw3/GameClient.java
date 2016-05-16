package hw3;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Scanner;

/**
 * Created by GleasonK on 2/27/16.
 */
public class GameClient {
    private DatagramSocket sock;
    private DatagramPacket packet;
    private MulticastSocket mcSocket;
    private InetAddress group, kibgroup;
    private int mcPort;
    private InetAddress address;
    private byte[] repBuff, recBuff;
    private int serverPort;
    private Game game;

    private Scanner scanner;

    public GameClient(String addr, int port){
        this.scanner = new Scanner(System.in);
        try {
            this.serverPort = port;
            this.sock = new DatagramSocket();
            this.address = InetAddress.getByName(addr);
            this.repBuff = new byte[GameServer.BUF_LENGTH];
            this.recBuff = new byte[GameServer.BUF_LENGTH];
        } catch (IOException e){
            e.printStackTrace();
            System.exit(0);
        }
    }

    public void joinAsPlayer() {
         try {
             System.out.println("Joining as player");
             PlayerData pd = new PlayerData(PlayerData.PLAYER_TYPE);

             ByteArrayOutputStream baos = new ByteArrayOutputStream(GameServer.BUF_LENGTH);
             ObjectOutputStream oos = new ObjectOutputStream(baos);
             oos.writeObject(pd);
             this.repBuff = baos.toByteArray();
             this.packet = new DatagramPacket(this.repBuff, this.repBuff.length, this.address, this.serverPort);
             this.sock.send(this.packet);
             oos.close(); baos.close();

             this.packet = new DatagramPacket(this.recBuff, this.recBuff.length); // Packet came from server thread
             this.sock.receive(this.packet);
             playGame(this.packet);
         } catch (IOException e){
             e.printStackTrace();
         } catch(ClassNotFoundException e){
             e.printStackTrace();
         }
    }

    public void playGame(DatagramPacket packet) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bais = new ByteArrayInputStream(this.packet.getData());
        ObjectInputStream ois = new ObjectInputStream(bais);
        RoomInfo roomInfo = (RoomInfo) ois.readObject();
        ois.close();
        bais.close();
        System.out.println(roomInfo);

        this.mcPort = roomInfo.mcPort;
        this.mcSocket = new MulticastSocket(this.mcPort);
        this.group = InetAddress.getByName(roomInfo.mcip);
        mcSocket.joinGroup(group);
        mcSocket.setTimeToLive(64);
        boolean firstPlayer = (roomInfo.player2 == null);
        game = new Game();
        if (firstPlayer){
            System.out.println("Waiting for Player 2...");
            receiveGameObject();
            System.out.println("Player 2 Join.");
            System.out.print("Enter a word: ");
            String word = getLine();
            game.setWord(word);
            sendGameObject(game); // Now player 2 has the game
        } else {
            sendGameObject(game);
            receiveGameObject();
            System.out.println("Joined Game");
            System.out.println("Player 1 entering word... ");
            game = receiveGameObject();
        }

        while (!game.isGameOver()){ // In the game now
            if (firstPlayer){
                game = receiveGameObject();
                System.out.println(game.toString());
            } else {
                System.out.print("Enter a guess: ");
                String guess = getLine();
                game.guessLetter(guess.charAt(0));
                sendGameObject(game);
                System.out.println(game.toString());
            }
        }
        mcSocket.leaveGroup(group);
        scanner.close();
    }

    private String getLine(){
        if (scanner.hasNextLine())
            return scanner.nextLine();
        return "";
    }

    public void sendGameObject(Game game) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(GameServer.BUF_LENGTH);
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(game);
        byte[] buff = baos.toByteArray();
        oos.close();
        baos.close();
        this.packet = new DatagramPacket(buff, buff.length, group, mcPort);
        this.mcSocket.send(packet);
    }

    public Game receiveGameObject() throws IOException, ClassNotFoundException{
        byte[] buff = new byte[GameServer.BUF_LENGTH];
        DatagramPacket rPacket = new DatagramPacket(buff, buff.length);
        mcSocket.receive(rPacket);
        ByteArrayInputStream bais = new ByteArrayInputStream(rPacket.getData());
        ObjectInputStream ois = new ObjectInputStream(bais);
        Game game = (Game) ois.readObject();
        ois.close(); bais.close();
        return game;
    }

    public Object receiveObject() throws IOException, ClassNotFoundException{
        byte[] buff = new byte[GameServer.BUF_LENGTH];
        DatagramPacket rPacket = new DatagramPacket(buff, buff.length);
        mcSocket.receive(rPacket);
        ByteArrayInputStream bais = new ByteArrayInputStream(rPacket.getData());
        ObjectInputStream ois = new ObjectInputStream(bais);
        Object o = ois.readObject();
        ois.close(); bais.close();
        return o;
    }

    public void sendString(String str) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(GameServer.BUF_LENGTH);
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(str);
        byte[] buff = baos.toByteArray();
        oos.close();
        baos.close();
        this.packet = new DatagramPacket(buff, buff.length, kibgroup, mcPort);
        this.mcSocket.send(packet);
    }

    public void joinAsKibitzer(){
        try {
            System.out.println("Joining as kibitzer");
            PlayerData pd = new PlayerData(PlayerData.KIBITZ_TYPE);

            ByteArrayOutputStream baos = new ByteArrayOutputStream(GameServer.BUF_LENGTH);
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(pd);
            this.repBuff = baos.toByteArray();
            this.packet = new DatagramPacket(this.repBuff, this.repBuff.length, this.address, this.serverPort);
            this.sock.send(this.packet);
            oos.close(); baos.close();

            this.packet = new DatagramPacket(this.recBuff, this.recBuff.length); // Packet came from server thread
            this.sock.receive(this.packet);
            kibitz(this.packet);
        } catch (IOException e){
            e.printStackTrace();
        } catch(ClassNotFoundException e){
            e.printStackTrace();
        }
    }

    public void kibitz(DatagramPacket packet) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bais = new ByteArrayInputStream(this.packet.getData());
        ObjectInputStream ois = new ObjectInputStream(bais);
        RoomInfo roomInfo = (RoomInfo) ois.readObject();
        ois.close();
        bais.close();

        this.mcPort = roomInfo.mcPort;
        this.mcSocket = new MulticastSocket(this.mcPort);
        this.group = InetAddress.getByName(roomInfo.mcip);
        this.kibgroup = InetAddress.getByName(roomInfo.kibip);
        mcSocket.joinGroup(group);
        mcSocket.joinGroup(kibgroup);
        mcSocket.setTimeToLive(64);

        System.out.println("Type and hit enter to kibitz");
        Thread read = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (true) {
                        Object incomingMsg = receiveObject();
                        if (incomingMsg instanceof String)
                            System.out.println(incomingMsg.toString());
                        else if (incomingMsg instanceof Game) {
                            game = (Game) incomingMsg;
                            System.out.println(game.getMessage());
                            if (game.isGameOver()) System.exit(0);
                        }
                    }
                } catch (IOException e){
                    e.printStackTrace();
                } catch (ClassNotFoundException e){
                    e.printStackTrace();
                }

            }
        });
        Thread send = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (true) {
                        if (game != null && game.isGameOver()) {
                            scanner.close();
                            return;
                        }
                        String line = getLine();
                        sendString(line);
                    }
                } catch (IOException e){
                    e.printStackTrace();
                }
            }
        });
        read.start();
        send.start();
    }

    public synchronized void sendGameData(Game game){
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(GameServer.BUF_LENGTH);
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(game);
            this.repBuff = baos.toByteArray();
            this.packet = new DatagramPacket(this.repBuff, this.repBuff.length, group, mcPort);
            this.mcSocket.send(this.packet);
            oos.close(); baos.close();
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        GameClient gc = new GameClient("localhost",GameServer.DEFAULT_PORT);
        gc.joinAsPlayer();
//        gc.joinAsKibitzer();
    }
}
