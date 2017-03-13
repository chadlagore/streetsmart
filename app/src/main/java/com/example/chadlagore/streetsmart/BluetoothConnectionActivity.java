package com.example.chadlagore.streetsmart;

import android.bluetooth.BluetoothAdapter;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.widget.TextView;

import org.w3c.dom.Text;

import static com.example.chadlagore.streetsmart.R.id.app_toolbar;
import static com.example.chadlagore.streetsmart.R.id.bluetooth_connection_toolbar;

/**
 * Created by bfbachmann on 2017-03-13.
 */

public class BluetoothConnectionActivity extends AppCompatActivity {
    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_connection);

        Toolbar myChildToolbar =
                (Toolbar) findViewById(R.id.bluetooth_connection_toolbar);
        setSupportActionBar(myChildToolbar);

        /* Expand the toolbar at the top of the screen */
        Toolbar appToolbar = (Toolbar) findViewById(bluetooth_connection_toolbar);
        setSupportActionBar(appToolbar);

        /* Get a support ActionBar corresponding to this toolbar */
        ActionBar ab = getSupportActionBar();

        /* Enable the Up button */
        ab.setDisplayHomeAsUpEnabled(true);

//        showDevice();
    }

    public void showDevice() {
        TextView deviceInfo = new TextView(getApplicationContext());

        /* Create new textView for message */
        deviceInfo.setText("Hello World!");
    }
}
