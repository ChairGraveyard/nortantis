package nortantis;

import hoten.voronoi.Center;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Set;

/**
 * Holds pieces of a map created while generating it which are needed for editing it.
 *
 */
public class MapParts
{
	public GraphImpl graph;
	public BufferedImage landBackground;
	public List<Set<Center>> mountainGroups;
	public TextDrawer textDrawer;
}