package org.licketysplit.syncmanager;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class JsonToFile {
    private File file;

    public JsonToFile(File file){
        this.file = file;
    }

    public JSONArray getJSONArray() throws IOException {
        return  new JSONArray(FileUtils.readFileToString(file, StandardCharsets.UTF_8));
    }

    public JSONObject getJSONObject() throws IOException {
        return new JSONObject(FileUtils.readFileToString(file, StandardCharsets.UTF_8));
    }

    public void writeJSONArray(JSONArray arr) {
        try {
            FileWriter writer = new FileWriter(file);
            writer.write(arr.toString());
            writer.flush();
        } catch( IOException e){
            e.printStackTrace();
        }
    }

    public void writeJSONObject(JSONObject obj) {
        try {
            FileWriter writer = new FileWriter(file);
            writer.write(obj.toString());
            writer.flush();
        } catch( IOException e){
            e.printStackTrace();
        }
    }
}