package hw4;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

/**
 * Created by GleasonK on 3/21/16.
 */
public class CloudServer {
    public static final String EOF = "EOF";

    private ServerSocket serverSocket;
    private InetAddress address;
    private int serverPort;

    public CloudServer(String addr, int port) {
        try {
            this.serverPort = port;
            this.address = InetAddress.getByName(addr);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    public void runServer() throws IOException, ClassNotFoundException {
        this.serverSocket = new ServerSocket(this.serverPort);
        while (true) {
            System.out.println("Server listening for connections...");
            Socket client = serverSocket.accept();  //create a new socket to communicate with a client
            System.out.println("Connected to client - " + client.getLocalAddress().toString());
            Thread t = new Thread(new ServerThread(client));
            t.start();
        }
    }

    class ServerThread implements Runnable {
        private Socket client;

        public ServerThread(Socket client) {
            this.client = client;
        }

        @Override
        public void run() {
            try {
                InputStream in = client.getInputStream();
                ObjectInputStream ois = new ObjectInputStream(in);
                ObjectOutputStream oos = new ObjectOutputStream(client.getOutputStream());
                CloudData data = (CloudData) ois.readObject();
                System.out.println("Received command - " + data.toString());
                switch (data.command) {
                    case CloudData.CMD_DOWNLOAD:
                        downloadFile(oos, data);
                        break;
                    case CloudData.CMD_UPLOAD:
                        uploadFile(ois, data);
                        break;
                }
                System.out.println("Command Complete");
                ois.close();
                oos.close();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        private void downloadFile(ObjectOutputStream oos, CloudData data) throws IOException {
            File f = new File(data.filename + "_server");
            if (!f.exists()){
                oos.writeObject(EOF);
                return;
            }
            Scanner scan = new Scanner(f);
            while (scan.hasNext()) {
                String line = scan.nextLine();
                oos.writeObject(line);
            }
            oos.writeObject(EOF);
            scan.close();
        }

        private void uploadFile(ObjectInputStream ois, CloudData data) throws IOException, ClassNotFoundException {
            File f = new File(data.filename + "_server");
            PrintWriter writer = new PrintWriter(f);
            String line = (String) ois.readObject();
            while (!line.equals(EOF)) {
                writer.println(line);
                line = (String) ois.readObject();
            }
            writer.close();
        }
    }

    public static void main(String[] args) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    new CloudServer("localhost", 61000).runServer();
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    new CloudServer("localhost", 61001).runServer();
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    new CloudServer("localhost", 61002).runServer();
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }
}
