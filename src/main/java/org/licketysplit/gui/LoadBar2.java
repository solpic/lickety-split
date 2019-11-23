package org.licketysplit.gui;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;

public class LoadBar2 {

    LoadBar2(JFrame frame) {
        for (int i = 0; i <= 20; i++) {
            final int currentValue = i;
            try {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        System.out.println(("okay"));

                        //pb.setValue(10);


                    }

                });
                java.lang.Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }


        }
    }
}