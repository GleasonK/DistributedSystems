package hw5;

/**
 * Created by GleasonK on 4/10/16.
 */
public class Config {
    public static final String REMOTE_MANAGER_IP_1 = "localhost";
    public static final int REMOTE_MANAGER_PORT_1  = 60010;

    public static final String REMOTE_MANAGER_IP_2 = "localhost";
    public static final int REMOTE_MANAGER_PORT_2  = 60011;

    public static final String REMOTE_MANAGER_IP_3 = "localhost";
    public static final int REMOTE_MANAGER_PORT_3  = 60012;

    public static final long DIRTY_UPDATE_INTERVAL = 15  * 1000;
    public static final long FILE_REQUEST_TIMEOUT  = 8  * 1000;
    public static final long HEARTBEAT_INTERVAL    = 15 * 1000; // 3 second interval
    public static final int  HEARTBEAT_TIMEOUT     = 5;         // 3 second timeout

    public static final String FILE_STORE_PATH = "data";

    public static final String MULTICAST_IP_1 = "231.0.8.10";
    public static final int  MULTICAST_PORT_1 = 8001;

    public static final String MULTICAST_IP_2 = "231.0.8.11";
    public static final int  MULTICAST_PORT_2 = 8002;

    public static final String MULTICAST_IP_3 = "231.0.8.12";
    public static final int  MULTICAST_PORT_3 = 8032;

    public static final int SERVER_PORT_1 = 61001;
    public static final int SERVER_PORT_2 = 61002;
    public static final int SERVER_PORT_3 = 61003;
    public static final int SERVER_PORT_4 = 61004;
    public static final int SERVER_PORT_5 = 61005;
    public static final int SERVER_PORT_6 = 61006;
    public static final int SERVER_PORT_7 = 61007;
    public static final int SERVER_PORT_8 = 61008;
    public static final int SERVER_PORT_9 = 61009;
}
