package com.example.chadlagore.streetsmart;

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
import org.json.JSONObject;

import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by devin on 2017-03-13.
 */

public class TrafficGraph extends DialogFragment {

    final private static long updateGraphDelay = 0;
    final private static long updateGraphInterval = 5000;

    /* Class debuggig tag. */
    static final private String TAG = "traffic_graph";

    static private Long intersection_id;
    static private Intersection intersection;
    static private StreetSmartClient client;
    private static TimerTask graphUpdateTimer = null;
    private static boolean running = true;
    Queue<DataPoint> pointsToGraph;

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
        initGraphUpdateTimer();
        return frag;
    }

    /**
     * Initalizes timer (by making the first call).
     */
    private static void initGraphUpdateTimer() {
        Log.i(TAG, "running callMapUpdate");
        callMapUpdate();
    }

    /**
     * Class performs async updates to graph.
     *
     * Runs every <code>updateGraphInterval</code>ms after an initial delay of
     * <code>updateMapDelay</code>. The StreetSmartAPI is queried for new data, this task occurs
     * asynchronously. The graph is updated based on stale data from the previous collection
     * attempt.
     */
    public static class UpdateGraphTask extends AsyncTask<Intersection, Void, Void> {

        @Override
        protected Void doInBackground(Intersection... params) {
            JSONArray newIntersectionData = null;

            Log.i(TAG, "task still running");
            Long hash = client.requestIntersection(intersection.getIntersectionID());

            /* We can block because we're async */
            while ((newIntersectionData = client.request(hash)) == null) { /* spin */ }

            Log.i(TAG, "data up!");

            return null;
        }

        @Override
        protected void onCancelled() {
            Log.i(TAG, "onCancelled runs.");
        }

    }

    /*
     * Actual async map update function. Makes use of the UpdadeGraphTask AsyncTask.
     */
    private static void callMapUpdate() {
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
        return inflater.inflate(R.layout.dialog_layout, container);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get textview from the view and set interection name for display
        TextView tv = (TextView) view.findViewById(R.id.traffic_graph_text_id);
        tv.setText(this.intersection.getIntersectionName());

        // Create new graph view and add populate with data points for
        // the intersection in question.
        GraphView gv = (GraphView) view.findViewById(R.id.traffic_graph_plot);
        BarGraphSeries<DataPoint> series = new BarGraphSeries<DataPoint>(new DataPoint[] {
            new DataPoint(0,1),
            new DataPoint(1,5),
            new DataPoint(2,3),
            new DataPoint(3,2),
            new DataPoint(4,6)
        });
        // Add the graph to the TrafficGraph dialog
        gv.addSeries(series);
    }

    @Override
    public void onDismiss(DialogInterface dialogInterface) {
        Log.i(TAG, "cancelling");
        graphUpdateTimer.cancel();
    }

}
