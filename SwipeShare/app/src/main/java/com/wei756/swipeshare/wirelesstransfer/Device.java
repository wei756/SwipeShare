package com.wei756.swipeshare.wirelesstransfer;

import android.util.Log;

import java.io.*;
import java.net.Socket;
import java.security.MessageDigest;
import java.util.ArrayList;

public abstract class Device {

    final int PASS = 1;
    final int SUCCESS = 0;
    final int FAILED = -1;

    boolean DEBUG = false;

    Socket socket;

    Thread inputThread;
    InputRunnable inputRunnable;

    InputStream in;
    OutputStream out;
    DataInputStream din;
    DataOutputStream dout;

    int port;
    String key;
    String guestAddress, guestName;
    String hostAddress, hostName;

    public boolean isStandBy(){
        return inputRunnable.isRunning();
    }

    /**
     * 파일의 SHA256를 반환합니다.
     * @param filename
     * @return
     * @throws Exception
     */
    public String getHashFile(String filename) throws Exception {
        String SHA = "";
        int buff = 16384;
        try {
            RandomAccessFile file = new RandomAccessFile(filename, "r");
            MessageDigest hashSum = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[buff];
            byte[] partialHash = null;
            long read = 0; // calculate the hash of the hole file for the test
            long offset = file.length();
            int unitsize;
            while (read < offset) {
                unitsize = (int) (((offset - read) >= buff) ? buff : (offset - read));
                file.read(buffer, 0, unitsize);
                hashSum.update(buffer, 0, unitsize);
                read += unitsize;
            }
            file.close();
            partialHash = new byte[hashSum.getDigestLength()];
            partialHash = hashSum.digest();
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < partialHash.length; i++) {
                sb.append(Integer.toString((partialHash[i] & 0xff) + 0x100, 16).substring(1));
            }
            SHA = sb.toString();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return SHA;
    }

    /**
     * 디바이스로부터 요청을 받습니다.
     *
     * @return
     */
    protected String[] getCommand() {
        String[] result = new String[3];
        result[1] = null;
        result[2] = null;
        try {
            String cmd = din.readUTF();
            if (DEBUG)
                Log.e("Device", "< " + cmd);
            result[2] = cmd;

            if (cmd.contains(" ")) {
                result[0] = cmd.substring(0, cmd.indexOf(" "));
                result[1] = cmd.substring(cmd.indexOf(" ") + 1);
            } else {
                result[0] = cmd;
            }

        } catch (IOException e) {
            e.printStackTrace();
            result[0] = "ERR";
            result[1] = e.getMessage();
        }

        return result;
    }

    /**
     * 디바이스로 요청을 보냅니다.
     *
     * @return
     */
    protected int sendCommand(String exec, int content) {
        return sendCommand(exec, "" + content);
    }

    /**
     * 디바이스로 요청을 보냅니다.
     *
     * @return
     */
    protected int sendCommand(String exec, String content) {
        return sendCommand(exec + " " + content);
    }

    /**
     * 디바이스로 요청을 보냅니다.
     *
     * @return
     */
    protected int sendCommand(String cmd) {
        try {
            if (DEBUG)
                Log.v("Device", "> " + cmd);
            dout.writeUTF(cmd);
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
        return 0;
    }

    /**
     * 들어오는 명령을 비동기로 처리하는 역할을 합니다.
     */
    abstract class InputRunnable implements Runnable {
        ArrayList<InputService> services = new ArrayList<>();

        private volatile boolean running = true;
        InputRunnable() {
            init();
        }

        /**
         * 서비스를 등록합니다.
         */
        abstract protected void init();

        /**
         * 루프를 중지합니다.
         */
        public void terminate() {
            running = false;
        }

        /**
         * 루프가 실행중인지 반환합니다.
         * @return
         */
        public boolean isRunning() {
            return running;
        }

        /**
         * 등록된 서비스에 명령을 전달합니다.
         */
        @Override
        public void run() {
            while(running) {
                String[] cmd = getCommand();
                String exec = cmd[0];
                String content = cmd[1];
                String raw = cmd[2];
                for(InputService service : services) {
                    service.put(raw);
                }
                if ("BYE".equals(exec)) {
                    //return 0;
                    terminate();
                } else if ("ERR".equals(exec)) {
                    Log.w("InputRunnable", "통신 에러!!! : " + content);
                    //return -1;
                }
            }
            Log.w("InputRunnable", "인바운드 스레드 종료");
        }
    }

    /**
     * 들어온 명령을 처리합니다.
     */
    protected abstract class InputService {
        protected Device parent;

        boolean enabled = false;

        /**
         * 부모 인스턴스를 전달합니다.
         * @param parent
         */
        protected InputService(Device parent) {
            this.parent = parent;
        }

        protected int put(String str) {
            String[] cmd = splitCommand(str);
            String exec = cmd[0];
            String content = cmd[1];

            if (enabled) {
                if (onEnabled(exec, content, str) != PASS)
                    enabled = false;
            } else {
                if (onDisabled(exec, content, str) != PASS)
                    enabled = true;
            }

            return 0;
        }

        abstract protected int onEnabled(String exec, String content, String raw);

        abstract protected int onDisabled(String exec, String content, String raw);

        protected String[] splitCommand(String cmd) {
            String[] result = new String[2];
            result[1] = null;
            try {
                if (cmd.contains(" ")) {
                    result[0] = cmd.substring(0, cmd.indexOf(" "));
                    result[1] = cmd.substring(cmd.indexOf(" ") + 1);
                } else {
                    result[0] = cmd;
                }
            } catch (NullPointerException e) {
                e.printStackTrace();
                result[0] = "ERR";
            }

            return result;
        }
    }
}
