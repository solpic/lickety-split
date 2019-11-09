package org.licketysplit.syncmanager.messages;

import org.json.JSONObject;
import org.licketysplit.env.Environment;
import org.licketysplit.securesocket.messages.DefaultHandler;
import org.licketysplit.securesocket.messages.Message;
import org.licketysplit.securesocket.messages.MessageHandler;
import org.licketysplit.securesocket.messages.ReceivedMessage;

import java.io.IOException;

public class UpdateManifestResponse extends Message {
    public JSONObject manifest;

    public byte[] toBytes(){
        return this.manifest.toString().getBytes();
    }

    public void fromBytes(byte[] manifest){
        String manifestStr = new String(manifest);
        this.manifest = new JSONObject(manifestStr);
    }

    public UpdateManifestResponse() {
    }

    public UpdateManifestResponse(JSONObject manifest) {
        this.manifest = manifest;
    }

    @DefaultHandler(type = UpdateManifestResponse.class)
    public static class UpdateManifestResponseHandler implements MessageHandler {
        @Override
        public void handle(ReceivedMessage m) {
            UpdateManifestResponse updateManifestRequest = m.getMessage();
            Environment env = m.getEnv();
            JSONObject otherUserManifest = updateManifestRequest.manifest;
            try {
                env.getFM().syncManifests(otherUserManifest);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}