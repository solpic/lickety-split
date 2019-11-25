package org.licketysplit.env;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Debugger {
    ConcurrentHashMap<String, Trigger> triggers;
    boolean enabled;
    Debugger(boolean enabled) {
        triggers = new ConcurrentHashMap<>();
        this.enabled = enabled;
    }
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void triggerWithArray(String key, Object[] args) {
        if(enabled) {
            synchronized (triggers) {
                if (triggers.containsKey(key)) {
                    Trigger trigger = triggers.get(key);
                    trigger.trigger(args);
                }
            }
        }
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
        triggerWithArray(key, args);
    }


    public String serializeTrigger(String key, Object[] args) throws Exception{
        ObjectMapper objectMapper = new ObjectMapper();
        List<Object> objects = Arrays.asList(args);
        String argsB64 = Base64.getEncoder().encodeToString(objectMapper.writeValueAsString(objects).getBytes());
        return String.format("TRIGGER %s %s", key, argsB64);
    }

    public void parseTrigger(String line) throws Exception {
        // [datetime] username: TRIGGER triggername triggerargs...
        Matcher matcher = Pattern.compile("\\[[^\\]]*\\] ([^:]*): TRIGGER ([^\\s]*) ([^\\s]*)").matcher(line);
        if(matcher.find()) {
            String username = matcher.group(1);
            String triggerName = matcher.group(2);
            String triggerArgs = matcher.group(3);
            String argsDecoded = new String(Base64.getDecoder().decode(triggerArgs));
            ObjectMapper objectMapper = new ObjectMapper();
            Object[] args = objectMapper.readValue(argsDecoded, Object[].class);
            Object[] argsArray = new Object[args.length+1];
            argsArray[0] = username;
            for (int i = 0; i < args.length; i++) {
                argsArray[i+1] = args[i];
            }
            triggerWithArray(triggerName, argsArray);
        }
        return;
    }

    private static Debugger global = null;
    private static Object globalLock = new Object();
    public static Debugger global() {
        synchronized (globalLock) {
            if(global==null) {
                global = new Debugger(false);
            }
            return global;
        }
    }
}
