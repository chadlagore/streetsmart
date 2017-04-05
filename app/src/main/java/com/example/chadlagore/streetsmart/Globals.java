package com.example.chadlagore.streetsmart;

import android.support.annotation.Nullable;

/**
 * Created by bfbachmann on 2017-04-03.
 */

public class Globals {
    public static Intersection bluetoothIntersection = null;
    public static final long intersectionID = 999;
    public static double latitude = 0;
    public static double longitude = 0;


    public Globals() {
        /* Do nothing */
    }

    public static void setBluetoothIntersection(String slatitude, String slongitude) {
        try {
            latitude = Double.parseDouble(slatitude);
            longitude = Double.parseDouble(slongitude);
        } catch (NumberFormatException e) {
            /* This is okay. It means the user passed invalid coordinates, so ignore them */
            return;
        }
    }

    @Nullable
    public static Intersection getBluetoothIntersection(MapFragment fragment) {
        if (latitude != 0 && longitude != 0) {
            return new Intersection(latitude, longitude, "Blueooth Device",
                    String.valueOf(latitude) + ", " + String.valueOf(longitude), 999, -1, fragment);
        } else {
            return null;
        }
    }
}
