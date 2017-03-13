package com.example.chadlagore.streetsmart;

import android.support.v7.app.AppCompatActivity;

import com.google.android.gms.maps.OnMapReadyCallback;
import android.content.Intent;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import static com.example.chadlagore.streetsmart.R.id.app_toolbar;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    MapFragment mapFragment;
    GoogleMap googleMap;
    Timer updateMapTimer;
    boolean stopTimer = false;
    boolean addMarker = false;

    Random RAND = new Random();

    int updateMapTime = 500; // ms
    int updateMapDelay = 5000; // ms
    double markerLat = 49.000;
    double markerLon = 122.000;

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

        /** Initialize the map overlay timer. */
        if (addMarker) {
            initMapUpdateTimer();
        }
        mapFragment = getMapFragement();
    }

    /* Inflate toolbar menu */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    /** Initializes the map updater. */
    private void initMapUpdateTimer() {
        Timer updateMapTimer = new Timer();
        UpdateMapTask my_task = new UpdateMapTask();
        updateMapTimer.schedule(my_task, updateMapDelay, updateMapTime);
    }

    /** Asyncronously gets access to map. */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        googleMap = googleMap;
        initMapUpdateTimer();
    }

    /** Class performs async updates to map. */
    public class UpdateMapTask extends TimerTask {
        public void run() {
            markerLat += 1;
            markerLon += 1;

            if (!stopTimer) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        updateMapMarkers();
                    }
                });
            }
        }
    }

    /** Function to periodically update map markers. */
    private void updateMapMarkers() {
        Log.i("gmaps_timer", "updating map markers");

        // Change this!
        Location lastLoc = mapFragment.getLastLocation();
        lastLoc.setLatitude(lastLoc.getLatitude() + RAND.nextDouble() / 50);
        lastLoc.setLongitude(lastLoc.getLongitude() + RAND.nextDouble() / 50);
        mapFragment.addMarker(lastLoc);
    }

    /** Handles Terrain button click. */
    public boolean onOptionsItemSelected(MenuItem item) {
        FragmentManager getSupportFragmentManager;
        MapFragment fragment = (MapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        switch (item.getItemId()) {
            case R.id.normal_button:
                fragment.changeMapType(fragment.MAP_TYPES[GoogleMap.MAP_TYPE_NORMAL]);
                return true;

            case R.id.hybrid_button:
                fragment.changeMapType(fragment.MAP_TYPES[GoogleMap.MAP_TYPE_HYBRID]);
                return true;

            case R.id.terrain_button:
                fragment.changeMapType(fragment.MAP_TYPES[GoogleMap.MAP_TYPE_TERRAIN]);
                return true;

            case R.id.satellite_button:
                fragment.changeMapType(fragment.MAP_TYPES[GoogleMap.MAP_TYPE_NONE]);
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }

    /** Returns map fragment. */
    public MapFragment getMapFragement() {
        return (MapFragment)getSupportFragmentManager().findFragmentById(R.id.map);
    }

    @Override
    public void onPause() {
        super.onPause();
        stopTimer = true;
    }

    @Override
    public void onResume() {
        super.onResume();
        stopTimer = false;
    }
}
