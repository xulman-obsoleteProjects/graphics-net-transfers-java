package de.mpicbg.ulman.simviewer.elements;

import cleargl.GLVector;
import graphics.scenery.Node;

/** Corresponds to one element that simulator's DrawVector() can send.
    Compared to the superclass Vector, this one recognizes vector as
    two graphical elements, a Shaft and a Head, and has therefore more
    housekeeping attributes.
    The class governs all necessary pieces of information to display
    (user-defined scaled version of) the vector, and the Scenery's Nodes are
    pointed inside this class to (re)fetch the actual display data/instructions. */
public class VectorSH extends Vector
{
	public VectorSH() //without connection to Scenery
	{
		super();
		nodeHead = null;
	}

	public VectorSH(final Node shaftNode, //with connection to Scenery
	                final Node headNode)
	{
		super(shaftNode);
		nodeHead = headNode;
	}

	// ------- main defining attributes -------
	/** reference on the graphics Node that draws the vector's head,
	    reference on the Node that draws vector's shaft is in super.node */
	public final Node nodeHead;

	// ------- derived (aux) attributes -------
	/** this attribute is a function of vector:
	    it defines the position/placement of the head of the vector */
	public final GLVector auxHeadBase = new GLVector(0.f,3);

	// ------- setters -------
	/** shadow/override the superclass'es applyScale() with a new one
	    that can provide default values for the 'headPosRatio' */
	public void applyScale(final float scale)
	{
		this.applyScale(scale,0.2f);
	}

	/** shadow/override the superclass'es update() with a new one
	    that can provide default values for the 'headPosRatio' */
	public void update(final Vector v)
	{
		this.update(v,0.2f);
	}


	/** adjusts aux attribs to draw the vector scale-times
	    larger than what it is originally; if such scaling
	    is required, it must not be called before this.update() */
	public void applyScale(final float scale, final float headPosRatio)
	{
		super.applyScale(scale);

		auxHeadBase.set(0, base.x());
		auxHeadBase.set(1, base.y());
		auxHeadBase.set(2, base.z());
		auxHeadBase.plusAssign( vector.times(scale * (1f-headPosRatio)) );
	}

	/** clones the given 'v' into this vector, and updates all necessary aux attribs */
	public void update(final Vector v, final float headPosRatio)
	{
		updateAndScale(v,1f,headPosRatio);
	}


	/** short cut method (also sligtly more performance-optimal)
	    to replace constructs: v.update(V); v.applyScale(scale,headPosRatio); */
	public void updateAndScale(final Vector v,
	                           final float scale, final float headPosRatio)
	{
		super.update(v);
		this.applyScale(scale,headPosRatio);
	}
}
