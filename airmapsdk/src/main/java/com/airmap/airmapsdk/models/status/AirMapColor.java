package com.airmap.airmapsdk.models.status;

import androidx.annotation.ColorRes;

import com.airmap.airmapsdk.R;

public enum AirMapColor {
    Red("red"), Yellow("yellow"), Green("green"), Orange("orange");

    private final String text;

    AirMapColor(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }

    public static AirMapColor fromString(String text) {
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

    public int intValue() {
        switch (this) {
            case Red:
                return 4;
            case Orange:
                return 3;
            case Yellow:
                return 2;
            default:
            case Green:
                return 1;
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
