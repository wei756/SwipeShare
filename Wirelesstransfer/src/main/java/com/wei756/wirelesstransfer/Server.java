package com.wei756.wirelesstransfer;

import java.io.*;
import java.net.*;

public class Server extends Device {

    ServerSocket soc;

    Socket client;

    FileOutputStream fout;

    String key;
    String clientAddress, clientName = "";
    String serverName = "데스크탑";

    public Server(int port, String key) {
        try {
            this.serverName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        System.out.println("I: 서버 이름: " + this.serverName);

        try {
            soc = new ServerSocket(port);
            this.key = key;
            System.out.println("I: " + port + "번 포트로 서버 시작됨");

            int attempt = 0;
            while (true) { // 연결 재시도를 위한 루프
                int status;
                if ((status = readyForClient()) == 0) {
                    if (standBy() == 0)
                        break;
                } else if(status == -2) { // TimeoutException
                    break;
                } else {
                    // 연결 재시도
                    attempt++;
                    if (attempt > 10) {
                        System.out.println("W: 클라이언트 연결 실패");
                    }
                }
            }
        } catch (IOException e) {

        } finally {
            if (soc != null) {
                try {
                    soc.close();
                } catch (IOException ex) {
                }
            }
        }
        System.out.println("I: 서버 종료");
    }

    /**
     * 클라이언트와 연결이 되었을 때 작업
     */
    @Override
    protected int standBy() {
        while (true) { // 명령 입력을 위한 루프
            String[] cmd = getCommand();
            String exec = cmd[0];
            String content = cmd[1];

            if ("HELLO".equals(exec)) {
                if (getClientInfo() != 0) {
                    sendCommand("BYE"); // 접속 종료
                }

            } else if ("SEND".equals(exec)) {
                int resultcode;
                for (int i = 0; i < 10; i++) {
                    resultcode = getFile(content);
                    if (resultcode == 0) {
                        sendCommand("GOOD"); // 수신 성공
                        break;
                    } else if (i < 9) {
                        sendCommand("BAD"); // 재시도
                    } else {
                        sendCommand("END"); // 시도 횟수 초과로 인한 수신 종료
                    }
                }

            } else if ("BYE".equals(exec)) {
                return 1;
            } else if ("ERR".equals(exec)) {
                System.out.println(("E: 통신 에러!!! : " + content));
                return -1;
            }

        }
    }

    /**
     * 클라이언트와의 연결을 대기합니다.
     *
     * @return
     */
    public int readyForClient() {
        try {
            soc.setSoTimeout(7200);
            client = soc.accept();                       //클라이언트의 접속을 받습니다.
            System.out.println("I: 클라이언트 연결됨!");

            in = null;
            out = null;
            fout = null;
            in = client.getInputStream();                   //클라이언트 스트림 개통
            out = client.getOutputStream();                 //클라이언트 스트림 개통
            din = new DataInputStream(in);                  //InputStream을 이용해 데이터 단위로 입력을 받는 DataInputStream을 개통합니다.
            dout = new DataOutputStream(out);               //OutputStream을 이용해 데이터 단위로 보내는 스트림을 개통합니다.
        } catch (IOException e) {
            if (e instanceof SocketTimeoutException) {
                System.out.println("W: 오랫동안 기기 연결이 감지되지 않아 서버를 종료합니다.");

                return -2;
            }else {
                System.out.println("E: 클라이언트와 연결하는 중 에러가 발생하였습니다!");
                e.printStackTrace();

                return -1;
            }
        }

        return 0;
    }

    /**
     * 클라이언트의 정보를 확인합니다.
     *
     * @return
     */
    public int getClientInfo() {
        String key = "";
        while (true) {
            String[] cmd = getCommand();
            String exec = cmd[0];
            String content = cmd[1];

            if ("KEY".equals(exec)) {
                key = content;
            } else if ("ADDR".equals(exec)) {
                this.clientAddress = content;
            } else if ("NAME".equals(exec)) {
                this.clientName = content;
            } else if ("OVER".equals(exec)) {
                break;
            }
        }
        if (!this.key.equals(key)) { // 올바르지 않은 키
            System.out.println("E: 신뢰할 수 없는 클라이언트입니다.");
            return -1;
        }

        sendCommand("HI", this.clientName);
        sendCommand("IM", this.serverName);
        return 0;
    }

    /**
     * 클라이언트로부터 파일을 받습니다.
     *
     * @return
     */
    int getFile(String filename) {
        try {
            int dataSize = 0; // 파일 전송 횟수 (per 1000bytes)
            String hash = null;

            while (true) {
                String[] cmd = getCommand();
                String exec = cmd[0];
                String content = cmd[1];

                if ("SIZE".equals(exec)) {
                    dataSize = Integer.parseInt(content);
                } else if ("HASH".equals(exec)) {
                    hash = content;
                } else if ("GO".equals(exec)) {
                    break;
                }
            }

            int pos = filename.lastIndexOf( "\\" );
            String name = filename.substring(pos + 1);    // 파일 이름 추출
            pos = filename.lastIndexOf( "/" );
            name = filename.substring(pos + 1);    // 파일 이름 추출
            System.out.println("filename:       " + name);
            File file = new File(name);                     // 파일 생성
            fout = new FileOutputStream(file);              // 파일 스트림 생성

            int datas = dataSize;
            System.out.println("dataSize:" + dataSize);
            byte[] buffer = new byte[1024];
            int len;

            // download data
            soc.setSoTimeout(2000);
            for (; dataSize > 0; dataSize--) {
                len = din.read(buffer);
                fout.write(buffer, 0, len);
            }
            System.out.println("I: OVER 대기");

            while (true) {
                String[] cmd = getCommand();
                String exec = cmd[0];
                String content = cmd[1];

                if ("OVER".equals(exec)) {
                    System.out.println("I: 수신 종료");
                    break;
                } else if ("ERR".equals(exec)) {
                    System.out.println(("E: 통신 에러!!! : " + content));
                    return -1;
                }
            }

            System.out.println("I: " + datas + " KB 수신");
            fout.flush();
            fout.close();
            System.out.println("I: 파일 무결성 검사를 시작합니다...");

            try {
                if (getHashFile(name).equals(hash)) { // 무결성 검증
                    System.out.println("I: 무결성 검사 성공!");
                } else {
                    System.out.println("I: 무결성 검사 실패");
                    return -2;
                }
            } catch (Exception e) {
                System.out.println("I: 실패");
                e.printStackTrace();
                return -2;
            }

        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
        return 0;
    }

}
