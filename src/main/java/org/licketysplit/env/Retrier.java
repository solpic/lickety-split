package org.licketysplit.env;

import java.util.HashMap;
import java.util.Map;

public class Retrier {
    int[] waitTimes;
    int attemptResetTime;
    public Retrier(int[] waitTimes, int retryCount, int attemptResetTime) {
        this.waitTimes = waitTimes;
        this.processes = new HashMap<>();
        this.attemptResetTime = attemptResetTime;
    }

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
                if(firstTry) return false;
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
                if(waitFor>0) {
                    Thread.sleep(waitFor);
                }
                retryInfo.lastKnownAttemptTime = System.currentTimeMillis();
                return true;
            }else{
                if(System.currentTimeMillis()>retryInfo.lastKnownAttemptTime+attemptResetTime) {
                    retryInfo.attempts = 0;
                    retryInfo.lastKnownAttemptTime = System.currentTimeMillis();
                    return true;
                }else{
                    return false;
                }
            }
        }
    }

}
