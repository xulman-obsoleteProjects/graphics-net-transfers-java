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


package de.mpicbg.ulman.simviewer;

import cleargl.GLVector;
import graphics.scenery.*;
import graphics.scenery.backends.ShaderType;
import graphics.scenery.Material.CullingMode;
import sc.iview.SciView;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import de.mpicbg.ulman.simviewer.elements.Point;
import de.mpicbg.ulman.simviewer.elements.Line;
import de.mpicbg.ulman.simviewer.elements.Vector;
import de.mpicbg.ulman.simviewer.elements.VectorSH;

/**
 * Adapted from TexturedCubeJavaExample.java from the scenery project,
 * originally created by kharrington on 7/6/16.
 *
 * This file was created and is being developed by Vladimir Ulman, 2018.
 */
public class DisplaySceneAllInstancing extends DisplayScene
{
	/** constructor to create an empty window */
	public
	DisplaySceneAllInstancing(final SciView sciView, final boolean fullInstancing,
	                          final float[] sOffset, final float[] sSize)
	{
		super(sciView, sOffset, sSize);
		this.fullInstancing = fullInstancing;

		//also init materials of the master instances
		refMaterials = new Material[3];
		if (fullInstancing)
		{
			final List<ShaderType> sList = new ArrayList<>(2);
			sList.add(ShaderType.VertexShader);
			sList.add(ShaderType.FragmentShader);

			refMaterials[0] = ShaderMaterial.fromClass(DisplayScene.class, sList);
			refMaterials[1] = ShaderMaterial.fromClass(DisplayScene.class, sList);
			refMaterials[2] = ShaderMaterial.fromClass(DisplayScene.class, sList);
		}
		else
		{
			refMaterials[0] = ShaderMaterial.fromFiles("DefaultDeferredInstanced.vert", "DefaultDeferred.frag");
			refMaterials[1] = ShaderMaterial.fromFiles("DefaultDeferredInstanced.vert", "DefaultDeferred.frag");
			refMaterials[2] = ShaderMaterial.fromFiles("DefaultDeferredInstanced.vert", "DefaultDeferred.frag");
		}
		//
		for (Material m : refMaterials)
		{
			m.setCullingMode(CullingMode.None);
			m.setAmbient(  new GLVector(1.0f, 1.0f, 1.0f) );
			m.setSpecular( new GLVector(1.0f, 1.0f, 1.0f) );
		}

		//instancing:
		//define a master instance point (Sphere)
		refMaterials[0].setDiffuse(new GLVector(1.0f,0.6f,0.6f));
		refPointNode = factoryForPoints();
		refPointNode.setMaterial(refMaterials[0]);
		refPointNode.getInstancedProperties().put("ModelMatrix", refPointNode::getModel);
		if (fullInstancing)
			refPointNode.getInstancedProperties().put("Color", () -> new GLVector(0.5f, 0.5f, 0.5f, 1.0f));
		refPointNode.setName("master instance - sphere");
		scene.addChild(refPointNode);

		//define a master instance line
		refMaterials[1].setDiffuse(new GLVector(0.6f,1.0f,0.6f));
		refLineNode = factoryForLines();
		refLineNode.setMaterial(refMaterials[1]);
		refLineNode.getInstancedProperties().put("ModelMatrix", refLineNode::getModel);
		if (fullInstancing)
			refLineNode.getInstancedProperties().put("Color", () -> new GLVector(0.5f, 0.5f, 0.5f, 1.0f));
		refLineNode.setName("master instance - line");
		scene.addChild(refLineNode);

		//define a master instance vector as two instances (of the same material):
		//the vector shaft (slim Cylinder) and head (Cone)
		refMaterials[2].setDiffuse(new GLVector(0.6f,0.6f,1.0f));
		refVectorNode_Shaft = factoryForVectorShafts();
		refVectorNode_Shaft.setMaterial(refMaterials[2]);
		refVectorNode_Shaft.getInstancedProperties().put("ModelMatrix", refVectorNode_Shaft::getModel);
		if (fullInstancing)
			refVectorNode_Shaft.getInstancedProperties().put("Color", () -> new GLVector(0.5f, 0.5f, 0.5f, 1.0f));
		refVectorNode_Shaft.setName("master instance - vector shaft");
		scene.addChild(refVectorNode_Shaft);
		//
		refVectorNode_Head = factoryForVectorHeads();
		refVectorNode_Head.setMaterial(refMaterials[2]);
		refVectorNode_Head.getInstancedProperties().put("ModelMatrix", refVectorNode_Head::getModel);
		if (fullInstancing)
			refVectorNode_Head.getInstancedProperties().put("Color", () -> new GLVector(0.5f, 0.5f, 0.5f, 1.0f));
		refVectorNode_Head.setName("master instance - vector head");
		scene.addChild(refVectorNode_Head);
	}
	//----------------------------------------------------------------------------


	//instancing, master instances:
	final Sphere   refPointNode;
	final Cylinder refLineNode;
	final Cylinder refVectorNode_Shaft;
	final Cone     refVectorNode_Head;

	final boolean fullInstancing;

	/** materials used by the master instances: 0-point,1-line,2-vector */
	final Material[] refMaterials;
	//----------------------------------------------------------------------------


	/** this is designed (yet only) for SINGLE-THREAD application! */
	public
	void addUpdateOrRemovePoint(final int ID,final Point p)
	{
	 synchronized (lockOnChangingSceneContent)
	 {
		//attempt to retrieve node of this ID
		Point n = pointNodes.get(ID);

		//negative color is an agreed signal to remove the point
		//also, get rid of a point whose radius is "impossible"
		if (p.colorRGB.x() < 0 || p.radius.x() < 0.0f)
		{
			if (n != null)
			{
				//scene.removeChild(n.node); -- was never added to the scene
				refPointNode.getInstances().remove(n.node);
				pointNodes.remove(ID);
			}
			return;
		}

		//shall we create a new point?
		if (n == null)
		{
			//new point: adding
			n = new Point( new Node() );
			final Node nn = n.node;

			//define the point
			nn.setMaterial(refPointNode.getMaterial());
			nn.setScale(n.radius);
			nn.setPosition(n.centre);

			//spawn another instance
			nn.getInstancedProperties().put("ModelMatrix", nn::getWorld);
			if (fullInstancing)
				nn.getInstancedProperties().put("Color", n::getColorRGB);
			nn.setParent(scene);
			refPointNode.getInstances().add(nn);

			pointNodes.put(ID,n);
			showOrHideMe(ID,n.node,spheresShown);
		}

		//now update the point with the current data
		n.update(p);
		n.lastSeenTick = tickCounter;
		n.node.updateWorld(false,false);
	 }
	}


	/** this is designed (yet only) for SINGLE-THREAD application! */
	public
	void addUpdateOrRemoveLine(final int ID,final Line l)
	{
	 synchronized (lockOnChangingSceneContent)
	 {
		//attempt to retrieve node of this ID
		Line n = lineNodes.get(ID);

		//negative color is an agreed signal to remove the line
		if (l.colorRGB.x() < 0)
		{
			if (n != null)
			{
				//scene.removeChild(n.node); -- was never added to the scene
				refLineNode.getInstances().remove(n.node);
				lineNodes.remove(ID);
			}
			return;
		}

		//shall we create a new line?
		if (n == null)
		{
			//new line: adding
			n = new Line( new Node() );
			final Node nn = n.node;

			//define the line
			nn.setMaterial(refLineNode.getMaterial());
			nn.setScale(n.auxScale);
			nn.setPosition(n.base);

			//spawn another instance
			nn.getInstancedProperties().put("ModelMatrix", nn::getWorld);
			if (fullInstancing)
				nn.getInstancedProperties().put("Color", n::getColorRGB);
			nn.setParent(scene);
			refLineNode.getInstances().add(nn);

			lineNodes.put(ID,n);
			showOrHideMe(ID,n.node,linesShown);
		}

		//update the line with the current data
		n.update(l);
		n.lastSeenTick = tickCounter;

		//finally, set the new absolute orientation
		n.node.getRotation().setIdentity();
		ReOrientNode(n.node, defaultNormalizedUpVector, l.vector);
		//NB: this triggers n.node.updateWorld() automatically
	 }
	}


	/** this is designed (yet only) for SINGLE-THREAD application! */
	public
	void addUpdateOrRemoveVector(final int ID,final Vector v)
	{
	 synchronized (lockOnChangingSceneContent)
	 {
		//attempt to retrieve node of this ID
		VectorSH n = vectorNodes.get(ID);

		//negative color is an agreed signal to remove the vector
		if (v.colorRGB.x() < 0)
		{
			if (n != null)
			{
				//scene.removeChild(n.node); -- was never added to the scene
				refVectorNode_Shaft.getInstances().remove(n.node);
				refVectorNode_Head.getInstances().remove(n.nodeHead);
				vectorNodes.remove(ID);
			}
			return;
		}

		//shall we create a new vector?
		if (n == null)
		{
			//new vector: adding
			n = new VectorSH( new Node(),new Node() );
			final Node ns = n.node;
			final Node nh = n.nodeHead;

			//define the vector
			ns.setMaterial(refVectorNode_Shaft.getMaterial());
			ns.setScale(n.auxScale);
			ns.setPosition(n.base);

			nh.setMaterial(refVectorNode_Head.getMaterial());
			nh.setScale(n.auxScaleHead);
			nh.setPosition(n.auxHeadBase);

			//spawn another instances
			ns.getInstancedProperties().put("ModelMatrix", ns::getWorld);
			if (fullInstancing)
				ns.getInstancedProperties().put("Color", n::getColorRGB);
			ns.setParent(scene);
			refVectorNode_Shaft.getInstances().add(ns);

			nh.getInstancedProperties().put("ModelMatrix", nh::getWorld);
			if (fullInstancing)
				nh.getInstancedProperties().put("Color", n::getColorRGB);
			nh.setParent(scene);
			refVectorNode_Head.getInstances().add(nh);

			vectorNodes.put(ID,n);
			showOrHideMeForVectorSH(ID);
		}

		//update the vector with the current data
		n.updateAndScale(v,vectorsStretch,vec_headLengthRatio);
		n.lastSeenTick = tickCounter;

		//finally, set the new absolute orientation
		n.node.getRotation().setIdentity();
		ReOrientNode(n.node, defaultNormalizedUpVector, v.vector);
		n.nodeHead.setRotation(n.node.getRotation());
		//NB: this triggers n.nodes.updateWorld() automatically
	 }
	}


	/** remove all objects that were last touched before tickCounter-tolerance */
	public
	void garbageCollect(int tolerance)
	{
	 synchronized (lockOnChangingSceneContent)
	 {
		//NB: HashMap may be modified while being swept through only via iterator
		//    (and iterator must remove the elements actually)
		Iterator<Integer> i = pointNodes.keySet().iterator();
		while (i.hasNext())
		{
			final Point p = pointNodes.get(i.next());

			if (p.lastSeenTick+tolerance < tickCounter)
			{
				//scene.removeChild(p.node); -- was never added to the scene
				refPointNode.getInstances().remove(p.node);
				i.remove();
			}
		}

		i = lineNodes.keySet().iterator();
		while (i.hasNext())
		{
			final Line l = lineNodes.get(i.next());

			if (l.lastSeenTick+tolerance < tickCounter)
			{
				//scene.removeChild(l.node); -- was never added to the scene
				refLineNode.getInstances().remove(l.node);
				i.remove();
			}
		}

		i = vectorNodes.keySet().iterator();
		while (i.hasNext())
		{
			final VectorSH v = vectorNodes.get(i.next());

			if (v.lastSeenTick+tolerance < tickCounter)
			{
				//scene.removeChild(v.node); -- was never added to the scene
				refVectorNode_Shaft.getInstances().remove(v.node);
				refVectorNode_Head.getInstances().remove(v.nodeHead);
				i.remove();
			}
		}
	 }
	}
	//----------------------------------------------------------------------------


	public
	void EnableFrontFaceCulling()
	{
		super.EnableFrontFaceCulling();

		for (Material m : refMaterials)
			m.setCullingMode(CullingMode.Front);
	}

	public
	void DisableFrontFaceCulling()
	{
		super.DisableFrontFaceCulling();

		for (Material m : refMaterials)
			m.setCullingMode(CullingMode.None);
	}
}
