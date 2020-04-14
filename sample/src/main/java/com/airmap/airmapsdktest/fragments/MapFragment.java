package com.airmap.airmapsdktest.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.airmap.airmapsdk.models.rules.AirMapRuleset;
import com.airmap.airmapsdk.models.status.AirMapAirspaceStatus;
import com.airmap.airmapsdk.ui.activities.MyLocationMapActivity;
import com.airmap.airmapsdk.ui.views.AirMapMapView;
import com.airmap.airmapsdktest.R;
import com.airmap.airmapsdktest.activities.MapDemoActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class MapFragment extends Fragment implements AirMapMapView.OnMapDataChangeListener {

    private AirMapMapView mapView;                  // mapbox MapView wrapper
    private FloatingActionButton myLocationFab;       // FAB to view/change selected rulesets

    private AirMapMapView.DynamicConfiguration configuration;

    public static MapFragment newInstance() {
        return new MapFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        mapView = view.findViewById(R.id.map_view);
        mapView.onCreate(savedInstanceState);
        mapView.addOnMapDataChangedListener(this);


        myLocationFab = view.findViewById(R.id.my_location_fab);
        myLocationFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MyLocationMapActivity) getActivity()).goToLastLocation(true);
            }
        });

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ((MapDemoActivity) getActivity()).setMapView(mapView);
        mapView.getMapAsync(null);

        configuration = new AirMapMapView.DynamicConfiguration(null, null, true);
        mapView.configure(configuration);
    }

    @Override
    public void onRulesetsChanged(List<AirMapRuleset> availableRulesets, List<AirMapRuleset> selectedRulesets) {
        ((MapDemoActivity) getActivity()).setRulesets(availableRulesets, selectedRulesets);
    }

    @Override
    public void onAdvisoryStatusChanged(AirMapAirspaceStatus status) {
        ((MapDemoActivity) getActivity()).setAdvisoryStatus(status);
    }

    @Override
    public void onAdvisoryStatusLoading() {
    }

    public void onRulesetSelected(AirMapRuleset ruleset) {
        configuration.preferredRulesetIds.add(ruleset.getId());
        configuration.unpreferredRulesetIds.remove(ruleset.getId());
        mapView.configure(configuration);
    }

    public void onRulesetDeselected(AirMapRuleset ruleset) {
        configuration.preferredRulesetIds.remove(ruleset.getId());
        configuration.unpreferredRulesetIds.add(ruleset.getId());
        mapView.configure(configuration);
    }

    public void onRulesetSwitched(AirMapRuleset fromRuleset, AirMapRuleset toRuleset) {
        configuration.preferredRulesetIds.remove(fromRuleset.getId());
        configuration.preferredRulesetIds.add(toRuleset.getId());
        mapView.configure(configuration);
    }

    public AirMapMapView getMapView() {
        return mapView;
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
    public void onDestroyView() {
        super.onDestroyView();
        mapView.onDestroy();

        mapView.removeOnMapDataChangedListener(this);
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