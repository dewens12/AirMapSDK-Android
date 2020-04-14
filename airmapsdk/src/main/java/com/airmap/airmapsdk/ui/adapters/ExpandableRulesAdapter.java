package com.airmap.airmapsdk.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.RecyclerView;

import com.airmap.airmapsdk.Analytics;
import com.airmap.airmapsdk.R;
import com.airmap.airmapsdk.models.rules.AirMapRule;

import java.util.LinkedHashMap;
import java.util.List;

public class ExpandableRulesAdapter extends ExpandableRecyclerAdapter<AirMapRule.Status, AirMapRule> {

    public ExpandableRulesAdapter(LinkedHashMap<AirMapRule.Status, List<AirMapRule>> rulesMap) {
        super(rulesMap);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        switch (viewType) {
            case PARENT_VIEW_TYPE:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_brief_section, parent, false);
                return new SectionViewHolder(view);
            case CHILD_VIEW_TYPE:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_brief_rule, parent, false);
                return new RuleViewHolder(view);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);

        switch (getItemViewType(position)) {
            case PARENT_VIEW_TYPE: {
                final AirMapRule.Status status = (AirMapRule.Status) getItem(position);
                SectionViewHolder sectionViewHolder = (SectionViewHolder) holder;
                sectionViewHolder.descriptionTextView.setText(getStatusString(status));
                sectionViewHolder.statusImageView.setImageResource(getStatusIcon(status));
                sectionViewHolder.expandImageView.setImageResource(isExpanded(status) ? R.drawable.ic_drop_down_up : R.drawable.ic_drop_down);
                sectionViewHolder.status = status;
                break;
            }
            case CHILD_VIEW_TYPE: {
                final AirMapRule rule = (AirMapRule) getItem(position);
                RuleViewHolder ruleViewHolder = (RuleViewHolder) holder;
                ruleViewHolder.descriptionTextView.setText(rule.getShortText());

                break;
            }
        }
    }

    @Override
    protected void toggleExpandingViewHolder(final RecyclerView.ViewHolder holder, final boolean expanded) {
        ((SectionViewHolder) holder).expandImageView.setImageResource(expanded ? R.drawable.ic_drop_down_up : R.drawable.ic_drop_down);

        if (expanded) {
            String label = "";
            switch (((SectionViewHolder) holder).status) {
                case Conflicting:
                    label = Analytics.Label.CONFLICTING;
                    break;
                case NotConflicting:
                    label = Analytics.Label.NON_CONFLICTING;
                    break;
                case MissingInfo:
                    label = Analytics.Label.NEEDS_MORE_INFO;
                    break;
                case InformationRules:
                    label = Analytics.Label.INFORMATIONAL;
                    break;
            }
            Analytics.logEvent(Analytics.Event.flightPlanBrief, Analytics.Action.expand, label);
        }
    }

    @StringRes
    private int getStatusString(AirMapRule.Status status) {
        switch (status) {
            case Conflicting:
                return R.string.conflicting_rules;
            case MissingInfo:
                return R.string.missing_info_rules;
            case InformationRules:
                return R.string.informational_rules;
            case NotConflicting:
                return R.string.not_conflicting_rules;
        }
        return -1;
    }

    @DrawableRes
    private int getStatusIcon(AirMapRule.Status status) {
        switch (status) {
            case Conflicting:
                return R.drawable.ic_restricted;
            case MissingInfo:
                return R.drawable.ic_asterisk;
            case InformationRules:
                return R.drawable.ic_asterisk_yellow;
            case NotConflicting:
                return R.drawable.ic_checkmark;
        }
        return R.drawable.ic_asterisk_yellow;
    }

    protected class SectionViewHolder extends RecyclerView.ViewHolder {

        TextView descriptionTextView;
        ImageView statusImageView;
        ImageView expandImageView;
        AirMapRule.Status status;

        public SectionViewHolder(View itemView) {
            super(itemView);

            descriptionTextView = itemView.findViewById(R.id.description_text_view);
            statusImageView = itemView.findViewById(R.id.status_badge_image_view);
            expandImageView = itemView.findViewById(R.id.expand_image_view);
        }
    }

    protected class RuleViewHolder extends RecyclerView.ViewHolder {
        TextView descriptionTextView;

        public RuleViewHolder(View itemView) {
            super(itemView);

            descriptionTextView = itemView.findViewById(R.id.description_text_view);
        }
    }
}
