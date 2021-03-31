package weiner.noah.noshake.posenet.test;

import org.opencv.core.Point;

public class KeyFeature {
    private Point pt;
    private double xDiff;
    private double yDiff;


    private int valid;

    //the overall "vector" of displacement (just the length of hypotenuse of displacement triangle)
    private double dispVect;


    public KeyFeature(Point coords, double xDisp, double yDisp, double totalDisp) {
        this.pt = coords;
        this.xDiff = xDisp;
        this.yDiff = yDisp;
        this.dispVect = totalDisp;
        this.valid = 1;
    }

    public Point getPt() {
        return pt;
    }

    public double getxDiff() {
        return xDiff;
    }

    public double getyDiff() {
        return yDiff;
    }

    public double getDispVect() {
        return dispVect;
    }

    public boolean isValid() {
        return this.valid == 1;
    }

    public void setValid(int n) {
        this.valid = n;
    }
}
