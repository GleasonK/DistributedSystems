package hw6;

import java.io.IOException;

/**
 * Created by GleasonK on 5/15/16.
 */
public class Group1Servers {
    public static void main(String[] args) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    new CloudServer(
                            Config.SERVER_PORT_1,
                            Config.REMOTE_MANAGER_IP_1,
                            Config.REMOTE_MANAGER_PORT_1,
                            Config.S_MULTICAST_IP_1,
                            hw5.Config.MULTICAST_PORT_1
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
//                            Config.SERVER_PORT_2,
//                            Config.REMOTE_MANAGER_IP_1,
//                            Config.REMOTE_MANAGER_PORT_1,
//                            Config.S_MULTICAST_IP_1,
//                            Config.S_MULTICAST_PORT_1
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
//                            Config.SERVER_PORT_3,
//                            Config.REMOTE_MANAGER_IP_1,
//                            Config.REMOTE_MANAGER_PORT_1,
//                            Config.S_MULTICAST_IP_1,
//                            hw5.Config.MULTICAST_PORT_1
//                    ).runServer();
//                } catch (IOException | ClassNotFoundException e) {
//                    e.printStackTrace();
//                }
//            }
//        }).start();
    }
}
