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
import org.scijava.ui.behaviour.ClickBehaviour;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import sc.iview.SciView;
import sc.iview.commands.demo.ParticleDemo;
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
public class DisplayScene
{
	/** constructor to create an empty window */
	public
	DisplayScene(final SciView sciView,
	             final float[] sOffset, final float[] sSize)
	{
		if (sOffset.length != 3 || sSize.length != 3)
			throw new RuntimeException("Offset and Size must be 3-items long: 3D world!");

		//init the scene dimensions
		sceneOffset  = sOffset.clone();
		sceneSize    = sSize.clone();
		this.sciView = sciView;

		//the overall down scaling of the displayed objects such that moving around
		//the scene with Scenery is vivid (move step size is fixed in Scenery), at
		//the same time we want the objects and distances to be defined with our
		//non-scaled coordinates -- so we hook everything underneath the fake object
		//that is downscaled (and consequently all is downscaled too) but defined with
		//at original scale (with original coordinates and distances)
		DsFactor = sciView.getSceneryRenderer().toString().contains("vulkan")? 0.04f : 0.1f;

		//introduce an invisible "fake" object
		scene = new Box(new GLVector(0.0f,3));
		scene.setScale(new GLVector(DsFactor,3));
		scene.setName("SimViewer");
		sciView.addNode(scene);

		//init the colors -- the material lookup table
		materials = new Material[7];
		(materials[0] = new Material()).setDiffuse( new GLVector(1.0f, 1.0f, 1.0f) );
		(materials[1] = new Material()).setDiffuse( new GLVector(1.0f, 0.0f, 0.0f) );
		(materials[2] = new Material()).setDiffuse( new GLVector(0.0f, 1.0f, 0.0f) );
		(materials[3] = new Material()).setDiffuse( new GLVector(0.2f, 0.4f, 1.0f) ); //lighter blue
		(materials[4] = new Material()).setDiffuse( new GLVector(0.0f, 1.0f, 1.0f) );
		(materials[5] = new Material()).setDiffuse( new GLVector(1.0f, 0.0f, 1.0f) );
		(materials[6] = new Material()).setDiffuse( new GLVector(1.0f, 1.0f, 0.0f) );
		//
		for (Material m : materials)
		{
			m.setCullingMode(CullingMode.None);
			m.setAmbient(  new GLVector(1.0f, 1.0f, 1.0f) );
			m.setSpecular( new GLVector(1.0f, 1.0f, 1.0f) );
		}

		//also init materials of the master instances
		final List<ShaderType> sList = new ArrayList<>(2);
		sList.add(ShaderType.VertexShader);
		sList.add(ShaderType.FragmentShader);

		refMaterials = new Material[3];
		refMaterials[0] = ShaderMaterial.fromClass(ParticleDemo.class, sList);
		refMaterials[1] = ShaderMaterial.fromClass(ParticleDemo.class, sList);
		refMaterials[2] = ShaderMaterial.fromClass(ParticleDemo.class, sList);
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
		refPointNode = new Sphere(1.0f, 12);
		refPointNode.setMaterial(refMaterials[0]);
		refPointNode.getInstancedProperties().put("ModelMatrix", refPointNode::getModel);
		refPointNode.getInstancedProperties().put("Color", () -> new GLVector(0.5f, 0.5f, 0.5f, 1.0f));
		refPointNode.setName("master instance - sphere");
		scene.addChild(refPointNode);

		//define a master instance line
		refMaterials[1].setDiffuse(new GLVector(0.6f,1.0f,0.6f));
		refLineNode = new Cylinder(0.3f, 1.0f, 4);
		refLineNode.setMaterial(refMaterials[1]);
		refLineNode.getInstancedProperties().put("ModelMatrix", refLineNode::getModel);
		refLineNode.getInstancedProperties().put("Color", () -> new GLVector(0.5f, 0.5f, 0.5f, 1.0f));
		refLineNode.setName("master instance - line");
		scene.addChild(refLineNode);

		//define a master instance vector as two instances (of the same material):
		//the vector shaft (slim Cylinder) and head (Cone)
		refMaterials[2].setDiffuse(new GLVector(0.6f,0.6f,1.0f));
		refVectorNode_Shaft = new Cylinder(0.3f, 1.0f-vec_headLengthRatio, 4);
		refVectorNode_Shaft.setMaterial(refMaterials[2]);
		refVectorNode_Shaft.getInstancedProperties().put("ModelMatrix", refVectorNode_Shaft::getModel);
		refVectorNode_Shaft.getInstancedProperties().put("Color", () -> new GLVector(0.5f, 0.5f, 0.5f, 1.0f));
		refVectorNode_Shaft.setName("master instance - vector shaft");
		scene.addChild(refVectorNode_Shaft);
		//
		refVectorNode_Head = new Cone(vec_headToShaftWidthRatio * 0.3f, vec_headLengthRatio, 4, defaultNormalizedUpVector);
		refVectorNode_Head.setMaterial(refMaterials[2]);
		refVectorNode_Head.getInstancedProperties().put("ModelMatrix", refVectorNode_Head::getModel);
		refVectorNode_Head.getInstancedProperties().put("Color", () -> new GLVector(0.5f, 0.5f, 0.5f, 1.0f));
		refVectorNode_Head.setName("master instance - vector head");
		scene.addChild(refVectorNode_Head);
	}

	/** attempts to close this rendering window */
	public
	void stop()
	{
		//this.close();
	}
	//----------------------------------------------------------------------------


	/** short cut to the sciView itself - the main root of the scene graph */
	final SciView sciView;

	/** short cut to the root Node underwhich all displayed objects should be hooked up,
	    this one is typically hooked right under the this.sciView */
	final Node scene;

	/** 3D position of the scene, to position well the lights and camera */
	final float[] sceneOffset;
	/** 3D size (diagonal vector) of the scene, to position well the lights and camera */
	final float[] sceneSize;

	/** the common scaling factor applied on all spatial units before their submitted to the scene */
	final float DsFactor;

	/** fixed lookup table with colors, in the form of materials... */
	final Material[] materials;

	//instancing, master instances:
	private Sphere   refPointNode;
	private Cylinder refLineNode;
	private Cylinder refVectorNode_Shaft;
	private Cone     refVectorNode_Head;
	private final float vec_headLengthRatio = 0.2f;         //relative scale (0,1)
	private final float vec_headToShaftWidthRatio = 10.0f;  //absolute value/width
	private final GLVector defaultNormalizedUpVector = new GLVector(0.0f,1.0f,0.0f);

	/** materials used by the master instances: 0-point,1-line,2-vector */
	final Material[] refMaterials;
	//----------------------------------------------------------------------------


	private Cylinder[] axesData = null;
	private boolean   axesShown = false;

	public
	boolean ToggleDisplayAxes()
	{
		//first run, init the data
		if (axesData == null)
		{
			axesData = new Cylinder[] {
				new Cylinder(1.0f,30.f,4),
				new Cylinder(1.0f,30.f,4),
				new Cylinder(1.0f,30.f,4)};

			//set material - color
			//NB: RGB colors ~ XYZ axes
			axesData[0].setMaterial(materials[1]);
			axesData[1].setMaterial(materials[2]);
			axesData[2].setMaterial(materials[3]);

			//set orientation for x,z axes
			ReOrientNode(axesData[0],defaultNormalizedUpVector,new GLVector(1.0f,0.0f,0.0f));
			ReOrientNode(axesData[2],defaultNormalizedUpVector,new GLVector(0.0f,0.0f,1.0f));

			//place all axes into the scene centre
			final GLVector centre = new GLVector(
				(sceneOffset[0] + 0.5f*sceneSize[0]),
				(sceneOffset[1] + 0.5f*sceneSize[1]),
				(sceneOffset[2] + 0.5f*sceneSize[2]));
			axesData[0].setPosition(centre);
			axesData[1].setPosition(centre);
			axesData[2].setPosition(centre);

			axesData[0].setName("compass axis: X");
			axesData[1].setName("compass axis: Y");
			axesData[2].setName("compass axis: Z");
		}

		//add-or-remove from the scene
		for (Node n : axesData)
			if (axesShown) scene.removeChild(n);
			else           scene.addChild(n);

		//toggle the flag
		axesShown ^= true;

		return axesShown;
	}
	//----------------------------------------------------------------------------


	private graphics.scenery.Line[] borderData = null;
	private boolean borderShown = false;

	public
	boolean ToggleDisplaySceneBorder()
	{
		//first run, init the data
		if (borderData == null)
		{
			borderData = new graphics.scenery.Line[] {
				new graphics.scenery.Line(6), new graphics.scenery.Line(6),
				new graphics.scenery.Line(6), new graphics.scenery.Line(6)  };

			final GLVector sxsysz = new GLVector(sceneOffset[0]             , sceneOffset[1]             , sceneOffset[2]             );
			final GLVector lxsysz = new GLVector(sceneOffset[0]+sceneSize[0], sceneOffset[1]             , sceneOffset[2]             );
			final GLVector sxlysz = new GLVector(sceneOffset[0]             , sceneOffset[1]+sceneSize[1], sceneOffset[2]             );
			final GLVector lxlysz = new GLVector(sceneOffset[0]+sceneSize[0], sceneOffset[1]+sceneSize[1], sceneOffset[2]             );
			final GLVector sxsylz = new GLVector(sceneOffset[0]             , sceneOffset[1]             , sceneOffset[2]+sceneSize[2]);
			final GLVector lxsylz = new GLVector(sceneOffset[0]+sceneSize[0], sceneOffset[1]             , sceneOffset[2]+sceneSize[2]);
			final GLVector sxlylz = new GLVector(sceneOffset[0]             , sceneOffset[1]+sceneSize[1], sceneOffset[2]+sceneSize[2]);
			final GLVector lxlylz = new GLVector(sceneOffset[0]+sceneSize[0], sceneOffset[1]+sceneSize[1], sceneOffset[2]+sceneSize[2]);

			//first of the two mandatory surrounding fake points that are never displayed
			for (graphics.scenery.Line l : borderData) l.addPoint(sxsysz);

			//C-shape around the front face (one edge missing)
			borderData[0].addPoint(lxlysz);
			borderData[0].addPoint(sxlysz);
			borderData[0].addPoint(sxsysz);
			borderData[0].addPoint(lxsysz);
			borderData[0].setMaterial(materials[1]);

			//the same around the right face
			borderData[1].addPoint(lxlylz);
			borderData[1].addPoint(lxlysz);
			borderData[1].addPoint(lxsysz);
			borderData[1].addPoint(lxsylz);
			borderData[1].setMaterial(materials[3]);

			//the same around the rear face
			borderData[2].addPoint(sxlylz);
			borderData[2].addPoint(lxlylz);
			borderData[2].addPoint(lxsylz);
			borderData[2].addPoint(sxsylz);
			borderData[2].setMaterial(materials[1]);

			//the same around the left face
			borderData[3].addPoint(sxlysz);
			borderData[3].addPoint(sxlylz);
			borderData[3].addPoint(sxsylz);
			borderData[3].addPoint(sxsysz);
			borderData[3].setMaterial(materials[3]);

			borderData[0].setName("border wire frame");
			borderData[1].setName("border wire frame");
			borderData[2].setName("border wire frame");
			borderData[3].setName("border wire frame");

			for (graphics.scenery.Line l : borderData)
			{
				//second of the two mandatory surrounding fake points that are never displayed
				l.addPoint(sxsysz);
				l.setEdgeWidth(0.02f);
			}
		}

		//add-or-remove from the scene
		for (Node n : borderData)
			if (borderShown) scene.removeChild(n);
			else             scene.addChild(n);

		//toggle the flag
		borderShown ^= true;

		return borderShown;
	}
	//----------------------------------------------------------------------------


	private PointLight[][] fixedLights;

	public
	void CreateFixedLightsRamp()
	{
		//two light ramps at fixed positions
		//----------------------------------
		//elements of coordinates of positions of lights
		final float xLeft   = (sceneOffset[0] + 0.05f*sceneSize[0]) *DsFactor;
		final float xCentre = (sceneOffset[0] + 0.50f*sceneSize[0]) *DsFactor;
		final float xRight  = (sceneOffset[0] + 0.95f*sceneSize[0]) *DsFactor;

		final float yTop    = (sceneOffset[1] + 0.05f*sceneSize[1]) *DsFactor;
		final float yBottom = (sceneOffset[1] + 0.95f*sceneSize[1]) *DsFactor;

		final float zNear = (sceneOffset[2] + 1.3f*sceneSize[2]) *DsFactor;
		final float zFar  = (sceneOffset[2] - 0.3f*sceneSize[2]) *DsFactor;

		//tuned such that, given current light intensity and fading, the rear cells are dark yet visible
		final float radius = 1.1f*sceneSize[1] *DsFactor;

		//create the lights, one for each upper corner of the scene
		fixedLights = new PointLight[2][6];
		(fixedLights[0][0] = new PointLight(radius)).setPosition(new GLVector(xLeft  ,yTop   ,zNear));
		(fixedLights[0][1] = new PointLight(radius)).setPosition(new GLVector(xLeft  ,yBottom,zNear));
		(fixedLights[0][2] = new PointLight(radius)).setPosition(new GLVector(xCentre,yTop   ,zNear));
		(fixedLights[0][3] = new PointLight(radius)).setPosition(new GLVector(xCentre,yBottom,zNear));
		(fixedLights[0][4] = new PointLight(radius)).setPosition(new GLVector(xRight ,yTop   ,zNear));
		(fixedLights[0][5] = new PointLight(radius)).setPosition(new GLVector(xRight ,yBottom,zNear));

		(fixedLights[1][0] = new PointLight(radius)).setPosition(new GLVector(xLeft  ,yTop   ,zFar));
		(fixedLights[1][1] = new PointLight(radius)).setPosition(new GLVector(xLeft  ,yBottom,zFar));
		(fixedLights[1][2] = new PointLight(radius)).setPosition(new GLVector(xCentre,yTop   ,zFar));
		(fixedLights[1][3] = new PointLight(radius)).setPosition(new GLVector(xCentre,yBottom,zFar));
		(fixedLights[1][4] = new PointLight(radius)).setPosition(new GLVector(xRight ,yTop   ,zFar));
		(fixedLights[1][5] = new PointLight(radius)).setPosition(new GLVector(xRight ,yBottom,zFar));

		fixedLights[0][0].setName("PointLight: x-left, y-bottom, z-near");
		fixedLights[0][1].setName("PointLight: x-left, y-top, z-near");
		fixedLights[0][2].setName("PointLight: x-centre, y-bottom, z-near");
		fixedLights[0][3].setName("PointLight: x-centre, y-top, z-near");
		fixedLights[0][4].setName("PointLight: x-right, y-bottom, z-near");
		fixedLights[0][5].setName("PointLight: x-right, y-top, z-near");

		fixedLights[1][0].setName("PointLight: x-left, y-bottom, z-far");
		fixedLights[1][1].setName("PointLight: x-left, y-top, z-far");
		fixedLights[1][2].setName("PointLight: x-centre, y-bottom, z-far");
		fixedLights[1][3].setName("PointLight: x-centre, y-top, z-far");
		fixedLights[1][4].setName("PointLight: x-right, y-bottom, z-far");
		fixedLights[1][5].setName("PointLight: x-right, y-top, z-far");

		//common settings of all lights
		final GLVector lightsColor = new GLVector(1.0f, 1.0f, 1.0f);
		for (PointLight[] lightRamp : fixedLights)
			for (PointLight l : lightRamp)
			{
				l.setIntensity((200.0f*DsFactor)*(200.0f*DsFactor));
				l.setEmissionColor(lightsColor);
				l.setVisible(false);
				sciView.addNode(l);
			}
	}

	public
	void RemoveFixedLightsRamp()
	{
		for (PointLight[] lightRamp : fixedLights)
			for (PointLight l : lightRamp)
				sciView.deleteNode(l);

		fixedLights = null;
	}

	//the state flags of the lights
	public enum fixedLightsState { FRONT, REAR, BOTH, NONE };
	protected fixedLightsState fixedLightsChoosen = fixedLightsState.NONE;

	public
	fixedLightsState ToggleFixedLights()
	{
		if (fixedLights == null)
		{
			System.out.println("Creating light ramp before turning it on...");
			this.CreateFixedLightsRamp();
		}

		switch (fixedLightsChoosen)
		{
		case FRONT:
			for (PointLight l : fixedLights[0]) l.setVisible(false);
			for (PointLight l : fixedLights[1]) l.setVisible(true);
			fixedLightsChoosen = fixedLightsState.REAR;
			break;
		case REAR:
			for (PointLight l : fixedLights[0]) l.setVisible(true);
			fixedLightsChoosen = fixedLightsState.BOTH;
			break;
		case BOTH:
			for (PointLight l : fixedLights[0]) l.setVisible(false);
			for (PointLight l : fixedLights[1]) l.setVisible(false);
			fixedLightsChoosen = fixedLightsState.NONE;
			break;
		case NONE:
			for (PointLight l : fixedLights[0]) l.setVisible(true);
			fixedLightsChoosen = fixedLightsState.FRONT;
			break;
		}

		//report the current state
		return fixedLightsChoosen;
	}


	//----------------------------------------------------------------------------


	/** counts how many times the "tick message" has been received, this message
	    is assumed to be sent typically after one simulation round is over */
	private int tickCounter = 0;
	//
	public
	void increaseTickCounter()
	{
	 synchronized (lockOnChangingSceneContent)
	 {
		++tickCounter;
	 }
	}
	//----------------------------------------------------------------------------


	/** A handle on a lock (synchronization subject) that shall be used when a caller
	    wants to modify any of the pointNodes, lineNodes, vectorNodes or tickCounter.

	    Since the DisplayScene's API for updating the displayed graphics was not
	    designed for concurrent access, the callers have to synchronize explicitly
	    among themselves. And they shall do it precisely around this attribute. */
	public final Object lockOnChangingSceneContent = new Object();

	/** these points are registered with the display, but not necessarily always visible */
	private final Map<Integer,Point> pointNodes = new HashMap<>();
	/** these lines are registered with the display, but not necessarily always visible */
	private final Map<Integer,Line> lineNodes = new HashMap<>();
	/** these vectors are registered with the display, but not necessarily always visible */
	private final Map<Integer,VectorSH> vectorNodes = new HashMap<>();


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
		if (p.color < 0 || p.radius.x() < 0.0f)
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
		if (l.color < 0)
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
		if (v.color < 0)
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
			nh.setScale(n.auxScale);
			nh.setPosition(n.auxHeadBase);

			//spawn another instances
			ns.getInstancedProperties().put("ModelMatrix", ns::getWorld);
			ns.getInstancedProperties().put("Color", n::getColorRGB);
			ns.setParent(scene);
			refVectorNode_Shaft.getInstances().add(ns);

			nh.getInstancedProperties().put("ModelMatrix", nh::getWorld);
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


	public
	void removeAllObjects()
	{
	 synchronized (lockOnChangingSceneContent)
	 {
		tickCounter = Integer.MAX_VALUE;
		garbageCollect(-1);
	 }
	}

	public
	void garbageCollect()
	{
		garbageCollect(0);
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
	//
	/** flag for external modules to see if they should call garbageCollect() */
	public boolean garbageCollecting = true;
	//----------------------------------------------------------------------------


	/** cell forces are typically small in magnitude compared to the cell size,
	    this defines the current magnification applied when displaying the force vectors */
	private float vectorsStretch = 1.f;

	float getVectorsStretch()
	{ return vectorsStretch; }

	void setVectorsStretch(final float vs)
	{
	 synchronized (lockOnChangingSceneContent)
	 {
		//update the stretch factor...
		vectorsStretch = vs;

		//...and rescale all vectors presently existing in the system
		vectorNodes.values().forEach( n -> {
			n.applyScale(vectorsStretch,vec_headLengthRatio);
			n.node.updateWorld(false,false);
			n.nodeHead.updateWorld(false,false);
		} );
	 }
	}
	//----------------------------------------------------------------------------


	/** groups visibility conditions across modes for one displayed object, e.g. sphere */
	private class elementVisibility
	{
		public boolean g_Mode = true;   //the cell debug mode, operated with 'g' key
		public boolean G_Mode = true;   //the global purpose debug mode, operated with 'G'
	}

	/** signals if we want to have cells (spheres) displayed (even if cellsData is initially empty) */
	private elementVisibility spheresShown = new elementVisibility();

	/** signals if we want to have cell lines displayed */
	private elementVisibility linesShown = new elementVisibility();

	/** signals if we want to have cell forces (vectors) displayed */
	private elementVisibility vectorsShown = new elementVisibility();

	/** signals if we want to have cell "debugging" elements displayed */
	private boolean cellDebugShown = false;

	/** signals if we want to have general purpose "debugging" elements displayed */
	private boolean generalDebugShown = false;


	public
	boolean ToggleDisplayCellSpheres()
	{
	 synchronized (lockOnChangingSceneContent)
	 {
		//toggle the flag
		spheresShown.g_Mode ^= true;

		//sync expected_* constants with current state of visibility flags
		//apply the new setting on the points
		for (Integer ID : pointNodes.keySet())
			showOrHideMe(ID,pointNodes.get(ID).node,spheresShown);

		return spheresShown.g_Mode;
	 }
	}

	public
	boolean ToggleDisplayCellLines()
	{
	 synchronized (lockOnChangingSceneContent)
	 {
		linesShown.g_Mode ^= true;

		for (Integer ID : lineNodes.keySet())
			showOrHideMe(ID,lineNodes.get(ID).node,linesShown);

		return linesShown.g_Mode;
	 }
	}

	public
	boolean ToggleDisplayCellVectors()
	{
	 synchronized (lockOnChangingSceneContent)
	 {
		vectorsShown.g_Mode ^= true;

		for (Integer ID : vectorNodes.keySet())
			//showOrHideMe(ID,vectorNodes.get(ID).node,vectorsShown);
			showOrHideMeForVectorSH(ID);

		return vectorsShown.g_Mode;
	 }
	}

	public
	boolean ToggleDisplayCellDebug()
	{
	 synchronized (lockOnChangingSceneContent)
	 {
		cellDebugShown ^= true;

		//"debug" objects might be present in any shape primitive
		for (Integer ID : pointNodes.keySet())
			showOrHideMe(ID,pointNodes.get(ID).node,spheresShown);
		for (Integer ID : lineNodes.keySet())
			showOrHideMe(ID,lineNodes.get(ID).node,linesShown);
		for (Integer ID : vectorNodes.keySet())
			//showOrHideMe(ID,vectorNodes.get(ID).node,vectorsShown);
			showOrHideMeForVectorSH(ID);

		return cellDebugShown;
	 }
	}


	public
	boolean ToggleDisplayGeneralDebugSpheres()
	{
	 synchronized (lockOnChangingSceneContent)
	 {
		//toggle the flag
		spheresShown.G_Mode ^= true;

		//sync expected_* constants with current state of visibility flags
		//apply the new setting on the points
		for (Integer ID : pointNodes.keySet())
			showOrHideMe(ID,pointNodes.get(ID).node,spheresShown);

		return spheresShown.G_Mode;
	 }
	}

	public
	boolean ToggleDisplayGeneralDebugLines()
	{
	 synchronized (lockOnChangingSceneContent)
	 {
		linesShown.G_Mode ^= true;

		for (Integer ID : lineNodes.keySet())
			showOrHideMe(ID,lineNodes.get(ID).node,linesShown);

		return linesShown.G_Mode;
	 }
	}

	public
	boolean ToggleDisplayGeneralDebugVectors()
	{
	 synchronized (lockOnChangingSceneContent)
	 {
		vectorsShown.G_Mode ^= true;

		for (Integer ID : vectorNodes.keySet())
			//showOrHideMe(ID,vectorNodes.get(ID).node,vectorsShown);
			showOrHideMeForVectorSH(ID);

		return vectorsShown.G_Mode;
	 }
	}

	public
	boolean ToggleDisplayGeneralDebug()
	{
	 synchronized (lockOnChangingSceneContent)
	 {
		generalDebugShown ^= true;

		//"debug" objects might be present in any shape primitive
		for (Integer ID : pointNodes.keySet())
			showOrHideMe(ID,pointNodes.get(ID).node,spheresShown);
		for (Integer ID : lineNodes.keySet())
			showOrHideMe(ID,lineNodes.get(ID).node,linesShown);
		for (Integer ID : vectorNodes.keySet())
			//showOrHideMe(ID,vectorNodes.get(ID).node,vectorsShown);
			showOrHideMeForVectorSH(ID);

		return generalDebugShown;
	 }
	}


	public
	void EnableFrontFaceCulling()
	{
		for (Material m : materials)
			m.setCullingMode(CullingMode.Front);
		for (Material m : refMaterials)
			m.setCullingMode(CullingMode.Front);
	}

	public
	void DisableFrontFaceCulling()
	{
		for (Material m : materials)
			m.setCullingMode(CullingMode.None);
		for (Material m : refMaterials)
			m.setCullingMode(CullingMode.None);
	}
	//----------------------------------------------------------------------------


	//ID space of graphics primitives: 31 bits
	//
	//     lowest 16 bits: ID of the graphics element itself
	//next lowest  1 bit : proper (=0) or debug (=1) element
	//next lowest 14 bits: "identification" of ONE single cell
	//     highest 1 bit : not used (sign bit)
	//
	//note: general purpose elements have cell "identification" equal to 0
	//      in which case the debug bit is not applied
	//note: there are 4 graphics primitives: points, lines, vectors, meshes;
	//      each is living in its own list of elements (e.g. this.pointNodes)

	//constants to "read out" respective information
	@SuppressWarnings("unused")
	private static final int MASK_ELEM   = ((1 << 16)-1);
	private static final int MASK_DEBUG  =   1 << 16;
	private static final int MASK_CELLID = ((1 << 14)-1) << 17;

	/** given the current display preference in 'displayFlag',
	    the visibility of the object 'n' with ID is adjusted,
	    the decided state is indicated in the return value */
	private
	boolean showOrHideMe(final int ID, final Node n, final elementVisibility displayFlag)
	{
		boolean vis = false;
		if ((ID & MASK_CELLID) == 0)
		{
			//the ID does not belong to any cell, still the (filtering) flags apply
			vis = displayFlag.G_Mode == true ? generalDebugShown : displayFlag.G_Mode;
		}
		else
		{
			vis = displayFlag.g_Mode == true && (ID & MASK_DEBUG) > 0 ?
				cellDebugShown : displayFlag.g_Mode;

			//NB: follows this table
			// flag  MASK_DEBUG   cellDebugShown    result
			// true    1          true              true
			// true    1          false             false
			// true    0          true              true
			// true    0          false             true

			// false   1          true              false
			// false   1          false             false
			// false   0          true              false
			// false   0          false             false
		}

		if (n != null) n.setVisible(vis);
		return vis;
	}

	private
	void showOrHideMeForVectorSH(final int ID)
	{
		final VectorSH v = vectorNodes.get(ID);
		if (v == null)
			throw new RuntimeException("Invalid vector ID given (ID="+ID+")");

		v.nodeHead.setVisible( showOrHideMe(ID,v.node,vectorsShown) );
		//NB: sets the same visibility to both nodes, see few lines above
	}
	//----------------------------------------------------------------------------


	public
	void reportSettings()
	{
		System.out.println("push mode       : N/A"                                 + "  \tscreenshots            : N/A");
		System.out.println("garbage collect.: " + garbageCollecting                + "  \ttickCounter            : " + tickCounter);
		System.out.println("scene border    : " + borderShown                      + "  \torientation compass    : " + axesShown);

		System.out.println("visibility      : 'g' 'G'"                                                               +  "\t'g' mode   (cell debug): " + cellDebugShown);
		System.out.println("         points :  "+(spheresShown.g_Mode? "Y":"N")+"   "+(spheresShown.G_Mode? "Y":"N") + " \t'G' mode (global debug): " + generalDebugShown);
		System.out.println("         lines  :  "+(  linesShown.g_Mode? "Y":"N")+"   "+(  linesShown.G_Mode? "Y":"N") + " \tvector elongation      : " + vectorsStretch + "x");
		System.out.println("         vectors:  "+(vectorsShown.g_Mode? "Y":"N")+"   "+(vectorsShown.G_Mode? "Y":"N") + " \tfront faces culling    : " + (materials[0].getCullingMode() == CullingMode.Front));

		System.out.println("number of points: " + this.pointNodes.size() + "\t  lines: "+this.lineNodes.size() + "\t  vectors: "+this.vectorNodes.size());
		System.out.println("color legend    :        white: velocity, 1stInnerMost2Yolk");
		System.out.println(" red: overlap            green: cell&skeleton          blue: friction, skelDev");
		System.out.println("cyan: body             magenta: tracks, rep&drive    yellow: slide, 2ndInnerMost2Yolk, tracksFF");
	}
	//----------------------------------------------------------------------------


	/** Rotates the node such that its orientation (whatever it is for the node, e.g.
	    the axis of rotational symmetry in a cylinder) given with _normalized_
	    currentNormalizedOrientVec will match the new orientation newOrientVec. */
	public
	void ReOrientNode(final Node node, final GLVector currentNormalizedOrientVec,
	                  final GLVector newOrientVec)
	{
		//plan: vector/cross product of the initial object's orientation and the new orientation,
		//and rotate by angle that is taken from the scalar product of the two

		//the rotate angle
		final float rotAngle = (float)Math.acos(currentNormalizedOrientVec.times(newOrientVec.getNormalized()));

		//for now, the second vector for the cross product
		GLVector tmpVec = newOrientVec;

		//two special cases when the two orientations are (nearly) colinear:
		//
		//a) the same direction -> nothing to do (don't even update the currentNormalizedOrientVec)
		if (Math.abs(rotAngle) < 0.01f) return;
		//
		//b) the opposite direction -> need to "flip"
		if (Math.abs(rotAngle-Math.PI) < 0.01f)
		{
			//define non-colinear helping vector, e.g. take a perpendicular one
			tmpVec = new GLVector(-newOrientVec.y(), newOrientVec.x(), 0.0f);
		}

		//axis along which to perform the rotation
		tmpVec = currentNormalizedOrientVec.cross(tmpVec).normalize();
		node.getRotation().rotateByAngleNormalAxis(rotAngle, tmpVec.x(),tmpVec.y(),tmpVec.z());

		//System.out.println("rot axis=("+tmpVec.x()+","+tmpVec.y()+","+tmpVec.z()
		//                   +"), rot angle="+rotAngle+" rad");
	}

	/** Calls the ReOrientNode() before the normalized variant of newOrientVec
	    will be stored into the currentNormalizedOrientVec. */
	public
	void ReOrientNodeAndSaveNewNormalizedOrientation(final Node node,
	                  final GLVector currentNormalizedOrientVec,
	                  final GLVector newOrientVec)
	{
		ReOrientNode(node, currentNormalizedOrientVec, newOrientVec);

		//update the current orientation
		currentNormalizedOrientVec.minusAssign(currentNormalizedOrientVec);
		currentNormalizedOrientVec.plusAssign(newOrientVec);
		currentNormalizedOrientVec.normalize();
	}
	//----------------------------------------------------------------------------


	/** reference on the currently available FlightRecording: the object
	    must initialized outside and reference on it is given here, otherwise
	    the reference must be null */
	CommandFromFlightRecorder flightRecorder = null;

	private class BehaviourForFlightRecorder implements ClickBehaviour
	{
		BehaviourForFlightRecorder(final char key) { actionKey = key; }
		final char actionKey;

		@Override
		public void click( final int x, final int y )
		{
			if (flightRecorder != null)
			{
				try {
					boolean status = false;
					switch (actionKey)
					{
					case '7':
						status = flightRecorder.rewindAndSendFirstTimepoint();
						break;
					case '8':
						status = flightRecorder.sendPrevTimepointMessages();
						break;
					case '9':
						status = flightRecorder.sendNextTimepointMessages();
						break;
					case '0':
						status = flightRecorder.rewindAndSendLastTimepoint();
						break;
					}

					if (!status)
						System.out.println("No FlightRecording file is opened.");
				}
				catch (InterruptedException e) {
					System.out.println("DisplayScene: Interrupted and stopping...");
					stop();
				}
			}
			else System.out.println("FlightRecording is not available.");
		}
	}
}
