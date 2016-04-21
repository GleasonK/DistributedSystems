package hw2;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by GleasonK on 2/13/16.
 */
public class DistMaxServer {
    public  static final int DEFAULT_PORT = 60010;
    public  static final int BUF_LENGTH = 2048;
    private static final int MAX_VAL = 100;

    private Set<ClientInfo> clients;
    private DatagramSocket sock;
    private DatagramPacket packet, replyPacket;
    private int N,M;
    private int[][] matrix;
    private int[] maxVals;
    private byte[] recBuf;
    private byte[] repBuf;

    public DistMaxServer(int N, int M){
        this.N=N;
        this.M=M;
        this.clients = new HashSet<>();
        populateMatrix();
        printMatrix();
        this.maxVals = new int[N];
        try {
            this.sock = new DatagramSocket(DEFAULT_PORT);
            this.recBuf = new byte[BUF_LENGTH];
            System.out.println("Listening on port "+DEFAULT_PORT+" for connections...");
        } catch (SocketException e){
            e.printStackTrace();
        }
    }

    public void runServer() throws IOException, ClassNotFoundException{
        int row=0;

        while (row < matrix.length){
            System.out.println("Waiting for data...");
            this.packet = new DatagramPacket(this.recBuf, this.recBuf.length);
            this.sock.receive(this.packet);
            ByteArrayInputStream bais = new ByteArrayInputStream(this.packet.getData());
            ObjectInputStream ois = new ObjectInputStream(bais);
            MaxData data = (MaxData) ois.readObject();
            ois.close(); bais.close();
            ClientInfo ci = new ClientInfo(packet.getAddress(),packet.getPort());
            if (!clients.contains(ci)) clients.add(ci); // Keep track of clients.
            System.out.println("Received MaxData - " + data.toString());
            switch (data.type){
                case MaxData.TYPE_REQUEST: // Send row to client
                    MaxData repData = new MaxData(row, matrix[row],MaxData.TYPE_REQUEST);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream(BUF_LENGTH);
                    ObjectOutputStream oos = new ObjectOutputStream(baos);
                    oos.writeObject(repData);
                    this.repBuf = baos.toByteArray();
                    this.replyPacket = new DatagramPacket(repBuf, repBuf.length, this.packet.getAddress(), this.packet.getPort());
                    this.sock.send(this.replyPacket);
                    oos.close(); baos.close();
                    System.out.println(String.format("Sent row %d to %s:%d\n", row, packet.getAddress().toString(), packet.getPort()));
                    row++;
                    break;
                case MaxData.TYPE_RESULT:  // Register a result
                    this.maxVals[data.row] = data.max;
                    break;
            }
        }
        System.out.println("MaxVals: " + Arrays.toString(this.maxVals));

        int max = this.maxVals[0];
        for (int i=0; i<maxVals.length; i++){
            if (maxVals[i] > max) max = maxVals[i];
        }
        System.out.println("ClientList: " + clients.toString());
        MaxData repData = new MaxData(row, null, MaxData.TYPE_RESULT);
        repData.setMax(max);
        ByteArrayOutputStream baos = new ByteArrayOutputStream(BUF_LENGTH);
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(repData);
        this.repBuf = baos.toByteArray();
        oos.close(); baos.close();
        for (ClientInfo ci : clients){
            this.replyPacket = new DatagramPacket(repBuf, repBuf.length, ci.address, ci.port);
            this.sock.send(this.replyPacket);
        }
        System.out.println("Max Value: " + max);
    }

    public void populateMatrix(){
        this.matrix = new int[N][M];   // [[],[]]
        for (int i=0; i<N; i++) {
            for (int j=0; j<M; j++) {
                this.matrix[i][j] = (int)(Math.random()*MAX_VAL);
            }
        }
    }

    private void printMatrix(){
        for(int i=0; i<N; i++){
            System.out.println(Arrays.toString(this.matrix[i]));
        }
    }

    class ClientInfo {
        private InetAddress address;
        private int port;
        public ClientInfo(InetAddress addr, int port){
            this.address = addr;
            this.port = port;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof ClientInfo){
                ClientInfo ci = (ClientInfo) obj;
                return ci.port==port && ci.address.equals(address);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return address.toString().hashCode() ^ port;
        }

        @Override
        public String toString() {
            return this.address.toString() + ":" + this.port;
        }
    }

    public static void main(String[] args) {
        try {
            new DistMaxServer(8,5).runServer();
        } catch (IOException | ClassNotFoundException e){
            e.printStackTrace();
        }
    }
}
