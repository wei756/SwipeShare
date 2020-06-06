package com.wei756.wirelesstransfer;

import java.io.*;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.util.Base64;
import java.util.Enumeration;

public class Client extends Device {

    FileInputStream fin;

    ContactService contactService;
    FileOutputService fileOutputService;

    public Client(String address, int port, String key) {
        try {
            this.hostAddress = address;
            this.port = port;
            this.key = key;
            this.guestAddress = getHostAddress();
            this.guestName = "";

            socket = new Socket(address, port);
            Log.i("Client", port + "번 포트로 클라이언트 시작됨");

            while (true) { // 연결 재시도를 위한 루프
                if (readyForHost() == 0) {
                    System.out.println("I: 인바운드 스레드 시작");
                    Client self = this;
                    inputRunnable = new InputRunnable() {
                        @Override
                        protected void init() {
                            services.add(contactService = new ContactService(self));
                            services.add(fileOutputService = new FileOutputService(self));
                        }
                    };
                    inputThread = new Thread(inputRunnable);
                    inputThread.start();
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

    public void bye() {
        sendCommand("BYE");
        inputRunnable.terminate();
    }

    /**
     * 서버에 연결합니다.
     *
     * @return
     */
    public int readyForHost() {
        try {
            socket.setSoTimeout(5000);
            in = socket.getInputStream();                   //서버 스트림 개통
            out = socket.getOutputStream();                 //서버 스트림 개통
            din = new DataInputStream(in);                  //InputStream을 이용해 데이터 단위로 입력을 받는 DataInputStream을 개통합니다.
            dout = new DataOutputStream(out);               //OutputStream을 이용해 데이터 단위로 보내는 스트림을 개통합니다.
            System.out.println("I: 서버 연결됨!");
            socket.setSoTimeout(100);

        } catch (IOException e) {
            System.out.println("E: 서버에 연결하는 중 에러가 발생하였습니다!");
            e.printStackTrace();
            return -1;
        }

        return 0;
    }

    /**
     * 서버로 게스트의 정보를 전송합니다.
     *
     * @return
     */
    public void sendClientInfo() {
        contactService.sendClientInfo();
    }

    /**
     * 서버로 파일을 전송합니다.
     *
     * @return
     */
    public void sendFile(String filename) {
        fileOutputService.sendFile(filename);
    }

    String getHostAddress() {
        //Device에 있는 모든 네트워크에 대해 뺑뺑이를 돕니다.
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();

                //네트워크 중에서 IP가 할당된 넘들에 대해서 뺑뺑이를 한 번 더 돕니다.
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {

                    InetAddress inetAddress = enumIpAddr.nextElement();

                    if (inetAddress.isLoopbackAddress()) {
                        Log.i("IPAddress", intf.getDisplayName() + "(loopback) | " + inetAddress.getHostAddress());
                    } else {
                        Log.i("IPAddress", intf.getDisplayName() + " | " + inetAddress.getHostAddress());
                    }

                    //루프백이 아니고, IPv4가 맞다면 리턴~~~
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        return inetAddress.getHostAddress().toString();
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 서버 연결 직후 연결을 인증하거나 연결 해제 시 쓰입니다.
     */
    class ContactService extends InputService {
        String tmpGuestName;

        /**
         * 부모 인스턴스를 전달합니다.
         *
         * @param parent
         */
        ContactService(Device parent) {
            super(parent);
        }

        /**
         * 클라이언트의 정보를 확인합니다.
         *
         * @return
         */
        @Override
        public int onEnabled(String exec, String content, String raw) {
            if ("BYE".equals(exec)) {
                return SUCCESS;
            }
            return PASS;
        }

        /**
         * 클라이언트의 정보 확인을 시작합니다.
         *
         * @return
         */
        @Override
        public int onDisabled(String exec, String content, String raw) {

            if ("HI".equals(exec)) {
                tmpGuestName = content;
            } else if ("IM".equals(exec)) {
                hostName = content;
                if (!guestName.equals(tmpGuestName)) { // 올바르지 않은 클라이언트 이름
                    System.out.println("W: 서버 측에서 확인한 내 이름이 정확하지 않습니다.");
                    return PASS;
                } else
                    return SUCCESS;
            } else {
                System.out.println("V: " + exec + " " + content);
                System.out.println("E: 올바르지 않은 응답입니다.");
            }
            return PASS;
        }

        /**
         * 서버로 클라이언트의 정보를 전송합니다.
         *
         * @return
         */
        public void sendClientInfo() {
            sendCommand("HELLO");
            sendCommand("ADDR", parent.guestAddress);
            sendCommand("NAME", parent.guestName);
            sendCommand("KEY", parent.key);
            sendCommand("OVER");
        }
    }

    /**
     * 파일을 전송하는 데 쓰입니다.
     */
    class FileOutputService extends InputService {
        final int FAILED_INVAILD_HASH = -2;

        FileInputStream fin;

        String key;

        boolean transfer = false;
        String filename; // 파일 이름
        int dataSize = 0; // 파일 전송 횟수 (per 1000bytes)
        String hash = null; // 파일 해시

        /**
         * 부모 인스턴스를 전달합니다.
         *
         * @param parent
         */
        FileOutputService(Device parent) {
            super(parent);
        }

        /**
         * 파일을 전송합니다.
         *
         * @return
         */
        @Override
        public int onEnabled(String exec, String content, String raw) {
            if ("GOOD".equals(exec)) {
                System.out.println("I: 전송 성공!");
                return SUCCESS;
            } else if ("BAD".equals(exec)) {
                System.out.println("W: 전송 실패");
                return FAILED;
            } else if ("END".equals(exec)) {
                System.out.println("W: 전송 실패(재시도 횟수 초과)");
                System.out.println("W: 파일 전송을 중단합니다.");
                return FAILED;
            }
            return PASS;

        }

        @Override
        public int onDisabled(String exec, String content, String raw) {
            return PASS;
        }


        /**
         * 서버로 파일을 전송합니다.
         *
         * @return
         */
        public int sendFile(String filename) {
            try {
                fin = new FileInputStream(new File(filename));
                this.filename = filename;

                byte[] buffer = new byte[1024];
                int len;
                int dataSize = 0;

                while ((len = fin.read(buffer)) > 0) {
                    dataSize++;
                }

                fin.close();

                int datas = dataSize;

                fin = new FileInputStream(filename);
                sendCommand("SEND", filename.substring(filename.lastIndexOf("/") + 1));
                sendCommand("SIZE", dataSize);
                sendCommand("HASH", getHashFile(filename));

                sendCommand("GO");
                enabled = true;
                len = 0;
                for (; dataSize > 0; dataSize--) {                   //데이터를 읽어올 횟수만큼 FileInputStream에서 파일의 내용을 읽어옵니다.
                    len = fin.read(buffer);        //FileInputStream을 통해 파일에서 입력받은 데이터를 버퍼에 임시저장하고 그 길이를 측정합니다.
                    Base64.Encoder encoder = Base64.getEncoder();
                    sendCommand("" + len, new String(encoder.encode(buffer)));
                }
                Thread.sleep(1000);
                sendCommand("OVER");

                System.out.println("I: " + datas + "KB 송신 완료");

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
}