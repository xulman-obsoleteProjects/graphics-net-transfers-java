
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



package cz.it4i.ulman.graphics_net_transfers_java.server;

import java.util.Locale;
import java.util.Scanner;

import org.joml.Vector3f;
import de.mpicbg.ulman.simviewer.DisplayScene;
import de.mpicbg.ulman.simviewer.elements.Point;
import de.mpicbg.ulman.simviewer.elements.Line;
import de.mpicbg.ulman.simviewer.elements.Vector;

/**
 * A class to parse the messages according the "network-protocol" and
 * to command the SimViewer consequently. The "network-protocol" consists
 * of commands to draw, update, or delete primitive graphics such as points
 * or lines. The protocol is the best defined in the EmbryoGen simulator's
 * code, see the file DisplayUnits/SceneryDisplayUnit.cpp in there. The protocol
 * is utilized, e.g., in the CommandFromNetwork and CommandFromFlightRecorder
 * classes.
 *
 * This file was created and is being developed by Vladimir Ulman, 2019.
 */
public class NetMessagesProcessor
{
	/** constructor to store the connection to a displayed window
	    that shall be commanded from the incoming messages */
	public NetMessagesProcessor(final DisplayScene _scene)
	{
		scene = _scene;
	}

	/** the entry function to process the incoming message; since the "tick" message
	    may trigger a short waiting before a screen shot of the commanding window is
	    requested (see the code of the processTickMessage()) and the waiting can be
	    interrupted, this method may throw an InterruptedException */
	public
	void processMsg(final String msg)
	throws InterruptedException
	{
	 synchronized (scene.lockOnChangingSceneContent)
	 {
		try {
			if (msg.startsWith("v1 points")) processPoints(msg,true);
			else
			if (msg.startsWith("v1 lines")) processLines(msg,true);
			else
			if (msg.startsWith("v1 vectors")) processVectors(msg,true);
			else
			if (msg.startsWith("v2 points")) processPoints(msg);
			else
			if (msg.startsWith("v2 lines")) processLines(msg);
			else
			if (msg.startsWith("v2 vectors")) processVectors(msg);
			else
			if (msg.startsWith("v1 triangles")) processTriangles(msg);
			else
			if (msg.startsWith("v1 tick")) processTickMessage(msg.substring(8));
			else
				System.out.println("NetMessagesProcessor: Don't understand this msg: "+msg);
			scene.refreshInspectorPanel();
		}
		catch (java.util.InputMismatchException e) {
			System.out.println("NetMessagesProcessor: Parsing error: " + e.getMessage());
		}
	 }
	}
	//----------------------------------------------------------------------------

	/** reference on the controlled rendering display */
	private final DisplayScene scene;


	private
	void processPoints(final String msg)
	{ processPoints(msg,false); }

	private
	void processPoints(final String msg, boolean oldV1colors)
	{
		Scanner s = new Scanner(msg).useLocale(Locale.ENGLISH);

		//System.out.println("processing point msg: "+msg);

		//this skips the "v1 points" - the two tokens
		s.next();
		s.next();
		final int N = s.nextInt();

		if (N > 10) scene.suspendNodesUpdating();

		//is the next token 'dim'?
		if (s.next("dim").startsWith("dim") == false)
		{
			System.out.println("NetMessagesProcessor: Don't understand this msg: "+msg);
			s.close();
			return;
		}

		//so the next token is dimensionality of the points
		final int D = s.nextInt();

		//now, point by point is reported
		final Point p = new Point();

		for (int n=0; n < N; ++n)
		{
			//extract the point ID
			int ID = s.nextInt();

			//now read and save coordinates
			int d=0;
			for (; d < D && d < 3; ++d) p.centre.setComponent(d, s.nextFloat());
			//read possibly remaining coordinates (for which we have no room to store them)
			for (; d < D; ++d) s.nextFloat();
			//NB: all points in the same message (in this function call) are of the same dimensionality

			p.radius.x = s.nextFloat();
			p.radius.y = p.radius.x;
			p.radius.z = p.radius.x;
			if (oldV1colors) readV1Color(s,p.colorRGB);
			else             readV2Color(s,p.colorRGB);

			scene.addUpdateOrRemovePoint(ID,p);
		}

		s.close();

		if (N > 10) scene.resumeNodesUpdating();
	}


	private
	void processLines(final String msg)
	{ processLines(msg,false); }

	private
	void processLines(final String msg, boolean oldV1colors)
	{
		Scanner s = new Scanner(msg).useLocale(Locale.ENGLISH);

		//System.out.println("processing point msg: "+msg);

		//this skips the "v1 lines" - the two tokens
		s.next();
		s.next();
		final int N = s.nextInt();

		if (N > 10) scene.suspendNodesUpdating();

		//is the next token 'dim'?
		if (s.next("dim").startsWith("dim") == false)
		{
			System.out.println("NetMessagesProcessor: Don't understand this msg: "+msg);
			s.close();
			return;
		}

		//so the next token is dimensionality of the points
		final int D = s.nextInt();

		//now, point pair by pair is reported
		final Line l = new Line();

		for (int n=0; n < N; ++n)
		{
			//extract the point ID
			int ID = s.nextInt();

			//now read the first in the pair and save coordinates
			int d=0;
			for (; d < D && d < 3; ++d) l.base.setComponent(d, s.nextFloat());
			//read possibly remaining coordinates (for which we have no room to store them)
			for (; d < D; ++d) s.nextFloat();

			//now read the second in the pair and save sizes
			d=0;
			for (; d < D && d < 3; ++d) l.vector.setComponent(d, s.nextFloat() - l.base.get(d));
			//read possibly remaining coordinates (for which we have no room to store them)
			for (; d < D; ++d) s.nextFloat();

			if (oldV1colors) readV1Color(s,l.colorRGB);
			else             readV2Color(s,l.colorRGB);

			scene.addUpdateOrRemoveLine(ID,l);
		}

		s.close();

		if (N > 10) scene.resumeNodesUpdating();
	}


	private
	void processVectors(final String msg)
	{ processVectors(msg,false); }

	private
	void processVectors(final String msg, boolean oldV1colors)
	{
		Scanner s = new Scanner(msg).useLocale(Locale.ENGLISH);

		//System.out.println("processing point msg: "+msg);

		//this skips the "v1 vectors" - the two tokens
		s.next();
		s.next();
		final int N = s.nextInt();

		if (N > 10) scene.suspendNodesUpdating();

		//is the next token 'dim'?
		if (s.next("dim").startsWith("dim") == false)
		{
			System.out.println("NetMessagesProcessor: Don't understand this msg: "+msg);
			s.close();
			return;
		}

		//so the next token is dimensionality of the points
		final int D = s.nextInt();

		//now, point pair by pair is reported
		final Vector v = new Vector();

		for (int n=0; n < N; ++n)
		{
			//extract the point ID
			int ID = s.nextInt();

			//now read the first in the pair and save coordinates
			int d=0;
			for (; d < D && d < 3; ++d) v.base.setComponent(d, s.nextFloat());
			//read possibly remaining coordinates (for which we have no room to store them)
			for (; d < D; ++d) s.nextFloat();

			//now read the second in the pair and save sizes
			d=0;
			for (; d < D && d < 3; ++d) v.vector.setComponent(d, s.nextFloat());
			//read possibly remaining coordinates (for which we have no room to store them)
			for (; d < D; ++d) s.nextFloat();

			if (oldV1colors) readV1Color(s,v.colorRGB);
			else             readV2Color(s,v.colorRGB);

			scene.addUpdateOrRemoveVector(ID,v);
		}

		s.close();

		if (N > 10) scene.resumeNodesUpdating();
	}


	private
	void processTriangles(final String msg)
	{
		System.out.println("NetMessagesProcessor: not implemented yet: "+msg);
	}


	/** this is a general (free format) message, which is assumed
	    to be sent typically after one simulation round is over */
	private
	void processTickMessage(final String msg)
	throws InterruptedException
	{
		System.out.println("NetMessagesProcessor: Got tick message: "+msg);

		//check if we should save the screen
		if (scene.savingScreenshots)
		{
			//give scenery some grace time to redraw everything
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				//a bit unexpected to be stopped here, so we leave a note and forward the exception upstream
				System.out.println("NetMessagesProcessor: Interrupted just before requesting a screen shot:");
				e.printStackTrace();
				throw e;
			}

			scene.saveNextScreenshot();
		}

		if (scene.garbageCollecting) scene.garbageCollect();

		scene.increaseTickCounter();
	}


	private
	void readV1Color(final Scanner s, final Vector3f color)
	{
		final int colorIndex = s.nextInt();
		switch (colorIndex)
		{
		case 1:
			color.x = 1.0f;
			color.y = 0.0f;
			color.z = 0.0f;
			break;
		case 2:
			color.x = 0.0f;
			color.y = 1.0f;
			color.z = 0.0f;
			break;
		case 3:
			color.x = 0.0f;
			color.y = 0.0f;
			color.z = 1.0f;
			break;
		case 4:
			color.x = 0.0f;
			color.y = 1.0f;
			color.z = 1.0f;
			break;
		case 5:
			color.x = 1.0f;
			color.y = 0.0f;
			color.z = 1.0f;
			break;
		case 6:
			color.x = 1.0f;
			color.y = 1.0f;
			color.z = 0.0f;
			break;
		default:
			color.x = 1.0f;
			color.y = 1.0f;
			color.z = 1.0f;
		}
	}

	private
	void readV2Color(final Scanner s, final Vector3f color)
	{
		color.x = s.nextFloat();
		color.y = s.nextFloat();
		color.z = s.nextFloat();
	}
}
