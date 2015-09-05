package nortantis;

import hoten.geom.Point;
import hoten.voronoi.Center;
import hoten.voronoi.Corner;
import hoten.voronoi.Edge;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.math3.stat.regression.SimpleRegression;

import util.Helper;
import util.ImageHelper;
import util.Pair;
import util.Range;

public class TextDrawer
{
	private MapSettings settings;
	private final int mountainRangeMinSize = 50;
	// y offset added to names of mountain groups smaller than a range.
	private final double mountainGroupYOffset = 25;
	private final int backGroundBlendKernelBaseSize = 10;
	// How big a river must be , in terms of edge.river, to be considered for labeling.
	private final int riverMinWidth = 3;
	// Rivers shorter than this will not be named. This must be at least 3.
	private final int riverMinLength = 3;
	// This is how far away from a river it's name will be drawn.
	private final double riverNameRiseHeight = -12;
	private final double maxWordLengthComparedToAverage = 2.0;
	private final double thresholdForPuttingTitleOnLand = 0.3;
	
	private BufferedImage landAndOceanBackground;
	private List<Area> textBounds;
	Random r;
	private NameGenerator nameGenerator;
	private NameCompiler nameCompiler;
	Area graphBounds;
	private double sizeMultiplyer;
	
	public TextDrawer(MapSettings settings, double sizeMultiplyer)
	{
		this.settings = settings;
		this.sizeMultiplyer = sizeMultiplyer;
		textBounds = new ArrayList<>();
		// I create a new Random instead of passing one in so that small differences in the way 
		// the random number generator is used previous to the TextDrawer do not change the text.
		this.r = new Random(settings.textRandomSeed);
				
		List<String> placeNames = new ArrayList<>();
		List<Pair<String, String>> nounAdjectivePairs = new ArrayList<>();
		List<Pair<String, String>> nounVerbPairs = new ArrayList<>();
		for (String book : settings.books)
		{
			String placeNameFilename = "assets/books/" + book + "_place_names.txt";
			try
			{				
				try(BufferedReader br = new BufferedReader(new FileReader(new File(placeNameFilename)))) 
				{
				    for(String line; (line = br.readLine()) != null; )
				    {
				    	// Remove white space lines.
				        if (!line.trim().isEmpty())
				        {
				        	placeNames.add(line);
				        }
				    }
				}
			} 
			catch (IOException e)
			{
				throw new RuntimeException("Unable to read place name file " + placeNameFilename + ". Is \"" + book + "\" a valid book?", e);
			}
			nounAdjectivePairs.addAll(readStringPairs("assets/books/" + book + "_noun_adjective_pairs.txt"));
			nounVerbPairs.addAll(readStringPairs("assets/books/" + book + "_noun_verb_pairs.txt"));
		}
				
		this.nameGenerator = new NameGenerator(r, placeNames, maxWordLengthComparedToAverage);

			
		nameCompiler = new NameCompiler(r, nounAdjectivePairs, nounVerbPairs);
		
		// Scale the fonts based on settings.size.
		settings.titleFont = settings.titleFont.deriveFont(settings.titleFont.getStyle(),
				(int)(settings.titleFont.getSize() * sizeMultiplyer));
		settings.regionFont = settings.regionFont.deriveFont(settings.regionFont.getStyle(),
				(int)(settings.regionFont.getSize() * sizeMultiplyer));
		settings.mountainRangeFont = settings.mountainRangeFont.deriveFont(settings.mountainRangeFont.getStyle(),
				(int)(settings.mountainRangeFont.getSize() * sizeMultiplyer));
		settings.otherMountainsFont = settings.otherMountainsFont.deriveFont(settings.otherMountainsFont.getStyle(),
				(int)(settings.otherMountainsFont.getSize() * sizeMultiplyer));
		settings.riverFont = settings.riverFont.deriveFont(settings.riverFont.getStyle(),
				(int)(settings.riverFont.getSize() * sizeMultiplyer));

		
//		out.println("noun adjective pairs: ");
//		Helper.printMultiLine(nounAdjectivePairs);
//		out.println("noun verb pairs: ");
//		Helper.printMultiLine(nounVerbPairs);
	}
	
	private List<Pair<String, String>> readStringPairs(String filename)
	{
		List<Pair<String, String>> result = new ArrayList<>();
		try(BufferedReader br = new BufferedReader(new FileReader(new File(filename)))) 
		{
			int lineNum = 0;
		    for(String line; (line = br.readLine()) != null; ) 
		    {
		    	lineNum++;
		    	
		    	// Remove white space lines.
		        if (!line.trim().isEmpty())
		        {
		        	String[] parts = line.split("\t");
		        	if (parts.length != 2)
		        	{
		        		System.out.println("Warning: No string pair found in " + filename + " at line " + lineNum + ".");
		        		continue;
		        	}
		        	result.add(new Pair<>(parts[0], parts[1]));
		        }
		    }
		} 
		catch (FileNotFoundException e)
		{
			throw new RuntimeException(e);
		} 
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}	
		
		return result;
	}
	
	public void drawText(GraphImpl graph, BufferedImage map, BufferedImage landAndOceanBackground,
			List<Set<Center>> mountainRanges)
	{				
		// All text drawn must be done so in order from highest to lowest priority because if I try to draw
		// text on top of other text, the latter will not be displayed.
		
		this.landAndOceanBackground = landAndOceanBackground;
		graphBounds = new Area(new java.awt.Rectangle(0, 0, graph.getWidth(), graph.getHeight()));

		Graphics2D g = map.createGraphics();
		
		g.setFont(settings.titleFont);
		g.setColor(settings.textColor);
		
		addTitle(map, graph, g);
		
		g.setFont(settings.regionFont);
		for (TectonicPlate plate : graph.plates)
		{
			if (plate.type == PlateType.Continental)
			{
				Set<Center> plateCenters = findPlateCentersLandOnly(graph, plate);
				Set<Point> locations = extractLocationsFromCenters(plateCenters);
				// This check is necessary because I'm doing a regression of the center locations.
				// The regression requires at least 3 points.
				if (plateCenters.size() >= 3)
				{
					drawNameHorizontal(map, g, nameGenerator.generateName(), locations, graph, true);
				}
			}
		}
		
		for (Set<Center> mountainRange : mountainRanges)
		{
			if (mountainRange.size() >= mountainRangeMinSize)
			{
				g.setFont(settings.mountainRangeFont);
				Set<Point> locations = extractLocationsFromCenters(mountainRange);
				drawNameRotated(map, g, nameCompiler.compileName() + " Range", locations);
				// This check is necessary because I'm doing a regression of the Center locations.
				// The regression requires at least 3 points.
			}
			else
			{
				g.setFont(settings.otherMountainsFont);
				if (mountainRange.size() >= 2)
				{
					if (mountainRange.size() == 2)
						drawNameHorizontal(map, g, nameCompiler.compileName() + " Twin Peaks", 
								extractLocationsFromCenters(mountainRange), 
								graph, false, mountainGroupYOffset * sizeMultiplyer);
					else
					{
						drawNameRotated(map, g, nameCompiler.compileName() + "  Mountains", 
								extractLocationsFromCenters(mountainRange),
								mountainGroupYOffset * sizeMultiplyer);
					}
				}
				else
				{
					drawNameHorizontal(map, g, nameCompiler.compileName() + " Peak",
							extractLocationsFromCenters(mountainRange), graph,
							false, mountainGroupYOffset * sizeMultiplyer);
				}
			}
		}
		
		List<Set<Corner>> rivers = findRivers(graph);
		for (Set<Corner> river : rivers)
		{
			if (river.size() >= riverMinLength)
			{
				Set<Point> locations = extractLocationsFromCorners(river);
				drawNameRotated(map, g, nameCompiler.compileName() + " River", locations,
						riverNameRiseHeight * sizeMultiplyer);
			}
			
		}
		
		g.dispose();
	}
	
	private void addTitle(BufferedImage map, GraphImpl graph, Graphics2D g)
	{
		// Find the widest ocean plate.
		Map<TectonicPlate, Double> oceanPlateWidths = new HashMap<>();
		for (TectonicPlate p : graph.plates)
			if (p.type == PlateType.Oceanic)
				oceanPlateWidths.put(p, findWidth(p.centers));

		Map<TectonicPlate, Double> landPlateWidths = new HashMap<>();
		for (TectonicPlate p : graph.plates)
			if (p.type == PlateType.Continental)
				landPlateWidths.put(p, findWidth(p.centers));
		
		TectonicPlate titlePlate;
		if (landPlateWidths.size() > 0 && ((double)oceanPlateWidths.size()) / landPlateWidths.size() < thresholdForPuttingTitleOnLand)
		{
			titlePlate = Helper.argmax(landPlateWidths);
		}
		else
		{
			titlePlate = Helper.argmax(oceanPlateWidths);
		}
				
		if (!drawNameHorizontal(map, g, "The Land of " + nameGenerator.generateName(),
				extractLocationsFromCenters(titlePlate.centers), graph, true))
		{
			// The title didn't fit. Try drawing it without "The Land of".
			drawNameHorizontal(map, g, nameGenerator.generateName(),
					extractLocationsFromCenters(titlePlate.centers), graph, true);
		}
		else
		{
			// Generate a name and throw it away to make sure names don't change when settings.size changes.
			nameGenerator.generateName();
		}
	}
	
	private double findWidth(Set<Center> centers)
	{
		double min = Collections.min(centers, new Comparator<Center>()
				{
					public int compare(Center c1, Center c2)
					{
						return Double.compare(c1.loc.x, c2.loc.x);
					}					
				}).loc.x;
		double max = Collections.max(centers, new Comparator<Center>()
				{
					public int compare(Center c1, Center c2)
					{
						return Double.compare(c1.loc.x, c2.loc.x);
					}					
				}).loc.x;
		return max - min;
	}
		
	private Set<Point> extractLocationsFromCenters(Set<Center> centers)
	{
		Set<Point> result = new TreeSet<Point>();
		for (Center c : centers)
		{
			result.add(c.loc);
		}
		return result;
	}

	private Set<Point> extractLocationsFromCorners(Set<Corner> corners)
	{
		Set<Point> result = new TreeSet<Point>();
		for (Corner c : corners)
		{
			result.add(c.loc);
		}
		return result;
	}

	/**
	 * For finding rivers.
	 */
	private List<Set<Corner>> findRivers(GraphImpl graph)
	{
		List<Set<Corner>> rivers = new ArrayList<>();
		Set<Corner> explored = new HashSet<>();
		for (Edge edge : graph.edges)
		{
			if (edge.river >= riverMinWidth && edge.v0 != null && edge.v1 != null 
					&& !explored.contains(edge.v0) && !explored.contains(edge.v1))
			{
				Set<Corner> river = followRiver(edge.v0, edge.v1);
				
				// This count shouldn't be necessary. For some reason followRiver(...) is returning
				// rivers which contain many Corners already in explored.
				int count = 0;
				for (Corner c : river)
				{
					if (explored.contains(c))
						count++;
				}
				
				explored.addAll(river);
				
				if (count < 3)
					rivers.add(river);
			}
		}
		
		return rivers;
	}
	
	/**
	 *  Searches along edges to find corners which are connected by a river. If the river forks, only
	 *	one direction is followed.
	 * @param last The search will not go in the direction of this corner.
	 * @param head The search will go in the direction of this corner.
	 * @return A set of corners which form a river.
	 */
	private Set<Corner> followRiver(Corner last, Corner head)
	{
		assert last != null;
		assert head != null;			
		assert !head.equals(last);
		
		Set<Corner> result = new HashSet<>();
		result.add(head);
		result.add(last);
		
		Set<Edge> riverEdges = new TreeSet<>();
		for (Edge e : head.protrudes)
			if (e.river >= riverMinWidth)
				riverEdges.add(e);
		
		if (riverEdges.size() == 0)
		{
			throw new IllegalArgumentException("\"last\" should be connected to head by a river edge");
		}
		if (riverEdges.size() == 1)
		{
			// base case
			return result;
		}
		if (riverEdges.size() == 2)
		{
			// Find the other river corner which is not "last".
			Corner other = null;
			for (Edge e : riverEdges)
				if (head.equals(e.v0) && !last.equals(e.v1))
				{
					other = e.v1;
				}
				else if (head.equals(e.v1) && !last.equals(e.v0))
				{
					other = e.v0;
				}

			
			if (other == null)
			{
				// The only direction this river can go goes to a null corner. This is a base case.
				return result;
			}

			result.addAll(followRiver(head, other));
			return result;
		}
		else
		{
			// There are more than 2 river edges connected to head.
			
			// Sort the river edges by river width.
			List<Edge> edgeList = new ArrayList<>(riverEdges);
			Collections.sort(edgeList, new Comparator<Edge>()
					{
						public int compare(Edge e0, Edge e1) 
						{
							return -Integer.compare(e0.river, e1.river);
						}						
					});
			Corner nextHead = null;
			
			// Find which edge contains "last".
			int indexOfLast = -1;
			for (int i : new Range(edgeList.size()))
			{
				if (last == edgeList.get(i).v0 || last == edgeList.get(i).v1)
				{
					indexOfLast = i;
					break;
				}
			}
			assert indexOfLast != -1;

			// Are there 2 edges which are wider rivers than all others?
			if (edgeList.get(1).river > edgeList.get(2).river)
			{
				
				// If last is one of those 2.
				if (indexOfLast < 2)
				{
					// nextHead = the other larger option.
					Edge nextHeadEdge;
					if (indexOfLast == 0)
					{
						nextHeadEdge = edgeList.get(1);
					}
					else
					{
						nextHeadEdge = edgeList.get(0);
					}
					
					if (!head.equals(nextHeadEdge.v0))
					{
						nextHead = nextHeadEdge.v0;
						assert nextHead != null;
					}
					else if (!head.equals(nextHeadEdge.v1))
					{
						nextHead = nextHeadEdge.v1;
						assert nextHead != null;
					}
					else
					{
						assert false; // Both corners cannot be the head.
					}
					
				}
				else
				{
					// This river is joining a larger river. This is a base case because the smaller
					// river should have a different name than the larger one.
					return result;
				}
			}
			else
			{
				// Choose the option with the largest river, avoiding choosing "last".
				edgeList.remove(indexOfLast);
				
				nextHead = head.equals(edgeList.get(0).v0) ? edgeList.get(0).v1 : edgeList.get(0).v0;
				assert nextHead != null;
			}
			// Leave the other options for the global search to hit later.
			
			result.addAll(followRiver(head, nextHead));
			return result;
		}
	}
		
	/**
	 * Draws the given name to the map with the area around the name drawn from landAndOceanBackground
	 * to make it readable when the name is drawn on top of mountains or trees.
	 */
	private void drawBackgroundBlending(BufferedImage map, Graphics2D g, int width, int height,
			Point upperLeftCorner, double angle)
	{		
		int kernelSize = (int)(backGroundBlendKernelBaseSize * sizeMultiplyer);
		int padding = kernelSize/2;
		
		BufferedImage textBG = new BufferedImage(width + padding*2, height + padding*2, 
				BufferedImage.TYPE_BYTE_GRAY);
				
		Graphics2D bG = textBG.createGraphics();
		bG.setFont(g.getFont());
		bG.setColor(Color.white);
		bG.fillRect(padding, padding, width, height);
		
		// Use convolution to make a hazy background for the text.
		BufferedImage haze = ImageHelper.convolveGrayscale(textBG, ImageHelper.createGaussianKernel(kernelSize));
		ImageHelper.maximizeContrastGrayscale(haze);
								
		ImageHelper.combineImagesWithMaskInRegion(map, landAndOceanBackground, haze, 
				((int)upperLeftCorner.x) - padding, (int)(upperLeftCorner.y) - padding, angle);
	}

	private void drawNameHorizontalAtPoint(Graphics2D g, String name, Point location, boolean boldBackground)
	{		
		Font original = g.getFont();
		Color originalColor = g.getColor();
		Font background = g.getFont().deriveFont(1, (int)(g.getFont().getSize()));
		FontMetrics metrics = g.getFontMetrics(original);
		
		Point curLoc = new Point(location.x , location.y);
		for (int i : new Range(name.length()))
		{
			if (boldBackground)
			{
				g.setFont(background);
				g.setColor(settings.boldBackgroundColor);
				g.drawString("" + name.charAt(i), (int)curLoc.x, (int)curLoc.y);
				g.setFont(original);
				g.setColor(originalColor);
			}
			g.drawString("" + name.charAt(i), (int)curLoc.x, (int)curLoc.y);
			curLoc.x += metrics.stringWidth("" + name.charAt(i));
		}
	}

	private boolean drawNameHorizontal(BufferedImage map, Graphics2D g, String name, Set<Point> locations,
			GraphImpl graph, boolean boldBackground)
	{
		return drawNameHorizontal(map, g, name, locations, graph, boldBackground, 0);
	}
	
	/**
	 * @param yOffset Distance added to the y direction when determining where to draw the name. Positive y is down.
	 * @return True iff text was drawn.
	 */
	private boolean drawNameHorizontal(BufferedImage map, Graphics2D g, String name, Set<Point> locations,
			GraphImpl graph, boolean boldBackground, double yOffset)
	{
		FontMetrics metrics = g.getFontMetrics(g.getFont());
		int width = metrics.stringWidth(name);
		int height = metrics.getHeight();
		
		
		Point centroid = findCentroid(locations);
		
		centroid.y += yOffset;
		
		String[] parts = name.split(" ");
		
		if (parts.length > 1 && (
			   !locations.contains(graph.getCenterOf((int)centroid.x - width/2, (int)centroid.y - height/2).loc)
			|| !locations.contains(graph.getCenterOf((int)centroid.x - width/2, (int)centroid.y + height/2).loc)
			|| !locations.contains(graph.getCenterOf((int)centroid.x + width/2, (int)centroid.y - height/2).loc)
			|| !locations.contains(graph.getCenterOf((int)centroid.x + width/2, (int)centroid.y + height/2).loc)))
		{		
			// One or more of the corners doesn't fit in the region. Draw it on 2 lines.
			int start = name.length()/2;
			int closestL = start;
			for (; closestL >= 0; closestL--)
				if (name.charAt(closestL) == ' ')
						break;
			int closestR = start;
			for (; closestR < name.length(); closestR++)
				if (name.charAt(closestR) == ' ')
						break;
			int pivot;
			if (Math.abs(closestL - start) < Math.abs(closestR - start))
				pivot = closestL;
			else
				pivot = closestR;
			String nameLine1 = name.substring(0, pivot);
			String nameLine2 = name.substring(pivot + 1);
			Point ulCorner1 = new Point(centroid.x - metrics.stringWidth(nameLine1)/2, 
					centroid.y - metrics.getHeight()/2);
			Point ulCorner2 =  new Point(centroid.x - metrics.stringWidth(nameLine2)/2,
					centroid.y + metrics.getHeight()/2);
			
			// Make sure we don't draw on top of existing text. Only draw the text if both lines can be drawn.
			// Check line 1.
			java.awt.Rectangle bounds1 = new java.awt.Rectangle((int)ulCorner1.x, 
					(int)ulCorner1.y, metrics.stringWidth(nameLine1), metrics.getHeight());
//			g.drawRect(bounds1.x, bounds1.y, bounds1.width, bounds1.height);
			Area area1 = new Area(bounds1);
			if (overlapsExistingTextOrIsOffMap(area1))
				return false;
			// Check line 2.
			java.awt.Rectangle bounds2 = new java.awt.Rectangle((int)ulCorner2.x, 
					(int)ulCorner2.y, metrics.stringWidth(nameLine2), metrics.getHeight());
//			g.drawRect(bounds2.x, bounds2.y, bounds2.width, bounds2.height);
			Area area2 = new Area(bounds2);
			if (overlapsExistingTextOrIsOffMap(area2))
				return false;
			textBounds.add(area1);
			textBounds.add(area2);

			drawBackgroundBlending(map, g, (int)bounds1.getWidth(), (int)bounds1.getHeight(), ulCorner1, 0);
			drawNameHorizontalAtPoint(g, nameLine1, new Point(ulCorner1.x, 
					ulCorner1.y + metrics.getAscent()), boldBackground);
			
			drawBackgroundBlending(map, g, (int)bounds2.getWidth(), (int)bounds2.getHeight(), ulCorner2, 0);
			drawNameHorizontalAtPoint(g, nameLine2, new Point(ulCorner2.x, 
					ulCorner2.y + metrics.getAscent()), boldBackground);
		}
		else
		{	
			// Make sure we don't draw on top of existing text.
			java.awt.Rectangle bounds = new java.awt.Rectangle((int)centroid.x - width/2, 
					(int)centroid.y - height/2, metrics.stringWidth(name), height);
			//g.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
			Area area = new Area(bounds);
			if (overlapsExistingTextOrIsOffMap(area))
				return false;
			textBounds.add(area);
			
			Point boundsLocation = new Point(bounds.getLocation().x, bounds.getLocation().y);
			
			drawBackgroundBlending(map, g, width, height, boundsLocation, 0);
			
			drawNameHorizontalAtPoint(g, name, new Point(boundsLocation.x, 
					boundsLocation.y + metrics.getAscent()), boldBackground);
		}
		return true;
	}

	public void drawNameRotated(BufferedImage map, Graphics2D g, String name, Set<Point> locations)
	{
		drawNameRotated(map, g, name, locations, 0.0);
	}

	/**
	 * Draws the given name at the centroid of the given plateCenters. The angle the name is
	 * drawn at is the least squares line through the plate centers. This does not break text
	 * into multiple lines.
	 * @param riseOffset The text will be raised (positive y) by this much distance above the centroid when
	 *  drawn. The rotation will be applied to this location. If there is already a name drawn above the object,
	 *  I try negating the riseOffset to draw the name below it. Positive y is down.
	 */
	public void drawNameRotated(BufferedImage map, Graphics2D g, String name, Set<Point> locations,
			double riseOffset)
	{
		
		FontMetrics metrics = g.getFontMetrics(g.getFont());
		int width = metrics.stringWidth(name);
		int height = metrics.getHeight();
				
		Point centroid = findCentroid(locations);
		
		SimpleRegression regression = new SimpleRegression();
		for (Point p : locations)
		{
			regression.addObservation(new double[]{p.x}, p.y);
		}
		regression.regress();
				
		// Find the angle to rotate the text to.
		double y0 = regression.predict(0);
		double y1 = regression.predict(1);
		// Move the intercept to the origin.
		y1 -= y0;
		y0 = 0;
		double angle = Math.atan(y1/1.0);
		
		Point offset = new Point(riseOffset * Math.sin(angle), -riseOffset * Math.cos(angle));		
		Point pivot = new Point(centroid.x - offset.x, centroid.y - offset.y);
		
		AffineTransform orig = g.getTransform();
		g.rotate(angle, pivot.x, pivot.y);
		
		// Make sure we don't draw on top of existing text.
		java.awt.Rectangle bounds = new java.awt.Rectangle((int)(pivot.x - width/2), 
				(int)(pivot.y - height/2), width, height);
		Area area = new Area(bounds);
		area = area.createTransformedArea(g.getTransform());
		if (overlapsExistingTextOrIsOffMap(area))
		{
			// If there is a riseOffset, try negating it to put the name below the object instead of above.
			offset = new Point(-riseOffset * Math.sin(angle), riseOffset * Math.cos(angle));
	
			pivot = new Point(centroid.x - offset.x, centroid.y - offset.y);
			bounds = new java.awt.Rectangle((int)(pivot.x - width/2),
					(int)(pivot.y - height/2), width, height);
			
			g.setTransform(orig);
			g.rotate(angle, pivot.x, pivot.y);
			area = new Area(bounds);
			area = area.createTransformedArea(g.getTransform());
			if (overlapsExistingTextOrIsOffMap(area))
			{
				// Give up.
				//g.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
				g.setTransform(orig);
				return;
			}
		}
		textBounds.add(area);
		
		Point boundsLocation = new Point(bounds.getLocation().x, bounds.getLocation().y);
		
		drawBackgroundBlending(map, g, (int)bounds.getWidth(), (int)bounds.getHeight(), 
				boundsLocation, angle);
		
		//g.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
		
		g.drawString(name, (int)(boundsLocation.x), (int)(boundsLocation.y + metrics.getAscent()));
		g.setTransform(orig);
	}

	private Set<Center> findPlateCentersLandOnly(final GraphImpl graph, final TectonicPlate plate)
	{		
		Set<Center> plateCenters = new HashSet<Center>();
		for (Center c : plate.centers)
		{
			if (!c.water)
				plateCenters.add(c);
		}
		return plateCenters;
	}

	public Point findCentroid(Collection<Point> plateCenters)
	{
		Point centroid = new Point(0, 0);
		for (Point p : plateCenters)
		{
			centroid.x += p.x;
			centroid.y += p.y;
		}
		centroid.x /= plateCenters.size();
		centroid.y /= plateCenters.size();
		
		return centroid;
	}
	
	private boolean overlapsExistingTextOrIsOffMap(Area bounds)
	{
		for (Area a : textBounds)
		{
			Area aCopy = new Area(a);
			aCopy.intersect(bounds);
			if (!aCopy.isEmpty())
				return true;
		}
		return !graphBounds.contains(bounds.getBounds2D());
	}
}