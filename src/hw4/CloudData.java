package hw4;

import java.io.Serializable;

/**
 * Created by GleasonK on 3/21/16.
 */
public class CloudData implements Serializable {
    public static final String CMD_DOWNLOAD = "download";
    public static final String CMD_UPLOAD   = "upload";

    public String command;
    public String filename;

    public CloudData(String command, String filename){
        this.command  = command;
        this.filename = filename;
    }

    @Override
    public String toString() {
        return this.command + " - " + this.filename;
    }
}
