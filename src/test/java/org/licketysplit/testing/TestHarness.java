package org.licketysplit.testing;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.*;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.jcraft.jsch.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class TestHarness {
    public TestHarness() {

    }

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

    public void stopInstances() throws Exception {
        ec2 = AmazonEC2ClientBuilder.defaultClient();
        List<Instance> instances = getInstances();
        List<Instance> running = instances.stream()
                .filter(i -> i.getState().getName().equals("running"))
                .collect(Collectors.toList());

        List<String> toStop = running.stream().map(i -> i.getInstanceId())
                .collect(Collectors.toList());
        ec2.stopInstances(new StopInstancesRequest(
                toStop
        ));
        waitUntilState(toStop, "stopped");
    }

    void upload(String host, String[] sources, String[] destinations) throws Exception {
        JSch jSch = new JSch();
        jSch.addIdentity("licketysplit-p2p.pem");
        jSch.setConfig("StrictHostKeyChecking", "no");
        Session session = jSch.getSession("ec2-user", host);
        session.connect();
        ChannelSftp sftp = (ChannelSftp) session.openChannel("sftp");
        sftp.connect();
        for (int i = 0; i < sources.length; i++) {
            System.out.println(String.format(
                    "Uploading file: %s, as: %s, to host: %s",
                    sources[i], destinations[i], host));
            sftp.put(sources[i], destinations[i]);
        }
        sftp.exit();
        sftp.disconnect();
        session.disconnect();
    }

    void download(String host, String src, String dst) throws Exception {
        JSch jSch = new JSch();
        jSch.addIdentity("licketysplit-p2p.pem");
        jSch.setConfig("StrictHostKeyChecking", "no");
        Session session = jSch.getSession("ec2-user", host);
        session.connect();
        ChannelSftp sftp = (ChannelSftp) session.openChannel("sftp");
        sftp.connect();
        sftp.get(src, dst);
        sftp.exit();
        sftp.disconnect();
        session.disconnect();
    }


    public void runLocal(String cmd) throws Exception {
        String cmdPath =  cmd;
        ProcessBuilder pb = new ProcessBuilder("cmd", "/c", cmdPath);
        pb.redirectErrorStream();
        Process p = pb.start();
        BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), "UTF-8"));
        String line;
        while ((line = br.readLine()) != null) {
            System.out.println(line);
        }
        int exitCode = p.waitFor();

        System.out.println("Process terminated with " + exitCode);
    }

    private void runLocal(TestNetworkManager.PeerGenInfo peer) throws Exception {
        String cmdPath = Paths.get(peer.workingDir, TestRunner.cmdFilename+".bat").toAbsolutePath().toString();
        runLocal(cmdPath);
    }

    void runSSH(String host, String cmd) throws Exception {
        System.out.println(String.format("Running cmd: %s, on host %s", cmd, host));
        JSch jSch = new JSch();
        jSch.addIdentity("licketysplit-p2p.pem");
        jSch.setConfig("StrictHostKeyChecking", "no");
        Session session = jSch.getSession("ec2-user", host);
        session.connect();
        ChannelExec channel = (ChannelExec)session.openChannel("exec");
        channel.setCommand(cmd);
        channel.connect();
        InputStream input = channel.getInputStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(input, "UTF-8"));
        String line;
        while ((line = br.readLine()) != null) {
            System.out.println(line);
        }
        channel.disconnect();
        session.disconnect();

        System.out.println(String.format("Done running %s on host %s", cmd, host));
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

        System.out.println(String.format("%d instances total, %d running, will start %d",
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

    public static class P2PTestInfo {
        public TestNetworkManager.TestNetworkDataInfo data;
        public String logFolder;
        public Map<String, String> logFiles;
        public P2PTestInfo() {}
    }

    public void uploadToBucket(String file, String key, String bucketName) throws Exception {
        System.out.format("Uploading %s to S3 bucket %s...\n", file, bucketName);

        final AmazonS3 s3 = AmazonS3ClientBuilder.standard().withRegion(Regions.US_EAST_1).build();
        try {
            s3.putObject(bucketName, key, new File(file));
        } catch (AmazonServiceException e) {
            System.err.println(e.getErrorMessage());
            System.exit(1);
        }
    }

    String jarPath = "C:\\Users\\meps5\\IdeaProjects\\licketysplit\\target\\lickety-split-1.0-SNAPSHOT-jar-with-dependencies.jar";
    String jarDest = "p2p.jar";
    P2PTestInfo createAndUploadFiles(long remoteCount, long localCount, boolean shouldRedeploy) throws Exception {
        int listenPort = 15000;
        List<Instance> instances = getInstances().stream()
                .filter(i->i.getState().getName().equals("running"))
                .collect(Collectors.toList());
        List<TestNetworkManager.PeerGenInfo> peers = new ArrayList<>();
        int peerNumber = 0;
        String rootUser;
        for (Instance instance : instances) {
            if(peerNumber>=remoteCount) break;
            String username = String.format("remote-testuser-%d", peerNumber);
            peers.add(new TestNetworkManager.PeerGenInfo(
                    username,
                    instance.getPublicIpAddress(),
                    listenPort,
                    instance.getInstanceId(),
                    peerNumber==0,
                    false
            ));
            if(peerNumber==0) rootUser = username;
            peerNumber++;
        }
        for (long i = 0; i < localCount; i++) {
            String username = String.format("local-testuser-%d", peerNumber);
            peers.add(new TestNetworkManager.PeerGenInfo(
                    username,
                    "localhost",
                    -1,
                    "local-"+username,
                    peerNumber==0,
                    true
            ));
            if(peerNumber==0) rootUser = username;
            peerNumber++;
        }

        String testDataPath = "test-data";
        TestNetworkManager testNetworkManager = new TestNetworkManager();
        TestNetworkManager.TestNetworkDataInfo data = testNetworkManager.generateNetworkWithPeers(testDataPath, peers);

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

            if(shouldRedeploy||peer.isLocal) {
                sources.add(jarPath);
                dest.add(jarDest);
            }

            String rootStr = "";
            if(peer.isRoot) {
                rootStr = "isroot=yes";
            }
            String cmd = String.format(
                    "killall java8\njava8 -jar %s peerstart %s %s %s %s",
                    jarDest, peer.username, peer.ip, Integer.toString(peer.port), rootStr
            );

            String localPath = null;
            if(peer.isLocal) {
                String localDir = Paths.get(testDataPath, peer.username).toString();
                File localDirFile = new File(localDir);
                localDirFile.mkdir();
                localPath = localDirFile.getPath();
                cmd = String.format(
                        "cd %s\n%s -jar %s peerstart %s %s %s %s",
                        Paths.get(localDir).toAbsolutePath().toString(),
                        "\"C:\\Program Files\\Java\\jdk1.8.0_231\\bin\\java.exe\""
                        ,jarDest, peer.username, peer.ip, Integer.toString(peer.port), rootStr
                );

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
        for (TestNetworkManager.PeerGenInfo peer : peers) {
            runningCount.addAndGet(1);
            new Thread(() -> {
                try {
                    if(!peer.isLocal) {
                        runSSH(peer.ip, "sh " + TestRunner.cmdFilename);
                    }else{
                        runLocal(peer);
                    }
                }catch(Exception e) {
                    e.printStackTrace();
                }
                System.out.println("Decrementing runner count");
                runningCount.decrementAndGet();
            }).start();
        }
        do {
            Thread.sleep(1000);
            if(runningCount.get()==0) {
                break;
            }else{
                System.out.println(String.format("%d peers still running", runningCount.get()));
            }
        } while(true);


        String logFolder = "remote-logs";
        FileUtils.cleanDirectory(new File(logFolder));
        HashMap<String, String> logFiles = new HashMap<>();
        for (TestNetworkManager.PeerGenInfo peer : peers) {
            String src = "log";
            String file = String.format("%s%s%s.log",
                    peer.username,
                    peer.isRoot?"-ROOT":"",
                    peer.isLocal?"-LOCAL":""
            );
            String dest = Paths.get(logFolder, file).toString();

            System.out.println(String.format(
                    "Downloading logs for user %s, host %s, to file %s",
                    peer.username, peer.ip, file
            ));

            if(peer.isLocal) {
                FileUtils.copyFile(Paths.get(peer.workingDir, "log").toFile(), new File(dest));
            }else {
                download(peer.ip, src, dest);
            }
            logFiles.put(peer.username, dest);
        }

        P2PTestInfo results = new P2PTestInfo();
        results.data = data;
        results.logFolder = logFolder;
        results.logFiles = logFiles;
        return results;
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
            System.out.println("Process terminated with " + exitCode);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void cleanAndPackage() {
        String packageString = "\"C:\\Program Files\\JetBrains\\IntelliJ IDEA Community Edition 2019.2.2\\jbr\\bin\\java.exe\" -Dmaven.multiModuleProjectDirectory=C:\\Users\\meps5\\IdeaProjects\\licketysplit \"-Dmaven.home=C:\\Program Files\\JetBrains\\IntelliJ IDEA Community Edition 2019.2.2\\plugins\\maven\\lib\\maven3\" \"-Dclassworlds.conf=C:\\Program Files\\JetBrains\\IntelliJ IDEA Community Edition 2019.2.2\\plugins\\maven\\lib\\maven3\\bin\\m2.conf\" \"-Dmaven.ext.class.path=C:\\Program Files\\JetBrains\\IntelliJ IDEA Community Edition 2019.2.2\\plugins\\maven\\lib\\maven-event-listener.jar\" \"-javaagent:C:\\Program Files\\JetBrains\\IntelliJ IDEA Community Edition 2019.2.2\\lib\\idea_rt.jar=63996:C:\\Program Files\\JetBrains\\IntelliJ IDEA Community Edition 2019.2.2\\bin\" -Dfile.encoding=UTF-8 -classpath \"C:\\Program Files\\JetBrains\\IntelliJ IDEA Community Edition 2019.2.2\\plugins\\maven\\lib\\maven3\\boot\\plexus-classworlds-2.6.0.jar\" org.codehaus.classworlds.Launcher -Didea.version2019.2.2 package";
        String cleanString = "\"C:\\Program Files\\JetBrains\\IntelliJ IDEA Community Edition 2019.2.2\\jbr\\bin\\java.exe\" -Dmaven.multiModuleProjectDirectory=C:\\Users\\meps5\\IdeaProjects\\licketysplit \"-Dmaven.home=C:\\Program Files\\JetBrains\\IntelliJ IDEA Community Edition 2019.2.2\\plugins\\maven\\lib\\maven3\" \"-Dclassworlds.conf=C:\\Program Files\\JetBrains\\IntelliJ IDEA Community Edition 2019.2.2\\plugins\\maven\\lib\\maven3\\bin\\m2.conf\" \"-Dmaven.ext.class.path=C:\\Program Files\\JetBrains\\IntelliJ IDEA Community Edition 2019.2.2\\plugins\\maven\\lib\\maven-event-listener.jar\" \"-javaagent:C:\\Program Files\\JetBrains\\IntelliJ IDEA Community Edition 2019.2.2\\lib\\idea_rt.jar=64003:C:\\Program Files\\JetBrains\\IntelliJ IDEA Community Edition 2019.2.2\\bin\" -Dfile.encoding=UTF-8 -classpath \"C:\\Program Files\\JetBrains\\IntelliJ IDEA Community Edition 2019.2.2\\plugins\\maven\\lib\\maven3\\boot\\plexus-classworlds-2.6.0.jar\" org.codehaus.classworlds.Launcher -Didea.version2019.2.2 clean";
        runCommand(cleanString);
        runCommand(packageString);

    }

    public void installJava8() throws Exception {
        ec2 = AmazonEC2ClientBuilder.defaultClient();
        List<Instance> instances = getInstances();
        startInstanceCount(instances, instances.size());

        instances = getInstances();
        for (Instance instance : instances) {
            runSSH(instance.getPublicIpAddress(), "sudo yum -y install java-1.8.0-openjdk");
        }
    }

    public P2PTestInfo generateNetwork(long remoteCount, long localCount, boolean shouldRedeploy) throws Exception {
        if(shouldRedeploy) cleanAndPackage();
        ec2 = AmazonEC2ClientBuilder.defaultClient();
        List<Instance> instances = getInstances();
        startInstanceCount(instances, remoteCount);
        return createAndUploadFiles(remoteCount, localCount, shouldRedeploy);
    }
}
