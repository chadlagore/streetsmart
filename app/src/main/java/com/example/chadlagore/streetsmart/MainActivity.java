package com.example.chadlagore.streetsmart;

import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    SupportMapFragment mapFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onSatelliteClick(View view) {
        FragmentManager getSupportFragmentManager ;
        MapFragment fragment = (MapFragment)getSupportFragmentManager()
                .findFragmentById(R.id.map);
        fragment.changeMapType(fragment.MAP_TYPES[GoogleMap.MAP_TYPE_SATELLITE]);
    }

    public void onNormalClick(View view) {
        FragmentManager getSupportFragmentManager ;
        MapFragment fragment = (MapFragment)getSupportFragmentManager()
                .findFragmentById(R.id.map);
        fragment.changeMapType(fragment.MAP_TYPES[GoogleMap.MAP_TYPE_NORMAL]);
    }

    public void onHybridClick(View view) {
        FragmentManager getSupportFragmentManager ;
        MapFragment fragment = (MapFragment)getSupportFragmentManager()
                .findFragmentById(R.id.map);
        fragment.changeMapType(fragment.MAP_TYPES[GoogleMap.MAP_TYPE_HYBRID]);
    }

    public void onTerrainClick(View view) {
        FragmentManager getSupportFragmentManager ;
        MapFragment fragment = (MapFragment)getSupportFragmentManager()
                .findFragmentById(R.id.map);
        fragment.changeMapType(fragment.MAP_TYPES[GoogleMap.MAP_TYPE_TERRAIN]);
    }

    public void onNoneClick(View view) {
        FragmentManager getSupportFragmentManager ;
        MapFragment fragment = (MapFragment)getSupportFragmentManager()
                .findFragmentById(R.id.map);
        fragment.changeMapType(fragment.MAP_TYPES[GoogleMap.MAP_TYPE_NONE]);
    }
}
