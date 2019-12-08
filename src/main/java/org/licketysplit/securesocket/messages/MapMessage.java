package org.licketysplit.securesocket.messages;

import java.util.HashMap;
import java.util.Map;

/**
 * A helper message type that simply contains a Map object.
 * This is used during handshaking, when there is no benefit
 * to defining specifically typed messages.
 */
public class MapMessage extends JSONMessage {
    /**
     * The key value pairs.
     */
    Map<String, Object> values;

    /**
     * Gets values.
     *
     * @return the values
     */
    public Map<String, Object> getValues() {
        return values;
    }

    /**
     * Gets values.
     *
     * @return the value map
     */
    public Map<String, Object> val() {
        return values;
    }

    /**
     * Sets values map.
     *
     * @param values the values
     */
    public void setValues(Map<String, Object> values) {
        this.values = values;
    }

    /**
     * Instantiates a new Map message.
     */
    public MapMessage() {
        values = new HashMap<>();
    }
}
