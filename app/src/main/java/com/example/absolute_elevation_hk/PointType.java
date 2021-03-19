package com.example.absolute_elevation_hk;

public class PointType {
    double x = 0;
    double y = 0;
    double p = 0;

    public static PointType init(double x, double y, double p) {
        PointType tempPoint = new PointType();
        tempPoint.setP(p);
        tempPoint.setX(x);
        tempPoint.setY(y);
        return  tempPoint;
    }

    public double getP() {
        return p;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public void setP(double p) {
        this.p = p;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }


}
