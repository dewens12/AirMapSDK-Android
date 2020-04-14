package com.airmap.airmapsdk.models.rules;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.airmap.airmapsdk.R;
import com.airmap.airmapsdk.models.AirMapBaseModel;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static com.airmap.airmapsdk.util.Utils.optString;

public class AirMapRuleset implements Serializable, AirMapBaseModel, Comparable {

    public enum Type {
        PickOne("pickone"), Required("required"), Optional("optional");

        private String text;

        Type(String text) {
            this.text = text;
        }

        public static AirMapRuleset.Type fromString(String text) {
            switch (text.toLowerCase()) {
                case "required":
                    return Required;
                case "optional":
                    return Optional;
                case "pick1":
                case "pickone":
                    return PickOne;
            }

            return Required;
        }

        public String toString() {
            return text;
        }

        public int intValue() {
            if (this == PickOne) {
                return 2;
            } else if (this == Optional) {
                return 1;
            } else {
                return 0;
            }
        }

        public @StringRes int getTitle() {
            switch(this) {
                case PickOne:
                    return R.string.pick_one;
                case Optional:
                    return R.string.optional;
                case Required:
                    return R.string.required;
                default:
                    return -1;
            }
        }
    }


    private String id;
    private String name;
    private String shortName;
    private AirMapJurisdiction jurisdiction;
    private AirMapRuleset.Type type;
    private boolean isDefault;
    private String summary;
    private List<String> layers;

    private List<AirMapRule> rules;

    public AirMapRuleset() {
    }

    public AirMapRuleset(JSONObject resultJson) {
        constructFromJson(resultJson);
    }

    @Override
    public AirMapRuleset constructFromJson(JSONObject json) {
        if (json != null) {
            setId(optString(json, "id"));
            setName(optString(json, "name"));
            setShortName(optString(json, "short_name"));

            JSONObject jurisdictionObject = json.optJSONObject("jurisdiction");
            if (jurisdictionObject != null) {
                setJurisdiction(new AirMapJurisdiction(jurisdictionObject));
            }
            setSummary(optString(json, "description"));
            setType(Type.fromString(optString(json, "selection_type")));

            setDefault(json.optBoolean("default"));

            layers = new ArrayList<>();
            JSONArray layersJson = json.has("layers") ? json.optJSONArray("layers") : json.optJSONArray("airspace_types");
            for (int i = 0; layersJson != null && i < layersJson.length(); i++) {
                layers.add(optString(layersJson, i));
            }

            rules = new ArrayList<>();
            JSONArray rulesJSON = json.optJSONArray("rules");
            for (int j = 0; rulesJSON != null && j < rulesJSON.length(); j++) {
                rules.add(new AirMapRule(rulesJSON.optJSONObject(j)));
            }
        }
        return this;
    }

    public String getId() {
        return id;
    }

    public AirMapRuleset setId(String id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public AirMapRuleset setName(String name) {
        this.name = name;
        return this;
    }

    public AirMapJurisdiction getJurisdiction() {
        return jurisdiction;
    }

    public AirMapRuleset setJurisdiction(AirMapJurisdiction jurisdiction) {
        this.jurisdiction = jurisdiction;
        return this;
    }

    public String getShortName() {
        if (TextUtils.isEmpty(shortName)) {
            return name;
        }
        return shortName;
    }

    public AirMapRuleset setShortName(String shortName) {
        this.shortName = shortName;
        return this;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public AirMapRuleset.Type getType() {
        return type;
    }

    public AirMapRuleset setType(AirMapRuleset.Type type) {
        this.type = type;
        return this;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    public List<String> getLayers() {
        return layers;
    }

    public List<AirMapRule> getRules() {
        return rules;
    }

    public void setRules(List<AirMapRule> rules) {
        this.rules = rules;
    }

    @Override
    public int compareTo(@NonNull Object o) {
        if (!(o instanceof AirMapRuleset)) {
            return -1;
        }

        if (type.intValue() > ((AirMapRuleset) o).getType().intValue()) {
            return 1;
        } else if (type.intValue() < ((AirMapRuleset) o).getType().intValue()) {
            return -1;
        } else {
            return 0;
        }
    }

    @Override
    public String toString() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        return o != null && o instanceof AirMapRuleset && ((AirMapRuleset) o).getId().equals(this.getId());
    }

    @Override
    public int hashCode() {
        if (id == null) {
            return 0;
        }
        return id.hashCode();
    }
}
