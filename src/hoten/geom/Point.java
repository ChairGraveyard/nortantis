package hoten.geom;

import java.io.Serializable;


/**
 * Point.java
 *
 * @author Connor
 */
@SuppressWarnings("serial")
public class Point implements Comparable<Point>, Serializable
{

    public static double distance(Point _coord, Point _coord0) {
        return Math.sqrt((_coord.x - _coord0.x) * (_coord.x - _coord0.x) + (_coord.y - _coord0.y) * (_coord.y - _coord0.y));
    }
    public double x, y;

    public Point(double x, double y) {
        this.x = x;
        this.y = y;
    }
        
    /**
     * Returns a new point whose value is this point minus other.
     */
    public Point subtract(Point other)
    {
    	return new Point(x - other.x, y - other.y);
    }

    @Override
    public String toString() {
        return x + ", " + y;
    }

    public double l2() {
        return x * x + y * y;
    }

    public double length() {
        return Math.sqrt(x * x + y * y);
    }
    
    public static Point interpolate(Point p1, Point p2, double c)
    {
    	return new Point(c * (p1.x) + (1 - c) * p2.x, c * (p1.y)+ (1 - c) * p2.y);
    }
    
	@Override
	public int compareTo(Point other)
	{
		int c1 = Double.compare(x, other.x);
		if (c1 < 0)
			return -1;
		if (c1 > 0)
			return 1;
		
		int c2 = Double.compare(y, other.y);
		if (c2 < 0)
			return -1;
		if (c2 > 0)
			return 1;
	
		return 0;
	}
}
