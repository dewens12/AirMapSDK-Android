package com.airmap.airmapsdk.models;

import android.content.Context;

import androidx.core.content.ContextCompat;

import com.airmap.airmapsdk.R;
import com.airmap.airmapsdk.util.PointMath;
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
import com.mapbox.services.api.utils.turf.TurfJoins;
import com.mapbox.services.commons.geojson.Feature;
import com.mapbox.services.commons.geojson.LineString;
import com.mapbox.services.commons.geojson.MultiPoint;
import com.mapbox.services.commons.geojson.Point;
import com.mapbox.services.commons.geojson.Polygon;
import com.mapbox.services.commons.models.Position;

import java.util.ArrayList;
import java.util.List;

public class PolygonContainer extends Container {

    private List<LatLng> path;
    private List<LatLng> midpoints;
    private Polygon polygon;

    public PolygonContainer(Context context, MapboxMap map) {
        super(context, map);
    }

    public void drawOnMap(List<LatLng> path, Polygon polygon) {
        this.path = path;
        this.polygon = polygon;
        this.midpoints = PointMath.getMidpointsFromLatLngs(path);

        List<Position> positions = latLngsToPositions(path);
        List<Position> midPositions = latLngsToPositions(midpoints);
        List<Position> lineString = new ArrayList<>(positions);

        // if polygon layer doesn't exist, create and add to map
        if (map.getLayer(POINT_LAYER) == null) {
            Source pointSource = new GeoJsonSource(POINT_SOURCE, Feature.fromGeometry(MultiPoint.fromCoordinates(positions)));
            map.addSource(pointSource);
            Layer pointLayer = new SymbolLayer(POINT_LAYER, POINT_SOURCE)
                    .withProperties(PropertyFactory.iconImage(CORNER_IMAGE));
            map.addLayer(pointLayer);

            Source midpointSource = new GeoJsonSource(MIDPOINT_SOURCE, Feature.fromGeometry(MultiPoint.fromCoordinates(midPositions)));
            map.addSource(midpointSource);
            Layer midpointLayer = new SymbolLayer(MIDPOINT_LAYER, MIDPOINT_SOURCE)
                    .withProperties(PropertyFactory.iconImage(MIDPOINT_IMAGE));
            map.addLayer(midpointLayer);

            Source polygonSource = new GeoJsonSource(POLYGON_SOURCE, Feature.fromGeometry(polygon));
            map.addSource(polygonSource);
            Layer polygonLayer = new FillLayer(POLYGON_LAYER, POLYGON_SOURCE)
                    .withProperties(PropertyFactory.fillColor(ContextCompat.getColor(context, R.color.colorAccent)), PropertyFactory.fillOpacity(0.5f));
            map.addLayerBelow(polygonLayer, POINT_LAYER);

            Source polylineSource = new GeoJsonSource(POLYLINE_SOURCE, Feature.fromGeometry(LineString.fromCoordinates(lineString)));
            map.addSource(polylineSource);
            Layer polylineLayer = new LineLayer(POLYLINE_LAYER, POLYLINE_SOURCE)
                    .withProperties(PropertyFactory.lineColor(ContextCompat.getColor(context, R.color.colorPrimary)), PropertyFactory.lineOpacity(0.9f));
            map.addLayerAbove(polylineLayer, POLYGON_LAYER);

        // otherwise, update source
        } else {
            GeoJsonSource pointsSource = map.getSourceAs(POINT_SOURCE);
            pointsSource.setGeoJson(Feature.fromGeometry(MultiPoint.fromCoordinates(positions)));

            GeoJsonSource midpointsSource = map.getSourceAs(MIDPOINT_SOURCE);
            midpointsSource.setGeoJson(Feature.fromGeometry(MultiPoint.fromCoordinates(midPositions)));

            map.removeLayer(INTERSECTION_LAYER);
            map.removeSource(INTERSECTION_SOURCE);

            GeoJsonSource polygonSource = map.getSourceAs(POLYGON_SOURCE);
            polygonSource.setGeoJson(Feature.fromGeometry(polygon));

            FillLayer polygonFill = map.getLayerAs(Container.POLYGON_LAYER);
            polygonFill.setProperties(PropertyFactory.fillColor(ContextCompat.getColor(context, R.color.colorAccent)));

            GeoJsonSource polylineSource = map.getSourceAs(POLYLINE_SOURCE);
            polylineSource.setGeoJson(Feature.fromGeometry(LineString.fromCoordinates(lineString)));
        }
    }

    public boolean checkForIntersections() {
        List<LatLng> points = PointMath.findIntersections(path);
        if (points.isEmpty()) {
            return false;
        }

        List<Position> intersections = latLngsToPositions(points);
        if (map.getLayer(INTERSECTION_LAYER) == null) {
            Source intersectionSource = new GeoJsonSource(INTERSECTION_SOURCE, Feature.fromGeometry(MultiPoint.fromCoordinates(intersections)));
            map.addSource(intersectionSource);
            Layer intersectionLayer = new SymbolLayer(INTERSECTION_LAYER, INTERSECTION_SOURCE)
                    .withProperties(PropertyFactory.iconImage(INTERSECTION_IMAGE));
            map.addLayer(intersectionLayer);
        } else {
            GeoJsonSource intersectionsSource = map.getSourceAs(INTERSECTION_SOURCE);
            intersectionsSource.setGeoJson(Feature.fromGeometry(MultiPoint.fromCoordinates(intersections)));
        }

        return true;
    }

    @Override
    public LatLngBounds getLatLngBoundsForZoom() {
        LatLngBounds.Builder latLngBounds = new LatLngBounds.Builder();
        for (List<Position> list : polygon.getCoordinates()) {
            for (Position position : list) {
                latLngBounds.include(new LatLng(position.getLatitude(), position.getLongitude()));
            }
        }
        return latLngBounds.build();
    }

    @Override
    public void clear() {
        path = null;
        polygon = null;
        midpoints = null;

        map.removeLayer(POINT_LAYER);
        map.removeSource(POINT_SOURCE);

        map.removeLayer(MIDPOINT_LAYER);
        map.removeSource(MIDPOINT_SOURCE);

        map.removeLayer(INTERSECTION_LAYER);
        map.removeSource(INTERSECTION_SOURCE);

        map.removeLayer(POLYGON_LAYER);
        map.removeSource(POLYGON_SOURCE);

        map.removeLayer(POLYLINE_LAYER);
        map.removeSource(POLYLINE_SOURCE);
    }

    public boolean isTooComplex() {
        return path.size() > 18;
    }

    public boolean isValid() {
        return polygon != null && PointMath.findIntersections(path).isEmpty() && !isTooComplex();
    }

    public LatLng[] getNeighborPoints(LatLng point) {
        LatLng closestPoint = getClosestPointOrMidpoint(point);
        int index = path.indexOf(closestPoint);

        // midpoint
        if (index == -1) {
            index = midpoints.indexOf(closestPoint);
            if (index == 0) {
                return new LatLng[]{path.get(0), path.get(1)};
            } else {
                return new LatLng[]{path.get(index), path.get(index + 1)};
            }

        // point
        } else {
            if (index == 0) {
                return new LatLng[]{path.get(path.size() - 2), path.get(index + 1)};
            } else if (index == path.size() - 2) {
                return new LatLng[]{path.get(index - 1), path.get(index + 1)};
            } else {
                return new LatLng[]{path.get(index - 1), path.get(index + 1)};
            }
        }
    }

    private LatLng getClosestPointOrMidpoint(LatLng latLng) {
        double shortestDistance = Double.MAX_VALUE;
        LatLng closestPoint = null;

        List<LatLng> allPoints = new ArrayList<>(path);
        allPoints.addAll(midpoints);
        for (LatLng pathPoint : allPoints) {
            double dist = latLng.distanceTo(pathPoint);
            if (closestPoint == null || dist < shortestDistance) {
                closestPoint = pathPoint;
                shortestDistance = dist;
            }
        }

        return closestPoint;
    }

    public List<LatLng> replacePoint(LatLng fromLatLng, LatLng toLatLng) {
        List<LatLng> newPoints = new ArrayList<>(path);
        LatLng closestPoint = getClosestPointOrMidpoint(fromLatLng);
        int index = path.indexOf(closestPoint);
        if (index == -1) {
            index = midpoints.indexOf(closestPoint);
            newPoints.add(index + 1, toLatLng);
        } else {
            // treat first/last point special
            if (index == 0 || index == path.size() - 1) {
                newPoints.remove(path.size() - 1);
                newPoints.remove(0);
                newPoints.add(0, toLatLng);
                newPoints.add(toLatLng);
            } else {
                newPoints.remove(index);
                newPoints.add(index, toLatLng);
            }
        }

        return newPoints;
    }

    public List<LatLng> deletePoint(LatLng latLng) {
        List<LatLng> newPoints = new ArrayList<>(path);
        LatLng closestPoint = getClosestPointOrMidpoint(latLng);

        int index = newPoints.indexOf(closestPoint);
        if (index > 0) {
            newPoints.remove(index);
        } else if (index == 0) {
            newPoints.remove(path.size() - 1);
            newPoints.remove(0);
            LatLng newEnd = newPoints.get(0);
            newPoints.add(newEnd);
        }

        return newPoints;
    }

    //TODO: this isn't the real way to find visual center (see mapbox's visual center)
    public LatLng getVisualCenter() {
        LatLngBounds latLngBounds = new LatLngBounds.Builder().includes(path).build();

        LatLng center = latLngBounds.getCenter();
        if (TurfJoins.inside(Point.fromCoordinates(Position.fromLngLat(center.getLongitude(), center.getLatitude())), polygon)) {
            return center;
        }

        return path.get(0);
    }

    public List<LatLng> getPath() {
        return path;
    }

    public List<LatLng> getMidpoints() {
        return midpoints;
    }

    public Polygon getPolygon() {
        return polygon;
    }
}
