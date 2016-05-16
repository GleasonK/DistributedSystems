package hw6;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Created by GleasonK on 3/21/16.
 */
public class RemoteManager {
    public static final int BUF_LENGTH = 2048;
    private int rmPort;
    private Set<String> fileCache;
    private final List<ServerInfo> servers;
    private DatagramSocket socket;
    private DatagramPacket packet;
    private MulticastSocket mcSocket;
    private InetAddress mcAddress;
    private int mcPort;
    private byte[] recBuf;

    public RemoteManager(int rmPort, String mcIP, int mcPort) {
        this.servers = new ArrayList<>();
        this.fileCache = new HashSet<>();
        try {
            this.socket = new DatagramSocket(rmPort);
            this.recBuf = new byte[BUF_LENGTH];
            this.mcAddress = InetAddress.getByName(mcIP);
            this.mcPort = mcPort;
            this.mcSocket = new MulticastSocket(mcPort);
            this.mcSocket.setInterface(InetAddress.getLocalHost());
            System.out.println("Listening on port " + rmPort + " for connections...");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void runRemoteManager() throws IOException, ClassNotFoundException {
        new Thread(new MulticastListenThread()).start();
        while(true){
            System.out.println("Waiting for data...");
            this.packet = new DatagramPacket(this.recBuf, this.recBuf.length);
            this.socket.receive(this.packet);
            Thread t = new Thread(new RMThread(this.packet));
            t.start();
        }
    }

    class MulticastListenThread implements Runnable {
        @Override
        public void run() {
            try {
                mcSocket.joinGroup(mcAddress);
                while (true) {
                    CloudMessage incomingMsg = (CloudMessage) receiveObject();
                    System.out.println("RM_Multicast Received - " + incomingMsg.toString());

                    switch (incomingMsg.type) {
                        case CloudMessage.CMD_NEW_FILE: // Other server has newer version
                            CloudFile cf = (CloudFile) incomingMsg.data;
                            if (fileCache.contains(cf.getNameWithVersion())) break;
                            System.out.println("RM_FileUpdate - " + incomingMsg.toString());
                            notifyServer(incomingMsg);
                            break;
                        case CloudMessage.CMD_REQUEST_ALL:
                            System.out.println("RM_Request_All");
                            notifyServer(incomingMsg);
                            break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        public void notifyServer(CloudMessage cm) throws IOException, ClassNotFoundException {
            if (servers.isEmpty()) return; // No one to notify
            ServerInfo si = servers.get(0);
            System.out.println("Notifying " + si.toString());
            Socket server = new Socket(si.ip, si.port);
            ObjectOutputStream oos = new ObjectOutputStream(server.getOutputStream());
            ObjectInputStream ois  = new ObjectInputStream(server.getInputStream());
            oos.writeObject(cm);
            System.out.println("Notified " + si.toString());
            ois.readObject();
            server.close();
        }

        public Object receiveObject() throws IOException, ClassNotFoundException {
            byte[] buff = new byte[1024];
            DatagramPacket rPacket = new DatagramPacket(buff, buff.length);
            mcSocket.receive(rPacket);
            ByteArrayInputStream bais = new ByteArrayInputStream(rPacket.getData());
            ObjectInputStream ois = new ObjectInputStream(bais);
            CloudMessage cm = (CloudMessage) ois.readObject();
            ServerInfo si=null;
            if (cm.data instanceof CloudFile) {
                CloudFile cf = (CloudFile) cm.data;
                si = cf.getLocation();
            } else {
                si = (ServerInfo)cm.data;
            }
            if (si.ip==null) si.ip = rPacket.getAddress().getHostAddress();
            ois.close();
            bais.close();
            return cm;
        }
    }

    /**
     * Server Communication Methods
     */
    private void broadcastFileUpdate(CloudFile cf){
        if (fileCache.contains(cf.getNameWithVersion())) { // TODO: Might not be necessary
            System.out.println("Ignoring broadcast. Already have " + cf.getNameWithVersion());
            return;
        }
        this.fileCache.add(cf.getNameWithVersion());
        try {
            CloudMessage msg = new CloudMessage(CloudMessage.CMD_NEW_FILE, cf);
            broadcastRMMessage(msg);
        } catch( IOException e){
            e.printStackTrace();
        }
    }

    private void requestAllFiles(ServerInfo si){
        try {
            CloudMessage cm = new CloudMessage(CloudMessage.CMD_REQUEST_ALL, si);
            broadcastRMMessage(cm);
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    private void broadcastRMMessage(CloudMessage msg) throws IOException{
        ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(msg);
        byte[] buff = baos.toByteArray();
        oos.close();
        baos.close();
        DatagramPacket packet = new DatagramPacket(buff, buff.length, mcAddress, mcPort);
        mcSocket.send(packet);
        System.out.println("MULTICAST SENT");
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

    public void addServer(String addr, int port){
        ServerInfo si = new ServerInfo(addr, port);
        if (this.servers.contains(si)) return;
        registerServer(si);
    }

    // Add server to list and begin a heartbeat connection
    private synchronized void registerServer(ServerInfo serverInfo){
        if (this.servers.isEmpty()) {
//            fileCache.clear();
            requestAllFiles(serverInfo);
        }
        this.servers.add(serverInfo);
        Thread t = new Thread(new HeartBeatThread(serverInfo));
        t.start();
    }

    class RMThread implements Runnable {
        private DatagramPacket iPacket, rPacket;
        private byte[] buff;

        public RMThread(DatagramPacket data){
            this.iPacket = data;
        }

        @Override
        public void run() {
            try {
                ByteArrayInputStream bais = new ByteArrayInputStream(this.iPacket.getData());
                ObjectInputStream ois = new ObjectInputStream(bais);
                CloudMessage data = (CloudMessage) ois.readObject();
                ois.close();
                bais.close();

                switch (data.type){
                    case CloudMessage.CMD_DOWNLOAD:
                        handleClient(data);
                        break;
                    case CloudMessage.CMD_UPLOAD:
                        handleClient(data);
                        break;
                    case CloudMessage.CMD_SERVER_UP:
                        handleServer(data);
                        break;
                    case CloudMessage.CMD_NEW_FILE:
                        handleNewFile(data);
                        break;
                    default:
                        break;
                }
            } catch (IOException | ClassNotFoundException e){
                e.printStackTrace();
            }
        }

        public void handleClient(CloudMessage data) throws IOException{
            ByteArrayOutputStream baos = new ByteArrayOutputStream(BUF_LENGTH);
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            if (!(data.data instanceof String)) return;
            String dataString = (String) data.data;
            int hashKey = calculateHash(dataString);
            System.out.println("Hash=>"+hashKey);
            ServerInfo server = servers.get(hashKey % servers.size());

            oos.writeObject(server);
            buff = baos.toByteArray();

            this.rPacket = new DatagramPacket(buff, buff.length, this.iPacket.getAddress(), this.iPacket.getPort());
            socket.send(this.rPacket);
        }

        public void handleServer(CloudMessage data) throws IOException{
            if (!(data.data instanceof ServerInfo)) return;
            ServerInfo si = (ServerInfo) data.data;
            String serverIP = this.iPacket.getAddress().getHostAddress();
            int port = si.port;
            addServer(serverIP, port);
        }

        public void handleNewFile(CloudMessage data){
            if (!(data.data instanceof CloudFile)) return;
            CloudFile cf = (CloudFile) data.data;
            broadcastFileUpdate(cf);
        }
    }

    class HeartBeatThread implements Runnable {
        private static final boolean DEBUG = true;
        private ServerInfo serverInfo;
        private boolean responded;
        private boolean running;
        private Socket server;
        private ObjectOutputStream oos;
        private ObjectInputStream  ois;

        public HeartBeatThread(ServerInfo serverInfo){
            this.serverInfo = serverInfo;
            this.responded = false;
            this.running = true;
        }

        @Override
        public void run() {
            try {
                server = new Socket(serverInfo.ip, serverInfo.port);
                oos    = new ObjectOutputStream(server.getOutputStream());
                ois    = new ObjectInputStream(server.getInputStream());
            } catch (IOException e){
                e.printStackTrace();
            }

            while(running) {
                ExecutorService executor = Executors.newSingleThreadExecutor();
                Future<Boolean> pingAck  = executor.submit(new PingTask());
                try {
                    if (DEBUG) System.out.println("Pinging..");
                    this.responded = pingAck.get(Config.HEARTBEAT_TIMEOUT, TimeUnit.SECONDS);
                    if (DEBUG) System.out.println("Response from "+server.toString()+": " + responded);
                } catch (Exception e) { // TimeoutException Interrupted and Execution Exceptions
                    pingAck.cancel(true);
                    handleTimeout();
                } finally {
                    executor.shutdownNow();
                }

                // Sleep until next ping/ack
                if (running)
                    try { Thread.sleep(Config.HEARTBEAT_INTERVAL); } catch (InterruptedException e){ e.printStackTrace(); }
                this.responded = false; // Reset responded
            }
        }

        private void handleTimeout() { // Synchronized because of the remove
            this.running = false;
            synchronized (servers){
                servers.remove(serverInfo);
                if (DEBUG) {
                    System.out.println(serverInfo.toString() + " - Timeout, Removing from active servers.");
                    System.out.println("Remaining Servers: " + servers);
                }
            }
            try {
                if (!server.isClosed())server.close();
            } catch (IOException e){
                e.printStackTrace();
            } // Close the socket
        }

        // Consider multicast heartbeat? Weird comparisons required
        class PingTask implements Callable<Boolean> {

            @Override
            public Boolean call() throws Exception {
                CloudMessage cm = new CloudMessage(CloudMessage.CMD_HEARTBEAT, CloudMessage.PING);
                oos.writeObject(cm);

                cm = (CloudMessage) ois.readObject();
                System.out.println("HeartBeat from " + serverInfo.toString() + " - " + cm.toString());
                return true;
            }
        }
    }

    public void testHeartBeat(){
        try {
            System.out.println(servers);
            ServerInfo si = servers.isEmpty() ? new ServerInfo("",1000) : servers.get(0);
            Thread t = new Thread(new HeartBeatThread(si));
            t.start();
            t.join();
            System.out.println(servers);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            RemoteManager rm = new RemoteManager(Config.REMOTE_MANAGER_PORT_1, Config.RM_MULTICAST_IP, Config.RM_MULTICAST_PORT);
            rm.runRemoteManager();
        } catch (Exception e){
            e.printStackTrace();
        }

    }


}
