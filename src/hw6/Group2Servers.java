package hw6;

import java.io.IOException;

/**
 * Created by GleasonK on 5/15/16.
 */
public class Group2Servers {
    public static void main(String[] args) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    new CloudServer(
                            Config.SERVER_PORT_4,
                            Config.REMOTE_MANAGER_IP_2,
                            Config.REMOTE_MANAGER_PORT_2,
                            Config.S_MULTICAST_IP_2,
                            Config.S_MULTICAST_PORT_2
                    ).runServer();
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }).start();

//        try {Thread.sleep(1000);} catch (InterruptedException e){e.printStackTrace();}
//
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    new CloudServer(
//                            Config.SERVER_PORT_5,
//                            Config.REMOTE_MANAGER_IP_2,
//                            Config.REMOTE_MANAGER_PORT_2,
//                            Config.S_MULTICAST_IP_2,
//                            Config.S_MULTICAST_PORT_2
//                    ).runServer();
//                } catch (IOException | ClassNotFoundException e) {
//                    e.printStackTrace();
//                }
//            }
//        }).start();
//
//        try {Thread.sleep(1000);} catch (InterruptedException e){e.printStackTrace();}
//
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    new CloudServer(
//                            Config.SERVER_PORT_6,
//                            Config.REMOTE_MANAGER_IP_2,
//                            Config.REMOTE_MANAGER_PORT_2,
//                            Config.S_MULTICAST_IP_2,
//                            Config.S_MULTICAST_PORT_2
//                    ).runServer();
//                } catch (IOException | ClassNotFoundException e) {
//                    e.printStackTrace();
//                }
//            }
//        }).start();
    }
}
