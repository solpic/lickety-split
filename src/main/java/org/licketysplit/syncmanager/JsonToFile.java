package org.licketysplit.syncmanager;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Class to read and write JSONObjects
 * from files.
 */
public class JsonToFile {
    /**
     * File to read and write from.
     */
    private File file;

    /**
     * Instantiates a new Json to file.
     *
     * @param file the file
     */
    public  JsonToFile(File file){
        this.file = file;
    }

    /**
     * Deserializes JSONObject from the file.
     *
     * @return the deserialized JSONObject
     * @throws IOException the io exception
     */
    public JSONObject getJSONObject() throws IOException {
        return new JSONObject(FileUtils.readFileToString(file, StandardCharsets.UTF_8));
    }

    /**
     * Serializes a JSONObject and writes it to the file.
     *
     * @param obj the JSONObject to serialize
     */
    public void writeJSONObject(JSONObject obj) {
        try {
            FileWriter writer = new FileWriter(file);
            writer.write(obj.toString());
            writer.flush();
            writer.close();
        } catch( IOException e){
            e.printStackTrace();
        }
    }
}
