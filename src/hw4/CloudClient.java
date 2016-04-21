package hw4;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;

/**
 * Created by GleasonK on 3/21/16.
 */
public class CloudClient {
    public static final String EOF = "EOF";
    public static final int BUF_LENGTH = 2048;

    private InetAddress address;
    private int serverPort;

    public CloudClient(int port, String addr){
        try {
            this.serverPort = port;
            this.address = InetAddress.getByName(addr);

        } catch (IOException e){
            e.printStackTrace();
            System.exit(0);
        }
    }

    // Read client input and spawn thread to do work
    public void runClient(){
        Scanner scan = new Scanner(System.in);
        while(true){
            System.out.print("1. Download\n2. Upload\nEnter Command Number: ");
            int choice = scan.nextInt();
            String cmd = choice == 1 ? CloudData.CMD_DOWNLOAD : CloudData.CMD_UPLOAD;
            System.out.print("Enter filename: ");
            String fname = scan.next();
            if (!fname.toLowerCase().equals(fname)){
                System.out.println("Error - Only use lowercase!");
                continue;
            }
            CloudData data = new CloudData(cmd, fname);
            Thread t = new Thread(new ClientThread(data));
            t.start();
        }
    }

    class ClientThread implements Runnable{
        private CloudData data;
        private Socket server;
        private byte[] repBuff, recBuff;
        private DatagramSocket socket;
        private DatagramPacket packet;

        public ClientThread(CloudData data){
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
                this.packet = new DatagramPacket(this.repBuff, this.repBuff.length, address, serverPort);
                socket.send(this.packet);
                oos.close();
                baos.close();

                this.packet = new DatagramPacket(this.recBuff, this.recBuff.length); // Packet came from server thread
                socket.receive(this.packet);
                ByteArrayInputStream bais = new ByteArrayInputStream(this.packet.getData());
                ObjectInputStream ois = new ObjectInputStream(bais);
                ServerInfo serverInfo = (ServerInfo) ois.readObject();
                ois.close();
                bais.close();
                executeCommand(serverInfo, data);
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        public void executeCommand(ServerInfo serverInfo, CloudData data) throws IOException, ClassNotFoundException{
            System.out.println(serverInfo);
            this.server = new Socket(serverInfo.ip, serverInfo.port);
            ObjectOutputStream oos = new ObjectOutputStream(server.getOutputStream());

            oos.writeObject(data);
            oos.flush();
            System.out.println(data);
            switch (data.command){
                case CloudData.CMD_UPLOAD:
                    uploadFile(oos, data);
                    break;
                case CloudData.CMD_DOWNLOAD:
                    ObjectInputStream ois = new ObjectInputStream(server.getInputStream());
                    downloadFile(ois, data);
                    ois.close();
                    break;
            }
            oos.close();
            server.close();
        }

        public void uploadFile(ObjectOutputStream oos, CloudData data) throws IOException{
            File f = new File(data.filename);
            if (!f.exists()){
                System.out.println("File not found");
                oos.writeObject(EOF);
                return;
            }
            Scanner scan = new Scanner(f);
            while (scan.hasNext()){
                String line = scan.nextLine();
                oos.writeObject(line);
            }
            oos.writeObject(EOF);
            scan.close();
        }

        public void downloadFile(ObjectInputStream ois, CloudData data) throws IOException, ClassNotFoundException{
            File f = new File(data.filename);
            PrintWriter writer = new PrintWriter(f);
            String line = (String) ois.readObject();
            while (!line.equals(EOF)) {
//                System.out.println(line);
                writer.println(line);
                line = (String) ois.readObject();
            }
            writer.close();
        }
    }


    public static void main(String[] args) {
        new CloudClient(CloudMiddleware.DEFAULT_PORT, CloudMiddleware.SERVER_IP).runClient();
    }
}
