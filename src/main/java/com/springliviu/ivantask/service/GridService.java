package com.springliviu.ivantask.service;

import com.springliviu.ivantask.model.Point;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GridService {

    private static final int MIN_COORD = -5;
    private static final int MAX_COORD = 5;

    private final Random random = new Random();

    public Map<String, Object> generateFigure() {
        int numPoints = random.nextInt(6) + 1; // Случайное число от 1 до 6

        Set<String> usedCoordinates = new HashSet<>();
        List<Point> points = new ArrayList<>();

        // Генерация уникальных точек в пределах [-5; 5] по X и Y
        while (points.size() < numPoints) {
            int x = random.nextInt(MAX_COORD - MIN_COORD + 1) + MIN_COORD;
            int y = random.nextInt(MAX_COORD - MIN_COORD + 1) + MIN_COORD;
            String key = x + "," + y;

            if (!usedCoordinates.contains(key)) {
                points.add(new Point(x, y));
                usedCoordinates.add(key);
            }
        }

        // Сортируем точки по углу от центра масс, чтобы фигура отображалась правильно
        if (points.size() > 2) {
            sortPointsByAngle(points);
        }

        // Определяем название фигуры по количеству точек
        String type = switch (points.size()) {
            case 1 -> "point";
            case 2 -> "segment";
            case 3 -> "triangle";
            case 4 -> "quadrilateral";
            case 5 -> "pentagon";
            case 6 -> "hexagon";
            default -> "figure";
        };

        Map<String, Object> result = new HashMap<>();
        result.put("points", points);
        result.put("type", type);

        // Дополнительно: определение типа треугольника
        if (points.size() == 3) {
            String triangleType = getTriangleType(points);
            result.put("triangleType", triangleType);
        }

        // Дополнительно: определение типа четырёхугольника
        if (points.size() == 4) {
            String quadType = getQuadrilateralType(points);
            result.put("quadrilateralType", quadType);
        }

        return result;
    }

    // Сортировка точек по углу относительно центра — для правильного порядка рисования
    private void sortPointsByAngle(List<Point> points) {
        double centerX = points.stream().mapToDouble(Point::getX).average().orElse(0);
        double centerY = points.stream().mapToDouble(Point::getY).average().orElse(0);

        points.sort(Comparator.comparingDouble(p ->
                Math.atan2(p.getY() - centerY, p.getX() - centerX)));
    }

    // Определение типа треугольника по длинам сторон и углам
    private String getTriangleType(List<Point> pts) {
        double a = distance(pts.get(0), pts.get(1));
        double b = distance(pts.get(1), pts.get(2));
        double c = distance(pts.get(2), pts.get(0));

        if (equals(a, b) && equals(b, c)) return "равносторонний";
        if (isRightAngled(a, b, c)) return "прямоугольный";
        if (equals(a, b) || equals(b, c) || equals(c, a)) return "равнобедренный";

        return "разносторонний";
    }

    private boolean isDegenerateQuadrilateral(List<Point> pts) {
        // Если площадь четырёхугольника около нуля — это почти линия
        double area = Math.abs(
                pts.get(0).getX() * pts.get(1).getY() +
                        pts.get(1).getX() * pts.get(2).getY() +
                        pts.get(2).getX() * pts.get(3).getY() +
                        pts.get(3).getX() * pts.get(0).getY()
                        - pts.get(1).getX() * pts.get(0).getY()
                        - pts.get(2).getX() * pts.get(1).getY()
                        - pts.get(3).getX() * pts.get(2).getY()
                        - pts.get(0).getX() * pts.get(3).getY()
        ) / 2.0;

        return area < 1.0; // меньше 1 клетки — считаем вырожденным
    }

    // Определение типа четырёхугольника (включая трапецию)
    private String getQuadrilateralType(List<Point> pts) {
        double a = distance(pts.get(0), pts.get(1));
        double b = distance(pts.get(1), pts.get(2));
        double c = distance(pts.get(2), pts.get(3));
        double d = distance(pts.get(3), pts.get(0));

        boolean allSidesEqual = equals(a, b) && equals(b, c) && equals(c, d);
        boolean oppositeSidesEqual = equals(a, c) && equals(b, d);

        double angle1 = angleBetween(pts.get(0), pts.get(1), pts.get(2));
        double angle2 = angleBetween(pts.get(1), pts.get(2), pts.get(3));
        double angle3 = angleBetween(pts.get(2), pts.get(3), pts.get(0));
        double angle4 = angleBetween(pts.get(3), pts.get(0), pts.get(1));

        boolean allAnglesRight = isRightAngle(angle1) &&
                isRightAngle(angle2) &&
                isRightAngle(angle3) &&
                isRightAngle(angle4);
        if (isDegenerateQuadrilateral(pts)) {
            return "вырожденная фигура";
        }
        if (allSidesEqual && allAnglesRight) return "квадрат";
        if (oppositeSidesEqual && allAnglesRight) return "прямоугольник";
        if (allSidesEqual) return "ромб";

        // Проверка на трапецию: только одна пара сторон параллельна
        boolean abParallelCD = areParallel(pts.get(0), pts.get(1), pts.get(2), pts.get(3));
        boolean bcParallelDA = areParallel(pts.get(1), pts.get(2), pts.get(3), pts.get(0));

        if (abParallelCD ^ bcParallelDA) return "трапеция"; // XOR — только одна пара

        return "четырёхугольник";
    }

    // Проверка на параллельность двух отрезков (по наклону)
    private boolean areParallel(Point p1, Point p2, Point p3, Point p4) {
        double dx1 = p2.getX() - p1.getX();
        double dy1 = p2.getY() - p1.getY();
        double dx2 = p4.getX() - p3.getX();
        double dy2 = p4.getY() - p3.getY();

        if (dx1 == 0 && dx2 == 0) return true; // обе вертикальны
        if (dx1 == 0 || dx2 == 0) return false; // одна вертикальна — не параллельны

        double slope1 = dy1 / dx1;
        double slope2 = dy2 / dx2;

        return Math.abs(slope1 - slope2) < 0.01; // допуск на погрешность
    }

    // Расстояние между двумя точками
    private double distance(Point p1, Point p2) {
        return Math.hypot(p1.getX() - p2.getX(), p1.getY() - p2.getY());
    }

    // Проверка равенства двух чисел с учетом погрешности
    private boolean equals(double a, double b) {
        return Math.abs(a - b) < 0.01;
    }

    // Проверка на прямоугольность треугольника по Пифагору
    private boolean isRightAngled(double a, double b, double c) {
        double[] sides = {a, b, c};
        Arrays.sort(sides);
        return equals(sides[0] * sides[0] + sides[1] * sides[1], sides[2] * sides[2]);
    }

    // Вычисление угла между двумя сторонами, исходящими из общей точки
    private double angleBetween(Point a, Point b, Point c) {
        double[] v1 = {b.getX() - a.getX(), b.getY() - a.getY()};
        double[] v2 = {c.getX() - b.getX(), c.getY() - b.getY()};

        double dot = v1[0] * v2[0] + v1[1] * v2[1];
        double len1 = Math.hypot(v1[0], v1[1]);
        double len2 = Math.hypot(v2[0], v2[1]);

        return Math.acos(dot / (len1 * len2));
    }

    // Проверка, близок ли угол к 90°
    private boolean isRightAngle(double angle) {
        return Math.abs(Math.toDegrees(angle) - 90) < 5;
    }
}
