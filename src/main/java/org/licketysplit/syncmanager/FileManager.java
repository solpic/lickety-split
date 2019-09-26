package org.licketysplit.syncmanager;

import java.io.File;
import java.io.*;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.*;

public class FileManager {

    public FileManager(){}

    public void initializeFiles(String directoryLocation, String username){
        boolean directoryCreated = new File(System.getProperty("user.home") + "/LicketySplit").mkdirs();
        if(!directoryCreated) { // If directory created then don't initialize, mostly for testing right now
            return;
        }
        this.initializeConfigs(directoryLocation, username);
        this.initializeManifest();

    }

    private void initializeConfigs(String directoryLocation, String username){
        ObjectMapper mapper = new ObjectMapper();
        File configs = new File(this.getConfigsDirectory() + ".configs.txt");
        try {
            FileOutputStream outputStream = new FileOutputStream(configs);
            JsonGenerator generator = mapper.getFactory().createGenerator(outputStream);
            mapper.writeValue(generator, new ConfigsInfo(username, directoryLocation));
            generator.close();
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    private void initializeManifest(){
        File file = new File(this.getConfigsDirectory() + ".manifest.txt");
    }

    private String getConfigsDirectory(){
        return System.getProperty("user.home") + "/LicketySplit/";
    }

    public String getSharedDirectory(){
        ObjectMapper mapper = new ObjectMapper();
        try{
            ConfigsInfo configs = mapper.readValue(new File(this.getConfigsDirectory() + ".configs.txt"), ConfigsInfo.class);
            return configs.getSharedDirectory();
        } catch(IOException e){
            e.printStackTrace();
        }
        return "";
    }

    public String getUsername(){
        ObjectMapper mapper = new ObjectMapper();
        try{
            ConfigsInfo configs = mapper.readValue(new File(this.getConfigsDirectory() + ".manifest.txt"), ConfigsInfo.class);
            return configs.getUsername();
        } catch(IOException e){
            e.printStackTrace();
        }
        return "";
    }

    //Adds file to manifest and folder
    public void addFile(String fileLocation){
        ObjectMapper mapper = new ObjectMapper();
        File source = new File(fileLocation);
        File dest = new File(this.getSharedDirectory() + source.getName());
        try {
            FileOutputStream outputStream = new FileOutputStream(new File(this.getConfigsDirectory() + ".manifest.txt"));
            JsonGenerator generator = mapper.getFactory().createGenerator(outputStream);
            this.addFileHelper(source, dest);
            mapper.writeValue(generator, new FileInfo(dest));
            generator.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void addFileHelper(File source, File dest) throws IOException {
        FileUtils.copyFile(source, dest);
    }

    public static void UpdateFileInManifest(String fileName){

    }

}
