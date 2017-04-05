package com.example.chadlagore.streetsmart;

import android.app.Activity;
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
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
    protected boolean streaming = false;
    protected View loader;
    protected List<AsyncTask> taskList;
    protected LineChart chart = null;
    protected LineData distanceData = null;
    protected LineData carCountData = null;
    protected int firstDistanceEntryIndex = 0;
    protected int firstCarCountEntryIndex = 0;


    /* Constants */
    private final int REQUEST_ENABLE_BT = 1;
    private final String BLUETOOTH = "BLUETOOTH";
    protected final String TASK_CANCELLED = "CANCELLED";

    /**
     * This is run when another activity calls startActivity() or startActivityForResult() with this
     * activity.
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(BLUETOOTH, "Starting BluetoothConnectionActivity");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_connection);

        /* Add the toolbar so we have access to the "Back" button */
        Toolbar myChildToolbar = (Toolbar) findViewById(R.id.bluetooth_connection_toolbar);
        setSupportActionBar(myChildToolbar);
        Toolbar appToolbar = (Toolbar) findViewById(bluetooth_connection_toolbar);
        setSupportActionBar(appToolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        loader = findViewById(R.id.progress_wheel);
        taskList = Collections.synchronizedList(new ArrayList<AsyncTask>());

        /* Set up distance plot */
        chart = (LineChart) findViewById(R.id.distance_chart);
        List<Entry> distanceEntries = new ArrayList<Entry>();
        distanceEntries.add(new Entry(0, 0));
        LineDataSet dataSet = new LineDataSet(distanceEntries, "Distance Readings");
        distanceData = new LineData(dataSet);
        carCountData = new LineData(dataSet);
        chart.setData(distanceData);
        chart.invalidate();

        /* Create a Bluetooth Adapter */
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            /*
             * Creating the Adapter probably failed because you are running in the emulator
             * Return to MainActivity
             */
            showBluetoothDialog("This device does not appear to have Bluetooth capabilities which "
                    + "are required for this action.", "Bluetooth Unavailable");
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
                loader.setVisibility(View.VISIBLE);
                EstablishConnectionTask task = new EstablishConnectionTask();
                task.execute();
            }
        }
    }

    /**
     * Adds the task to the task list, signals other AsynchTasks to abort, then starts the task.
     * Also displays loader.
     * NEVER call execute() on the task you pass to this function!
     */
    protected synchronized void startTask(AsyncTask task) {
        Log.d(BLUETOOTH, "Adding task to tasklist.");

        /* Cancel currently running tasks */
        for (AsyncTask currentTask : taskList) {
            if (currentTask instanceof SendCalibrateCommandTask) {
                ((SendCalibrateCommandTask) currentTask).cancel(true);
            }
            else if (currentTask instanceof GetDeviceStateTask) {
                ((GetDeviceStateTask) currentTask).cancel(true);
            }
            else if (currentTask instanceof StreamCarCountTask) {
                ((StreamCarCountTask) currentTask).cancel(true);
            }
            else ((StreamDistanceDataTask) currentTask).cancel(true);
        }

        if (streaming) {
            if (!(task instanceof StreamDistanceDataTask)) {
                /* We were streaming and the user started another task. Cancel stream! */
                streaming = false;
                Button streamButton = (Button) findViewById(R.id.stream_data_button);
                streamButton.setText("STREAM DISTANCE READINGS");
            } else if (!(task instanceof StreamCarCountTask)) {
                /*
                 * We were streaming car count data and the user started another task.
                 * Cancel stream!
                 */
                streaming = false;
                Button streamButton = (Button) findViewById(R.id.stream_car_count_button);
                streamButton.setText("STREAM CAR COUNT");
            }
        }

        taskList.add(task);

        Log.d(BLUETOOTH, "Starting task.");
        if (task instanceof SendCalibrateCommandTask) {
            loader.setVisibility(View.VISIBLE);
            ((SendCalibrateCommandTask) task).execute();
        }
        else if (task instanceof GetDeviceStateTask) {
            loader.setVisibility(View.VISIBLE);
            ((GetDeviceStateTask) task).execute();
        }
        else if (task instanceof StreamCarCountTask) {
            streaming = true;
            chart.setData(carCountData);
            chart.notifyDataSetChanged();
            ((StreamCarCountTask) task).execute();
        }
        else {
            streaming = true;
            chart.setData(distanceData);
            chart.notifyDataSetChanged();
            ((StreamDistanceDataTask) task).execute();
        }
    }

    /**
     * Removes the task from the taskList and hides loader.
     * @param task
     */
    protected synchronized void endTask(AsyncTask task) {
        Log.d(BLUETOOTH, "Removing task from tasklist.");

        taskList.remove(taskList.indexOf(task));
        if (taskList.isEmpty()) {
            loader.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Returns whether or not this task was cancelled regardless of its type.
     */
    protected boolean taskCancelled(AsyncTask task) {
        if (task instanceof GetDeviceStateTask) {
            return ((GetDeviceStateTask)task).isCancelled();
        } else if (task instanceof SendCalibrateCommandTask) {
            return ((SendCalibrateCommandTask) task).isCancelled();
        } else if (task instanceof  StreamCarCountTask) {
            return ((StreamCarCountTask) task).isCancelled();
        } else {
            return ((StreamDistanceDataTask)task).isCancelled();
        }
    }

    /**
     * Called when the "CALIBRATE" button is pressed
     */
    public void calibrate(View view) {
        Log.d(BLUETOOTH, "Calibrate button pressed.");
        startTask(new SendCalibrateCommandTask());
    }

    /**
     * Called when the "STREAM DISTANCE READINGS" button is pressed
     */
    public void streamDistanceData(View view) {
        Log.d(BLUETOOTH, "Stream Distance Readings button pressed.");
        Button streamButton = (Button) findViewById(R.id.stream_data_button);

        if (streaming) {
            streaming = false;
            streamButton.setText("STREAM DISTANCE READINGS");
        } else {
            streaming = true;
            streamButton.setText("CANCEL STREAM");
            startTask(new StreamDistanceDataTask());
        }
    }

    /**
     * Called when the "STREAM CAR COUNT" button is pressed
     */
    public void streamCarCount(View view) {
        Log.d(BLUETOOTH, "Stream Car Count button pressed.");
        Button streamButton = (Button) findViewById(R.id.stream_car_count_button);

        if (streaming) {
            streaming = false;
            streamButton.setText("STREAM CAR COUNT");
        } else {
            streaming = true;
            streamButton.setText("CANCEL STREAM");
            startTask(new StreamCarCountTask());
        }
    }

    /**
     * Called when the "GET DEVICE STATUS" button is pressed
     */
    public void status(View view) {
        Log.d(BLUETOOTH, "Device Status button pressed.");
        startTask(new GetDeviceStateTask());
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

        if (BTSocket != null) {
            try {
                BTSocket.close();
            } catch (IOException e) {
                /* If this fails the socket was already closed or destroyed, so nothing to do */
            }
        }
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
     * Receive a string from the remote Bluetooth device we are currently connected to
     * Precondition: must be connected to the remote device and the inputStream can't be null
     * WARNING: this function is blocking! Make sure it is called in a non-main thread!
     * @param timeoutMillis timeout for this function in milliseconds. Pass 0 for no timeout.
     * @return the string received if it was received in full within the timeout period,
     * otherwise return null
     */
    @Nullable
    private String receiveString(int timeoutMillis, AsyncTask thisTask) {
        inputBuffer = new byte[2048];
        String dataReceived = "";
        boolean started = false;
        int bytesRead;

        long startTime = System.currentTimeMillis();

        while (true) {
            if (taskCancelled(thisTask)) return TASK_CANCELLED;

            try {
                bytesRead = inputStream.read(inputBuffer);

                if (inputBuffer[0] == '$' || started) {
                    started = true;
                    dataReceived += new String(inputBuffer, 0, bytesRead);
                }

                if (inputBuffer[0] == '\n' && started) {
                    return dataReceived.replace("$", "").replace("\n","");
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
    private synchronized boolean sendString(String data) {
        try {
            outputStream.write(data.getBytes());
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
                        getSocketStreams();
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

            loader.setVisibility(View.INVISIBLE);
        }
    }


    /**
     * Asynchronous task for sending the calibrate command to a Bluetooth device.
     * NOTE: For this task to run the bluetooth adapter must have been initialized correctly.
     */
    private class SendCalibrateCommandTask extends AsyncTask<String, String, Integer> {
        private final int CALIBRATION_SUCCESS = 0;
        private final int NOT_CALIBRATED = 1;

        /**
         * Implicitly called when execute() is called on an AsynchTask of this type.
         * @param params should just be void
         * @return 1 if an error occurred attempting to establish a connection, 0 on success.
         */
        @Override
        protected Integer doInBackground(String... params) {
            Log.d(BLUETOOTH, "SendCalibrateCommandTask started.");

            /* Send command to bluetooth for calibration of distance sensor via NIOS */
            if(!sendString("C")) {
              /* Calibration unsuccessful */
                return NOT_CALIBRATED;
            }
            else {
                return CALIBRATION_SUCCESS;
            }
        }

        /**
         * Called when doInBackground finishes ONLY if .cancel(true) was called on this task
         */
        @Override
        protected void onCancelled() {
            endTask(this);
        }

        /**
         * Implicitly called when the task is done executing. This will update the UI with the
         * device UUID and name.
         */
        @Override
        protected void onPostExecute(Integer calibrateResult) {
            endTask(this);

            if (calibrateResult == CALIBRATION_SUCCESS) {
                String calibrationDist = receiveString(2000, this);

                if (calibrationDist == null) {
                    showBluetoothDialog("No calibration distance received.",
                            "Bluetooth Error");
                    return;
                } else if (calibrationDist.equals(TASK_CANCELLED)) {
                    return;
                }

                /* Update calibration distance on UI */
                TextView calDistView = (TextView) findViewById(R.id.calibration_dist_value);
                calDistView.setText(calibrationDist.replace("C", "") + " cm");
            } else {
                showBluetoothDialog("Failed to send calibration command to remote device.",
                        "Bluetooth Error");
            }

            Log.d(BLUETOOTH, "SendCalibrateCommandTask done.");
        }
    }


    /**
     * Asynchronous task for streaming distance readings from a Bluetooth device.
     * NOTE: For this task to run the bluetooth adapter must have been initialized correctly.
     */
    private class StreamDistanceDataTask extends AsyncTask<String, String, Integer> {
        private final int SUCCESS = 0;
        private final int FAILURE = 1;
        private final int CANCELLED = 2;

        /**
         * Send the Request Distance Reading Stream Command
         * @param params unused
         * @return 0 on success and 1 on failure
         */
        @Override
        protected Integer doInBackground(String... params) {
            Log.d(BLUETOOTH, "StreamDistanceDataTask started.");

            if (sendString("D")) {
                String data;

                while (streaming) {
                    data = receiveString(3000, this);
                    if (data.equals(TASK_CANCELLED)) return CANCELLED;
                    else if (data != null) {
                        publishProgress(data.replace("D", ""));
                    }
                }
            } else {
                return FAILURE;
            }
            return SUCCESS;
        }

        @Override
        protected void onProgressUpdate(String... data) {
            TextView distanceView = (TextView) findViewById(R.id.dist_reading_value);
            distanceView.setText(data[0] + " cm");
            float distanceReading = Float.parseFloat(data[0]);
            distanceData.addEntry(new Entry(distanceData.getEntryCount(), distanceReading), 0);

            /* If the chart is getting full remove its first data point */
            if (distanceData.getDataSetCount() >= 10) {
                Log.d(BLUETOOTH, "Removing entry from distance chart");
                distanceData.removeEntry(firstDistanceEntryIndex, 0);
                firstDistanceEntryIndex++;
            }

            chart.notifyDataSetChanged();
            chart.invalidate();
        }

        @Override
        protected void onCancelled() {
            sendString("X");
            endTask(this);
        }

        @Override
        protected void onPostExecute(Integer result) {
            /* Tell the remote device to stop streaming */
            sendString("X");

            if (result == FAILURE) {
                showBluetoothDialog("Failed to receive data from remote device.",
                        "Bluetooth Error");
            }

            Log.d(BLUETOOTH, "StreamDistanceDataTask done.");
            endTask(this);
        }
    }

    /**
     * Asynchronous task for requesting adn receiving status information from a Bluetooth device.
     * NOTE: For this task to run the bluetooth adapter must have been initialized correctly.
     */
    private class GetDeviceStateTask extends AsyncTask<String, String, Integer> {
        private final int SUCCESS = 0;
        private final int FAILURE = 1;

        /**
         * Send the Request Device Status Command
         * @param params unused
         * @return SUCCESS or FAILURE
         */
        @Override
        protected Integer doInBackground(String... params) {
            Log.d(BLUETOOTH, "GetDeviceStateTask started.");

            if (sendString("S")) {
                return SUCCESS;
            }
            return FAILURE;
        }

        @Override
        protected void onCancelled() {
            endTask(this);
        }

        @Override
        protected void onPostExecute(Integer result) {
            if (result == SUCCESS) {
                String data = receiveString(5000, this);

                if (data == null) {
                    showBluetoothDialog("Could not receive data from remote device.",
                            "Bluetooth Error");
                    endTask(this);
                    return;
                }

                String[] values = data.split(",");

                if (values.length < 5) {
                    showBluetoothDialog("Invalid data received from device.", "Bluetooth Error");
                    endTask(this);
                    return;
                }

                /* Update UI with data we received */
                String distance = values[0].replace("S", "");
                String wifiStatus = values[1];
                String calibrationDist = values[2];
                String latitude = values[3];
                String longitude = "-" + values[4]; /* The longitude comes in with the wrong sign */

                TextView distView = (TextView) findViewById(R.id.dist_reading_value);
                TextView wifiView = (TextView) findViewById(R.id.wifi_status_value);
                TextView calDistView = (TextView) findViewById(R.id.calibration_dist_value);
                TextView GPSView = (TextView) findViewById(R.id.gps_data_value);

                distView.setText(distance + " cm");
                wifiView.setText(wifiStatus);
                calDistView.setText(calibrationDist + " cm");
                GPSView.setText(latitude + ", " + longitude);

                /* Add this device to MainActivity's map */
                Globals.setBluetoothIntersection(latitude, longitude);
            } else {
                showBluetoothDialog("Failed to send command to remote device.", "Bluetooth Error");
            }

            Log.d(BLUETOOTH, "GetDeviceStateTask done.");
            endTask(this);
        }
    }

    /**
     * Asynchronously stream car count from remove Blueooth device
     * NOTE: For this task to run the Bluetooth addapter must have been initialized correctly.
     */
    private class StreamCarCountTask extends AsyncTask<String, String, Integer> {
        private final Integer SUCCESS = 0;
        private final Integer FAILED = 1;

        @Override
        protected Integer doInBackground(String... params) {
            Log.d(BLUETOOTH, "Streaming car count data.");
            String data;

            if (sendString("O")) {

                while (streaming) {
                    data = receiveString(7000, this);

                    if (data == null || data == TASK_CANCELLED) {
                        return FAILED;
                    }

                    publishProgress(data);
                }
                return SUCCESS;
            } else {
                return FAILED;
            }
        }

        @Override
        protected void onProgressUpdate(String ...data) {
            int carCount = Integer.parseInt(data[0]);
            Log.d(BLUETOOTH, "Updating car count graph");

            // TODO: getEntryCount might be a problem
            carCountData.addEntry(new Entry(carCountData.getEntryCount(), carCount), 0);

            /* If the chart is getting full remove its first data point */
            if (carCountData.getDataSetCount() >= 10) {
                carCountData.removeEntry(firstCarCountEntryIndex, 0);
                firstCarCountEntryIndex++;
            }

            chart.notifyDataSetChanged();
            chart.invalidate();
        }

        @Override
        protected void onCancelled() {
            sendString("X");
            endTask(this);
        }

        @Override
        protected void onPostExecute(Integer result) {
            Log.d(BLUETOOTH, "StreamCarCountTask done.");
            sendString("X");

            if (result == FAILED) {
                showBluetoothDialog("Bluetooth Error", "No data received from remote device.");
            }
            endTask(this);
        }
    }
}
