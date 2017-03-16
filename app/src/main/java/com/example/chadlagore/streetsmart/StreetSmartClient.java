package com.example.chadlagore.streetsmart;

import com.google.android.gms.maps.model.LatLngBounds;

import org.json.*;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by chadlagore on 2017-03-14.
 *
 * A simple client for accessing the StreetSmartAPI.
 */

public class StreetSmartClient {

    /* The heroku host. */
    private final String base_url;

    /* The log tag */
    private final String intersection_tag = "intersection_resp";

    /* A JSON Array to hold responses from the server. */
    public JSONArray responseJSON;

    /* A hashmap to connect intersections to ids. */
    private HashMap<Long, Intersection> intersections;

    OkHttpClient client;

    /**
     * Initialize the base_url and endpoints.
     */
    public StreetSmartClient() {
        URL base, intersection;
        client = new OkHttpClient();
        base_url = "tranquil-shore-92989.herokuapp.com";
        responseJSON = null;
        intersections = new HashMap<Long, Intersection>();
    }

    /**
     * Collects intersections within bounds specified by <code>bounds</code>.
     * May be called from the Main Activity, but callers must check whether the result is null
     * before using.
     *
     * @param bounds
     * @return a JSONArray of intersections.
     */
    public HashMap<Long, Intersection> getIntersections(LatLngBounds bounds) {

        HttpUrl.Builder builder= new HttpUrl.Builder()
                .scheme("http")
                .host(base_url)
                .addPathSegment("traffic")
                .addPathSegment("intersections");

        /* Only add bounds if bounds passed. */
        if (bounds != null) {
            builder.addQueryParameter("lat_lte", String.valueOf(bounds.northeast.latitude))
                    .addQueryParameter("lat_gte", String.valueOf(bounds.southwest.latitude))
                    .addQueryParameter("lon_lte", String.valueOf(bounds.southwest.longitude))
                    .addQueryParameter("lon_gte", String.valueOf(bounds.northeast.longitude));
        }

        /* Build the url. */
        HttpUrl url = builder.build();

        /* Build a new request. */
        Request request = new Request.Builder()
                .url(url)
                .build();

        /* Queue the request, handle failure and response async. */
        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                } else {
                    try {
                        responseJSON = new JSONArray(response.body().string());
                        buildIntersections(responseJSON);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        return this.intersections;
    }

    /**
     * Given a JSONArray, builds HashMap of intersections up.
     * @param jArray != null
     *               A JSONArray containing intersections.
     */
    private void buildIntersections(JSONArray jArray) {
        if (jArray != null) {
            for (int i = 0; i < responseJSON.length(); i++) {
                Long id = null;
                JSONObject intersectionJSON = null;

                try {
                    intersectionJSON = jArray.getJSONObject(i);
                    id = intersectionJSON.getLong("id");
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                intersections.put(id, new Intersection(intersectionJSON));
            }
        }
    }
}