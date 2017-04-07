package com.example.chadlagore.streetsmart;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.graphics.Color;
import android.os.AsyncTask;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.SimpleTimeZone;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TabHost;
import android.widget.Toast;

import static android.support.v4.content.FileProvider.getUriForFile;
import static okhttp3.internal.http.HttpDate.format;

public class HistoricalDataActivity extends AppCompatActivity {

    protected final String TAG = "historical_data_activity";

    /* Graph objects */
    private boolean makingRequest = false;
    private String lastTabID;
    private static LineData hourlyData;
    private static LineData dailyData;
    private static LineData weeklyData;
    private static LineData monthlyData;
    private static LineData yearlyData;
    private static LineData currentDataset;
    private LineChart historicalChart;
    private final String csv_file = "street_smart_historical.csv";
    private File cache_dir;
    private List<Entry> cachedResult;
    private int intersectionID = 1;

    /* Tab objects */
    private TabHost tabHost = null;
    private TabHost.TabSpec hourlyTab;
    private TabHost.TabSpec dailyTab;
    private TabHost.TabSpec weeklyTab;
    private TabHost.TabSpec monthlyTab;
    private TabHost.TabSpec yearlyTab;

    /* Start and end dates */
    private static DateFormat dateFormat;
    protected Date endDate;
    protected Date startDate;


    /**
     * Set content view, add toolbar.
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historical_data);

//        intersectionID = getIntent().getParcelableExtra("intersectionID");

        /* Generate toolbar at top of activity. */
        Toolbar appToolbar = (Toolbar) findViewById(R.id.historical_toolbar);
        setSupportActionBar(appToolbar);
        ActionBar actionBar = getSupportActionBar();

        /* Add back button for ancestral navigation. */
        actionBar.setDisplayHomeAsUpEnabled(true);

         /* Create onClick listeners for the date selection buttons */
        Button startDateButton = (Button) findViewById(R.id.start_date_button);
        startDateButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                Log.i("Historical Activity", "The onClickListener for the start date button" +
                        "was triggered");
                onStartDayClick(v);
            }
        });

        /* And now the listener for the end date button */
        Button endDateButton = (Button) findViewById(R.id.end_date_button);
        endDateButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                Log.i("Historical Activity", "The onClickListener for the end date button was" +
                        "triggered");
                onEndDayClick(v);
            }
        });

        /* Set up historical data plot */
        historicalChart = (LineChart) findViewById(R.id.historical_chart);

        List<Entry> dummyEntries = new ArrayList<Entry>();
        dummyEntries.add(new Entry(0, 0));
        LineDataSet dataSet = new LineDataSet(dummyEntries, "Historical Data");
        dataSet.setValueTextColor(Color.WHITE);
        hourlyData = new LineData(dataSet);
        dailyData = new LineData(dataSet);
        weeklyData = new LineData(dataSet);
        monthlyData = new LineData(dataSet);
        yearlyData = new LineData(dataSet);
        hourlyData = new LineData(dataSet);
        currentDataset = hourlyData;
        historicalChart.setData(currentDataset);
        historicalChart.notifyDataSetChanged();
        historicalChart.invalidate();

        dateFormat = new SimpleDateFormat();
        dateFormat.setTimeZone(new SimpleTimeZone(SimpleTimeZone.UTC_TIME, "UTC"));
    }

    /**
     * A class for requesting and storing historical data from the StreetSmart API.
     * Requests execute asyncronously, but can update the historical graph by calling
     * addDataPointsToChart from onPostExecute.
     */
    public class HistoricalRequest {

        private final String base_url = "tranquil-shore-92989.herokuapp.com";
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
        public class AddDataPointsToSet extends AsyncTask<JSONObject, Integer, List<Entry>> {

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
            protected List<Entry> doInBackground(JSONObject... jsonObjs) {

                /* List for results. */
                List<Entry> results = new ArrayList<Entry>();

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
                        results.add(new Entry((float)x, (float)y));
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

                /* Sort data by timestamp. */
                Collections.sort(results, new Comparator<Entry>() {

                    @Override
                    public int compare(Entry dp1, Entry dp2) {
                        return (int)(dp1.getX() - dp2.getX());
                    }
                });

                cachedResult = results;

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
            protected void onPostExecute(List<Entry> result) {
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


    // Menu icons are inflated just as they were with actionbar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, "Inflating toolbar");
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
                Log.d(TAG, "Menu item clicked: " + tabId.toString());

                if (tabId == lastTabID || makingRequest) return;

                String granularity = null;

                /*
                 * For each tab, we'll need to retreive different
                 * information from the server. Handle each case separately
                 */
                if (hourlyTab.getTag().equals(tabId)) {
                    currentDataset = hourlyData;
                    granularity = "hourly";
                } else if (dailyTab.getTag().equals(tabId)) {
                    currentDataset = dailyData;
                    granularity = "daily";
                } else if (weeklyTab.getTag().equals(tabId)) {
                    currentDataset = weeklyData;
                    granularity = "weekly";
                } else if (monthlyTab.getTag().equals(tabId)) {
                    currentDataset = monthlyData;
                    granularity = "monthly";
                } else if (yearlyTab.getTag().equals(tabId)) {
                    currentDataset = yearlyData;
                    granularity = "yearly";
                }

                lastTabID = tabId;
                Log.d(TAG, "Showing " + granularity+ " data.");

                if (granularity != null) {
                    HistoricalRequest request =
                            new HistoricalRequest((int)startDate.getTime(), (int)endDate.getTime(),
                                    granularity, intersectionID);
                    makingRequest = true;
                    request.execute();
                }
            }
        });

        return true;
    }


    /**
     * Called when the user click something in the toolbar
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getTitle() == "Export") {
            /* Back out if running on emulator. */
            if (Build.FINGERPRINT.startsWith("generic")) {
                Toast.makeText(this, "Export feature not supported on emulator.",
                        Toast.LENGTH_SHORT).show();
                return true;
            }

                /* Convert to list and try to send email. */
            List<Entry> to_csv = new ArrayList<Entry>(this.cachedResult);
            if (export(to_csv)) {
                sendUserEmail("example@gmail.com" /* TODO: read in user email in GUI. */);
            }
        }

        return true;
    }

    /**
     * Sub-class which defines the DatePicker dialog fragment which presents
     * to the user the view to select start and end dates for the historical
     * period they wish to view.
     */
    public static class DatePickerFragment extends DialogFragment implements
            DatePickerDialog.OnDateSetListener {

        /* Reference to the date object of the superclass which
        this object corresponds to */
        public HistoricalDataActivity hda;

        /* Assign to one of the two options below */
        public Integer id;

        /* Start day/ end day */
        private Integer START_DAY = 1;
        private Integer END_DAY = 2;

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            return new DatePickerDialog(getActivity(), this, year, month, day);
        }

        /**
         * Function is a callback, called when the user enters a new start date
         * or a new end date. Parameters are filled automatically. Logic is that
         * start date and end dates are handled separately, having their info
         * stored in members of this class.
         *
         * The Integer 'id' identifies this object as either being a start date
         * or an end date. Be sure to assign it an appropriate value when you
         * create it.
         *
         * @param dp, the date picker
         *
         * @param year, an integer representing the year
         *
         * @param month, integer representing the year
         *
         * @param day, integer representing the day of the month
         */
        public void onDateSet(DatePicker dp, int year, int month, int day) {
            Log.d(this.hda.TAG, "Setting date");

            /* Invalidate cached datasets */
            hourlyData = null;
            dailyData = null;
            weeklyData = null;
            monthlyData = null;
            yearlyData = null;

            /* Handle the end date */
            if (this.id.equals(this.END_DAY)) {
                try {
                    String date = dateFormat.format(getDateFromDatePicker(dp));
                    this.hda.endDate = dateFormat.parse(date);
                    Log.d(this.hda.TAG, "setting end date to " + this.hda.toString());
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }

            /* Handle the start date */
            if (this.id.equals(this.START_DAY)) {
                Log.d(this.hda.TAG, "User selected start day");
                try {
                    String date = dateFormat.format(getDateFromDatePicker(dp));
                    this.hda.startDate = dateFormat.parse(date);
                    Log.d(this.hda.TAG, "setting start date to " + this.hda.toString());
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * Method returns a Date object from a datePicker object.
         *
         * @param datePicker, the datePicker returned from the callback
         *
         * @return, the date object
         */
        public static java.util.Date getDateFromDatePicker(DatePicker datePicker){
            int day = datePicker.getDayOfMonth();
            int month = datePicker.getMonth();
            int year =  datePicker.getYear();

            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, day);

            return calendar.getTime();
        }
    }

    /**
     * Method simply shows the date picker dialog fragment
     * for the end date of the period.
     *
     * @param view, the view which collects information about the
     *              date from the user
     */
    public void onEndDayClick(View view) {
        DialogFragment df = new DatePickerFragment();

        /* Set the id so we can identify the date later */
        DatePickerFragment dpf = (DatePickerFragment) df;
        dpf.id = dpf.END_DAY;

        /* Hack but it will work */
        dpf.hda = this;

        /* Show the dialog */
        df.show(getFragmentManager(), "endDatePicker");

        Log.i(this.TAG, "The dialog for entering the end date was opened");
    }

    /**
     * Method displays the dialog fragment for the user to enter
     * the start date of the period over which to view historical
     * data for the intersection in question.
     *
     * @param view, the view which collects information from the user
     */
    public void onStartDayClick(View view) {
        DialogFragment df = new DatePickerFragment();

        /* Set the id so we can identify it later */
        DatePickerFragment dpf = (DatePickerFragment) df;
        dpf.id = dpf.START_DAY;

        /* Hack but it will work */
        dpf.hda = this;

        /* Show the dialog */
        df.show(getFragmentManager(), "startDatePicker");

        Log.i(this.TAG, "The dialog for entering the start date was opened");
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
    private void addDataPointsToChart(List<Entry> result, double max_x, double max_y,
                                      double min_x, double min_y) {
        /* Add datapoints to chart */
        Log.i(TAG, "Adding data to chart: " + result.toString());

        LineDataSet newSet = new LineDataSet(result, "Historical Data");
        currentDataset = new LineData(newSet);
        historicalChart.setData(currentDataset);
        historicalChart.notifyDataSetChanged();
        historicalChart.invalidate();

        /* Add datapoints to chart, adjust axes etc. */
        cachedResult = result;
        makingRequest = false;
    }

    /**
     * Generates a CSV file from a List of DataPoints. Saves the file to disk.
     * We only use one CSV file on the device at a time to save the space.
     * @param data a List of DataPoints != null.
     * @return false if could not save to file.
     */
    private boolean export(List<Entry> data) {

        /* Sort data by timestamp. */
        Collections.sort(data, new Comparator<Entry>() {

            @Override
            public int compare(Entry dp1, Entry dp2) {
                return (int)(dp1.getX() - dp2.getX());
            }
        });

        /* Build up CSV. */
        StringBuilder sb = new StringBuilder();
        sb.append("timestamp,datetime,cars\n");

        for (Entry d : data) {

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
