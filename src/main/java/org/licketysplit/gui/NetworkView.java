/* From http://java.sun.com/docs/books/tutorial/index.html */

package org.licketysplit.gui;


import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.licketysplit.env.Environment;
import org.licketysplit.filesharer.DownloadManager;
import org.licketysplit.syncmanager.FileInfo;
import org.licketysplit.syncmanager.FileManager;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

/**
 * TableDemo is just like SimpleTableDemo, except that it uses a custom
 * TableModel.
 */
public class NetworkView extends JPanel {
    private boolean DEBUG = false;

    JTable fileTable;
    Environment env;
    JLabel statusLabel;
    ConcurrentHashMap<String, FileInfo> fileInfo = new ConcurrentHashMap<>();

    synchronized void uploadNewFile() {
        new NewFileView(env, frame);
    }

    synchronized void banUser() {
        new BanUserView(env, frame);
    }

    synchronized void newUser() {

        new NewUserView(env, frame);
    }

    synchronized void listUsers() {
        new ListUsers(env, frame);
    }

    public File downloadsFolder() throws Exception {
        File downloads = new File("Downloads");
        if(downloads.isFile()) {
            throw new Exception("Need access to 'Downloads' as file");
        }
        if(!downloads.exists()) {
            downloads.mkdir();
        }

        return downloads;
    }

    synchronized void disconnect() {
        Map<Thread, StackTraceElement[]> threads = Thread.getAllStackTraces();
        Thread curr = Thread.currentThread();
        for (Thread thread : threads.keySet()) {
            if(!thread.equals(curr)) {
                thread.stop();
            }
        }

        NetworkView.main(new String[]{});
    }

    JFrame frame;
    public NetworkView(JFrame frame, Environment env) {
        super(new GridLayout(1, 0));
        this.frame = frame;

        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("Actions");

        JMenuItem addFileItem = new JMenuItem("Upload new file");
        addFileItem.addActionListener((ActionEvent e) -> {
            SwingUtilities.invokeLater(() -> {
                uploadNewFile();
            });
        });
        menu.add(addFileItem);
        menu.addSeparator();
        JMenuItem listUsersItem = new JMenuItem("List Users");
        listUsersItem.addActionListener((ActionEvent e) -> {
            SwingUtilities.invokeLater(() -> {
                listUsers();
            });
        });
        menu.add(listUsersItem);

        if(env.getRootKey()!=null) {
            JMenuItem banUserItem = new JMenuItem("Ban User");
            banUserItem.addActionListener((ActionEvent e) -> {
                SwingUtilities.invokeLater(() -> {
                    banUser();
                });
            });
            menu.add(banUserItem);

            JMenuItem newUserItem = new JMenuItem("New User");
            newUserItem.addActionListener((ActionEvent e) -> {
                SwingUtilities.invokeLater(() -> {
                    newUser();
                });
            });
            menu.add(newUserItem);
        }

        menu.addSeparator();
        JMenuItem refreshItem = new JMenuItem("Refresh");

        refreshItem.addActionListener((a) -> {
            SwingUtilities.invokeLater(() -> {
                try {
                    env.getSyncManager().syncManifests();
                    updateTable();
                } catch(Exception e) {
                    e.printStackTrace();
                }
            });
        });
        menu.add(refreshItem);
        JMenuItem disconnectItem = new JMenuItem("Exit");
        disconnectItem.addActionListener((e) -> {
            SwingUtilities.invokeLater(() -> {
                disconnect();
            });
        });
        menu.add(disconnectItem);


        menuBar.add(menu);
        frame.setJMenuBar(menuBar);

        this.env = env;
        env.getFM().setChangeHandler(() -> {
            SwingUtilities.invokeLater(() -> {
                updateTable();
            });
        });

        env.getFM().setDownloadChanged((file) -> {
            SwingUtilities.invokeLater(() -> {
                updateTableEntry(file.name, file);
            });
        });
        this.setLayout(new BorderLayout());

        fileTable = new JTable(createTableModel());
        for(int i = 0; i<fileTable.getColumnCount(); i++) {

            fileTable.getColumnModel().getColumn(i).setCellRenderer(
                    new DefaultTableCellRenderer() {
                        @Override
                        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {

                            //Cells are by default rendered as a JLabel.
                            JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);

                            //Get the status for the current row.
                            DefaultTableModel m = (DefaultTableModel) table.getModel();
                            Object[] vals = rows.get(m.getValueAt(row, 0));

                            Color fg = Color.GRAY;
                            try {
                                FileStatusCode s = statusFromStatusString((String) vals[2]);
                                switch (s) {
                                    case HASFILE:
                                        fg = Color.BLACK;
                                        break;
                                    case DOESNT_HAVE:
                                        fg = Color.GRAY;
                                        break;
                                    case DOWNLOADING:
                                        fg = Color.BLUE;
                                        break;
                                    case CORRUPTED:
                                        fg = Color.RED;
                                        break;
                                    default:
                                        fg = Color.PINK;
                                }
                            }catch(Exception e) {
                            }
                            l.setForeground(fg);

                            //Return the JLabel which renders the cell.
                            return l;
                        }
                    });
        }
        fileTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        fileTable.setPreferredScrollableViewportSize(new Dimension(600, 400));

        //Create the scroll pane and add the table to it.
        JScrollPane scrollPane = new JScrollPane(fileTable);

        //Add the scroll pane to this panel.
        add(scrollPane);
        updateTable();

        final JPopupMenu popup = new JPopupMenu();
        JMenuItem downloadItem = new JMenuItem("Download");
        downloadItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedRow = fileTable.getSelectedRow();
                String filename = (String)fileTable.getValueAt(selectedRow, 0);
                env.getFS().download(fileInfo.get(filename));
            }
        });
        popup.add(downloadItem);

        JMenuItem deleteItem = new JMenuItem("Delete File");
        deleteItem.addActionListener((a) -> {
            try {
                int selectedRow = fileTable.getSelectedRow();
                String filename = (String) fileTable.getValueAt(selectedRow, 0);
                env.getSyncManager().deleteFile(filename);
            } catch(Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(frame,
                        String.format("Error deleting file: %s", e.getMessage()));
            }
            //env.getFS().download(fileInfo.get(filename));
        });
        popup.add(deleteItem);

        JMenuItem cancelDownloadItem = new JMenuItem("Cancel Download");
        cancelDownloadItem.addActionListener((a) -> {
            try {
                int selectedRow = fileTable.getSelectedRow();
                String filename = (String) fileTable.getValueAt(selectedRow, 0);
                DownloadManager dm = env.getFS().currentProgress(filename);
                if(dm!=null)
                    dm.cancelDownload();
                else
                    throw new Exception("No download in progress");
            } catch(Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(frame,
                        String.format("Error cancelling download: %s", e.getMessage()));
            }
            //env.getFS().download(fileInfo.get(filename));
        });
        popup.add(cancelDownloadItem);

        JMenuItem updateItem = new JMenuItem("Update file");
        updateItem.addActionListener((a) -> {
            try {
                int selectedRow = fileTable.getSelectedRow();
                String dst = (String) fileTable.getValueAt(selectedRow, 0);
                JFileChooser fc = new JFileChooser();
                int returnVal = fc.showOpenDialog(frame);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File src = fc.getSelectedFile();
                    env.getSyncManager().updateFile(dst, src.getPath());
                }
            } catch(Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(frame,
                        String.format("Error updating file: %s", e.getMessage()));
            }
            //env.getFS().download(fileInfo.get(filename));
        });
        popup.add(updateItem);

        JMenuItem viewFileItem = new JMenuItem("Create Local Copy and View");
        viewFileItem.addActionListener((a) -> {
            try {
                int selectedRow = fileTable.getSelectedRow();
                String dst = (String) fileTable.getValueAt(selectedRow, 0);
                FileStatusCode fileStatusCode = statusFromStatusString((String)fileTable.getValueAt(selectedRow, 2));
                if(fileStatusCode==FileStatusCode.HASFILE) {
                    String src = env.getFM().getSharedDirectoryPath(dst);
                    File downloads = downloadsFolder();
                    Path dstFile = Paths.get(downloads.getPath(), new File(dst).getName());
                    FileUtils.copyFile(new File(src), dstFile.toFile());
                    viewFileInExplorer(dstFile.toFile());
                }else{
                    throw new Exception("File isn't downloaded");
                }
            } catch(Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(frame,
                        String.format("Error viewing file: %s", e.getMessage()));
            }
        });

        popup.add(viewFileItem);


        popup.addPopupMenuListener(new PopupMenuListener() {

            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        System.out.println("Changing selection");
                        int rowAtPoint = fileTable.rowAtPoint(SwingUtilities.convertPoint(popup, new Point(0, 0), fileTable));
                        if (rowAtPoint > -1) {
                            fileTable.setRowSelectionInterval(rowAtPoint, rowAtPoint);
                        }
                    }
                });
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                // TODO Auto-generated method stub

            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
                // TODO Auto-generated method stub

            }
        });
        fileTable.setComponentPopupMenu(popup);
        JPanel statusPanel = new JPanel();
        statusPanel.setBorder(new BevelBorder(BevelBorder.LOWERED));
        this.add(statusPanel, BorderLayout.SOUTH);
        statusPanel.setPreferredSize(new Dimension(this.getWidth(), 16));
        statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.X_AXIS));
        statusLabel = new JLabel("status");
        statusLabel.setHorizontalAlignment(SwingConstants.LEFT);
        statusPanel.add(statusLabel);

        env.changes.setHandler("peerlist-changed", (arg) -> {
            SwingUtilities.invokeLater(() -> {
                statusLabel.setText(String.format("Connected to %d peers", env.getPm().getPeers().size()));
            });
        });
        env.changes.runHandler("peerlist-changed", 0);

        env.changes.setHandler("download-failed", (arg) -> {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(frame,
                        String.format("Download failed for file: %s", (String)arg));
            });
        });
    }


    public DefaultTableModel createTableModel() {
        DefaultTableModel m = new DefaultTableModel();

        m.setColumnIdentifiers(new String[] {"Name", "Size", "Status"});
        return m;
    }

    public static void viewFileInExplorer(File f) throws Exception {
        if (System.getProperty("os.name").contains("Windows")) {
            Runtime.getRuntime().exec("explorer.exe /select," + f.getAbsolutePath());
        }
    }

    enum FileStatusCode {
        DOWNLOADING,
        HASFILE,
        CORRUPTED,
        DOESNT_HAVE
    }
    FileStatusCode statusFromStatusString(String status) {
        if(status.contains("Downloading")) {
            return FileStatusCode.DOWNLOADING;
        }else if(status.contains("N/A")) {
            return FileStatusCode.DOESNT_HAVE;
        }else if(status.contains("Downloaded")) {
            return FileStatusCode.HASFILE;
        }else {
            return FileStatusCode.CORRUPTED;
        }
    }

    Random r = new Random(100);

    ConcurrentHashMap<String, Object[]> rows = new ConcurrentHashMap<String, Object[]>();
    final Object updateLocker = new Object();


    String formatSizeStr(float size) {
        if(size>1024*1024*1024) {
            return String.format("%.2f GB", size/(1024.0f*1024.0f*1024.0f));
        }else if(size>1024*1024) {
            return String.format("%.2f MB", size/(1024.0f*1024.0f));
        }else if(size>1024) {
            return String.format("%.2f KB", size/1024.0f);
        }else{
            return String.format("%.2f B", size);
        }
    }

    String formatSpeedStr(float downloadSpeed) {
        if(downloadSpeed>1024) {
            if(downloadSpeed>1024*1024) {
                //show in mb/s
                return String.format("%.2f MB/s", downloadSpeed/(1024.0f*1024.0f));
            }else {
                //Show in KB/s
                return String.format("%.2f KB/s", downloadSpeed/1024.0f);
            }
        }else{
            // Show in B/s
            return String.format("%.2f B/s", downloadSpeed);
        }
    }

    Object[] valsFromStatus(FileManager.FileStatus status) {
        Object[] obj = new Object[3];
        String statusStr = null;
        if (status.isDownloading) {
            statusStr = "Downloading ...";
            statusStr += " "+formatSpeedStr(status.downloadSpeed) +" "+ String.format("%.2f %%", status.downloadProgress * 100);
        } else if (status.hasFile) {
            if (status.fileCorrupted) {
                statusStr = "Corrupted";
            } else {
                statusStr = "Downloaded";
            }
        }else{
            statusStr = "N/A";
        }
        obj[0] = status.name;
        obj[1] = formatSizeStr(status.size);
        obj[2] = statusStr;
        return obj;
    }

    Lock updateTableEntryLock = new ReentrantLock();
    public void updateTableEntry(String name, FileInfo file) {
        if(updateTableEntryLock.tryLock()) {
            try {
                synchronized (updateLocker) {
                    if (!rows.containsKey(name)) return;
                    FileManager.FileStatus status = env.getFM().getFileStatus(file);
                    Object[] objects = valsFromStatus(status);
                    DefaultTableModel m = (DefaultTableModel) fileTable.getModel();
                    int rownumber = getRowNumber(m, name);
                    for (int i = 0; i < objects.length; i++) {
                        m.setValueAt(objects[i], rownumber, i);
                    }
                    rows.put(name, objects);
                }
            } catch(Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null,
                        String.format("Error updating table: %s", e.getMessage()));
            } finally {
                updateTableEntryLock.unlock();
            }
        }

    }
    int getRowNumber(DefaultTableModel m, String name) {
        for(int i = 0; i<m.getRowCount(); i++) {
            if(m.getValueAt(i, 0).equals(name)) return i;
        }
        return -1;
    }
    public void updateTableEntry(String name, Object[] vals) {
        synchronized (updateLocker) {
            DefaultTableModel m = (DefaultTableModel) fileTable.getModel();
            int rownumber = getRowNumber(m, name);
            for (int i = 0; i < vals.length; i++) {
                m.setValueAt(vals[i], rownumber, i);
            }
            rows.put(name, vals);
        }
    }
    void removeTableEntry(String name) {
        synchronized (updateLocker) {

            DefaultTableModel m = (DefaultTableModel) fileTable.getModel();
            int rownumber = getRowNumber(m, name);
            m.removeRow(rownumber);
            rows.remove(name);
        }
    }

    void addTableEntry(String name, Object[] vals) {
        synchronized (updateLocker) {
            DefaultTableModel m = (DefaultTableModel) fileTable.getModel();
            m.addRow(vals);
            rows.put(name, vals);
        }
    }
    Object updateLock = new Object();

    public void updateTable() {
        synchronized (updateLock) {
            try {
                JSONObject manifest = env.getFM().getManifest();
                System.out.println("Updating file list with manifest\n" + manifest.toString(4));
                DefaultTableModel m = (DefaultTableModel) fileTable.getModel();
                JSONArray files = (JSONArray) manifest.get("files");
                HashMap<String, Object[]> fileChanges = new HashMap<>();

                for (int i = 0; i < files.length(); i++) {
                    FileInfo file = new FileInfo(new JSONObject(files.get(i).toString()));
                    fileInfo.put(file.name, file);
                    if (!file.deleted) {
                        FileManager.FileStatus status = env.getFM().getFileStatus(file);
                        fileChanges.put(file.name, valsFromStatus(status));
                    }
                }
                // Delete missing
                for (int i = 0; i < m.getRowCount(); i++) {
                    String name = (String) m.getValueAt(i, 0);

                    if (!fileChanges.containsKey(name)) {
                        removeTableEntry(name);
                    } else {
                        updateTableEntry(name, fileChanges.get(name));
                        fileChanges.remove(name);
                    }
                }
                for (Map.Entry<String, Object[]> entry : fileChanges.entrySet()) {
                    addTableEntry(entry.getKey(), entry.getValue());
                }
                m.fireTableDataChanged();
            } catch (Exception e) {
                env.getLogger().log(Level.INFO,
                        "Error updating GUI",
                        e);
            }
        }
    }

    /**
     * Create the GUI and show it. For thread safety, this method should be
     * invoked from the event-dispatching thread.
     */
    private static void createAndShowGUI() {
        //Create and set up the window.
        JFrame frame = new JFrame("Lickety Split");
        frame.setLayout(new BorderLayout());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //Create and set up the content pane.
        StartScreen newContentPane = null;
        try {
            newContentPane = new StartScreen(frame);
        } catch(Exception e) {
            e.printStackTrace();
        }
        newContentPane.setOpaque(true); //content panes must be opaque
        frame.setContentPane(newContentPane);

        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }

    public static void createAndShowNetwork(Environment env) {
        //Create and set up the window.
        JFrame frame = new JFrame("Lickety Split");
        frame.setLayout(new BorderLayout());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //Create and set up the content pane.
        JPanel newContentPane = null;
        try {
            newContentPane = new NetworkView(frame, env);
        } catch(Exception e) {
            e.printStackTrace();
        }
        newContentPane.setOpaque(true); //content panes must be opaque
        frame.setContentPane(newContentPane);

        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }catch(Exception e) {
            e.printStackTrace();
        }
        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }
}

           
         
    