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
 * Container to represent and (re-)define the border frame around the scene.
 */
public class SceneBorderData
{
	/** data with position to be shared with the scenery */
	final Vector3f[] positions = new Vector3f[12];
	/** data with scales (sizes) to be shared with the scenery */
	final Vector3f[] scales    = new Vector3f[12];

	/** the scenery's nodes that actually make up the border frame */
	final Node[] borderData = new Node[12];

	/** this essentially provides a view on this.borderData in
	    a way that permits mutating individual Nodes of the array but
	    does not permit to change the array itself nor its content */
	public List<Node> borderData()
	{
		return Arrays.asList(borderData);
	}

	/** an optional reference on a parent node -- this is useful for a caller
	    to have a place to store the reference on the upstream node under
	    which all border frame nodes might be hanged... */
	public Node parentNode = null;

	/** thickness of the "wireframe" */
	public final float barRadius;


	public
	SceneBorderData()
	{
		this.barRadius = 0.7f;
		allocate();
	}

	public
	SceneBorderData(final float barRadius)
	{
		this.barRadius = barRadius;
		allocate();
	}

	private
	void allocate()
	{
		for (int i=0; i < 12; ++i)
		{
			positions[i]  = new Vector3f(0);
			scales[i]     = new Vector3f(1);
			borderData[i] = new Cylinder(barRadius,1.0f,4);

			borderData[i].setPosition(positions[i]);
			borderData[i].setScale(scales[i]);
		}

		final float halfPI = (float)(0.5*Math.PI);
		for (int i=0; i < 4; ++i)
			borderData[i].setRotation( new Quaternionf().rotateXYZ(0,0,-halfPI).normalize() );
		for (int i=8; i < 12; ++i)
			borderData[i].setRotation( new Quaternionf().rotateXYZ(halfPI,0,0).normalize() );
	}


	public
	void shapeForThisScene(final float[] sceneOffset, final float[] sceneSize)
	{
		//x-axes aligned
		for (int i=0; i < 4; ++i)
		{
			borderData[i].setName("left-right bar (x axis)");
			scales[i].y = sceneSize[0];
			positions[i].set(sceneOffset[0], sceneOffset[1], sceneOffset[2]);
		}

		final Vector3f dx = new Vector3f(sceneSize[0],0.f,0.f);
		final Vector3f dy = new Vector3f(0.f,sceneSize[1],0.f);
		final Vector3f dz = new Vector3f(0.f,0.f,sceneSize[2]);

		positions[1].add(dy);
		positions[2].add(dy).add(dz);
		positions[3].add(dz);

		//y-axes aligned
		for (int i=4; i < 8; ++i)
		{
			borderData[i].setName("bottom-up bar (y axis)");
			scales[i].y = sceneSize[1];
			positions[i].set(sceneOffset[0], sceneOffset[1], sceneOffset[2]);
		}

		positions[5].add(dx);
		positions[6].add(dx).add(dz);
		positions[7].add(dz);

		//z-axes aligned
		for (int i=8; i < 12; ++i)
		{
			borderData[i].setName("front-rear bar (z axis)");
			scales[i].y = sceneSize[2];
			positions[i].set(sceneOffset[0], sceneOffset[1], sceneOffset[2]);
		}

		positions[ 9].add(dx);
		positions[10].add(dx).add(dy);
		positions[11].add(dy);

		for (Node b : borderData)
			b.setNeedsUpdate(true);
	}


	//typically when non-instancing
	public
	void setMaterial(final Palette pal)
	{
		for (int i=0; i < 4; ++i)
			borderData[i].setMaterial(pal.getMaterial(3));
		for (int i=4; i < 12; ++i)
			borderData[i].setMaterial(pal.getMaterial(1));
	}

	//typically when instancing
	public
	void setMaterial(final Material mat)
	{
		for (Node b : borderData)
			b.setMaterial(mat);
	}
	public static final Vector4f borderRedColor  = new Vector4f(1.0f, 0.0f, 0.0f, 1.0f);
	public static final Vector4f borderBlueColor = new Vector4f(0.0f, 0.0f, 1.0f, 1.0f);


	//typically when non-instancing
	public
	void becomeChildOf(final Node parentNode)
	{
		for (Node b : borderData)
			parentNode.addChild(b);
	}

	//typically when instancing
	public
	void setParentTo(final Node parentNode)
	{
		for (Node b : borderData)
			b.setParent(parentNode);
	}
}
