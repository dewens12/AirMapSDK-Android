package com.airmap.airmapsdk.models.status;

import androidx.annotation.ColorRes;

import com.airmap.airmapsdk.R;
import com.airmap.airmapsdk.models.AirMapBaseModel;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static com.airmap.airmapsdk.util.Utils.optString;

@SuppressWarnings("unused")
public class AirMapAirspaceStatus implements Serializable, AirMapBaseModel {
    public enum StatusColor {
        Red("red"), Yellow("yellow"), Green("green"), Orange("orange");

        private final String text;

        StatusColor(String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }

        public static StatusColor fromString(String text) {
            switch (text) {
                case "red":
                    return Red;
                case "yellow":
                    return Yellow;
                case "green":
                    return Green;
                case "orange":
                    return Orange;
                default:
                    return null;
            }
        }

        public @ColorRes
        int getColorRes() {
            switch (this) {
                case Red:
                    return R.color.status_red;
                case Orange:
                    return R.color.status_orange;
                case Yellow:
                    return R.color.status_yellow;
                default:
                case Green:
                    return R.color.status_green;
            }
        }
    }

    private StatusColor advisoryColor;
    private List<AirMapAdvisory> advisories;

    /**
     * Initialize an AirMapStatus from JSON
     *
     * @param statusJson A JSON representation of an AirMapStatus
     */
    public AirMapAirspaceStatus(JSONObject statusJson) {
        constructFromJson(statusJson);
    }

    /**
     * Initialize an AirMapStatus with default values
     */
    public AirMapAirspaceStatus() {

    }

    @Override
    public AirMapAirspaceStatus constructFromJson(JSONObject json) {
        if (json != null) {
            List<AirMapAdvisory> advisories = new ArrayList<>();
            JSONArray advisoriesJson = json.optJSONArray("advisories");
            for (int i = 0; advisoriesJson != null && i < advisoriesJson.length(); i++) {
                advisories.add(new AirMapAdvisory(advisoriesJson.optJSONObject(i)));
            }
            setAdvisories(advisories);
            setAdvisoryColor(StatusColor.fromString(optString(json, "color")));
        }
        return this;
    }

    public StatusColor getAdvisoryColor() {
        return advisoryColor;
    }

    public AirMapAirspaceStatus setAdvisoryColor(StatusColor advisoryColor) {
        this.advisoryColor = advisoryColor;
        return this;
    }

    public List<AirMapAdvisory> getAdvisories() {
        return advisories;
    }

    public AirMapAirspaceStatus setAdvisories(List<AirMapAdvisory> advisories) {
        this.advisories = advisories;
        return this;
    }
}
