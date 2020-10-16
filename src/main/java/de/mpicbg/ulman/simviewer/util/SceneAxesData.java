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


package de.mpicbg.ulman.simviewer.util;

import org.joml.Vector3f;
import org.joml.Vector4f;
import org.joml.Quaternionf;
import graphics.scenery.Node;
import graphics.scenery.Cylinder;
import graphics.scenery.Material;

import java.util.Arrays;
import java.util.List;

/**
 * Container to represent and (re-)define the orientation compass in the centre of the scene.
 */
public class SceneAxesData
{
	/** data with position to be shared with the scenery */
	final Vector3f position = new Vector3f(0);
	/** data with scale (size) to be shared with the scenery */
	final Vector3f scale    = new Vector3f(1);

	/** the scenery's nodes that actually make up the orientation compass */
	final Node[] axesData = new Node[3];

	/** this essentially provides a view on this.axesData in
	    a way that permits mutating individual Nodes of the array but
	    does not permit to change the array itself nor its content */
	public List<Node> axesData()
	{
		return Arrays.asList(axesData);
	}

	/** an optional reference on a parent node -- this is useful for a caller
	    to have a place to store the reference on the upstream node under
	    which the compass nodes might be hanged... */
	public Node parentNode = null;

	/** thickness of the compass axes */
	public final float barRadius;

	/** length of the compass axes */
	public final float barLength;


	public
	SceneAxesData()
	{
		this.barRadius = 0.7f;
		this.barLength = 30.0f;
		allocate();
	}

	public
	SceneAxesData(final float barRadius)
	{
		this.barRadius = barRadius;
		this.barLength = 30.0f;
		allocate();
	}

	public
	SceneAxesData(final float barRadius, final float barLength)
	{
		this.barRadius = barRadius;
		this.barLength = barLength;
		allocate();
	}

	private
	void allocate()
	{
		scale.y = barLength;

		for (int i=0; i < 3; ++i)
		{
			axesData[i] = new Cylinder(barRadius,1.0f,4);

			axesData[i].setPosition(position);
			axesData[i].setScale(scale);
		}

		final float halfPI = (float)(0.5*Math.PI);
		axesData[0].setRotation( new Quaternionf().rotateXYZ(0,0,-halfPI).normalize() );
		axesData[2].setRotation( new Quaternionf().rotateXYZ(halfPI,0,0).normalize() );
	}


	public
	void shapeForThisScene(final float[] sceneOffset, final float[] sceneSize)
	{
		axesData[0].setName("compass axis: X");
		axesData[1].setName("compass axis: Y");
		axesData[2].setName("compass axis: Z");

		//place all axes into the scene centre
		position.set(sceneOffset[0] + 0.5f*sceneSize[0],
		             sceneOffset[1] + 0.5f*sceneSize[1],
		             sceneOffset[2] + 0.5f*sceneSize[2]);

		for (Node a : axesData)
			a.setNeedsUpdate(true);
	}


	//typically when non-instancing
	public
	void setMaterial(final Palette pal)
	{
		axesData[0].setMaterial(pal.getMaterial(1));
		axesData[1].setMaterial(pal.getMaterial(2));
		axesData[2].setMaterial(pal.getMaterial(3));
	}

	//typically when instancing
	public
	void setMaterial(final Material mat)
	{
		for (Node a : axesData)
			a.setMaterial(mat);
	}
	public static final Vector4f axisRedColor   = new Vector4f(1.0f, 0.0f, 0.0f, 1.0f);
	public static final Vector4f axisGreenColor = new Vector4f(0.0f, 1.0f, 0.0f, 1.0f);
	public static final Vector4f axisBlueColor  = new Vector4f(0.0f, 0.0f, 1.0f, 1.0f);


	//typically when non-instancing
	public
	void becomeChildOf(final Node parentNode)
	{
		for (Node a : axesData)
			parentNode.addChild(a);
	}

	//typically when instancing
	public
	void setParentTo(final Node parentNode)
	{
		for (Node a : axesData)
			a.setParent(parentNode);
	}
}
