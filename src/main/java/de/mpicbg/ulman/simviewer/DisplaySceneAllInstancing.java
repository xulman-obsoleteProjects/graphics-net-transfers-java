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


package de.mpicbg.ulman.simviewer;

import org.joml.Vector3f;
import org.joml.Vector4f;
import graphics.scenery.*;
import graphics.scenery.backends.ShaderType;
import graphics.scenery.Material.CullingMode;
import sc.iview.SciView;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.Consumer;

import de.mpicbg.ulman.simviewer.elements.Point;
import de.mpicbg.ulman.simviewer.elements.Line;
import de.mpicbg.ulman.simviewer.elements.Vector;
import de.mpicbg.ulman.simviewer.elements.VectorSH;
import de.mpicbg.ulman.simviewer.util.SceneAxesData;
import de.mpicbg.ulman.simviewer.util.SceneBorderData;

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
			m.setAmbient(  new Vector3f(1.0f, 1.0f, 1.0f) );
			m.setSpecular( new Vector3f(1.0f, 1.0f, 1.0f) );
		}

		//instancing:
		//grouping nodes (visible in the sciview's scene graph panel)
		//to gather master instances of the same category
		final Node[] mastersGroupNodes = new Node[3];
		mastersGroupNodes[CATEGORY1_CELL]      = new Node("cell master instances");
		mastersGroupNodes[CATEGORY1_CELLDBG]   = new Node("cell debug master instances");
		mastersGroupNodes[CATEGORY1_GLOBALDBG] = new Node("global debug master instances");
		scene.addChild(mastersGroupNodes[CATEGORY1_CELL]);
		scene.addChild(mastersGroupNodes[CATEGORY1_CELLDBG]);
		scene.addChild(mastersGroupNodes[CATEGORY1_GLOBALDBG]);

		final String NArefNodesPrefix = "should not be visible, temporary ";

		//define a master instances for point (Sphere)
		refMaterials[CATEGORY0_POINTS].setDiffuse(new Vector3f(1.0f,0.6f,0.6f));
		for (int i=0; i < 3; ++i)
		{
			final Sphere sMain = defineSphereMaster();
			final Sphere sAux  = defineSphereMaster();
			sAux.setName(NArefNodesPrefix+sAux.getName());  //adjust aux node name

			refPoints[i][CATEGORY2_MAIN] = sMain;
			refPoints[i][CATEGORY2_AUX]  = sAux;

			mastersGroupNodes[i].addChild(sMain);
			sMain.addChild(sAux);
		}

		//define a master instances for line
		refMaterials[CATEGORY0_LINES].setDiffuse(new Vector3f(0.6f,1.0f,0.6f));
		for (int i=0; i < 3; ++i)
		{
			final Cylinder lMain = defineLineMaster();
			final Cylinder lAux  = defineLineMaster();
			lAux.setName(NArefNodesPrefix+lAux.getName());  //adjust aux node name

			refLines[i][CATEGORY2_MAIN] = lMain;
			refLines[i][CATEGORY2_AUX]  = lAux;

			mastersGroupNodes[i].addChild(lMain);
			lMain.addChild(lAux);
		}

		//define a master instances for vector as two instances (of the same material):
		//the vector shaft (slim Cylinder) and head (Cone)
		refMaterials[CATEGORY0_VECTORSHAFTS].setDiffuse(new Vector3f(0.6f,0.6f,1.0f));
		for (int i=0; i < 3; ++i)
		{
			final Cylinder sMain = defineVectorShaftMaster();
			final Cylinder sAux  = defineVectorShaftMaster();
			sAux.setName(NArefNodesPrefix+sAux.getName());    //adjust aux node name

			refVectorShafts[i][CATEGORY2_MAIN] = sMain;
			refVectorShafts[i][CATEGORY2_AUX]  = sAux;

			mastersGroupNodes[i].addChild(sMain);
			sMain.addChild(sAux);


			final Cone cMain = defineVectorHeadMaster();
			final Cone cAux  = defineVectorHeadMaster();
			cAux.setName(NArefNodesPrefix+cAux.getName());    //adjust aux node name

			refVectorHeads[i][CATEGORY2_MAIN] = cMain;
			refVectorHeads[i][CATEGORY2_AUX]  = cAux;

			mastersGroupNodes[i].addChild(cMain);
			cMain.addChild(cAux);
		}
	}

	public
	void CreateDisplayAxes()
	{
		//remove any old axes, if they exist at all...
		RemoveDisplayAxes();

		axesData = new SceneAxesData();
		axesData.shapeForThisScene(sceneOffset,sceneSize);
		axesData.setMaterial(refMaterials[CATEGORY0_LINES]);

		axesData.parentNode = defineLineMaster();
		axesData.parentNode.setName("Scene orientation compass");
		axesData.parentNode.setVisible(axesShown);
		axesData.setParentTo(axesData.parentNode);

		final Iterator<Node> axes = axesData.axesData().iterator();
		Node a = axes.next();
		a.getInstancedProperties().put("ModelMatrix", a::getWorld);
		if (fullInstancing)
			a.getInstancedProperties().put("Color", () -> SceneAxesData.axisRedColor);
		axesData.parentNode.getInstances().add(a);

		a = axes.next();
		a.getInstancedProperties().put("ModelMatrix", a::getWorld);
		if (fullInstancing)
			a.getInstancedProperties().put("Color", () -> SceneAxesData.axisGreenColor);
		axesData.parentNode.getInstances().add(a);

		a = axes.next();
		a.getInstancedProperties().put("ModelMatrix", a::getWorld);
		if (fullInstancing)
			a.getInstancedProperties().put("Color", () -> SceneAxesData.axisBlueColor);
		axesData.parentNode.getInstances().add(a);

		scene.addChild(axesData.parentNode);
	}

	public
	void CreateDisplaySceneBorder()
	{
		//remove any old border, if it exists at all...
		RemoveDisplaySceneBorder();

		borderData = new SceneBorderData();
		borderData.shapeForThisScene(sceneOffset,sceneSize);
		borderData.setMaterial(refMaterials[CATEGORY0_LINES]);

		borderData.parentNode = defineLineMaster();
		borderData.parentNode.setName("Scene border frame");
		borderData.parentNode.setVisible(borderShown);
		borderData.setParentTo(borderData.parentNode);

		int i=0;
		for (Node b : borderData.borderData())
		{
			b.getInstancedProperties().put("ModelMatrix", b::getWorld);
			if (fullInstancing)
			{
				//NB: follows the pattern of SceneBorderData.setMaterial(palette)
				if (i < 4)
					b.getInstancedProperties().put("Color", () -> SceneBorderData.borderBlueColor);
				else
					b.getInstancedProperties().put("Color", () -> SceneBorderData.borderRedColor);
				++i;
			}
			borderData.parentNode.getInstances().add(b);
		}

		scene.addChild(borderData.parentNode);
	}
	//----------------------------------------------------------------------------

	void requestWorldUpdate(boolean force)
	{
		scene.updateWorld(true,force);
		if (axesData != null) axesData.axesData().forEach( (a) -> a.setNeedsUpdate(true) );
		if (borderData != null) borderData.borderData().forEach( (b) -> b.setNeedsUpdate(true) );
		pointNodes.values().forEach( (p) -> p.node.setNeedsUpdate(true) );
		lineNodes.values().forEach( (l) -> l.node.setNeedsUpdate(true) );
		vectorNodes.values().forEach( (v) -> { v.node.setNeedsUpdate(true); v.nodeHead.setNeedsUpdate(true); } );
	}
	//----------------------------------------------------------------------------

	private Sphere defineSphereMaster()
	{
		final Sphere refPointNode = factoryForPoints();
		refPointNode.setMaterial(refMaterials[CATEGORY0_POINTS]);
		refPointNode.getInstancedProperties().put("ModelMatrix", refPointNode::getModel);
		if (fullInstancing)
			refPointNode.getInstancedProperties().put("Color", () -> new Vector4f(0.5f, 0.5f, 0.5f, 1.f));
		refPointNode.setName("sphere master instance");
		return refPointNode;
	}

	private Cylinder defineLineMaster()
	{
		final Cylinder refLineNode = factoryForLines();
		refLineNode.setMaterial(refMaterials[CATEGORY0_LINES]);
		refLineNode.getInstancedProperties().put("ModelMatrix", refLineNode::getModel);
		if (fullInstancing)
			refLineNode.getInstancedProperties().put("Color", () -> new Vector4f(0.5f, 0.5f, 0.5f, 1.f));
		refLineNode.setName("line master instance");
		return refLineNode;
	}

	private Cylinder defineVectorShaftMaster()
	{
		final Cylinder refVectorNode_Shaft = factoryForVectorShafts();
		refVectorNode_Shaft.setMaterial(refMaterials[CATEGORY0_VECTORSHAFTS]);
		refVectorNode_Shaft.getInstancedProperties().put("ModelMatrix", refVectorNode_Shaft::getModel);
		if (fullInstancing)
			refVectorNode_Shaft.getInstancedProperties().put("Color", () -> new Vector4f(0.5f, 0.5f, 0.5f, 1.f));
		refVectorNode_Shaft.setName("vector shaft master instance");
		return refVectorNode_Shaft;
	}

	private Cone defineVectorHeadMaster()
	{
		final Cone refVectorNode_Head = factoryForVectorHeads();
		refVectorNode_Head.setMaterial(refMaterials[CATEGORY0_VECTORSHAFTS]);
		refVectorNode_Head.getInstancedProperties().put("ModelMatrix", refVectorNode_Head::getModel);
		if (fullInstancing)
			refVectorNode_Head.getInstancedProperties().put("Color", () -> new Vector4f(0.5f, 0.5f, 0.5f, 1.f));
		refVectorNode_Head.setName("vector head master instance");
		return refVectorNode_Head;
	}
	//----------------------------------------------------------------------------

	//all master instances for:
	//  the 4 displayed primitives (sphere, "line", vector as head and shaft),
	//  the 3 categories (cell, cell debug, global debug),
	//  each category having 2 sub: the main and the aux instances
	private final Sphere[][]   refPoints       = new Sphere[3][2];
	private final Cylinder[][] refLines        = new Cylinder[3][2];
	private final Cylinder[][] refVectorShafts = new Cylinder[3][2];
	private final Cone[][]     refVectorHeads  = new Cone[3][2];

	static final int CATEGORY0_POINTS       = 0;
	static final int CATEGORY0_LINES        = 1;
	static final int CATEGORY0_VECTORSHAFTS = 2;
	static final int CATEGORY0_VECTORHEADS  = 3;
	//
	static final int CATEGORY1_CELL      = 0;
	static final int CATEGORY1_CELLDBG   = 1;
	static final int CATEGORY1_GLOBALDBG = 2;
	//
	static final int CATEGORY2_MAIN  = 0;
	static final int CATEGORY2_AUX   = 1;

	private
	int getCategory1(final int ID)
	{
		if ((ID & MASK_CELLID) == 0) return CATEGORY1_GLOBALDBG;
		if ((ID & MASK_DEBUG) > 0) return CATEGORY1_CELLDBG;
		return CATEGORY1_CELL;
	}

	//convenience all-in-one container
	private final Node[][][] allMasters = { refPoints, refLines, refVectorShafts, refVectorHeads };

	//convenience handlers of the instancing masters
	private void applyOnAllAuxMasters(final Consumer<Node> method)
	{
		for (Node[][] shapeMasters : allMasters)
			for (Node[] dbgLevelMaster : shapeMasters)
				method.accept( dbgLevelMaster[CATEGORY2_AUX] );
	}

	private void applyOnAllMainMasters(final Consumer<Node> method)
	{
		for (Node[][] shapeMasters : allMasters)
			applyOnSpecificMainMasters(shapeMasters,method);
	}

	private void applyOnSpecificMainMasters(final int cat0, final Consumer<Node> method)
	{
		applyOnSpecificMainMasters(allMasters[cat0],method);
	}

	private void applyOnSpecificMainMasters(final Node[][] masters, final Consumer<Node> method)
	{
		for (Node[] n : masters) method.accept( n[CATEGORY2_MAIN] );
	}


	final boolean fullInstancing;

	/** materials used by the master instances: 0-point,1-line,2-vector */
	final Material[] refMaterials;
	//----------------------------------------------------------------------------


	private boolean updateNodesImmediately = true;

	/** only signals/enables the 'batch process' mode,
	    this is designed (yet only) for SINGLE-THREAD application! */
	public
	void suspendNodesUpdating()
	{
	 synchronized (lockOnChangingSceneContent)
	 {
		applyOnAllAuxMasters(master -> master.setVisible(false));
		updateNodesImmediately = false;
	 }
	}

	/** calls processNodesYetToBeSmth() and switches back to the 'online process' mode,
	    this is designed (yet only) for SINGLE-THREAD application! */
	public
	void resumeNodesUpdating()
	{
	 synchronized (lockOnChangingSceneContent)
	 {
		updateNodesImmediately = true;

		//for now, move everything from their aux sub-cats to their main sub-categories
		for (Node[][] shapeMasters : allMasters)
			for (Node[] dbgLevelMaster : shapeMasters)
			{
				//per one category:
				final List<Node> auxInstances = dbgLevelMaster[CATEGORY2_AUX].getInstances();
				dbgLevelMaster[CATEGORY2_MAIN].getInstances().addAll(auxInstances);
				auxInstances.clear();
			}
	 }
	}

	private
	Node getAppropriateMaster(final Node[][] shapeMasters, final int ID)
	{
		final int cat1 = getCategory1(ID);
		final int cat2 = updateNodesImmediately ? CATEGORY2_MAIN : CATEGORY2_AUX;
		return shapeMasters[cat1][cat2];
	}

	private
	void addToAppropriateMaster(final int ID, final Point p)
	{
		getAppropriateMaster(refPoints,ID).getInstances().add(p.node);
	}

	private
	void addToAppropriateMaster(final int ID, final Line l)
	{
		getAppropriateMaster(refLines,ID).getInstances().add(l.node);
	}

	private
	void addToAppropriateMaster(final int ID, final VectorSH v)
	{
		getAppropriateMaster(refVectorShafts,ID).getInstances().add(v.node);
		getAppropriateMaster(refVectorHeads, ID).getInstances().add(v.nodeHead);
	}

	private
	void removeFromAppropriateMaster(final int ID, final Point p)
	{
		refPoints[getCategory1(ID)][CATEGORY2_MAIN].getInstances().remove(p.node);
	}

	private
	void removeFromAppropriateMaster(final int ID, final Line l)
	{
		refLines[getCategory1(ID)][CATEGORY2_MAIN].getInstances().remove(l.node);
	}

	private
	void removeFromAppropriateMaster(final int ID, final VectorSH v)
	{
		refVectorShafts[getCategory1(ID)][CATEGORY2_MAIN].getInstances().remove(v.node);
		refVectorHeads[ getCategory1(ID)][CATEGORY2_MAIN].getInstances().remove(v.nodeHead);
	}
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
		if (p.colorRGB.x < 0 || p.radius.x < 0.0f)
		{
			if (n != null)
			{
				removeFromAppropriateMaster(ID,n);
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
			nn.setMaterial(refMaterials[CATEGORY0_POINTS]);
			nn.setScale(n.radius);
			nn.setPosition(n.centre);

			//spawn another instance
			nn.getInstancedProperties().put("ModelMatrix", nn::getWorld);
			if (fullInstancing)
				nn.getInstancedProperties().put("Color", n::getColorRGBA);
			nn.setParent(scene);

			addToAppropriateMaster(ID,n);
			pointNodes.put(ID,n);
			showOrHideMe(ID,n.node,spheresShown);
		}

		//now update the point with the current data
		n.update(p);
		n.lastSeenTick = tickCounter;
		n.node.setNeedsUpdate(true);
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
		if (l.colorRGB.x < 0)
		{
			if (n != null)
			{
				removeFromAppropriateMaster(ID,n);
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
			nn.setMaterial(refMaterials[CATEGORY0_LINES]);
			nn.setScale(n.auxScale);
			nn.setPosition(n.base);

			//spawn another instance
			nn.getInstancedProperties().put("ModelMatrix", nn::getWorld);
			if (fullInstancing)
				nn.getInstancedProperties().put("Color", n::getColorRGBA);
			nn.setParent(scene);

			addToAppropriateMaster(ID,n);
			lineNodes.put(ID,n);
			showOrHideMe(ID,n.node,linesShown);
		}

		//update the line with the current data
		n.update(l);
		n.lastSeenTick = tickCounter;

		//finally, set the new absolute orientation
		DisplayScene.rotateNodeToDir(n.node, l.vector);
		n.node.setNeedsUpdate(true);
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
		if (v.colorRGB.x < 0)
		{
			if (n != null)
			{
				removeFromAppropriateMaster(ID,n);
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
			ns.setMaterial(refMaterials[CATEGORY0_VECTORSHAFTS]);
			ns.setScale(n.auxScale);
			ns.setPosition(n.base);

			nh.setMaterial(refMaterials[CATEGORY0_VECTORSHAFTS]);
			nh.setScale(n.auxScaleHead);
			nh.setPosition(n.auxHeadBase);

			//spawn another instances
			ns.getInstancedProperties().put("ModelMatrix", ns::getWorld);
			if (fullInstancing)
				ns.getInstancedProperties().put("Color", n::getColorRGBA);
			ns.setParent(scene);

			nh.getInstancedProperties().put("ModelMatrix", nh::getWorld);
			if (fullInstancing)
				nh.getInstancedProperties().put("Color", n::getColorRGBA);
			nh.setParent(scene);

			addToAppropriateMaster(ID,n);
			vectorNodes.put(ID,n);
			showOrHideMeForVectorSH(ID);
		}

		//update the vector with the current data
		n.updateAndScale(v,vectorsStretch,vec_headLengthRatio);
		n.lastSeenTick = tickCounter;

		//finally, set the new absolute orientation
		DisplayScene.rotateNodeToDir(n.node, v.vector);
		n.nodeHead.setRotation(n.node.getRotation());
		n.node.setNeedsUpdate(true);
		n.nodeHead.setNeedsUpdate(true);
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
			final int ID = i.next();
			final Point p = pointNodes.get(ID);

			if (p.lastSeenTick+tolerance < tickCounter)
			{
				removeFromAppropriateMaster(ID,p);
				i.remove();
			}
		}

		i = lineNodes.keySet().iterator();
		while (i.hasNext())
		{
			final int ID = i.next();
			final Line l = lineNodes.get(ID);

			if (l.lastSeenTick+tolerance < tickCounter)
			{
				removeFromAppropriateMaster(ID,l);
				i.remove();
			}
		}

		i = vectorNodes.keySet().iterator();
		while (i.hasNext())
		{
			final int ID = i.next();
			final VectorSH v = vectorNodes.get(ID);

			if (v.lastSeenTick+tolerance < tickCounter)
			{
				removeFromAppropriateMaster(ID,v);
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
