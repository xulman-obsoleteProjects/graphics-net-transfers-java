/**
BSD 2-Clause License

Copyright (c) 2019, Vladimír Ulman
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
   list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/


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

	/** object's color in the RGB format */
	public final GLVector colorRGB = new GLVector(1.0f,0.2f,0.2f,1.0f);
	public GLVector getColorRGB() { return colorRGB; }

	public int lastSeenTick = 0;

	public void update(final Point p)
	{
		centre.set(0, p.centre.x());
		centre.set(1, p.centre.y());
		centre.set(2, p.centre.z());
		radius.set(0, p.radius.x());
		radius.set(1, p.radius.y());
		radius.set(2, p.radius.z());

		colorRGB.set(0, p.colorRGB.x());
		colorRGB.set(1, p.colorRGB.y());
		colorRGB.set(2, p.colorRGB.z());
	}
}
