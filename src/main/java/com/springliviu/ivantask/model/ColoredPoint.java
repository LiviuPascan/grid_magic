package com.springliviu.ivantask.model;

public class ColoredPoint extends Point {
    private String color;

    public ColoredPoint(int x, int y, String color) {
        super(x, y);
        this.color = color;
    }

    public String getColor() { return color; }

    public void setColor(String color) { this.color = color; }
}
