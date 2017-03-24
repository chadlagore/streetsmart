package com.example.chadlagore.streetsmart;

import android.util.Log;

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

import static android.content.ContentValues.TAG;

/**
 * Created by chadlagore on 2017-03-14.
 *
 * A simple client for accessing the StreetSmartAPI.
 */

public class StreetSmartClient {

    /* The heroku host. */
    private final String base_url;

    /* The log tag */
    private final String TAG = "intersection_resp";

    /* The tag of the next JSON response . */
    private Long nextHash;

    /* A JSON Array to hold responses from the server. */
    public JSONArray responseJSON;

    /* A JSON mapping to hold responses from the server. */
    private HashMap<Long, JSONArray> responses;

    OkHttpClient client;

    /**
     * Initialize client, url, and intersections buffer.
     *
     */
    public StreetSmartClient() {
        URL base, intersection;

        client = new OkHttpClient();
        base_url = "tranquil-shore-92989.herokuapp.com";
        responseJSON = null;
        nextHash = 0L;
        responses = new HashMap<Long, JSONArray>();
    }


    /**
     * Collects intersections within bounds specified by <code>bounds</code>.
     * May be called from the Main Activity, but callers must check whether the result is null
     * before using.
     *
     * @param bounds
     * @return a JSONArray of intersections.
     */
    public void updateIntersections(LatLngBounds bounds) {

        HttpUrl.Builder builder= new HttpUrl.Builder()
                .scheme("http")
                .host(base_url)
                .addPathSegment("traffic")
                .addPathSegment("intersections");

        /* Only add bounds if bounds passed. */
        if (bounds != null) {
            builder.addQueryParameter("lat_lte", String.valueOf(bounds.northeast.latitude))
                    .addQueryParameter("lat_gte", String.valueOf(bounds.southwest.latitude))
                    .addQueryParameter("lon_lte", String.valueOf(bounds.northeast.longitude))
                    .addQueryParameter("lon_gte", String.valueOf(bounds.southwest.longitude));
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
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public JSONArray getLatestIntersections() {
        return this.responseJSON;
    }

    /**
     * Requests a single intersection by ID.
     * Returns a hash to collect its result.
     * @param intersection_id
     */
    public Long requestIntersection(Long intersection_id) {
        HttpUrl url = new HttpUrl.Builder()
                .scheme("http")
                .host(base_url)
                .addPathSegment("traffic")
                .addPathSegment("intersections")
                .addQueryParameter("id", Long.toString(intersection_id))
                .build();

        /* Build a new request. */
        Request request = new Request.Builder()
                .url(url)
                .build();

        final Long hash = getNextHash();

        /* Queue the request, handle failure and response async. */
        try {
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
                            Log.i(TAG, "Logging request with tag " + hash);
                            responses.put(hash, new JSONArray(response.body().string()));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        return hash;
    }

    /**
     * Collects the nextHash and increments the counter.
     * We probably want to change this to a hash function.
     * @return the next hash.
     */
    private Long getNextHash() {
        nextHash += 1;
        return nextHash;
    }

    /**
     * If request corresponding to <code>hash</code> is ready, return it. Otherwise null.
     * @param hash
     * @return the data corresponding to the hash or null if it is not ready.
     */
    public JSONArray request(Long hash) {
        if (responses.containsKey(hash)) {
            JSONArray result = responses.get(hash);
            responses.remove(hash);
            return result;
        } else {
            return null;
        }
    }
}