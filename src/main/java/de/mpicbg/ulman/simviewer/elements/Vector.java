package de.mpicbg.ulman.simviewer.elements;

import cleargl.GLVector;
import graphics.scenery.Node;

/** corresponds to one element that simulator's DrawVector() can send */
public class Vector
{
	public Vector()             { node = null; }   //without connection to Scenery
	public Vector(final Node v) { node = v; }      //  with  connection to Scenery

	public final Node node;
	public final GLVector base   = new GLVector(0.f,3);
	public final GLVector vector = new GLVector(0.f,3);
	public int color;

	public int lastSeenTick = 0;

	//this attribute is a function of vector
	public final GLVector auxScale = new GLVector(1.f,3);
	void updateAuxAttribs()
	{
		//auxScale.y is the vector length (because master instance vector
		//is oriented along y axis, so we elongate it to the desired length
		//only along this axis, and then rotate, then place to 'base')
		//auxScale.x and .z should remain 1 (= no scaling)
		auxScale.set(1, (float)Math.sqrt(vector.length2()) );
	}

	public void update(final Vector v)
	{
		base.set(0, v.base.x());
		base.set(1, v.base.y());
		base.set(2, v.base.z());
		vector.set(0, v.vector.x());
		vector.set(1, v.vector.y());
		vector.set(2, v.vector.z());
		color = v.color;

		//also update the auxScale:
		updateAuxAttribs();
	}
}
