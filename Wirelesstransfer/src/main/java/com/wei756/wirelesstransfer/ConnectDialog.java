package com.wei756.wirelesstransfer;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class ConnectDialog extends JFrame {
    ConnectPanel connectPanel;

    public ConnectDialog(String address, String port, String key) {
        initUI(address, port, key);
    }

    public ConnectDialog() {
        initUI();
    }

    private void initUI(String address, String port, String key) {
        connectPanel = new ConnectPanel();
        connectPanel.setSize(320, 320);
        connectPanel.setLocation(0, 0);
        connectPanel.setInfo(address, port, key);
        updateConnectPanel();
        add(connectPanel);

        initUI();
    }

    private void initUI() {
        setSize(400, 400);
        setTitle("Connect");
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setLayout(null);
    }

    public void setInfo(String address, String port, String key) {
        connectPanel.setInfo(address, port, key);
    }

    public void updateConnectPanel() {
        connectPanel.update();
    }
}


/**
 * qr코드 패널
 */
class ConnectPanel extends JPanel {
    Graphics2D g2d;
    int width, height;
    int cx, cy;

    String address, port, key;
    BufferedImage imageQRCode;

    private void doDrawing(Graphics g) {
        g2d = (Graphics2D) g;

        Dimension size = getSize();
        Insets insets = getInsets();

        width = size.width - insets.left - insets.right;
        height = size.height - insets.top - insets.bottom;

        cx = width / 2;
        cy = height / 2;

        drawQRCode();
    }

    public void drawPoint(float x, float y, float z) {
        int _x, _y;
        g2d.setColor(Color.red);
        g2d.setFont(new Font("맑은 고딕", Font.PLAIN, 12));

        // xy 평면
        _x = cx + (int) (x);
        _y = cy / 2 - (int) (y);
        g2d.drawOval(_x - 3, _y - 3, 5, 5);
        g2d.drawString("[" + x + "," + y + "," + z + "]", _x + 5, _y + 5);
    }

    public void setInfo(String address, String port, String key) {
        this.address = address;
        this.port = port;
        this.key = key;

        generateQRCode(address, port, key);
    }

    public void drawQRCode() {
        if (imageQRCode != null) {
            Paint paintQRCode = new TexturePaint(imageQRCode, new Rectangle(0, 0, 300, 300));
            g2d.setPaint(paintQRCode);
            g2d.fillRect(0, 0, 300, 300);

            g2d.dispose();
        }
    }

    /**
     * QR코드를 생성한뒤 imageQRCode에 저장합니다.
     *
     * @param address
     * @param port
     * @param key
     */
    private void generateQRCode(String address, String port, String key) {
        generateQRCode(address + " " + port + " " + key);
    }

    /**
     * QR코드를 생성한뒤 imageQRCode에 저장합니다.
     *
     * @param str
     */
    private void generateQRCode(String str) {
        imageQRCode = null;
        try {
            // 코드인식시 링크걸 URL주소
            String codeurl = new String(str.getBytes("UTF-8"), "ISO-8859-1");
            // 큐알코드 바코드 생상값
            int qrcodeColor = 0xFF2e4e96;
            // 큐알코드 배경색상값
            int backgroundColor = 0xFFFFFFFF;

            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            // 3,4번째 parameter값 : width/height값 지정
            BitMatrix bitMatrix = qrCodeWriter.encode(codeurl, BarcodeFormat.QR_CODE, 200, 200);
            //
            MatrixToImageConfig matrixToImageConfig = new MatrixToImageConfig(qrcodeColor, backgroundColor);
            BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix, matrixToImageConfig);
            // ImageIO를 사용한 바코드 파일쓰기
            //ImageIO.write(bufferedImage, "png", new File("qrcode.png"));
            imageQRCode = bufferedImage;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        doDrawing(g);
    }

    public void update() {
        revalidate();
        repaint();
    }
}