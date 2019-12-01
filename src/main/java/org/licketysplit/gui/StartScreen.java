package org.licketysplit.gui;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.licketysplit.env.Environment;
import org.licketysplit.filesharer.FileSharer;
import org.licketysplit.securesocket.peers.KeyStore;
import org.licketysplit.securesocket.peers.PeerInfoDirectory;
import org.licketysplit.securesocket.peers.PeerManager;
import org.licketysplit.securesocket.peers.UserInfo;
import org.licketysplit.syncmanager.FileManager;
import org.licketysplit.syncmanager.SyncManager;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class StartScreen extends JPanel {
    File savedNetworksFile() throws Exception{
        File file = new File("networks.txt");
        if(!file.exists()) {
            boolean newFile = file.createNewFile();
            if(!newFile) {
                throw new Exception();
            }
            FileUtils.writeStringToFile(file, "[]", "UTF-8");
        }
        return file;
    }
    SavedNetwork[] loadSavedNetworks() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(savedNetworksFile(), SavedNetwork[].class);
    }


    void addNetworkToList(File bootstrap)  {
        try {

            String name = JOptionPane.showInputDialog("Name of network (for your use only)?");
            SavedNetwork savedNetwork = makeFromBootstrap(name, bootstrap);
            SavedNetwork[] savedNetworks = loadSavedNetworks();
            SavedNetwork[] newSavedNetworks = new SavedNetwork[savedNetworks.length+1];
            for (int i = 0; i < savedNetworks.length; i++) {
                newSavedNetworks[i] = savedNetworks[i];
            }
            newSavedNetworks[newSavedNetworks.length-1] = savedNetwork;

            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(savedNetworksFile(), newSavedNetworks);

            Object[][] data = Arrays.stream(loadSavedNetworks())
                    .map(e-> new Object[]{e.getName()})
                    .toArray(Object[][]::new);
            DefaultTableModel model = (DefaultTableModel)(table.getModel());
            model.setRowCount(0);
            for (Object[] datum : data) {
                model.addRow(datum);
            }
        }catch(Exception e) {
            JOptionPane.showMessageDialog(null,
                    String.format("Error adding new network to list: %s", e.getMessage())
            );
            e.printStackTrace();
        }
    }

    public void createdNetwork(File bootstrap) {
        addNetworkToList(bootstrap);
    }

    private static void unzip(String zipFilePath, String destDir) throws Exception {
        File dir = new File(destDir);
        // create output directory if it doesn't exist
        if(!dir.exists()) dir.mkdirs();
        FileInputStream fis;
        //buffer for read and write data to file
        byte[] buffer = new byte[1024];
        fis = new FileInputStream(zipFilePath);
        ZipInputStream zis = new ZipInputStream(fis);
        ZipEntry ze = zis.getNextEntry();
        while(ze != null){
            String fileName = ze.getName();
            File newFile = new File(destDir + File.separator + fileName);
            System.out.println("Unzipping to "+newFile.getAbsolutePath());
            //create directories for sub directories in zip
            new File(newFile.getParent()).mkdirs();
            FileOutputStream fos = new FileOutputStream(newFile);
            int len;
            while ((len = zis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
            fos.close();
            //close this ZipEntry
            zis.closeEntry();
            ze = zis.getNextEntry();
        }
        //close last ZipEntry
        zis.closeEntry();
        zis.close();
        fis.close();

    }

    void checkNotFileAndMkdir(File path) throws Exception {
        if (path.isFile()) {
            throw new Exception(String.format("File '%s' exists, need access as directory", path.getAbsolutePath()));
        }
        if(!path.exists()) {
            path.mkdir();
        }
    }
    public File baseDirForNetwork(String name) throws Exception {
        File data = new File("data");
        checkNotFileAndMkdir(data);

        File base = Paths.get(data.getPath(), name).toFile();
        checkNotFileAndMkdir(base);
        File shared = Paths.get(base.getPath(), "shared").toFile();

        checkNotFileAndMkdir(shared);

        File configs = Paths.get(base.getPath(), "config").toFile();
        checkNotFileAndMkdir(configs);

        return base;
    }

    public SavedNetwork makeFromBootstrap(String name, File bootstrap) throws Exception {
        Path temp = Files.createTempDirectory("temp");
        unzip(bootstrap.getAbsolutePath(), temp.toString());

        File base = baseDirForNetwork(name);
        File infodir = Paths.get(base.getPath(), "peer-info-directory.json").toFile();
        File idkey = Paths.get(base.getPath(), "idkey").toFile();
        File userinfo = Paths.get(base.getPath(), "userinfo.json").toFile();
        File rootkey = Paths.get(base.getPath(), "rootkey").toFile();

        FileUtils.copyFile(Paths.get(temp.toString(), "peerinfodir").toFile(), infodir);
        FileUtils.copyFile(Paths.get(temp.toString(), "idkey").toFile(), idkey);
        FileUtils.copyFile(Paths.get(temp.toString(), "userinfo").toFile(), userinfo);
        File rootkeyTemp = Paths.get(temp.toString(), "rootkey").toFile();

        SavedNetwork network = new SavedNetwork();
        network.idKey = idkey.getAbsolutePath();
        network.infoDir = infodir.getAbsolutePath();
        network.userInfo = userinfo.getAbsolutePath();

        network.name = name;
        network.sharedFiles = Paths.get(base.getPath(), "shared").toFile().getAbsolutePath();
        network.configs = Paths.get(base.getPath(), "config").toFile().getAbsolutePath();

        network.rootKey = null;
        if(rootkeyTemp.exists()) {
            FileUtils.copyFile(rootkeyTemp, rootkey);
            network.rootKey = rootkey.getAbsolutePath();
        }

        FileUtils.cleanDirectory(temp.toFile());
        FileUtils.deleteDirectory(temp.toFile());

        return network;
    }



    public static class SavedNetwork {
        String idKey;
        String rootKey;
        String infoDir;
        String userInfo;
        String sharedFiles;
        String configs;
        String name;

        public String getUserInfo() {
            return userInfo;
        }

        public void setUserInfo(String userInfo) {
            this.userInfo = userInfo;
        }


        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public SavedNetwork() {
        }

        public String getIdKey() {
            return idKey;
        }

        public void setIdKey(String idKey) {
            this.idKey = idKey;
        }

        public String getRootKey() {
            return rootKey;
        }

        public void setRootKey(String rootKey) {
            this.rootKey = rootKey;
        }

        public String getInfoDir() {
            return infoDir;
        }

        public void setInfoDir(String infoDir) {
            this.infoDir = infoDir;
        }

        public String getSharedFiles() {
            return sharedFiles;
        }

        public void setSharedFiles(String sharedFiles) {
            this.sharedFiles = sharedFiles;
        }

        public String getConfigs() {
            return configs;
        }

        public void setConfigs(String configs) {
            this.configs = configs;
        }

        public Environment start() throws Exception{
            PeerInfoDirectory info = new PeerInfoDirectory(infoDir);
            info.load();

            ObjectMapper objectMapper = new ObjectMapper();
            HashMap hashMap = objectMapper.readValue(new File(userInfo), HashMap.class);
            String username = (String)hashMap.get("username");
            String ip = (String)hashMap.get("ip");
            String port = (String)hashMap.get("port");

            boolean isRoot = rootKey!=null;

            KeyStore rootKeyStore = null;
            if(isRoot) {
                rootKeyStore = new KeyStore(rootKey);
                rootKeyStore.load();
            }

            KeyStore idKeyStore = new KeyStore(idKey);
            idKeyStore.load();

            PeerManager.ServerInfo serverInfo = new PeerManager.ServerInfo(Integer.parseInt(port), ip);
            org.licketysplit.securesocket.peers.UserInfo user = new UserInfo(username, serverInfo);
            PeerManager pm = new PeerManager();
            Environment env = new Environment(user, pm, true);
            File log = new File("log");
            env.getLogger().setLogFile(log);

            pm.setEnv(env);
            env.setInfo(info);
            env.setIdentityKey(idKeyStore);
            if(isRoot) env.setRootKey(rootKeyStore);
            info.env(env);

            FileManager fm = new FileManager();
            FileSharer fs = new FileSharer();


            SyncManager sm = new SyncManager();

            env.setFM(fm);
            env.setFS(fs);
            env.setDirectory(sharedFiles);
            env.setConfigs(configs);
            fs.setEnv(env);
            fm.setEnv(env);
            pm.setEnv(env);
            sm.setEnv(env);
            env.setSyncManager(sm);

            fm.initializeFiles(username);

            env.getPm().start();
            return env;
        }
    }

    void deleteNetwork() {
        String networkName = "unknown";
        try {
            int selectedRow = table.getSelectedRow();
            networkName = (String) table.getValueAt(selectedRow, 0);

            SavedNetwork[] savedNetworks = loadSavedNetworks();
            for (SavedNetwork savedNetwork : savedNetworks) {
                if(savedNetwork.getName().equals(networkName)) {
                    File base = baseDirForNetwork(networkName);
                    FileUtils.cleanDirectory(base);
                    FileUtils.deleteDirectory(base);

                    String finalNetworkName = networkName;
                    SavedNetwork[] newSavedNetworks =
                            Arrays.stream(savedNetworks)
                            .filter(s -> !s.getName().equals(finalNetworkName))
                            .toArray(SavedNetwork[]::new);

                    ObjectMapper mapper = new ObjectMapper();
                    mapper.writeValue(savedNetworksFile(), newSavedNetworks);

                    Object[][] data = Arrays.stream(loadSavedNetworks())
                            .map(e-> new Object[]{e.getName()})
                            .toArray(Object[][]::new);
                    DefaultTableModel model = (DefaultTableModel)(table.getModel());
                    model.setRowCount(0);
                    for (Object[] datum : data) {
                        model.addRow(datum);
                    }
                    return;
                }
            }
            throw new Exception("Network not found");
        }catch (Exception e) {
            JOptionPane.showMessageDialog(
                    null,
                    String.format("Error deleting network '%s': %s", networkName, e.getMessage())
            );
            e.printStackTrace();
        }
    }

    void connect() {
        String networkName = "unknown";
        try {
            int selectedRow = table.getSelectedRow();
            networkName = (String) table.getValueAt(selectedRow, 0);

            SavedNetwork[] savedNetworks = loadSavedNetworks();
            for (SavedNetwork savedNetwork : savedNetworks) {
                if(savedNetwork.getName().equals(networkName)) {
                    Environment env = savedNetwork.start();

                    NetworkView.createAndShowNetwork(env);
                    setVisible(false);
                    frame.dispose();
                    return;
                }
            }
            throw new Exception("Network not found");
        }catch (Exception e) {
            JOptionPane.showMessageDialog(
                    null,
                    String.format("Error loading network '%s': %s", networkName, e.getMessage())
            );
            e.printStackTrace();
        }
    }

    public static void selectFile(File f) throws Exception {

    }

    void addNew() {
        JFileChooser fc = new JFileChooser();
        int returnVal = fc.showOpenDialog(frame);
        File selectedFile;
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            selectedFile = fc.getSelectedFile();
            addNetworkToList(selectedFile);
        }
    }

    void createNew() {
        new CreateNewNetworkView(this, frame);
    }
    JFrame frame;
    final JTable table;
    public StartScreen(JFrame frame) throws Exception {
        super();
        this.frame = frame;
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        JPanel listedProfiles = new JPanel();
        Object[][] data = Arrays.stream(loadSavedNetworks())
                .map(e-> new Object[]{e.getName()})
                .toArray(Object[][]::new);
        table = new JTable(new DefaultTableModel());
        DefaultTableModel m = (DefaultTableModel)table.getModel();
        m.setColumnIdentifiers(new String[]{"Network Name"});
        m.setRowCount(0);
        for (Object[] datum : data) {
            m.addRow(datum);
        }
        table.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent mouseEvent) {
                JTable table =(JTable) mouseEvent.getSource();
                Point point = mouseEvent.getPoint();
                int row = table.rowAtPoint(point);
                if (mouseEvent.getClickCount() == 2 && table.getSelectedRow() != -1) {
                    connect();
                }
            }
        });
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setPreferredScrollableViewportSize(new Dimension(600, 400));

        //Create the scroll pane and add the table to it.
        JScrollPane scrollPane = new JScrollPane(table);

        //Add the scroll pane to this panel.
        listedProfiles.add(scrollPane);
        //panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        add(new JLabel("Networks"));
        add(listedProfiles);

        JButton connectButton = new JButton("Connect to Selected Network");
        connectButton.addActionListener((e) -> {
            SwingUtilities.invokeLater(() -> {
                connect();
            });
        });


        JButton deleteButton = new JButton("Delete Selected Network");
        deleteButton.addActionListener((e) -> {
            SwingUtilities.invokeLater(() -> {
                deleteNetwork();
            });
        });

        JButton addNewButton = new JButton("Add Network from Bootstrap File");
        addNewButton.addActionListener((e) ->{
            SwingUtilities.invokeLater(() -> {
                addNew();
            });
        });

        JButton createNewButton = new JButton("Create New Network");
        createNewButton.addActionListener((e) -> {
            SwingUtilities.invokeLater(() -> {
                createNew();
            });
        });
        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new GridLayout(0, 4));
        buttonsPanel.add(connectButton);
        buttonsPanel.add(deleteButton);
        buttonsPanel.add(addNewButton);
        buttonsPanel.add(createNewButton);
        add(buttonsPanel);

        setVisible(true);
    }
}
