package com.airmap.airmapsdk.ui.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.airmap.airmapsdk.Analytics;
import com.airmap.airmapsdk.R;
import com.airmap.airmapsdk.ui.views.AirMapMapView;
import com.airmap.airmapsdk.util.AirMapConstants;
import com.airmap.airmapsdk.util.AirMapLocationEngine;
import com.airmap.airmapsdk.util.Utils;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerMode;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin;
import com.mapbox.services.android.telemetry.location.LocationEngineListener;
import com.mapbox.services.android.telemetry.location.LocationEnginePriority;

import timber.log.Timber;

public abstract class MyLocationMapActivity extends AppCompatActivity implements LocationEngineListener {

    private static final int REQUEST_LOCATION_PERMISSION = 7737;
    private static final int REQUEST_TURN_ON_LOCATION = 8849;

    private AirMapMapView.OnMapLoadListener mapLoadListener;

    protected LocationLayerPlugin locationLayerPlugin;
    private AirMapLocationEngine locationEngine;
    private LocationRequest locationRequest;

    private boolean hasLoadedMyLocation;
    private boolean isLocationDialogShowing;
    private boolean isMapFailureDialogShowing;


    @Override
    public void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        setupMapLoadListener();

        locationRequest = LocationRequest.create();
        locationRequest.setInterval(500);
        locationRequest.setFastestInterval(250);
        locationRequest.setPriority(Utils.useGPSForLocation(this) ? LocationRequest.PRIORITY_HIGH_ACCURACY : LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        requestLocationPermissionIfNeeded();
    }

    @Override
    public void onRestart() {
        super.onRestart();

        setupMapLoadListener();
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onStart() {
        super.onStart();

        if (locationLayerPlugin != null && requestLocationPermissionIfNeeded()) {
            locationLayerPlugin.onStart();
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        if (locationLayerPlugin != null) {
            locationLayerPlugin.onStop();
        }

        if (locationEngine != null) {
            locationEngine.removeLocationUpdates();
        }

        if (getMapView() != null && mapLoadListener != null) {
            getMapView().removeOnMapLoadListener(mapLoadListener);
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_TURN_ON_LOCATION: {
                if (resultCode == Activity.RESULT_OK) {
                    Timber.i("Location setting turned on by user");
                    if (locationEngine != null) {
                        locationEngine.getLastLocation();
                        locationEngine.requestLocationUpdates();
                    } else if (getMapView().getMap() != null) {
                        setupLocationEngine();
                    }
                } else {
                    Timber.i("Location setting not turned on by user");
                    hasLoadedMyLocation = true;
                }

                isLocationDialogShowing = false;
                break;
            }
        }
    }

    @Override
    public void onConnected() {
        Timber.i("LocationEngine onConnected");
    }


    @Override
    public void onLocationChanged(Location location) {
        Timber.i("LocationEngine onLocationChanged: %s", location);
        zoomTo(location, false);
    }

    @SuppressLint("MissingPermission")
    public void goToLastLocation(boolean force) {
        if (force) {
            hasLoadedMyLocation = false;
        }

        if (!requestLocationPermissionIfNeeded()) {
            return;
        }

        if (locationEngine != null) {
            if (locationEngine.getLastLocation() != null) {
                zoomTo(locationEngine.getLastLocation(), force);
            } else {
                locationEngine.getLastKnownLocation();
                turnOnLocation();
            }
        } else {
            turnOnLocation();
        }
    }

    private void zoomTo(Location location, boolean force) {
        // only zoom to user's location once
        if (!hasLoadedMyLocation || force) {
            Timber.i("zoomTo: %s", location);

            int duration = getMapView().getMap().getCameraPosition().zoom < 10 ? 2500 : 1000;
            getMapView().getMap().animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 13), duration);
            locationEngine.removeLocationUpdates();
            hasLoadedMyLocation = true;

            // save location to prefs
            PreferenceManager.getDefaultSharedPreferences(this)
                    .edit()
                    .putFloat(AirMapConstants.LAST_LOCATION_LATITUDE, (float) location.getLatitude())
                    .putFloat(AirMapConstants.LAST_LOCATION_LONGITUDE, (float) location.getLongitude())
                    .apply();
        }
    }

    private void setupMapLoadListener() {
        // mapview not set yet
        if (getMapView() == null) {
            return;
        }

        mapLoadListener = new AirMapMapView.OnMapLoadListener() {
            @Override
            public void onMapLoaded() {
                if (hasLoadedMyLocation) {
                    return;
                }
                Timber.i("onMapLoaded");

                // use saved location is there is one
                float savedLatitude = PreferenceManager.getDefaultSharedPreferences(MyLocationMapActivity.this)
                        .getFloat(AirMapConstants.LAST_LOCATION_LATITUDE, 0);
                float savedLongitude = PreferenceManager.getDefaultSharedPreferences(MyLocationMapActivity.this)
                        .getFloat(AirMapConstants.LAST_LOCATION_LONGITUDE, 0);
                if (savedLatitude != 0 && savedLongitude != 0) {
                    getMapView().getMap().moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(savedLatitude, savedLongitude), 13));
                }

                setupLocationEngine();
            }

            @Override
            public void onMapFailed(AirMapMapView.MapFailure failure) {
                switch (failure) {
                    case INACCURATE_DATE_TIME_FAILURE:
                        // record issue in firebase & logs
                        Analytics.report(new Exception("Mapbox map failed to load due to invalid date/time"));
                        Timber.e("Mapbox map failed to load due to invalid date/time");

                        // ask user to enable "Automatic Date/Time"
                        new AlertDialog.Builder(MyLocationMapActivity.this)
                                .setTitle(R.string.error_loading_map_title)
                                .setMessage(R.string.error_loading_map_message)
                                .setPositiveButton(R.string.error_loading_map_button, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        // open settings and kill this activity
                                        startActivity(new Intent(Settings.ACTION_DATE_SETTINGS));
                                        finish();
                                    }
                                })
                                .setNegativeButton(android.R.string.cancel, null)
                                .show();
                        break;
                    case NETWORK_CONNECTION_FAILURE:
                        // record issue in firebase & logs
                        String log = Utils.getMapboxLogs();
                        Analytics.report(new Exception("Mapbox map failed to load due to no network connection: " + log));
                        Timber.e("Mapbox map failed to load due to no network connection");

                        // check if dialog is already showing
                        if (isMapFailureDialogShowing) {
                            return;
                        }

                        // ask user to turn on wifi/LTE
                        new AlertDialog.Builder(MyLocationMapActivity.this)
                                .setTitle(R.string.error_loading_map_title)
                                .setMessage(R.string.error_loading_map_network_message)
                                .setPositiveButton(R.string.error_loading_map_network_button, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        isMapFailureDialogShowing = false;
                                        // open settings and kill this activity
                                        startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                                        finish();
                                    }
                                })
                                .setNegativeButton(android.R.string.cancel, null)
                                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                                    @Override
                                    public void onDismiss(DialogInterface dialogInterface) {
                                        isMapFailureDialogShowing = false;
                                    }
                                })
                                .show();

                        isMapFailureDialogShowing = true;

                        break;
                    case UNKNOWN_FAILURE:
                    default:
                        // record issue in firebase & logs
                        String logs = Utils.getMapboxLogs();
                        Analytics.report(new Exception("Mapbox map failed to load due to unknown reason: " + logs));
                        Timber.e("Mapbox map failed to load due to unknown reason: %s", logs);

                        //TODO: show generic error to user?
                        break;
                }
            }
        };

        getMapView().addOnMapLoadListener(mapLoadListener);
    }

    @SuppressLint("MissingPermission")
    protected void setupLocationEngine() {
        if (!requestLocationPermissionIfNeeded()) {
            return;
        }

        Timber.d("setupLocationEngine");
        if (locationEngine != null) {
            Timber.w("already has location engine");
            return;
        }
        locationEngine = AirMapLocationEngine.getLocationEngine(this);
        locationEngine.setLocationRequest(locationRequest);
        locationEngine.addLocationEngineListener(this);
        locationEngine.activate();

        locationLayerPlugin = new LocationLayerPlugin(getMapView(), getMapView().getMap(), locationEngine, R.style.CustomLocationLayer);
        locationLayerPlugin.setLocationLayerEnabled(LocationLayerMode.TRACKING);

        turnOnLocation();
    }

    private boolean requestLocationPermissionIfNeeded() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
            return false;
        }

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            for (int i = 0; i < permissions.length; i++) {
                if ((permissions[i].equals(Manifest.permission.ACCESS_FINE_LOCATION) || permissions[i].equals(Manifest.permission.ACCESS_COARSE_LOCATION))
                        && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    goToLastLocation(false);
                }
            }
        }
    }

    /**
     * This turns on Wifi/cell location tracking using Google Play services
     * It shows a dismissible dialog for users that don't have location already enabled
     */
    public void turnOnLocation() {
        LocationSettingsRequest settingsRequest = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)
                .setAlwaysShow(true)
                .build();

        Task<LocationSettingsResponse> task = LocationServices.getSettingsClient(this).checkLocationSettings(settingsRequest);
        task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
            @SuppressLint("MissingPermission")
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                if (!requestLocationPermissionIfNeeded()) {
                    return;
                }

                // All location settings are satisfied. The client can initialize
                // location requests here.
                if (locationEngine != null) {
                    locationEngine.getLastLocation();
                    locationEngine.requestLocationUpdates();
                } else if (getMapView().getMap() != null) {
                    setupLocationEngine();
                }
            }
        });

        task.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    if (isLocationDialogShowing) {
                        return;
                    }

                    // Location settings are not satisfied, but this can be fixed
                    // by showing the user a dialog.
                    try {
                        // Show the dialog by calling startResolutionForResult(),
                        // and check the result in onActivityResult().
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(MyLocationMapActivity.this, REQUEST_TURN_ON_LOCATION);

                        isLocationDialogShowing = true;
                    } catch (IntentSender.SendIntentException sendEx) {
                        // Ignore the error.
                    }
                }
            }
        });
    }

    public void setLocationProvider(boolean useGPSForLocation) {
        if (locationEngine != null) {
            locationEngine.removeLocationUpdates();
            locationEngine.setPriority(useGPSForLocation ? LocationEnginePriority.HIGH_ACCURACY : LocationEnginePriority.BALANCED_POWER_ACCURACY);
        }

        turnOnLocation();
    }

    protected Location getMyLocation() {
        return locationLayerPlugin != null ? locationLayerPlugin.getLastKnownLocation() : null;
    }

    protected abstract AirMapMapView getMapView();

    public void setMapView(AirMapMapView mapView) {
        setupMapLoadListener();
    }
}