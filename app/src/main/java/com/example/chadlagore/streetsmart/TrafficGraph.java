package com.example.chadlagore.streetsmart;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.IntentService;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.BarGraphSeries;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by devin on 2017-03-13.
 */

public class TrafficGraph extends DialogFragment {

    final private static long updateGraphDelay = 0;
    final private static long updateGraphInterval = 1000;

    /* Class debuggig tag. */
    static final private String TAG = "traffic_graph";

    static private Long intersection_id;
    static private Intersection intersection;
    static private StreetSmartClient client;
    private static TimerTask graphUpdateTimer = null;
    private static BarGraphSeries<DataPoint> series;
    private static int x;

    public TrafficGraph() {
        // Empty constructor --use newInstance defined below
    }

    public static TrafficGraph newInstance(Intersection intersection_to_graph) {
        intersection = intersection_to_graph;

        /* Build graph. */
        TrafficGraph frag = new TrafficGraph();
        Bundle args = new Bundle();

        /* Set title. */
        args.putString("title", intersection.getIntersectionName());
        frag.setArguments(args);
        client = new StreetSmartClient();

        /* Set update timer. */
//        scheduleMapUpdate();
        x = 0;
        return frag;
    }

    /**
     * Class performs async updates to graph.
     *
     * Runs every <code>updateGraphInterval</code>ms after an initial delay of
     * <code>updateMapDelay</code>. The StreetSmartAPI is queried for new data, this task occurs
     * asynchronously. This function blocks until it finishes and updates the graph,
     * which is OK because async. TODO: condition variable?
     */
    public static class UpdateGraphTask extends AsyncTask<Intersection, BarGraphSeries<DataPoint>, Void> {

        @Override
        protected Void doInBackground(Intersection... params) {
            JSONArray newIntersectionData = null;

            Log.i(TAG, "updating graph data.");
            Long hash = client.requestIntersection(intersection.getIntersectionID());

            /* We can block because we're async */
            while ((newIntersectionData = client.request(hash)) == null) { /* spin */ }

            try {
                JSONObject obj = newIntersectionData.getJSONObject(0);
                series.appendData(new DataPoint(getNewXVAlue(), obj.getDouble("cars")), true, 4);
            } catch (JSONException e) {
                Log.i(TAG, "recived data, but failed to get object.");
                e.printStackTrace();
            }
            Log.i(TAG, newIntersectionData.toString());

            return null;
        }
    }

    private static Integer getNewXVAlue() {
        x += 1;
        return x;
    }

    /*
     * Schedules a map update timer to run on an asyncronous task.
     * Called once when the fragent is set up for a new intersection.
     * The timer is cancelled in OnDismiss.
     */
    private static void scheduleMapUpdate() {
        final Handler handler = new Handler();
        Timer timer = new Timer();

        /* Set up task. */
        graphUpdateTimer = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        try {
                            UpdateGraphTask performGraphUpdate = new UpdateGraphTask();
                            performGraphUpdate.execute();
                        } catch (Exception e) {
                            Log.i(TAG, "task failed");
                            e.printStackTrace();
                        }
                    }
                });
            }
        };

        /* Schedule task. */
        Log.i(TAG, "scheduling");
        timer.schedule(graphUpdateTimer, updateGraphDelay, updateGraphInterval);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.dialog_layout, container);

        TextView tv = (TextView) rootView.findViewById(R.id.traffic_graph_text_id);
        tv.setText(this.intersection.getIntersectionName());

        GraphView gv = (GraphView) rootView.findViewById(R.id.traffic_graph_plot);

        series = new BarGraphSeries<DataPoint>();
        gv.addSeries(series);
        gv.getViewport().setXAxisBoundsManual(true);
        gv.getViewport().setMinX(0);
        gv.getViewport().setMaxX(10);

        gv.getViewport().setYAxisBoundsManual(true);
        gv.getViewport().setMinY(0);
        gv.getViewport().setMaxY(12);

//        series.resetData(generateData());

        return rootView;
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

//        // Get textview from the view and set interection name for display
//        TextView tv = (TextView) view.findViewById(R.id.traffic_graph_text_id);
//        tv.setText(this.intersection.getIntersectionName());
//
//        // Create new graph view and add populate with data points for
//        // the intersection in question.
//        GraphView gv = (GraphView) view.findViewById(R.id.traffic_graph_plot);
//        BarGraphSeries<DataPoint> series = new BarGraphSeries<DataPoint>(new DataPoint[] {
//            new DataPoint(0,1),
//            new DataPoint(1,5),
//            new DataPoint(2,3),
//            new DataPoint(3,2),
//            new DataPoint(4,6)
//        });
//        // Add the graph to the TrafficGraph dialog
//        gv.addSeries(series);
    }

    @Override
    public void onResume() {
        super.onResume();
        scheduleMapUpdate();
    }

    private DataPoint[] generateData() {
        int count = 1;
        DataPoint[] values = new DataPoint[0];
        for (int i=0; i<count; i++) {
            double x = i;
            double f = 5;
            double y = 3;
            DataPoint v = new DataPoint(x, y);
            values[i] = v;
        }
        return values;
    }

    @Override
    public void onDismiss(DialogInterface dialogInterface) {
        Log.i(TAG, "cancelling");
        graphUpdateTimer.cancel();
    }

}
