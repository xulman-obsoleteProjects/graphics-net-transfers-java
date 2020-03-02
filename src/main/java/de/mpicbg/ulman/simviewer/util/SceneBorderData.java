package de.mpicbg.ulman.simviewer.util;

import cleargl.GLVector;
import graphics.scenery.Node;
import graphics.scenery.Cylinder;
import graphics.scenery.Material;
import de.mpicbg.ulman.simviewer.DisplayScene;

import java.util.Arrays;
import java.util.List;

/**
 * Container to represent and (re-)define the border frame around the scene.
 */
public class SceneBorderData
{
	/** data with position to be shared with the scenery */
	final GLVector[] positions = new GLVector[12];
	/** data with scales (sizes) to be shared with the scenery */
	final GLVector[] scales    = new GLVector[12];

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
			positions[i]  = new GLVector(0.f,3);
			scales[i]     = new GLVector(1.f,3);
			borderData[i] = new Cylinder(barRadius,1.0f,4);

			borderData[i].setPosition(positions[i]);
			borderData[i].setScale(scales[i]);
		}

		for (int i=0; i < 4; ++i)
			DisplayScene.ReOrientNode(borderData[i],DisplayScene.defaultNormalizedUpVector,new GLVector(1.0f,0.0f,0.0f));
		for (int i=8; i < 12; ++i)
			DisplayScene.ReOrientNode(borderData[i],DisplayScene.defaultNormalizedUpVector,new GLVector(0.0f,0.0f,1.0f));
	}


	public
	void shapeForThisScene(final float[] sceneOffset, final float[] sceneSize)
	{
		//x-axes aligned
		for (int i=0; i < 4; ++i)
		{
			borderData[i].setName("left-right bar (x axis)");
			scales[i].set(1,sceneSize[0]);
			positions[i].set(0, sceneOffset[0]);
			positions[i].set(1, sceneOffset[1]);
			positions[i].set(2, sceneOffset[2]);
		}

		final GLVector dx = new GLVector(sceneSize[0],0.f,0.f);
		final GLVector dy = new GLVector(0.f,sceneSize[1],0.f);
		final GLVector dz = new GLVector(0.f,0.f,sceneSize[2]);

		positions[1].plusAssign(dy);
		positions[2].plusAssign(dy.plus(dz));
		positions[3].plusAssign(dz);

		//y-axes aligned
		for (int i=4; i < 8; ++i)
		{
			borderData[i].setName("bottom-up bar (y axis)");
			scales[i].set(1,sceneSize[1]);
			positions[i].set(0, sceneOffset[0]);
			positions[i].set(1, sceneOffset[1]);
			positions[i].set(2, sceneOffset[2]);
		}

		positions[5].plusAssign(dx);
		positions[6].plusAssign(dx.plus(dz));
		positions[7].plusAssign(dz);

		//z-axes aligned
		for (int i=8; i < 12; ++i)
		{
			borderData[i].setName("front-rear bar (z axis)");
			scales[i].set(1,sceneSize[2]);
			positions[i].set(0, sceneOffset[0]);
			positions[i].set(1, sceneOffset[1]);
			positions[i].set(2, sceneOffset[2]);
		}

		positions[ 9].plusAssign(dx);
		positions[10].plusAssign(dx.plus(dy));
		positions[11].plusAssign(dy);

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
	public static final GLVector borderRedColor  = new GLVector(1.0f, 0.0f, 0.0f, 1.0f);
	public static final GLVector borderBlueColor = new GLVector(0.0f, 0.0f, 1.0f, 1.0f);


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
