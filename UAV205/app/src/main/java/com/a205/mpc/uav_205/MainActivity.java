package com.a205.mpc.uav_205;

import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.VerticalSeekBar;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
//socket: https://www.jianshu.com/p/089fb79e308b

public class MainActivity extends AppCompatActivity {
    private static final int MIN_RSSI = -100;
    private static final int MAX_RSSI = -55;
    private Socket clientSocket = null;
    private ImageButton btn_lock;
    private ImageButton btn_connect;
    private ImageView   icon_signal;
    private boolean isLock = true;
    private boolean isConnect = false;
    private VerticalSeekBar seekBar;
    private WifiInfo wifiInfo;
    private Thread monitor;
    private int[] signal_lv = {R.drawable.signal_lv0, R.drawable.signal_lv1,
            R.drawable.signal_lv2, R.drawable.signal_lv3, R.drawable.signal_lv4};

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
        seekBar     = (VerticalSeekBar)findViewById(R.id.vertical_Seekbar);
        icon_signal = (ImageView)findViewById(R.id.signal);
        WifiManager mWifiManager = (WifiManager)this.getSystemService(this.WIFI_SERVICE);
        wifiInfo = mWifiManager.getConnectionInfo();

        if (!"UAV205".equals(wifiInfo.getSSID()))
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Incorrect SSID");
            builder.setMessage("Please select correct SSID.");
            builder.create();
            AlertDialog dialog = builder.create();
            dialog.show();
            dialog.dismiss();
        }

        monitor = new Thread(new Runnable(){
            @Override
            public void run() {
                while (true) {
                    try {
                        WifiManager mWifiManager = (WifiManager)MainActivity.this.getSystemService(MainActivity.WIFI_SERVICE);
                        wifiInfo = mWifiManager.getConnectionInfo();
                        final int level = calculateSignalLevel(wifiInfo.getRssi(), 4);
                        Log.e("UAV Rssi:", Integer.toString(level));
                        runOnUiThread(new Runnable() {        //可以使用此方法臨時交給UI做顯示
                            public void run() {
                                icon_signal.setImageResource(signal_lv[level]);
                            }

                        });

                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        });
        monitor.start();
        Toast.makeText(getApplicationContext(), wifiInfo.getSSID()+"RSSI:"+Integer.toString(wifiInfo.getRssi()), Toast.LENGTH_SHORT).show();
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


