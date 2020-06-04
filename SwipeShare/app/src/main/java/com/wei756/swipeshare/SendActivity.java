package com.wei756.swipeshare;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.wei756.swipeshare.wirelesstransfer.Client;

public class SendActivity extends AppCompatActivity {
    ConstraintLayout clConnect;
    EditText etAddress, etPort, etKey;
    Button btnConnect, btnDisconnect, btnSendFile;
    Client client;

    String filepath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send);

        clConnect = (ConstraintLayout) findViewById(R.id.cl_connect);

        etAddress = (EditText) findViewById(R.id.et_address);
        etPort = (EditText) findViewById(R.id.et_port);
        etKey = (EditText) findViewById(R.id.et_key);
        btnConnect = (Button) findViewById(R.id.btn_connect);
        btnDisconnect = (Button) findViewById(R.id.btn_disconnect);
        btnSendFile = (Button) findViewById(R.id.btn_sendfile);

        btnConnect.setOnClickListener(view -> connect());
        btnDisconnect.setOnClickListener(view -> disconnect());
        btnSendFile.setOnClickListener(view -> sendFile());

        filepath = initIntent();

        IntentIntegrator intentIntegrator = new IntentIntegrator(this);
        intentIntegrator.setBeepEnabled(false);//바코드 인식시 소리
        intentIntegrator.initiateScan();
    }

    /**
     * QR코드 인식 후
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
            } else {
                //Toast.makeText(this, "Scanned: " + result.getContents(), Toast.LENGTH_LONG).show();
                try {
                    String[] results = result.getContents().split(" ");
                    etAddress.setText(results[0]);
                    etPort.setText(results[1]);
                    etKey.setText(results[2]);
                    connect();
                } catch(Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, "QR 코드가 올바르지 않습니다: " + result.getContents(), Toast.LENGTH_LONG).show();
                }

            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private String initIntent() {
        // 인텐트를 얻어오고, 액션과 MIME 타입을 가져온다
        Intent intent = getIntent();

        String action = intent.getAction();
        String type = intent.getType();

        // 인텐트 정보가 있는 경우 실행
        if (Intent.ACTION_SEND.equals(action) && type != null) {
            Log.d("SendActivity", "initIntent: " + type);
            // 가져온 인텐트의 텍스트 정보
            if ("application/*".equals(type)) {
                return ((Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM)).getPath();
            } else if ("image/*".equals(type) || "video/*".equals(type)) {
                return getImagePathToUri(getApplicationContext(), (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM));
            }
        }
        return null;
    }

    /**
     * 기기에 접속합니다.
     */
    private void connect() {
        new Thread(() -> {
            if (client != null) {
                client.bye(); // 이전 연결 종료
                client = null;
            }
            client = new Client(etAddress.getText().toString(),
                    Integer.parseInt(etPort.getText().toString()),
                    etKey.getText().toString());

            runOnUiThread(() -> {
                clConnect.setVisibility(View.GONE);
                btnDisconnect.setVisibility(View.VISIBLE);
                btnSendFile.setVisibility(View.VISIBLE);
            });
        }).start();
    }

    /**
     * 기기 연결을 해제합니다.
     */
    private void disconnect() {
        new Thread(() -> {
            try{
                client.bye();
            } catch (NullPointerException e) {
                e.printStackTrace();
            } finally {
                client = null;
                runOnUiThread(() -> {
                    clConnect.setVisibility(View.VISIBLE);
                    btnDisconnect.setVisibility(View.GONE);
                    btnSendFile.setVisibility(View.GONE);
                });
            }
        }).start();
    }

    /**
     * 파일을 전송합니다.
     */
    private void sendFile() {
        new Thread(() -> {
            try{
                int status = -1;
                while (status == -1 && filepath != null) {
                    status = client.sendFile(filepath);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                disconnect();
            }
        }).start();
    }

    /**
     * Uri로부터 절대 경로를 반환합니다.
     *
     * @param context context
     * @param data    uri
     * @return 절대 경로
     */
    public String getImagePathToUri(Context context, Uri data) {
        // 사용자가 선택한 이미지의 정보를 받아옴
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = context.getContentResolver().query(data, proj, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();

        // 이미지의 경로 값
        String imgPath = cursor.getString(column_index);

        return imgPath;
    }

}
