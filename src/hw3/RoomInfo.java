package hw3;

import java.io.Serializable;

/**
 * Created by GleasonK on 2/28/16.
 */
public class RoomInfo implements Serializable{
    String mcip, kibip;
    int mcPort, kibPort;

    PlayerData player1;
    PlayerData player2;

    public RoomInfo(String mcip, int mcPort, String kibip, int kibPort) {
        this.mcip = mcip;
        this.mcPort = mcPort;
        this.kibip=kibip;
        this.kibPort=kibPort;
    }

    public void setPlayer1(PlayerData player1) {
        this.player1 = player1;
    }

    public void setPlayer2(PlayerData player2) {
        this.player2 = player2;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof RoomInfo){
            RoomInfo ci = (RoomInfo) obj;
            return ci.mcPort==mcPort && ci.mcip.equals(mcip);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return mcip.hashCode() ^ mcPort;
    }

    @Override
    public String toString() {
        return "Game@" + this.mcip + ":" + this.mcPort;
    }
}
