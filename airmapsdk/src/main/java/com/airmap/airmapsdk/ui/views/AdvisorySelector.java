package com.airmap.airmapsdk.ui.views;

import android.graphics.PointF;
import android.graphics.RectF;

import androidx.annotation.Nullable;

import com.airmap.airmapsdk.models.rules.AirMapRuleset;
import com.airmap.airmapsdk.models.status.AirMapAdvisory;
import com.airmap.airmapsdk.models.status.AirMapAirspaceStatus;
import com.airmap.airmapsdk.util.Utils;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.style.layers.Filter;
import com.mapbox.services.commons.geojson.Feature;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import timber.log.Timber;

public class AdvisorySelector {

    private AtomicBoolean isBusy;

    public AdvisorySelector() {
        isBusy = new AtomicBoolean(false);
    }

    public boolean isBusy() {
        return isBusy.get();
    }

    public void selectAdvisoriesAt(final LatLng latLng, final AirMapMapView mapView, final Callback callback) {
        isBusy.set(true);

        PointF clickPoint = mapView.getMap().getProjection().toScreenLocation(latLng);
        int slop = Utils.dpToPixels(mapView.getContext(), 10).intValue();
        RectF clickRect = new RectF(clickPoint.x - slop, clickPoint.y - slop, clickPoint.x + slop, clickPoint.y + slop);
        Filter.Statement filter = Filter.has("id");

        final List<Feature> selectedFeatures = mapView.getMap().queryRenderedFeatures(clickRect, filter);
        if (selectedFeatures.isEmpty()) {
            isBusy.set(false);
            return;
        }

        List<AirMapAdvisory> allAdvisories = mapView.getCurrentAdvisories();

        Feature featureClicked = null;
        AirMapAdvisory advisoryClicked = null;
        Set<AirMapAdvisory> filteredAdvisories = new HashSet<>();

        for (Feature feature : selectedFeatures) {
            if (allAdvisories == null) {
                break;
            }

            for (AirMapAdvisory advisory : allAdvisories) {
                if (advisory.getId().equals(feature.getStringProperty("id"))) {
                    // set as the clicked advisory based on size/importance
                    if (advisoryClicked == null || hasHigherPriority(advisory, advisoryClicked)) {
                        featureClicked = feature;
                        advisoryClicked = advisory;

                    }
                    filteredAdvisories.add(advisory);
                }
            }
        }
        callback.onAdvisorySelected(featureClicked, advisoryClicked, filteredAdvisories);

        // if matching advisory found, we're done
        if (advisoryClicked != null) {
            isBusy.set(false);
            return;
        }

        // no advisory matched the feature, wait for next 3 advisory loads to look for match
        // callback for when advisories are loaded
        Timber.w("Feature clicked doesn't have matching advisory yet");
        mapView.addOnMapDataChangedListener(new AirMapMapView.OnMapDataChangeListener() {
            AtomicInteger count = new AtomicInteger(0);

            @Override
            public void onRulesetsChanged(List<AirMapRuleset> availableRulesets, List<AirMapRuleset> selectedRulesets) {}

            @Override
            public void onAdvisoryStatusChanged(AirMapAirspaceStatus status) {
                if (status == null || status.getAdvisories() == null || status.getAdvisories().isEmpty()) {
                    return;
                }

                Feature featureClicked = null;
                AirMapAdvisory advisoryClicked = null;
                Set<AirMapAdvisory> filteredAdvisories = new HashSet<>();

                for (Feature feature : selectedFeatures) {
                    for (AirMapAdvisory advisory : status.getAdvisories()) {
                        if (advisory.getId().equals(feature.getStringProperty("id"))) {
                            // set as the clicked advisory based on size/importance
                            if (advisoryClicked == null || hasHigherPriority(advisory, advisoryClicked)) {
                                featureClicked = feature;
                                advisoryClicked = advisory;
                            }

                            filteredAdvisories.add(advisory);
                        }
                    }
                }

                if (featureClicked != null) {
                    Timber.w("Matching advisory found for feature");
                    callback.onAdvisorySelected(featureClicked, advisoryClicked, filteredAdvisories);
                    mapView.removeOnMapDataChangedListener(this);
                    isBusy.set(false);
                } else {
                    if (count.get() > 3) {
                        mapView.removeOnMapDataChangedListener(this);
                        isBusy.set(false);
                    }
                }

                count.incrementAndGet();
            }

            @Override
            public void onAdvisoryStatusLoading() {}
        });
    }

    private boolean hasHigherPriority(AirMapAdvisory advisory, AirMapAdvisory selectedAdvisory) {
        switch (selectedAdvisory.getType()) {
            case Emergencies:
            case School:
            case Fires:
            case Prison:
            case Wildfires:
            case PowerPlant:
                return false;
            default:
                return true;
        }
    }

    private boolean hasHigherPriority(Feature feature, Feature selectedFeature) {
        String type = selectedFeature.getStringProperty("category");
        switch (type) {
            case "emergency":
            case "school":
            case "fire":
            case "prison":
            case "wildfire":
            case "power_plant":
                return false;
            default:
                return true;
        }
    }

    public interface Callback {
        void onAdvisorySelected(@Nullable Feature featureClicked, @Nullable AirMapAdvisory advisoryClicked, @Nullable Set<AirMapAdvisory> advisoriesSelected);
    }
}
