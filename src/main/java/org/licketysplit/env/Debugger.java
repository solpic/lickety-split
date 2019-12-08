package org.licketysplit.env;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is a debugging helper for the software.
 */
public class Debugger {
    /**
     * The map of keys to callback functions.
     */
    ConcurrentHashMap<String, Trigger> triggers;
    /**
     * True if enabled.
     */
    boolean enabled;

    /**
     * Instantiates a new Debugger.
     *
     * @param enabled the enabled
     */
    Debugger(boolean enabled) {
        triggers = new ConcurrentHashMap<>();
        this.enabled = enabled;
    }

    /**
     * Sets enabled.
     *
     * @param enabled the enabled
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Call a callback function with args.
     *
     * @param key  the key of the callback
     * @param args the args
     */
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

    /**
     * The interface Trigger.
     */
    public interface Trigger {
        /**
         * Trigger.
         *
         * @param args the args
         */
        void trigger(Object ...args);
    }

    /**
     * Sets trigger.
     *
     * @param key     the key
     * @param trigger the trigger
     */
    public void setTrigger(String key, Trigger trigger) {
        synchronized (triggers) {
            triggers.put(key, trigger);
        }
    }

    /**
     * Trigger.
     *
     * @param key  the key
     * @param args the args
     */
    public void trigger(String key, Object ...args) {
        triggerWithArray(key, args);
    }


    /**
     * Serialize trigger string. This allows us to trigger callbacks over
     * the network, since we just log the trigger string and it is
     * triggered on the remote host.
     *
     * @param key  the key
     * @param args the args
     * @return the string
     * @throws Exception the exception
     */
    public String serializeTrigger(String key, Object[] args) throws Exception{
        ObjectMapper objectMapper = new ObjectMapper();
        List<Object> objects = Arrays.asList(args);
        String argsB64 = Base64.getEncoder().encodeToString(objectMapper.writeValueAsString(objects).getBytes());
        return String.format("TRIGGER %s %s", key, argsB64);
    }

    /**
     * Parse trigger string. This will allow us to call the appropriate trigger
     * on the remote host. If this is not a trigger string it simply returns.
     *
     * @param line the line
     * @throws Exception the exception
     */
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

    /**
     * Global debugger.
     *
     * @return the debugger
     */
    public static Debugger global() {
        synchronized (globalLock) {
            if(global==null) {
                global = new Debugger(false);
            }
            return global;
        }
    }
}
