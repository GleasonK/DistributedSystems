package hw6;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by GleasonK on 3/21/16.
 */
public class CloudServer {
    private ServerSocket serverSocket;
    private MulticastSocket mcSocket;
    private InetAddress address, mcAddress;
    private int serverPort, mcPort, rmPort;
    private String fileStorePath;
    private Map<String,CloudFile> fileSystem;
    private Map<String,CloudFile> dirtyFiles;
    private Map<String,List<CloudFile>> fileCandidates;
    public static final int BUF_LENGTH = 2048;
    private boolean connected;

    public CloudServer(int port, String rmAddr, int rmPort, String mcIP, int mcPort) {
        try {
            this.serverPort = port;
            this.rmPort = rmPort;
            this.address = InetAddress.getByName(rmAddr);
            this.mcAddress = InetAddress.getByName(mcIP);
            this.mcPort = mcPort;
            this.fileStorePath = Config.FILE_STORE_PATH + "/" + port;
            this.fileSystem = FileUtils.getFileMap(this.fileStorePath, serverPort);
            this.dirtyFiles = new ConcurrentHashMap<String,CloudFile>(this.fileSystem);
            this.fileCandidates = new ConcurrentHashMap<String,List<CloudFile>>();
            this.mcSocket = new MulticastSocket(mcPort);
            this.mcSocket.setInterface(InetAddress.getLocalHost());
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        }

        System.out.println(fileSystem);

        if (!makeServerDirectory(this.fileStorePath)){
            System.out.println("Failed to make data directory. Permission error.");
            System.exit(0);
        }
    }

    // Create a directory for each server to hold their data
    private boolean makeServerDirectory(String path){
        File repoDir = new File(path);
        if (!repoDir.exists()) {
            System.out.println("Creating server data directory: " + repoDir);
            try{
                return repoDir.mkdirs();
            } catch(SecurityException se){
                return false;
            }
        }
        return true;
    }

    private String getFileStorePath(CloudFile cf){
        return fileStorePath + File.separator + cf.getNameWithVersion();
    }

    public void runServer() throws IOException, ClassNotFoundException {
        Thread t;
        t = new Thread(new ServerStartThread(rmPort));
        t.start();
        this.serverSocket = new ServerSocket(this.serverPort);
        while (true) {
            System.out.println("Server listening for connections...");
            Socket client = serverSocket.accept();  //create a new socket to communicate with a client
            System.out.println("Connected to client - " + client.getLocalAddress().toString());
            t = new Thread(new ServerThread(client));
            t.start();
        }
    }

    /**
     * Thread sets up the heartbeat with the RM
     * Then, remains active as the MulticastReceiver
     */
    class ServerStartThread implements Runnable {

        private Socket server;
        private DatagramSocket socket;
        private DatagramPacket packet;
        private int rmPort;

        public ServerStartThread(int rmPort) {
            try {
                this.socket = new DatagramSocket();
                this.rmPort = rmPort;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            new Thread(new HeartbeatThread()).start();
            new Thread(new MulticastListenThread()).start();
            new Thread(new DirtyUpdaterThread()).start();
        }

        class HeartbeatThread implements Runnable {

            @Override
            public void run() {
                CloudMessage cm = new CloudMessage(CloudMessage.CMD_SERVER_UP, new ServerInfo(address.getHostAddress(), serverPort));
                    while (true) {
                        try {
                            ByteArrayOutputStream baos = new ByteArrayOutputStream(BUF_LENGTH);
                            ObjectOutputStream oos = new ObjectOutputStream(baos);
                            oos.writeObject(cm);
                            byte[] repBuff = baos.toByteArray();
                            packet = new DatagramPacket(repBuff, repBuff.length, address, rmPort);
                            socket.send(packet);
                            oos.close();
                            baos.close();
                            if (connected) {
                                System.out.println("Reminding RM... Will send next reminder in 15 seconds.");
                                Thread.sleep(15 * 1000);
                            } else {
                                System.out.println("Trying to connect to RM...");
                                Thread.sleep(1 * 1000);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
            }
        }

        class MulticastListenThread implements Runnable {
            @Override
            public void run() {
                try {
                    mcSocket.joinGroup(mcAddress);
                    while (true) {
                        CloudMessage incomingMsg = (CloudMessage) receiveObject();
                        System.out.println(serverPort + ": MULTICAST_RECEIVE " + incomingMsg.toString());
                        CloudFile cf = (CloudFile) incomingMsg.data;
                        if (cf.getLocation().port == serverPort) continue;
                        switch (incomingMsg.type) {
                            case CloudMessage.CMD_UPDATE: // Other server has newer version
                                dirtyFiles.put(cf.getName(), cf);
                                System.out.println("FileUpdate - " + incomingMsg);
                                System.out.println(dirtyFiles.toString());
                                break;
                            case CloudMessage.CMD_REQUEST: // Other server requesting a file
                                CloudFile respCf = dirtyFiles.get(cf.getName());
                                if (respCf == null) respCf = fileSystem.get(cf.getName());
                                if (respCf == null) break;
                                CloudMessage resp = new CloudMessage(CloudMessage.CMD_RESPONSE, respCf);
                                ServerInfo location = cf.getLocation();
                                Socket server = new Socket(location.ip, location.port);
                                ObjectOutputStream oos = new ObjectOutputStream(server.getOutputStream());
                                oos.writeObject(resp);
                                oos.flush();
//                                server.close(); //TODO: Uncomment?
                                break;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }

            }

        }

        class DirtyUpdaterThread implements Runnable {
            @Override
            public void run() {
                try {
                    while (true) {
                        Thread.sleep(Config.DIRTY_UPDATE_INTERVAL);
                        System.out.println(serverPort + " Updating Dirty Files - " + dirtyFiles.toString());
                        for (CloudFile df : dirtyFiles.values()) {
                            ServerInfo si = df.getLocation(); //TODO: Maybe unnecessary?
                            if (si.ip==null)
                                requestFile(df);
                            else
                                retrieveFile(df);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        public Object receiveObject() throws IOException, ClassNotFoundException {
            byte[] buff = new byte[1024];
            DatagramPacket rPacket = new DatagramPacket(buff, buff.length);
            mcSocket.receive(rPacket);
            ByteArrayInputStream bais = new ByteArrayInputStream(rPacket.getData());
            ObjectInputStream ois = new ObjectInputStream(bais);
            CloudMessage cm = (CloudMessage) ois.readObject();
            CloudFile cf = (CloudFile)cm.data;
            ServerInfo location = cf.getLocation();
            if (location.ip==null) location.ip = rPacket.getAddress().getHostAddress();
            ois.close();
            bais.close();
            return cm;
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
                CloudMessage data = (CloudMessage) ois.readObject();
                System.out.println(serverPort + " Received Command - " + data.toString());
                switch (data.type) {
                    case CloudMessage.CMD_DOWNLOAD:
                        downloadFile(oos, data);
                        break;
                    case CloudMessage.CMD_UPLOAD:
                        uploadFile(ois, data);
                        break;
                    case CloudMessage.CMD_HEARTBEAT:
                        ack(ois, oos, data);
                        break;
                    case CloudMessage.CMD_RESPONSE:
                        addCandidate(data);
                        break;
                    case CloudMessage.CMD_NEW_FILE:
                        handleNewFile(data, oos);
                        break;
                    case CloudMessage.CMD_REQUEST_ALL:
                        responseAll(data,oos);
                        break;
                }
                System.out.println(serverPort + " Command Complete");
                ois.close();
                oos.close();
                client.close();
            } catch (EOFException e) {
                System.out.println("Lost connection to RM!");
                connected=false;
            } catch (Exception e){
                System.out.println("State change - " + e.getMessage());
            }
        }

        private void addCandidate(CloudMessage msg){
            CloudFile cf = (CloudFile) msg.data;
            ServerInfo location = cf.getLocation();
            if (location.ip==null) location.ip = client.getInetAddress().getHostAddress();
            if (fileCandidates.containsKey(cf.getName())) fileCandidates.get(cf.getName()).add(cf);
        }

        private void downloadFile(ObjectOutputStream oos, CloudMessage data) throws IOException, ClassNotFoundException {
            if (!(data.data instanceof String)) return;
            CloudFile cf = fileSystem.get(data.data);
            if (cf==null) { // TODO: Check other Servers for the file
                System.out.println("Searching for file...");
                cf = FileUtils.strToCloudFile((String)data.data);
                cf.setLocation(new ServerInfo(null, serverPort));
                cf = requestFile(cf);
                if (cf==null){ // TODO: Have client handle this as file not found on server
                    oos.writeObject(CloudMessage.EOF);
                    return;
                }
            } else if (dirtyFiles.containsKey(cf.getName())){
                cf = dirtyFiles.get(cf.getName()); // Contains IP and version number
                if (cf.getLocation().ip != null) retrieveFile(cf);
                else cf = requestFile(cf);
            }

            File f = new File(fileStorePath + "/" + cf.getNameWithVersion());
            if (!f.exists()){  // TODO: Have client check for empty file, not found error message there.
                oos.writeObject(CloudMessage.EOF);
                return;
            }
            Scanner scan = new Scanner(f);
            while (scan.hasNext()) {
                String line = scan.nextLine();
                oos.writeObject(line);
            }
            oos.writeObject(CloudMessage.EOF);
            scan.close();
        }

        private void uploadFile(ObjectInputStream ois, CloudMessage data) throws IOException, ClassNotFoundException {
            if (!(data.data instanceof String)) return;
            // Check if file is in filesystem, increment version if it is
            CloudFile oldCf;
            CloudFile newCf;
            synchronized (fileSystem) {
                oldCf = fileSystem.remove(data.data);
                if (oldCf != null) newCf = CloudFile.incrementedVersion(oldCf);
                else newCf = FileUtils.strToCloudFile((String) data.data);
                // Clean up and remove old file
                fileSystem.put(newCf.getName(), newCf);
            }

            newCf.setLocation(new ServerInfo(null,serverPort));

            String filePath = fileStorePath + "/" + newCf.getNameWithVersion();
            System.out.println("Uploading to " + filePath);
            File f = new File(filePath);
            PrintWriter writer = new PrintWriter(f);
            String line = (String) ois.readObject();
            while (!line.equals(CloudMessage.EOF)) {
                writer.println(line);
                line = (String) ois.readObject();
            }
            writer.close();

            if (dirtyFiles.containsKey(newCf.getName())) dirtyFiles.remove(newCf.getName());
            if (oldCf != null && !oldCf.getNameWithVersion().equals(newCf.getNameWithVersion())) { // Could be issue if same name on update
                f = new File(getFileStorePath(oldCf));
                if (f.exists()) f.delete();
            }

            // Broadcast new file upload
            broadcastFileUpdate(newCf);
        }

        private void handleNewFile(CloudMessage data, ObjectOutputStream oos) throws IOException, ClassNotFoundException {
            if (!(data.data instanceof CloudFile)) return;
            oos.writeObject(data);
            CloudFile cf = (CloudFile) data.data;
            ServerInfo si = cf.getLocation();
            if (si.ip==null) si.ip = client.getInetAddress().getHostAddress();
            if (fileSystem.containsKey(cf.getName()) && // Old file
                    fileSystem.get(cf.getName()).getNameWithVersion().equals(cf.getNameWithVersion())) return;
            retrieveNewFile(cf);
            System.out.println("HANDLING NEW FILE3");
        }

        private void responseAll(CloudMessage data, ObjectOutputStream oos) throws IOException, ClassNotFoundException {
            if (!(data.data instanceof ServerInfo)) return;
            oos.writeObject("OK");
            oos.close();
            ServerInfo si = (ServerInfo) data.data;
            for (CloudFile f : fileSystem.values()){
                CloudMessage msg = new CloudMessage(CloudMessage.CMD_NEW_FILE, f);
                System.out.println("Offering - " + msg.toString() + " to " + si.toString());
                Socket server = new Socket(si.ip, si.port);
                oos = new ObjectOutputStream(server.getOutputStream());
                ObjectInputStream ois = new ObjectInputStream(server.getInputStream());
                oos.writeObject(msg);
                ois.readObject();
                oos.close();
                ois.close();
                server.close();
            }
        }


        // TODO: Catch exception and start a Reconnect loop (When RM goes down, Exception thrown)
        private void ack(ObjectInputStream ois, ObjectOutputStream oos, CloudMessage data) throws Exception {
            while (true) {
                connected=true;
                System.out.println("PingAck - " + serverPort);
                data.type = CloudMessage.ACK; // Change to ACK
                oos.writeObject(data);
                data = (CloudMessage) ois.readObject();
            }
        }
    }

    /**
     * Server Communication Methods
     */
    private void broadcastFileUpdate(CloudFile cf) throws IOException {
        notifyRM(cf);
        CloudMessage msg = new CloudMessage(CloudMessage.CMD_UPDATE, cf);
        broadcastCloudMessage(msg);
    }

    private void broadcastNewFileUpdate(CloudFile cf) throws IOException {
        CloudMessage msg = new CloudMessage(CloudMessage.CMD_UPDATE, cf);
        broadcastCloudMessage(msg);
    }

    private void notifyRM(CloudFile cf) throws IOException{
        DatagramSocket socket = new DatagramSocket();
        CloudMessage cm = new CloudMessage(CloudMessage.CMD_NEW_FILE, cf);
        ByteArrayOutputStream baos = new ByteArrayOutputStream(BUF_LENGTH);
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(cm);
        byte[] repBuff = baos.toByteArray();
        DatagramPacket packet = new DatagramPacket(repBuff, repBuff.length, address, rmPort);
        socket.send(packet);
        oos.close();
        baos.close();
        socket.close();
        System.out.println("Notified RM of new file!");
    }

    private void broadcastCloudMessage(CloudMessage msg) throws IOException{
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

    // TODO: Think about waiting for the multicast responses. How long, thread and kill?
    // TODO: How to timeout if no responses and list of candidates empty perhaps?
    public CloudFile requestFile(CloudFile cf) throws IOException, ClassNotFoundException {
        CloudMessage msg = new CloudMessage(CloudMessage.CMD_REQUEST,cf);
        ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(msg);
        byte[] buff = baos.toByteArray();
        oos.close();
        baos.close();
        List<CloudFile> candidates = new ArrayList<>();
        fileCandidates.put(cf.getName(),candidates);
        DatagramPacket packet = new DatagramPacket(buff, buff.length, mcAddress, mcPort);
        mcSocket.send(packet);
        System.out.println("MULTICAST SENT");
        long timeout = System.currentTimeMillis() + Config.FILE_REQUEST_TIMEOUT;
        while(System.currentTimeMillis() < timeout || !candidates.isEmpty()){
            if (candidates.isEmpty()){
                try {Thread.sleep(500);} catch (InterruptedException e){ e.printStackTrace(); }
            } else {
                CloudFile cfCandidate;
                synchronized (candidates){
                    cfCandidate = candidates.remove(0);
                }
                boolean retrieved = tryCandidate(cfCandidate);
                if (retrieved) {
                    fileCandidates.remove(cf.getName());
                    return cfCandidate;
                }
            }
        }
        fileCandidates.remove(cf.getName());
        System.out.println(serverPort + " Request Timeout - " + cf.toString());
        if (fileSystem.containsKey(cf.getName())){
            if (dirtyFiles.containsKey(cf.getName())) dirtyFiles.remove(cf.getName());
            return fileSystem.get(cf.getName());
        }
        return null;
    }

    public boolean tryCandidate(CloudFile candidate) throws IOException, ClassNotFoundException{
        System.out.println("Trying Candidate: " + candidate);
        if (candidate==null) return false;
        synchronized (fileSystem) {
            if (fileSystem.containsKey(candidate.getName())) {
                CloudFile oldCf = fileSystem.get(candidate.getName());
                if (oldCf != null && candidate.getVersion() >= oldCf.getVersion()) return false;
            }
        }

        retrieveFile(candidate);
        System.out.println("Retrieved - " + candidate.toString());
        return true;
    }

    public void retrieveFile(CloudFile cf) throws IOException, ClassNotFoundException {
        // Request download first
        System.out.println("Retrieve - " + cf.toString());
        ServerInfo location = cf.getLocation();
        Socket server = new Socket(location.ip, location.port);
        CloudMessage data = new CloudMessage(CloudMessage.CMD_DOWNLOAD,cf.getName());
        ObjectOutputStream oos = new ObjectOutputStream(server.getOutputStream());
        oos.writeObject(data);
        oos.flush();

        // Begin download
        File f = new File(getFileStorePath(cf));
        PrintWriter writer = new PrintWriter(f);
        ObjectInputStream ois = new ObjectInputStream(server.getInputStream());
        String line = (String) ois.readObject();
        while (!line.equals(CloudMessage.EOF)) {
            writer.println(line);
            line = (String) ois.readObject();
        }
        writer.close();
        CloudFile oldCf = fileSystem.get(cf.getName());
        if (oldCf != null && !oldCf.getNameWithVersion().equals(cf.getNameWithVersion())) new File(getFileStorePath(oldCf)).delete();
        if (dirtyFiles.containsKey(cf.getName())) dirtyFiles.remove(cf.getName());
        cf.setLocation(new ServerInfo(null, serverPort));

        fileSystem.put(cf.getName(),cf);
        System.out.println(fileSystem.toString()); // TODO REMOVE
        server.close();
    }

    public void retrieveNewFile(CloudFile cf) throws IOException, ClassNotFoundException{
        retrieveFile(cf);
        broadcastNewFileUpdate(cf);
    }
}
