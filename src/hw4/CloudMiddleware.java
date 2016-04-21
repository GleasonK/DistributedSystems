package hw4;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by GleasonK on 3/21/16.
 */
public class CloudMiddleware {
    public static final String SERVER_IP = "localhost";
    public static final int DEFAULT_PORT = 60010;
    public static final int BUF_LENGTH = 2048;

    private List<ServerInfo> servers;
    private DatagramSocket socket;
    private DatagramPacket packet;
    private byte[] recBuf;

    public CloudMiddleware() {
        this.servers = new ArrayList<>();
        try {
            this.socket = new DatagramSocket(DEFAULT_PORT);
            this.recBuf = new byte[BUF_LENGTH];
            System.out.println("Listening on port " + DEFAULT_PORT + " for connections...");
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public void runMiddleware() throws IOException, ClassNotFoundException {
        while(true){
            System.out.println("Waiting for data...");
            this.packet = new DatagramPacket(this.recBuf, this.recBuf.length);
            this.socket.receive(this.packet);
            Thread t = new Thread(new MiddlewareThread(this.packet));
            t.start();
        }
    }

    class MiddlewareThread implements Runnable {
        private DatagramPacket iPacket, rPacket;
        private byte[] buff;

        public MiddlewareThread(DatagramPacket data){
            this.iPacket = data;
        }

        @Override
        public void run() {
            try {
                ByteArrayInputStream bais = new ByteArrayInputStream(this.iPacket.getData());
                ObjectInputStream ois = new ObjectInputStream(bais);
                CloudData data = (CloudData) ois.readObject();
                ois.close();
                bais.close();

                ByteArrayOutputStream baos = new ByteArrayOutputStream(BUF_LENGTH);
                ObjectOutputStream oos = new ObjectOutputStream(baos);

                // Get the Server Hash
                int hashKey = calculateHash(data.filename);
                System.out.println("Hash=>"+hashKey);
                ServerInfo server = servers.get(hashKey % servers.size());

                oos.writeObject(server);
                this.buff = baos.toByteArray();

                this.rPacket = new DatagramPacket(buff, buff.length, this.iPacket.getAddress(), this.iPacket.getPort());
                socket.send(this.rPacket);

                oos.close();
                baos.close();
            } catch (IOException | ClassNotFoundException e){
                e.printStackTrace();
            }
        }
    }

    public int calculateHash(String filename){
        char[] chars = filename.toCharArray();
        int sum=0;
        for (int i = 0; i < chars.length; i++) {
            int val = 1 + chars[i] - 'a';
            sum+= val;
        }
        return sum;
    }

    public void addServer(String addr, int port){
        this.servers.add(new ServerInfo(addr,port));
    }

    public void addServer(ServerInfo serverInfo){
        this.servers.add(serverInfo);
    }

    public static void main(String[] args) {
        try {
            CloudMiddleware middleware = new CloudMiddleware();
            middleware.addServer("localhost",61000);
            middleware.addServer("localhost",61001);
            middleware.addServer("localhost",61002);
            middleware.runMiddleware();
        } catch (IOException | ClassNotFoundException e){
            e.printStackTrace();
        }
    }
}
