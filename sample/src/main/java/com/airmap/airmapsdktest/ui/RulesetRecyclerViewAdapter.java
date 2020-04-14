package com.airmap.airmapsdktest.ui;

import android.content.Context;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.airmap.airmapsdk.Analytics;
import com.airmap.airmapsdk.models.rules.AirMapRuleset;
import com.airmap.airmapsdk.ui.adapters.EmptyableAdapter;
import com.airmap.airmapsdktest.R;
import com.marshalchen.ultimaterecyclerview.gridSection.SectionedRecyclerViewAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

public class RulesetRecyclerViewAdapter extends SectionedRecyclerViewAdapter<RulesetRecyclerViewAdapter.HeaderViewHolder, RulesetRecyclerViewAdapter.ViewHolder, RecyclerView.ViewHolder>
        implements EmptyableAdapter {

    private Context context;
    private RulesetListener listener;

    private List<AirMapRuleset> rulesets;
    private Set<AirMapRuleset> selectedRulesets;
    private LinkedHashMap<Section, List<AirMapRuleset>> sectionRulesetMap;

    public RulesetRecyclerViewAdapter(Context context, RulesetListener listener) {
        this.context = context;
        this.selectedRulesets = new HashSet<>();
        this.listener = listener;
    }

    private void calculateSections() {
        if (rulesets == null) {
            return;
        }

        // sort all rulesets by jurisdiction, type
        Collections.sort(rulesets, new Comparator<AirMapRuleset>() {
            @Override
            public int compare(AirMapRuleset o1, AirMapRuleset o2) {
                if (o1.getJurisdiction().getRegion().intValue() > o2.getJurisdiction().getRegion().intValue()) {
                    return -1;
                } else if (o1.getJurisdiction().getRegion().intValue() < o2.getJurisdiction().getRegion().intValue()) {
                    return 1;
                } else {
                    if (o1.getJurisdiction().getId() > o2.getJurisdiction().getId()) {
                        return 1;
                    } else if (o1.getJurisdiction().getId() < o2.getJurisdiction().getId()) {
                        return -1;
                    } else {
                        if (o1.getType().intValue() > o2.getType().intValue()) {
                            return -1;
                        } else {
                            return 1;
                        }
                    }
                }
            }
        });

        SparseArray<AirMapRuleset.Type> titleSections = new SparseArray<>();
        sectionRulesetMap = new LinkedHashMap<>();

        for (AirMapRuleset ruleset : rulesets) {
            AirMapRuleset.Type firstType = titleSections.get(ruleset.getJurisdiction().getId());
            boolean showName = false;
            if (firstType == null || firstType.intValue() <= ruleset.getType().intValue()) {
                showName = true;
                titleSections.put(ruleset.getJurisdiction().getId(), ruleset.getType());
            }

            Section section = new Section(ruleset.getJurisdiction().getId(), ruleset.getJurisdiction().getName(), ruleset.getType(), showName);
            List<AirMapRuleset> rulesetList = sectionRulesetMap.get(section);
            if (rulesetList == null) {
                rulesetList = new ArrayList<>();
            }
            rulesetList.add(ruleset);
            sectionRulesetMap.put(section, rulesetList);
        }
    }

    @Override
    protected boolean hasFooterInSection(int section) {
        return false;
    }

    @Override
    protected HeaderViewHolder onCreateSectionHeaderViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ruleset_section_header, parent, false);
        return new HeaderViewHolder(view);
    }

    @Override
    protected RecyclerView.ViewHolder onCreateSectionFooterViewHolder(ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    protected ViewHolder onCreateItemViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ruleset, parent, false);
        return new ViewHolder(view);
    }

    @Override
    protected void onBindSectionHeaderViewHolder(HeaderViewHolder holder, int sectionNumber) {
        Section section = null;
        int index = 0;
        for (Section key : sectionRulesetMap.keySet()) {
            if (index == sectionNumber) {
                section = key;
                break;
            }
            index++;
        }

        holder.sectionTitleTextView.setText(section.jurisdictionName);
        holder.sectionTitleTextView.setVisibility(section.showName ? View.VISIBLE : View.GONE);
        holder.sectionSubtitleTextView.setText(section.type.getTitle());
    }

    @Override
    protected void onBindSectionFooterViewHolder(RecyclerView.ViewHolder holder, int section) {
    }

    @Override
    public int getSectionCount() {
        return sectionRulesetMap != null ? sectionRulesetMap.keySet().size() : 0;
    }

    @Override
    public int getItemCountForSection(int sectionIndex) {
        if (sectionRulesetMap == null) {
            return 0;
        }
        int index = 0;
        for (Section section : sectionRulesetMap.keySet()) {
            if (index == sectionIndex) {
                return sectionRulesetMap.get(section).size();
            }
            index++;
        }
        return -1;
    }

    @Override
    protected void onBindItemViewHolder(final ViewHolder holder, int section, int position) {
        final AirMapRuleset ruleset = getItem(section, position);

        boolean selected = selectedRulesets.contains(ruleset);

        holder.nameTextView.setText(ruleset.getName());
        holder.nameTextView.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), selected ? R.color.font_title : R.color.font_description_dark));

        holder.selectorView.setVisibility(selected ? View.VISIBLE : View.INVISIBLE);

        holder.infoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onRulesetInfoPressed(ruleset);

                Analytics.logEvent(Analytics.Event.rules, Analytics.Action.tap, Analytics.Label.RULES_INFO, ruleset.getId());
            }
        });

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // if already selected
                if (selectedRulesets.contains(ruleset)) {
                    // optional rules can be unselected
                    if (ruleset.getType() == AirMapRuleset.Type.Optional) {
                        selectedRulesets.remove(ruleset);
                        notifyDataSetChanged();
                        listener.onRulesetDeselected(ruleset);
                    }

                    if (ruleset.getType() == AirMapRuleset.Type.Optional) {
                        Analytics.logEvent(Analytics.Event.rules, Analytics.Action.deselect, Analytics.Label.OPTIONAL, ruleset.getId());
                    }

                    // not yet selected
                } else {
                    // if pick one is selected, need to deselect its selected sibling(s)
                    if (ruleset.getType() == AirMapRuleset.Type.PickOne) {
                        for (AirMapRuleset selectedRuleset : new ArrayList<>(selectedRulesets)) {

                            if (selectedRuleset.getType() == AirMapRuleset.Type.PickOne
                                    && selectedRuleset.getJurisdiction().getId() == ruleset.getJurisdiction().getId()
                                    && selectedRuleset.getJurisdiction().getRegion() == ruleset.getJurisdiction().getRegion()) {
                                selectedRulesets.remove(selectedRuleset);
                                selectedRulesets.add(ruleset);
                                listener.onRulesetSwitched(selectedRuleset, ruleset);
                                break;
                            }
                        }
                    } else {
                        selectedRulesets.add(ruleset);
                        listener.onRulesetSelected(ruleset);
                    }
                    notifyDataSetChanged();

                    if (ruleset.getType() == AirMapRuleset.Type.Optional) {
                        Analytics.logEvent(Analytics.Event.rules, Analytics.Action.select, Analytics.Label.OPTIONAL, ruleset.getId());
                    } else if (ruleset.getType() == AirMapRuleset.Type.PickOne) {
                        Analytics.logEvent(Analytics.Event.rules, Analytics.Action.select, ruleset.getId());
                    }
                }
            }
        });
    }

    public AirMapRuleset getItem(int sectionIndex, int position) {
        int index = 0;
        for (Section section : sectionRulesetMap.keySet()) {
            if (index == sectionIndex) {
                return sectionRulesetMap.get(section).get(position);
            }
            index++;
        }

        return null;
    }

    public List<AirMapRuleset> getData() {
        return rulesets;
    }

    public void setData(List<AirMapRuleset> rulesets, List<AirMapRuleset> selectedRulesets) {
        this.rulesets = rulesets;
        this.selectedRulesets = new HashSet<>(selectedRulesets);

        calculateSections();
        notifyDataSetChanged();
    }

    public void setData(List<AirMapRuleset> rulesets) {
        this.rulesets = rulesets;

        calculateSections();
        notifyDataSetChanged();
    }

    public void setSelectedRulesets(Set<AirMapRuleset> selectedRulesets) {
        this.selectedRulesets = selectedRulesets;
        notifyDataSetChanged();
    }

    public Set<AirMapRuleset> getSelectedRulesets() {
        return selectedRulesets;
    }

    public void unselectRuleset(AirMapRuleset ruleset) {
        if (selectedRulesets.contains(ruleset)) {
            selectedRulesets.remove(ruleset);
            notifyDataSetChanged();
        }
    }

    public int[] indexesOfSection(AirMapRuleset ruleset) {
        int sectionTop = 0;
        int indexOfItem = 0;
        int sectionBottom = 0;

        Section section = new Section(ruleset.getJurisdiction().getId(), ruleset.getJurisdiction().getName(), ruleset.getType(), false);
        int currentSection = 0;

        for (Section s : sectionRulesetMap.keySet()) {
            if (s.equals(section)) {
                int sectionCount = getItemCountForSection(currentSection);
                indexOfItem += sectionRulesetMap.get(s).indexOf(ruleset) + 1;
                sectionBottom += sectionCount;
                break;
            } else {
                int sectionCount = getItemCountForSection(currentSection) + 1;
                sectionTop += sectionCount;
                indexOfItem += sectionCount;
                sectionBottom += sectionCount;
            }
            currentSection++;
        }
        return new int[]{sectionTop, indexOfItem, sectionBottom};
    }

    @Override
    public String getEmptyText() {
        return context.getString(R.string.no_rulesets);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        View selectorView;
        TextView nameTextView;
        ImageButton infoButton;

        public ViewHolder(View itemView) {
            super(itemView);

            selectorView = itemView.findViewById(R.id.selector_view);
            nameTextView = itemView.findViewById(R.id.rule_name_text_view);
            infoButton = itemView.findViewById(R.id.info_button);
        }
    }

    class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView sectionTitleTextView;
        TextView sectionSubtitleTextView;

        HeaderViewHolder(View itemView) {
            super(itemView);

            sectionTitleTextView = itemView.findViewById(R.id.section_title_text_view);
            sectionSubtitleTextView = itemView.findViewById(R.id.section_subtitle_text_view);
        }
    }

    private class Section {
        private final int jurisdiction;
        private final String jurisdictionName;
        private final AirMapRuleset.Type type;
        private final boolean showName;

        public Section(int jurisdiction, String jurisdictionName, AirMapRuleset.Type type, boolean showName) {
            this.jurisdiction = jurisdiction;
            this.jurisdictionName = jurisdictionName;
            this.type = type;
            this.showName = showName;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Section && ((Section) o).jurisdiction == (this.jurisdiction) && ((Section) o).type == this.type;
        }

        @Override
        public int hashCode() {
            int result = 80;
            result = 37 * result + jurisdiction;
            result = 37 * result + jurisdictionName.hashCode();
            result = 37 * result + type.intValue();
            return result;
        }
    }


    public interface RulesetListener {
        void onRulesetSelected(AirMapRuleset ruleset);

        void onRulesetDeselected(AirMapRuleset ruleset);

        void onRulesetSwitched(AirMapRuleset fromRuleset, AirMapRuleset toRuleset);

        void onRulesetInfoPressed(AirMapRuleset ruleset);
    }
}
