package org.licketysplit.syncmanager;

import java.io.File;
import java.io.*;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SequenceWriter;
import org.apache.commons.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileManager {

    public FileManager(){}

    //INITIALIZATION

    public void initializeFiles(String directoryLocation, String username){
        Path path = Paths.get(System.getProperty("user.home"), "LicketySplit");
        boolean directoryCreated = new File(path.toString()).mkdirs();
        if(!directoryCreated) { // If directory created then don't initialize, mostly for testing right now
            return;
        }
        this.initializeConfigs(directoryLocation, username);
        this.initializeManifest();

    }

    private void initializeConfigs(String directoryLocation, String username){
        //Create's configs file and fills it with configs JSON object {sharedDirectory, username}
        ObjectMapper mapper = new ObjectMapper();
        File configs = new File(this.getConfigsPath( ".configs.txt"));

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
        File file = new File(this.getConfigsPath( ".manifest.txt"));
    }

    //GETTERS

    private String getConfigsPath(String fileName){
        Path path = Paths.get(System.getProperty("user.home"), "LicketySplit", fileName);
        return path.toString();
    }

    public String getSharedDirectoryPath(){
        ObjectMapper mapper = new ObjectMapper();

        try{
            ConfigsInfo configs = mapper.readValue(new File(this.getConfigsPath( ".configs.txt")), ConfigsInfo.class);
            return configs.getSharedDirectory();
        } catch(IOException e){
            e.printStackTrace();
        }
        return "";
    }

    public String getUsername(){
        ObjectMapper mapper = new ObjectMapper();

        try{
            ConfigsInfo configs = mapper.readValue(new File(this.getConfigsPath( ".manifest.txt")), ConfigsInfo.class);
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
        File dest = new File(this.getSharedDirectoryPath() + source.getName());

        try {
            if(dest.length() == 0) { //If first file added
                FileWriter fileWriter = new FileWriter(new File(this.getConfigsPath(".manifest.txt")), true);
                SequenceWriter seqWriter = mapper.writer().writeValuesAsArray(fileWriter);
                this.addFileHelper(source, dest);
                seqWriter.write(new FileInfo(dest));
                seqWriter.close();
            } else { //If preexisting files
                JsonGenerator g = mapper.getFactory().createGenerator(new FileOutputStream(".manifest.txt"));
                mapper.writeValue(g, new FileInfo(source));
                g.close();
            }
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
