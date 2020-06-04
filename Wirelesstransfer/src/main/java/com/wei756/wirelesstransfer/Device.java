package com.wei756.wirelesstransfer;

import java.io.*;
import java.net.SocketException;
import java.security.MessageDigest;

public abstract class Device {

    InputStream in;
    OutputStream out;
    DataInputStream din;
    DataOutputStream dout;

    /**
     * 클라이언트와 연결이 되었을 때 작업
     */
    abstract protected int standBy();


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
        String[] result = new String[2];
        result[1] = null;
        try {
            String cmd = din.readUTF();
            System.out.println("< " + cmd);

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
            System.out.println("> " + cmd);
            dout.writeUTF(cmd);
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
        return 0;
    }
}
