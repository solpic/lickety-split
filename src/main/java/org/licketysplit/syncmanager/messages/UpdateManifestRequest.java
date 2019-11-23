package org.licketysplit.syncmanager.messages;

import org.json.JSONObject;
import org.licketysplit.env.Environment;
import org.licketysplit.securesocket.messages.*;

import java.io.IOException;

public class UpdateManifestRequest extends Message {
    public JSONObject manifest;

    public byte[] toBytes(){
        return this.manifest.toString().getBytes();
    }

    public void fromBytes(byte[] manifest){
        String manifestStr = new String(manifest);
        this.manifest = new JSONObject(manifestStr);
    }

    public UpdateManifestRequest() {
    }

    public UpdateManifestRequest(JSONObject manifest) {
        this.manifest = manifest;
    }

    @DefaultHandler(type = UpdateManifestRequest.class)
    public static class UpdateManifestRequestHandler implements MessageHandler {
        @Override
        public void handle(ReceivedMessage m) {
            UpdateManifestRequest updateManifestRequest = m.getMessage();
            Environment env = m.getEnv();
            JSONObject theirManifest = updateManifestRequest.manifest;
            try {
                JSONObject yourManifest = env.getFM().getManifest();
                env.getFM().syncManifests(theirManifest);
                m.respond(new UpdateManifestResponse(yourManifest), null);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}