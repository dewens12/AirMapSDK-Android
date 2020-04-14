package com.airmap.airmapsdktest.activities;

import android.os.Bundle;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.airmap.airmapsdk.models.rules.AirMapRuleset;
import com.airmap.airmapsdk.models.status.AirMapAirspaceStatus;
import com.airmap.airmapsdk.ui.activities.MyLocationMapActivity;
import com.airmap.airmapsdk.ui.views.AirMapMapView;
import com.airmap.airmapsdk.ui.views.SwipeDisableableViewPager;
import com.airmap.airmapsdktest.R;
import com.airmap.airmapsdktest.fragments.AdvisoriesFragment;
import com.airmap.airmapsdktest.fragments.MapFragment;
import com.airmap.airmapsdktest.fragments.RulesetsFragment;
import com.google.android.material.tabs.TabLayout;

import java.util.List;

public class MapDemoActivity extends MyLocationMapActivity {

    private Toolbar toolbar;
    private TabLayout tabLayout;
    private MapPagerAdapter pagerAdapter;
    private SwipeDisableableViewPager viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupViews();
    }

    private void setupViews() {
        toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(getTitle());
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        pagerAdapter = new MapPagerAdapter(getSupportFragmentManager());

        viewPager = findViewById(R.id.view_pager);
        viewPager.setOffscreenPageLimit(3);
        viewPager.setPagingEnabled(false);
        viewPager.setAdapter(pagerAdapter);

        tabLayout = findViewById(R.id.tab_layout);
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.setTabMode(TabLayout.MODE_FIXED);
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
    }

    public void setRulesets(List<AirMapRuleset> availableRulesets, List<AirMapRuleset> selectedRulesets) {
        getRulesetsFragment().setRulesets(availableRulesets, selectedRulesets);
    }

    public void selectRuleset(AirMapRuleset ruleset) {
        getMapFragment().onRulesetSelected(ruleset);
    }

    public void deselectRuleset(AirMapRuleset ruleset) {
        getMapFragment().onRulesetDeselected(ruleset);
    }

    public void switchSelectedRulesets(AirMapRuleset fromRuleset, AirMapRuleset toRuleset) {
        getMapFragment().onRulesetSwitched(fromRuleset, toRuleset);
    }

    public void setAdvisoryStatus(AirMapAirspaceStatus status) {
        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
            if (fragment instanceof AdvisoriesFragment) {
                ((AdvisoriesFragment) fragment).setAdvisoryStatus(status);
            }
        }
    }

    private MapFragment getMapFragment() {
        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
            if (fragment instanceof MapFragment) {
                return (MapFragment) fragment;
            }
        }

        return null;
    }

    private RulesetsFragment getRulesetsFragment() {
        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
            if (fragment instanceof RulesetsFragment) {
                return (RulesetsFragment) fragment;
            }
        }

        return null;
    }

    @Override
    protected AirMapMapView getMapView() {
        if (getMapFragment() != null) {
            return getMapFragment().getMapView();
        }

        return null;
    }

    private class MapPagerAdapter extends FragmentPagerAdapter {

        public MapPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return MapFragment.newInstance();
                case 1:
                    return RulesetsFragment.newInstance();
                case 2:
                    return AdvisoriesFragment.newInstance();
            }

            return null;
        }

        @Override
        public String getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Map";
                case 1:
                    return "Rules";
                case 2:
                    return "Advisories";
            }

            return "";
        }

        @Override
        public int getCount() {
            return 3;
        }
    }
}
