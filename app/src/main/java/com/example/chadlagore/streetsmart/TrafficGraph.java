package com.example.chadlagore.streetsmart;

import android.app.DialogFragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

/**
 * Created by devin on 2017-03-13.
 */

public class TrafficGraph extends DialogFragment {

    private String intersectionName;


    public TrafficGraph() {
        // Empty constructor --use newInstance defined below
    }

    public static TrafficGraph newInstance(String intersection_name) {
        TrafficGraph frag = new TrafficGraph();
        Bundle args = new Bundle();
        args.putString("title", intersection_name);
        frag.intersectionName = intersection_name;
        frag.setArguments(args);
        return frag;
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
        tv.setText(this.intersectionName);

        // Create new graph view and add populate with data points for
        // the intersection in question.
        GraphView gv = (GraphView) view.findViewById(R.id.traffic_graph_plot);
        LineGraphSeries<DataPoint> series = new LineGraphSeries<DataPoint>(new DataPoint[] {
            new DataPoint(0,1),
            new DataPoint(1,5),
            new DataPoint(2,3),
            new DataPoint(3,2),
            new DataPoint(4,6)
        });
        // Add the graph to the TrafficGraph dialog
        gv.addSeries(series);
    }

}
