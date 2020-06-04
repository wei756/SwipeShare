package com.wei756.swipeshare.wirelesstransfer;

import java.io.*;
import java.net.InetAddress;

public class Main {

    public static void main(String[] args) throws Exception {
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));

        while (true) {
            System.out.print("root> ");
            String cmd = input.readLine();
            String addr, key;
            int port;
            switch (cmd) {
                case "open server":
                    port = getPort(input);
                    key = "" + (Math.round(Math.random() * 89999999) + 10000000);
                    System.out.println("I: 포트: " + port);
                    System.out.println("I: 키: " + key);
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