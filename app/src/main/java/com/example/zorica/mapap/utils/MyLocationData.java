package com.example.zorica.mapap.utils;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.widget.Spinner;
import android.widget.Toast;

import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.view.LocationDisplay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol;
import com.esri.arcgisruntime.symbology.Symbol;

public class MyLocationData {

    private MapView mMapView;
    private LocationDisplay mLocationDisplay;
    private Context context;

    private int requestCode = 2;
    String[] reqPermissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission
            .ACCESS_COARSE_LOCATION};

    public MyLocationData(Context context, MapView mapView) {
        super();
        this.mMapView = mapView;
        this.context = context;
    }

    public Point setLocation() {


        ArcGISMap mMap = new ArcGISMap(Basemap.createStreetsVector());
        mMapView.setMap(mMap);
        buildAlertMessageNoGps();

        // get the MapView's LocationDisplay
        mLocationDisplay = mMapView.getLocationDisplay();


        // Listen to changes in the status of the location data source.
        mLocationDisplay.addDataSourceStatusChangedListener(new LocationDisplay.DataSourceStatusChangedListener() {
            @Override
            public void onStatusChanged(LocationDisplay.DataSourceStatusChangedEvent dataSourceStatusChangedEvent) {

                if (dataSourceStatusChangedEvent.isStarted())
                    return;

                if (dataSourceStatusChangedEvent.getError() == null)
                    return;


                boolean permissionCheck1 = ContextCompat.checkSelfPermission(context, reqPermissions[0]) ==
                        PackageManager.PERMISSION_GRANTED;
                boolean permissionCheck2 = ContextCompat.checkSelfPermission(context, reqPermissions[1]) ==
                        PackageManager.PERMISSION_GRANTED;

                if (!(permissionCheck1 && permissionCheck2)) {

                    ActivityCompat.requestPermissions((Activity) context, reqPermissions, requestCode);
                } else {

                    String message = String.format("Error in DataSourceStatusChangedListener: %s", dataSourceStatusChangedEvent
                            .getSource().getLocationDataSource().getError().getMessage());
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show();
                }
            }
        });

        mLocationDisplay.startAsync();
//      mLocationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.COMPASS_NAVIGATION);
        mLocationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.NAVIGATION);
        mLocationDisplay.setCourseSymbol(new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CIRCLE, Color.GREEN, 15 ));
        mLocationDisplay.setDefaultSymbol(new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CIRCLE, Color.GREEN, 15 ));
        mLocationDisplay.setHeadingSymbol(new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CIRCLE, Color.GREEN, 15 ));
        mLocationDisplay.setAcquiringSymbol(new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CIRCLE, Color.GREEN, 15 ));


        return mLocationDisplay.getLocation().getPosition();


    }


    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage("Please enable your GPS before proceeding")
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        context.startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }
}
