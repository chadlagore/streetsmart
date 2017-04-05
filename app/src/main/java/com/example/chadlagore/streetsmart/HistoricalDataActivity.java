package com.example.chadlagore.streetsmart;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.jjoe64.graphview.series.DataPoint;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class HistoricalDataActivity extends AppCompatActivity {

    private final String TAG = "historical_data_activity";

    /**
     * A class for requesting and storing historical data from the StreetSmart API.
     * Requests execute asyncronously, but can update the historical graph by calling
     * addDataPointsToChart from onPostExecute.
     */
    public class HistoricalRequest {

        private final String base_url = "tranquil-shore-92989.herokuapp.com";;
        private final String TAG = "historical_request";
        private int start_date;
        private int end_date;
        private long id;
        private String granularity;
        public JSONObject meta, data;
        OkHttpClient client;

        /**
         * Create a new historical request, parameters correspond to API parameters.
         *
         * @param start_date an integer representing the POSIXct timestamp of the start_date.
         * @param end_date an integer representing the POSIXct timestamp of the end_date.
         * @param granularity either "hourly", "daily", "weekly", "monthly", or "yearly"
         * @param id the intersection id
         */
        public HistoricalRequest(int start_date, int end_date, String granularity, long id) {
            this.start_date = start_date;
            this.end_date = end_date;
            this.granularity = granularity;
            this.id = id;
            this.client = new OkHttpClient();
        }

        /**
         * Queues a request for the query specified in the construction of the HistoricalRequest.
         * Generates a callback to catch the response. If the response was a success, queues
         * an asynchronous task to update the DataPoints in the plot.
         */
        public void execute() {
            HttpUrl.Builder builder= new HttpUrl.Builder()
                    .scheme("http")
                    .host(base_url)
                    .addPathSegment("traffic")
                    .addPathSegment("historical");

            /* Add API paramters. */
            builder.addQueryParameter("id", String.valueOf(this.id))
                    .addQueryParameter("start_date", String.valueOf(this.start_date))
                    .addQueryParameter("end_date", String.valueOf(this.end_date))
                    .addQueryParameter("granularity", String.valueOf(this.granularity));

            /* Build the url. */
            HttpUrl url = builder.build();

            /* Build a new request. */
            Request request = new Request.Builder().url(url).build();

            /**
             * Enqueue the request and handle responses asynchronously.
             * If the request is a failure, print stack trace.
             * If the request is a success, check the response code.
             * If the response code signifies a 400 or 500, throw.
             * On successful response, trigger AsyncTask for datapoint update which
             * leads to a chart update on the main thread.
             */
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

                            /* Collect results meta and data payloads. */
                            JSONObject result = new JSONObject(response.body().string());
                            meta = result.getJSONObject("meta");
                            data = result.getJSONObject("data");

                            /* Update datapoints with AsyncTask. */
                            new AddDataPointsToSet().execute(data);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });

        }

        /**
         * AsyncTask to add new DataPoints from API into DataPoint Set.
         * Updates progress in terms of the number of data points it has yet to update.
         */
        public class AddDataPointsToSet extends AsyncTask<JSONObject, Integer, Set<DataPoint>> {

            /*
             * Calculate these in `doInBackground`, then pass them to addDataPointsToChart
             * to reduce calculations done in the main thread.
             */
            double max_x, max_y, min_x, min_y;

            /**
             * Adds (x,y) coords into a Set of DataPoints.
             * @param jsonObjs the `data` object from the request.
             * @return A Set of DataPoints for plotting.
             */
            @Override
            protected Set<DataPoint> doInBackground(JSONObject... jsonObjs) {

                /* List for results. */
                Set<DataPoint> results = new HashSet<DataPoint>();

                /* Only one object is actually passed (data). */
                JSONObject data = jsonObjs[0];
                Iterator keys = data.keys();

                int i = 0; /* Publish progress too. */
                while (keys.hasNext()) {
                    String key = (String) keys.next();

                    /* Try to collect each datapoint from the JSON. */
                    try {
                        double x = Double.valueOf(key);
                        double y = Double.valueOf((Double) data.get(key));
                        results.add(new DataPoint(x,y));
                        updateMax(x, y);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    /* Update progress by counting number of keys. */
                    try {
                        publishProgress((int) ((i / (float) meta.getDouble("results")) * 100));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    i++;
                }

                return results;
            }

            /**
             * Initializes maxes and mins.
             */
            @Override
            protected void onPreExecute() {
                max_x = 0;
                max_y = 0;
                min_x = Integer.MAX_VALUE;
                min_y = Integer.MAX_VALUE;
            }

            /**
             * Updates data point aggregation progress.
             * @param progress an integer percent of the progress complete.
             */
            @Override
            protected void onProgressUpdate(Integer... progress) {
                setProgressPercent(progress[0]);
            }

            /**
             * Called once the aggregation of DataPoints is complete.
             * @param result
             */
            @Override
            protected void onPostExecute(Set<DataPoint> result) {
                addDataPointsToChart(result, max_x, max_y, min_x, min_y);
            }

            /**
             * Updates max and min values during with recent values.
             * @param x the most recent x value seen.
             * @param y the most recent y value seen.
             */
            private void updateMax(double x, double y) {
                if (x > max_x) {
                    max_x = x;
                } else if (x < min_x) {
                    min_x = x;
                }

                if (y > max_y) {
                    max_y = y;
                } else if (y < min_y) {
                    min_y = y;
                }
            }

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historical_data);
        HistoricalRequest request = new HistoricalRequest(
                1490800000, 1490831240, "hourly", 250);
        request.execute();
    }

    /**
     * Handle hourly click.
     * @param view
     */
    private void onHourlyClick(View view) {
        // HistoricalRequest request = new HistoricalRequest( ... );
        // request.execute(); <--- results in a call to addDataPointsToChart and several setProgressPercent calls.
    }

    /**
     * Handle daily click.
     * @param view
     */
    private void onDailyClick(View view) {
        // HistoricalRequest request = new HistoricalRequest( ... );
        // request.execute(); <--- results in a call to addDataPointsToChart and several setProgressPercent calls.
    }

    /**
     * Handle weekly click.
     * @param view
     */
    private void onWeeklyClick(View view) {
        // HistoricalRequest request = new HistoricalRequest( ... );
        // request.execute(); <--- results in a call to addDataPointsToChart and several setProgressPercent calls.
    }

    /**
     * Handle monthly click.
     * @param view
     */
    private void onMonthlyClick(View view) {
        // HistoricalRequest request = new HistoricalRequest( ... );
        // request.execute(); <--- results in a call to addDataPointsToChart and several setProgressPercent calls.
    }

    /**
     * Handle yearly click.
     * @param view
     */
    private void onYearlyClick(View view) {
        // HistoricalRequest request = new HistoricalRequest( ... );
        // request.execute(); <--- results in a call to addDataPointsToChart and several setProgressPercent calls.
    }

    /**
     * Update a progress bar for integer changes in percent of data loaded.
     * @param progressPercent
     */
    public void setProgressPercent(Integer progressPercent) {
        /* Update progress bar. */
    }

    /**
     * Adding a set of DataPoints to a chart. Adjusts axes to fit new data.
     * @param result a Set of DataPoints (x = POSIXct timetamp, y = number of cars).
     * @param max_x maximum timestamp in Set.
     * @param max_y maximum cars seen in Set.
     * @param min_x minimum timestamp in Set.
     * @param min_y minimum cars seen in Set.
     */
    private void addDataPointsToChart(Set<DataPoint> result, double max_x, double max_y,
                                      double min_x, double min_y) {
        /* Add datapoints to chart, adjust axes etc. */
        Log.i(TAG, result.toString());
    }
}
