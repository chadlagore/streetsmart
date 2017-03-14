package com.example.chadlagore.streetsmart;

import android.Manifest;
import android.app.Dialog;
import android.support.v4.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.List;

/**
 * Created by chadlagore on 2017-03-10.
 */

public class MapFragment extends SupportMapFragment implements GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener,
    GoogleMap.OnInfoWindowClickListener,
    GoogleMap.OnMapLongClickListener,
    GoogleMap.OnMapClickListener,
    GoogleMap.OnMarkerClickListener,
    LocationListener {

    private GoogleApiClient mGoogleApiClient;
    private Location mCurrentLocation;
    private LocationManager mLocationManager;
    private LocationRequest mLocationRequest;

    protected final int[] MAP_TYPES = {
            GoogleMap.MAP_TYPE_SATELLITE,
            GoogleMap.MAP_TYPE_NORMAL,
            GoogleMap.MAP_TYPE_HYBRID,
            GoogleMap.MAP_TYPE_TERRAIN,
            GoogleMap.MAP_TYPE_NONE
    };

    private int curMapTypeIndex = 1;
    private double vancouverLat = 49.2827;
    private double vancouverLon = 123.1207;

    /*
     * Connect to GoogleMapsAPI.
     */
    @Override
    public void onConnected(Bundle bundle) {
        mCurrentLocation = getLastLocation();

        if (mCurrentLocation != null) {
            handleNewLocation(mCurrentLocation);
        } else {
            Log.i("gmaps", "Current location is null, looking up new.");
            LocationServices.FusedLocationApi.requestLocationUpdates(
                        mGoogleApiClient, mLocationRequest, this);
        }
    }

    /*
     * Update the map type given a mapTypeId.
     */
    public void changeMapType(int mapTypeId) {
        Log.i("gmaps", "Updating map type.");
        getMap().setMapType(MAP_TYPES[mapTypeId]);
    }

    /*
     * Update camera for new position.
     */
    private void handleNewLocation( Location location ) {
        Log.i("gmaps", "Building camera position.");
        Log.i("gmaps", "Lat: " + location.getLatitude());
        Log.i("gmaps", "Lon: " + location.getLongitude());
        CameraPosition position = CameraPosition.builder()
                .target(new LatLng(location.getLatitude(),
                        location.getLongitude()))
                .zoom(16f)
                .build();

        getMap().moveCamera(CameraUpdateFactory
                .newCameraPosition( position ));

        getMap().setMapType( MAP_TYPES[curMapTypeIndex] );
        getMap().setTrafficEnabled( true );
        getMap().setMyLocationEnabled( true );
        getMap().getUiSettings().setZoomControlsEnabled( true );
    }


    /*
     * Connects to GoogleApiClient, create LocationRequest.
     */
    @Override
    public void onStart() {
        Log.i("gmaps", "Starting maps...");
        super.onStart();
        mGoogleApiClient.connect();

        // Create the LocationRequest object
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10 * 1000)        // 10 seconds, in milliseconds
                .setFastestInterval(1 * 1000); // 1 second, in milliseconds
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi
                    .removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
    }

    public Location getLastLocation() {
        return LocationServices
                .FusedLocationApi
                .getLastLocation(mGoogleApiClient);
    }

    public void addMarker(Location loc) {
        getMap().addMarker(new MarkerOptions()
                .position(new LatLng(loc.getLatitude(), loc.getLongitude()))
                .title("new pointer")
                .snippet("close to you!")
                .rotation((float) -15.0)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
        );
    }

    @Override
    public void onResume() {
        super.onResume();
        mGoogleApiClient.connect();
    }


    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public void onInfoWindowClick(Marker marker) {

    }

    @Override
    public void onMapClick(LatLng latLng) {

    }

    @Override
    public void onMapLongClick(LatLng latLng) {

    }

    private void showDialog(Marker marker) {
        TrafficGraph trafficGraph = TrafficGraph.newInstance("Some Title");
        trafficGraph.show(getActivity().getFragmentManager(), "dialog_layout");
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        showDialog(marker);
        return true;
    }

    /*
     * Create a new view and initialize listeners.
     */
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.i("gmaps", "View created.");

        setHasOptionsMenu(true);

        mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        initListeners();
    }

    /*
     * Initialize map listeners.
     */
    private void initListeners() {
        getMap().setOnMarkerClickListener(this);
        getMap().setOnMapLongClickListener(this);
        getMap().setOnInfoWindowClickListener( this );
        getMap().setOnMapClickListener(this);
    }

    /*
     * Response to location change.
     */
    @Override
    public void onLocationChanged(Location location) {
        handleNewLocation(location);
    }
}
