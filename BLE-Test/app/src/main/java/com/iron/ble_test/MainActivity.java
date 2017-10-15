package com.iron.ble_test;
/*
* sample: https://github.com/googlesamples/android-BluetoothLeGatt/blob/master/Application/src/main/java/com/example/android/bluetoothlegatt/DeviceScanActivity.java
* doc: https://han-ya.blogspot.tw/2015/10/android-app-ble-bluetoothlegatt.html
* permission: <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/> <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
* trun GPS
* ex: https://han-ya.blogspot.tw/2015/10/android-app-ble-bluetoothlegatt.html
* 0x2800是"Primary Service"
* */

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import static android.bluetooth.BluetoothProfile.STATE_CONNECTED;
import static android.bluetooth.BluetoothProfile.STATE_DISCONNECTED;

public class MainActivity extends AppCompatActivity {
    final String TAG = "BLE";
    private static final int REQUEST_ENABLE_BT = 1;
    private static final long SCAN_PERIOD = 5000;
    private BluetoothAdapter mBluetoothAdapter;
    private Spinner ble_list;
    private Handler mHandler;
    private boolean mScanning;
    private ArrayList<BluetoothDevice> mBleDev;
    private ArrayList<String> mDevName;
    private BluetoothLeScanner mLEScanner;
    private ScanSettings settings;
    private List<ScanFilter> filters;
    private String mSelAddr;
    private String mName;
    private BluetoothGatt mBluetoothGatt;
    private Button btn_connect;
    private List<BluetoothGattService> mServiceList;

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        ble_list = (Spinner) findViewById(R.id.ble_spin);
        mBleDev = new ArrayList<BluetoothDevice>();
        mDevName = new ArrayList<String>();
        mHandler = new Handler();
        btn_connect = (Button) findViewById(R.id.btn_connect);


        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ble_list.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Object obj = ble_list.getAdapter().getItem(position);
                mSelAddr = mBleDev.get(position).getAddress();//"00:11:05:09:00:90";//obj.toString();
                mName = mDevName.get(position);
                Log.e("BLE: sel ", mSelAddr);
                Toast.makeText(getApplicationContext(), mSelAddr,
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }


    @Override
    protected void onResume() {
        super.onResume();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

        if (Build.VERSION.SDK_INT >= 21) {
            Log.e("BLE ", "SDK Ver >= 21");
            mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
            settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();
        }
        // Initializes list view adapter.
        scanLeDevice(true);
        //ble_list.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mDevName));
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    if (Build.VERSION.SDK_INT < 21)
                        mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    else
                        mLEScanner.stopScan(mScanCallback);
                    invalidateOptionsMenu();

                    if (mDevName.size() > 0) {
                        ble_list.setId(0);
                        Log.e("BLE ", "scan stop " + ble_list.getId());
                    }
                    ble_list.setAdapter(new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1, mDevName));
                }
            }, SCAN_PERIOD);

            Log.e("BLE ", "Start scan");
            mScanning = true;
            if (Build.VERSION.SDK_INT < 21)
                mBluetoothAdapter.startLeScan(mLeScanCallback);
            else {
                Log.e("BLE ", "Scan Callback");
                mLEScanner.startScan(filters, settings, mScanCallback);
            }

        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        invalidateOptionsMenu();
    }

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {

            BluetoothDevice btDevice = result.getDevice();

            if (!mBleDev.contains(btDevice)) {
                if (btDevice.getName() != null) {
                    mBleDev.add(btDevice);
                    mDevName.add(btDevice.getName());
                    Log.e("BLE: ", btDevice.getAddress());
                }
            }
            //connectToDevice(btDevice);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult sr : results) {
                Log.i("ScanResult - Results", sr.toString());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e("Scan Failed", "Error Code: " + errorCode);
        }
    };

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
    new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.e("BLE ", "---onLeScan---");
                    mBleDev.add(device);
                    mDevName.add(device.getName());
                    Log.e("BLE: ", device.getName());
                }
            });
        }
    };

    public void connect(View view) {
        if (mBluetoothAdapter == null || mSelAddr == null) {
            Log.w("BLE: ", "BluetoothAdapter not initialized or unspecified address.");
            return;
        }
        Log.e(TAG, "Addr: "+mSelAddr);
        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(mSelAddr);

        if (device == null) {
            Log.w("BLE: ", "Device not found.  Unable to connect.");
        }

        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);


    }


    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
//                mConnectionState = STATE_CONNECTED;
                //broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());


            } else if (newState == STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
//                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                //broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                mServiceList = mBluetoothGatt.getServices();
                if (mServiceList == null) return;
                Log.e(TAG, "Get Service "+mBluetoothGatt.getServices().size());
                String uuid;
                for (BluetoothGattService gattService : mServiceList) {
                    uuid = gattService.getUuid().toString();
                    Log.e(TAG, "UUID: "+gattService.getUuid().toString()+"Name: "+
                            BleNamesResolver.resolveServiceName(uuid));

                    List<BluetoothGattCharacteristic> gattCharacteristics =
                            gattService.getCharacteristics();
                    for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                        Log.e(TAG, "  UUID: "+gattCharacteristic.getUuid().toString()+"Name: "+
                                BleNamesResolver.resolveCharacteristicName(uuid));
                    }
                }
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                    broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }
    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);


    }
}
