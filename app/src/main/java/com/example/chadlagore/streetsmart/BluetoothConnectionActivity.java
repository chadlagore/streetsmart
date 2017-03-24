package com.example.chadlagore.streetsmart;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

import static com.example.chadlagore.streetsmart.R.id.bluetooth_connection_toolbar;

/**
 * Created by bfbachmann on 2017-03-13.
 */

public class BluetoothConnectionActivity extends AppCompatActivity {

    /* Some variables we will need access to throughout this activity */
    private static BluetoothAdapter bluetoothAdapter = null;
    private static BluetoothSocket BTSocket = null;
    private InputStream inputStream = null;
    private OutputStream outputStream = null;
    private byte[] inputBuffer = null;
    private String dataReceived = "";

    /* Constants */
    private final int REQUEST_ENABLE_BT = 1;
    private final String BLUETOOTH = "BLUETOOTH";
    private final String BTConnectionTag = "BT_CONNECTION";


    /**
     * This is run when another activity calls startActivity() or startActivityForResult() with this
     * activity.
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
                /* Bluetooth is enabled, establish connection */
                getSocketStreams();
                EstablishConnectionTask task = new EstablishConnectionTask();
                task.execute();
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
            /* The user gave us Bluetooth permissions! YAY! Establish connection. */
            EstablishConnectionTask task = new EstablishConnectionTask();
            task.execute();
        } else {
            /* The user would not grant us Bluetooth permissions */
            showBluetoothDialog("Sorry, you must grant Bluetooth permissions to connect to " +
                            "external devices.", "Permissions Required");
            return;
        }
    }


    /**
     * Show dialog box telling user bluetooth is not available.
     * @param message the message to put in the dialog box.
     * @param title the title of the dialog box.
     */
    protected void showBluetoothDialog(String message, String title) {
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


    /**
     * Initializes the input and output streams for the current BTSocket
     * Precondition: BTSocket is not null
     */
    private void getSocketStreams() {
        /* Get input and output streams from Bluetooth Socket */
        try {
            inputStream = BTSocket.getInputStream();
        } catch (IOException e) {
            showBluetoothDialog("Sorry, an error occurred while trying to communicate with the " +
                    "remote device.", "Bluetooth Input Socket Error");
            return;
        }
        try {
            outputStream = BTSocket.getOutputStream();
        } catch (IOException e) {
            showBluetoothDialog("Sorry, an error occurred while trying to communicate with the " +
                    "remote device.", "Bluetooth Output Socket Error");
            return;
        }
    }


    /**
     * Just closes the Bluetooth connection with the remote device by closing the socket
     */
    private void destroyConnection() {
        if (BTSocket != null) {
            try {
                BTSocket.close();
            } catch (IOException e) {
                /* If this fails the socket was already closed or destroyed, so nothing to do */
            }
        }
    }


    /**
     * Display the String data on the screen
     * @param data
     */
    private void displayData(String data) {
        Log.i(BLUETOOTH, "Received Data: " + data);
        TextView receivedDataView = (TextView) findViewById(R.id.received_data);
        TextView receivedDistValue = (TextView) findViewById(R.id.dist_reading_value);
        receivedDataView.setVisibility(TextView.VISIBLE);
        receivedDataView.setText("Received Data: " + data);
        receivedDistValue.setText(data);
    }


    /**
     * Receive a string from the remote Bluetooth device we are currently connected to
     * Precondition: must be connected to the remote device and the inputStream can't be null
     * WARNING: this function is blocking! Make sure it is called in a non-main thread!
     * @param timeoutMillis timeout for this function in milliseconds. Pass 0 for no timeout.
     * @return the string received if it was received in full within the timeout period,
     * otherwise return null
     */
    @Nullable
    private String receiveString(int timeoutMillis) {
        inputBuffer = new byte[2048];
        dataReceived = "";
        boolean started = false;
        int bytes_read;

        long startTime = System.currentTimeMillis();

        while (true) {
            try {
                bytes_read = inputStream.read(inputBuffer);

                if (inputBuffer[0] == '$' || started) {
                    started = true;
                    dataReceived += new String(inputBuffer, 0, bytes_read);
                }

                if (inputBuffer[0] == '\n' && started) {
                    dataReceived = dataReceived.replace("$", "").replace("\n", "");
                    return dataReceived;
                }
            } catch (IOException e) {
                return null;
            }

            if (timeoutMillis != 0 &&
                    System.currentTimeMillis() - startTime > timeoutMillis) return null;
        }
    }


    /**
     * Tries to send the String data to the remote Bluetooth device
     * Precondition: outputStream has been initialized properly
     * @param data
     * @return true on success and false on failure
     */
    private boolean sendString(String data) {
        try {
            outputStream.write(("$" + data + "\n").getBytes());
        } catch (IOException e) {
            return false;
        }
        return true;
    }


    /**
     * Asynchronous task for establishing a connection with a Bluetooth device.
     * NOTE: For this task to run the bluetooth adapter must have been initialized correctly.
     */
    private class EstablishConnectionTask extends AsyncTask<String, String, Integer> {
        private final int CONNECTION_SUCCESS = 0;
        private final int NOT_PAIRED = 1;
        private final int CONNECTION_ERROR = 2;
        private ParcelUuid deviceUUID;
        private String deviceName;

        /**
         * Implcitly called when execute() is called on an AsynchTask of this type.
         * @param params should just be void
         * @return 1 if an error occurred attempting to establish a connection, 0 on success.
         */
        protected Integer doInBackground(String ...params) {
            /* Search for paired Bluetooth devices */
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

            if (pairedDevices.isEmpty()) {
                /* We are not paired with a device */
                return NOT_PAIRED;
            } else {
                /* We are already paired */
                for (BluetoothDevice device : pairedDevices) {
                    /*
                     * Assume this is the one we want to connect to and
                     * attempt to establish a connection
                     */
                    try {
                        deviceUUID = device.getUuids()[0];
                        deviceName = device.getName();
                        BTSocket = device.createRfcommSocketToServiceRecord(deviceUUID.getUuid());
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
                    return CONNECTION_ERROR;
                }

                return CONNECTION_SUCCESS;
            }
        }

        /**
         * Implicitly called when the task is done executing. This will update the UI with the
         * device UUID and name.
         */
        @Override
        protected void onPostExecute(Integer connectionResult) {
            if (connectionResult == NOT_PAIRED) {
                showBluetoothDialog("You must be paired with a device to perform this action.",
                        "Not Paired");
            } else if (connectionResult == CONNECTION_ERROR) {
                showBluetoothDialog("Sorry, an error occurred while attempting to connect to " +
                                "the remote device. Please make sure you are paired with the " +
                                "correct device.\n",
                        "Bluetooth Connection Failure");
            } else {
                TextView uuidView = (TextView) findViewById(R.id.uuid_value);
                uuidView.setText(deviceUUID.toString());
                TextView nameView = (TextView) findViewById(R.id.device_name_value);
                nameView.setText(deviceName);
            }
        }
    }


    /**
     * Asynchronous task for sending the calibrate command to a Bluetooth device.
     * NOTE: For this task to run the bluetooth adapter must have been initialized correctly.
     */
    private class SendCalibrateCommandTask extends AsyncTask<String, String, Integer> {
        @Override
        protected Integer doInBackground(String... params) {
            return null;
        }

        @Override
        protected void onPostExecute(Integer integer) {
            super.onPostExecute(integer);
        }
    }


    /**
     * Asynchronous task for streaming distance readings from a Bluetooth device.
     * NOTE: For this task to run the bluetooth adapter must have been initialized correctly.
     */
    private class StreamDistanceDataTask extends AsyncTask<String, String, Integer> {
        @Override
        protected Integer doInBackground(String... params) {
            return null;
        }

        @Override
        protected void onPostExecute(Integer integer) {
            super.onPostExecute(integer);
        }
    }

    /**
     * Asynchronous task for requesting adn receiving status information from a Bluetooth device.
     * NOTE: For this task to run the bluetooth adapter must have been initialized correctly.
     */
    private class GetDeviceStateTask extends AsyncTask<String, String, Integer> {

        /**
         * Send the Request Device Status Command
         * @param params
         * @return 1 on success and 0 on failure
         */
        @Override
        protected Integer doInBackground(String... params) {
            if (sendString("S")) {
                return 1;
            }
            return 0;
        }

        @Override
        protected void onPostExecute(Integer result) {
            if (result == 1) {
                /* Success */
                String data = receiveString(2000);

                if (data == null) {
                    showBluetoothDialog("Could not receive data from remote device.",
                            "Bluetooth Error");
                    return;
                }

                String[] values = data.split(",");

                String distance = values[0];
                String wifiStatus = values[1];
                String calibrationDist = values[2];
                String latitude = values[3];
                String longitude = values[4];

                TextView distView = (TextView) findViewById(R.id.dist_reading_value);
                TextView wifiView = (TextView) findViewById(R.id.wifi_status_value);
                TextView calDistView = (TextView) findViewById(R.id.calibration_dist_value);
                TextView GPSView = (TextView) findViewById(R.id.gps_data_value);

                distView.setText(distance);
                wifiView.setText(wifiStatus);
                calDistView.setText(calibrationDist);
                GPSView.setText(latitude + ", " + longitude);
            } else {
                showBluetoothDialog("Failed to send command to remote device.", "Bluetooth Error");
            }
        }
    }
}
