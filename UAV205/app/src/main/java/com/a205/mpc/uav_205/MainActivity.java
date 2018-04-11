package com.a205.mpc.uav_205;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.VerticalSeekBar;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
//socket: https://www.jianshu.com/p/089fb79e308b

public class MainActivity extends AppCompatActivity {
    private Socket clientSocket = null;
    private ImageButton btn_lock;
    private ImageButton btn_connect;
    private boolean isLock = true;
    private boolean isConnect = false;
    private VerticalSeekBar seekBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportActionBar().hide();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        setContentView(R.layout.activity_main);
        btn_connect = (ImageButton)findViewById(R.id.btn_connect);
        btn_lock = (ImageButton)findViewById(R.id.btn_lock);
        seekBar = (VerticalSeekBar)findViewById(R.id.vertical_Seekbar);

    }

    public static byte[] command(byte act, byte val)
    {
        byte[] data = new byte[4];

        data[3] = val;
        data[2] = act;
        data[1] = '#';
        data[0] = '@';
        return data;
    }

    private Runnable connectThread = new Runnable() {
        @Override
        public void run() {
            try {
                Log.d("UAV: ", "connect to uav");
                clientSocket = new Socket();
                clientSocket.connect(new InetSocketAddress("192.168.123.1", 80), 1000);
                if (clientSocket.isConnected()) {

                    runOnUiThread(new Runnable() {
                        public void run()
                        {
                            btn_connect.setImageResource(R.drawable.connected);
                        }
                    });
                    isConnect = true;
                    Log.d("UAV: ", "connect success");
                }
                else {
                    runOnUiThread(new Runnable() {
                        public void run()
                        {
                            Toast.makeText(getApplicationContext(), "Connect UAV fail", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

            } catch (Exception e) {
                clientSocket = null;
                runOnUiThread(new Runnable() {
                    public void run()
                    {
                        Toast.makeText(getApplicationContext(), "Connect UAV fail", Toast.LENGTH_SHORT).show();
                    }
                });
                e.printStackTrace();
            }

        }
    };

    public void connect(View view)
    {
        Log.e("UAV: ", "connect...");
        if (!isConnect) {
            Thread thread = new Thread(connectThread);
            thread.start();

        }
        else {
            if (clientSocket != null)
            {
                try {
                    clientSocket.close();
                }catch (Exception e) {
                    e.printStackTrace();
                }
            }
            btn_connect.setImageResource(R.drawable.disconnected);
            isConnect = true;
        }
    }

    private void sendCmd(byte[] cmd)
    {
        final byte[] dat = cmd;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                    if (clientSocket.isConnected()) {
                        OutputStream mmOutStream = clientSocket.getOutputStream();
                        mmOutStream.write(dat);
                        mmOutStream.flush();;
                        Log.d("SocketSend", "OK");
                    }
                    Thread.sleep(100);

                } catch (Exception e) {
                    clientSocket = null;
                    e.printStackTrace();
                }
            }
        }).start();

    }

    public void lock(View view)
    {
        if (clientSocket == null) {
            Toast.makeText(getApplicationContext(), "Connect UAV fail", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isLock) {
            sendCmd(command((byte)'d', (byte)0));
            btn_lock.setImageResource(R.drawable.unlock);
            isLock = false;
            seekBar.setProgress(0);
        }
        else {
            sendCmd(command((byte) 'D', (byte) 0));
            btn_lock.setImageResource(R.drawable.lock);
            isLock = true;
        }
        isLock = !isLock;
    }
}


