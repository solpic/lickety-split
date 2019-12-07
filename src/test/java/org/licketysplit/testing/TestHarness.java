package org.licketysplit.testing;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.HttpMethod;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.*;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.jcraft.jsch.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.licketysplit.Main;
import org.licketysplit.env.Debugger;
import org.licketysplit.env.EnvLogger;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestHarness {
    static String logFolder = "test-logs";
    public static void initializeLogs() {
        File file = new File(logFolder);
        if(!file.exists()) {
            file.mkdir();
        }
    }
    public static Logger fileOnlyLogger(String name, String filename) throws IOException {
        initializeLogs();
        filename = Paths.get(logFolder, filename).toString();
        EnvLogger.resetLogmanager();
        Logger logger = Logger.getLogger(name);
        for (Handler handler : logger.getHandlers()) {
            logger.removeHandler(handler);
        }

        FileHandler fh = new FileHandler(filename);
        fh.setFormatter(new SimpleFormatter() {
            private static final String format = "[%1$tF %1$tT.%1$tL] %3$s %n";

            @Override
            public synchronized String format(LogRecord lr) {
                return lr.getMessage()+"\n";
            }
        });
        logger.addHandler(fh);

        return logger;
    }

    public static Logger fileAndConsoleLogger(String name, String filename) throws Exception {
        initializeLogs();
        filename = Paths.get(logFolder, filename).toString();
        EnvLogger.resetLogmanager();
        Logger logger = Logger.getLogger(name);
        for (Handler handler : logger.getHandlers()) {
            logger.removeHandler(handler);
        }

        FileHandler fh = new FileHandler(filename);
        fh.setFormatter(new SimpleFormatter() {
            private static final String format = "[%1$tF %1$tT.%1$tL] %3$s %n";

            @Override
            public synchronized String format(LogRecord lr) {
                return String.format("%s\n", lr.getMessage());
            }
        });
        logger.addHandler(fh);
        EnvLogger.StdoutConsoleHandler handler = new EnvLogger.StdoutConsoleHandler();
        handler.setFormatter(new SimpleFormatter() {
            private static final String format = "[%1$tF %1$tT.%1$tL] %3$s %n";

            @Override
            public synchronized String format(LogRecord lr) {
                return lr.getMessage()+"\n";
            }
        });

        logger.addHandler(handler);

        return logger;
    }

    public TestHarness() {

    }

    public Logger allLogs;
    public Logger testStatusLogger;

    AmazonEC2 ec2 = null;
    int maxInstances = 0;

    public List<Instance> getInstances() throws Exception {
        boolean done = false;
        ArrayList<Instance> instances = new ArrayList<>();

        DescribeInstancesRequest request = new DescribeInstancesRequest();
        while(!done) {
            DescribeInstancesResult response = ec2.describeInstances(request);

            for(Reservation reservation : response.getReservations()) {
                for(Instance instance : reservation.getInstances()) {
                    instances.add(instance);
                    String stateName = instance.getState().getName();
                    if(stateName.equals("stopped")||stateName.equals("running")) {
                        maxInstances++;
                    }
                }
            }

            request.setNextToken(response.getNextToken());

            if(response.getNextToken() == null) {
                done = true;
            }
        }
        return instances;
    }

    boolean checkIfAllInState(List<String> ids, String state) throws Exception {
        List<Instance> instances = getInstances();
        for (Instance instance : instances) {
            if(instance.getState().getName().equals(state)) {
                ids.remove(instance.getInstanceId());
            }
        }

        return ids.size()==0;
    }

    public int stopInstances() throws Exception {
        ec2 = AmazonEC2ClientBuilder.defaultClient();
        List<Instance> instances = getInstances();
        List<Instance> running = instances.stream()
                .filter(i -> i.getState().getName().equals("running"))
                .collect(Collectors.toList());
        int count = running.size();

        List<String> toStop = running.stream().map(i -> i.getInstanceId())
                .collect(Collectors.toList());
        ec2.stopInstances(new StopInstancesRequest(
                toStop
        ));
        waitUntilState(toStop, "stopped");
        return count;
    }


    Map<String, ChannelSftp> sftps = new HashMap<>();
    ChannelSftp getSftp(String host) throws Exception {
        synchronized (sftps) {
            if(!sftps.containsKey(host)) {
                JSch jsch = new JSch();
                jsch.addIdentity(pemPath);
                jsch.setConfig("StrictHostKeyChecking", "no");
                Session session = jsch.getSession("ec2-user", host);
                session.connect();
                ChannelSftp sftp = (ChannelSftp) session.openChannel("sftp");
                sftp.connect();
                sftps.put(host, sftp);
            }
            return sftps.get(host);
        }
    }


    void upload(String host, String[] sources, String[] destinations) throws Exception {
        testStatusLogger.log(Level.INFO, "Uploading files to "+host);
        JSch jSch = new JSch();
        jSch.addIdentity(pemPath);
        jSch.setConfig("StrictHostKeyChecking", "no");
        Session session = jSch.getSession("ec2-user", host);
        session.connect();
        ChannelSftp sftp = (ChannelSftp) session.openChannel("sftp");
        sftp.connect();
        //ChannelSftp sftp = getSftp(host);
        for (int i = 0; i < sources.length; i++) {
            testStatusLogger.log(Level.INFO,
            String.format(
                    "Uploading file: %s, as: %s, to host: %s",
                    sources[i], destinations[i], host));
            sftp.put(sources[i], destinations[i]);
        }
        sftp.exit();
        sftp.disconnect();
        session.disconnect();
    }

    void download(String host, String src, String dst) throws Exception {
        ChannelSftp sftp = getSftp(host);
        sftp.get(src, dst);
    }

    String pemPath = Paths.get("aws-files", "licketysplit-p2p.pem").toAbsolutePath().toString();
    void checkAssertion(String line) throws Exception {
        String assertStr = "Assert: ";
        int i = line.indexOf(assertStr);
        if(i>=0) {
            int comma = line.indexOf(",", i);
            String valStr = line.substring(i+assertStr.length(), comma);

            boolean val = false;
            if("true".equals(valStr)) val = true;
            else if("false".equals(valStr)) val = false;
            else throw new Exception("Assert error");

            String msg = line.substring(comma+1, line.length());
            assertEquals(val, true, msg);
        }
    }

    Object finishLock = new Object();
    boolean finished = false;
    public void finish() throws Exception {
        synchronized (finishLock) {
            finished = true;
        }
    }

    void runSSH(String host, String cmd, boolean withTriggers, boolean useLog) throws Exception {
        JSch jsch = new JSch();
        jsch.addIdentity(pemPath);
        jsch.setConfig("StrictHostKeyChecking", "no");
        Session session = jsch.getSession("ec2-user", host);
        session.connect();
        //session.connect();
        ChannelExec channel = (ChannelExec)session.openChannel("exec");
        channel.setCommand(cmd);
        channel.connect();
        InputStream input = channel.getInputStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(input, "UTF-8"));
        String line;
        while ((line = br.readLine()) != null) {
            if(useLog)
            allLogs.log(Level.INFO, line);
            else
                System.out.println(line);
            if(withTriggers) {
                Debugger.global().parseTrigger(line);
            }
        }
        channel.disconnect();
//        session.disconnect();

        if(useLog) {
            testStatusLogger.log(Level.INFO, String.format("Done running %s on host %s", cmd, host));
        }else{
            System.out.println("Done running "+cmd+" on host "+host);
        }
    }


    public void runLocal(String cmd) throws Exception {
        String cmdPath =  cmd;
        ProcessBuilder pb = new ProcessBuilder("cmd", "/c", cmdPath);
        pb.redirectErrorStream();
        Process p = pb.start();
        BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), "UTF-8"));
        String line;
        while ((line = br.readLine()) != null) {
            allLogs.log(Level.INFO, line);
            Debugger.global().parseTrigger(line);
        }
        int exitCode = p.waitFor();

        testStatusLogger.log(Level.INFO, "Process terminated with " + exitCode);
    }

    private void runLocal(TestNetworkManager.PeerGenInfo peer) throws Exception {
        if(peer.localThreaded) {
            Main.main(peer.args);
        }else {
            String cmdPath = Paths.get(peer.workingDir, TestRunner.cmdFilename + ".bat").toAbsolutePath().toString();
            runLocal(cmdPath);
        }
    }


    void startInstanceCount(List<Instance> instances, long instancesCount) throws Exception {
        long runningCount = instances.stream()
                .filter(i -> i.getState().getName().equals("running"))
                .count();
        List<Instance> stopped = instances.stream()
                .filter(i -> i.getState().getName().equals("stopped"))
                .collect(Collectors.toList());

        instancesCount = instancesCount-runningCount;
        if(instancesCount>stopped.size()) {
            instancesCount = stopped.size();
        }

        testStatusLogger.log(Level.INFO, String.format("%d instances total, %d running, will start %d",
                runningCount+stopped.size(), runningCount, instancesCount));

        List<Instance> started = new LinkedList<>();
        for (Instance instance : stopped) {
            if(started.size()>=instancesCount) break;
            started.add(instance);
        }
        List<String> toStart = started.stream().map(i -> i.getInstanceId()).collect(Collectors.toList());
        if(toStart.size()>0) {
            StartInstancesResult startInstancesResult = ec2.startInstances(new StartInstancesRequest(toStart));

            waitUntilState(toStart, "running");
        }
    }

    public void cleanAllRunning() throws Exception {
        ec2 = AmazonEC2ClientBuilder.defaultClient();
        List<Instance> instances = getInstances();
        List<Instance> running = instances.stream().filter(e -> e.getState().getName().equals("running"))
                .collect(Collectors.toList());
        for (Instance instance : running) {
            runSSH(instance.getPublicIpAddress(), "killall java8; rm -r ~/*", false, false);
        }
    }

    public void logDownloader() throws Exception {

        ec2 = AmazonEC2ClientBuilder.defaultClient();
        List<Instance> instances = getInstances();
        List<Instance> running = instances.stream().filter(e -> e.getState().getName().equals("running")).collect(Collectors.toList());
        while(true) {
            for (Instance instance : running) {
                try {
                    download(instance.getPublicIpAddress(), "log", Paths.get("test-data-root", "remote-logs", instance.getInstanceId() + ".log").toString());
                }catch(Exception e) {
                    e.printStackTrace();
                }
            }
            Thread.sleep(5000);
        }
    }

    public void restartAll(int count) throws Exception {
        try {
            stopInstances();
        }catch(Exception e) {
            e.printStackTrace();
        }
        ec2 = AmazonEC2ClientBuilder.defaultClient();
        List<Instance> instances = getInstances();
        startInstanceCount(instances, count);
    }

    public static class P2PTestInfo {
        public TestNetworkManager.TestNetworkDataInfo data;
        public String logFolder;
        public Map<String, String> logFiles;
        public P2PTestInfo() {}
    }

    public String getPresignedURL(String bucketName, String objectKey, double daysExpiration) throws Exception {
        java.util.Date expiration = new java.util.Date();
        long expTimeMillis = expiration.getTime();
        expTimeMillis += (long)(daysExpiration*1000*60*60*24);
        expiration.setTime(expTimeMillis);

        final AmazonS3 s3 = AmazonS3ClientBuilder.standard().withRegion(Regions.US_EAST_1).build();
        GeneratePresignedUrlRequest generatePresignedUrlRequest =
                new GeneratePresignedUrlRequest(bucketName, objectKey)
                        .withMethod(HttpMethod.GET)
                        .withExpiration(expiration);
        URL url = s3.generatePresignedUrl(generatePresignedUrlRequest);
        return url.toString();
    }

    public void uploadToBucket(String file, String key, String bucketName) throws Exception {
        testStatusLogger.log(Level.INFO, String.format("Uploading %s to S3 bucket %s...\n", file, bucketName));

        final AmazonS3 s3 = AmazonS3ClientBuilder.standard().withRegion(Regions.US_EAST_1).build();
        try {
            s3.putObject(bucketName, key, new File(file));
        } catch (AmazonServiceException e) {
            System.err.println(e.getErrorMessage());
            System.exit(1);
        }
    }

    String jarPath = "C:\\Users\\Merrill\\Documents\\LicketySplit\\lickety-split\\target\\lickety-split-1.0-SNAPSHOT-jar-with-dependencies.jar";
    String jarDest = "p2p.jar";

    public boolean dontDeployLocalGenerateBootstrapInstead = false;

    P2PTestInfo createAndUploadFiles(long remoteCount, long localCount, String runcmd, boolean shouldRedeploy, boolean localThreaded
    , boolean exitAfterStart) throws Exception {
        if(remoteCount>0) cleanAllRunning();
        jarPath = System.getProperty("jarPath");
        String _testDataPath = "test-data";
        String _testingFolder = "test-data-root";
        File testingFolderDir = new File(_testingFolder);
        if(!testingFolderDir.exists()) {
            testingFolderDir.mkdir();
        }
        FileUtils.cleanDirectory(testingFolderDir);
        File testDataDir = Paths.get(testingFolderDir.getPath(), _testDataPath).toFile();
        if(!testDataDir.exists()) {
            testDataDir.mkdir();
        }
        FileUtils.cleanDirectory(testDataDir);
        int listenPort = 15000;
        List<TestNetworkManager.PeerGenInfo> peers = new ArrayList<>();
        int peerNumber = 0;
        String rootUser;

        int localPort = 15000;
        for (long i = 0; i < localCount; i++) {
            String username = String.format("testuser-%d-local", peerNumber);
            peers.add(new TestNetworkManager.PeerGenInfo(
                    username,
                    "localhost",
                    localPort,
                    "local-"+username,
                    peerNumber==0,
                    true,
                    localThreaded
            ));
            localPort++;
            if(peerNumber==0) rootUser = username;
            peerNumber++;
        }
        if(remoteCount>0) {
            List<Instance> instances = getInstances().stream()
                    .filter(i -> i.getState().getName().equals("running"))
                    .collect(Collectors.toList());
            for (int i = 0; i < remoteCount; i++) {
                Instance instance = instances.get(i);
                String username = String.format("testuser-%d-remote", peerNumber);
                peers.add(new TestNetworkManager.PeerGenInfo(
                        username,
                        instance.getPublicIpAddress(),
                        listenPort,
                        instance.getInstanceId(),
                        peerNumber == 0,
                        false,
                        localThreaded
                ));
                if (peerNumber == 0) rootUser = username;
                peerNumber++;
            }
        }


        String jarurl = getPresignedURL("licketysplit-jar", "jar", 5);
        String wgetCmd = String.format("wget -O %s \"%s\"", jarDest, jarurl);
        TestNetworkManager testNetworkManager = new TestNetworkManager();
        TestNetworkManager.TestNetworkDataInfo data = testNetworkManager.generateNetworkWithPeers(testDataDir.getPath(), peers);

        for (TestNetworkManager.PeerGenInfo peer : peers) {
            List<String> sources = new ArrayList<>();
            ArrayList<String> dest = new ArrayList<>();

            if(peer.isRoot) {
                sources.add(data.rootKeyFile);
                dest.add(TestRunner.rootKeyFilename);
            }

            sources.add(data.infoFile);
            dest.add(TestRunner.infoFilename);

            sources.add(data.idKeys.get(peer.username));
            dest.add(TestRunner.idKeyFilename);

            if(peer.isLocal) {
                sources.add(jarPath);
                dest.add(jarDest);
            }

            String rootStr = "";
            if(peer.isRoot) {
                rootStr = "isroot=yes";
            }
            String cmd = String.format(
                    "killall java8\nrm %s\n%s\njava8 -jar %s %s %s %s %s %s &",
                    jarDest, wgetCmd, jarDest, runcmd,  peer.username, peer.ip, Integer.toString(peer.port), rootStr
            );

            String localPath = null;
            if(peer.isLocal) {
                String localDir = Paths.get(testDataDir.getPath(), peer.username).toString();
                File localDirFile = new File(localDir);
                localDirFile.mkdir();
                localPath = localDirFile.getPath();
                cmd = String.format(
                        "cd %s\n%s -jar %s %s %s %s %s %s",
                        Paths.get(localDir).toAbsolutePath().toString(),
                        "\"C:\\Program Files\\Java\\jdk1.8.0_231\\bin\\java.exe\"", runcmd
                        ,jarDest, peer.username, peer.ip, Integer.toString(peer.port), rootStr
                );

                peer.args = new String[] {runcmd, peer.username, peer.ip, Integer.toString(peer.port), rootStr, localPath};

                peer.workingDir = localPath;
                peer.cmd = cmd;
            }

            File cmdFile = File.createTempFile("cmd", null);
            Files.write(cmdFile.toPath(), cmd.getBytes());

            sources.add(cmdFile.getPath());
            if(peer.isLocal) {
                dest.add(TestRunner.cmdFilename+".bat");
            }else {
                dest.add(TestRunner.cmdFilename);
            }

            String[] src = new String[sources.size()];
            String[] dst = new String[dest.size()];
            for (int i = 0; i < sources.size(); i++) {
                src[i] = sources.get(i);
                dst[i] = dest.get(i);
            }

            if(peer.isLocal) {
                localUpload(localPath, src, dst);
            }else {
                upload(peer.ip, src, dst);
            }

            cmdFile.deleteOnExit();
        }
        AtomicInteger runningCount = new AtomicInteger(0);
        ConcurrentHashMap<String, Boolean> runningMap = new ConcurrentHashMap<>();


        Debugger dbg = Debugger.global();
        dbg.setEnabled(true);
        BlockingQueue<Object[]> assertions = new LinkedBlockingQueue<>();
        dbg.setTrigger("assert", (Object ...args) ->{
            assertions.add(args);
        });
        dbg.setTrigger("print", (Object ...args) -> {
            testStatusLogger.log(Level.INFO, (String)args[1]);
        });

        AtomicInteger count = new AtomicInteger(peers.size());
        dbg.setTrigger("done", (Object ...args) -> {
            count.decrementAndGet();
            if(count.get()==0) {
                try {
                    finish();
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        });


        boolean firstUser = true;
        for (TestNetworkManager.PeerGenInfo peer : peers) {
            runningCount.addAndGet(1);
            runningMap.put(peer.username, true);
            boolean finalFirstUser = firstUser;
            new Thread(() -> {
                try {
                    if(!peer.isLocal) {
                        runSSH(peer.ip, "sh " + TestRunner.cmdFilename, true, true);
                    }else{
                        runLocal(peer);
                    }
                }catch(Exception e) {
                    e.printStackTrace();
                }
                testStatusLogger.log(Level.INFO, "Decrementing runner count");
                runningMap.put(peer.username, false);
                runningCount.decrementAndGet();
            }).start();
            firstUser = false;
        }

        String _logFolder = "remote-logs";
        File remoteLogsFolder = Paths.get(testingFolderDir.getPath(), _logFolder).toFile();
        if(!remoteLogsFolder.exists()) {
            remoteLogsFolder.mkdir();
        }
        FileUtils.cleanDirectory(remoteLogsFolder);
        new Thread(() -> {
            try {
                do {
                    Thread.sleep(1000);
                    synchronized (finishLock) {
                        if(finished) {
                            testStatusLogger.log(Level.INFO, "Forcing finish");
                            break;
                        }
                    }
                    if(runningCount.get()==0) {
                        finish();
                        break;
                    }else{
                        String peerlist = runningMap.entrySet().stream()
                                .filter(e -> e.getValue())
                                .map(e -> e.getKey())
                                .collect(Collectors.joining(", "));
                        //System.out.println(String.format("%d peers still running: %s", runningCount.get(), peerlist));

                    }
                } while(true);
                finish();
            }catch(Exception e) {
                e.printStackTrace();
            }
        }).start();


        do {
            Object[] args = assertions.poll(1000, TimeUnit.MILLISECONDS);
            downloadLogsRetry(peers, remoteLogsFolder.getPath(), 3);
            if(args!=null) {
                String username = (String)args[0];
                boolean val = (boolean)args[1];
                String msg = (String)args[2];
                if(val) {
                    testStatusLogger.log(Level.INFO, String.format("TEST PASSED!\n%s ->\n%s", username, msg));
                }else{
                    testStatusLogger.log(Level.INFO, String.format(
                            "TEST FAILED!\n%s ->\n%s", username, msg)
                    );
                }
            }
            synchronized (finishLock) {
                if(finished) {
                    break;
                }
            }
        } while(true);

        HashMap<String, String> logFiles = downloadLogsRetry(peers, remoteLogsFolder.getPath(), 3);


        P2PTestInfo results = new P2PTestInfo();
        results.data = data;
        results.logFolder = remoteLogsFolder.getPath();
        results.logFiles = logFiles;
        return results;
    }

    HashMap<String, String> downloadLogsRetry(List<TestNetworkManager.PeerGenInfo> peers, String logFolder, int retries) throws Exception {
        try {
            return downloadLogs(peers, logFolder);
        } catch(Exception e) {
            if(retries==0) {
                throw e;
            }else{
                testStatusLogger.log(Level.INFO, "Retrying download logs", e);
                e.printStackTrace();
            }
        }
        return null;
    }

    HashMap<String, String> downloadLogs(List<TestNetworkManager.PeerGenInfo> peers, String logFolder) throws Exception {
        HashMap<String, String> logFiles = new HashMap<>();
        boolean failed = false;
        for (TestNetworkManager.PeerGenInfo peer : peers) {
            try {
                String src = "log";
                String file = String.format("%s%s%s.log",
                        peer.username,
                        peer.isRoot ? "-ROOT" : "",
                        peer.isLocal ? "-LOCAL" : ""
                );
                String dest = Paths.get(logFolder, file).toString();

//            System.out.println(String.format(
//                    "Downloading logs for user %s, host %s, to file %s",
//                    peer.username, peer.ip, file
//            ));

                if (peer.isLocal) {
                    FileUtils.copyFile(Paths.get(peer.workingDir, "log").toFile(), new File(dest));
                } else {
                    download(peer.ip, src, dest);
                }
                logFiles.put(peer.username, dest);
            } catch(Exception e) {
                e.printStackTrace();
                failed = true;
            }
        }
        if(failed) throw new Exception();
        return logFiles;
    }
    private void localUpload(String localPath, String[] src, String[] dst) throws Exception{
        for (int i = 0; i < src.length; i++) {
            FileUtils.copyFile(new File(src[i]), Paths.get(localPath, dst[i]).toFile());
        }
    }

    void waitUntilState(List<String> ids, String state) throws Exception {
        double waitSeconds = 10.0;
        while(!checkIfAllInState(ids, state)) {
            System.out.println(
                    String.format(
                            "Instances not yet changed state, waiting %f seconds for %d instances to state change\nIDs: %s",
                            waitSeconds,
                            ids.size(),
                            ids.stream().collect(Collectors.joining(", "))
                    )
            );
            Thread.sleep((int) (waitSeconds * 1000));
        }
    }

    void runCommand(String command) {

        try {
            ProcessBuilder pb = new ProcessBuilder("cmd", "/c", command);
            pb.redirectError();
            Process p = pb.start();
            InputStream inputStream = p.getInputStream();
            String out = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
            int exitCode = p.waitFor();
            //System.out.println(out);
            testStatusLogger.log(Level.INFO, "Process terminated with " + exitCode);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void cleanAndPackage() {
        String packageString = System.getProperty("packageCommandString");
        String cleanString = "\"C:\\Program Files\\Java\\jdk-12\\bin\\java.exe\" -Dmaven.multiModuleProjectDirectory=C:\\Users\\Merrill\\Documents\\LicketySplit\\lickety-split \"-Dmaven.home=D:\\IntelliJ IDEA Community Edition 2019.2.2\\plugins\\maven\\lib\\maven3\" \"-Dclassworlds.conf=D:\\IntelliJ IDEA Community Edition 2019.2.2\\plugins\\maven\\lib\\maven3\\bin\\m2.conf\" \"-Dmaven.ext.class.path=D:\\IntelliJ IDEA Community Edition 2019.2.2\\plugins\\maven\\lib\\maven-event-listener.jar\" \"-javaagent:D:\\IntelliJ IDEA Community Edition 2019.2.2\\lib\\idea_rt.jar=60668:D:\\IntelliJ IDEA Community Edition 2019.2.2\\bin\" -Dfile.encoding=UTF-8 -classpath \"D:\\IntelliJ IDEA Community Edition 2019.2.2\\plugins\\maven\\lib\\maven3\\boot\\plexus-classworlds-2.6.0.jar\" org.codehaus.classworlds.Launcher -Didea.version2019.2.2 clean -P local-threads";
        //runCommand(cleanString);
        runCommand(packageString);

    }

    public void installJava8() throws Exception {
        ec2 = AmazonEC2ClientBuilder.defaultClient();
        List<Instance> instances = getInstances();
        startInstanceCount(instances, instances.size());

        instances = getInstances();
        for (Instance instance : instances) {
            runSSH(instance.getPublicIpAddress(), "sudo yum -y install java-1.8.0-openjdk", false, false);
        }
    }

    public P2PTestInfo generateNetwork(long remoteCount, long localCount, String runcmd, boolean shouldRedeploy, boolean localThreaded,
                                       boolean exitAfterStart) throws Exception {
        jarPath = System.getProperty("jarPath");
        File jar = new File(jarPath);
        if(shouldRedeploy) {
            cleanAndPackage();
        }
        if(!jar.exists()) {
            throw new Exception(String.format("NO jar found at %s", jar.getPath()));
        }

        if(remoteCount>0&&shouldRedeploy) {
            uploadToBucket(jarPath, "jar", "licketysplit-jar");
        }
        if(remoteCount>0) {
            ec2 = AmazonEC2ClientBuilder.defaultClient();
            List<Instance> instances = getInstances();
            startInstanceCount(instances, remoteCount);
        }
        return createAndUploadFiles(remoteCount, localCount, runcmd, shouldRedeploy, localThreaded, exitAfterStart);
    }
}
