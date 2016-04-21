package hw5;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Created by GleasonK on 3/21/16.
 */
public class CloudClient {
    //TODO: Delete this. Make middleware
    private List<ServerInfo> remoteManagers;

    public static final int BUF_LENGTH = 2048;

    private InetAddress rmAddress;
    private int rmPort;

    public CloudClient(int port, String addr){
        try {
            this.remoteManagers = new ArrayList<>();
            this.rmPort = port;
            this.rmAddress = InetAddress.getByName(addr);
        } catch (IOException e){
            e.printStackTrace();
            System.exit(0);
        }
    }

    private void addRM(ServerInfo rm){
        this.remoteManagers.add(rm);
    }

    // Read client input and spawn thread to do work
    public void runClient(){
        Scanner scan = new Scanner(System.in);
        while(true){
            System.out.print("1. Download\n2. Upload\nEnter Command Number: ");
            int choice = scan.nextInt();
            String cmd = choice == 1 ? CloudMessage.CMD_DOWNLOAD : CloudMessage.CMD_UPLOAD;
            System.out.print("Enter filename: ");
            String fname = scan.next();
            if (!fname.toLowerCase().equals(fname)){
                System.out.println("Error - Only use lowercase!");
                continue;
            }
            CloudMessage data = new CloudMessage(cmd, fname);
            Thread t = new Thread(new ClientThread(data));
            t.start();
        }
    }

    class ClientThread implements Runnable{
        private CloudMessage data;
        private Socket server;
        private byte[] repBuff, recBuff;
        private DatagramSocket socket;
        private DatagramPacket packet;

        public ClientThread(CloudMessage data){
            try {
                this.data = data;
                this.socket = new DatagramSocket();
                this.repBuff = new byte[BUF_LENGTH];
                this.recBuff = new byte[BUF_LENGTH];
            } catch (IOException e){
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                // First contact the Middleware to get a Server
                ByteArrayOutputStream baos = new ByteArrayOutputStream(BUF_LENGTH);
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject(data);
                this.repBuff = baos.toByteArray();

                // TODO: rmPort to middleware port etc
                int hash = calculateHash((String)data.data);
                ServerInfo rm = remoteManagers.get(hash % remoteManagers.size());
                this.packet = new DatagramPacket(this.repBuff, this.repBuff.length, InetAddress.getByName(rm.ip), rmPort);
                socket.send(this.packet);
                oos.close();
                baos.close();

                this.packet = new DatagramPacket(this.recBuff, this.recBuff.length); // Packet came from server thread
                socket.receive(this.packet);
                ByteArrayInputStream bais = new ByteArrayInputStream(this.packet.getData());
                ObjectInputStream ois = new ObjectInputStream(bais);
                ServerInfo serverInfo = (ServerInfo) ois.readObject();
                serverInfo.ip = rm.ip; // TODO: In distributed env servers would give own. Servers giving localhost here though
                ois.close();
                bais.close();
                executeCommand(serverInfo, data);
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        public void executeCommand(ServerInfo serverInfo, CloudMessage data) throws IOException, ClassNotFoundException{
            System.out.println(serverInfo);
            this.server = new Socket(serverInfo.ip, serverInfo.port);
            ObjectOutputStream oos = new ObjectOutputStream(server.getOutputStream());

            oos.writeObject(data);
            oos.flush();
            System.out.println(data);
            switch (data.type){
                case CloudMessage.CMD_UPLOAD:
                    uploadFile(oos, data);
                    break;
                case CloudMessage.CMD_DOWNLOAD:
                    ObjectInputStream ois = new ObjectInputStream(server.getInputStream());
                    downloadFile(ois, data);
                    ois.close();
                    break;
            }
            oos.close();
            server.close();
        }

        public void uploadFile(ObjectOutputStream oos, CloudMessage data) throws IOException{
            if (!(data.data instanceof String)) return;
            File f = new File((String)data.data);
            if (!f.exists()){
                System.out.println("File not found");
                oos.writeObject(CloudMessage.EOF);
                return;
            }
            Scanner scan = new Scanner(f);
            while (scan.hasNext()){
                String line = scan.nextLine();
                oos.writeObject(line);
            }
            oos.writeObject(CloudMessage.EOF);
            scan.close();
        }

        //TODO: Check if file is empty (failed transmission) delete if it is.
        public void downloadFile(ObjectInputStream ois, CloudMessage data) throws IOException, ClassNotFoundException{
            if (!(data.data instanceof String)) return;
            File f = new File((String)data.data);
            PrintWriter writer = new PrintWriter(f);
            String line = (String) ois.readObject();
            while (!line.equals(CloudMessage.EOF)) {
                writer.println(line);
                line = (String) ois.readObject();
            }
            writer.close();
        }
    }

    public int calculateHash(String filename){
        filename = filename.replaceAll("\\.","");
        char[] chars = filename.toCharArray();
        int sum=0;
        for (int i = 0; i < chars.length; i++) {
            int val = 1 + chars[i] - 'a';
            sum+= val;
        }
        return sum;
    }


    public static void main(String[] args) {
        CloudClient client = new CloudClient(Config.REMOTE_MANAGER_PORT_1, Config.REMOTE_MANAGER_IP_1);
        client.addRM(new ServerInfo(Config.REMOTE_MANAGER_IP_1, Config.REMOTE_MANAGER_PORT_1));
        client.addRM(new ServerInfo(Config.REMOTE_MANAGER_IP_2, Config.REMOTE_MANAGER_PORT_2));
        client.addRM(new ServerInfo(Config.REMOTE_MANAGER_IP_3, Config.REMOTE_MANAGER_PORT_3));
        client.runClient();
    }
}
