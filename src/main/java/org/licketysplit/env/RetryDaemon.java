//package org.licketysplit.env;
//
//import java.util.Collections;
//import java.util.HashMap;
//import java.util.LinkedList;
//import java.util.Map;
//import java.util.concurrent.BlockingQueue;
//import java.util.concurrent.atomic.AtomicBoolean;
//
//public class RetryDaemon {
//    int[] waitTimes;
//    boolean loop;
//    Map<String, RetryInfo> retries;
//    Environment env;
//
//
//    class NextAttempt {
//        Attempt runner;
//        long startAt;
//        String key;
//
//        void start() {
//            RetryInfo r = null;
//            synchronized (retries) {
//                r = retries.get(key);
//                synchronized (r) {
//                    r.count++;
//                }
//            }
//            run(key, r.runner);
//        }
//    }
//
//    void fail(String key) throws Exception {
//        NextAttempt next = null;
//        synchronized (retries) {
//            if(!retries.containsKey(key)) {
//                throw new Exception(String.format("Tried to fail on unknown retry '%s'", key));
//            }else{
//                RetryInfo r = retries.get(key);
//                if(r.count>waitTimes.length&&!loop) {
//                    retries.remove(key);
//                    return;
//                }
//                next = new NextAttempt();
//                next.startAt = System.currentTimeMillis()+waitTimes[r.count%waitTimes.length];
//                next.key = key;
//                next.runner = r.runner;
//            }
//        }
//        synchronized (pending) {
//            if(pending.stream().filter(e->e.key.equals(key)).count()==0)
//                pending.add(next);
//        }
//    }
//
//    void success(String key) {
//        synchronized (retries) {
//            if(retries.containsKey(key)) {
//                retries.remove(key);
//            }
//        }
//    }
//
//    void run(String key, Attempt runner) {
//        runner.run(() -> { success(key)}, () -> {fail(key)});
//    }
//
//    LinkedList<NextAttempt> pending;
//    AtomicBoolean running;
//
//
//    void startThread() {
//        new Thread(() -> {
//            do {
//                try {
//                    NextAttempt next = null;
//                    synchronized (pending) {
//                        Collections.sort(pending, (a, b) -> {
//                            return (int)(a.startAt-b.startAt);
//                        });
//                        if(pending.size()>0)
//                            next = pending.remove();
//                    }
//                    if(next!=null) {
//                        long diff = next.startAt - System.currentTimeMillis();
//                        if(diff>0) {
//                            Thread.sleep(diff);
//                        }
//                        next.start();
//                    }else{
//                        Thread.sleep(2000);
//                    }
//
//                }catch (Exception e) {
//                    env.log("Retry error", e);
//                }
//            } while(running.get());
//        }).start();
//    }
//
//
//    public void retry(String key, Attempt runner) {
//        NextAttempt next = null;
//        synchronized (retries) {
//            if(!retries.containsKey(key)) {
//                RetryInfo r = new RetryInfo();
//                r.runner = runner;
//                r.count = 0;
//                next = new NextAttempt();
//                next.key = key;
//                next.startAt = System.currentTimeMillis();
//            }else{
//                return;
//            }
//        }
//
//        synchronized (pending) {
//            if(pending.stream().filter(e->e.key.equals(key)).count()==0)
//                pending.add(next);
//        }
//    }
//
//    /*
//    Retries should trigger automatically
//    If not in map, retry starts loop
//    Success ends loop and removes from map
//    Running out of attempts removes from loop and map as well
//
//    Retry triggers threader if not in map
//    Events are failures or success
//    Both lead into blockingqueue
//    StartEvent, FailureEvent, and Successes all go into queue
//    Thread waits on blockingqueue until next event
//    If failure and has more waittimes, then put in new startevent
//    otherwise remove from map
//    If success remove from map
//    If startevent start
//     */
//    public RetryDaemon(int[] waitTimes, boolean loop, Environment env) {
//        this.waitTimes = waitTimes;
//        this.loop = loop;
//        this.env = env;
//        retries = new HashMap<>();
//        pending = new LinkedList<>();
//    }
//
//    class RetryInfo {
//        Attempt runner;
//        int count;
//    }
//
//    interface Handler {
//        void go() throws Exception;
//    }
//
//    public interface Attempt {
//        void run(Handler success, Handler fail);
//    }
//}
