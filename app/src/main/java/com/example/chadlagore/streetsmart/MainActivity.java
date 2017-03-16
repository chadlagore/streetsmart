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
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import org.json.JSONArray;

import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Array;
import java.util.ArrayList;
import java.util.HashMap;
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
    JSONArray intersectionsJSON;

    boolean stopTimer = false;
    boolean addMarker = true;

    // Master list of markers being tracked --may want
    // to update to a resizable array in the future. When the
    // app is opened, the first batch of data will be used to
    // define the elements of this array
    private HashMap<Integer, Intersection> intersections = new HashMap<Integer, Intersection>();

    // thresholds for levels of busyness
    private static final long GREEN = 1;
    private static final long YELLOW = 5;
    private static final long RED = 10;

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

    /**
     * Initializes the map updater.
     */
    private void initMapUpdateTimer() {
        Timer updateMapTimer = new Timer();
        UpdateMapTask my_task = new UpdateMapTask();
        updateMapTimer.schedule(my_task, updateMapDelay, updateMapTime);
    }

    /**
     * Asyncronously gets access to map.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        googleMap = googleMap;
        initMapUpdateTimer();
    }

    /**
     * Class performs async updates to map.
     *
     * Runs every <code>updateMapTime</code>ms after an initial delay of <code>updateMapDelay</code>.
     * The StreetSmartAPI is queried for new data, this task occurs asynchronously. Then the markers
     * on the map are updated to match with the most recently available data. This data likely does
     * not correspond to the that of the current API call.
     */
    public class UpdateMapTask extends TimerTask {
        public void run() {

            if (!stopTimer) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        collectNewIntersectionData(
                                new LatLngBounds(new LatLng(49.26, -123.10),
                                        new LatLng(49.27, -123.12)));
                    }
                });
            }
        }
    }

    /**
     * Method will update map markers with new data on current conditions.
     * Update frequency is set by updateMapDelay and updateMapTime which are
     * instance variables of this class.
     *
     * Maps will be displayed with markers in green, red and yellow, depending
     * on how busy they have been during the last 30 seconds.
     */
    private void updateMapMarkers() {
        Log.i("gmaps_timer", "updating map markers");

        for (int i = 0; i < intersectionsJSON.length(); i++) {

            try {

                // if the intersection object and its associated markers
                // has already been created, simply update the intersection
                // with the most recent number of passthroughs during the last minute
                if (intersections.containsKey(intersectionsJSON.getJSONObject(i).getInt("id"))) {

                    // get the json obj at index 1
                    JSONObject jsonobj = intersectionsJSON.getJSONObject(i);

                    // set the number of passthroughs to new value
                    intersections.get(jsonobj.getInt("id"))
                            .setPassthroughsLastMinute((long) jsonobj.getDouble("cars"));

                // else we need to create the intersection
                } else {

                    // get the json object at index i
                    JSONObject jsonobj = intersectionsJSON.getJSONObject(i);

                    // create the intersection using data from the server
                    Intersection intersect = new Intersection(
                            jsonobj.getDouble("latitude"),
                            jsonobj.getDouble("longitude"),
                            jsonobj.getString("street_a"),
                            jsonobj.getString("street_b"),
                            (long) jsonobj.getDouble("cars"),
                            jsonobj.getInt("id"),
                            this.mapFragment
                    );

                    // add to the list of intersections
                    intersections.put(jsonobj.getInt("id"), intersect);
                }

            // if there was an error while parsing the
            // response, raise an error
            } catch (Exception e) {
                e.printStackTrace();
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

        /* Ask client to update asyncronously. */
        streetSmartClient.updateIntersections(bounds);

        /* Get latest data. */
        JSONArray response = streetSmartClient.getLatestIntersections();

        /* Test if we have a response yet. */
        if (response != null) {
            intersectionsJSON = streetSmartClient.responseJSON;
            updateMapMarkers();
        }
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
     *
     * @param requestCode
     *
     * @param resultCode
     *
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
