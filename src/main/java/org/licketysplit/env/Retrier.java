package org.licketysplit.env;

import java.util.HashMap;
import java.util.Map;

public class Retrier {
    int[] waitTimes;
    int attemptResetTime;
    public Retrier(int[] waitTimes, int attemptResetTime) {
        this.waitTimes = waitTimes;
        this.processes = new HashMap<>();
        this.attemptResetTime = attemptResetTime;
    }

    public Environment env;
    public static class RetryInfo {
        int attempts = 0;
        long lastKnownAttemptTime = -1;
    }
    Map<String, RetryInfo> processes;

    public void success(String key) {
        synchronized (processes) {
            if(processes.containsKey(key)) processes.remove(key);
        }
    }

    public boolean tryOrRetry(String key, boolean firstTry) throws Exception {
        // failureObject lets us make sure the original caller finished, and not a different caller
        RetryInfo retryInfo;
        synchronized (processes) {
            if(processes.containsKey(key)) {
                if(firstTry) {
                    env.log(String.format("Retrier failed for '%s', claiming first try", key));
                    return false;
                }
                retryInfo = processes.get(key);
                retryInfo.lastKnownAttemptTime = System.currentTimeMillis();
                retryInfo.attempts++;
            }else{
                retryInfo = new RetryInfo();
                processes.put(key, retryInfo);
            }
        }
        synchronized (retryInfo) {
            if(retryInfo.attempts<waitTimes.length) {
                long waitFor = waitTimes[retryInfo.attempts]+retryInfo.lastKnownAttemptTime-System.currentTimeMillis();
                env.log(String.format("Will retry attempt %d '%s' in %d milliseconds", retryInfo.attempts, key, waitFor));
                if(waitFor>0) {
                    Thread.sleep(waitFor);
                }
                retryInfo.lastKnownAttemptTime = System.currentTimeMillis();
                return true;
            }else{
                if(System.currentTimeMillis()>retryInfo.lastKnownAttemptTime+attemptResetTime) {
                    env.log(String.format("Retrying '%s' after resetting attempt count", key));
                    retryInfo.attempts = 0;
                    retryInfo.lastKnownAttemptTime = System.currentTimeMillis();
                    return true;
                }else{
                    env.log(String.format("Won't allow retry of '%s' due to too many attempts", key));
                    return false;
                }
            }
        }
    }

}
