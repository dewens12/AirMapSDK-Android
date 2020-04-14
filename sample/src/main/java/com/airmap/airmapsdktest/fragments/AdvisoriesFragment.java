package com.airmap.airmapsdktest.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airmap.airmapsdk.AirMapException;
import com.airmap.airmapsdk.models.shapes.AirMapPolygon;
import com.airmap.airmapsdk.models.status.AirMapAdvisory;
import com.airmap.airmapsdk.models.status.AirMapAirspaceStatus;
import com.airmap.airmapsdk.networking.callbacks.AirMapCallback;
import com.airmap.airmapsdk.networking.services.AirMap;
import com.airmap.airmapsdk.networking.services.MappingService;
import com.airmap.airmapsdk.ui.adapters.ExpandableAdvisoriesAdapter;
import com.airmap.airmapsdktest.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;

import timber.log.Timber;

public class AdvisoriesFragment extends Fragment {

    private RecyclerView advisoriesRecyclerView;
    private ExpandableAdvisoriesAdapter advisoriesAdapter;
    private View loadingView;

    public static AdvisoriesFragment newInstance() {
        return new AdvisoriesFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_advisories, container, false);

        advisoriesRecyclerView = view.findViewById(R.id.advisories_recycler_view);
        advisoriesRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));

        loadingView = view.findViewById(R.id.loading_view);

        return view;
    }

    private void loadAdvisories(List<String> rulesets, AirMapPolygon polygon) {
        AirMap.getAirspaceStatus(polygon, rulesets, new AirMapCallback<AirMapAirspaceStatus>() {
            @Override
            public void onSuccess(AirMapAirspaceStatus advisoryStatus) {
                // if the activity has been destroy, ignore response
                if (!isFragmentActive()) {
                    return;
                }

                if (advisoryStatus != null) {
                    Collections.sort(advisoryStatus.getAdvisories(), new Comparator<AirMapAdvisory>() {
                        @Override
                        public int compare(AirMapAdvisory o1, AirMapAdvisory o2) {
                            if (o1.getColor().intValue() > o2.getColor().intValue()) {
                                return -1;
                            } else if (o2.getColor().intValue() > o1.getColor().intValue()) {
                                return 1;
                            }

                            return 0;
                        }
                    });

                    // put into a linked hash map for the adapter
                    LinkedHashMap<MappingService.AirMapAirspaceType, List<AirMapAdvisory>> advisoryMap = new LinkedHashMap<>();
                    for (AirMapAdvisory advisory : advisoryStatus.getAdvisories()) {
                        List<AirMapAdvisory> advisories = advisoryMap.get(advisory.getType());
                        if (advisories == null) {
                            advisories = new ArrayList<>();
                        }
                        advisories.add(advisory);
                        advisoryMap.put(advisory.getType(), advisories);
                    }

                    // ExpandableAdvisoriesAdapter shows the advisories grouped by category
                    advisoriesAdapter = new ExpandableAdvisoriesAdapter(advisoryMap);
                } else {
                    advisoriesAdapter = new ExpandableAdvisoriesAdapter(new LinkedHashMap<MappingService.AirMapAirspaceType, List<AirMapAdvisory>>());
                }

                advisoriesRecyclerView.setAdapter(advisoriesAdapter);
                loadingView.setVisibility(View.GONE);
            }

            @Override
            public void onError(AirMapException e) {
                Timber.e(e, "Getting advisories failed");

                //TODO: show error
            }
        });
    }

    public void setAdvisoryStatus(AirMapAirspaceStatus advisoryStatus) {
        if (advisoryStatus != null) {
            Collections.sort(advisoryStatus.getAdvisories(), new Comparator<AirMapAdvisory>() {
                @Override
                public int compare(AirMapAdvisory o1, AirMapAdvisory o2) {
                    if (o1.getColor().intValue() > o2.getColor().intValue()) {
                        return -1;
                    } else if (o2.getColor().intValue() > o1.getColor().intValue()) {
                        return 1;
                    }
                    return 0;
                }
            });

            // put into a linked hash map for the adapter
            LinkedHashMap<MappingService.AirMapAirspaceType, List<AirMapAdvisory>> advisoryMap = new LinkedHashMap<>();
            for (AirMapAdvisory advisory : advisoryStatus.getAdvisories()) {
                List<AirMapAdvisory> advisories = advisoryMap.get(advisory.getType());
                if (advisories == null) {
                    advisories = new ArrayList<>();
                }
                advisories.add(advisory);
                advisoryMap.put(advisory.getType(), advisories);
            }

            // ExpandableAdvisoriesAdapter shows the advisories grouped by category
            advisoriesAdapter = new ExpandableAdvisoriesAdapter(advisoryMap);
        } else {
            advisoriesAdapter = new ExpandableAdvisoriesAdapter(new LinkedHashMap<MappingService.AirMapAirspaceType, List<AirMapAdvisory>>());
        }

        advisoriesRecyclerView.setAdapter(advisoriesAdapter);
        loadingView.setVisibility(View.GONE);
    }

    protected boolean isFragmentActive() {
        return getActivity() != null && !getActivity().isFinishing() && !getActivity().isDestroyed() && isResumed() && !isDetached();
    }
}
