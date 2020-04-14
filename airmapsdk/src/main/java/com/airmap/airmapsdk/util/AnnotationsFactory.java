package com.airmap.airmapsdk.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;

import com.airmap.airmapsdk.R;
import com.airmap.airmapsdk.models.Coordinate;
import com.airmap.airmapsdk.models.shapes.AirMapPolygon;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.MarkerViewOptions;
import com.mapbox.mapboxsdk.annotations.PolygonOptions;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.geometry.LatLng;

import java.util.ArrayList;

public class AnnotationsFactory {

    public static final String MIDPOINT_TAG = "midpoint";
    public static final String CORNER_TAG = "corner";
    public static final String INTERSECTION_TAG = "intersection";

    private static Icon cornerIcon;
    private static Icon midpointIcon;
    private static Icon intersectionIcon;

    private Context context;

    public AnnotationsFactory(Context context) {
        this.context = context;
        cornerIcon = IconFactory.getInstance(context).fromBitmap(getBitmapForDrawable(context, R.drawable.white_circle));
        midpointIcon = IconFactory.getInstance(context).fromBitmap(getBitmapForDrawable(context, R.drawable.gray_circle));
        intersectionIcon = IconFactory.getInstance(context).fromBitmap(getBitmapForDrawable(context, R.drawable.intersection_circle));
    }

    public static PolygonOptions getMapboxPolygon(AirMapPolygon airMapPolygon) {
        PolygonOptions polygonOptions = new PolygonOptions();
        for (Coordinate coordinate : airMapPolygon.getCoordinates()) {
            polygonOptions.add(new LatLng(coordinate.getLatitude(), coordinate.getLongitude()));
        }

        return polygonOptions;
    }

    public PolygonOptions getDefaultPolygonOptions() {
        PolygonOptions options = new PolygonOptions();
        options.fillColor(ContextCompat.getColor(context, R.color.airmap_colorFill));
        options.alpha(0.75f);
        return options;
    }

    private PolygonOptions getDefaultRedPolygonOptions() {
        PolygonOptions options = new PolygonOptions();
        options.fillColor(ContextCompat.getColor(context, R.color.status_red));
        options.alpha(0.66f);
        return options;
    }

    public PolylineOptions getDefaultPolylineOptions() {
        PolylineOptions options = new PolylineOptions();
        options.color(ContextCompat.getColor(context, R.color.colorPrimary));
        options.width(2);
        return options;
    }

    public MarkerViewOptions getDefaultMarkerOptions(LatLng latLng) {
        MarkerViewOptions options = new MarkerViewOptions();
        options.position(latLng);
        options.icon(cornerIcon);
        options.title(CORNER_TAG);
        options.anchor(0.5f, 0.5f);
        return options;
    }

    public MarkerViewOptions getDefaultMidpointMarker(LatLng latLng) {
        MarkerViewOptions options = new MarkerViewOptions();
        options.position(latLng);
        options.icon(midpointIcon);
        options.title(MIDPOINT_TAG);
        options.anchor(0.5f, 0.5f);
        return options;
    }

    public MarkerViewOptions getIntersectionMarker(LatLng latLng) {
        MarkerViewOptions options = new MarkerViewOptions();
        options.position(latLng);
        options.icon(intersectionIcon);
        options.title(INTERSECTION_TAG);
        options.anchor(0.5f, 0.5f);
        return options;
    }

    //Emulate a circle as a polygon with a bunch of sides
    public ArrayList<LatLng> polygonCircleForCoordinate(LatLng location, double radius) {
        int degreesBetweenPoints = 8;
        int numberOfPoints = (int) Math.floor(360 / degreesBetweenPoints);
        double distRadians = radius / 6371000.0; // earth radius in meters
        double centerLatRadians = location.getLatitude() * Math.PI / 180;
        double centerLonRadians = location.getLongitude() * Math.PI / 180;
        ArrayList<LatLng> polygons = new ArrayList<>(); //array to hold all the points
        for (int index = 0; index < numberOfPoints; index++) {
            double degrees = index * degreesBetweenPoints;
            double degreeRadians = degrees * Math.PI / 180;
            double pointLatRadians = Math.asin(Math.sin(centerLatRadians) * Math.cos(distRadians) + Math.cos(centerLatRadians) * Math.sin(distRadians) * Math.cos(degreeRadians));
            double pointLonRadians = centerLonRadians + Math.atan2(Math.sin(degreeRadians) * Math.sin(distRadians) * Math.cos(centerLatRadians),
                    Math.cos(distRadians) - Math.sin(centerLatRadians) * Math.sin(pointLatRadians));
            double pointLat = pointLatRadians * 180 / Math.PI;
            double pointLon = pointLonRadians * 180 / Math.PI;
            LatLng point = new LatLng(pointLat, pointLon);
            polygons.add(point);
        }
        return polygons;
    }

    public static Bitmap getBitmapForDrawable(Context context, @DrawableRes int id) {
        Drawable drawable = ContextCompat.getDrawable(context, id);
        Bitmap bitmap;
        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            bitmap = bitmapDrawable.getBitmap();
        } else {
            if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
                bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
            } else {
                bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            }

            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
        }
        return bitmap;
    }
}
