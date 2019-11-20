package org.licketysplit.securesocket.messages;

import java.util.HashMap;
import java.util.Map;

public class MapMessage extends JSONMessage {
    Map<String, Object> values;

    public Map<String, Object> getValues() {
        return values;
    }

    public Map<String, Object> val() {
        return values;
    }

    public void setValues(Map<String, Object> values) {
        this.values = values;
    }

    public MapMessage() {
        values = new HashMap<>();
    }
}
