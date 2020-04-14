package com.airmap.airmapsdktest.activities;

import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.animation.LinearInterpolator;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import com.airmap.airmapsdk.AirMapException;
import com.airmap.airmapsdk.models.flight.AirMapFlight;
import com.airmap.airmapsdk.models.traffic.AirMapTraffic;
import com.airmap.airmapsdk.networking.callbacks.AirMapCallback;
import com.airmap.airmapsdk.networking.callbacks.AirMapTrafficListener;
import com.airmap.airmapsdk.networking.services.AirMap;
import com.airmap.airmapsdk.ui.views.AirMapMapView;
import com.airmap.airmapsdktest.R;
import com.airmap.airmapsdktest.Utils;
import com.airmap.airmapsdktest.ui.TrafficMarker;
import com.airmap.airmapsdktest.ui.TrafficMarkerOptions;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;

public class TrafficDemoActivity extends BaseActivity implements AirMapMapView.OnMapLoadListener, AirMapTrafficListener {

    private Toolbar toolbar;
    private AirMapMapView mapView;

    private TextToSpeech textToSpeech;
    private List<TrafficMarker> trafficMarkers;
    private AirMapFlight currentFlight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_traffic);

        textToSpeech = new TextToSpeech(this, null);
        textToSpeech.setLanguage(Locale.getDefault());
        trafficMarkers = new ArrayList<>();

        toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(getTitle());
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mapView = findViewById(R.id.map_view);
        mapView.onCreate(savedInstanceState);
        mapView.addOnMapLoadListener(this);
    }

    @Override
    public void onMapLoaded() {
        mapView.getMap().animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(34.0195, -118.4912), 12.5));

        AirMap.getCurrentFlight(new AirMapCallback<AirMapFlight>() {
            @Override
            protected void onSuccess(AirMapFlight response) {
                // if user has flight, add traffic
                if (response != null) {
                    currentFlight = response;

                    // add a marker for our flight
                    MarkerOptions markerOptions = new MarkerOptions()
                            .position(currentFlight.getCoordinate().toMapboxLatLng())
                            .icon(IconFactory.getInstance(TrafficDemoActivity.this).fromBitmap(Utils.getBitmap(TrafficDemoActivity.this, R.drawable.current_flgiht_marker_icon)));

                    mapView.getMap().addMarker(markerOptions);

                    // Start listening for traffic
                    AirMap.enableTrafficAlerts(TrafficDemoActivity.this);
                    Toast.makeText(TrafficDemoActivity.this, "Enabling traffic alerts", Toast.LENGTH_SHORT).show();

                    // if not, user needs to create flight first
                } else {
                    Toast.makeText(TrafficDemoActivity.this, "No active flight. Please create a flight first.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            protected void onError(AirMapException e) {
                Timber.e(e, "Get current flight failed");
            }
        });
    }

    @Override
    public void onMapFailed(AirMapMapView.MapFailure reason) {
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
        mapView.removeOnMapLoadListener(this);

        AirMap.disableTrafficAlerts();
        textToSpeech.shutdown();
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

    @Override
    public void onAddTraffic(List<AirMapTraffic> added) {
        boolean shouldSayTraffic = false;
        for (AirMapTraffic traffic : added) {
            TrafficMarkerOptions trafficMarkerOptions = new TrafficMarkerOptions().setTraffic(traffic).icon(getIcon(traffic));
            TrafficMarker trafficMarker = (TrafficMarker) mapView.getMap().addMarker(trafficMarkerOptions);
            trafficMarkers.add(trafficMarker);
            showTrafficAlert(traffic);

            if (traffic.getTrafficType() == AirMapTraffic.TrafficType.Alert) {
                shouldSayTraffic = true;
            }
        }

        if (shouldSayTraffic) {
            sayTraffic();
        }
    }

    @Override
    public void onUpdateTraffic(List<AirMapTraffic> updated) {
        boolean shouldSayTraffic = false;
        for (AirMapTraffic traffic : updated) {
            TrafficMarker marker = searchById(traffic);
            if (marker != null) {
                if (traffic.getTrafficType() == AirMapTraffic.TrafficType.Alert && marker.getTraffic().getTrafficType() == AirMapTraffic.TrafficType.SituationalAwareness) { //If it changed from SitAwareness to Alert, notify
                    showTrafficAlert(traffic);
                    shouldSayTraffic = true;
                    marker.setIcon(getIcon(traffic)); //Change from Sit Awareness to Alert Icon
                }
                if (traffic.getTrueHeading() != marker.getTraffic().getTrueHeading()) {
                    marker.setIcon(getIcon(traffic));
                }
                marker.setTraffic(traffic);
                LatLng latLng = Utils.getLatLngFromCoordinate(traffic.getCoordinate());
                final ValueAnimator markerAnimator = ObjectAnimator.ofObject(marker, "position", new LatLngEvaluator(), marker.getPosition(), latLng); //Animate the traffic's location from old position to new position
                markerAnimator.setDuration(1000);
                markerAnimator.setInterpolator(new LinearInterpolator());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        markerAnimator.start();
                    }
                });
            }
        }
        if (shouldSayTraffic) {
            sayTraffic();
        }
    }

    @Override
    public void onRemoveTraffic(List<AirMapTraffic> removed) {
        for (AirMapTraffic traffic : removed) {
            TrafficMarker marker = searchById(traffic);
            if (marker != null) {
                marker.remove();
                trafficMarkers.remove(marker);
            }
        }
    }

    /**
     * Audio alert
     */
    protected void sayTraffic() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            textToSpeech.speak(getString(R.string.traffic), TextToSpeech.QUEUE_FLUSH, null, null);
        } else {
            textToSpeech.speak(getString(R.string.traffic), TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    public void showTrafficAlert(@NonNull AirMapTraffic traffic) {
        LatLng activeFlightLocation = Utils.getLatLngFromCoordinate(currentFlight.getCoordinate());
        double distanceInMeters = activeFlightLocation.distanceTo(Utils.getLatLngFromCoordinate(traffic.getCoordinate()));
        double distance = Utils.metersToMiles(distanceInMeters);
        double speed = Utils.ktsToMph(traffic.getGroundSpeedKt());
        double timeInHours = distance / speed;
        final StringBuilder trafficText = new StringBuilder()
                .append(traffic.getProperties().getAircraftId()).append("\n")
                .append(getString(R.string.distance_in_miles, distance)).append(" ")
                .append(Utils.directionFromBearing(this, traffic.getTrueHeading())).append("\n")
                .append(Utils.minutesToMinSec(this, timeInHours * 60));

        Toast.makeText(this, trafficText.toString(), Toast.LENGTH_SHORT).show();
    }

    public TrafficMarker searchById(AirMapTraffic traffic) {
        for (TrafficMarker marker : trafficMarkers) {
            if (marker.getTraffic().equals(traffic)) {
                return marker;
            }
        }
        return null;
    }

    /**
     * Dynamically provides an icon based on which direction the traffic is traveling
     *
     * @param traffic The traffic to get an icon for
     * @return An icon
     */
    private Icon getIcon(@NonNull AirMapTraffic traffic) {
        //Generate the icon dynamically based on which direction the traffic is pointing/traveling
        IconFactory factory = IconFactory.getInstance(this);
        int id = 0;
        if (traffic.getTrafficType() == AirMapTraffic.TrafficType.SituationalAwareness) {
            id = getResources().getIdentifier("sa_traffic_marker_icon_" + Utils.directionFromBearing(this, traffic.getTrueHeading()).toLowerCase(), "drawable", getPackageName());
        } else if (traffic.getTrafficType() == AirMapTraffic.TrafficType.Alert) {
            id = getResources().getIdentifier("traffic_marker_icon_" + Utils.directionFromBearing(this, traffic.getTrueHeading()).toLowerCase(), "drawable", getPackageName());
        }
        return factory.fromBitmap(Utils.getBitmap(this, id));
    }

    private class LatLngEvaluator implements TypeEvaluator<LatLng> {
        // Method is used to interpolate the marker animation.
        private LatLng latLng = new LatLng();

        @Override
        public LatLng evaluate(float fraction, LatLng startValue, LatLng endValue) {
            latLng.setLatitude(startValue.getLatitude() + ((endValue.getLatitude() - startValue.getLatitude()) * fraction));
            latLng.setLongitude(startValue.getLongitude() + ((endValue.getLongitude() - startValue.getLongitude()) * fraction));
            return latLng;
        }
    }
}
