package de.mpicbg.ulman.simviewer.elements;

import cleargl.GLVector;
import graphics.scenery.Node;

/** corresponds to one element that simulator's DrawVector() can send;
    compared to the superclass Vector, this one recognizes vector as
    two graphical elements, a Shaft and a Head, and has therefore more
    housekeeping attributes */
public class VectorSH extends Vector
{
	/** Hratio is a relative distance from the top of the vector
	    to the transition point between the shaft and the head,
	    relative is taken w.r.t. the vector length */
	public VectorSH(final float Hratio) //without connection to Scenery
	{
		super();
		nodeHead = null;
		auxSRatio = 1.0f - Hratio;
	}

	/** Hratio is a relative distance from the top of the vector
	    to the transition point between the shaft and the head,
	    relative is taken w.r.t. the vector length */
	public VectorSH(final float Hratio, //with connection to Scenery
	                final Node shaftNode,
	                final Node headNode)
	{
		super(shaftNode);
		nodeHead = headNode;
		auxSRatio = 1.0f - Hratio;
	}

	/** reference on the graphics Node that draws the vector's head,
	    reference on the Node that draws vector's shaft is in super.node */
	public final Node nodeHead;

	/** relative distance from the base of the vector to
	    the transition point between the shaft and the head,
	    relative w.r.t. the vector length */
	public float auxSRatio;

	public final GLVector auxHeadBase = new GLVector(0.f,3);
	void updateAuxAttribs()
	{
		super.updateAuxAttribs();

		auxHeadBase.set(0, base.x());
		auxHeadBase.set(1, base.y());
		auxHeadBase.set(2, base.z());
		auxHeadBase.plusAssign( vector.times(auxSRatio) );
	}
}
