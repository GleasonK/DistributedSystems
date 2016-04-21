package hw2;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Created by GleasonK on 2/13/16.
 */
public class DistMaxClient {
    private DatagramSocket sock;
    private DatagramPacket packet, rPacket;
    private InetAddress address;
    private byte[] repBuff, recBuff;
    private int serverPort;

    public DistMaxClient(String addr, int port){
        try {
            this.serverPort = port;
            this.sock = new DatagramSocket();
            this.address = InetAddress.getByName(addr);
            this.repBuff = new byte[DistMaxServer.BUF_LENGTH];
            this.recBuff = new byte[DistMaxServer.BUF_LENGTH];
        } catch (IOException e){
            e.printStackTrace();
            System.exit(0);
        }
    }

    public void processData() throws IOException, ClassNotFoundException{
        while(true){
            MaxData reqData = new MaxData(0,null,MaxData.TYPE_REQUEST); // Send request to server
            sendMaxData(reqData);

            // Receive a row from the server. If type RESULT, we are done.
            this.rPacket = new DatagramPacket(this.recBuff, this.recBuff.length);
            this.sock.receive(this.rPacket);
            ByteArrayInputStream bais = new ByteArrayInputStream(this.rPacket.getData());
            ObjectInputStream ois = new ObjectInputStream(bais);
            MaxData data = (MaxData) ois.readObject();

            if (data.type.equals(MaxData.TYPE_REQUEST)){
                Thread t = new Thread(new MaxFinderT(data));
                t.start();
            } else {
                System.out.println("Server - Max val was " + data.max);
                break; // We are done once TYPE_RESULT type is received.
            }
        }
    }

    public synchronized void sendMaxData(MaxData data){
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(DistMaxServer.BUF_LENGTH);
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(data);
            this.repBuff = baos.toByteArray();
            this.packet = new DatagramPacket(this.repBuff, this.repBuff.length, this.address, this.serverPort);
            this.sock.send(this.packet);
            oos.close(); baos.close();
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    class MaxFinderT implements Runnable {
        private MaxData data;

        public MaxFinderT(MaxData data){
            this.data = data;
        }

        @Override
        public void run(){
            System.out.println("Data " + data.toString() + " running...");
            int max = data.vals[0];
            for (int i=0; i<data.vals.length; i++){
                if (data.vals[i] > max) max=data.vals[i];
            }
            MaxData reply = new MaxData(data.row,null,MaxData.TYPE_RESULT);
            reply.setMax(max);
            sendMaxData(reply);
        }
    }

    public static void main(String[] args) {
        DistMaxClient client = new DistMaxClient("localhost",DistMaxServer.DEFAULT_PORT);
        try {
            client.processData();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

}
