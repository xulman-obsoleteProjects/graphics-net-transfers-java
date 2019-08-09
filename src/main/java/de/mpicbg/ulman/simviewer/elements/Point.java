package de.mpicbg.ulman.simviewer.elements;

import cleargl.GLVector;
import graphics.scenery.Node;

/** Corresponds to one element that simulator's DrawPoint() can send.
    The class governs all necessary pieces of information to display
    a point/sphere, and the Scenery's Nodes are pointed inside this class
    to (re)fetch the actual display data/instructions. */
public class Point
{
	public Point()             { node = null; }   //without connection to Scenery
	public Point(final Node p) { node = p; }      //  with  connection to Scenery

	public final Node node;
	public final GLVector centre = new GLVector(0.f,3);
	public final GLVector radius = new GLVector(0.f,3);
	public int color;

	public int lastSeenTick = 0;

	public void update(final Point p)
	{
		centre.set(0, p.centre.x());
		centre.set(1, p.centre.y());
		centre.set(2, p.centre.z());
		radius.set(0, p.radius.x());
		radius.set(1, p.radius.y());
		radius.set(2, p.radius.z());
		color = p.color;
	}
}
