package org.licketysplit.testing;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import org.licketysplit.env.Debugger;
import org.licketysplit.filesharer.DownloadManager;
import org.licketysplit.securesocket.encryption.SymmetricCipher;
import org.licketysplit.testing.TestHarness.P2PTestInfo;

import java.io.File;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Runs various tests by simulating peers using Amazon EC2 servers.
 */
public class EC2Test {
    public static boolean shouldRedeploy() {
        return "yes".equals( System.getProperty("shouldRedeploy"));
    }
    public static boolean localThreaded() {
        return "yes".equals(System.getProperty("useLocalThreaded"));
    }

    @Test
    public void downloadSpeedTest() throws Exception {
        TestHarness testHarness = new TestHarness();
        P2PTestInfo hackMaster = new P2PTestInfo();
        Logger allLogs = TestHarness.fileOnlyLogger("allLogs", Paths.get("everything.log").toString());
        Logger testStatus = TestHarness.fileAndConsoleLogger("testStatus", Paths.get("test-results.log").toString());
        testHarness.allLogs = allLogs;
        testHarness.testStatusLogger = testStatus;
        ConcurrentHashMap<String,Boolean> hasFiles = new ConcurrentHashMap<>();
        int remoteCount = 10;
        int localCount = 0;
        float mb = TestRunner.fileMB;
        AtomicLong startTime = new AtomicLong();
        Debugger.global().setTrigger("start-test", (Object ...args) -> {
            startTime.set(System.currentTimeMillis());
            testStatus.log(Level.INFO, String.format("Test starting!"));
        });
        class Progress {
            public long curTime;
            float progress;
            String username;

            public float sampleProgress(Map<String, List<Progress>> progressUpdates, long time, long actualStartTime) {
                float sumProgress = 0.0f;
                int count = 0;
                for (Map.Entry<String, List<Progress>> userProgress : progressUpdates.entrySet()) {
                    List<Progress> progresses = userProgress.getValue();
                    Collections.sort(progresses, (a, b) -> {
                        return (int)(a.curTime - b.curTime);
                    });
                    Progress start = null;
                    Progress end = null;
                    Progress tmpStart = progresses.get(0);
                    if(tmpStart.curTime>time) {
                        sumProgress += tmpStart.progress;
                        count++;
                        continue;
                    }
                    Progress tmpEnd = progresses.get(progresses.size() - 1);
                    if(tmpEnd.curTime<time) {
                        sumProgress += 1.0f;
                        count++;
                        continue;
                    }
                    for (int i = 0; i < progresses.size(); i++) {
                        Progress cur = progresses.get(i);
                        if(cur.curTime>=time) {
                            end = cur;
                            if(i>0) start = progresses.get(i-1);
                             i =progresses.size();
                        }
                    }
                    float startProgress = 0.0f;
                    if(start!=null) startProgress = start.progress;
                    if(end!=null) {
                        long startTime = actualStartTime;
                        if(start!=null) startTime = start.curTime;
                        long endTime = end.curTime;
                        float endProgress = end.progress;
                        float timeDiffNormalized = ((float)(time-startTime))/((float)(endTime-startTime));

                        float progress = timeDiffNormalized*endProgress + (1.0f-timeDiffNormalized)*startProgress;
                        sumProgress += progress;
                        count++;
                    }

                }

                float v = sumProgress/count;
                return v;
            }
        }
        Map<String, List<Progress>> progressUpdates = new HashMap<>();
        Debugger.global().setTrigger("progress", (Object ...args) -> {
            String username = (String)args[0];
            float progress = new Float(((Double)args[1]));
            long curTime = System.currentTimeMillis();

            Progress p = new Progress();
            p.curTime = curTime;
            p.username = username;
            p.progress = progress;
            synchronized (progressUpdates) {
                if(!progressUpdates.containsKey(username)) {
                    progressUpdates.put(username, new LinkedList<Progress>());
                }
                List<Progress> progresses = progressUpdates.get(username);
                progresses.add(p);
            }
            testStatus.log(Level.INFO, String.format("%s has completed %f%%", username, progress*100));
        });
        Debugger.global().setTrigger("download-complete", (Object ...args) -> {
            String username = (String)args[0];
            hasFiles.putIfAbsent(username, true);
            testStatus.log(Level.INFO, String.format(
                    "%d out of %d have completed downloads", hasFiles.size(), (remoteCount+localCount-1)
            ));
            if(hasFiles.size()==(remoteCount+localCount-1)) {
                long curTime = System.currentTimeMillis();
                synchronized (progressUpdates){
                    for (Map.Entry<String, List<Progress>> entry : progressUpdates.entrySet()) {
                        Progress p = new Progress();
                        p.curTime = curTime;
                        p.progress = 1.0f;
                        p.username = entry.getKey();
                        entry.getValue().add(p);
                    }
                }
                long elapsed = curTime - startTime.get();

                testStatus.log(Level.INFO, String.format(
                        "Test finished, total elapsed time: %f minutes, file size: %f MB, downloading peers: %d",
                        (((float)elapsed)/(1000*60)),
                        mb,
                        (remoteCount+localCount-1)));

                Progress p = new Progress();
                StringBuilder data = new StringBuilder();
                data.append("Progress %, Download Speed MB/s, Time (seconds), Total Peers, File Size (MB)\n");
                float lastSampled = 0.0f;
                long diff = elapsed/100;
                for(long tm = startTime.get(); tm<=curTime; tm += diff) {
                    float sampledProgress = p.sampleProgress(progressUpdates, tm, startTime.get());
                    float sampledSpeed = (sampledProgress-lastSampled)*mb*1000.0f/((float)diff);
                    data.append(String.format(
                            "%f, %f, %f, %d, %f\n",
                            sampledProgress*100.0f, sampledSpeed, (tm-startTime.get())/1000.0f,
                            (remoteCount+localCount-1),
                            mb
                    ));
                    lastSampled = sampledProgress;
                }

                File testOutput = new File("test-output.csv");
                try {
                    if(!testOutput.exists()) testOutput.createNewFile();
                    FileUtils.writeStringToFile(testOutput, data.toString(), "UTF-8");
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        });
        if(remoteCount>0)
            testHarness.cleanAllRunning();
        TestHarness.P2PTestInfo results = testHarness.generateNetwork(remoteCount, localCount, "peerstart", shouldRedeploy(), localThreaded(), false);
    }

    @Test
    public void sharingStressTest() throws Exception {
        TestHarness testHarness = new TestHarness();
        P2PTestInfo hackMaster = new P2PTestInfo();
        Logger allLogs = TestHarness.fileOnlyLogger("allLogs", Paths.get("everything.log").toString());
        Logger testStatus = TestHarness.fileAndConsoleLogger("testStatus", Paths.get("test-results.log").toString());
        testHarness.allLogs = allLogs;
        testHarness.testStatusLogger = testStatus;
        int remoteCount = 10;
        int localCount = 0;

        class Progress {
            public long curTime;
            float progress;
            String username;

            public float sampleProgress(Map<String, List<Progress>> progressUpdates, long time, long actualStartTime) {
                float sumProgress = 0.0f;
                int count = 0;
                for (Map.Entry<String, List<Progress>> userProgress : progressUpdates.entrySet()) {
                    List<Progress> progresses = userProgress.getValue();
                    Collections.sort(progresses, (a, b) -> {
                        return (int)(a.curTime - b.curTime);
                    });
                    Progress start = null;
                    Progress end = null;
                    Progress tmpStart = progresses.get(0);
                    if(tmpStart.curTime>time) {
                        sumProgress += tmpStart.progress;
                        count++;
                        continue;
                    }
                    Progress tmpEnd = progresses.get(progresses.size() - 1);
                    if(tmpEnd.curTime<time) {
                        sumProgress += 1.0f;
                        count++;
                        continue;
                    }
                    for (int i = 0; i < progresses.size(); i++) {
                        Progress cur = progresses.get(i);
                        if(cur.curTime>=time) {
                            end = cur;
                            if(i>0) start = progresses.get(i-1);
                            i =progresses.size();
                        }
                    }
                    float startProgress = 0.0f;
                    if(start!=null) startProgress = start.progress;
                    if(end!=null) {
                        long startTime = actualStartTime;
                        if(start!=null) startTime = start.curTime;
                        long endTime = end.curTime;
                        float endProgress = end.progress;
                        float timeDiffNormalized = ((float)(time-startTime))/((float)(endTime-startTime));

                        float progress = timeDiffNormalized*endProgress + (1.0f-timeDiffNormalized)*startProgress;
                        sumProgress += progress;
                        count++;
                    }

                }

                float v = sumProgress/count;
                return v;
            }
        }
        Map<String, List<Progress>> progressUpdates = new HashMap<>();
        Debugger.global().setTrigger("progress", (Object ...args) -> {
            String username = (String)args[0];
            float progress = new Float(((Double)args[1]));
            //testStatus.log(Level.INFO, String.format("%s has completed %f%%", username, progress*100));
        });
        Debugger.global().setTrigger("download-complete", (Object ...args) -> {
            String username = (String)args[0];
            String fname = (String)args[1];
            Integer lengthChunks = (Integer)args[2];
            float sizeMB = lengthChunks* DownloadManager.chunkLengthRaw/(1024.0f*1024.0f);
            testStatus.log(Level.INFO, String.format("%s has COMPLETED download %s, size %.2f MB", username, fname, sizeMB));
        });
        Debugger.global().setTrigger("download-failed", (Object ...args) -> {
            String username = (String)args[0];
            String fname = (String)args[1];
            Integer lengthChunks = (Integer)args[2];
            float sizeMB = lengthChunks* DownloadManager.chunkLengthRaw/(1024.0f*1024.0f);
            testStatus.log(Level.INFO, String.format("%s has FAILED download %s, size %.2f MB", username, fname, sizeMB));
        });
        Debugger.global().setTrigger("download-cancelled", (Object ...args) -> {
            String username = (String)args[0];
            String fname = (String)args[1];
            Integer lengthChunks = (Integer)args[2];
            float sizeMB = lengthChunks* DownloadManager.chunkLengthRaw/(1024.0f*1024.0f);
            testStatus.log(Level.INFO, String.format("%s has CANCELLED download %s, size %.2f MB", username, fname, sizeMB));
        });

        TestHarness.P2PTestInfo results = testHarness.generateNetwork(remoteCount, localCount, "peerstart", shouldRedeploy(), localThreaded(), false);
    }

    @Test
    public void deployTestNetwork() throws Exception {
        TestHarness testHarness = new TestHarness();
        P2PTestInfo hackMaster = new P2PTestInfo();
        Logger allLogs = TestHarness.fileOnlyLogger("allLogs", Paths.get("everything.log").toString());
        Logger testStatus = TestHarness.fileAndConsoleLogger("testStatus", Paths.get("test-results.log").toString());
        testHarness.allLogs = allLogs;
        testHarness.testStatusLogger = testStatus;
        ConcurrentHashMap<String, String> hasFiles = new ConcurrentHashMap<>();
        int remoteCount = 10;
        // Change to peerstart for bigfile test
        TestHarness.P2PTestInfo results = testHarness.generateNetwork(remoteCount, 1, "peerstart-with-local", shouldRedeploy(), localThreaded(), true);
    }

    @Test
    public void logDownloader() throws Exception {
        TestHarness testHarness = new TestHarness();
        testHarness.logDownloader();
    }

    @Test
    public void cleanAllRunning() throws Exception {
        TestHarness testHarness = new TestHarness();

    }

    @Test
    public void encryptionTest() throws Exception {
        SymmetricCipher cipher = new SymmetricCipher();
        SymmetricCipher.SymmetricKey symmetricKey = cipher.generateKey();
        cipher.setKey(symmetricKey.getKey().getEncoded(), symmetricKey.getIv());

        Random random = new Random();
        for(int i = 0 ; i<1000; i++) {
            int size = random.nextInt(20000)+100;
            byte[] plaintext = new byte[size];
            random.nextBytes(plaintext);

            byte[] ciphertext = cipher.encrypt(plaintext);
            System.out.println(plaintext.length+" -> "+ciphertext.length);
            byte[] decrypted = cipher.decrypt(ciphertext);

            assertArrayEquals(plaintext, decrypted);
        }
    }

    @Test
    public void restartAll() throws Exception {
        TestHarness testHarness = new TestHarness();
        Logger allLogs = TestHarness.fileOnlyLogger("allLogs", Paths.get("everything.log").toString());
        Logger testStatus = TestHarness.fileAndConsoleLogger("testStatus", Paths.get("test-results.log").toString());       testHarness.allLogs = allLogs;
        testHarness.testStatusLogger = testStatus;
        testHarness.restartAll(10);
        return;
    }

    @Test
    public void stopAll() throws Exception {
        TestHarness testHarness = new TestHarness();
        Logger allLogs = TestHarness.fileOnlyLogger("allLogs", Paths.get("everything.log").toString());
        Logger testStatus = TestHarness.fileAndConsoleLogger("testStatus", Paths.get("test-results.log").toString());       testHarness.allLogs = allLogs;
        testHarness.testStatusLogger = testStatus;
        testHarness.stopInstances();
        return;
    }
}
