package com.example.zorica.mapap;

import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.ArcGISFeature;
import com.esri.arcgisruntime.data.Feature;
import com.esri.arcgisruntime.data.FeatureEditResult;
import com.esri.arcgisruntime.data.FeatureQueryResult;
import com.esri.arcgisruntime.data.QueryParameters;
import com.esri.arcgisruntime.data.ServiceFeatureTable;
import com.esri.arcgisruntime.geometry.Geometry;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.GeoElement;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.IdentifyLayerResult;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.tasks.networkanalysis.AttributeParameterValue;
import com.esri.arcgisruntime.tasks.networkanalysis.DirectionManeuver;
import com.esri.arcgisruntime.tasks.networkanalysis.Route;
import com.esri.arcgisruntime.tasks.networkanalysis.RouteParameters;
import com.esri.arcgisruntime.tasks.networkanalysis.RouteResult;
import com.esri.arcgisruntime.tasks.networkanalysis.RouteTask;
import com.esri.arcgisruntime.tasks.networkanalysis.Stop;
import com.esri.arcgisruntime.tasks.networkanalysis.TravelMode;
import com.example.zorica.mapap.utils.MyLocationData;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "FindRouteSample";

    private MapView mMapView;
    private RouteTask mRouteTask;
    private RouteParameters mRouteParams;
    private Route mRoute;
    private SimpleLineSymbol mRouteSymbol;
    private GraphicsOverlay mGraphicsOverlay;

    private ListView mDrawerList;
    private ProgressBar mProgressBar;

    private Point mLocation;

    private ServiceFeatureTable mServiceFeatureTable;
    private FeatureLayer mFeatureLayer;
    private ArcGISFeature mSelectedArcGISFeature;
    private android.graphics.Point mClickPoint;
    private String mSelectedArcGISFeatureAttributeValue;
    private String mAttributeID;
    private ArcGISMap mMap;
    private boolean areBenchesShowing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, null, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        mProgressBar = (ProgressBar) findViewById(R.id.progress);


        mMapView = (MapView) findViewById(R.id.mapView);

        MyLocationData myLocationData = new MyLocationData(MainActivity.this, mMapView);
        mLocation = myLocationData.setLocation();
        Log.d("mlocation", mLocation.toString());

        mMap = new ArcGISMap(Basemap.createStreetsVector());
        Viewpoint skopjePoint = new Viewpoint(41.9973, 21.4280, 20000);

        mMap.setInitialViewpoint(skopjePoint);
        mMapView.setMap(mMap);

        mServiceFeatureTable = new ServiceFeatureTable(getResources().getString(R.string.feature_layer_area));
        mServiceFeatureTable.setFeatureRequestMode(ServiceFeatureTable.FeatureRequestMode.ON_INTERACTION_CACHE);

        mFeatureLayer = new FeatureLayer(mServiceFeatureTable);

        mMap.getOperationalLayers().add(mFeatureLayer);


        mMapView.setOnTouchListener(new DefaultMapViewOnTouchListener(this, mMapView) {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {


                mClickPoint = new android.graphics.Point((int) e.getX(), (int) e.getY());


                mFeatureLayer.clearSelection();
                mSelectedArcGISFeature = null;


                final ListenableFuture<IdentifyLayerResult> futureIdentifyLayer = mMapView.identifyLayerAsync(mFeatureLayer, mClickPoint, 5, false, 1);


                futureIdentifyLayer.addDoneListener(new Runnable() {
                    @Override
                    public void run() {
                        try {

                            IdentifyLayerResult layerResult = futureIdentifyLayer.get();

                            List<GeoElement> resultGeoElements = layerResult.getElements();
                            if (resultGeoElements.size() > 0) {
                                if (resultGeoElements.get(0) instanceof ArcGISFeature) {

                                    mSelectedArcGISFeature = (ArcGISFeature) resultGeoElements.get(0);
                                    mFeatureLayer.selectFeature(mSelectedArcGISFeature);
                                    mAttributeID = mSelectedArcGISFeature.getAttributes().get("objectid").toString();

                                    mSelectedArcGISFeatureAttributeValue = (String) mSelectedArcGISFeature.getAttributes().get("availability");


                                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

                                    if (mSelectedArcGISFeatureAttributeValue.equals("true")) {
                                        builder.setTitle("Оваа клупа е слободна.");
                                        builder.setMessage("Дали сакате да ја обележите како зафатена?");
                                    } else {

                                        builder.setTitle("Оваа клупа е зафатена.");
                                        builder.setMessage("Дали сакате да ја обележите како слободна?");
                                    }

                                    builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {

                                        public void onClick(DialogInterface dialog, int which) {
                                            if (mSelectedArcGISFeatureAttributeValue.equals("true")) {
                                                mSelectedArcGISFeature.getAttributes().put("availability", "false");
                                            } else {
                                                mSelectedArcGISFeature.getAttributes().put("availability", "true");
                                            }

                                            mProgressBar.setVisibility(View.VISIBLE);
                                            ListenableFuture<Void> tableResult = mServiceFeatureTable.updateFeatureAsync(mSelectedArcGISFeature);

                                            applyServerEdits();
                                            dialog.dismiss();
                                        }
                                    });

                                    builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {

                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {

                                            dialog.dismiss();
                                        }
                                    });

                                    AlertDialog alert = builder.create();
                                    alert.show();
                                } else {
                                    Toast.makeText(MainActivity.this, mSelectedArcGISFeatureAttributeValue, Toast.LENGTH_SHORT).show();
                                }
                            }
                        } catch (Exception e) {
                            Toast.makeText(MainActivity.this, "Грешка на сервер, пробајте повторно.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                return super.onSingleTapConfirmed(e);
            }
        });

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
            setTitle(getString(R.string.app_name));
        }


    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.free_bench_item) {

            if (areBenchesShowing)
                mMap.getOperationalLayers().remove(mFeatureLayer);

            mProgressBar.setVisibility(View.VISIBLE);
            mServiceFeatureTable = new ServiceFeatureTable(getResources().getString(R.string.feature_layer_points));
            mServiceFeatureTable.setFeatureRequestMode(ServiceFeatureTable.FeatureRequestMode.MANUAL_CACHE);

            mFeatureLayer = new FeatureLayer(mServiceFeatureTable);

            mFeatureLayer.setSelectionColor(Color.rgb(244, 226, 66));
            mFeatureLayer.setSelectionWidth(3);

            mMap.getOperationalLayers().add(mFeatureLayer);


            mServiceFeatureTable.loadAsync();

            mServiceFeatureTable.addDoneLoadingListener(new Runnable() {
                @Override
                public void run() {

                    QueryParameters params = new QueryParameters();
                    params.setWhereClause("availability = 'true'");

                    List<String> outFields = new ArrayList<>();
                    outFields.add("*");

                    final ListenableFuture<FeatureQueryResult> future = mServiceFeatureTable.populateFromServiceAsync(params, true, outFields);

                    future.addDoneListener(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                //call get on the future to get the result
                                if (mProgressBar.isShown())
                                    mProgressBar.setVisibility(View.GONE);

                                FeatureQueryResult result = future.get();
                                Iterator<Feature> iterator = result.iterator();
                                Feature feature;

                                int counter = 0;
                                while (iterator.hasNext()) {
                                    feature = iterator.next();
                                    counter++;
                                }

                                if (counter == 0) {
                                    Toast.makeText(MainActivity.this, "Нема слободни клупи.", Toast.LENGTH_SHORT).show();
                                    areBenchesShowing = false;

                                } else {
                                    areBenchesShowing = true;

                                }


                            } catch (Exception e) {
                                Toast.makeText(MainActivity.this, "Грешка на сервер, пробајте повторно.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            });


        } else if (id == R.id.all_benches_item) {

            if (areBenchesShowing)
                mMap.getOperationalLayers().remove(mFeatureLayer);

            mProgressBar.setVisibility(View.VISIBLE);
            mServiceFeatureTable = new ServiceFeatureTable(getResources().getString(R.string.feature_layer_points));
            mServiceFeatureTable.setFeatureRequestMode(ServiceFeatureTable.FeatureRequestMode.ON_INTERACTION_CACHE);

            mFeatureLayer = new FeatureLayer(mServiceFeatureTable);


            mFeatureLayer.setSelectionColor(Color.rgb(244, 226, 66));
            mFeatureLayer.setSelectionWidth(3);

            mMap.getOperationalLayers().add(mFeatureLayer);

            mServiceFeatureTable.addDoneLoadingListener(new Runnable() {
                @Override
                public void run() {
                    if (mProgressBar.isShown())
                        mProgressBar.setVisibility(View.GONE);

                    areBenchesShowing = true;
                }
            });

        } else if (id == R.id.not_in_park_item) {
            showRouteToPark();

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void setupSymbols() {

        mGraphicsOverlay = new GraphicsOverlay();
        mMapView.getGraphicsOverlays().add(mGraphicsOverlay);

        mRouteSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.rgb(20, 219, 114), 10);
    }


    @Override
    protected void onPause() {
        super.onPause();
        mMapView.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.resume();
    }

    public void showRouteToPark() {

        setupSymbols();

        mProgressBar.setVisibility(View.VISIBLE);

        // create RouteTask instance
        mRouteTask = new RouteTask(getString(R.string.routing_service));

        final ListenableFuture<RouteParameters> listenableFuture = mRouteTask.createDefaultParametersAsync();
        listenableFuture.addDoneListener(new Runnable() {
            @Override
            public void run() {
                try {
                    if (listenableFuture.isDone()) {
                        int i = 0;
                        mRouteParams = listenableFuture.get();
                        mProgressBar.setVisibility(View.GONE);


                        List<Stop> routeStops = mRouteParams.getStops();

                        mRouteParams.setReturnDirections(true);
                        TravelMode walkMode = mRouteParams.getTravelMode();
                        walkMode.getAttributeParameterValues().remove(2);


                        AttributeParameterValue attributeParameterValue = new AttributeParameterValue();
                        attributeParameterValue.setAttributeName("Preferred for Pedestrians");
                        attributeParameterValue.setParameterName("Restriction Usage");
                        attributeParameterValue.setParameterValue(5);
                        walkMode.getAttributeParameterValues().add(attributeParameterValue);

                        walkMode.setImpedanceAttributeName("WalkTime");

                        walkMode.getRestrictionAttributeNames().clear();
                        walkMode.getRestrictionAttributeNames().add(0, "Preferred for Pedestrians");
                        walkMode.getRestrictionAttributeNames().add(1, "Walking");

                        walkMode.setUseHierarchy(false);
                        walkMode.setTimeAttributeName("WalkTime");
                        walkMode.setDistanceAttributeName("Miles");
                        walkMode.setType("WALK");
                        walkMode.setName("Walking Time");

                        mRouteParams.setTravelMode(walkMode);


                        routeStops.add(new Stop(mLocation));
                        routeStops.add(new Stop(new Point(21.422236, 42.003023, SpatialReferences.getWgs84())));


                        RouteResult result = mRouteTask.solveRouteAsync(mRouteParams).get();
                        final List routes = result.getRoutes();
                        mRoute = (Route) routes.get(0);

                        Graphic routeGraphic = new Graphic(mRoute.getRouteGeometry(), mRouteSymbol);

                        mGraphicsOverlay.getGraphics().add(routeGraphic);


                        final List<DirectionManeuver> directions = mRoute.getDirectionManeuvers();

                        String[] directionsArray = new String[directions.size()];

                        for (DirectionManeuver dm : directions) {
                            directionsArray[i++] = dm.getDirectionText();
                        }
                        Log.d(TAG, directions.get(0).getGeometry().getExtent().getXMin() + "");
                        Log.d(TAG, directions.get(0).getGeometry().getExtent().getYMin() + "");

                        mDrawerList.setAdapter(new ArrayAdapter<>(getApplicationContext(),
                                R.layout.drawer_layout_text, directionsArray));

                        if (mProgressBar.getVisibility() == View.VISIBLE) {
                            mProgressBar.setVisibility(View.GONE);
                        }
//                        mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//                            @Override
//                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                                if (mGraphicsOverlay.getGraphics().size() > 3) {
//                                    mGraphicsOverlay.getGraphics().remove(mGraphicsOverlay.getGraphics().size() - 1);
//                                }
//                                DirectionManeuver dm = directions.get(position);
//                                Geometry gm = dm.getGeometry();
//                                Viewpoint vp = new Viewpoint(gm.getExtent(), 20);
//                                mMapView.setViewpointAsync(vp, 3);
//                                SimpleLineSymbol selectedRouteSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, R.color.colorAccent, 3);
//                                Graphic selectedRouteGraphic = new Graphic(directions.get(position).getGeometry(), selectedRouteSymbol);
//                                mGraphicsOverlay.getGraphics().add(selectedRouteGraphic);
//                            }
//                        });

                    }
                } catch (Exception e) {

                }
            }
        });


    }


    private void applyServerEdits() {

        try {
            final ListenableFuture<List<FeatureEditResult>> updatedServerResult = mServiceFeatureTable.applyEditsAsync();

            updatedServerResult.addDoneListener(new Runnable() {

                @Override
                public void run() {
                    try {

                        List<FeatureEditResult> edits = updatedServerResult.get();
                        if (edits.size() > 0) {
                            if (!edits.get(0).hasCompletedWithErrors()) {
                                if (mProgressBar.isShown()) {
                                    mProgressBar.setVisibility(View.GONE);
                                }
                                //attachmentList.add(fileName);
                                mAttributeID = mSelectedArcGISFeature.getAttributes().get("objectid").toString();
                                // update the attachment list view on the control panel
                                Toast.makeText(MainActivity.this, getApplication().getString(R.string.success_message), Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(MainActivity.this, getApplication().getString(R.string.failure_message), Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(MainActivity.this, getApplication().getString(R.string.failure_edit_results), Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {

                        e.printStackTrace();
                    }
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
