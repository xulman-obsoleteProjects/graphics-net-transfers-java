package de.mpicbg.ulman.simviewer.elements;

import cleargl.GLVector;
import graphics.scenery.Node;

/** corresponds to one element that simulator's DrawLine() can send;
    graphically, line is essentially a vector without an arrow head */
public class Line extends Vector
{
	public Line()             { super(); }    //without connection to Scenery
	public Line(final Node l) { super(l); }   //  with  connection to Scenery

	/** converts a line, given via its end positions, into a vector-like representation */
	public void reset(final GLVector posA, final GLVector posB, final int color)
	{
		//essentially supplies the functionality of the Vector::update(),
		//difference is in the semantics of the input
		base.set(0, posA.x());
		base.set(1, posA.y());
		base.set(2, posA.z());
		vector.set(0, posB.x()-posA.x());
		vector.set(1, posB.y()-posA.y());
		vector.set(2, posB.z()-posA.z());
		this.color = color;

		//also update the vector's auxScale:
		updateAuxAttribs();
	}

	public void update(final Line l)
	{
		super.update(l);
	}
}
