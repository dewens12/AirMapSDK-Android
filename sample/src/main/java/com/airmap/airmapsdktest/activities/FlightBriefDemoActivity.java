package com.airmap.airmapsdktest.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;

import com.airmap.airmapsdk.AirMapException;
import com.airmap.airmapsdk.models.Coordinate;
import com.airmap.airmapsdk.models.flight.AirMapFlightBriefing;
import com.airmap.airmapsdk.models.flight.AirMapFlightPlan;
import com.airmap.airmapsdk.models.rules.AirMapRule;
import com.airmap.airmapsdk.models.shapes.AirMapGeometry;
import com.airmap.airmapsdk.models.shapes.AirMapPolygon;
import com.airmap.airmapsdk.networking.callbacks.AirMapCallback;
import com.airmap.airmapsdk.networking.services.AirMap;
import com.airmap.airmapsdk.ui.adapters.ExpandableRulesAdapter;
import com.airmap.airmapsdk.util.AirMapConstants;
import com.airmap.airmapsdk.util.BriefingEvaluator;
import com.airmap.airmapsdktest.R;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;

import timber.log.Timber;

public class FlightBriefDemoActivity extends BaseActivity {

    private Toolbar toolbar;
    private RecyclerView rulesRecyclerView;
    private View loadingView;

    private String flightPlanId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flight_brief);

        toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(getTitle());
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        rulesRecyclerView = findViewById(R.id.rules_recycler_view);
        rulesRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        loadingView = findViewById(R.id.loading_view);

        flightPlanId = getIntent().getStringExtra(AirMapConstants.FLIGHT_PLAN_ID_EXTRA);

        // no flight plan, create one
        if (TextUtils.isEmpty(flightPlanId)) {
            AirMapFlightPlan flightPlan = getSampleFlightPlan();
            AirMap.createFlightPlan(flightPlan, new AirMapCallback<AirMapFlightPlan>() {
                @Override
                protected void onSuccess(AirMapFlightPlan response) {
                    if (!isActive()) {
                        return;
                    }

                    flightPlanId = response.getPlanId();
                    loadBrief();

                    invalidateOptionsMenu();
                }

                @Override
                protected void onError(AirMapException e) {
                    Timber.e(e, "Flight plan creation failed: %s", e.getDetailedMessage());
                    if (isActive()) {
                        showErrorDialog("An error occurred while creating your flight plan or retrieving your flight brief.");
                    }
                }
            });
        } else {
            loadBrief();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // only show Fly option if user has flight plan
        if (!TextUtils.isEmpty(flightPlanId)) {
            getMenuInflater().inflate(R.menu.brief, menu);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == R.id.fly) {
            AirMap.submitFlightPlan(flightPlanId, new AirMapCallback<AirMapFlightPlan>() {
                @Override
                protected void onSuccess(AirMapFlightPlan response) {
                    Timber.i("Successfully created flight with flight ID %s from flight plan", response.getFlightId());
                    if (isActive()) {
                        Toast.makeText(FlightBriefDemoActivity.this, "Flight created!", Toast.LENGTH_SHORT).show();
                    }

                }

                @Override
                protected void onError(AirMapException e) {
                    Timber.e(e, "Error submitting flight plan: %s", e.getDetailedMessage());
                    if (isActive()) {
                        Toast.makeText(FlightBriefDemoActivity.this, "Failed to create flight", Toast.LENGTH_SHORT).show();
                    }

                }
            });
        }

        return super.onOptionsItemSelected(menuItem);
    }

    private void loadBrief() {
        AirMap.getFlightBrief(flightPlanId, new AirMapCallback<AirMapFlightBriefing>() {
            @Override
            protected void onSuccess(AirMapFlightBriefing briefing) {
                loadingView.setVisibility(View.GONE);

                // calculate which rules are being violated, followed, etc
                LinkedHashMap<AirMapRule.Status, List<AirMapRule>> sortedRulesMap = BriefingEvaluator.computeRulesViolations(briefing);

                ExpandableRulesAdapter rulesRecyclerAdapter = new BriefingAdapter(sortedRulesMap);
                rulesRecyclerView.setAdapter(rulesRecyclerAdapter);
                rulesRecyclerAdapter.expandAll();
            }

            @Override
            protected void onError(AirMapException e) {
                Timber.e(e, "Erring getting flight brief: %s", e.getDetailedMessage());
                if (isActive()) {
                    showErrorDialog("An error occurred while creating your flight plan or retrieving your flight brief.");
                }

            }
        });
    }

    private AirMapFlightPlan getSampleFlightPlan() {
        // create polygon from coordinates
        List<Coordinate> coordinates = new ArrayList<>();
        coordinates.add(new Coordinate(34.02440874647921, -117.49167761708696));
        coordinates.add(new Coordinate(34.020040687842254, -117.4968401460024));
        coordinates.add(new Coordinate(34.01648293903452, -117.4923151205652));
        coordinates.add(new Coordinate(34.02080536486173, -117.48725884231055));
        coordinates.add(new Coordinate(34.02440874647921, -117.49167761708696));
        AirMapPolygon polygon = new AirMapPolygon();
        polygon.setCoordinates(coordinates);
        JSONObject geometryJSON = AirMapGeometry.getGeoJSONFromGeometry(polygon);

        AirMapFlightPlan flightPlan = new AirMapFlightPlan();
        flightPlan.setPilotId(AirMap.getUserId());
        flightPlan.setGeometry(geometryJSON.toString());
        flightPlan.setBuffer(0);
        flightPlan.setTakeoffCoordinate(coordinates.get(0));

        String[] rulesetIds = {"usa_national_marine_sanctuary", "usa_ama", "usa_sec_91", "usa_national_park", "usa_airmap_rules", "usa_sec_336"};
        flightPlan.setRulesetIds(Arrays.asList(rulesetIds));

        flightPlan.setPublic(true);

        // default max alt - 100m
        flightPlan.setMaxAltitude(100);

        // default start & end time - now to 4 hours from now
        long duration = 4 * 60 * 60 * 1000;
        flightPlan.setDurationInMillis(duration);
        flightPlan.setStartsAt(new Date());
        flightPlan.setEndsAt(new Date(System.currentTimeMillis() + duration));
        return flightPlan;
    }

    /**
     * You can customize the UI of the briefing rows by overriding onCreateViewHolder
     */
    private class BriefingAdapter extends ExpandableRulesAdapter {

        BriefingAdapter(LinkedHashMap<AirMapRule.Status, List<AirMapRule>> rulesMap) {
            super(rulesMap);
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            switch (viewType) {
                case PARENT_VIEW_TYPE:
                    view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_rule_section, parent, false);
                    return new SectionViewHolder(view);
                case CHILD_VIEW_TYPE:
                    view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_rule, parent, false);
                    return new RuleViewHolder(view);
            }
            return super.onCreateViewHolder(parent, viewType);
        }
    }
}
