package com.airmap.airmapsdk.models;

import android.content.Context;
import android.text.TextUtils;

import androidx.core.content.ContextCompat;

import com.airmap.airmapsdk.Analytics;
import com.airmap.airmapsdk.R;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.style.layers.FillLayer;
import com.mapbox.mapboxsdk.style.layers.Layer;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.mapboxsdk.style.sources.Source;
import com.mapbox.services.commons.geojson.Feature;
import com.mapbox.services.commons.geojson.LineString;
import com.mapbox.services.commons.geojson.Point;
import com.mapbox.services.commons.geojson.Polygon;
import com.mapbox.services.commons.models.Position;

import java.util.ArrayList;
import java.util.List;

public class CircleContainer extends Container {

    private LatLng center;
    private double radius;
    private List<LatLng> points;

    public CircleContainer(Context context, MapboxMap map) {
        super(context, map);
    }

    public void drawOnMap(LatLng center, double radius) {
        this.center = center;
        this.radius = radius;
        this.points = calculateCirclePoints(center, radius);

        List<Position> positions = latLngsToPositions(points);
        List<List<Position>> coordinates = new ArrayList<>();
        coordinates.add(positions);

        List<Position> lineString = new ArrayList<>(positions);
        lineString.add(positions.get(0));

        // if polygon layer doesn't exist, create and add to map
        if (map.getLayer(POINT_LAYER) == null) {
            Source pointSource = new GeoJsonSource(POINT_SOURCE, Feature.fromGeometry(Point.fromCoordinates(Position.fromLngLat(center.getLongitude(), center.getLatitude()))));
            map.addSource(pointSource);
            Layer pointLayer = new SymbolLayer(POINT_LAYER, POINT_SOURCE)
                .withProperties(PropertyFactory.iconImage(CORNER_IMAGE));
            map.addLayer(pointLayer);

            Source polygonSource = new GeoJsonSource(POLYGON_SOURCE, Feature.fromGeometry(Polygon.fromCoordinates(coordinates)));
            map.addSource(polygonSource);
            FillLayer polygonLayer = new FillLayer(POLYGON_LAYER, POLYGON_SOURCE)
                    .withProperties(PropertyFactory.fillColor(ContextCompat.getColor(context, R.color.colorAccent)), PropertyFactory.fillOpacity(0.5f));
            map.addLayerBelow(polygonLayer, POINT_LAYER);

            Source polylineSource = new GeoJsonSource(POLYLINE_SOURCE, Feature.fromGeometry(LineString.fromCoordinates(lineString)));
            map.addSource(polylineSource);
            Layer polylineLayer = new LineLayer(POLYLINE_LAYER, POLYLINE_SOURCE)
                    .withProperties(PropertyFactory.lineColor(ContextCompat.getColor(context, R.color.colorPrimary)), PropertyFactory.lineOpacity(0.9f));
            map.addLayerAbove(polylineLayer, POLYGON_LAYER);

        // otherwise, update source
        } else {
            GeoJsonSource polygonSource = map.getSourceAs(POLYGON_SOURCE);
            polygonSource.setGeoJson(Feature.fromGeometry(Polygon.fromCoordinates(coordinates)));

            FillLayer polygonFill = map.getLayerAs(Container.POLYGON_LAYER);
            polygonFill.setProperties(PropertyFactory.fillColor(ContextCompat.getColor(context, R.color.colorAccent)));

            GeoJsonSource polylineSource = map.getSourceAs(POLYLINE_SOURCE);
            polylineSource.setGeoJson(Feature.fromGeometry(LineString.fromCoordinates(lineString)));
        }
    }

    public void move(LatLng center) {
        this.center = center;
        this.points = calculateCirclePoints(center, radius);

        List<Position> positions = latLngsToPositions(points);
        List<List<Position>> coordinates = new ArrayList<>();
        coordinates.add(positions);

        List<Position> lineString = new ArrayList<>(positions);
        lineString.add(positions.get(0));

        GeoJsonSource pointSource = map.getSourceAs(POINT_SOURCE);
        pointSource.setGeoJson(Feature.fromGeometry(Point.fromCoordinates(Position.fromLngLat(center.getLongitude(), center.getLatitude()))));

        GeoJsonSource polygonSource = map.getSourceAs(POLYGON_SOURCE);
        polygonSource.setGeoJson(Feature.fromGeometry(Polygon.fromCoordinates(coordinates)));

        FillLayer polygonFill = map.getLayerAs(Container.POLYGON_LAYER);
        polygonFill.setProperties(PropertyFactory.fillColor(ContextCompat.getColor(context, R.color.colorAccent)));

        GeoJsonSource polylineSource = map.getSourceAs(POLYLINE_SOURCE);
        polylineSource.setGeoJson(Feature.fromGeometry(LineString.fromCoordinates(lineString)));
    }

    @Override
    public LatLngBounds getLatLngBoundsForZoom() {
        Analytics.logDebug("center", center.toString());
        Analytics.logDebug("points", TextUtils.join(" - ", points));
        return new LatLngBounds.Builder().includes(points).build();
    }

    @Override
    public void clear() {
        center = null;
        points = null;

        map.removeLayer(POINT_LAYER);
        map.removeSource(POINT_SOURCE);

        map.removeLayer(POLYGON_LAYER);
        map.removeSource(POLYGON_SOURCE);

        map.removeLayer(POLYLINE_LAYER);
        map.removeSource(POLYLINE_SOURCE);
    }

    public boolean isValid() {
        return center != null && points != null;
    }

    public void setCenter(LatLng center) {
        this.center = center;
    }

    public LatLng getCenter() {
        return center;
    }

    public double getRadius() {
        return radius;
    }

    public List<LatLng> getPoints() {
        return points;
    }

    public static ArrayList<LatLng> calculateCirclePoints(LatLng location, double radius) {
        int degreesBetweenPoints = 8;
        int numberOfPoints = (int) Math.floor(360 / degreesBetweenPoints);
        double distRadians = radius / 6371000.0; // earth radius in meters
        double centerLatRadians = location.getLatitude() * Math.PI / 180;
        double centerLonRadians = location.getLongitude() * Math.PI / 180;
        ArrayList<LatLng> polygons = new ArrayList<>(); //array to hold all the path
        for (int index = 0; index < numberOfPoints; index++) {
            double degrees = index * degreesBetweenPoints;
            double degreeRadians = degrees * Math.PI / 180;
            double pointLatRadians = Math.asin(Math.sin(centerLatRadians) * Math.cos(distRadians) + Math.cos(centerLatRadians) * Math.sin(distRadians) * Math.cos(degreeRadians));
            double pointLonRadians = centerLonRadians + Math.atan2(Math.sin(degreeRadians) * Math.sin(distRadians) * Math.cos(centerLatRadians),
                    Math.cos(distRadians) - Math.sin(centerLatRadians) * Math.sin(pointLatRadians));
            double pointLat = pointLatRadians * 180 / Math.PI;
            double pointLon = pointLonRadians * 180 / Math.PI;
            polygons.add(new LatLng(pointLat, pointLon));
        }
        return polygons;
    }
}
