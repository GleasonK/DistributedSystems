package hw4;

import java.io.Serializable;

/**
 * Created by GleasonK on 3/21/16.
 */
public class ServerInfo implements Serializable{
    public String ip;
    public int port;

    public ServerInfo(String ip, int port){
        this.ip=ip;
        this.port=port;
    }

    @Override
    public String toString() {
        return String.format("Server{%s:%d}",ip,port);
    }
}
