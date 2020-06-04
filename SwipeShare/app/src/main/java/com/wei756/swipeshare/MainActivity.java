package com.wei756.swipeshare;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.wei756.swipeshare.wirelesstransfer.Client;

public class MainActivity extends AppCompatActivity {
    EditText etAddress, etPort, etKey;
    Button btnConnect, btnDisconnect, btnSendFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etAddress = (EditText) findViewById(R.id.et_address);
        etPort = (EditText) findViewById(R.id.et_port);
        etKey = (EditText) findViewById(R.id.et_key);
        btnConnect = (Button) findViewById(R.id.btn_connect);
        btnDisconnect = (Button) findViewById(R.id.btn_disconnect);
        btnSendFile = (Button) findViewById(R.id.btn_sendfile);

        btnConnect.setOnClickListener(view ->
            new Thread(() -> {
                Client client = new Client(etAddress.getText().toString(),
                        Integer.parseInt(etPort.getText().toString()),
                        etKey.getText().toString());
            }).start()
        );
    }
}
