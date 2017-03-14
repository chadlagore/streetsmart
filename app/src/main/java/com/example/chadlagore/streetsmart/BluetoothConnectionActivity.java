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
import android.widget.ProgressBar;

import java.util.Set;

import static com.example.chadlagore.streetsmart.R.id.bluetooth_connection_toolbar;

/**
 * Created by bfbachmann on 2017-03-13.
 */

public class BluetoothConnectionActivity extends AppCompatActivity {
    private final static int REQUEST_ENABLE_BT = 1;
    ProgressBar spinnyWheel;
    ViewGroup thisViewGroup;
    Context thisContext;
    BluetoothAdapter bluetoothAdapter;

    /**
     * Create a BroadcastReceiver for Bluetooth connections.
     */
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                /*
                 * Bluetooth discovery has found a device. Get the BluetoothDevice
                 * object and its info from the Intent.
                 */
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress();
            }
        }
    };


    /**
     * This is run when an object of this class is instantiated
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_connection);
        spinnyWheel = (ProgressBar) findViewById(R.id.progressWheel);
        thisViewGroup = (ViewGroup) findViewById(R.id.btConnectLinearLayout);
        thisContext = getApplicationContext();

        /* Add the toolbar so we have access to the "Back" button */
        Toolbar myChildToolbar = (Toolbar) findViewById(R.id.bluetooth_connection_toolbar);
        setSupportActionBar(myChildToolbar);

        /* Expand the toolbar at the top of the screen */
        Toolbar appToolbar = (Toolbar) findViewById(bluetooth_connection_toolbar);
        setSupportActionBar(appToolbar);

        /* Enable up button on support ActionBar corresponding to this toolbar */
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        /* Create a Bluetooth Adapter */
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            /*
             * Creating the Adapter probably failed because you are running in the emulator
             * Return to MainActivity
             */
            showBluetoothDialog("This device does not appear to have Bluetooth capabilities which " +
                "are required for this action.", "Bluetooth Unavailable");
        } else if (!bluetoothAdapter.isEnabled()) {
            /* Bluetooth is available but not enabled, request enable */
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        if (bluetoothAdapter != null) {
            /* Make the progress wheel visible while we search for devices */
            spinnyWheel.setVisibility(View.VISIBLE);

            /* Register for broadcasts when a Bluetooth device is discovered. */
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(broadcastReceiver, filter);
        }
    }


    /**
     * This method is called when the object of this class is destroyed
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        /* Don't forget to unregister the ACTION_FOUND receiver. */
        try {
            unregisterReceiver(broadcastReceiver);
        } catch (Exception e) {
            //TODO: do something
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

            /* Search for paired Bluetooth devices */
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

            if (pairedDevices.isEmpty()) {
                /* There are no paired devices so we have to scan for available ones */
                boolean searchBegan = bluetoothAdapter.startDiscovery();
                if (!searchBegan) {
                    /* Device search failed for some reason */
                    showBluetoothDialog("Bluetooth Device Discovery failed.", "OOPS!");
                } else {
                    /* Set up a view so the user can see available devices */
                    showBluetoothDialog("This feature has not yet been implemented.", "OOPS!");
                }
            }
        } else {
            /* The user would not grant us Bluetooth permissions */
            showBluetoothDialog("Sorry, you must grant Bluetooth permissions to connect to " +
                            "external devices.", "Permissions Required.");
        }
    }


    /**
     *  Show dialog box telling user bluetooth is not available
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
}
