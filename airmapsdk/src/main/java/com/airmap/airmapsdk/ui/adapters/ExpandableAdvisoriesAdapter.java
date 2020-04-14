package com.airmap.airmapsdk.ui.adapters;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.airmap.airmapsdk.Analytics;
import com.airmap.airmapsdk.R;
import com.airmap.airmapsdk.models.status.AirMapAdvisory;
import com.airmap.airmapsdk.models.status.AirMapColor;
import com.airmap.airmapsdk.models.status.properties.AirMapAirportProperties;
import com.airmap.airmapsdk.models.status.properties.AirMapControlledAirspaceProperties;
import com.airmap.airmapsdk.models.status.properties.AirMapHeliportProperties;
import com.airmap.airmapsdk.models.status.properties.AirMapNotamProperties;
import com.airmap.airmapsdk.models.status.properties.AirMapPowerPlantProperties;
import com.airmap.airmapsdk.models.status.properties.AirMapTfrProperties;
import com.airmap.airmapsdk.models.status.properties.AirMapWildfireProperties;
import com.airmap.airmapsdk.networking.services.MappingService;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import timber.log.Timber;

import static com.airmap.airmapsdk.util.Utils.checkAndStartIntent;

public class ExpandableAdvisoriesAdapter extends ExpandableRecyclerAdapter<Pair<MappingService.AirMapAirspaceType, AirMapColor>, AirMapAdvisory> {

    public ExpandableAdvisoriesAdapter(LinkedHashMap<MappingService.AirMapAirspaceType, List<AirMapAdvisory>> advisories) {
        super(separateByColor(advisories));
    }

    public void setDataUnseparated(LinkedHashMap<MappingService.AirMapAirspaceType, List<AirMapAdvisory>> data) {
        super.setData(data == null ? null : separateByColor(data));
    }

    @Override
    public void setData(@Nullable LinkedHashMap<Pair<MappingService.AirMapAirspaceType, AirMapColor>, List<AirMapAdvisory>> dataMap) {
        Timber.wtf("Should not be calling setData in ExpandableAdvisoriesAdapter");
        throw new RuntimeException("please call setDataUnseparated instead");
    }

    private static LinkedHashMap<Pair<MappingService.AirMapAirspaceType, AirMapColor>, List<AirMapAdvisory>> separateByColor(Map<MappingService.AirMapAirspaceType, List<AirMapAdvisory>> og) {
        LinkedHashMap<Pair<MappingService.AirMapAirspaceType, AirMapColor>, List<AirMapAdvisory>> dataTemp = new LinkedHashMap<>();
        for (MappingService.AirMapAirspaceType type : og.keySet()) {
            for (AirMapAdvisory advisory : og.get(type)) {
                Pair<MappingService.AirMapAirspaceType, AirMapColor> key = new Pair<>(type, advisory.getColor());
                List<AirMapAdvisory> list = dataTemp.containsKey(key) ? dataTemp.get(key) : new ArrayList<AirMapAdvisory>();
                list.add(advisory);
                dataTemp.put(key, list);
            }
        }
        List<Pair<MappingService.AirMapAirspaceType, AirMapColor>> keys = new ArrayList<>(dataTemp.keySet());
        // Sort based on color
        Collections.sort(keys, new Comparator<Pair<MappingService.AirMapAirspaceType, AirMapColor>>() {
            @Override
            public int compare(Pair<MappingService.AirMapAirspaceType, AirMapColor> p1, Pair<MappingService.AirMapAirspaceType, AirMapColor> p2) {
                if (p1.second == p2.second) return p1.first.toString().compareTo(p2.first.toString());
                if (p1.second == AirMapColor.Red) return -1;
                if (p2.second == AirMapColor.Red) return 1;
                if (p1.second == AirMapColor.Orange) return -1;
                if (p2.second == AirMapColor.Orange) return 1;
                if (p1.second == AirMapColor.Yellow) return -1;
                if (p2.second == AirMapColor.Yellow) return 1;
                return 0;
            }
        });
        LinkedHashMap<Pair<MappingService.AirMapAirspaceType, AirMapColor>, List<AirMapAdvisory>> data = new LinkedHashMap<>();
        for (Pair<MappingService.AirMapAirspaceType, AirMapColor> key : keys) {
            data.put(key, dataTemp.get(key));
        }

        return data;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case PARENT_VIEW_TYPE: {
                return new AirspaceTypeViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_advisory_type, parent, false));
            }
            case CHILD_VIEW_TYPE: {
                return new AdvisoryViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_advisory, parent, false));
            }
        }
        return null;
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);

        if (holder instanceof AirspaceTypeViewHolder) {
            Pair<MappingService.AirMapAirspaceType, AirMapColor> type = (Pair<MappingService.AirMapAirspaceType, AirMapColor>) getItem(position);
            Context context = holder.itemView.getContext();
            String typeText = context.getString(type.first.getTitle());
            String typeAndQuantityText = context.getString(R.string.advisory_type_quantity, typeText, Integer.toString(dataMap.get(type).size()));
            AirMapColor color = type.second;

            ((AirspaceTypeViewHolder) holder).type = type.first;
            ((AirspaceTypeViewHolder) holder).backgroundView.setBackgroundColor(ContextCompat.getColor(context, color.getColorRes()));
            ((AirspaceTypeViewHolder) holder).textView.setText(typeAndQuantityText);
            ((AirspaceTypeViewHolder) holder).textView.setTextColor(ContextCompat.getColor(context, getTextColor(color)));
            ((AirspaceTypeViewHolder) holder).expandImageView.setImageResource(isExpanded(type) ? R.drawable.ic_drop_down_up : R.drawable.ic_drop_down);
            ((AirspaceTypeViewHolder) holder).expandImageView.setColorFilter(ContextCompat.getColor(context, getTextColor(color)), PorterDuff.Mode.SRC_ATOP);

        } else if (holder instanceof AdvisoryViewHolder) {
            final AirMapAdvisory advisory = (AirMapAdvisory) getItem(position);
            ((AdvisoryViewHolder) holder).backgroundView.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), advisory.getColor().getColorRes()));
            ((AdvisoryViewHolder) holder).titleTextView.setText(advisory.getName());
            ((AdvisoryViewHolder) holder).infoTextView.setOnClickListener(null);
            ((AdvisoryViewHolder) holder).infoTextView.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.font_light_grey));

            ((AdvisoryViewHolder) holder).descriptionTextView.setVisibility(View.GONE);
            ((AdvisoryViewHolder) holder).linkButton.setVisibility(View.GONE);

            String info = "";
            if (advisory.getType() != null) {
                switch (advisory.getType()) {
                    case TFR: {
                        final AirMapTfrProperties tfr = advisory.getTfrProperties();
                        SimpleDateFormat dateFormat;
                        if (tfr.getStartTime() != null && tfr.getEndTime() != null) {
                            if (DateUtils.isToday(tfr.getStartTime().getTime())) {
                                dateFormat = new SimpleDateFormat("h:mm a");
                            } else {
                                dateFormat = new SimpleDateFormat("MMM d h:mm a");
                            }
                            info = dateFormat.format(tfr.getStartTime()) + " - " + dateFormat.format(tfr.getEndTime());
                        }

                        if (!TextUtils.isEmpty(tfr.getUrl())) {
                            holder.itemView.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Analytics.logEvent(Analytics.Page.ADVISORIES, Analytics.Action.tap, Analytics.Label.TFR_DETAILS);

                                    String url = tfr.getUrl();
                                    if (!url.contains("http") && !url.contains("https")) {
                                        url = "http://" + url;
                                    }
                                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                                    checkAndStartIntent(holder.itemView.getContext(), intent);
                                }
                            });
                        }
                        break;
                    }
                    case PowerPlant: {
                        AirMapPowerPlantProperties powerPlant = advisory.getPowerPlantProperties();
                        info = powerPlant.getTech();
                        break;
                    }
                    case Fires:
                    case Wildfires: {
                        AirMapWildfireProperties wildfire = advisory.getWildfireProperties();
                        SimpleDateFormat dateFormat = new SimpleDateFormat("h:mm a");
                        String unknownSize = holder.itemView.getContext().getString(R.string.unknown_size);

                        if (wildfire != null && wildfire.getEffectiveDate() != null) {
                            info = dateFormat.format(wildfire.getEffectiveDate())  + " - " + (wildfire.getSize() == -1 ? unknownSize : String.format(Locale.US, "%d acres", wildfire.getSize()));
                        }
                        break;
                    }
                    case Airport: {
                        final AirMapAirportProperties airport = advisory.getAirportProperties();
                        info = formatPhoneNumber(holder.itemView.getContext(), airport.getPhone());

                        if (!TextUtils.isEmpty(airport.getPhone())) {
                            ((AdvisoryViewHolder) holder).infoTextView.setTextColor(ContextCompat.getColor(((AdvisoryViewHolder) holder).infoTextView.getContext(), R.color.colorAccent));
                            holder.itemView.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    call(holder.itemView.getContext(), advisory.getName(), airport.getPhone());
                                }
                            });
                        } else if (advisory.getOptionalProperties() != null) {
                            if (!TextUtils.isEmpty(advisory.getOptionalProperties().getUrl())) {
                                info = advisory.getOptionalProperties().getUrl();
                                holder.itemView.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(advisory.getOptionalProperties().getUrl()));
                                        checkAndStartIntent(holder.itemView.getContext(), intent);
                                    }
                                });
                            }

                            if (!TextUtils.isEmpty(advisory.getOptionalProperties().getDescription())) {
                                ((AdvisoryViewHolder) holder).descriptionTextView.setText(advisory.getOptionalProperties().getDescription());
                                ((AdvisoryViewHolder) holder).descriptionTextView.setVisibility(View.VISIBLE);
                            }
                        }


                        break;
                    }
                    case Heliport: {
                        final AirMapHeliportProperties heliport = advisory.getHeliportProperties();
                        info = formatPhoneNumber(holder.itemView.getContext(), heliport.getPhoneNumber());

                        if (!TextUtils.isEmpty(heliport.getPhoneNumber())) {
                            ((AdvisoryViewHolder) holder).infoTextView.setTextColor(ContextCompat.getColor(((AdvisoryViewHolder) holder).infoTextView.getContext(), R.color.colorAccent));
                            holder.itemView.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    call(holder.itemView.getContext(), advisory.getName(), heliport.getPhoneNumber());
                                }
                            });
                        }
                        break;
                    }
                    case Notam: {
                        final AirMapNotamProperties notam = advisory.getNotamProperties();
                        SimpleDateFormat dateFormat;
                        if (notam.getStartTime() != null && DateUtils.isToday(notam.getStartTime().getTime())) {
                            dateFormat = new SimpleDateFormat("h:mm a");
                        } else {
                            dateFormat = new SimpleDateFormat("MMM d h:mm a");
                        }
                        info = dateFormat.format(notam.getStartTime()) + " - " + dateFormat.format(notam.getEndTime());

                        if (!TextUtils.isEmpty(notam.getUrl())) {
                            holder.itemView.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Analytics.logEvent(Analytics.Page.ADVISORIES, Analytics.Action.tap, Analytics.Label.TFR_DETAILS);

                                    String url = notam.getUrl();
                                    if (!url.contains("http") && !url.contains("https")) {
                                        url = "http://" + url;
                                    }
                                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                                    checkAndStartIntent(holder.itemView.getContext(), intent);
                                }
                            });
                        }
                        break;
                    }
                    case ControlledAirspace: {
                        AirMapControlledAirspaceProperties controlledAirspaceProperties = advisory.getControlledAirspaceProperties();

                        if (controlledAirspaceProperties.isLaanc() && controlledAirspaceProperties.isAuthorization()) {
                            info = holder.itemView.getContext().getString(R.string.airspace_laanc_authorization_automated);
                            ((AdvisoryViewHolder) holder).infoTextView.setTextColor(ContextCompat.getColor(((AdvisoryViewHolder) holder).infoTextView.getContext(), R.color.colorAccent));
                        }
                        break;
                    }
                    case AMA:
                    case Custom:
                    case University:
                    case City:
                    case Park: {
                        if (advisory.getOptionalProperties() != null) {
                            if (!TextUtils.isEmpty(advisory.getOptionalProperties().getUrl())) {
                                info = advisory.getOptionalProperties().getUrl();
                                holder.itemView.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        String url = advisory.getOptionalProperties().getUrl();
                                        if (!url.contains("http") && !url.contains("https")) {
                                            url = "http://" + url;
                                        }
                                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                                        holder.itemView.getContext().startActivity(intent);
                                    }
                                });
                            }

                            if (!TextUtils.isEmpty(advisory.getOptionalProperties().getDescription())) {
                                ((AdvisoryViewHolder) holder).descriptionTextView.setText(advisory.getOptionalProperties().getDescription());
                                ((AdvisoryViewHolder) holder).descriptionTextView.setVisibility(View.VISIBLE);
                            }
                        }
                    }
                }
            }

            // if this advisory accepts digital notice, show user
            if (advisory.getRequirements() != null && advisory.getRequirements().getNotice() != null && advisory.getRequirements().getNotice().isDigital()) {
                info = holder.itemView.getContext().getString(R.string.accepts_digital_notice);
                holder.itemView.setOnClickListener(null);
                ((AdvisoryViewHolder) holder).infoTextView.setTextColor(ContextCompat.getColor(((AdvisoryViewHolder) holder).infoTextView.getContext(), R.color.colorAccent));
            }

            if (advisory.getOptionalProperties() != null) {
                if (!TextUtils.isEmpty(advisory.getOptionalProperties().getUrl())) {
                    holder.itemView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            String url = advisory.getOptionalProperties().getUrl();
                            if (!url.contains("http") && !url.contains("https")) {
                                url = "http://" + url;
                            }
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                            holder.itemView.getContext().startActivity(intent);
                        }
                    });

                    ((AdvisoryViewHolder) holder).linkButton.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), R.color.font_white), PorterDuff.Mode.SRC_ATOP);
                    ((AdvisoryViewHolder) holder).linkButton.setVisibility(View.VISIBLE);
                }
            }

            ((AdvisoryViewHolder) holder).infoTextView.setText(info);
            ((AdvisoryViewHolder) holder).infoTextView.setVisibility(TextUtils.isEmpty(info) ? View.GONE : View.VISIBLE);
        }
    }

    private String formatPhoneNumber(Context context, String number) {
        if (TextUtils.isEmpty(number)) {
            return context.getString(R.string.no_phone_number_provided);
        }

        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        Locale locale = Locale.getDefault();
        String country = locale != null && locale.getCountry() != null && !TextUtils.isEmpty(locale.getCountry()) ? locale.getCountry() : "US";
        try {
            Phonenumber.PhoneNumber phoneNumber = phoneUtil.parse(number, country);
            return phoneUtil.format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.NATIONAL);
        } catch (NumberParseException e) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return PhoneNumberUtils.formatNumber(number, country);
            }
            return PhoneNumberUtils.formatNumber(number);
        }
    }

    private void call(final Context context, String name, final String phoneNumber) {
        new AlertDialog.Builder(context)
                .setTitle(name)
                .setMessage(context.getString(R.string.do_you_want_to_call, name))
                .setPositiveButton(R.string.call, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phoneNumber));
                        boolean handled = checkAndStartIntent(context, intent);
                        if (!handled) {
                            Toast.makeText(context, R.string.no_dialer_found, Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }


    @Override
    protected void toggleExpandingViewHolder(final RecyclerView.ViewHolder holder, final boolean expanded) {
        ((AirspaceTypeViewHolder) holder).expandImageView.setImageResource(expanded ? R.drawable.ic_drop_down_up : R.drawable.ic_drop_down);

        if (expanded) {
            Analytics.logEvent(Analytics.Event.advisories, Analytics.Action.expand, ((AirspaceTypeViewHolder) holder).type.toString());
        }
    }

    private AirMapColor calculateStatusColor(MappingService.AirMapAirspaceType type) {
        AirMapColor color = AirMapColor.Green;
        for (AirMapAdvisory advisory : dataMap.get(type)) {
            if (advisory.getColor() == AirMapColor.Red) {
                color = AirMapColor.Red;
                break;
            } else if (advisory.getColor() == AirMapColor.Orange) {
                color = AirMapColor.Orange;
            } else if (advisory.getColor() == AirMapColor.Yellow && color == AirMapColor.Green) {
                color = AirMapColor.Yellow;
            }
        }

        return color;
    }

    @ColorRes
    private int getTextColor(AirMapColor statusColor) {
        if (statusColor == AirMapColor.Yellow) {
            return R.color.font_black;
        }

        return R.color.font_white;
    }

    private class AirspaceTypeViewHolder extends RecyclerView.ViewHolder {
        View backgroundView;
        TextView textView;
        ImageView expandImageView;
        MappingService.AirMapAirspaceType type;

        AirspaceTypeViewHolder(View itemView) {
            super(itemView);

            backgroundView = itemView.findViewById(R.id.background_view);
            textView = itemView.findViewById(R.id.title_text_view);
            expandImageView = itemView.findViewById(R.id.expand_image_view);
        }
    }

    private class AdvisoryViewHolder extends RecyclerView.ViewHolder {
        View backgroundView;
        TextView titleTextView;
        TextView infoTextView;
        TextView descriptionTextView;
        ImageView linkButton;

        AdvisoryViewHolder(View itemView) {
            super(itemView);

            backgroundView = itemView.findViewById(R.id.background_view);
            titleTextView = itemView.findViewById(R.id.title_text_view);
            infoTextView = itemView.findViewById(R.id.info_text_view);
            descriptionTextView = itemView.findViewById(R.id.description_text_view);
            linkButton = itemView.findViewById(R.id.link_button);
        }
    }
}
