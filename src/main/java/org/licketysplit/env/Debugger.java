package org.licketysplit.env;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Debugger {
    ConcurrentHashMap<String, Trigger> triggers;
    boolean enabled;
    Debugger(boolean enabled) {
        triggers = new ConcurrentHashMap<>();
        this.enabled = enabled;
    }
    public interface Trigger {
        void trigger(Object ...args);
    }
    public void setTrigger(String key, Trigger trigger) {
        synchronized (triggers) {
            triggers.put(key, trigger);
        }
    }
    public void trigger(String key, Object ...args) {
        if(enabled) {
            synchronized (triggers) {
                if (triggers.containsKey(key)) {
                    Trigger trigger = triggers.get(key);
                    trigger.trigger(args);
                }
            }
        }
    }
}
