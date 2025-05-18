package com.springliviu.ivantask.logic;

import com.springliviu.ivantask.model.Edge;
import com.springliviu.ivantask.model.Point;

import java.util.*;

import static java.lang.Math.*;

public class FigureIdentifier {

    private static final double EPSILON = 1e-5;

    /**
     * Главный метод — определяет тип фигуры по заданным точкам и рёбрам.
     * Все переданные точки уже отсортированы так, как они будут отрисованы.
     */
    public static String identifyFigure(List<Point> points, List<Edge> edges) {
        // Удаляю повторяющиеся точки по координатам
        points = deduplicate(points);

        // Удаляю промежуточные точки, лежащие на одной прямой между соседями
        points = removeInlinePoints(points);

        // Простейшие случаи
        if (points.size() == 1) return "точка";
        if (points.size() == 2) return "отрезок";

        // Проверка: все точки на одной прямой — это фрагмент
        if (areAllPointsColinear(points)) return "фрагмент";

        // Проверка на самопересечения по заданным рёбрам
        if (hasSelfIntersections(points, edges)) {
            return "фигура с самопересечениями: " + getNameBySides(points.size());
        }

        // Анализ конкретных фигур
        return switch (points.size()) {
            case 3 -> "треугольник: " + classifyTriangle(points);
            case 4 -> "четырёхугольник: четырёхугольник";
            default -> getNameBySides(points.size());
        };
    }

    /**
     * Удаляю полностью дублирующиеся точки
     */
    private static List<Point> deduplicate(List<Point> points) {
        Set<String> seen = new HashSet<>();
        List<Point> result = new ArrayList<>();
        for (Point p : points) {
            String key = p.getX() + "," + p.getY();
            if (seen.add(key)) result.add(p);
        }
        return result;
    }

    /**
     * Удаляю точки, которые лежат строго на прямой между соседними (не влияют на фигуру)
     */
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

    /**
     * Проверяю, лежат ли все точки на одной прямой
     */
    private static boolean areAllPointsColinear(List<Point> points) {
        if (points.size() < 3) return true;
        Point a = points.get(0), b = points.get(1);
        for (int i = 2; i < points.size(); i++) {
            if (!isColinear(a, b, points.get(i))) return false;
        }
        return true;
    }

    /**
     * Проверка: три точки лежат на одной прямой
     */
    private static boolean isColinear(Point a, Point b, Point c) {
        return abs((b.getX() - a.getX()) * (c.getY() - a.getY()) -
                (b.getY() - a.getY()) * (c.getX() - a.getX())) < EPSILON;
    }

    /**
     * Классификация треугольника: прямоугольный, равнобедренный, разносторонний
     */
    private static String classifyTriangle(List<Point> t) {
        double a = distance(t.get(0), t.get(1));
        double b = distance(t.get(1), t.get(2));
        double c = distance(t.get(2), t.get(0));

        boolean isRight = isRightAngle(a, b, c);
        boolean isIsosceles = abs(a - b) < EPSILON || abs(b - c) < EPSILON || abs(a - c) < EPSILON;

        if (isRight) return "прямоугольный";
        if (isIsosceles) return "равнобедренный";
        return "разносторонний";
    }

    /**
     * Проверяю, есть ли прямой угол в треугольнике
     */
    private static boolean isRightAngle(double a, double b, double c) {
        double a2 = a * a, b2 = b * b, c2 = c * c;
        return abs(a2 + b2 - c2) < EPSILON ||
                abs(a2 + c2 - b2) < EPSILON ||
                abs(b2 + c2 - a2) < EPSILON;
    }

    private static double distance(Point p1, Point p2) {
        return sqrt(pow(p1.getX() - p2.getX(), 2) + pow(p1.getY() - p2.getY(), 2));
    }

    /**
     * Проверка на самопересечения — только между несмежными рёбрами
     */
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

    /**
     * Проверяю, являются ли рёбра смежными (соседними) или соединены одной вершиной
     */
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

    /**
     * Проверка пересечения двух отрезков (без касаний по вершинам)
     */
    private static boolean segmentsIntersect(Point a, Point b, Point c, Point d) {
        if (a.equals(c) || a.equals(d) || b.equals(c) || b.equals(d)) return false;

        return ccw(a, c, d) != ccw(b, c, d) &&
                ccw(a, b, c) != ccw(a, b, d);
    }

    private static boolean ccw(Point a, Point b, Point c) {
        return (c.getY() - a.getY()) * (b.getX() - a.getX()) >
                (b.getY() - a.getY()) * (c.getX() - a.getX());
    }

    /**
     * Название многоугольника по количеству сторон
     */
    private static String getNameBySides(int n) {
        return switch (n) {
            case 3 -> "треугольник";
            case 4 -> "четырёхугольник";
            case 5 -> "пятиугольник";
            case 6 -> "шестиугольник";
            default -> n + "-угольник";
        };
    }
}
