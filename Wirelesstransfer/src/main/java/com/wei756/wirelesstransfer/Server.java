package com.wei756.wirelesstransfer;

import java.io.*;
import java.net.*;
import java.util.Base64;

public class Server extends Device {

    ServerSocket soc;

    FileOutputStream fout;

    public Server(int port, String key) {
        try {
            this.hostName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        Log.i("Server", "서버 이름: " + this.hostName);

        try {
            soc = new ServerSocket(port);
            this.key = key;
            Log.i("Server", port + "번 포트로 서버 열림");

            int attempt = 0;
            while (true) { // 연결 재시도를 위한 루프
                int status;
                if ((status = readyForClient()) == 0) {
                    attempt = 0;
                    Log.i("Server", "인바운드 스레드 시작");
                    Server self = this;
                    inputRunnable = new InputRunnable() {
                        @Override
                        protected void init() {
                            services.add(new ContactService(self));
                            services.add(new FileInputService(self));
                        }
                    };
                    inputThread = new Thread(inputRunnable);
                    inputThread.start();
                } else if (status == -2) { // TimeoutException
                    break;
                } else {
                    // 연결 재시도
                    attempt++;
                    if (attempt > 10) {
                        Log.w("Server", "클라이언트 연결 실패");
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
        Log.i("Server", "서버 종료");
    }

    /**
     * 클라이언트와의 연결을 대기합니다.
     *
     * @return
     */
    public int readyForClient() {
        try {
            soc.setSoTimeout(7200000);
            socket = soc.accept();                       //클라이언트의 접속을 받습니다.
            Log.i("Server", "게스트 연결 수락");

            in = null;
            out = null;
            fout = null;
            in = socket.getInputStream();                   //클라이언트 스트림 개통
            out = socket.getOutputStream();                 //클라이언트 스트림 개통
            din = new DataInputStream(in);                  //InputStream을 이용해 데이터 단위로 입력을 받는 DataInputStream을 개통합니다.
            dout = new DataOutputStream(out);               //OutputStream을 이용해 데이터 단위로 보내는 스트림을 개통합니다.
        } catch (IOException e) {
            if (e instanceof SocketTimeoutException) {
                Log.w("Server", "오랫동안 기기 연결이 감지되지 않아 서버를 종료합니다.");

                return -2;
            } else {
                Log.e("Server", "클라이언트와 연결하는 중 에러가 발생하였습니다!");
                e.printStackTrace();

                return -1;
            }
        }

        return 0;
    }

    class ContactService extends InputService {
        String key;

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
            if ("KEY".equals(exec)) {
                this.key = content;
            } else if ("ADDR".equals(exec)) {
                guestAddress = content;
            } else if ("NAME".equals(exec)) {
                guestName = content;
            } else if ("OVER".equals(exec)) {
                if (!this.key.equals(parent.key)) { // 올바르지 않은 키
                    Log.e("ContactService", "신뢰할 수 없는 클라이언트입니다.");
                    sendCommand("BYE"); // 접속 종료
                    return FAILED;
                } else {
                    sendCommand("HI", guestName);
                    sendCommand("IM", hostName);
                    Log.i("ContactService", "'" + guestName + "' 연결됨!");
                    return SUCCESS;
                }
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
            if ("HELLO".equals(exec)) {
                return SUCCESS;
            }
            return PASS;
        }

    }

    class FileInputService extends InputService {
        final int FAILED_INVAILD_HASH = -2;

        FileOutputStream fout;

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
        FileInputService(Device parent) {
            super(parent);
        }

        /**
         * 파일을 전송합니다.
         *
         * @return
         */
        @Override
        public int onEnabled(String exec, String content, String raw) {
            try {
                if (!transfer){
                    if ("SIZE".equals(exec)) {
                        dataSize = Integer.parseInt(content);
                    } else if ("HASH".equals(exec)) {
                        hash = content;
                    } else if ("GO".equals(exec)) { // 전송 요청 승인
                        Log.i("FileInputService", "파일 이름: " + filename);
                        Log.i("FileInputService", "파일 크기: " + dataSize + "KB");
                        File file = new File(filename);                     // 파일 생성
                        fout = new FileOutputStream(file);              // 파일 스트림 생성

                        transfer = true;
                    }
                } else { // 데이터 전송
                    if (dataSize > 0) { // 실제 데이터 전송
                        Base64.Decoder decoder = Base64.getDecoder();
                        byte[] data = decoder.decode(content);
                        fout.write(data, 0, Integer.parseInt(exec));
                        dataSize--;
                        if (dataSize == 0) {
                            Log.i("FileInputService", "OVER 대기");

                            fout.flush();
                            fout.close();
                        }
                    } else {
                        if ("OVER".equals(exec)) {
                            Log.i("FileInputService", "수신 완료");
                            Log.i("FileInputService", "파일 무결성 검사를 시작합니다...");
                            try {
                                if (getHashFile(filename).equals(hash)) { // 무결성 검증
                                    Log.i("FileInputService", "무결성 검사 성공!");
                                    sendCommand("GOOD");
                                    return SUCCESS;
                                } else {
                                    Log.i("FileInputService", "무결성 검사 실패");
                                    sendCommand("BAD");
                                    return FAILED_INVAILD_HASH;
                                }
                            } catch (Exception e) {
                                Log.i("FileInputService", "실패");
                                e.printStackTrace();
                                sendCommand("BAD");
                                return FAILED_INVAILD_HASH;
                            }

                        } else if ("ERR".equals(exec)) {
                            Log.e("FileInputService", "통신 에러!!! : " + content);
                            return FAILED;
                        }
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
                return FAILED;
            }
            return PASS;

        }

        /**
         * 파일 전송 과정을 시작합니다.
         *
         * @return
         */
        @Override
        public int onDisabled(String exec, String content, String raw) {
            if ("SEND".equals(exec)) {
                filename = content;
                return SUCCESS;
            }
            return PASS;
        }

    }

}