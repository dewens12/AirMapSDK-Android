package com.airmap.airmapsdk.models.flight;

import androidx.annotation.StringRes;

import com.airmap.airmapsdk.R;
import com.airmap.airmapsdk.models.AirMapBaseModel;

import org.json.JSONObject;

import java.io.Serializable;

import static com.airmap.airmapsdk.util.Utils.optString;

public class AirMapFlightFeature implements Serializable, AirMapBaseModel {

    public enum Status {
        Conflicting, NotConflicting, MissingInfo, InformationRules, Unknown;

        public static Status fromString(String text) {
            switch (text.toLowerCase()) {
                case "conflicting":
                    return Conflicting;
                case "not_conflicting":
                    return NotConflicting;
                case "missing_info":
                    return MissingInfo;
                case "informational":
                case "information_rules":
                    return InformationRules;
                default:
                    return Unknown;
            }
        }

        public int intValue() {
            switch (this) {
                case Conflicting:
                    return 0;
                case MissingInfo:
                    return 1;
                case InformationRules:
                    return 2;
                case NotConflicting:
                    return 3;
            }
            return -1;
        }
    }

    public enum InputType {
        Double,Boolean, String, Info, Unknown;

        private static InputType fromText(String text) {
            switch (text.toLowerCase()) {
                case "float":
                    return Double;
                case "bool":
                    return Boolean;
                case "string":
                    return String;
                case "info":
                    return Info;
                default:
                    return Unknown;
            }
        }

        public int value() {
            switch (this) {
                case Info:
                    return 0;
                case String:
                    return 1;
                case Double:
                    return 2;
                case Boolean:
                    return 3;
                default:
                    return 4;
            }
        }
    }

    public enum MeasurementType {
        Speed, Weight, Distance, Unknown;

        private static MeasurementType fromText(String text) {
            switch (text.toLowerCase()) {
                case "speed":
                    return Speed;
                case "weight":
                    return Weight;
                case "distance":
                    return Distance;
                default:
                    return Unknown;
            }
        }

        @StringRes
        public int getStringRes() {
            switch (this) {
                case Speed:
                    return R.string.flight_feature_speed;
                case Weight:
                    return R.string.flight_feature_weight;
                case Distance:
                    return R.string.flight_feature_distance;
                case Unknown:
                default:
                    return -1;
            }
        }
    }

    public enum MeasurementUnit {
        Kilograms, Meters, MetersPerSecond, Unknown;

        private static MeasurementUnit fromText(String text) {
            switch (text.toLowerCase()) {
                case "kilograms":
                    return Kilograms;
                case "meters":
                    return Meters;
                case "meters_per_sec":
                    return MetersPerSecond;
                default:
                    return Unknown;
            }
        }
    }

    private int id;
    private String flightFeature;
    private Status status;
    private String description;
    private InputType inputType;
    private MeasurementType measurementType;
    private MeasurementUnit measurementUnit;
    private boolean isCalculated;

    public AirMapFlightFeature(String flightFeature) {
        setFlightFeature(flightFeature);
    }

    public AirMapFlightFeature(JSONObject jsonObject) {
        constructFromJson(jsonObject);
    }

    @Override
    public AirMapBaseModel constructFromJson(JSONObject json) {
        setFlightFeature(optString(json, "flight_feature"));
        setStatus(Status.fromString(optString(json, "status")));
        setInputType(InputType.fromText(optString(json, "input_type")));
        setDescription(optString(json, "description"));
        setMeasurementType(MeasurementType.fromText(optString(json, "measurement_type")));
        setMeasurementUnit(MeasurementUnit.fromText(optString(json, "measurement_unit")));
        setCalculated(json.optBoolean("is_calculated"));
        return this;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFlightFeature() {
        return flightFeature;
    }

    public void setFlightFeature(String flightFeature) {
        this.flightFeature = flightFeature;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public InputType getInputType() {
        return inputType;
    }

    public void setInputType(InputType inputType) {
        this.inputType = inputType;
    }

    public MeasurementType getMeasurementType() {
        return measurementType;
    }

    public void setMeasurementType(MeasurementType measurementType) {
        this.measurementType = measurementType;
    }

    public MeasurementUnit getMeasurementUnit() {
        return measurementUnit;
    }

    public void setMeasurementUnit(MeasurementUnit measurementUnit) {
        this.measurementUnit = measurementUnit;
    }

    public boolean isCalculated() {
        return isCalculated;
    }

    public void setCalculated(boolean calculated) {
        isCalculated = calculated;
    }

    @Override
    public String toString() {
        return flightFeature;
    }

    @Override
    public boolean equals(Object o) {
        return o != null && o instanceof AirMapFlightFeature && ((AirMapFlightFeature) o).getFlightFeature().equals(this.flightFeature);
    }

    @Override
    public int hashCode() {
        return flightFeature.hashCode();
    }

    public boolean isAltitudeFeature() {
        return (flightFeature.contains("agl") || flightFeature.contains("altitude")) && getInputType() == InputType.Double;
    }
}
