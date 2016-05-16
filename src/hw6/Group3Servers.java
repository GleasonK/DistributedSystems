package hw6;

import java.io.IOException;

/**
 * Created by GleasonK on 5/15/16.
 */
public class Group3Servers {
    public static void main(String[] args) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    new CloudServer(
                            Config.SERVER_PORT_7,
                            Config.REMOTE_MANAGER_IP_3,
                            Config.REMOTE_MANAGER_PORT_3,
                            Config.S_MULTICAST_IP_3,
                            Config.S_MULTICAST_PORT_3
                    ).runServer();
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        try {Thread.sleep(2400);} catch (InterruptedException e){e.printStackTrace();}

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    new CloudServer(
                            Config.SERVER_PORT_8,
                            Config.REMOTE_MANAGER_IP_3,
                            Config.REMOTE_MANAGER_PORT_3,
                            Config.S_MULTICAST_IP_3,
                            Config.S_MULTICAST_PORT_3
                    ).runServer();
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        try {Thread.sleep(2300);} catch (InterruptedException e){e.printStackTrace();}

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    new CloudServer(
                            Config.SERVER_PORT_9,
                            Config.REMOTE_MANAGER_IP_3,
                            Config.REMOTE_MANAGER_PORT_3,
                            Config.S_MULTICAST_IP_3,
                            Config.S_MULTICAST_PORT_3
                    ).runServer();
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
