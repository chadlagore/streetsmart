package com.example.chadlagore.streetsmart;

import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceActivity.*;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresPermission;
import android.util.Log;

import com.google.android.gms.maps.model.LatLngBounds;

import org.json.*;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by chadlagore on 2017-03-14.
 *
 * A simple client for accessing the StreetSmartAPI.
 */

public class StreetSmartClient {

    private final String base_url;
    private final String intersection_endpoint;
    private final String intersection_tag = "intersection_resp";
    public JSONArray responseJSON;

    OkHttpClient client;

    /**
     * Initialize the base_url and endpoints.
     */
    public StreetSmartClient() {
        URL base, intersection;
        client = new OkHttpClient();
        base_url = "http://tranquil-shore-92989.herokuapp.com/";
        intersection_endpoint = "traffic/intersections/";
        responseJSON = null;
    }

    /**
     * Collects intersections within bounds specified by <code>bounds</code>.
     * May be called from the Main Activity, but callers must check whether the result is null
     * before using.
     *
     * @param bounds
     * @return a JSONArray of intersections.
     */
    public JSONArray getIntersection(LatLngBounds bounds) {

        /* Build a new request. */
        Request request = new Request.Builder()
                .url(base_url + intersection_endpoint)
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

        return this.responseJSON;
    }
}