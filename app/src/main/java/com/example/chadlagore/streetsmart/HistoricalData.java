package com.example.chadlagore.streetsmart;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.BarGraphSeries;
import com.jjoe64.graphview.series.DataPoint;

public class HistoricalData extends AppCompatActivity {

    private static BarGraphSeries<DataPoint> mon;
    private static BarGraphSeries<DataPoint> tues;
    private static BarGraphSeries<DataPoint> weds;
    private static BarGraphSeries<DataPoint> thurs;
    private static BarGraphSeries<DataPoint> fri;
    private static BarGraphSeries<DataPoint> sat;
    private static BarGraphSeries<DataPoint> sun;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historical_data);

        StreetSmartClient smc = new StreetSmartClient();
    }
}
