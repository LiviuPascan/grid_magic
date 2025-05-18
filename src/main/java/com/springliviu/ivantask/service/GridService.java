package com.springliviu.ivantask.service;

import com.springliviu.ivantask.model.Point;
import com.springliviu.ivantask.model.ColoredPoint;
import com.springliviu.ivantask.model.Edge;
import com.springliviu.ivantask.logic.FigureIdentifier;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GridService {

    private static final int MIN_COORD = -5;
    private static final int MAX_COORD = 5;
    private final Random random = new Random();

    public Map<String, Object> generateFigure() {
        int numPoints = random.nextInt(6) + 1; // от 1 до 6 включительно
        Set<String> used = new HashSet<>();
        List<Point> originalPoints = new ArrayList<>();

        // Генерация уникальных точек
        while (originalPoints.size() < numPoints) {
            int x = random.nextInt(MAX_COORD - MIN_COORD + 1) + MIN_COORD;
            int y = random.nextInt(MAX_COORD - MIN_COORD + 1) + MIN_COORD;
            if (used.add(x + "," + y)) {
                originalPoints.add(new Point(x, y));
            }
        }

        // Центрируем точки ближе к центру
        centerPoints(originalPoints);

        // Если ≥ 3, сортируем по углу для красивой отрисовки
        List<Integer> drawOrder = originalPoints.size() >= 3
                ? sortByAngle(originalPoints)
                : defaultOrder(originalPoints.size());

        // Готовим точки для отрисовки
        String[] colors = {"red", "green", "blue", "orange", "magenta", "black", "cyan"};
        List<ColoredPoint> visualPoints = new ArrayList<>();
        List<Point> orderedForAnalysis = new ArrayList<>();
        for (int i = 0; i < drawOrder.size(); i++) {
            Point p = originalPoints.get(drawOrder.get(i));
            visualPoints.add(new ColoredPoint(p.getX(), p.getY(), colors[i % colors.length]));
            orderedForAnalysis.add(p);
        }

        // Строим ребра
        List<Edge> edges = new ArrayList<>();
        for (int i = 0; i < orderedForAnalysis.size() - 1; i++) {
            edges.add(new Edge(i, i + 1));
        }
        if (orderedForAnalysis.size() >= 3) {
            edges.add(new Edge(orderedForAnalysis.size() - 1, 0)); // замыкаем
        }

        // Определяем тип фигуры
        String type = FigureIdentifier.identifyFigure(orderedForAnalysis, edges);


        Map<String, Object> result = new HashMap<>();
        result.put("points", visualPoints);
        result.put("edges", edges);
        result.put("type", type);
        return result;
    }

    private void centerPoints(List<Point> points) {
        for (Point p : points) {
            if (p.getX() >= 5) p.setX(p.getX() - 2);
            if (p.getY() >= 5) p.setY(p.getY() - 2);
        }
    }

    private List<Integer> sortByAngle(List<Point> points) {
        double cx = points.stream().mapToInt(Point::getX).average().orElse(0);
        double cy = points.stream().mapToInt(Point::getY).average().orElse(0);
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < points.size(); i++) indices.add(i);
        indices.sort(Comparator.comparingDouble(i ->
                Math.atan2(points.get(i).getY() - cy, points.get(i).getX() - cx)));
        return indices;
    }

    private List<Integer> defaultOrder(int n) {
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < n; i++) order.add(i);
        return order;
    }
}
