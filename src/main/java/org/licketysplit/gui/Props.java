package org.licketysplit.gui;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Date;
import java.net.URI;


public class Props {

	public static final Class<?>[] COLUMN_CLASSES = new Class[] { /*URI.class, */String.class,
			Long.class, Date.class, Boolean.class, Boolean.class, Boolean.class };


	public static final String LF = "Windows";
	//public static final InetAddress inetaddress = InetAddress.getLocalHost();
	/*static {
		try {
			final InetAddress inetaddress = InetAddress.getLocalHost();
			System.out.println(inetaddress);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}*/

}

/*

JMenu menu = new JMenu("File");
        menu.setMnemonic(KeyEvent.VK_W);
        JMenuItem item = null;
        //close menu item
        item = new JMenuItem("Sign Out");
        //item.setMnemonic(KeyEvent.VK_C);
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.out.println("User Signing Out");
                    frame.setVisible(false);
                    frame.dispose();
            }
        });
        menu.add(item);
        //new
        setOnline(false);
        item = new JMenuItem("Go" + (isOnline() ? " online" : " offline"));
        item.setMnemonic(KeyEvent.VK_N);
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.out.println("Connecting to Peers");
                //framework.makeNewWindow();
            }
        });
        menu.add(item);

        //quit
        item = new JMenuItem("Quit");
        item.setMnemonic(KeyEvent.VK_Q);
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.out.println("Quit request");
                //framework.quit(MyFrame.this);
            }
        });
        menu.add(item);

        JMenu menu1 = MenuUtil.makeMenu1();






 */
