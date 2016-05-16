package hw6;

/**
 * Created by GleasonK on 5/15/16.
 */
public class Group1RM {

    public static void main(String[] args) throws Exception{
        RemoteManager rm = new RemoteManager(Config.REMOTE_MANAGER_PORT_1, Config.RM_MULTICAST_IP, Config.RM_MULTICAST_PORT);
        rm.runRemoteManager();

    }
}
