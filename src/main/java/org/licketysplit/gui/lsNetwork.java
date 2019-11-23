package org.licketysplit.gui;

import java.io.*;
import java.net.*;
import java.util.*;
import static java.lang.System.out;
    public class lsNetwork {

        public static void main(String args[]) throws SocketException {

            //Form form = getIP4();
            getIP4(0);
            /*List<String> ls = getLocalInetAddress();
            for(String s: ls) {
                out.println(s);
            }*/
            return;
            }
        public static void printIP4() throws SocketException {
            final String ipadd;

            Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface netint : Collections.list(nets))
                if(netint.isUp()&&!netint.isLoopback()) {
                    displayInterfaceInformation(netint);
                }
            return;
        }
        public static String getIP4(int i) throws SocketException {
            String ip;
            Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface netint : Collections.list(nets))
                if(netint.isUp()&&!netint.isLoopback()) {
                    ip = displayInterfaceInformation(netint);
                    return ip;
                }
            return null;
        }
        public static List<String> getLocalInetAddress() {
            List<String> inetAddressList = new ArrayList<String>();
            try {
                Enumeration<NetworkInterface> enumeration = NetworkInterface.getNetworkInterfaces();
                while (enumeration.hasMoreElements()) {
                    NetworkInterface networkInterface = enumeration.nextElement();
                    Enumeration<InetAddress> addrs = networkInterface.getInetAddresses();
                    while (addrs.hasMoreElements()) {
                        inetAddressList.add(addrs.nextElement().getHostAddress());
                    }
                }
            } catch (SocketException e) {
                throw new RuntimeException("get local inet address fail", e);
            }
            return inetAddressList;
        }


        static String displayInterfaceInformation(NetworkInterface netint) throws SocketException {


            Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
            out.printf("INT TYPE: %s ", netint.getDisplayName());
            out.printf("INT Name: %s\n", netint.getName());
            for (InetAddress inetAddress : Collections.list(inetAddresses)) {
                if (inetAddress instanceof Inet4Address){

                    out.printf("IPV4address: %s\n\n", inetAddress.toString().substring(1));
                    return inetAddress.toString().substring(1);
                    //continue;
                }
                out.println(("(o)(o) ->>> "+inetAddress ));
            }
            out.printf("\n");
            return null;
        }
    }

