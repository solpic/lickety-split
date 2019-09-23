package org.licketysplit.gui;

import javax.swing.*;

public class main_g {
    public static void main (String[] args){
        SwingUtilities.invokeLater(new Runnable() {
                                       @Override
                                       public void run() {
                                           GUI myGui= new GUI();
                                           myGui.setVisible(true);
                                       }

                                   }
        );
    }
}
