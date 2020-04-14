package com.airmap.airmapsdktest.activities;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;

import com.airmap.airmapsdk.AirMapException;
import com.airmap.airmapsdk.controllers.RulesetsEvaluator;
import com.airmap.airmapsdk.models.Coordinate;
import com.airmap.airmapsdk.models.flight.AirMapFlightFeature;
import com.airmap.airmapsdk.models.flight.AirMapFlightPlan;
import com.airmap.airmapsdk.models.rules.AirMapRule;
import com.airmap.airmapsdk.models.rules.AirMapRuleset;
import com.airmap.airmapsdk.models.shapes.AirMapGeometry;
import com.airmap.airmapsdk.models.shapes.AirMapPolygon;
import com.airmap.airmapsdk.networking.callbacks.AirMapCallback;
import com.airmap.airmapsdk.networking.services.AirMap;
import com.airmap.airmapsdk.ui.views.AirMapMapView;
import com.airmap.airmapsdk.util.AirMapConstants;
import com.airmap.airmapsdktest.R;
import com.airmap.airmapsdktest.ui.FlightPlanDetailsAdapter;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class FlightPlanDemoActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TextView startTimeTextView;
    private TextView endTimeTextView;
    private TextView altitudeTextView;
    private SeekBar altitudeSeekBar;
    private RecyclerView flightFeaturesRecyclerView;
    private View loadingView;

    private AirMapFlightPlan flightPlan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flight_plan);

        toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(getTitle());
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        startTimeTextView = findViewById(R.id.start_time_value);
        endTimeTextView = findViewById(R.id.end_time_value);
        altitudeTextView = findViewById(R.id.altitude_value);
        altitudeSeekBar = findViewById(R.id.altitude_seekbar);
        altitudeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int altitude = 10 + 10 * progress;
                altitudeTextView.setText(altitude + " meters");
                flightPlan.setMaxAltitude(altitude);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        flightFeaturesRecyclerView = findViewById(R.id.flight_features_recycler_view);
        loadingView = findViewById(R.id.loading_view);

        // create polygon from coordinates
        List<Coordinate> coordinates = new ArrayList<>();
        coordinates.add(new Coordinate(34.02440874647921, -117.49167761708696));
        coordinates.add(new Coordinate(34.020040687842254, -117.4968401460024));
        coordinates.add(new Coordinate(34.01648293903452, -117.4923151205652));
        coordinates.add(new Coordinate(34.02080536486173, -117.48725884231055));
        coordinates.add(new Coordinate(34.02440874647921, -117.49167761708696));

        AirMapPolygon polygon = new AirMapPolygon();
        polygon.setCoordinates(coordinates);

        final JSONObject geometryJSON = AirMapGeometry.getGeoJSONFromGeometry(polygon);
        /* or use raw json
            {
                "type": "Polygon",
                "coordinates": [
                    [
                        [-118.49167761708696, 34.02440874647921],
                        [-118.4968401460024, 34.020040687842254],
                        [-118.4923151205652, 34.01648293903452],
                        [-118.48725884231055, 34.02080536486173],
                        [-118.49167761708696, 34.02440874647921]
                    ]
                ]
            }
         */

        // get rulesets (from jurisdictions) from geometry
        AirMap.getRulesets(geometryJSON, new AirMapCallback<List<AirMapRuleset>>() {
            @Override
            protected void onSuccess(List<AirMapRuleset> availableRulesets) {
                if (!availableRulesets.isEmpty()) {
                    // Calculate selected rulesets based off preferred & unpreferred rulesets
                    // If no preferred/unpreferred, defaults are selected
                    List<AirMapRuleset> selectedRulesets = RulesetsEvaluator.computeSelectedRulesets(availableRulesets, new AirMapMapView.AutomaticConfiguration());

                    createFlightPlan(geometryJSON, 100, null, selectedRulesets);

                    loadingView.setVisibility(View.GONE);
                }
            }

            @Override
            protected void onError(AirMapException e) {
                Timber.e(e, "Error getting rulesets from geometry %s : %s", geometryJSON, e.getDetailedMessage());
            }
        });
    }

    public void createFlightPlan(JSONObject geometry, float buffer, Coordinate takeoff, List<AirMapRuleset> selectedRulesets) {
        final List<String> selectedRulesetIds = new ArrayList<>();
        final List<AirMapFlightFeature> flightFeatures = new ArrayList<>();

        Map<AirMapFlightFeature, List<AirMapRule>> featuresMap = new HashMap<>();
        for (AirMapRuleset ruleset : selectedRulesets) {
            selectedRulesetIds.add(ruleset.getId());

            if (ruleset.getRules() == null) {
                continue;
            }

            for (AirMapRule rule : ruleset.getRules()) {
                if (rule.getFlightFeatures() != null && !rule.getFlightFeatures().isEmpty()) {
                    flightFeatures.addAll(rule.getFlightFeatures());

                    for (AirMapFlightFeature flightFeature : rule.getFlightFeatures()) {
                        List<AirMapRule> rules = new ArrayList<>();
                        if (featuresMap.containsKey(flightFeature)) {
                            rules = featuresMap.get(flightFeature);
                        }
                        rules.add(rule);

                        featuresMap.put(flightFeature, rules);
                    }
                }
            }
        }

        // sets required params
        flightPlan = new AirMapFlightPlan();
        flightPlan.setPilotId(AirMap.getUserId());
        flightPlan.setGeometry(geometry.toString());
        flightPlan.setBuffer(buffer);
        flightPlan.setTakeoffCoordinate(takeoff);
        flightPlan.setRulesetIds(selectedRulesetIds);

        flightPlan.setPublic(true);

        // default max alt - 100m
        flightPlan.setMaxAltitude(100);
        altitudeTextView.setText("100m");

        // default start & end time - now to 4 hours from now
        long duration = 4 * 60 * 60 * 1000;
        flightPlan.setDurationInMillis(duration);
        flightPlan.setStartsAt(new Date());
        flightPlan.setEndsAt(new Date(System.currentTimeMillis() + duration));

        SimpleDateFormat sdf = new SimpleDateFormat("h:mm a");
        startTimeTextView.setText(sdf.format(flightPlan.getStartsAt()));
        endTimeTextView.setText(sdf.format(flightPlan.getEndsAt()));

        FlightPlanDetailsAdapter detailsAdapter = new FlightPlanDetailsAdapter(this, flightPlan, featuresMap, null, new FlightPlanDetailsAdapter.FlightPlanChangeListener() {
            @Override
            public void onFlightPlanChanged() {
            }

            @Override
            public void onFlightFeatureRemoved(String flightFeature) {
            }

            @Override
            public void onFlightPlanSave() {
                saveFlightPlan();
            }
        });
        flightFeaturesRecyclerView.setAdapter(detailsAdapter);
    }

    private void saveFlightPlan() {
        AirMapCallback callback = new AirMapCallback<AirMapFlightPlan>() {
            @Override
            protected void onSuccess(AirMapFlightPlan response) {
                Toast.makeText(FlightPlanDemoActivity.this, "Flight plan successfully saved", Toast.LENGTH_SHORT).show();

                PreferenceManager.getDefaultSharedPreferences(FlightPlanDemoActivity.this)
                        .edit()
                        .putString(AirMapConstants.FLIGHT_PLAN_ID_EXTRA, response.getPlanId())
                        .apply();
            }

            @Override
            protected void onError(AirMapException e) {
                Toast.makeText(FlightPlanDemoActivity.this, "Flight plan failed to save", Toast.LENGTH_SHORT).show();
            }
        };

        if (TextUtils.isEmpty(flightPlan.getFlightId())) {
            AirMap.createFlightPlan(flightPlan, callback);
        } else {
            AirMap.patchFlightPlan(flightPlan, callback);
        }
    }
}
