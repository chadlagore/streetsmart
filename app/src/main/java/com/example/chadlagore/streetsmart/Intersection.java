package com.example.chadlagore.streetsmart;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by chadlagore on 2017-03-15.
 */

public class Intersection {
    private long id;
    private JSONObject jObj;
    private LatLng coordinates;

    private String street_b;


    public Intersection(JSONObject jObj) {
        this.jObj = jObj;
        try {
            this.id = jObj.getLong("id");
            coordinates = new LatLng(jObj.getDouble("latitude"), jObj.getDouble("longitude"));
            this.cars = jObj.getDouble("cars");
            this.street_a = jObj.getString("street_a");
            this.street_b = jObj.getString("street_b");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public long getId() {
        return id;
    }

    public JSONObject getjObj() {
        return jObj;
    }

    public double getCars() {
        return cars;
    }

    public LatLng getCoordinates() {
        return coordinates;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setjObj(JSONObject jObj) {
        this.jObj = jObj;
    }

    public void setCoordinates(LatLng coordinates) {
        this.coordinates = coordinates;
    }

    public void setCars(double cars) {
        this.cars = cars;
    }

    private double cars;

    public String getStreet_a() {
        return street_a;
    }

    public void setStreet_a(String street_a) {
        this.street_a = street_a;
    }

    private String street_a;

    public String getStreet_b() {
        return street_b;
    }

    public void setStreet_b(String street_b) {
        this.street_b = street_b;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Intersection that = (Intersection) o;

        if (id != that.id) return false;
        if (!street_b.equals(that.street_b)) return false;
        return street_a.equals(that.street_a);

    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + street_b.hashCode();
        result = 31 * result + street_a.hashCode();
        return result;
    }
}
