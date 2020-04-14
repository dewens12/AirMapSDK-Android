package com.airmap.airmapsdk.models.flight;

import android.text.TextUtils;

import com.airmap.airmapsdk.models.AirMapBaseModel;
import com.airmap.airmapsdk.models.Coordinate;
import com.airmap.airmapsdk.models.shapes.AirMapGeometry;
import com.airmap.airmapsdk.models.shapes.AirMapPath;
import com.airmap.airmapsdk.models.shapes.AirMapPoint;
import com.airmap.airmapsdk.models.shapes.AirMapPolygon;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import timber.log.Timber;

import static com.airmap.airmapsdk.util.Utils.getDateFromIso8601String;
import static com.airmap.airmapsdk.util.Utils.getIso8601StringFromDate;
import static com.airmap.airmapsdk.util.Utils.optString;

public class AirMapFlightPlan implements Serializable, AirMapBaseModel {

    private String planId;
    private String flightId;
    private String pilotId;
    private String aircraftId;
    private Date startsAt;
    private Date endsAt;
    private long durationInMillis;
    private float buffer;
    private String geometry;
    private float maxAltitude;
    private Coordinate takeoffCoordinate;
    private List<String> rulesetIds;
    private Map<String, FlightFeatureValue> flightFeatureValues;

    private Date createdAt;
    private boolean isPublic;
    private boolean notify;

    public AirMapFlightPlan() {
        isPublic = true;
    }

    public AirMapFlightPlan(AirMapFlightPlan planToClone) {
        setTakeoffCoordinate(planToClone.takeoffCoordinate);
        setMaxAltitude(planToClone.maxAltitude);
        setNotify(planToClone.notify);
        setPilotId(planToClone.pilotId);
        setAircraftId(planToClone.aircraftId);
        setPublic(planToClone.isPublic);
        setBuffer(planToClone.buffer);
        setGeometry(planToClone.geometry);
        setRulesetIds(planToClone.rulesetIds);
        setFlightFeatureValues(planToClone.flightFeatureValues);
        setCreatedAt(planToClone.createdAt);
        setStartsAt(planToClone.startsAt);
        setEndsAt(planToClone.endsAt);
        setDurationInMillis(planToClone.durationInMillis);
    }

    public AirMapFlightPlan(JSONObject flightJson) {
        constructFromJson(flightJson);
    }

    @Override
    public AirMapFlightPlan constructFromJson(JSONObject json) {
        if (json != null) {
            setPlanId(optString(json, "id"));
            setFlightId(optString(json, "flight_id"));
            setTakeoffCoordinate(new Coordinate(json.optDouble("takeoff_latitude", 0), json.optDouble("takeoff_longitude", 0)));
            setMaxAltitude((float) json.optDouble("max_altitude_agl"));
            setNotify(json.optBoolean("notify"));
            setPilotId(optString(json, "pilot_id"));
            setAircraftId(optString(json, "aircraft_id"));
            setPublic(json.optBoolean("public"));
            setBuffer((float) json.optDouble("buffer"));

            JSONObject geometryGeoJSON = json.optJSONObject("geometry");
            if (geometryGeoJSON != null) {
                setGeometry(geometryGeoJSON.toString());
            }

            rulesetIds = new ArrayList<>();
            JSONArray rulesetsJson = json.optJSONArray("rulesets");
            for (int i = 0; rulesetsJson != null && i < rulesetsJson.length(); i++) {
                rulesetIds.add(optString(rulesetsJson, i));
            }

            flightFeatureValues = new HashMap<>();
            JSONObject flightFeaturesJSON = json.optJSONObject("flight_features");
            if (flightFeaturesJSON != null) {
                Iterator<String> featuresItr = flightFeaturesJSON.keys();
                while (featuresItr.hasNext()) {
                    String key = featuresItr.next();
                    Object value = flightFeaturesJSON.opt(key);
                    FlightFeatureValue flightFeatureValue = new FlightFeatureValue(key, value);
                    flightFeatureValues.put(key, flightFeatureValue);
                }
            }

            //Created at
            if (json.has("created_at")) {
                setCreatedAt(getDateFromIso8601String(optString(json, "created_at")));
            } else if (json.has("creation_date")) {
                setCreatedAt(getDateFromIso8601String(optString(json, "creation_date")));
            }

            String startTime = optString(json, "start_time");
            String endTime = optString(json, "end_time");
            if (!TextUtils.isEmpty(startTime) && !startTime.equals("now")) {
                // if start date is before now, set it to now and shift end time
                Date startDate = getDateFromIso8601String(startTime);
                Date endDate = getDateFromIso8601String(endTime);
                long duration = endDate.getTime() - startDate.getTime();
                if (new Date().after(startDate)) {
                    setStartsAt(new Date());
                    setEndsAt(new Date(getStartsAt().getTime() + duration));
                } else {
                    setStartsAt(startDate);
                    setEndsAt(new Date(startDate.getTime() + duration));
                }
            } else {
                setEndsAt(getDateFromIso8601String(endTime));
            }
        }
        return this;
    }

    /**
     * Turn the AirMapFlight into a Map of it's fields and values for easy use with web calls
     *
     * @return The AirMapFlight encoded as a Map
     */
    public JSONObject getAsParams() {
        Map<String, Object> params = new HashMap<>();
        params.put("id", getPlanId());
        params.put("pilot_id", getPilotId());

        if (getAircraftId() != null && !getAircraftId().isEmpty()) {
            params.put("aircraft_id", getAircraftId());
        }

        if (getStartsAt() != null && getStartsAt().after(new Date()) && getIso8601StringFromDate(getStartsAt()) != null) {
            params.put("start_time", getIso8601StringFromDate(getStartsAt()));
        } else {
            params.put("start_time", "now");
        }

        if (getDurationInMillis() != 0) {
            long startTime = getStartsAt() != null ? getStartsAt().getTime() : System.currentTimeMillis();
            params.put("end_time", getIso8601StringFromDate(new Date(startTime + getDurationInMillis())));
        } else {
            params.put("end_time", getIso8601StringFromDate(getEndsAt()));
        }

        try {
            JSONObject geoJSON = new JSONObject(getGeometry());
            params.put("geometry", geoJSON);

            if (takeoffCoordinate != null) {
                params.put("takeoff_latitude", getTakeoffCoordinate().getLatitude());
                params.put("takeoff_longitude", getTakeoffCoordinate().getLongitude());
            } else {
                AirMapGeometry geometry = AirMapGeometry.getGeometryFromGeoJSON(geoJSON);
                Coordinate coordinate;
                if (geometry instanceof AirMapPolygon) {
                    coordinate = ((AirMapPolygon) geometry).getCoordinates().get(0);
                } else if (geometry instanceof AirMapPoint) {
                    coordinate = ((AirMapPoint) geometry).getCoordinate();
                } else { //path
                    coordinate = ((AirMapPath) geometry).getCoordinates().get(0);
                }
                params.put("takeoff_latitude", coordinate.getLatitude());
                params.put("takeoff_longitude", coordinate.getLongitude());
            }
        } catch (JSONException e) {
            Timber.e(e, "Failed to parse geojson: %s", getGeometry());
        }

        params.put("buffer", buffer);
        params.put("max_altitude_agl", getMaxAltitude());

        Iterator<Map.Entry<String, Object>> iterator = params.entrySet().iterator();
        while (iterator.hasNext()) { //Remove any null values
            Map.Entry<String, Object> entry = iterator.next();
            if (entry.getValue() == null || entry.getValue().equals("null")) {
                iterator.remove();
            }
        }
        JSONObject jsonObject = new JSONObject(params);

        if (rulesetIds != null && !rulesetIds.isEmpty()) {
            JSONArray rulesetsArray = new JSONArray(rulesetIds);
            try {
                jsonObject.put("rulesets", rulesetsArray);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        if (flightFeatureValues != null && !flightFeatureValues.isEmpty()) {
            JSONObject flightFeaturesJson = new JSONObject();
            for (FlightFeatureValue featureValue : flightFeatureValues.values()) {
                try {
                    // Don't include empty strings
                    if (featureValue.getValue() != null && featureValue.getValue() instanceof String && TextUtils.isEmpty((CharSequence) featureValue.getValue())) {
                        continue;
                    }

                    flightFeaturesJson.put(featureValue.getKey(), featureValue.getValue());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            try {
                jsonObject.put("flight_features", flightFeaturesJson);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return jsonObject;
    }

    /**
     * @param coordinates The coordinates to make a polygon from
     * @return A GeoJSON formatted String
     */
    public static String getPolygonString(List<Coordinate> coordinates) {
        return new AirMapPolygon(coordinates).toString();
    }

    /**
     * @param coordinates The coordinates to make a line of
     * @return a GeoJSON formatted String
     */
    public static String getLineString(List<Coordinate> coordinates) {
        return new AirMapPath(coordinates).toString();
    }

    /**
     * @param coordinate The coordinate to make the WKT string for
     * @return a GeoJSON formatted String
     */
    public static String getPointString(Coordinate coordinate) {
        return new AirMapPoint(coordinate).toString();
    }

    /**
     * Determines whether the current AirMapFlightPlan is valid or not
     *
     * @return The validity of the flight
     */
    public boolean isValid() {
        return planId != null;
    }

    /**
     * Determines whether the AirMapFlightPlan is a currently active flight
     *
     * @return Whether the flight is current or not
     */
    public boolean isActive() {
        Date now = new Date();
        return !TextUtils.isEmpty(flightId) && startsAt != null && endsAt != null && now.after(startsAt) && now.before(endsAt);
    }

    public String getPlanId() {
        return planId;
    }

    public String getFlightId() {
        return flightId;
    }

    public void setFlightId(String flightId) {
        this.flightId = flightId;
    }

    public String getPilotId() {
        return pilotId;
    }

    public AirMapFlightPlan setPilotId(String pilotId) {
        this.pilotId = pilotId;
        return this;
    }

    public Coordinate getTakeoffCoordinate() {
        return takeoffCoordinate;
    }

    public AirMapFlightPlan setTakeoffCoordinate(Coordinate takeoffCoordinate) {
        this.takeoffCoordinate = takeoffCoordinate;
        return this;
    }

    /**
     * @return maxAltitude of the flight in meters
     */
    public float getMaxAltitude() {
        return maxAltitude;
    }

    /**
     * @param maxAltitude Altitude of the flight in meters
     */
    public AirMapFlightPlan setMaxAltitude(float maxAltitude) {
        this.maxAltitude = maxAltitude;
        return this;
    }

    /**
     * @return buffer, in meters
     */
    public float getBuffer() {
        return buffer;
    }

    public AirMapFlightPlan setPlanId(String planId) {
        this.planId = planId;
        return this;
    }

    /**
     * @param buffer buffer, in meters
     */
    public AirMapFlightPlan setBuffer(float buffer) {
        this.buffer = buffer;
        return this;
    }

    public String getAircraftId() {
        return aircraftId;
    }

    public AirMapFlightPlan setAircraftId(String aircraftId) {
        this.aircraftId = aircraftId;
        return this;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public AirMapFlightPlan setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public Date getStartsAt() {
        return startsAt;
    }

    public AirMapFlightPlan setStartsAt(Date startsAt) {
        this.startsAt = startsAt;
        return this;
    }

    public Date getEndsAt() {
        return endsAt;
    }

    public AirMapFlightPlan setEndsAt(Date endsAt) {
        this.endsAt = endsAt;
        return this;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public AirMapFlightPlan setPublic(boolean aPublic) {
        isPublic = aPublic;
        return this;
    }

    public boolean shouldNotify() {
        return notify;
    }

    public AirMapFlightPlan setNotify(boolean notify) {
        this.notify = notify;
        return this;
    }

    public String getGeometry() {
        return geometry;
    }

    public AirMapFlightPlan setGeometry(AirMapPolygon geometry) {
        this.geometry = AirMapGeometry.getGeoJSONFromGeometry(geometry).toString();
        return this;
    }

    public AirMapFlightPlan setGeometry(String geometry) {
        this.geometry = geometry;
        return this;
    }

    public List<String> getRulesetIds() {
        return rulesetIds;
    }

    public void setRulesetIds(List<String> rulesetIds) {
        this.rulesetIds = rulesetIds;
    }

    public Map<String, FlightFeatureValue> getFlightFeatureValues() {
        return flightFeatureValues;
    }

    public void setFlightFeatureValues(Map<String, FlightFeatureValue> flightFeatureValues) {
        this.flightFeatureValues = flightFeatureValues;
    }

    public void setFlightFeatureValues(Set<FlightFeatureValue> flightFeatureValuesSet) {
        flightFeatureValues = new HashMap<>();

        for (FlightFeatureValue flightFeatureValue : flightFeatureValuesSet) {
            flightFeatureValues.put(flightFeatureValue.getKey(), flightFeatureValue);
        }
    }

    public void setFlightFeatureValue(FlightFeatureValue flightFeatureValue) {
        if (flightFeatureValues == null) {
            flightFeatureValues = new HashMap<>();
        } else {
            flightFeatureValues.remove(flightFeatureValue.getKey());
        }

        if (flightFeatureValue.getValue() != null) {
            flightFeatureValues.put(flightFeatureValue.getKey(), flightFeatureValue);
        }
    }

    public long getDurationInMillis() {
        return durationInMillis;
    }

    public void setDurationInMillis(long durationInMillis) {
        this.durationInMillis = durationInMillis;
    }

    /**
     * Comparison based on ID
     */
    @Override
    public boolean equals(Object o) {
        return o instanceof AirMapFlightPlan && getPlanId().equals(((AirMapFlightPlan) o).getPlanId());
    }
}
