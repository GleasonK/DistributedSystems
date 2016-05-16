package hw6;

import java.io.Serializable;

/**
 * Created by GleasonK on 3/21/16.
 */
public class CloudMessage implements Serializable {
    public static final String CMD_DOWNLOAD  = "download";
    public static final String CMD_UPLOAD    = "upload";
    public static final String CMD_HEARTBEAT = "heartbeat";
    public static final String CMD_SERVER_UP = "server";
    public static final String CMD_UPDATE    = "update";
    public static final String CMD_REQUEST   = "request";
    public static final String CMD_RESPONSE  = "response";
    public static final String CMD_NEW_FILE  = "new_file";
    public static final String CMD_REQUEST_ALL= "request_all";
    public static final String PING = "ping";
    public static final String ACK  = "ack";
    public static final String EOF  = "EOF";

    public String type;
    public Object data;

    public CloudMessage(String type, Object data){
        this.type  = type;
        this.data  = data;
    }

    @Override
    public String toString() {
        return this.type + " - " + this.data;
    }
}
