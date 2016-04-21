package hw3;

import java.io.Serializable;

/**
 * Created by GleasonK on 2/27/16.
 * The data that a client sends to the server for a certain type of join.
 * Used to distinguish Chat Room vs Player
 */
public class PlayerData implements Serializable {
    public static final String KIBITZ_TYPE = "kibitz";
    public static final String PLAYER_TYPE = "player";

    private String type;

    public PlayerData(String type){
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
