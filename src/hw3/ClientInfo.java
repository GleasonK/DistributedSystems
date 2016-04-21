package hw3;

import java.net.InetAddress;

/**
 * Created by GleasonK on 2/27/16.
 */
public class ClientInfo {
    private InetAddress address;
    private int port;
    public ClientInfo(InetAddress addr, int port){
        this.address = addr;
        this.port = port;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ClientInfo){
            ClientInfo ci = (ClientInfo) obj;
            return ci.port==port && ci.address.equals(address);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return address.toString().hashCode() ^ port;
    }

    @Override
    public String toString() {
        return this.address.toString() + ":" + this.port;
    }
}
