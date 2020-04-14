package com.airmap.airmapsdktest.activities;

import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;

import com.airmap.airmapsdk.AirMapException;
import com.airmap.airmapsdk.models.flight.AirMapFlight;
import com.airmap.airmapsdk.networking.callbacks.AirMapCallback;
import com.airmap.airmapsdk.networking.services.AirMap;
import com.airmap.airmapsdk.util.AnnotationsFactory;
import com.airmap.airmapsdktest.R;
import com.airmap.airmapsdktest.Utils;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.MarkerView;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

import java.util.Collections;
import java.util.List;

import timber.log.Timber;

public class TelemetryDemoActivity extends BaseActivity {

    private Toolbar toolbar;
    private MapView mapView;

    private AirMapFlight currentFlight;
    private Marker myMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_telemetry);

        toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(getTitle());
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mapView = findViewById(R.id.map_view);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(final MapboxMap map) {
                map.getUiSettings().setAllGesturesEnabled(false);
                setupMapDragging(map);

                AirMap.getCurrentFlight(new AirMapCallback<AirMapFlight>() {
                    @Override
                    protected void onSuccess(AirMapFlight flight) {
                        // if user has flight, add traffic
                        if (flight != null) {
                            currentFlight = flight;

                            // add a marker for our flight
                            MarkerOptions markerOptions = new MarkerOptions()
                                    .position(currentFlight.getCoordinate().toMapboxLatLng())
                                    .icon(IconFactory.getInstance(TelemetryDemoActivity.this).fromBitmap(Utils.getBitmap(TelemetryDemoActivity.this, R.drawable.current_flgiht_marker_icon)));

                            myMarker = map.addMarker(markerOptions);
                            map.animateCamera(CameraUpdateFactory.newLatLngZoom(flight.getCoordinate().toMapboxLatLng(), 12.5));

                            // Start listening for telemetry
                            Toast.makeText(TelemetryDemoActivity.this, "Enabling telemetry", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(TelemetryDemoActivity.this, "No active flight. Please go to brief and create a flight first.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    protected void onError(AirMapException e) {
                        Timber.e(e, "Get current flight failed");
                    }
                });
            }
        });
    }

    private void setupMapDragging(final MapboxMap map) {
        final float screenDensity = getResources().getDisplayMetrics().density;
        mapView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event != null) {
                    if (event.getPointerCount() > 1) {
                        return false; //Don't drag if there are multiple fingers on screen
                    }

                    if (myMarker == null) {
                        return false;
                    }

                    PointF tapPoint = new PointF(event.getX(), event.getY());
                    drag(map.getProjection().fromScreenLocation(tapPoint), false);

                    float toleranceSides = 4 * screenDensity;
                    float toleranceTopBottom = 10 * screenDensity;
                    float averageIconWidth = 42 * screenDensity;
                    float averageIconHeight = 42 * screenDensity;
                    RectF tapRect = new RectF(tapPoint.x - averageIconWidth / 2 - toleranceSides,
                            tapPoint.y - averageIconHeight / 2 - toleranceTopBottom,
                            tapPoint.x + averageIconWidth / 2 + toleranceSides,
                            tapPoint.y + averageIconHeight / 2 + toleranceTopBottom);

                    Marker newSelectedMarker = null;
                    List<MarkerView> nearbyMarkers = map.getMarkerViewsInRect(tapRect);
                    List<Marker> selectedMarkers = map.getSelectedMarkers();
                    if (selectedMarkers.isEmpty() && nearbyMarkers != null && !nearbyMarkers.isEmpty()) {
                        Collections.sort(nearbyMarkers);
                        for (Marker marker : nearbyMarkers) {
                            if (marker instanceof MarkerView && !((MarkerView) marker).isVisible()) {
                                continue; //Don't let user click on hidden midpoints
                            }
                            if (!marker.getTitle().equals(AnnotationsFactory.INTERSECTION_TAG)) {
                                newSelectedMarker = marker;
                                break;
                            }
                        }
                    } else if (!selectedMarkers.isEmpty()) {
                        newSelectedMarker = selectedMarkers.get(0);
                    }

                    if (newSelectedMarker != null && newSelectedMarker instanceof MarkerView) {
                        boolean doneDragging = event.getAction() == MotionEvent.ACTION_UP;
                        boolean deletePoint = false;

                        //DRAG!
                        map.selectMarker(newSelectedMarker); //Use the marker selection state to prevent selecting another marker when dragging over it
                        newSelectedMarker.hideInfoWindow();
                        drag(map.getProjection().fromScreenLocation(tapPoint), doneDragging);
                        if (doneDragging) {
                            map.deselectMarker(newSelectedMarker);
                        }
                        return true;
                    }
                }
                return false;
            }
        });
    }

    private void drag(LatLng newLocation, boolean doneDragging) {
        myMarker.setPosition(newLocation);

        AirMap.getTelemetryService().sendPositionMessage(currentFlight.getFlightId(), newLocation.getLatitude(), newLocation.getLongitude(), 0, (float) newLocation.getAltitude(), 1f);
    }

    // Mapbox requires lifecycle
    @Override
    public void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
}
