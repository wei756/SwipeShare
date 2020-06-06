package com.wei756.wirelesstransfer;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.InetAddress;

public class Main {

    public static final String VERSION = "1.0";

    public static void main(String[] args) throws Exception {
        String addr, key;
        int port;

        System.out.print("root> ");

        if (args.length > 0) {
            if ("--server".equals(args[0])) {
                port = (int)Math.round(Math.random() * 1000 + 32000);
                key = "" + (Math.round(Math.random() * 89999999) + 10000000);
                System.out.println("I: 포트: " + port);
                System.out.println("I: 키: " + key);

                ConnectDialog connectDialog = new ConnectDialog(InetAddress.getLocalHost().getHostAddress(), "" + port, key);
                connectDialog.setVisible(true);

                Server server = new Server(port, key);
            } else if ("--client".equals(args[0])) {
                BufferedReader input = new BufferedReader(new InputStreamReader(System.in));

                System.out.print("address? ");
                addr = input.readLine();
                port = getPort(input);
                System.out.print("key? ");
                key = input.readLine();

                System.out.println("I: 포트: " + port);
                System.out.println("I: 키: " + key);
                Client client = new Client(addr, port, key);
            } else {
                System.out.println("SwipeShare v" + VERSION);
                System.out.println("\t --server\t\tStart as receiver.");
                System.out.println("\t --client\t\tStart as sender.");
            }
        }
        System.exit(0);
    }
    public static void test() throws Exception {
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
        String cmd;
        while (true) {
            System.out.print("root> ");
            cmd = input.readLine();
            String addr, key;
            int port;
            switch (cmd) {
                case "open server":
                    port = (int)Math.round(Math.random() * 1000 + 32000);
                    key = "" + (Math.round(Math.random() * 89999999) + 10000000);
                    System.out.println("I: 포트: " + port);
                    System.out.println("I: 키: " + key);

                    ConnectDialog connectDialog = new ConnectDialog(InetAddress.getLocalHost().getHostAddress(), "" + port, key);
                    connectDialog.setVisible(true);

                    Server server = new Server(port, key);
                    break;

                case "connect":
                    System.out.print("address? ");
                    addr = input.readLine();
                    port = getPort(input);
                    System.out.print("key? ");
                    key = input.readLine();

                    System.out.println("I: 포트: " + port);
                    System.out.println("I: 키: " + key);
                    Client client = new Client(addr, port, key);
                    break;

                case "exit":
                    System.exit(0);
                    break;
            }
        }
    }

    static int getPort(BufferedReader input) {
        int port = 25566;
        try {
            while(true) {
                System.out.print("port? ");
                String userInput = input.readLine();

                if (!"".equals(userInput.trim())) {
                    try {
                        port = Integer.parseInt(userInput);
                        if (port > 0 && port < 65536)
                            break;
                        else
                            System.out.println("W: 올바르지 않은 포트 범위입니다.(1~65535)");
                    } catch (NumberFormatException nfe) {
                        System.out.println("W: 올바르지 않은 포트입니다.");
                    }
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        return port;
    }

}