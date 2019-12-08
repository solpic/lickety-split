package org.licketysplit.syncmanager.messages;

import org.json.JSONObject;
import org.licketysplit.env.Environment;
import org.licketysplit.securesocket.messages.*;

import java.io.IOException;
import java.util.logging.Level;

/**
 * Requests a copy of a peer's manifest for syncing.
 */
public class UpdateManifestRequest extends Message {
    /**
     * Our manifest.
     */
    public JSONObject manifest;

    /**
     * Serialization function
     * @return serialized message
     */
    public byte[] toBytes(){
        return this.manifest.toString().getBytes();
    }

    /**
     * Deserialization function
     * @param manifest the serialized message
     */
    public void fromBytes(byte[] manifest){
        String manifestStr = new String(manifest);
        this.manifest = new JSONObject(manifestStr);
    }

    /**
     * Instantiates a new Update manifest request.
     */
    public UpdateManifestRequest() {
    }

    /**
     * Instantiates a new Update manifest request.
     *
     * @param manifest the manifest
     */
    public UpdateManifestRequest(JSONObject manifest) {
        this.manifest = manifest;
    }

    /**
     * Default handler for the UpdateManifestRequest.
     */
    @DefaultHandler(type = UpdateManifestRequest.class)
    public static class UpdateManifestRequestHandler implements MessageHandler {
        /**
         * Called when the message is received,
         * syncs their manifest with our manifest,
         * then sends them a copy of our manifest.
         *
         * If the manifests were different, it then sends
         * out UpdateManifestRequests to all connected peers
         * to propagate whatever changes it received.
         * @param m the received message
         */
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

                // Sync the manifest
                boolean changed = env.getFM().syncManifests(theirManifest, theirUsername);
                // Send them a copy of our manifest
                m.getConn().sendFirstMessage(new UpdateManifestResponse(yourManifest), null);

                // If there were changes, propagate the changes to all peers
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