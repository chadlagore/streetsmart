package com.example.chadlagore.streetsmart;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;

import java.util.List;

import static com.example.chadlagore.streetsmart.R.id.app_toolbar;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /* Expand the toolbar at the top of the screen */
        Toolbar appToolbar = (Toolbar) findViewById(app_toolbar);
        setSupportActionBar(appToolbar);

        /* Add "DEVICE" button event listener */
        final Button deviceConnect = (Button) findViewById(R.id.deviceConnect);
        deviceConnect.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), BluetoothConnectionActivity.class);
                setContentView(R.layout.activity_bluetooth_connection);
                startActivity(intent);
            }
        });
    }

    /* Inflate toolbar menu */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        MapFragment fragment = (MapFragment)getSupportFragmentManager()
                .findFragmentById(R.id.map);

        switch (item.getItemId()) {
            case R.id.satellite_button:
                fragment.changeMapType(fragment.MAP_TYPES[GoogleMap.MAP_TYPE_SATELLITE]);
                return true;

            case R.id.normal_button:
                fragment.changeMapType(fragment.MAP_TYPES[GoogleMap.MAP_TYPE_NORMAL]);
                return true;

            case R.id.hybrid_button:
                fragment.changeMapType(fragment.MAP_TYPES[GoogleMap.MAP_TYPE_HYBRID]);
                return true;

            case R.id.terrain_button:
                fragment.changeMapType(fragment.MAP_TYPES[GoogleMap.MAP_TYPE_TERRAIN]);
                return true;

            case R.id.none_button:
                fragment.changeMapType(fragment.MAP_TYPES[GoogleMap.MAP_TYPE_NONE]);
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }
}
