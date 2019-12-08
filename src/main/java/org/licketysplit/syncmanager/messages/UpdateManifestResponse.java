package org.licketysplit.syncmanager.messages;

import org.json.JSONObject;
import org.licketysplit.env.Environment;
import org.licketysplit.securesocket.messages.DefaultHandler;
import org.licketysplit.securesocket.messages.Message;
import org.licketysplit.securesocket.messages.MessageHandler;
import org.licketysplit.securesocket.messages.ReceivedMessage;

import java.io.IOException;
import java.util.logging.Level;

/**
 * Response to an UpdateManifestRequest, contains a copy of the senders manifest.
 */
public class UpdateManifestResponse extends Message {
    /**
     * The Manifest.
     */
    public JSONObject manifest;

    /**
     * Serializes the message.
     * @return serialized message
     */
    public byte[] toBytes(){
        return this.manifest.toString().getBytes();
    }

    /**
     * Deserializes the message.
     * @param manifest the serialized message
     */
    public void fromBytes(byte[] manifest){
        String manifestStr = new String(manifest);
        this.manifest = new JSONObject(manifestStr);
    }

    /**
     * Instantiates a new Update manifest response.
     */
    public UpdateManifestResponse() {
    }

    /**
     * Instantiates a new Update manifest response.
     *
     * @param manifest the manifest
     */
    public UpdateManifestResponse(JSONObject manifest) {
        this.manifest = manifest;
    }

    /**
     * DefaultHandler for the UpdateManifestResponse
     */
    @DefaultHandler(type = UpdateManifestResponse.class)
    public static class UpdateManifestResponseHandler implements MessageHandler {
        @Override
        public void handle(ReceivedMessage m) {
            UpdateManifestResponse updateManifestRequest = m.getMessage();
            Environment env = m.getEnv();

            // Gets their manifest from the message
            JSONObject otherUserManifest = updateManifestRequest.manifest;
            try {
                // Syncs our manifest using their manifest
                boolean changed = env.getFM().syncManifests(otherUserManifest, m.getConn().getPeerAddress().getUser().getUsername());
                // If changes were made, propagate the changes
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