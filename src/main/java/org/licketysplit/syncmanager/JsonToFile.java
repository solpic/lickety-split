package org.licketysplit.syncmanager;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class JsonToFile {
    private File file;

    public  JsonToFile(File file){
        this.file = file;
    }

    public JSONObject getJSONObject() throws IOException {
        return new JSONObject(FileUtils.readFileToString(file, StandardCharsets.UTF_8));
    }

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
