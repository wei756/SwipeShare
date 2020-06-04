package com.wei756.swipeshare.wirelesstransfer;

import android.util.Log;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class Client extends Device {

    Socket socket;

    FileInputStream fin;

    String key;
    String serverAddress, serverName = "";
    int port;
    String clientAddress, clientName;

    public Client(String address, int port, String key) {
        try {
            this.serverAddress = address;
            this.port = port;
            this.key = key;
            this.clientAddress = InetAddress.getLocalHost().getHostAddress();
            this.clientName = "my galaxy";

            socket = new Socket(address, port);
            Log.i("Client", port + "번 포트로 클라이언트 시작됨");

            while (true) { // 연결 재시도를 위한 루프
                if (connect() == 0) {
                    if (sendClientInfo() != 0) {
                        break;
                    }
                    break;
                } else {
                    // 연결 재시도
                }
            }
        } catch (IOException e) {
            Log.e("Client", "서버를 연결하는 데 실패했습니다.");
            e.printStackTrace();
        }
    }

    /**
     * 서버와 연결이 되었을 때 작업
     */
    @Override
    protected int standBy() {
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));

        try {
            while (true) { // 명령 입력을 위한 루프
                System.out.print(serverName + "@" + serverAddress + "> ");
                String cmd = null, exec, content = "";
                cmd = input.readLine();
                if (cmd.contains(" ")) {
                    exec = cmd.substring(0, cmd.indexOf(" "));
                    content = cmd.substring(cmd.indexOf(" ") + 1);
                } else {
                    exec = cmd;
                }

                switch (exec) {
                    case "SEND":
                        int status = -1;
                        while(status == -1) {
                            status = sendFile(content);
                        }
                        break;

                    case "BYE":
                    case "EXIT":
                        sendCommand("BYE");
                        return 0;
                    default:
                        sendCommand(cmd);
                        break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public void bye() {
        sendCommand("BYE");
    }

    /**
     * 서버에 연결합니다.
     *
     * @return
     */
    public int connect() {
        try {
            in = socket.getInputStream();                   //서버 스트림 개통
            out = socket.getOutputStream();                 //서버 스트림 개통
            din = new DataInputStream(in);                  //InputStream을 이용해 데이터 단위로 입력을 받는 DataInputStream을 개통합니다.
            dout = new DataOutputStream(out);               //OutputStream을 이용해 데이터 단위로 보내는 스트림을 개통합니다.
            System.out.println("I: 서버 연결됨!");

        } catch (IOException e) {
            System.out.println("E: 서버에 연결하는 중 에러가 발생하였습니다!");
            e.printStackTrace();
            return -1;

        }

        return 0;
    }

    /**
     * 서버로 클라이언트의 정보를 전송합니다.
     *
     * @return
     */
    public int sendClientInfo() {
        sendCommand("HELLO");
        sendCommand("ADDR", this.clientAddress);
        sendCommand("NAME", this.clientName);
        sendCommand("KEY", this.key);
        sendCommand("OVER");

        while (true) {
            String[] cmd = getCommand();
            String exec = cmd[0];
            String content = cmd[1];

            if ("HI".equals(exec)) {
                if (!this.clientName.equals(content)) { // 올바르지 않은 클라이언트 이름
                    System.out.println("W: 서버 측에서 확인한 내 이름이 정확하지 않습니다.");
                }
            } else if ("IM".equals(exec)) {
                this.serverName = content;
                return 0;
            } else if ("BYE".equals(exec)) {
                System.out.println("E: 서버와의 연결에 실패했습니다.");
                return -2;
            } else {
                System.out.println("V: " + exec + " " + content);
                System.out.println("E: 올바르지 않은 응답입니다.");
                return -1;
            }
        }
    }

    /**
     * 서버로 파일을 전송합니다.
     *
     * @return
     */
    public int sendFile(String filename) {
        try {
            fin = new FileInputStream(new File(filename));

            byte[] buffer = new byte[1024];
            int len;
            int dataSize = 0;

            while ((len = fin.read(buffer)) > 0) {
                dataSize++;
            }

            fin.close();

            int datas = dataSize;

            fin = new FileInputStream(filename);
            sendCommand("SEND", filename);
            sendCommand("SIZE", dataSize);
            sendCommand("HASH", getHashFile(filename));

            sendCommand("GO");
            len = 0;
            for (; dataSize > 0; dataSize--) {                   //데이터를 읽어올 횟수만큼 FileInputStream에서 파일의 내용을 읽어옵니다.
                len = fin.read(buffer);        //FileInputStream을 통해 파일에서 입력받은 데이터를 버퍼에 임시저장하고 그 길이를 측정합니다.
                dout.write(buffer, 0, len);       //서버에게 파일의 정보(1kbyte만큼보내고, 그 길이를 보냅니다.
            }
            Thread.sleep(1000);
            sendCommand("OVER");

            System.out.println("I: " + datas + "KB 송신 완료");

            while (true) {
                String[] cmd = getCommand();
                String exec = cmd[0];
                String content = cmd[1];

                if ("GOOD".equals(exec)) {
                    System.out.println("I: 전송 성공!");
                    break;
                } else if ("BAD".equals(exec)) {
                    System.out.println("W: 전송 실패");
                    return -1;
                } else if ("END".equals(exec)) {
                    System.out.println("W: 전송 실패(재시도 횟수 초과)");
                    System.out.println("W: 파일 전송을 중단합니다.");
                    return -2;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
        return 0;
    }

}