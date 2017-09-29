package com.airmap.airmapsdk.util;

import android.content.Context;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.TypedValue;

import com.airmap.airmapsdk.AirMapException;
import com.airmap.airmapsdk.AirMapLog;
import com.airmap.airmapsdk.R;
import com.airmap.airmapsdk.models.Coordinate;
import com.airmap.airmapsdk.models.status.AirMapStatus;
import com.airmap.airmapsdk.networking.callbacks.AirMapCallback;
import com.airmap.airmapsdk.networking.services.AirMap;
import com.mapbox.mapboxsdk.annotations.PolygonOptions;
import com.mapbox.mapboxsdk.geometry.LatLng;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by Vansh Gandhi on 7/25/16.
 * Copyright © 2016 AirMap, Inc. All rights reserved.
 */
@SuppressWarnings("unused")
public class Utils {

    private static final String TAG = "Utils (SDK)";

    public static final String REFRESH_TOKEN_KEY = "AIRMAP_SDK_REFRESH_TOKEN";

    /** Return the value mapped by the given key, or {@code null} if not present or null. */
    public static String optString(JSONObject json, String key) {
        // http://code.google.com/p/android/issues/detail?id=13830
        if (json.isNull(key))
            return null;
        else
            return json.optString(key, null);
    }

    public static Float dpToPixels(Context context, int dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }

    public static boolean useMetric(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(AirMapConstants.MEASUREMENT_SYSTEM, AirMapConstants.IMPERIAL_SYSTEM).equals(AirMapConstants.METRIC_SYSTEM);
    }

    public static String titleCase(String s) {
        if (TextUtils.isEmpty(s)) {
            return s;
        }

        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    /**
     * Converts pressure in millimeters of mercury (Hg) to hectoPascals (hPa)
     *
     * @param hg Pressure in mm of Hg
     * @return Pressing in hPa
     */
    public static Float hgToHpa(float hg) {
        return (float) (hg * 33.864);
    }

    public static double feetToMeters(double feet) {
        return feet * 0.3048;
    }

    public static double metersToFeet(double meters) {
        return meters * 3.2808399;
    }

    public static double celsiusToFahrenheit(double celsius) {
        return 32 + (celsius * 9 / 5);
    }

    public static String getTemperatureString(Context context, double tempInCelsius, boolean useFahrenheit) {
        if (useFahrenheit) {
            return context.getString(R.string.fahrenheit_temp, Math.round(celsiusToFahrenheit(tempInCelsius)));
        }
        return context.getString(R.string.celsius_temp, Math.round(tempInCelsius));
    }

    public static int convertToDp(Context context, int px) {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) (px * scale + 0.5f);
    }

    public static double metersToMiles(double meters) {
        return meters * 0.000621371;
    }

    public static int milesToKilometers(int miles) {
        return (int) (miles * 1.609344);
    }

    public static int kilometersToMiles(int kilometers) {
        return (int) (kilometers / 1.609344);
    }

    public static int metersPerSecondToKts(int metersPerSecond) {
        return (int) (metersPerSecond * 1.94384);
    }

    public static int metersPerSecondToKmph(int metersPerSecond) {
        return (int) (metersPerSecond * 3.6);
    }
    public static int metersPerSecondToMph(int metersPerSecond) {
        return (int) (metersPerSecond * 2.23694);
    }

    public static DateFormat getDateTimeFormat() {
        return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault());
    }

    public static DateFormat getDateFormat() {
        return DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault());
    }

    public static DateFormat getTimeFormat() {
        return DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault());
    }

    public static String getIso8601StringFromDate(Date date) {
        if (date == null) {
            return null;
        }

        DateTimeFormatter dateTimeFormatter = ISODateTimeFormat.dateTime().withZoneUTC();
        return dateTimeFormatter.print(new DateTime(date));
    }

    /**
     * Formats a string into a @link{java.util Date} object
     *
     * @param iso8601 The ISO 8601 string to convert to a Date object
     * @return The converted Date
     */
    public static Date getDateFromIso8601String(String iso8601) {
        if (TextUtils.isEmpty(iso8601)) {
            return null;
        }

        try {
            DateTimeFormatter dateTimeFormatter = ISODateTimeFormat.dateTime().withZoneUTC();
            DateTime dateTime = dateTimeFormatter.parseDateTime(iso8601);
            return dateTime.toDate();
        } catch (UnsupportedOperationException | IllegalArgumentException e) {
            AirMapLog.e("AirMap Utils", "Error parsing date: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public static boolean statusSuccessful(JSONObject object) {
        return object != null && object.optString("status").equalsIgnoreCase("success");
    }

    public static void error(AirMapCallback listener, Exception e) {
        if (e != null && listener != null) {
            if (e.getMessage().toLowerCase().startsWith("unable to resolve host")) {
                listener.error(new AirMapException("No internet connection"));
            } else if (!e.getMessage().toLowerCase().contains("canceled")) { //Not an error if it was canceled
                listener.error(new AirMapException(e.getMessage()));
            }
        }
    }

    //So we don't have to be doing null checks constantly
    public static void error(AirMapCallback listener, int code, JSONObject json) {
        if (listener != null) {
            listener.error(new AirMapException(code, json));
        }
    }

    /**
     * @return Default duration presets when creating a flight
     */
    public static long[] getDurationPresets() {
        return new long[]{
                5 * 60 * 1000L, //5 minutes in millis
                10 * 60 * 1000L,
                15 * 60 * 1000L,
                30 * 60 * 1000L,
                45 * 60 * 1000L,
                60 * 60 * 1000L,
                90 * 60 * 1000L,
                120 * 60 * 1000L,
                150 * 60 * 1000L,
                180 * 60 * 1000L,
                210 * 60 * 1000L,
                240 * 60 * 1000L
        };
    }

    public static String getDurationText(Context context, long timeInMillis) {
        double oneMinute = 60 * 1000;
        double oneHour = 60 * oneMinute;

        if (timeInMillis >= oneHour) {
            double hours = timeInMillis / oneHour;
            return context.getResources().getQuantityString(R.plurals.duration_in_hours, (int) Math.ceil(hours), formatNumber(hours));
        } else {
            double minutes = timeInMillis / oneMinute;
            return context.getString(R.string.duration_in_minutes, formatNumber(minutes));
        }
    }

    private static String formatNumber(double number) {
        if (number == (long) number) {
            return String.format(Locale.getDefault(), "%d", (long) number);
        } else {
            return String.format(Locale.getDefault(), "%s", number);
        }
    }

    /**
     * @return Default altitude presets when creating a flight
     */
    public static double[] getAltitudePresets() {
        return new double[]{
                feetToMeters(50),
                feetToMeters(100),
                feetToMeters(200),
                feetToMeters(300),
                feetToMeters(400)
        };
    }

    /**
     * @return Default altitude presets when creating a flight
     */
    public static double[] getAltitudePresetsMetric() {
        return new double[]{
                15,
                30,
                60,
                90,
                120
        };
    }

    /**
     * @return Default buffer presets when creating a flight
     */
    public static double[] getBufferPresets() {
        return new double[]{
                feetToMeters(25),
                feetToMeters(50),
                feetToMeters(75),
                feetToMeters(100),
                feetToMeters(125),
                feetToMeters(150),
                feetToMeters(175),
                feetToMeters(200),
                feetToMeters(250),
                feetToMeters(300),
                feetToMeters(350),
                feetToMeters(400),
                feetToMeters(450),
                feetToMeters(500),
                feetToMeters(600),
                feetToMeters(700),
                feetToMeters(800),
                feetToMeters(900),
                feetToMeters(1000),
                feetToMeters(1250),
                feetToMeters(1500),
                feetToMeters(1750),
                feetToMeters(2000),
                feetToMeters(2500),
                feetToMeters(3000)
        };
    }

    /**
     * @return Default buffer presets when creating a flight
     */
    public static double[] getBufferPresetsMetric() {
        return new double[]{
                10,
                15,
                20,
                25,
                30,
                50,
                60,
                75,
                100,
                120,
                150,
                175,
                200,
                225,
                250,
                275,
                300,
                350,
                400,
                500,
                600,
                750,
                1000
        };
    }

    public static String getMeasurementText(Context context, double bufferInMeters, boolean useMetric) {
        int buffer = (int) (useMetric ? bufferInMeters : Math.round(metersToFeet(bufferInMeters)));
        return context.getString(useMetric ? R.string.units_meter_short : R.string.units_feet_short, buffer);
    }

    public static int indexOfNearestMatch(double meters, double[] presets) {
        for (int i = 0; i < presets.length; i++) {
            if (Math.round(presets[i]) >= Math.round(meters)) {
                return i;
            }
        }
        return presets.length - 1;
    }

    public static int indexOfNearestMatch(double meters, List<Double> presets) {
        for (int i = 0; i < presets.size(); i++) {
            if (round(presets.get(i), 2) >= round(meters, 2)) {
                return i;
            }
        }
        return presets.size() - 1;
    }

    private static double round(double value, int precision) {
        int scale = (int) Math.pow(10, precision);
        return (double) Math.round(value * scale) / scale;
    }

    public static int indexOfDurationPreset(long millis) {
        for (int i = 0; i < getDurationPresets().length; i++) {
            if (getDurationPresets()[i] >= millis) {
                return i;
            }
        }
        return -1;
    }

    public static String getPriceText(double priceInUSD) {
        return String.format("$%.2f", priceInUSD);
    }

    /**
     * Makes a polygon with many sides to simulate a circle
     *
     * @param radius     Radius of the circle to draw
     * @param coordinate Coordinate to draw the circle
     * @return A "circle"
     */
    public static PolygonOptions getCirclePolygon(double radius, Coordinate coordinate, int color) {
        //We'll make a polygon with 45 sides to make a "circle"
        int degreesBetweenPoints = 8; //45 sides
        int numberOfPoints = (int) Math.floor(360 / degreesBetweenPoints);
        double distRadians = radius / 6371000.0; // earth radius in meters
        double centerLatRadians = coordinate.getLatitude() * Math.PI / 180;
        double centerLonRadians = coordinate.getLongitude() * Math.PI / 180;
        ArrayList<LatLng> points = new ArrayList<>(); //array to hold all the points
        for (int index = 0; index < numberOfPoints; index++) {
            double degrees = index * degreesBetweenPoints;
            double degreeRadians = degrees * Math.PI / 180;
            double pointLatRadians = Math.asin(Math.sin(centerLatRadians) * Math.cos(distRadians) + Math.cos(centerLatRadians) * Math.sin(distRadians) * Math.cos(degreeRadians));
            double pointLonRadians = centerLonRadians + Math.atan2(Math.sin(degreeRadians) * Math.sin(distRadians) * Math.cos(centerLatRadians),
                    Math.cos(distRadians) - Math.sin(centerLatRadians) * Math.sin(pointLatRadians));
            double pointLat = pointLatRadians * 180 / Math.PI;
            double pointLon = pointLonRadians * 180 / Math.PI;
            LatLng point = new LatLng(pointLat, pointLon);
            points.add(point);
        }
        return new PolygonOptions().addAll(points).strokeColor(color).alpha(0.66f).fillColor(color);
    }

    public static int getStatusCircleColor(AirMapStatus latestStatus, Context context) {
        int color = 0;
        if (latestStatus != null) {
            AirMapStatus.StatusColor statusColor = latestStatus.getAdvisoryColor();
            if (statusColor == AirMapStatus.StatusColor.Red) {
                color = ContextCompat.getColor(context, R.color.status_red);
            } else if (statusColor == AirMapStatus.StatusColor.Orange) {
                color = ContextCompat.getColor(context, R.color.status_orange);
            } else if (statusColor == AirMapStatus.StatusColor.Yellow) {
                color = ContextCompat.getColor(context, R.color.status_yellow);
            } else if (statusColor == AirMapStatus.StatusColor.Green) {
                color = ContextCompat.getColor(context, R.color.status_green);
            }
        } else {
            color = 0x1E88E5;
        }
        return color;
    }

    public static String readInputStreamAsString(InputStream in) throws IOException {
        BufferedInputStream bis = new BufferedInputStream(in);
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        int result = bis.read();
        while (result != -1) {
            byte b = (byte) result;
            buf.write(b);
            result = bis.read();
        }
        return buf.toString();
    }

    /**
     * Formats a list of Coordinates into WKT format
     *
     * @param coordinates The list of coordinates to include in the result
     * @return A WKT formatted string of coordinates
     */
    public static String makeGeoString(List<Coordinate> coordinates) {
        return TextUtils.join(",", coordinates);
    }


    public static String getStagingUrl() {
        try {
            return AirMap.getConfig().getJSONObject("internal").getString("debug_url");
        } catch (JSONException e) {
            return "stage/";
        }
    }

    public static String getMqttDebugUrl() {
        try {
            return AirMap.getConfig().getJSONObject("internal").getString("mqtt_url");
        } catch (JSONException e) {
            return "v2/";
        }
    }

    public static String getTelemetryDebugUrl() {
        try {
            return AirMap.getConfig().getJSONObject("internal").getString("telemetry_url");
        } catch (JSONException e) {
            return "v2/";
        }
    }
}
