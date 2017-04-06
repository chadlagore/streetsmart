package com.example.chadlagore.streetsmart;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.AsyncTask;
import android.support.v4.view.LayoutInflaterFactory;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.ContactsContract;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import android.view.View;
import android.view.View.OnClickListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import android.widget.DatePicker;
import android.widget.FrameLayout;
import android.widget.TabHost;
import android.widget.Toast;

import static android.support.v4.content.FileProvider.getUriForFile;

public class HistoricalDataActivity extends AppCompatActivity {

    private final String TAG = "historical_data_activity";

    /* TabHost and members */
    private TabHost tabHost = null;
    private TabHost.TabSpec hourlyTab;
    private TabHost.TabSpec dailyTab;
    private TabHost.TabSpec weeklyTab;
    private TabHost.TabSpec monthlyTab;
    private TabHost.TabSpec yearlyTab;

    private final String csv_file = "street_smart_historical.csv";
    private File cache_dir;
    private Set<DataPoint> cached_result;

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

    /**
     * Set content view, add toolbar.
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historical_data);

        /* Generate toolbar at top of activity. */
        Toolbar appToolbar = (Toolbar) findViewById(R.id.historical_toolbar);
        setSupportActionBar(appToolbar);
        ActionBar actionBar = getSupportActionBar();

        /* Add back button for ancestral navigation. */
        actionBar.setDisplayHomeAsUpEnabled(true);

        /* TODO: delete this mock request. */
        HistoricalRequest request = new HistoricalRequest(
                1490200000, 1490831240, "hourly", 250);
        request.execute();

    }

    // Menu icons are inflated just as they were with actionbar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.historical_menu, menu);
        /* Now we create the view for the historical data */
        this.tabHost = (TabHost) findViewById(R.id.tabHost);
        tabHost.setup();

        /* Set up all the tabs */
        TabHost.TabSpec spec = tabHost.newTabSpec("Hourly");
        spec.setContent(R.id.Hourly);
        spec.setIndicator("Hourly");
        this.hourlyTab = spec;
        tabHost.addTab(spec);

        spec = tabHost.newTabSpec("Daily");
        spec.setContent(R.id.Daily);
        spec.setIndicator("Daily");
        this.dailyTab = spec;
        tabHost.addTab(spec);

        spec = tabHost.newTabSpec("Weekly");
        spec.setContent(R.id.Weekly);
        spec.setIndicator("Weekly");
        this.weeklyTab = spec;
        tabHost.addTab(spec);

        spec = tabHost.newTabSpec("Monthly");
        spec.setContent(R.id.Weekly);
        spec.setIndicator("Monthly");
        this.monthlyTab = spec;
        tabHost.addTab(spec);

        spec = tabHost.newTabSpec("Yearly");
        spec.setContent(R.id.Weekly);
        spec.setIndicator("Yearly");
        this.yearlyTab = spec;
        tabHost.addTab(spec);

        /* We'll also need to add a listener to detect when tabs change */
        tabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {

                /* For each tab, we'll need to retreive different
                information from the server. Handle each case seperately */
                if(hourlyTab.getTag().equals(tabId)) {
                    onHourlyClick();
                } else if (dailyTab.getTag().equals(tabId)) {
                    onDailyClick();
                } else if (weeklyTab.getTag().equals(tabId)) {
                    onWeeklyClick();
                } else if (monthlyTab.getTag().equals(tabId)) {
                    onMonthlyClick();
                } else if (yearlyTab.getTag().equals(tabId)) {
                    onYearlyClick();

                /* Finally, if the previous cases were exhausted, we have a
                problem, print the tabId and gracefully exit */
                } else {
                    System.out.println("The selected tab was not found. The id is: " + tabId);
                    Exception e = new Exception();
                    e.printStackTrace();
                    System.exit(0);
                }

            }
        });

//        HistoricalRequest request = new HistoricalRequest(
//                1490800000, 1490831240, "hourly", 250);
//        request.execute();
//        return true;

        return true;
    }

    public static class DatePickerFragment extends DialogFragment implements
            DatePickerDialog.OnDateSetListener {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            return new DatePickerDialog(getActivity(), this, year, month, day);
        }

        public void onDateSet(DatePicker dp, int year, int month, int day) {

        }
    }

    public void OnEndDayClick() {

    }

    public void onStartDayClick() {

    }

    /**
     * Handle hourly click
     */
    private void onHourlyClick() {

        /* Make graphs invisible */
        makeGraphsInvisible();

        /* Now make the relevant graph visible */
        final LayoutInflater factory = getLayoutInflater();
        final View v = factory.inflate(R.layout.activity_historical_data, null);
        View graph = (GraphView) v.findViewById(R.id.hourly_graph);
        graph.setVisibility(View.VISIBLE);

        //HistoricalRequest request = new HistoricalRequest();
        //request.execute();
    }

    /**
     * Method simply sets each graph view in the historical data
     * layout to invisible. This is necessary because the views will
     * otherwise overlap.
     */
    private void makeGraphsInvisible() {
        final LayoutInflater inf = getLayoutInflater();
        final View view = inf.inflate(R.layout.activity_historical_data, null);

        GraphView gv = (GraphView) view.findViewById(R.id.hourly_graph);
        gv.setVisibility(View.INVISIBLE);

        gv = (GraphView) view.findViewById(R.id.daily_graph);
        gv.setVisibility(View.INVISIBLE);

        gv = (GraphView) view.findViewById(R.id.weekly_graph);
        gv.setVisibility(View.INVISIBLE);

        gv = (GraphView) view.findViewById(R.id.monthly_graph);
        gv.setVisibility(View.INVISIBLE);

        gv = (GraphView) view.findViewById(R.id.yearly_graph);
        gv.setVisibility(View.INVISIBLE);
    }

    /**
     * Handle daily click.
     */
    private void onDailyClick() {
        // HistoricalRequest request = new HistoricalRequest( ... );
        // request.execute(); <--- results in a call to addDataPointsToChart and several setProgressPercent calls.
    }

    /**
     * Handle weekly click.
     */
    private void onWeeklyClick() {
        // HistoricalRequest request = new HistoricalRequest( ... );
        // request.execute(); <--- results in a call to addDataPointsToChart and several setProgressPercent calls.
    }

    /**
     * Handle monthly click.
     */
    private void onMonthlyClick() {
        // HistoricalRequest request = new HistoricalRequest( ... );
        // request.execute(); <--- results in a call to addDataPointsToChart and several setProgressPercent calls.
    }

    /**
     * Handle yearly click.
     */
    private void onYearlyClick() {
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
        //tabHost.getCurrentTabTag()
        Log.i(TAG, result.toString());
        this.cached_result = result;
    }


    /**
     * Handles click of Export button in HistoricalDataActivity.
     * Starts by writing the cached_result to disk as csv, then
     * attempts to send an email (with the cached csv as an attachment).
     * @param item
     */
    public boolean onOptionsItemSelected(MenuItem item) {

        /* Find out which button on toolbar pushed (only one for now). */
        switch (item.getItemId()) {
            case R.id.export_button:

                /* Back out if running on emulator. */
                if (Build.FINGERPRINT.startsWith("generic")) {
                    Toast.makeText(this, "Export feature not supported on emulator.",
                            Toast.LENGTH_SHORT).show();
                    return true;
                }

                /* Convert to list and try to send email. */
                List<DataPoint> to_csv = new ArrayList<DataPoint>(this.cached_result);
                if (export(to_csv)) {
                    sendUserEmail("example@gmail.com" /* TODO: read in user email in GUI. */);
                }
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Generates a CSV file from a List of DataPoints. Saves the file to disk.
     * We only use one CSV file on the device at a time to save the space.
     * @param data a List of DataPoints != null.
     * @return false if could not save to file.
     */
    private boolean export(List<DataPoint> data) {

        /* Sort data by timestamp. */
        Collections.sort(data, new Comparator<DataPoint>() {

            @Override
            public int compare(DataPoint dp1, DataPoint dp2) {
                return (int)(dp1.getX() - dp2.getX());
            }
        });

        /* Build up CSV. */
        StringBuilder sb = new StringBuilder();
        sb.append("timestamp,datetime,cars\n");

        for (DataPoint d : data) {

            /* Convert timestamp. */
            long itemLong = (long) (d.getX() * 1000);
            Date itemDate = new Date(itemLong);
            String itemDateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SS").format(itemDate);

            /* Append a row. */
            sb.append(String.valueOf(d.getX()));
            sb.append(",");
            sb.append(itemDateStr);
            sb.append(",");
            sb.append(String.valueOf(d.getY()));
            sb.append("\n");
        }

        return writeCsvToDisk(sb.toString());
    }

    /**
     * Writes the result to csv file. Returns false if fails.
     * @param result the string csv file.
     * @return false if fails.
     */
    private boolean writeCsvToDisk(String result) {
        FileOutputStream outputStream;

        try {
            outputStream = openFileOutput(this.csv_file, Context.MODE_PRIVATE);
            outputStream.write(result.getBytes());
            outputStream.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Sends the user an email with the attached csv.
     * @param email a user Email.
     * @return false if fails.
     */
    private boolean sendUserEmail(String email) {

        /* Set up email intent. */
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("text/plain");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[] {email});
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.hist_email_subject));
        emailIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.hist_email_body));

        /* Go collect cached file. */
        String pathToCsv = getFilesDir().toString() + "/" + csv_file;
        File file = new File(pathToCsv);

        /* Fail if file does not exist. */
        if (!file.exists()) {
            return false;
        } else if (!file.canRead()) {
            return false;
        }

        /* Stream file into email intent. */
        Uri contentUri = getUriForFile(this, "com.chadlagore.fileprovider", file);
        emailIntent.putExtra(Intent.EXTRA_STREAM, contentUri);

        /* Try to send the email. */
        try {
            startActivity(Intent.createChooser(emailIntent, "Send mail..."));
            return true;
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "There are no email clients installed.",
                    Toast.LENGTH_SHORT).show();
            return false;
        }
    }
}
