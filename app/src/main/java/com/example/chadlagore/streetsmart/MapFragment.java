package com.example.chadlagore.streetsmart;

import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * Created by chadlagore on 2017-03-10.
 */

public class MapFragment extends SupportMapFragment implements GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener,
    GoogleMap.OnInfoWindowClickListener,
    GoogleMap.OnMapLongClickListener,
    GoogleMap.OnMapClickListener,
    GoogleMap.OnMarkerClickListener,
    LocationListener,
    OnMapReadyCallback {

    private static final String TAG = "map_fragment";
    private GoogleApiClient mGoogleApiClient;
    private Location mCurrentLocation;
    private LocationManager mLocationManager;
    private LocationRequest mLocationRequest;
    private BitmapFactory bf;
    private GoogleMap googleMap;

    protected final int[] MAP_TYPES = {
            GoogleMap.MAP_TYPE_SATELLITE,
            GoogleMap.MAP_TYPE_NORMAL,
            GoogleMap.MAP_TYPE_HYBRID,
            GoogleMap.MAP_TYPE_TERRAIN,
            GoogleMap.MAP_TYPE_NONE
    };

    // initial location
    private int curMapTypeIndex = 1;

    /*
     * Connect to GoogleMapsAPI.
     */
    @Override
    public void onConnected(Bundle bundle) {
        mCurrentLocation = getLastLocation();
        getMapAsync(this);
    }

    /**
     * Asyncronously gets access to map.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        Log.i(TAG, "map ready.");

        if (mCurrentLocation != null) {
            handleNewLocation(mCurrentLocation);
        } else {
            Log.i(TAG, "Current location is null, looking up new.");
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, this);
        }

        initListeners();
    }

    /*
     * Update the map type given a mapTypeId.
     *
     * @param int mapTypeId. An integer corresponding to one of the map types in MAP_TYPES.
     */
    public void changeMapType(int mapTypeId) {
        Log.i(TAG, "Updating map type.");
        googleMap.setMapType(MAP_TYPES[mapTypeId]);
    }

    /*
     * Update camera with new location.
     *
     * @param Location location !=null a new location on to orient the camera on.
     */
    private void handleNewLocation(Location location) {
        Log.i(TAG, "Building camera position.");
        Log.i(TAG, "Lat: " + location.getLatitude());
        Log.i(TAG, "Lon: " + location.getLongitude());
        CameraPosition position = CameraPosition.builder()
                .target(new LatLng(location.getLatitude(),
                        location.getLongitude()))
                .zoom(16f)
                .build();

        googleMap.moveCamera(CameraUpdateFactory
                .newCameraPosition( position ));

        googleMap.setMapType( MAP_TYPES[curMapTypeIndex] );
        googleMap.setTrafficEnabled( true );
        googleMap.setMyLocationEnabled( true );
        googleMap.getUiSettings().setZoomControlsEnabled( true );
    }


    /*
     * Runs automatically on map startup.
     *
     * Connects to GoogleApiClient, creates LocationRequest
     * object to find user location..
     */
    @Override
    public void onStart() {
        Log.i(TAG, "Starting maps...");
        super.onStart();
        mGoogleApiClient.connect();

        // Create the LocationRequest object
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10 * 1000)        // 10 seconds, in milliseconds
                .setFastestInterval(1 * 1000); // 1 second, in milliseconds
    }

    /**
     * On map pause, we disconnect from the Google Maps API.
     * Reduces data consumption.
     */
    @Override
    public void onPause() {
        super.onPause();
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi
                    .removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
    }

    /**
     * Returns the last location collected by the LocationServices object.
     * @return Location is null when user has not enabled their location.
     */
    public Location getLastLocation() {
        return LocationServices
                .FusedLocationApi
                .getLastLocation(mGoogleApiClient);
    }

    /**
     * Method will create a marker, add it to the googleMap and return
     * a reference to it.
     *
     * @param latlng the latitude and longitude of the marker, provided
     *               from data form the server
     *
     * @param id the intersection id, which is the same as the ids for
     *           all the other markers associated with the same intersection
     *
     * @param title the title is <street_a> + " at " <street_b>
     *
     * @param color Thresholds for colors are defined in Intersection.class.
     *              Simply provide one of the three possible thresholds to
     *              set the marker to that color
     *
     * @return a reference to the marker which has been added to the map
     */
    public Marker createMarkerAddToMap(LatLng latlng, Long id, String title, long color) {

        return googleMap.addMarker( new MarkerOptions()
            .position(latlng)
            .title(title)
            .flat(false)
            .snippet(id.toString())
            .rotation((float) -15.0)
            .icon(getProperlyColoredIcon(color)));
    }

    /**
     * Method returns the correct icon for the provided level of busyness.
     * Thresholds for various colors are defined as static members of the
     * intersection class.
     *
     * @param color the value provided for the intersection from the
     *                 server about cars which have passed through during
     *                 the last minute
     *
     * @return the icon which should be used
     */
    public BitmapDescriptor getProperlyColoredIcon(Long color) {
        if (color.equals(Intersection.BLUE)) {
            return BitmapDescriptorFactory.fromResource(R.mipmap.bluetooth_icon);
        } else if (color.equals(Intersection.RED)) {
            return BitmapDescriptorFactory.fromResource(R.mipmap.level_red);
        } else if (color.equals(Intersection.YELLOW)) {
            return BitmapDescriptorFactory.fromResource(R.mipmap.level_yellow);
        } else {
            return BitmapDescriptorFactory.fromResource(R.mipmap.level_green);
        }
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

    /**
     * Method displays the custom dialog TraficGraph, which contains a title
     * (the street name), a graph updating in real time, and a button which
     * links to the HistoricalData Activity.
     *
     * @param marker the marker which the user has clicked on, needed because
     *               the intersections title is contained within the intersection
     *               object tagged to the marker in question.
     */
    private static void showDialog(Marker marker) {

        /* We need to collect the intersection to build the graph. */
        Long id = Long.valueOf(marker.getSnippet());
        Intersection intersection = ((MainActivity)getActivity()).getIntersection(id);

        /* Build graph. */
        TrafficGraph trafficGraph = TrafficGraph.newInstance(intersection);
        trafficGraph.show(getActivity().getFragmentManager(), "dialog_layout");
    }

    /**
     * Method is automatically called when the user clicks on a marker displayed
     * on the map.
     *
     * @param marker the marker the user clicked on
     *
     * @return returns true to indicate the click was handled
     */
    @Override
    public boolean onMarkerClick(Marker marker) {
        try {
            showDialog(marker);
        } catch (Exception e) {
            Log.i(TAG, "could not show dialog for intersection");
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /*
     * Create a new view and initialize listeners.
     */
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setHasOptionsMenu(true);

        Log.i(TAG, "Getting activity");
        mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    /*
     * Initialize map listeners.
     */
    private void initListeners() {
        if (googleMap != null) {
            googleMap.setOnMarkerClickListener(this);
            googleMap.setOnMapLongClickListener(this);
            googleMap.setOnInfoWindowClickListener(this);
            googleMap.setOnMapClickListener(this);
        } else {
            Log.i(TAG, "Google maps null, cannot update listeners.");
        }
    }

    /*
     * Response to location change. Simply call handleNewLocation method.
     */
    @Override
    public void onLocationChanged(Location location) {
        handleNewLocation(location);
    }

}
