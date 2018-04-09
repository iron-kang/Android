package com.a205.mpc.uav_205;

import android.content.pm.ActivityInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import java.net.Socket;
//socket: https://www.jianshu.com/p/089fb79e308b

public class MainActivity extends AppCompatActivity {
    private Socket clientSocket = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportActionBar().hide();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);


        setContentView(R.layout.activity_main);
    }

    public void connect(View view)
    {
        try {
            clientSocket = new Socket("192.168.123.1", 80);
        }catch (Exception e) {
            clientSocket = null;
            e.printStackTrace();
        }
    }
}


