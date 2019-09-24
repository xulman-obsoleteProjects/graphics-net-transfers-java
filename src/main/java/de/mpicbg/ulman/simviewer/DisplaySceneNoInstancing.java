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

import graphics.scenery.*;
import sc.iview.SciView;
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
public class DisplaySceneNoInstancing extends DisplayScene
{
	/** constructor to create an empty window */
	public
	DisplaySceneNoInstancing(final SciView sciView,
	                         final float[] sOffset, final float[] sSize)
	{
		super(sciView, sOffset, sSize);
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
		if (p.color < 0 || p.radius.x() < 0.0f)
		{
			if (n != null)
			{
				scene.removeChild(n.node);
				pointNodes.remove(ID);
			}
			return;
		}

		//shall we create a new point?
		if (n == null)
		{
			//new point: adding
			n = new Point( factoryForPoints() );
			n.node.setPosition(n.centre);
			n.node.setScale(n.radius);

			pointNodes.put(ID,n);
			this.addChild(n.node);
			showOrHideMe(ID,n.node,spheresShown);
		}

		//now update the point with the current data
		n.update(p);
		n.node.setMaterial(materials[n.color % materials.length]);
		n.lastSeenTick = tickCounter;

		this.nodeSetNeedsUpdate(n.node);
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
				scene.removeChild(n.node);
				lineNodes.remove(ID);
			}
			return;
		}

		//shall we create a new line?
		if (n == null)
		{
			//new line: adding
			n = new Line( factoryForLines() );
			n.node.setPosition(n.base);
			n.node.setScale(n.auxScale);

			lineNodes.put(ID,n);
			this.addChild(n.node);
			showOrHideMe(ID,n.node,linesShown);
		}

		//update the line with the current data
		n.update(l);
		n.node.setMaterial(materials[n.color % materials.length]);
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
				scene.removeChild(n.node);
				scene.removeChild(n.nodeHead);
				vectorNodes.remove(ID);
			}
			return;
		}

		//shall we create a new vector?
		if (n == null)
		{
			//new vector: adding
			n = new VectorSH( factoryForVectorShafts(), factoryForVectorHeads() );

			//define the vector
			n.node.setPosition(n.base);
			n.node.setScale(n.auxScale);

			n.nodeHead.setPosition(n.auxHeadBase);
			n.nodeHead.setScale(n.auxScale);

			vectorNodes.put(ID,n);
			this.addChild(n.node);
			this.addChild(n.nodeHead);
			showOrHideMeForVectorSH(ID);
		}

		//update the vector with the current data
		n.updateAndScale(v,vectorsStretch,vec_headLengthRatio);
		n.node.setMaterial(materials[n.color % materials.length]);
		n.nodeHead.setMaterial(materials[n.color % materials.length]);
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
				scene.removeChild(p.node);
				i.remove();
			}
		}

		i = lineNodes.keySet().iterator();
		while (i.hasNext())
		{
			final Line l = lineNodes.get(i.next());

			if (l.lastSeenTick+tolerance < tickCounter)
			{
				scene.removeChild(l.node);
				i.remove();
			}
		}

		i = vectorNodes.keySet().iterator();
		while (i.hasNext())
		{
			final VectorSH v = vectorNodes.get(i.next());

			if (v.lastSeenTick+tolerance < tickCounter)
			{
				scene.removeChild(v.node);
				scene.removeChild(v.nodeHead);
				i.remove();
			}
		}
	 }
	}
	//----------------------------------------------------------------------------


	/** flags if nodes should be scene.addChild(node)'ed and node.setNeedsUpdate(true)'ed
	    right away (the online process mode), or do all such later at once (the batch
	    process mode) because this might have positive performance impact */
	private boolean updateNodesImmediately = true;

	/** buffer of nodes to be added to the scene (ideally) at the same time */
	private final Node[] nodesYetToBeAdded    = new Node[10240]; //40 kB of RAM
	private int          nodesYetToBeAddedCnt = 0;

	/** buffer of nodes to have their 'needsUpdate' flag set (ideally) at the same time */
	private final Node[] nodesYetToBeUpdated    = new Node[10240]; //40 kB of RAM
	private int          nodesYetToBeUpdatedCnt = 0;

	/** only signals/enables the 'batch process' mode,
	    this is designed (yet only) for SINGLE-THREAD application! */
	public
	void suspendNodesUpdating()
	{
	 synchronized (lockOnChangingSceneContent)
	 {
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
		processNodesYetToBeSmth();
	 }
	}

	/** processes (ideally at the same time) and clears the content of
	    the two buffers (buffers for adding and updating nodes simultaneously) */
	private
	void processNodesYetToBeSmth()
	{
		for (int i=0; i < nodesYetToBeAddedCnt; ++i)
			scene.addChild( nodesYetToBeAdded[i] );
		nodesYetToBeAddedCnt = 0;

		for (int i=0; i < nodesYetToBeUpdatedCnt; ++i)
			nodesYetToBeUpdated[i].setNeedsUpdate(true);
		nodesYetToBeUpdatedCnt = 0;
	}

	/** either registers the node into the Scenery's scene immediately (when in the online
	    process mode), or registers into the 'nodesYetToBeAdded' buffer (when in the batch
	    process mode) */
	private
	void addChild(final Node node)
	{
		if (updateNodesImmediately) scene.addChild(node);
		else
		{
			nodesYetToBeAdded[nodesYetToBeAddedCnt++] = node;

			//overrun protection
			if (nodesYetToBeAddedCnt == nodesYetToBeAdded.length)
				processNodesYetToBeSmth();
		}
	}

	/** either sets 'needsUpdate' flag of the node immediately (when in the online
	    process mode), or registers into the 'nodesYetToBeUpdated' buffer to
	    have it set later (when in the batch process mode) */
	private
	void nodeSetNeedsUpdate(final Node node)
	{
		if (updateNodesImmediately) node.setNeedsUpdate(true);
		else
		{
			nodesYetToBeUpdated[nodesYetToBeUpdatedCnt++] = node;

			//overrun protection
			if (nodesYetToBeUpdatedCnt == nodesYetToBeUpdated.length)
				processNodesYetToBeSmth();
		}
	}
}
