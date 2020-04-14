package com.airmap.airmapsdk.util;

import android.graphics.PointF;

import androidx.annotation.Nullable;

import com.mapbox.mapboxsdk.geometry.LatLng;

import java.util.ArrayList;
import java.util.List;

public class PointMath {

    /**
     * TODO: test PolylineUtils#simplify instead
     * Simplify a polygon/path of points using the Ramer-Douglas-Peucker algorithm
     * @param vertices The points to simplify
     * @param distanceThreshold The threshold to simplify with
     * @return the simplified points
     */
    public static List<PointF> simplify(List<PointF> vertices, double distanceThreshold) {
        return vertices == null ? null : simplificationOf(vertices, distanceThreshold);
    }

    private static List<PointF> simplificationOf(List<PointF> vertices, double distanceThreshold) {
        List<PointF> simplifiedVertices = new ArrayList<>();
        Double maxDistance = null;
        int maxDistancePointFIdx = 0;
        int lastPointFIdx = vertices.size() - 1;
        int currentIdx = 0;
        for (PointF aVertex : vertices) {
            if (currentIdx != 0 && currentIdx != lastPointFIdx) {
                Double distance = shortestDistanceToSegment(aVertex, vertices.get(0), vertices.get(lastPointFIdx));
                if (maxDistance == null || distance > maxDistance) {
                    maxDistancePointFIdx = currentIdx;
                    maxDistance = distance;
                }
            }
            currentIdx++;
        }

        if (maxDistance != null) {
            if (maxDistance > distanceThreshold) {
                List<PointF> sub = simplify(vertices.subList(0, maxDistancePointFIdx + 1), distanceThreshold);
                List<PointF> sup = simplify(vertices.subList(maxDistancePointFIdx, lastPointFIdx + 1), distanceThreshold);

                simplifiedVertices.addAll(sub);
                simplifiedVertices.addAll(sup);

            } else {
                simplifiedVertices.add(vertices.get(0));
                simplifiedVertices.add(vertices.get(lastPointFIdx));
            }
        }
        return simplifiedVertices;
    }

    /**
     * Returns the shortest distance to a line segment
     * @param thePointF the point to calculate distance from
     * @param segmentPointF_A An endpoint of the segment to calculate distance to
     * @param segmentPointF_B The other endpoint of the segment to calculate distance to
     * @return The shortest distance to the segment
     */
    private static double shortestDistanceToSegment(PointF thePointF, PointF segmentPointF_A, PointF segmentPointF_B) {
        double area = calculateTriangleAreaGivenVertices(thePointF, segmentPointF_A, segmentPointF_B);
        double lengthSegment = distanceBetween(segmentPointF_A, segmentPointF_B);
        return (2 * area) / lengthSegment;
    }

    /**
     * Calculates the area of a triangle, given its vertices
     * @param a Point 1 of the triangle
     * @param b Point 2 of the triangle
     * @param c Point 3 of the triangle
     * @return the area of the triangle
     */
    private static double calculateTriangleAreaGivenVertices(PointF a, PointF b, PointF c) {
        return Math.abs(((a.x * (b.y - c.y)) + (b.x * (c.y - a.y)) + (c.x * (a.y - b.y))) / 2);
    }

    /**
     * Returns the distance between 2 points (using Pythagorean theorem)
     * @param a Point to calculate distance from
     * @param b Point to calculate distance to
     * @return the distance between the two points
     */
    public static double distanceBetween(PointF a, PointF b) {
        return Math.sqrt(((a.x - b.x) * (a.x - b.x)) + ((a.y - b.y) * (a.y - b.y)));
    }

    /**
     * Calculates midpoints of the line segments that make up a given shape
     * @param shape A polygon
     * @return a list of midpoints of the given shape
     */
    public static List<LatLng> getMidpointsFromLatLngs(List<LatLng> shape) {
        List<LatLng> midpoints = new ArrayList<>();
        for (int i = 1; i < shape.size(); i++) {
            double lat = (shape.get(i - 1).getLatitude() + shape.get(i).getLatitude()) / 2;
            double lng = (shape.get(i - 1).getLongitude() + shape.get(i).getLongitude()) / 2;
            midpoints.add(new LatLng(lat, lng));
        }
        return midpoints;
    }

    /**
     * Finds where a polygon has self intersections
     * @param polygon A list of vertices of the polygon
     * @return The intersections in the polygon (where 2 line segments meet)
     */
    public static List<LatLng> findIntersections(List<LatLng> polygon) {
        List<LatLng> intersections = new ArrayList<>();
        List<Line> lineSegments = new ArrayList<>();

        for (int i = 0; i < polygon.size() - 1; i++) {
            LatLng a = polygon.get(i);
            LatLng b = polygon.get(i + 1);
            lineSegments.add(new Line(a, b));
        }
        for (int i = 0; i < lineSegments.size(); i++) {
            for (int j = i+2; j < lineSegments.size(); j++) {
                LatLng intersection = lineIntersect(lineSegments.get(i), lineSegments.get(j));
                if (intersection != null) {
                    intersections.add(intersection);
                }
            }
        }
        for (LatLng polygonPoint : polygon) {
            //Remove points that are in the actual polygon cuz those aren't actually intersections
            intersections.remove(polygonPoint);
        }
        return intersections;
    }

    /**
     * Finds the intersection between 2 lines, if it exists
     * @param line1 Line 1 to check
     * @param line2 Line 2 to check
     * @return null if no intersection, otherwise the intersection point
     */
    @Nullable
    private static LatLng lineIntersect(Line line1, Line line2) {
        double x1 = line1.a.getLatitude();
        double y1 = line1.a.getLongitude();
        double x2 = line1.b.getLatitude();
        double y2 = line1.b.getLongitude();
        double x3 = line2.a.getLatitude();
        double y3 = line2.a.getLongitude();
        double x4 = line2.b.getLatitude();
        double y4 = line2.b.getLongitude();
        double denom = (y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1);
        if (denom == 0.0) { // Lines are parallel.
            return null;
        }
        double ua = ((x4 - x3) * (y1 - y3) - (y4 - y3) * (x1 - x3)) / denom;
        double ub = ((x2 - x1) * (y1 - y3) - (y2 - y1) * (x1 - x3)) / denom;
        if (ua >= 0.0f && ua <= 1.0f && ub >= 0.0f && ub <= 1.0f) {
            // Get the intersection point.
            return new LatLng((x1 + ua * (x2 - x1)), (y1 + ua * (y2 - y1)));
        }
        return null;
    }

    private static class Line {
        LatLng a, b;
        public Line(LatLng a, LatLng b) {
            this.a = a;
            this.b = b;
        }
    }
}