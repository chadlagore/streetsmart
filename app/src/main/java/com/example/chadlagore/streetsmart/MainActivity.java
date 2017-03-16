package com.example.chadlagore.streetsmart;

import android.support.v7.app.AppCompatActivity;

import com.google.android.gms.maps.OnMapReadyCallback;
import android.content.Intent;
import android.location.Location;
import android.support.v4.app.FragmentManager;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import org.json.JSONArray;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import static com.example.chadlagore.streetsmart.R.id.app_toolbar;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    static final int DE1_CONFIG = 1;

    MapFragment mapFragment;
    GoogleMap googleMap;
    Timer updateMapTimer;
    StreetSmartClient streetSmartClient;
    JSONArray intersections;

    boolean stopTimer = false;
    boolean addMarker = true;
    int updateMapTime = 5000; // ms
    int updateMapDelay = 100; // ms
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
                startActivityForResult(intent, DE1_CONFIG);
            }
        });

        /** Initialize the map overlay timer. */
        if (addMarker) {
            initMapUpdateTimer();
        }

        /** Get the new map fragment. */
        mapFragment = getMapFragement();

        /** Instantiate the street smart API client. */
        streetSmartClient = new StreetSmartClient();
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

            if (!stopTimer) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        collectNewIntersectionData(
                                new LatLngBounds(new LatLng(49.25, -123.10),
                                        new LatLng(49.27, -123.15)));
                    }
                });
            }
        }
    }

    /**
     * Calls the street smart API for new intersection data.
     * @param bounds != null
     *      Boundaries on the map requested.
     * @effects updates this.intersections with the new data.
     */
    private void collectNewIntersectionData(LatLngBounds bounds) {
        JSONArray response = streetSmartClient.getIntersection(bounds);
        if (streetSmartClient.responseJSON != null) {
//            Log.i("gmaps_timer", streetSmartClient.responseJSON.toString());
            intersections = streetSmartClient.responseJSON;
        }
    }

    /** Function to periodically update map markers. */
    private void updateMapMarkers() {
        Log.i("gmaps_timer", "updating map markers");
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

    /**
     * This is method gets invoked when an activity that was started from MainActivity
     * using startActivityForResult() returns control to MainActivity using finish().
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        /* Refresh MainActivity */
        Intent refresh = new Intent(this, MainActivity.class);
        startActivity(refresh);
        this.finish();
    }
}
