/*
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

import graphics.scenery.Node;
import org.joml.Vector3f;

/** Corresponds to one element that simulator's DrawLine() can send;
    graphically, line is essentially a vector without an arrow head.
    The class governs all necessary pieces of information to display
    the line, and the SciView's Nodes are pointed inside this class
    to (re)fetch the actual display data/instructions. */
public class Line extends Vector
{
	public Line()             { super(); }    //without connection to SciView
	public Line(final Node l) { super(l); }   //  with  connection to SciView

	/** converts a line, given via its end positions, into a vector-like representation */
	public void reset(final Vector3f posA, final Vector3f posB, final Vector3f rgbColor)
	{
		//essentially supplies the functionality of the Vector::update(),
		//difference is in the semantics of the input
		base.set( posA );
		vector.set( posB ).sub( posA );
		colorRGB.set( rgbColor );

		//also update the vector's auxScale:
		applyScale(1f);
	}

	public void update(final Line l)
	{
		super.update(l);
	}
}
