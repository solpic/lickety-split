package org.licketysplit.syncmanager.messages;

import org.json.JSONObject;
import org.licketysplit.env.Environment;
import org.licketysplit.securesocket.messages.*;

import java.io.IOException;
import java.util.logging.Level;

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
                String theirUsername = "unknown";
                try {
                    theirUsername = m.getConn().getPeerAddress().getUser().getUsername();
                }catch(Exception e) {}
                boolean changed = env.getFM().syncManifests(theirManifest, theirUsername);
                m.getConn().sendFirstMessage(new UpdateManifestResponse(yourManifest), null);

                if(changed) {
                    env.log("Changes, propagating manifest");
                    env.getSyncManager().syncManifests();
                }else{
                    env.log("No manifest changes");
                }
            } catch (Exception e) {
                env.getLogger().log(Level.INFO, "Error during manifest sync", e);
            }
        }
    }
}