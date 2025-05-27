package com.springliviu.ivantask.logic;

import com.springliviu.ivantask.model.Edge;
import com.springliviu.ivantask.model.Point;

import java.util.*;

import static java.lang.Math.*;

public class FigureIdentifier {

    private static final double EPSILON = 1e-5;

    /**
     * Main method - determines the type of figure based on the given points and edges.
     * The input points are assumed to be ordered for drawing.
     */
    public static String identifyFigure(List<Point> points, List<Edge> edges) {
        points = deduplicate(points); // Remove duplicate points
        points = removeInlinePoints(points); // Remove collinear middle points

        if (points.size() == 1) return "point";
        if (points.size() == 2) return "segment";
        if (areAllPointsColinear(points)) return "fragment";

        if (hasSelfIntersections(points, edges)) {
            return "self-intersecting: " + getNameBySides(points.size());
        }

        return switch (points.size()) {
            case 3 -> "triangle: " + classifyTriangle(points);
            case 4 -> "quadrilateral: " + classifyQuadrilateral(points);
            default -> getNameBySides(points.size());
        };
    }

    private static List<Point> deduplicate(List<Point> points) {
        Set<String> seen = new HashSet<>();
        List<Point> result = new ArrayList<>();
        for (Point p : points) {
            String key = p.getX() + "," + p.getY();
            if (seen.add(key)) result.add(p);
        }
        return result;
    }

    private static List<Point> removeInlinePoints(List<Point> points) {
        if (points.size() < 3) return points;
        List<Point> filtered = new ArrayList<>();
        int n = points.size();
        for (int i = 0; i < n; i++) {
            Point prev = points.get((i - 1 + n) % n);
            Point curr = points.get(i);
            Point next = points.get((i + 1) % n);
            if (!isColinear(prev, curr, next)) {
                filtered.add(curr);
            }
        }
        return filtered;
    }

    private static boolean areAllPointsColinear(List<Point> points) {
        if (points.size() < 3) return true;
        Point a = points.get(0), b = points.get(1);
        for (int i = 2; i < points.size(); i++) {
            if (!isColinear(a, b, points.get(i))) return false;
        }
        return true;
    }

    private static boolean isColinear(Point a, Point b, Point c) {
        return abs((b.getX() - a.getX()) * (c.getY() - a.getY()) -
                (b.getY() - a.getY()) * (c.getX() - a.getX())) < EPSILON;
    }

    private static String classifyTriangle(List<Point> t) {
        double a = distance(t.get(0), t.get(1));
        double b = distance(t.get(1), t.get(2));
        double c = distance(t.get(2), t.get(0));

        boolean isRight = isRightAngle(a, b, c);
        boolean isIsosceles = abs(a - b) < EPSILON || abs(b - c) < EPSILON || abs(a - c) < EPSILON;

        if (isRight) return "right";
        if (isIsosceles) return "isosceles";
        return "scalene";
    }

    private static String classifyQuadrilateral(List<Point> points) {
        double[] lengths = new double[4];
        for (int i = 0; i < 4; i++) {
            lengths[i] = distance(points.get(i), points.get((i + 1) % 4));
        }

        boolean oppositeSidesEqual = abs(lengths[0] - lengths[2]) < EPSILON &&
                abs(lengths[1] - lengths[3]) < EPSILON;
        boolean allSidesEqual = Arrays.stream(lengths).allMatch(l -> abs(l - lengths[0]) < EPSILON);

        double[] angles = new double[4];
        for (int i = 0; i < 4; i++) {
            angles[i] = angleBetween(points.get((i + 3) % 4), points.get(i), points.get((i + 1) % 4));
        }

        boolean allRight = Arrays.stream(angles).allMatch(a -> abs(a - 90) < 2);
        boolean hasOnePairParallel = isParallel(points.get(0), points.get(1), points.get(2), points.get(3)) ||
                isParallel(points.get(1), points.get(2), points.get(3), points.get(0));

        if (allRight && allSidesEqual) return "square";
        if (allRight && oppositeSidesEqual) return "rectangle";
        if (!allRight && allSidesEqual) return "rhombus";
        if (hasOnePairParallel) return "trapezoid";

        return "general";
    }
    private static boolean isRightAngle(double a, double b, double c) {
        double a2 = a * a, b2 = b * b, c2 = c * c;
        return abs(a2 + b2 - c2) < EPSILON ||
                abs(a2 + c2 - b2) < EPSILON ||
                abs(b2 + c2 - a2) < EPSILON;
    }

    private static double angleBetween(Point a, Point b, Point c) {
        double abX = a.getX() - b.getX(), abY = a.getY() - b.getY();
        double cbX = c.getX() - b.getX(), cbY = c.getY() - b.getY();
        double dot = abX * cbX + abY * cbY;
        double cross = abX * cbY - abY * cbX;
        return Math.toDegrees(Math.atan2(Math.abs(cross), dot));
    }

    private static boolean isParallel(Point a1, Point a2, Point b1, Point b2) {
        double dx1 = a2.getX() - a1.getX();
        double dy1 = a2.getY() - a1.getY();
        double dx2 = b2.getX() - b1.getX();
        double dy2 = b2.getY() - b1.getY();
        return abs(dx1 * dy2 - dy1 * dx2) < EPSILON;
    }

    private static double distance(Point p1, Point p2) {
        return sqrt(pow(p1.getX() - p2.getX(), 2) + pow(p1.getY() - p2.getY(), 2));
    }

    private static boolean hasSelfIntersections(List<Point> points, List<Edge> edges) {
        for (int i = 0; i < edges.size(); i++) {
            Point a1 = points.get(edges.get(i).getFrom());
            Point a2 = points.get(edges.get(i).getTo());

            for (int j = i + 1; j < edges.size(); j++) {
                if (isAdjacent(edges, i, j)) continue;

                Point b1 = points.get(edges.get(j).getFrom());
                Point b2 = points.get(edges.get(j).getTo());

                if (segmentsIntersect(a1, a2, b1, b2)) return true;
            }
        }
        return false;
    }

    private static boolean isAdjacent(List<Edge> edges, int i, int j) {
        Edge e1 = edges.get(i);
        Edge e2 = edges.get(j);
        return Math.abs(i - j) == 1 ||
                (i == 0 && j == edges.size() - 1) ||
                sharesVertex(e1, e2);
    }

    private static boolean sharesVertex(Edge e1, Edge e2) {
        return e1.getFrom() == e2.getFrom() ||
                e1.getFrom() == e2.getTo() ||
                e1.getTo() == e2.getFrom() ||
                e1.getTo() == e2.getTo();
    }

    private static boolean segmentsIntersect(Point a, Point b, Point c, Point d) {
        if (a.equals(c) || a.equals(d) || b.equals(c) || b.equals(d)) return false;

        return ccw(a, c, d) != ccw(b, c, d) &&
                ccw(a, b, c) != ccw(a, b, d);
    }

    private static boolean ccw(Point a, Point b, Point c) {
        return (c.getY() - a.getY()) * (b.getX() - a.getX()) >
                (b.getY() - a.getY()) * (c.getX() - a.getX());
    }

    private static String getNameBySides(int n) {
        return switch (n) {
            case 3 -> "triangle";
            case 4 -> "quadrilateral";
            case 5 -> "pentagon";
            case 6 -> "hexagon";
            default -> n + "-gon";
        };
    }
}
