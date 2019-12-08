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

/**
 * Entry GUI screen for the application.
 * This screen lists all the networks you have saved, and gives you various options
 * for interacting with those networks such as:
 * Connect to Network
 * Delete Network
 * Add New Network from Bootstrap File
 * Create New Network
 */
public class StartScreen extends JPanel {
    /**
     * Get a File object for information about saved networks.
     * This file points to where saved network information is stored
     *
     * @return the file
     * @throws Exception the exception
     */
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

    /**
     * Parse the savedNetworksFile from JSON into an array.
     *
     * @return the saved networks array.
     * @throws Exception the exception
     */
    SavedNetwork[] loadSavedNetworks() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(savedNetworksFile(), SavedNetwork[].class);
    }


    /**
     * Adds a network from a bootstrap file and updates table.
     *
     * @param bootstrap the bootstrap file
     */
    void addNetworkToList(File bootstrap)  {
        try {
            // Gets a name for display purposes
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

    /**
     * Callback for when a network is created, we simply add it to the network list from the bootstrap file 'bootstrap'
     *
     * @param bootstrap the bootstrap file
     */
    public void createdNetwork(File bootstrap) {
        addNetworkToList(bootstrap);
    }


    /**
     * Helper function to unzip a zip file at zipFilePath into destination directory destDir
     * @param zipFilePath target ZIP file
     * @param destDir where to unzip to
     */
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

    /**
     * Helper function that checks that 'path' is a directory, and if it does not exist make a new directory
     * at location 'path'
     *
     * @param path the path
     * @throws Exception the exception
     */
    void checkNotFileAndMkdir(File path) throws Exception {
        if (path.isFile()) {
            throw new Exception(String.format("File '%s' exists, need access as directory", path.getAbsolutePath()));
        }
        if(!path.exists()) {
            path.mkdir();
        }
    }

    /**
     * Gets the directory where a network named 'name' is located
     * Creates necessary folders if they don't exist
     *
     * @param name name of the network
     * @return file pointing to base dir of network
     * @throws Exception the exception
     */
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

    /**
     * Initializes a network from a bootstrap ZIP file.
     * Copies various files and creates various directories so that later on
     * the network can be easily loaded. Returns a SavedNetwork data structure
     * which can launch the PeerManager directly.
     *
     * @param name      name of the network
     * @param bootstrap the bootstrap file
     * @return SavedNetwork object representing network
     * @throws Exception the exception
     */
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


    /**
     * Helper class to keep track of information about a users saved
     * networks. Can also start the peer manager, which will start the
     * P2P aspect of the application
     */
    public static class SavedNetwork {
        /**
         * The Id key file location.
         */
        String idKey;
        /**
         * The Root key file location (only exists if you are a root)
         */
        String rootKey;
        /**
         * The peer info dir file location.
         */
        String infoDir;
        /**
         * The User info file location.
         */
        String userInfo;
        /**
         * The Shared files directory location.
         */
        String sharedFiles;
        /**
         * The Configs directory location.
         */
        String configs;
        /**
         * Name of this network.
         */
        String name;

        /**
         * Gets user info file location.
         *
         * @return the user info
         */
        public String getUserInfo() {
            return userInfo;
        }

        /**
         * Sets user info.
         *
         * @param userInfo the user info
         */
        public void setUserInfo(String userInfo) {
            this.userInfo = userInfo;
        }

        /**
         * Gets name.
         *
         * @return the name
         */
        public String getName() {
            return name;
        }

        /**
         * Sets name.
         *
         * @param name the name
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * Instantiates a new Saved network.
         */
        public SavedNetwork() {
        }

        /**
         * Gets id key.
         *
         * @return the id key
         */
        public String getIdKey() {
            return idKey;
        }

        /**
         * Sets id key.
         *
         * @param idKey the id key
         */
        public void setIdKey(String idKey) {
            this.idKey = idKey;
        }

        /**
         * Gets root key.
         *
         * @return the root key
         */
        public String getRootKey() {
            return rootKey;
        }

        /**
         * Sets root key.
         *
         * @param rootKey the root key
         */
        public void setRootKey(String rootKey) {
            this.rootKey = rootKey;
        }

        /**
         * Gets info dir.
         *
         * @return the info dir
         */
        public String getInfoDir() {
            return infoDir;
        }

        /**
         * Sets info dir.
         *
         * @param infoDir the info dir
         */
        public void setInfoDir(String infoDir) {
            this.infoDir = infoDir;
        }

        /**
         * Gets shared files.
         *
         * @return the shared files
         */
        public String getSharedFiles() {
            return sharedFiles;
        }

        /**
         * Sets shared files.
         *
         * @param sharedFiles the shared files
         */
        public void setSharedFiles(String sharedFiles) {
            this.sharedFiles = sharedFiles;
        }

        /**
         * Gets configs.
         *
         * @return the configs
         */
        public String getConfigs() {
            return configs;
        }

        /**
         * Sets configs.
         *
         * @param configs the configs
         */
        public void setConfigs(String configs) {
            this.configs = configs;
        }

        /**
         * Uses stored info about user info file,
         * peer info directory, ID key,
         * root key (if you are root) to load
         * relevant data structures from file
         * and start the PeerManager with a call to
         * env.getPm().start()
         *
         * This will connect you to all peers on the network
         * and allow you to upload and download files.
         *
         * @return the initialized environment
         * @throws Exception the exception
         */
        public Environment start() throws Exception{
            // Load peer info dir from file
            PeerInfoDirectory info = new PeerInfoDirectory(infoDir);
            info.load();

            // Load username, IP address and port from file
            ObjectMapper objectMapper = new ObjectMapper();
            HashMap hashMap = objectMapper.readValue(new File(userInfo), HashMap.class);
            String username = (String)hashMap.get("username");
            String ip = (String)hashMap.get("ip");
            String port = (String)hashMap.get("port");

            // If you are root load root key from file
            boolean isRoot = rootKey!=null;

            KeyStore rootKeyStore = null;
            if(isRoot) {
                rootKeyStore = new KeyStore(rootKey);
                rootKeyStore.load();
            }

            // Load ID key from file
            KeyStore idKeyStore = new KeyStore(idKey);
            idKeyStore.load();

            // Initialize relevant singletons
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

            // Start peer manager, this listens for new connections at your IP/port
            // and tries to connect to all known peers
            // You will also start serving downloads if other peers ask for them
            env.getPm().start();
            return env;
        }
    }

    /**
     * Delete network that is currently selected in the table.
     */
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

    /**
     * Connect to network that is selected in the table.
     */
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

    /**
     * Adds a new network from a bootstrap file by opening a file chooser.
     */
    void addNew() {
        JFileChooser fc = new JFileChooser();
        int returnVal = fc.showOpenDialog(frame);
        File selectedFile;
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            selectedFile = fc.getSelectedFile();
            addNetworkToList(selectedFile);
        }
    }

    /**
     * Opens a CreateNewNetworkView to create a new network.
     */
    void createNew() {
        new CreateNewNetworkView(this, frame);
    }

    /**
     * Parent JFrame.
     */
    JFrame frame;
    /**
     * Table listing networks
     */
    final JTable table;

    /**
     * Creates the view and adds necessary components
     *
     * @param frame parent JFrame
     * @throws Exception the exception
     */
    public StartScreen(JFrame frame) throws Exception {
        super();
        this.frame = frame;
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        // Load saved networks into table
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

        // Add double click listener to connect to a network
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

        // Add connect to network button
        JButton connectButton = new JButton("Connect to Selected Network");
        connectButton.addActionListener((e) -> {
            SwingUtilities.invokeLater(() -> {
                connect();
            });
        });

        // Add delete network button
        JButton deleteButton = new JButton("Delete Selected Network");
        deleteButton.addActionListener((e) -> {
            SwingUtilities.invokeLater(() -> {
                deleteNetwork();
            });
        });

        // Add 'Add Network ...' button
        JButton addNewButton = new JButton("Add Network from Bootstrap File");
        addNewButton.addActionListener((e) ->{
            SwingUtilities.invokeLater(() -> {
                addNew();
            });
        });

        // Add Create New Network button
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
