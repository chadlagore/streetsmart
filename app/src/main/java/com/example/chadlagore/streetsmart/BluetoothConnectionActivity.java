package com.example.chadlagore.streetsmart;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import static com.example.chadlagore.streetsmart.R.id.bluetooth_connection_toolbar;

/**
 * Created by bfbachmann on 2017-03-13.
 */

public class BluetoothConnectionActivity extends AppCompatActivity {

    /* Some variables we will need access to throughout this activity */
    private final static int REQUEST_ENABLE_BT = 1;
    private static UUID BT_UUID;
    private BluetoothAdapter bluetoothAdapter = null;
    private BluetoothSocket BTSocket = null;
    private InputStream inputStream = null;
    private OutputStream outputStream = null;
    private byte[] inputBuffer = null;


    /**
     * This is run when an object of this class is instantiated
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_connection);

        /* Add the toolbar so we have access to the "Back" button */
        Toolbar myChildToolbar = (Toolbar) findViewById(R.id.bluetooth_connection_toolbar);
        setSupportActionBar(myChildToolbar);
        Toolbar appToolbar = (Toolbar) findViewById(bluetooth_connection_toolbar);
        setSupportActionBar(appToolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        BT_UUID = UUID.fromString("c728a19e-e246-4595-8933-5aa3d1ebdbe0");

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
                establishConnection();
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
           establishConnection();
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
        /* Instantiate an AlertDialog. Builder with its constructor */
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
        destroyConnection();
    }


    private void establishConnection() {
         /* Search for paired Bluetooth devices */
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        if (pairedDevices.isEmpty()) {
            /* We are not paired with a device */
            showBluetoothDialog("You must be paired with a device to perform this action.",
                    "Not Paired");
        } else {
            /* We are already paired TODO: actually implement this */
            for (BluetoothDevice device : pairedDevices) {
                /*
                 * Assume this is the one we want to connect to and
                 * attempt to establish a connection
                 */
                try {
                    BTSocket = device.createInsecureRfcommSocketToServiceRecord(BT_UUID);
                    /* We connected successfully, don't connect to anything else */
                    break;
                } catch (IOException e) {
                    /* Try another device */
                    continue;
                }
            }
            bluetoothAdapter.cancelDiscovery();

            /* Try connect to the device */
            try {
                BTSocket.connect(); /* WARNING: this is a blocking call */
            } catch (IOException e) {
                showBluetoothDialog("Sorry, an error occurred while attempting to connect to " +
                "the remote device. Please make sure you are paired with the correct device.",
                        "Connection Failure");
            }

            /* The connection succeeded TODO: call manageConnection */
            showBluetoothDialog("Successfully connected to remote device!",
                    "Connection Successful");
        }
    }


    /**
     * Sends and received data from the remote device asynchronously
     */
    private void manageConnection() {
        /* Get input and output streams from Bluetooth Socket */
        try {
            inputStream = BTSocket.getInputStream();
        } catch (IOException e) {
            showBluetoothDialog("Failed to get input stream from socket.", "Bluetooth Socket Error");
        }
        try {
            outputStream = BTSocket.getOutputStream();
        } catch (IOException e) {
            showBluetoothDialog("Failed to get output stream from socket.", "Bluetooth Socket Error");
        }

        readBytesFromSocket();
    }


    /**
     * Just closes the Bluetooth connection with the remote device by closing the socket
     */
    private void destroyConnection() {
        if (BTSocket != null) {
            try {
                BTSocket.close();
            } catch (IOException e) {
                // TODO: handle this more appropriately
            }
        }
    }


    /**
     * Read data received from the remote device and display it on the screen until and IOException
     * occurs.
     */
    private void readBytesFromSocket() {
        inputBuffer = new byte[1024];
        int bytesRead;

        /* Keep listening for input until error occurs */
        while (true) {
            try {
                bytesRead = inputStream.read(inputBuffer);
            } catch (IOException e) {
                showBluetoothDialog("Input stream as disconnected.",
                        "Bluetooth Socket Error");
                break;
            }

            displayData(inputBuffer.toString());
        }
    }

    /**
     * Display the String data on the screen
     * @param data
     */
    private void displayData(String data) {
        TextView receivedDataView = (TextView) findViewById(R.id.received_data);
        receivedDataView.setVisibility(TextView.VISIBLE);
        receivedDataView.setText(data);
    }
}
