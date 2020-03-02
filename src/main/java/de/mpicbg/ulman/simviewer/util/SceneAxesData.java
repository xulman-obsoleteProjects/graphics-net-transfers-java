package de.mpicbg.ulman.simviewer.util;

import cleargl.GLVector;
import graphics.scenery.Node;
import graphics.scenery.Cylinder;
import graphics.scenery.Material;
import de.mpicbg.ulman.simviewer.DisplayScene;

import java.util.Arrays;
import java.util.List;

/**
 * Container to represent and (re-)define the orientation compass in the centre of the scene.
 */
public class SceneAxesData
{
	/** data with position to be shared with the scenery */
	final GLVector position = new GLVector(0.f,3);
	/** data with scale (size) to be shared with the scenery */
	final GLVector scale    = new GLVector(1.f,3);

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
		scale.set(1,barLength);

		for (int i=0; i < 3; ++i)
		{
			axesData[i] = new Cylinder(barRadius,1.0f,4);

			axesData[i].setPosition(position);
			axesData[i].setScale(scale);
		}

		DisplayScene.ReOrientNode(axesData[0],DisplayScene.defaultNormalizedUpVector,new GLVector(1.0f,0.0f,0.0f));
		DisplayScene.ReOrientNode(axesData[2],DisplayScene.defaultNormalizedUpVector,new GLVector(0.0f,0.0f,1.0f));
	}


	public
	void shapeForThisScene(final float[] sceneOffset, final float[] sceneSize)
	{
		axesData[0].setName("compass axis: X");
		axesData[1].setName("compass axis: Y");
		axesData[2].setName("compass axis: Z");

		//place all axes into the scene centre
		position.set(0, sceneOffset[0] + 0.5f*sceneSize[0]);
		position.set(1, sceneOffset[1] + 0.5f*sceneSize[1]);
		position.set(2, sceneOffset[2] + 0.5f*sceneSize[2]);

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
	public static final GLVector axisRedColor   = new GLVector(1.0f, 0.0f, 0.0f, 1.0f);
	public static final GLVector axisGreenColor = new GLVector(0.0f, 1.0f, 0.0f, 1.0f);
	public static final GLVector axisBlueColor  = new GLVector(0.0f, 0.0f, 1.0f, 1.0f);


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
