package com.airmap.airmapsdktest.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airmap.airmapsdk.models.rules.AirMapRuleset;
import com.airmap.airmapsdktest.R;
import com.airmap.airmapsdktest.activities.MapDemoActivity;
import com.airmap.airmapsdktest.ui.RulesetRecyclerViewAdapter;

import java.util.List;

import timber.log.Timber;

public class RulesetsFragment extends Fragment {

    private RecyclerView rulesetsRecyclerView;
    private RulesetRecyclerViewAdapter rulesetsRecyclerAdapter;
    private View loadingView;

    public static RulesetsFragment newInstance() {
        return new RulesetsFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_rulesets, container, false);

        loadingView = view.findViewById(R.id.loading_view);

        rulesetsRecyclerView = view.findViewById(R.id.rulesets_recycler_view);
        rulesetsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        rulesetsRecyclerAdapter = new RulesetRecyclerViewAdapter(getActivity(), new RulesetRecyclerViewAdapter.RulesetListener() {
            @Override
            public void onRulesetSelected(AirMapRuleset ruleset) {
                Timber.i("Ruleset selected: %s", ruleset.getShortName());
                if ((getActivity()) != null) {
                    ((MapDemoActivity) getActivity()).selectRuleset(ruleset);
                }
            }

            @Override
            public void onRulesetDeselected(AirMapRuleset ruleset) {
                Timber.i("Ruleset deselected: %s", ruleset.getShortName());
                ((MapDemoActivity) getActivity()).deselectRuleset(ruleset);
            }

            @Override
            public void onRulesetInfoPressed(AirMapRuleset ruleset) {
                Timber.i("Ruleset info pressed: %s", ruleset.getShortName());
            }

            @Override
            public void onRulesetSwitched(AirMapRuleset fromRuleset, AirMapRuleset toRuleset) {
                Timber.i("Ruleset switched from: %s to: %s", fromRuleset.getShortName(), toRuleset.getShortName());
                if ((getActivity()) != null) {
                    ((MapDemoActivity) getActivity()).switchSelectedRulesets(fromRuleset, toRuleset);
                }
            }
        });
        rulesetsRecyclerView.setAdapter(rulesetsRecyclerAdapter);

        return view;
    }


    public void setRulesets(List<AirMapRuleset> availableRulesets, List<AirMapRuleset> selectedRulesets) {
        rulesetsRecyclerAdapter.setData(availableRulesets, selectedRulesets);
        loadingView.setVisibility(View.GONE);
    }

}
