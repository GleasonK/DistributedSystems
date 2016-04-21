package hw5;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Contains some methods to list files and folders from a directory
 * [Resolved] Look into file metadata for version number storage => (NO) FileSystem dependent.
 * TODO: Load files and return as a CloudFile type, separating at version number
 */

public class FileUtils {
	
    /**
     * List all the files and folders from a directory
     * @param directoryName to be listed
     * @throws java.io.IOException
     */
    public void listFilesAndFolders(String directoryName) throws IOException{
        File directory = new File(directoryName);
        System.out.println("List of Files and folders in: " + directory.getCanonicalPath());
        //get all the files from a directory
        File[] fList = directory.listFiles();
        for (File file : fList){
            System.out.println(file.getName());
        }
    }
    
    /**
     * List all the files under a directory
     * @param directoryName to be listed
     * @throws java.io.IOException
     */
    public void listFiles(String directoryName) throws IOException{
        File directory = new File(directoryName);
        //get all the files from a directory
        System.out.println("List of Files in: " + directory.getCanonicalPath());
        File[] fList = directory.listFiles();
        for (File file : fList){
            if (file.isFile()){
                System.out.println(file.getName());
            }
        }
    }

    public static Map<String, CloudFile> getFileMap(String directoryName, int serverPort) throws IOException{
        File directory = new File(directoryName);
        Map<String,CloudFile> files = new ConcurrentHashMap<>();
        if (!directory.exists()) return files;
            //get all the files from a directory
        System.out.println("List of Files in: " + directory.getCanonicalPath());
        File[] fList = directory.listFiles();
        for (File file : fList){
            if (file.isFile()){
                CloudFile cf = strToCloudFile(file.getName());
                cf.setLocation(new ServerInfo(null, serverPort));
                files.put(cf.getName(),cf);
                System.out.println(cf);
            }
        }
        return files;
    }

    public static CloudFile strToCloudFile(String str) {
        String[] toks = str.split("\\.");
        String fname  = toks[0];
        int version   = 0;
        String ext    = toks.length > 2 ? toks[2] : "";
        try {
            version = toks.length > 1 ? Integer.parseInt(toks[1]) : 0;
        } catch (NumberFormatException e){
            version = 0;
            ext = toks[1];
        }
        String filename = fname + (ext.isEmpty() ? "" : '.'+ext);
        return new CloudFile(filename, version);
    }

    /**
     *List all the folder under a directory
     * @param directoryName to be listed
     * @throws java.io.IOException
     */
    public void listFolders(String directoryName) throws IOException{
        File directory = new File(directoryName);
        //get all the files from a directory
        System.out.println("List of Folders in: " + directory.getCanonicalPath());

        File[] fList = directory.listFiles();
        for (File file : fList){
            if (file.isDirectory()){
                System.out.println(file.getName());
            }
        }
    }

    /**
     * List all files from a directory and its subdirectories
     * @param directoryName to be listed
     * @throws java.io.IOException
     */
    public void listFilesAndFilesSubDirectories(String directoryName) throws IOException{
        File directory = new File(directoryName);
        //get all the files from a directory
        System.out.println("List of Files and file subdirectories in: " + directory.getCanonicalPath());
        File[] fList = directory.listFiles();
        for (File file : fList){
            if (file.isFile()){
                System.out.println(file.getAbsolutePath());
            } else if (file.isDirectory()){
                listFilesAndFilesSubDirectories(file.getAbsolutePath());
            }
        }
    }

    public static void main (String[] args) throws IOException{

        FileUtils listFilesUtil = new FileUtils();
        //play with the path, this is using variations on files in folders for eclipse
        final String directoryLinuxMac =".//";
        //final String directoryLinuxMac =".//src";
        System.out.println("-------");
        listFilesUtil.listFiles(directoryLinuxMac);
        System.out.println("-------");
        listFilesUtil.listFilesAndFilesSubDirectories(directoryLinuxMac);
        System.out.println("-------");
        listFilesUtil.listFolders(directoryLinuxMac);
        System.out.println("-------");
        listFilesUtil.listFilesAndFolders(directoryLinuxMac);
    }

}