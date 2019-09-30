package org.licketysplit.syncmanager;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
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

    public void writeJSONArray(JSONArray arr) throws IOException {
        FileUtils.writeByteArrayToFile(file, arr.toString().getBytes("utf-8"));
    }

    public void writeJSONObject(JSONObject obj) throws IOException {
        FileUtils.writeByteArrayToFile(file, obj.toString().getBytes("utf-8"));
    }
}
