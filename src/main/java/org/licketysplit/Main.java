package org.licketysplit;

import org.licketysplit.gui.NetworkView;
import org.licketysplit.testing.TestRunner;

/**
 * Application entry point,
 * and test entry point if running simulations.
 */
public class Main {
    public static void loadMainGUI() throws Exception {

    }

    public static void doubleStart(String []args) throws Exception {

    }

    /**
     * Application entry point, loads network start screen if normal usage.
     * If simulation/test, loads relevant TestRunner entry point.
     *
     * @param args the args
     * @throws Exception the exception
     */
    public static void main(String []args) throws Exception{
        if(args.length==0) {
            NetworkView.main(args);
        }else if(args.length==1) {
            for(int i= 0; i<Integer.parseInt(args[0]); i++) {
                new Thread(() -> {
                    NetworkView.main(new String[]{});
                }).start();
            }
        }else{
            TestRunner tester = new TestRunner();
            tester.run(args);
        }
    }
}
