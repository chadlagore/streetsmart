package com.example.chadlagore.streetsmart;

import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    MapFragment mapFragment;
    GoogleMap googleMap;
    Timer updateMapTimer;

    int updateMapTime = 5000; // ms
    int updateMapDelay = 3000; // ms
    double markerLat = 49.000;
    double markerLon = 122.000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mapFragment = getMapFragement();
        mapFragment.getMapAsync(this);
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

            runOnUiThread(new Runnable() {
                public void run() {
                    updateMapMarkers();
                }
            });
        }
    }

    private void updateMapMarkers() {
        if (mapFragment == null) {
            Log.i("gmaps_timer", "map null");
        }

        Log.i("gmaps_timer", "updating map markers");
    }

    /** Handles satellite button click. */
    public void onSatelliteClick(View view) {
        FragmentManager getSupportFragmentManager ;
        MapFragment fragment = (MapFragment)getSupportFragmentManager()
                .findFragmentById(R.id.map);
        fragment.changeMapType(fragment.MAP_TYPES[GoogleMap.MAP_TYPE_SATELLITE]);
    }

    /** Handles normal button click. */
    public void onNormalClick(View view) {
        FragmentManager getSupportFragmentManager ;
        MapFragment fragment = (MapFragment)getSupportFragmentManager()
                .findFragmentById(R.id.map);
        fragment.changeMapType(fragment.MAP_TYPES[GoogleMap.MAP_TYPE_NORMAL]);
    }

    /** Handles hybrid button click. */
    public void onHybridClick(View view) {
        FragmentManager getSupportFragmentManager ;
        MapFragment fragment = (MapFragment)getSupportFragmentManager()
                .findFragmentById(R.id.map);
        fragment.changeMapType(fragment.MAP_TYPES[GoogleMap.MAP_TYPE_HYBRID]);
    }

    /** Handles Terrain button click. */
    public void onTerrainClick(View view) {
        FragmentManager getSupportFragmentManager ;
        MapFragment fragment = (MapFragment)getSupportFragmentManager()
                .findFragmentById(R.id.map);
        fragment.changeMapType(fragment.MAP_TYPES[GoogleMap.MAP_TYPE_TERRAIN]);
    }

    /** Handles None button click. */
    public void onNoneClick(View view) {
        FragmentManager getSupportFragmentManager ;
        MapFragment fragment = (MapFragment)getSupportFragmentManager()
                .findFragmentById(R.id.map);
        fragment.changeMapType(fragment.MAP_TYPES[GoogleMap.MAP_TYPE_NONE]);
    }

    /** Returns map fragment. */
    public MapFragment getMapFragement() {
        return (MapFragment)getSupportFragmentManager().findFragmentById(R.id.map);
    }
}
