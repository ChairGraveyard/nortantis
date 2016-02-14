package nortantis;

import hoten.geom.Point;

import java.awt.geom.Area;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Stores a piece of text (and data about it) drawn onto a map.
 * @author joseph
 *
 */
@SuppressWarnings("serial")
public class MapText implements Serializable
{
	String value;
	/**
	 * The (possibly rotated) bounding boxes of the text. This only has size 2 if the text has 2 lines.
	 */
	transient List<Area> areas;
	
	TextType type;
	/**
	 * If the user has rotated the text, then this stores the angle. 0 means horizontal.
	 */
	double angle;
	/**
	 * If the user has moved the text, then this store the location. null means let the generator determine the location.
	 * For text that can be rotated, the text will be draw such that the center of it's bounding box is at this location.
	 * For text that cannot be rotated (title and region names), the bounding box of the text will be determined by
	 * font metrics added to this location.
	 */
	Point location;
	
	public MapText(String text, Point location, double angle, TextType type, List<Area> areas)
	{
		this.value = text;
		this.areas = areas;
		this.location = location;
		this.angle = angle;
		this.type = type;
	}

	public MapText(String text, Point location, double angle, TextType type)
	{
		this(text, location, angle, type, new ArrayList<Area>(0));
	}

	@Override
	public String toString()
	{
		return "MapText [value=" + value + ", type=" + type + ", angle=" + angle + ", location="
				+ location + "]";
	}

}