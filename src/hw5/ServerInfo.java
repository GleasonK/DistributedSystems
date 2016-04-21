package hw5;

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
    public boolean equals(Object obj) {
        if (obj instanceof ServerInfo){
            ServerInfo ci = (ServerInfo) obj;
            return ci.port==port && ci.ip.equals(ip);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return ip.hashCode() ^ port;
    }

    @Override
    public String toString() {
        return String.format("Server{%s:%d}",ip,port);
    }
}
