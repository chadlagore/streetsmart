package com.example.chadlagore.streetsmart;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

/**
 * Created by devin on 2017-03-14.
 *
 * Class provides an abstraction for an intersection. It contains location,
 * name, and current conditions data. When plotting current conditions on a
 * map across the city, the last_minute_passthroughs is taken as the metric
 * defining current conditions --either red (bad), yellow (ok), or green (good).
 */

public class Intersection {

    // Location data
    private double intersection_latituide;
    private double intersection_longitude;

    // Intersection name and id data
    private String crossroad_first;
    private String crossroad_second;
    private long intersection_id;

    // Map this intersection is associated with
    private MapFragment mapFragment;

    // Intersection markers. Three are needed as markers
    // cannot be updated at runtime.
    private Marker green;
    private Marker red;
    private Marker yellow;

    // Current conditions at intersection
    private long last_minute_passthroughs;

    /**
     * Constructor. Requires all the parameters that define an intersection.
     * Requests to the server for intersection level data return each of
     * these in json format.
     *
     * @param intersection_latituide latitude of traffic sensor
     *
     * @param intersection_longitude longitude of traffic sensor
     *
     * @param crossroad_first either of the crossroads of the intersection
     *
     * @param crossroad_second the other intersection crossroad
     *
     * @param last_minute_passthroughs passthroughs the intersection during the last
     *                                 minute, provided as the default measurement
     *                                 by the server.
     *
     * @param intersection_id id of the intersection, assigned on the server
     *
     * @param mapFragment the map fragment this intersection is associated with
     */
    public Intersection (double intersection_latituide,
                         double intersection_longitude,
                         String crossroad_first,
                         String crossroad_second,
                         long intersection_id,
                         long last_minute_passthroughs,
                         MapFragment mapFragment) {

        this.mapFragment = mapFragment;

        // Simply set instance variables
        this.intersection_id = intersection_id;
        this.intersection_longitude = intersection_longitude;
        this.intersection_latituide = intersection_latituide;
        this.crossroad_second = crossroad_second;
        this.crossroad_first = crossroad_first;
        this.mapFragment = mapFragment;

        // We'll need a latlng to create markers for the map
        LatLng latlng = new LatLng(this.intersection_latituide, this.intersection_longitude);

        // create the markers associated with this intersection
        this.green = mapFragment.addMarker(latlng);
        this.red = mapFragment.addMarker(latlng);
        this.yellow = mapFragment.addMarker(latlng);

        // initially set the to invisible
        this.green.setVisible(false);
        this.yellow.setVisible(false);
        this.red.setVisible(false);

        this.green.setSnippet(String.valueOf(intersection_id));
        this.yellow.setSnippet(String.valueOf(intersection_id));
        this.red.setSnippet(String.valueOf(intersection_id));

        // set the marker that is appropriately colored to visible
        setPassthroughsLastMinute(last_minute_passthroughs);
    }

    /**
     * Method will return one of the three markers associated with this
     * intersection. Which one is returned is dependent on the busyness
     * of the intersection.
     *
     * @return the marker that is currently visible
     */
    public Marker getMarker () {
        if (this.red.isVisible()) return this.red;
        else if (this.yellow.isVisible()) return this.yellow;
        else return this.green;
    }

    /**
     * Name is taken to be the crossroads concatenated together between
     * the string "at".
     *
     * @return String in the form '<crossroad_first> at <crossroad_second>'
     * */
    public String getIntersectionName() {
        return this.crossroad_first.concat(" at " + this.crossroad_second);
    }

    /**
     * Returns the intersection's latitude.
     *
     * @return the latitude, as double
     */
    public double getLatitude() {
        return this.intersection_latituide;
    }

    /**
     * Get the longitude of the intersection.
     *
     * @return longitude, as double
     */
    public double getLongitude() {
        return this.intersection_longitude;
    }

    /**
     * Each intersection is assinged an id corresponding to an entry
     * in the database that is persistent through time. Method returns
     * this ID.
     *
     * @return intersection id, as long
     */
    public long getIntersectionID() {
        return this.intersection_id;
    }

    /**
     * Every minute, each intersection will have a new count of
     * passthroughs associated with it.
     *
     * @return passthroughs in the last minute, as long
     */
    public long getPassthroughsLastMinute() {
        return this.last_minute_passthroughs;
    }

    /**
     * Method will update the number of passthroughs during the
     * last minute. This is the only member of the class which
     * should change over time. It will also set the appropriately
     * colored marker to visible.
     *
     * @param passthroughs an integer value, representing the number
     *                     of cars passing through the intersection during
     *                     the last minute. Used as the basis -- for now --
     *                     of determining the busyness of the intersection.
     *                     Data is provided by default in this format from
     *                     the server.
     */
    public void setPassthroughsLastMinute(long passthroughs) {
        this.last_minute_passthroughs = passthroughs;

        if (this.last_minute_passthroughs > 10) {
            red.setVisible(true);
        } else if (this.last_minute_passthroughs < 10 && this.last_minute_passthroughs >= 5) {
            yellow.setVisible(true);
        } else {
            green.setVisible(true);
        }
    }

}
