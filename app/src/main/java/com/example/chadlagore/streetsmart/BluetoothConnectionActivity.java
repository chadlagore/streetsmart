package com.example.chadlagore.streetsmart;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Set;

import static com.example.chadlagore.streetsmart.R.id.bluetooth_connection_toolbar;

/**
 * Created by bfbachmann on 2017-03-13.
 */

public class BluetoothConnectionActivity extends AppCompatActivity {

    /* Some variables we will need access to throughout this activity */
    private final static int REQUEST_ENABLE_BT = 1;
    private ProgressBar spinnyWheel;
    private LinearLayout deviceListLayout;
    private BluetoothAdapter bluetoothAdapter;
    private ArrayList<BluetoothDevice> deviceList;
    private BroadcastReceiver broadcastReceiver;


    /**
     * This is run when an object of this class is instantiated
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_connection);
        spinnyWheel = (ProgressBar) findViewById(R.id.progress_wheel);
        deviceListLayout = (LinearLayout) findViewById(R.id.bt_linear_layout);
        deviceList = new ArrayList<>();

        /* Add the toolbar so we have access to the "Back" button */
        Toolbar myChildToolbar = (Toolbar) findViewById(R.id.bluetooth_connection_toolbar);
        setSupportActionBar(myChildToolbar);
        Toolbar appToolbar = (Toolbar) findViewById(bluetooth_connection_toolbar);
        setSupportActionBar(appToolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

         /* Create a BroadcastReceiver for Bluetooth connections. */
        broadcastReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                /* Bluetooth discovery has found a device. Add it to the device list */
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    addToDeviceList(device);
                }
            }
        };

        /* Create a Bluetooth Adapter */
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            /*
             * Creating the Adapter probably failed because you are running in the emulator
             * Return to MainActivity
             */
            showBluetoothDialog("This device does not appear to have Bluetooth capabilities which " +
                "are required for this action.", "Bluetooth Unavailable");
        } else {
            if (!bluetoothAdapter.isEnabled()) {
                /*
                 * Bluetooth is available but not enabled, request enable. The user's response to
                 * this is handled in onActivityResult()
                 */
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            } else {
                /* Bluetooth is enabled, check for devices */
                checkAvailableDevices();
            }
        }
    }


    /**
     *  Respond to the result of requesting Bluetooth permissions
     *  If requesting permissions failed user will be returned to MainActivity
     *  On success we scan for devices to connect to
     */
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            /* The user gave us Bluetooth permissions! YAY! */
           checkAvailableDevices();
        } else {
            /* The user would not grant us Bluetooth permissions */
            showBluetoothDialog("Sorry, you must grant Bluetooth permissions to connect to " +
                            "external devices.", "Permissions Required");
        }
    }


    /**
     * Show dialog box telling user bluetooth is not available
     * @param message
     * @param title
     */
    private void showBluetoothDialog(String message, String title) {
        /* Instantiate an AlertDialog.Builder with its constructor */
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message).setTitle(title);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
                return;
            }
        });
        AlertDialog dialog = builder.create();
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                finish();
                return;
            }
        });
        dialog.show();
    }



    /**
     * This method is called when the object of this class is destroyed
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(broadcastReceiver);
        } catch (IllegalArgumentException e) {
            /* We are trying to unregister an already unregistered receiver */
        }

    }

    private void checkAvailableDevices() {
         /* Search for paired Bluetooth devices */
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        if (pairedDevices.isEmpty()) {
            /* There are no paired devices so we have to scan for available ones */
            spinnyWheel.setVisibility(View.VISIBLE);
            setDeviceListVisibility(TextView.VISIBLE);
            /*
             * Register for broadcasts when a Bluetooth device is discovered.
             * broadcastReceiver.onReceive() should be called every time a device is found.
             */
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(broadcastReceiver, filter);
            boolean searchBegan = bluetoothAdapter.startDiscovery();
            if (!searchBegan) {
                /* Device search failed for some reason, return to MainActivity */
                showBluetoothDialog("Bluetooth Device Discovery failed.", "OOPS!");
            }
        } else {
            /* We are already paired TODO: actually implement this */
            showBluetoothDialog("Nice! We're already paired with something.", "Already Paired!");
        }
    }


    /**
     * Add a device to the list of available devices and display the new device
     * @param device
     */
    private void addToDeviceList(BluetoothDevice device) {
        runOnUiThread(new DeviceListPopulator(device));
    }


    /**
     * Will make the deviceList in activity_bluetooth_connection visible
     * This device list will be updated with new devices every time one is detected
     * @param visibileOrNot one of TextView.VISIBLE and TextView.INVISIBLE
     */
    private void setDeviceListVisibility(int visibileOrNot) {
        ((ViewGroup)deviceListLayout).setVisibility(visibileOrNot);
    }


    /* For dynamically populating the list of available Bluetooth devices */
    public class DeviceListPopulator implements Runnable {
        BluetoothDevice device;

        DeviceListPopulator(BluetoothDevice device) {
            this.device = device;
        }

        public void run() {
            TextView deviceView = new TextView(getApplicationContext());
            deviceView.setText(device.getName());
            deviceListLayout.addView(deviceView);
            deviceList.add(device);
        }
    }
}
