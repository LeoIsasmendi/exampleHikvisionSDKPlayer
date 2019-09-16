package com.example.hikvisioncameraplayer;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.Button;

import com.google.android.material.textfield.TextInputEditText;

public class MainActivity extends AppCompatActivity {

    private TextInputEditText inputIPAddress, inputUsername, inputPassword;
    private Button btnConnect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        inputIPAddress = findViewById(R.id.input_ip_address);
        inputUsername = findViewById(R.id.input_username);
        inputPassword = findViewById(R.id.input_password);
        btnConnect = findViewById(R.id.btn_connect);
        btnConnect.setOnClickListener(getBtnConnectOnClickListener());
    }

    private View.OnClickListener getBtnConnectOnClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectToCamera(inputIPAddress.getText(), inputUsername.getText(), inputPassword.getText());
            }
        };
    }

    private void connectToCamera(Editable txtIpAddress, Editable txtUsername, Editable txtPassword) {
        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra(PlayerActivity.EXTRA_IP_ADDRESS, txtIpAddress.toString());
        intent.putExtra(PlayerActivity.EXTRA_USERNAME, txtUsername.toString());
        intent.putExtra(PlayerActivity.EXTRA_PASSWORD, txtPassword.toString());
        startActivity(intent);
    }

}
