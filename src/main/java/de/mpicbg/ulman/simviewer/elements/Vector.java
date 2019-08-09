package de.mpicbg.ulman.simviewer.elements;

import cleargl.GLVector;
import graphics.scenery.Node;

/** Corresponds to one element that simulator's DrawVector() can send.
    The class governs all necessary pieces of information to display
    (user-defined scaled version of) the vector, and the Scenery's Nodes are
    pointed inside this class to (re)fetch the actual display data/instructions. */
public class Vector
{
	public Vector()             { node = null; }   //without connection to Scenery
	public Vector(final Node v) { node = v; }      //  with  connection to Scenery

	// ------- main defining attributes -------
	public final Node node;
	public final GLVector base   = new GLVector(0.f,3);
	public final GLVector vector = new GLVector(0.f,3);
	public int color;

	public int lastSeenTick = 0;

	// ------- derived (aux) attributes -------
	/** this attribute is a function of vector:
	    auxScale.y is the vector length (because master instance vector
	    is oriented along y axis, so we elongate it to the desired length
	    only along this axis (and then rotate, then place to 'base')),
	    auxScale.x and .z should remain 1 (= no scaling) */
	public final GLVector auxScale = new GLVector(1.f,3);

	// ------- setters -------
	/** adjusts aux attribs to draw the vector scale-times
	    larger than what it is originally; if such scaling
	    is required, it must be called right after this.update() */
	public void applyScale(final float scale)
	{
		auxScale.set(1, scale * (float)Math.sqrt(vector.length2()) );
	}

	/** clones the given 'v' into this vector, and updates all necessary aux attribs */
	public void update(final Vector v)
	{
		updateAndScale(v,1f);
	}

	/** clones the given 'v' into this vector, and updates all necessary aux attribs;
	    it is a short cut method (also sligtly more performance-optimal)
	    to replace constructs: v.update(V); v.applyScale(scale); */
	public void updateAndScale(final Vector v, final float scale)
	{
		base.set(0, v.base.x());
		base.set(1, v.base.y());
		base.set(2, v.base.z());
		vector.set(0, v.vector.x());
		vector.set(1, v.vector.y());
		vector.set(2, v.vector.z());
		color = v.color;

		applyScale(scale);
	}
}
