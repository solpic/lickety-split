package org.licketysplit.testing;

public class WaitUntil {
    public WaitUntil() {
        stopTimeLock = new Object();
        stopTime = -1;
    }

    long stopTime;
    Object stopTimeLock;
    public void waitUntil(long initial) throws Exception{
        synchronized (stopTimeLock) {
            stopTime = System.currentTimeMillis() + initial;
        }

        boolean done = false;
        do{
            long delta = stopTime-System.currentTimeMillis();
            if(delta>0) {
                Thread.sleep(delta);
                if(System.currentTimeMillis()>stopTime) done = true;
            }else{
                done = true;
            }
        } while(!done);
    }

    public void extend(long time) {
        synchronized (stopTimeLock) {
            if(stopTime>=0) {
                stopTime = System.currentTimeMillis()+time;
            }
        }
    }
}
