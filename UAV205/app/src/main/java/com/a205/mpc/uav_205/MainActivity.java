package com.a205.mpc.uav_205;

import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Toast;
import android.widget.VerticalSeekBar;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
//socket: https://www.jianshu.com/p/089fb79e308b

public class MainActivity extends AppCompatActivity {
    private static final int MIN_RSSI = -100;
    private static final int MAX_RSSI = -55;
    private final String uav_ssid = "\"UAV205\"";
    private float x_control, y_control;
    private Socket clientSocket = null;
    private ImageButton btn_lock;
    private ImageButton btn_connect;
    private ImageButton btn_control;
    private ImageView   icon_signal;
    private VerticalSeekBar thrust;
    private boolean isLock = true;
    private boolean isConnect = false;
    private WifiInfo wifiInfo;
    private Thread monitor;
    private int[] signal_lv = {R.drawable.signal_lv0, R.drawable.signal_lv1,
            R.drawable.signal_lv2, R.drawable.signal_lv3, R.drawable.signal_lv4};

    private View.OnTouchListener btnListener = new View.OnTouchListener() {
        private float x, y, dx, dy;
        private double d;
        private int mx, my;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (!isConnect)
                return true;
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    x_control = btn_control.getX();
                    y_control = btn_control.getY();
                    x = event.getX();
                    y = event.getY();
//                    Log.e("original pos", String.valueOf(x) + "~~" + String.valueOf(y));
//                    Log.e("raw pos", String.valueOf(event.getRawX()) + "~~" + String.valueOf(event.getRawY()));
                    break;
                case MotionEvent.ACTION_MOVE:

                    Log.d("d", String.valueOf(d));
                    mx = (int) (event.getRawX() - x);
                    my = (int) (event.getRawY() - y);
                    dx = mx - x_control;
                    dy = my - y_control;
                    d = Math.sqrt(dx*dx + dy*dy);
                    if (d < 260) {
                        if (dx > 100)
                            sendCmd(command((byte)'b', (byte)'r'));
                        else if (dx < -100)
                            sendCmd(command((byte)'b', (byte)'l'));
                        else
                            sendCmd(command((byte)'b', (byte)'s'));

                        if (dy > 100)
                            sendCmd(command((byte)'b', (byte)'b'));
                        else if (dy < -100)
                            sendCmd(command((byte)'b', (byte)'f'));
                        else
                            sendCmd(command((byte)'b', (byte)'S'));
                        v.layout(mx, my, mx + v.getWidth(), my + v.getHeight());
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    v.layout((int)x_control, (int)y_control,
                            (int)x_control + v.getWidth(), (int)y_control + v.getHeight());
                    sendCmd(command((byte)'b', (byte)'s'));
                    sendCmd(command((byte)'b', (byte)'S'));
                    break;
            }
//            Log.e("address", String.valueOf(mx) + "~~" + String.valueOf(my));
            return true;
        }
    };

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
        btn_lock    = (ImageButton)findViewById(R.id.btn_lock);
        btn_control = (ImageButton)findViewById(R.id.control);
        icon_signal = (ImageView)findViewById(R.id.signal);
        thrust      = (VerticalSeekBar)findViewById(R.id.seekbar_thrust);

        btn_control.setOnTouchListener(btnListener);
        thrust.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
                Log.w("Thrust: ", "stop");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
                Log.w("Thrust: ", "start");
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                Log.w("Thrust: ", String.valueOf(progress));
                if (isConnect)
                    sendCmd(command((byte)'B', (byte)progress));
            }
        });

        monitor = new Thread(new Runnable(){
            @Override
            public void run() {
                while (true) {
                    try {
                        WifiManager mWifiManager = (WifiManager)MainActivity.this.getSystemService(MainActivity.WIFI_SERVICE);
                        wifiInfo = mWifiManager.getConnectionInfo();
                        final int level = calculateSignalLevel(wifiInfo.getRssi(), 4);

                        runOnUiThread(new Runnable() {
                            public void run() {
                                icon_signal.setImageResource(signal_lv[level]);
                            }

                        });

                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        monitor.start();
        WifiManager mWifiManager = (WifiManager)this.getSystemService(this.WIFI_SERVICE);
        wifiInfo = mWifiManager.getConnectionInfo();

        if (uav_ssid.compareTo(wifiInfo.getSSID()) != 0)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Incorrect SSID");
            builder.setIcon(R.mipmap.ic_launcher_round);
            builder.setMessage("Please select correct SSID.\n"+"SSID you select is "+wifiInfo.getSSID()+".").setPositiveButton("Close", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // FIRE ZE MISSILES!
                }
            });
            builder.create();
            AlertDialog dialog = builder.create();
            dialog.show();
//            dialog.dismiss();
            Toast.makeText(getApplicationContext(), "Icorrect SSID"+wifiInfo.getSSID(), Toast.LENGTH_SHORT).show();
        }
    }

    public static int calculateSignalLevel(int rssi, int numLevels) {
    /* in general, numLevels is 4  */
        if (rssi <= MIN_RSSI) {
            return 0;
        } else if (rssi >= MAX_RSSI) {
            return numLevels - 1;
        } else {
            float inputRange = (MAX_RSSI - MIN_RSSI);
            float outputRange = (numLevels - 1);

            return (int)((float)(rssi - MIN_RSSI) * outputRange / inputRange);
        }
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

    private void alertDialog(String title, String msg)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(title);
        builder.setIcon(R.mipmap.ic_launcher_round);
        builder.setMessage(msg).setPositiveButton("Close", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // FIRE ZE MISSILES!
            }
        });
        builder.create();
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private Runnable connectThread = new Runnable() {
        @Override
        public void run() {
            try {
                Log.d("UAV: ", "connect to uav");
                clientSocket = new Socket();
                clientSocket.connect(new InetSocketAddress("192.168.123.1", 80), 1000);
                if (clientSocket.isConnected()) {
                    clientSocket.setTcpNoDelay(true);
                    runOnUiThread(new Runnable() {
                        public void run()
                        {
                            btn_connect.setImageResource(R.drawable.connected);
                            btn_lock.setImageResource(R.drawable.lock);
                        }
                    });
                    isConnect = true;
                    sendCmd(command((byte) 'D', (byte) 0));

                    isLock = true;
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
                sendCmd(command((byte) 'D', (byte) 0));
                try {
                    clientSocket.close();
                }catch (Exception e) {
                    e.printStackTrace();
                }
            }

            btn_lock.setImageResource(R.drawable.lock);
            isLock = true;
            btn_connect.setImageResource(R.drawable.disconnected);
            isConnect = false;
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
                        mmOutStream.flush();
                        Log.e("Socke: ", "OK");
                    }

                } catch (Exception e) {

                    clientSocket = null;
                    runOnUiThread(new Runnable() {
                        public void run()
                        {
                            btn_connect.setImageResource(R.drawable.disconnected);
                            alertDialog("Network error", "Connect fail.");
                        }
                    });

                    isConnect = false;
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
            thrust.setProgress(0);
        }
        else {
            sendCmd(command((byte) 'D', (byte) 0));
            btn_lock.setImageResource(R.drawable.lock);
            isLock = true;
        }

    }
}


