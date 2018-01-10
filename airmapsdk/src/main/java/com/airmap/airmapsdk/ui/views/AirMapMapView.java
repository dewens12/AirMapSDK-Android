package com.airmap.airmapsdk.ui.views;

import android.arch.lifecycle.LifecycleObserver;
import android.content.Context;
import android.graphics.PointF;
import android.graphics.RectF;
import android.provider.Settings;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.Toast;

import com.airmap.airmapsdk.AirMapLog;
import com.airmap.airmapsdk.controllers.MapDataController;
import com.airmap.airmapsdk.controllers.MapStyleController;
import com.airmap.airmapsdk.models.rules.AirMapRuleset;
import com.airmap.airmapsdk.models.status.AirMapAdvisory;
import com.airmap.airmapsdk.models.status.AirMapAirspaceStatus;
import com.airmap.airmapsdk.util.Utils;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.style.layers.Filter;
import com.mapbox.services.commons.geojson.Feature;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by collin@airmap.com on 9/21/17.
 */

public class AirMapMapView extends MapView implements MapView.OnMapChangedListener, MapboxMap.OnMapClickListener, MapDataController.Callback {

    private static final String TAG = "AirMapMapView";

    private MapboxMap map;

    private MapStyleController mapStyleController;
    private MapDataController mapDataController;

    // optional callbacks
    private List<OnMapLoadListener> mapLoadListeners;
    private List<OnMapDataChangeListener> mapDataChangeListeners;
    private List<OnAdvisoryClickListener> advisoryClickListeners;

    public AirMapMapView(@NonNull Context context) {
        this(context, null, 0);
    }

    public AirMapMapView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AirMapMapView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        Configuration defaultConfig = new AutomaticConfiguration();
        init(defaultConfig);
    }

    public void init(Configuration configuration) {
        mapLoadListeners = new ArrayList<>();
        mapDataChangeListeners = new ArrayList<>();
        advisoryClickListeners = new ArrayList<>();

        // default data controller
        mapDataController = new MapDataController(this, configuration);

        mapStyleController = new MapStyleController(this, new MapStyleController.Callback() {
            @Override
            public void onMapStyleReset() {
                mapDataController.onMapReset();
            }

            @Override
            public void onMapStyleLoaded() {
                AirMapLog.e(TAG, "onMapStyleLoaded: " + getMap().getCameraPosition());
                mapDataController.onMapLoaded();

                for (OnMapLoadListener mapLoadListener : mapLoadListeners) {
                    mapLoadListener.onMapLoaded();
                }
            }
        });

        addOnMapChangedListener(this);
    }


    public void configure(Configuration configuration) {
        mapDataController.configure(configuration);
    }

    /**
     *  Override data controller
     *
     *  @param controller
     */
    public void setMapDataController(MapDataController controller) {
        // destroy old data controller (unsubscribe from rx)
        mapDataController.onDestroy();

        this.mapDataController = controller;
    }

    public void rotateMapTheme() {
        mapStyleController.rotateMapTheme();
    }

    @Override
    public void getMapAsync(final OnMapReadyCallback callback) {
        super.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {
                map = mapboxMap;
                map.getUiSettings().setLogoGravity(Gravity.BOTTOM | Gravity.END); // Move to bottom right
                map.getUiSettings().setAttributionGravity(Gravity.BOTTOM | Gravity.END); // Move to bottom right
                mapStyleController.onMapReady();

                if (callback != null) {
                    callback.onMapReady(mapboxMap);
                }

            }
        });
    }

    @Override
    public void onMapChanged(int change) {
        switch (change) {
            // check if map failed
            case MapView.DID_FAIL_LOADING_MAP: {
                MapFailure failure = MapFailure.UNKNOWN_FAILURE;

                /**
                 *  Devices with an inaccurate date/time will not be able to load the mapbox map
                 *  If the "automatic date/time" is disabled on the device and the map fails to load, recommend the user enable it
                 */
                int autoTimeSetting = Settings.Global.getInt(getContext().getContentResolver(), Settings.Global.AUTO_TIME, 0);
                if (autoTimeSetting == 0) {
                    failure = MapFailure.INACCURATE_DATE_TIME_FAILURE;
                } else if (!Utils.isNetworkConnected(getContext())) {
                    failure = MapFailure.NETWORK_CONNECTION_FAILURE;
                }

                for (OnMapLoadListener mapLoadListener : mapLoadListeners) {
                    mapLoadListener.onMapFailed(failure);
                }
                break;
            }
            case MapView.REGION_DID_CHANGE:
            case MapView.REGION_DID_CHANGE_ANIMATED: {
                if (mapDataController != null) {
                    mapDataController.onMapRegionChanged();
                }
                break;
            }
            default: {
                break;
            }
        }
    }

    @Override
    public void onMapClick(@NonNull LatLng point) {
        if (advisoryClickListeners == null || advisoryClickListeners.isEmpty()) {
            return;
        }

        PointF clickPoint = map.getProjection().toScreenLocation(point);
        RectF clickRect = new RectF(clickPoint.x - 10, clickPoint.y - 10, clickPoint.x + 10, clickPoint.y + 10);
        RectF mapRect = new RectF(getLeft(), getTop(), getRight(), getBottom());
        Filter.Statement filter = Filter.has("airspace_id");
        List<Feature> allFeatures = map.queryRenderedFeatures(mapRect, filter);

        if (allFeatures.size() > 400) {
            return;
        }

        List<Feature> selectedFeatures = map.queryRenderedFeatures(clickRect, filter);
        List<AirMapAdvisory> allAdvisories = mapDataController.getCurrentAdvisories();
        if (allAdvisories == null) {
            return;
        }

        Set<AirMapAdvisory> filteredAdvisories = new HashSet<>(); // Include only those with features on map
        AirMapAdvisory clickedAdvisory = null;
        for (AirMapAdvisory advisory : allAdvisories) {
            for (Feature feature : selectedFeatures) {
                if (feature.hasProperty("airspace_id") && advisory.getId().equals(feature.getStringProperty("airspace_id"))) {
                    clickedAdvisory = advisory;
                    break;
                }
            }

            for (Feature feature : allFeatures) {
                if (feature.getStringProperty("airspace_id").equals(advisory.getId())) {
                    filteredAdvisories.add(advisory);
                }
            }

        }

        if (clickedAdvisory != null) {
            for (OnAdvisoryClickListener advisoryClickListener : advisoryClickListeners) {
                advisoryClickListener.onAdvisoryClicked(clickedAdvisory);
            }
        }
    }

    @Override
    public void onRulesetsUpdated(List<AirMapRuleset> availableRulesets, List<AirMapRuleset> selectedRulesets, List<AirMapRuleset> previouslySelectedRulesetsSelectedRulesets) {
        AirMapLog.e(TAG, "onRulesetsUpdated to: " + selectedRulesets + " from: " + previouslySelectedRulesetsSelectedRulesets);

        setLayers(selectedRulesets, previouslySelectedRulesetsSelectedRulesets);

        for (OnMapDataChangeListener mapDataChangeListener : mapDataChangeListeners) {
            mapDataChangeListener.onRulesetsChanged(availableRulesets, selectedRulesets);
        }
    }

    @Override
    public void onAdvisoryStatusUpdated(AirMapAirspaceStatus advisoryStatus) {
        for (OnMapDataChangeListener mapDataChangeListener : mapDataChangeListeners) {
            mapDataChangeListener.onAdvisoryStatusChanged(advisoryStatus);
        }
    }

    @Override
    public void onAdvisoryStatusLoading() {
        for (OnMapDataChangeListener mapDataChangeListener : mapDataChangeListeners) {
            mapDataChangeListener.onAdvisoryStatusLoading();
        }
    }

    private void setLayers(List<AirMapRuleset> newRulesets, List<AirMapRuleset> oldRulesets) {
        if (oldRulesets != null) {
            for (AirMapRuleset oldRuleset : oldRulesets) {
                if (!newRulesets.contains(oldRuleset)) {
                    mapStyleController.removeMapLayers(oldRuleset.getId(), oldRuleset.getLayers());
                }
            }
        }


        for (AirMapRuleset newRuleset : newRulesets) {
            if (oldRulesets == null || !oldRulesets.contains(newRuleset)) {
                mapStyleController.addMapLayers(newRuleset.getId(), newRuleset.getLayers());
            }
        }
    }

    public MapboxMap getMap() {
        return map;
    }

    public List<AirMapRuleset> getSelectedRulesets() {
        return mapDataController.getSelectedRulesets();
    }

    // callbacks
    public void addOnMapLoadListener(OnMapLoadListener listener) {
        mapLoadListeners.add(listener);

        if (getMap() != null) {
            listener.onMapLoaded();
        }
    }

    public void removeOnMapLoadListener(OnMapLoadListener listener) {
        mapLoadListeners.remove(listener);
    }

    public void addOnMapDataChangedListener(OnMapDataChangeListener listener) {
        mapDataChangeListeners.add(listener);

        if (getMap() != null) {
            listener.onRulesetsChanged(mapDataController.getAvailableRulesets(), mapDataController.getSelectedRulesets());

            listener.onAdvisoryStatusChanged(mapDataController.getAirspaceStatus());
        }
    }

    public void removeOnMapDataChangedListener(OnMapDataChangeListener listener) {
        mapDataChangeListeners.remove(listener);
    }

    public void addOnAdvisoryClickListener(OnAdvisoryClickListener listener) {
        advisoryClickListeners.add(listener);
    }

    public void removeOnAdvisoryClickListener(OnAdvisoryClickListener listener) {
        advisoryClickListeners.remove(listener);
    }

    public interface OnMapLoadListener {
        void onMapLoaded();
        void onMapFailed(MapFailure reason);
    }

    public interface OnMapDataChangeListener {
        void onRulesetsChanged(List<AirMapRuleset> availableRulesets, List<AirMapRuleset> selectedRulesets);
        void onAdvisoryStatusChanged(AirMapAirspaceStatus status);
        void onAdvisoryStatusLoading();
    }

    public interface OnAdvisoryClickListener {
        void onAdvisoryClicked(AirMapAdvisory advisory);
    }

    public enum MapFailure {
        INACCURATE_DATE_TIME_FAILURE, NETWORK_CONNECTION_FAILURE, UNKNOWN_FAILURE
    }

    public abstract static class Configuration {
        public enum Type {
            AUTOMATIC,
            DYNAMIC,
            MANUAL
        }

        public final Type type;

        public Configuration(Type type) {
            this.type = type;
        }
    }

    public static class AutomaticConfiguration extends Configuration {

        public AutomaticConfiguration() {
            super(Type.AUTOMATIC);
        }
    }

    public static class DynamicConfiguration extends Configuration {

        public final List<String> preferredRulesetIds;
        public final List<String> unpreferredRulesetIds;
        public final boolean enableRecommendedRulesets;

        public DynamicConfiguration(@Nullable List<String> preferredRulesetIds, @Nullable List<String> unpreferredRulesetIds, boolean enableRecommendedRulesets) {
            super(Type.DYNAMIC);

            this.preferredRulesetIds = preferredRulesetIds != null ? preferredRulesetIds : new ArrayList<String>();
            this.unpreferredRulesetIds = unpreferredRulesetIds != null ? unpreferredRulesetIds : new ArrayList<String>();
            this.enableRecommendedRulesets = enableRecommendedRulesets;
        }
    }

    public static class ManualConfiguration extends Configuration {

        public final List<AirMapRuleset> selectedRulesets;

        private ManualConfiguration(List<AirMapRuleset> selectedRulesets) {
            super(Type.MANUAL);

            this.selectedRulesets = selectedRulesets;
        }
    }
}