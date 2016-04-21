package hw5;

/**
 * Created by GleasonK on 4/17/16.
 */
public class HomeworkTest {

    public static void startRemoteManager(final int port) throws Exception{
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    new RemoteManager(port).runRemoteManager();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static void startCloudServer(final int port, final String rmIP, final int rmPort) throws Exception{
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    new CloudServer(port,rmIP,rmPort).runServer();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static void main(String[] args) throws Exception {
        startRemoteManager(Config.REMOTE_MANAGER_PORT_1);
        startRemoteManager(Config.REMOTE_MANAGER_PORT_2);
        startRemoteManager(Config.REMOTE_MANAGER_PORT_3);

        startCloudServer(Config.SERVER_PORT_1, Config.REMOTE_MANAGER_IP_1, Config.REMOTE_MANAGER_PORT_1);
        Thread.sleep(500);
        startCloudServer(Config.SERVER_PORT_4, Config.REMOTE_MANAGER_IP_2, Config.REMOTE_MANAGER_PORT_2);
        startCloudServer(Config.SERVER_PORT_7, Config.REMOTE_MANAGER_IP_3, Config.REMOTE_MANAGER_PORT_3);
        Thread.sleep(500);
        startCloudServer(Config.SERVER_PORT_2, Config.REMOTE_MANAGER_IP_1, Config.REMOTE_MANAGER_PORT_1);
        startCloudServer(Config.SERVER_PORT_5, Config.REMOTE_MANAGER_IP_2, Config.REMOTE_MANAGER_PORT_2);
        startCloudServer(Config.SERVER_PORT_8, Config.REMOTE_MANAGER_IP_3, Config.REMOTE_MANAGER_PORT_3);
        Thread.sleep(500);
        startCloudServer(Config.SERVER_PORT_3, Config.REMOTE_MANAGER_IP_1, Config.REMOTE_MANAGER_PORT_1);
        startCloudServer(Config.SERVER_PORT_6, Config.REMOTE_MANAGER_IP_2, Config.REMOTE_MANAGER_PORT_2);
        startCloudServer(Config.SERVER_PORT_9, Config.REMOTE_MANAGER_IP_3, Config.REMOTE_MANAGER_PORT_3);
    }

}
