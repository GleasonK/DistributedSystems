package hw6;

import java.io.Serializable;

/**
 * Created by GleasonK on 4/12/16.
 */
public class CloudFile implements Serializable{
    private String filename;
    private int version;
    private ServerInfo location;

    public CloudFile(String filename, int version){
        this.filename=filename;
        this.version=version;
        this.location = null;
    }

    public CloudFile(String filename, int version, ServerInfo location){
        this.filename=filename;
        this.version=version;
        this.location = location;
    }

    public String getNameWithVersion(){
        String[] fext = filename.split("\\.");
        return fext[0]+ '.' + version + ((fext.length>1) ? '.'+fext[1] : "");
    }

    public String getName() {
        return filename;
    }

    public int getVersion() {
        return version;
    }

    public void setLocation(ServerInfo location) {
        this.location = location;
    }

    public ServerInfo getLocation() {
        return location;
    }

    // Return new incremented CloudFile
    public static CloudFile incrementedVersion(CloudFile cf){
        return new CloudFile(cf.getName(),cf.getVersion()+1);
    }

    @Override
    public int hashCode() {
        return filename.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CloudFile){
            CloudFile cf = (CloudFile) obj;
            return cf.filename.equals(filename);
        }
        return false;
    }

    @Override
    public String toString() {
        if (location!= null)
            return String.format("{File: %s, v%d, %s}",filename,version,location.toString());
        return String.format("{File: %s, v%d}",filename,version);
    }
}
