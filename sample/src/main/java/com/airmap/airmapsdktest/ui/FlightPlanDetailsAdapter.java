package com.airmap.airmapsdktest.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.airmap.airmapsdk.Analytics;
import com.airmap.airmapsdk.models.AirMapBaseModel;
import com.airmap.airmapsdk.models.flight.AirMapFlightFeature;
import com.airmap.airmapsdk.models.flight.AirMapFlightPlan;
import com.airmap.airmapsdk.models.flight.FlightFeatureConfiguration;
import com.airmap.airmapsdk.models.flight.FlightFeatureValue;
import com.airmap.airmapsdk.models.rules.AirMapRule;
import com.airmap.airmapsdk.ui.views.ToggleButton;
import com.airmap.airmapsdktest.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FlightPlanDetailsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int BINARY_VIEW_TYPE = 2;
    private static final int FIELD_VIEW_TYPE = 4;
    private static final int TEXT_VIEW_TYPE = 5;
    private static final int SAVE_BUTTON_TYPE = 6;

    private Context context;
    private AirMapFlightPlan flightPlan;
    private Map<AirMapFlightFeature, List<AirMapRule>> flightFeaturesMap;
    private List<AirMapFlightFeature> flightFeatures;
    private List<AirMapFlightFeature> duplicateFlightFeatures;
    private Map<String, FlightFeatureConfiguration> flightFeaturesConfigMap;
    private FlightPlanChangeListener flightPlanChangeListener;

    public FlightPlanDetailsAdapter(Context context, AirMapFlightPlan flightPlan, Map<AirMapFlightFeature, List<AirMapRule>> flightFeaturesMap,
                                    Map<String, FlightFeatureConfiguration> flightFeaturesConfigMap, FlightPlanChangeListener flightPlanChangeListener) {
        this.context = context;
        this.flightPlan = flightPlan;
        this.flightFeaturesMap = flightFeaturesMap;
        this.flightFeaturesConfigMap = flightFeaturesConfigMap;
        this.flightPlanChangeListener = flightPlanChangeListener;
        this.flightFeatures = new ArrayList<>(flightFeaturesMap.keySet());

        removeDuplicateFlightFeatures();
    }

    private void removeDuplicateFlightFeatures() {
        duplicateFlightFeatures = new ArrayList<>();

        for (AirMapFlightFeature flightFeature : new ArrayList<>(flightFeaturesMap.keySet())) {
            // don't displaying altitude flight features for now
            if (flightFeature.isAltitudeFeature()) {
                flightFeaturesMap.remove(flightFeature);
                flightFeatures.remove(flightFeature);
                duplicateFlightFeatures.add(flightFeature);
            }
        }

        Collections.sort(flightFeatures, new Comparator<AirMapFlightFeature>() {
            @Override
            public int compare(AirMapFlightFeature o1, AirMapFlightFeature o2) {
                if (o1.getInputType() != o2.getInputType()) {
                    return o1.getInputType().value() - o2.getInputType().value();
                } else {
                    return o1.getFlightFeature().compareTo(o2.getFlightFeature());
                }
            }
        });
    }

    private void onFlightPlanChanged() {
        flightPlanChangeListener.onFlightPlanChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        switch (viewType) {
            case BINARY_VIEW_TYPE:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_flight_plan_feature_binary, parent, false);
                return new FlightFeatureBinaryViewHolder(view);
            case FIELD_VIEW_TYPE:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_flight_plan_feature_field, parent, false);
                return new FlightFeatureFieldViewHolder(view);
            case TEXT_VIEW_TYPE:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_flight_plan_feature_text, parent, false);
                return new FlightFeatureTextViewHolder(view);
            case SAVE_BUTTON_TYPE:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_flight_plan_next, parent, false);
                return new NextButtonViewHolder(view);
        }

        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        switch (getItemViewType(position)) {
            case BINARY_VIEW_TYPE: {
                final AirMapFlightFeature flightFeature = (AirMapFlightFeature) getItem(position);
                FlightFeatureValue<Boolean> savedValue = flightPlan.getFlightFeatureValues() != null ? flightPlan.getFlightFeatureValues().get(flightFeature.getFlightFeature()) : null;

                final FlightFeatureBinaryViewHolder binaryViewHolder = (FlightFeatureBinaryViewHolder) holder;
                binaryViewHolder.descriptionTextView.setText(flightFeature.getDescription());

                binaryViewHolder.infoButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showFlightFeatureInfo(flightFeature);
                    }
                });

                boolean noSelected = savedValue != null && !savedValue.getValue();
                binaryViewHolder.noButton.setSelected(noSelected);
                binaryViewHolder.noButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        FlightFeatureValue featureValue = new FlightFeatureValue<>(flightFeature.getFlightFeature(), false);
                        flightPlan.setFlightFeatureValue(featureValue);
                        onFlightPlanChanged();

                        Analytics.logEvent(Analytics.Event.flightPlanCheck, Analytics.Action.change, flightFeature.getFlightFeature());
                    }
                });

                boolean yesSelected = savedValue != null && savedValue.getValue();
                binaryViewHolder.yesButton.setSelected(yesSelected);
                binaryViewHolder.yesButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        FlightFeatureValue featureValue = new FlightFeatureValue<>(flightFeature.getFlightFeature(), true);
                        flightPlan.setFlightFeatureValue(featureValue);
                        onFlightPlanChanged();

                        Analytics.logEvent(Analytics.Event.flightPlanCheck, Analytics.Action.change, flightFeature.getFlightFeature());
                    }
                });

                break;
            }
            case FIELD_VIEW_TYPE: {
                final AirMapFlightFeature flightFeature = (AirMapFlightFeature) getItem(position);
                final FlightFeatureFieldViewHolder fieldViewHolder = (FlightFeatureFieldViewHolder) holder;
                final FlightFeatureValue<String> savedValue = flightPlan.getFlightFeatureValues() != null ? flightPlan.getFlightFeatureValues().get(flightFeature.getFlightFeature()) : null;

                fieldViewHolder.descriptionTextView.setText(flightFeature.getDescription());

                if (fieldViewHolder.textWatcher != null) {
                    fieldViewHolder.editText.removeTextChangedListener(fieldViewHolder.textWatcher);
                }

                if (savedValue != null) {
                    fieldViewHolder.editText.setText(savedValue.getValue());
                }

                TextWatcher textWatcher = new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {

                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        FlightFeatureValue flightFeatureValue;
                        try {
                            float floatValue = Float.parseFloat(s.toString());
                            flightFeatureValue = new FlightFeatureValue<>(flightFeature.getFlightFeature(), floatValue);
                        } catch (NumberFormatException e) {
                            flightFeatureValue = new FlightFeatureValue<>(flightFeature.getFlightFeature(), s.toString());
                        }

                        flightPlan.setFlightFeatureValue(flightFeatureValue);
                        onFlightPlanChanged();

                        Analytics.logEvent(Analytics.Event.flightPlanCheck, Analytics.Action.change, flightFeature.getFlightFeature());
                    }
                };

                fieldViewHolder.textWatcher = textWatcher;
                fieldViewHolder.editText.addTextChangedListener(textWatcher);

                fieldViewHolder.infoButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showFlightFeatureInfo(flightFeature);
                    }
                });
                break;
            }
            case TEXT_VIEW_TYPE: {
                final AirMapFlightFeature flightFeature = (AirMapFlightFeature) getItem(position);
                final FlightFeatureTextViewHolder fieldViewHolder = (FlightFeatureTextViewHolder) holder;
                fieldViewHolder.descriptionTextView.setText(flightFeature.getDescription());
                fieldViewHolder.infoButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showFlightFeatureInfo(flightFeature);
                    }
                });
                break;
            }
            case SAVE_BUTTON_TYPE: {
                NextButtonViewHolder nextButtonViewHolder = (NextButtonViewHolder) holder;
                nextButtonViewHolder.nextButton.setText("Save");
                nextButtonViewHolder.nextButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Analytics.logEvent(Analytics.Event.flightPlanCheck, Analytics.Action.tap, Analytics.Label.BOTTOM_NEXT_BUTTON);

                        flightPlanChangeListener.onFlightPlanSave();
                    }
                });
                break;
            }
        }
    }

    private void showFlightFeatureInfo(final AirMapFlightFeature flightFeature) {
        List<AirMapRule> rules = flightFeaturesMap.get(flightFeature);
        Collections.sort(rules, new Comparator<AirMapRule>() {
            @Override
            public int compare(AirMapRule o1, AirMapRule o2) {
                return o1.getShortText().compareTo(o2.getShortText());
            }
        });

        StringBuilder rulesTextBuilder = new StringBuilder();
        Set<String> ruleSet = new HashSet<>();
        boolean learnMore = false;
        rulesTextBuilder.append("The following rule(s) apply:").append("\n").append("\n");
        for (AirMapRule rule : rules) {
            if (!ruleSet.contains(rule.getShortText())) {
                if (!ruleSet.isEmpty()) {
                    rulesTextBuilder.append("\n").append("\n");
                }
                rulesTextBuilder.append(rule.getShortText());
                ruleSet.add(rule.getShortText());
                if (rule.getShortText() != null && !rule.getShortText().equals(rule.getDescription())) {
                    learnMore = true;
                }
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle("Why We're Asking")
                .setMessage(rulesTextBuilder.toString())
                .setPositiveButton(android.R.string.ok, null);

        if (learnMore) {
            builder.setNegativeButton(R.string.learn_more, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();

                    showDetailedRules(flightFeature);
                }
            });
        }

        builder.show();
    }

    private void showDetailedRules(AirMapFlightFeature flightFeature) {
        List<AirMapRule> rules = flightFeaturesMap.get(flightFeature);
        Collections.sort(rules, new Comparator<AirMapRule>() {
            @Override
            public int compare(AirMapRule o1, AirMapRule o2) {
                return o1.toString().compareTo(o2.toString());
            }
        });

        StringBuilder rulesTextBuilder = new StringBuilder();
        Set<String> ruleSet = new HashSet<>();
        for (AirMapRule rule : rules) {
            if (!ruleSet.contains(rule.toString())) {
                if (!ruleSet.isEmpty()) {
                    rulesTextBuilder.append("\n").append("\n");
                }
                rulesTextBuilder.append(rule.toString());
                ruleSet.add(rule.toString());
            }
        }
        new AlertDialog.Builder(context)
                .setTitle("Official Rule")
                .setMessage(rulesTextBuilder.toString())
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    @Override
    public int getItemViewType(int position) {
        if (position == getItemCount() - 1) {
            return SAVE_BUTTON_TYPE;
        }

        AirMapBaseModel item = getItem(position);
        if (item instanceof AirMapFlightFeature) {
            switch (((AirMapFlightFeature) item).getInputType()) {
                case Double:
                    return FIELD_VIEW_TYPE;
                case String:
                    return FIELD_VIEW_TYPE;
                case Boolean:
                    return BINARY_VIEW_TYPE;
                case Info:
                default:
                    return TEXT_VIEW_TYPE;
            }
        }

        return TEXT_VIEW_TYPE;
    }

    private boolean showStatusIcon(AirMapRule rule) {
        for (AirMapFlightFeature flightFeature : rule.getFlightFeatures()) {
            if (flightPlan.getFlightFeatureValues() == null || !flightPlan.getFlightFeatureValues().containsKey(flightFeature.getFlightFeature())) {
                return true;
            }
        }

        return false;
    }

    private String getMeasurementWithUnits(double value, FlightFeatureConfiguration flightFeatureConfig) {
        String units = flightFeatureConfig.getValueConfig(false).getUnit();
        //TODO: i18n support?
        if (value == (long) value) {
            return String.format("%d %s", (int) value, units);
        } else if (value % .5 == 0) {
            return String.format("%.1f %s", value, units);
        } else {
            return String.format("%.2f %s", value, units);
        }
    }

    public AirMapBaseModel getItem(int position) {
        return flightFeatures.get(position);
    }

    @Override
    public int getItemCount() {
        return flightFeaturesMap != null ? flightFeaturesMap.size() + 1 : 1;
    }

    private class FlightFeatureBinaryViewHolder extends RecyclerView.ViewHolder {
        TextView descriptionTextView;
        ToggleButton noButton;
        ToggleButton yesButton;
        ImageButton infoButton;

        FlightFeatureBinaryViewHolder(View itemView) {
            super(itemView);

            descriptionTextView = itemView.findViewById(R.id.description_text_view);
            noButton = itemView.findViewById(R.id.no_button);
            yesButton = itemView.findViewById(R.id.yes_button);
            infoButton = itemView.findViewById(R.id.info_button);
        }
    }

    private class FlightFeatureSeekbarViewHolder extends RecyclerView.ViewHolder {
        TextView descriptionTextView;
        TextView valueTextView;
        SeekBar seekBar;
        ImageButton infoButton;

        FlightFeatureSeekbarViewHolder(View itemView) {
            super(itemView);

            descriptionTextView = itemView.findViewById(R.id.description_text_view);
            valueTextView = itemView.findViewById(R.id.value_text_view);
            seekBar = itemView.findViewById(R.id.seekbar);
            infoButton = itemView.findViewById(R.id.info_button);
        }
    }

    private class FlightFeatureFieldViewHolder extends RecyclerView.ViewHolder {
        TextView descriptionTextView;
        EditText editText;
        ImageButton infoButton;
        TextWatcher textWatcher;

        FlightFeatureFieldViewHolder(View itemView) {
            super(itemView);

            descriptionTextView = itemView.findViewById(R.id.description_text_view);
            editText = itemView.findViewById(R.id.edit_text);
            infoButton = itemView.findViewById(R.id.info_button);
        }
    }

    private class FlightFeatureTextViewHolder extends RecyclerView.ViewHolder {
        TextView descriptionTextView;
        ImageButton infoButton;

        FlightFeatureTextViewHolder(View itemView) {
            super(itemView);

            descriptionTextView = itemView.findViewById(R.id.description_text_view);
            infoButton = itemView.findViewById(R.id.info_button);
        }
    }

    private class NextButtonViewHolder extends RecyclerView.ViewHolder {
        Button nextButton;

        NextButtonViewHolder(View itemView) {
            super(itemView);
            nextButton = itemView.findViewById(R.id.next_button);
        }
    }

    public interface FlightPlanChangeListener {
        void onFlightPlanChanged();

        void onFlightFeatureRemoved(String flightFeature);

        void onFlightPlanSave();
    }
}
