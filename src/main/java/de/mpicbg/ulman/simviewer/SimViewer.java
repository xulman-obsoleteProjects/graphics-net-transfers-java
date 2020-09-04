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

import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import de.mpicbg.ulman.simviewer.util.NetMessagesProcessor;

import sc.iview.SciView;
import graphics.scenery.Light;
import graphics.scenery.Node;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * SimViewer.
 *
 * @author Vladimir Ulman
 */
@Plugin(type = Command.class, label = "SimViewer", menuRoot = "SciView",
        menu = { @Menu(label = "Demo"), @Menu(label = "SimViewer") })
public class SimViewer implements Command
{
	@Parameter
	private LogService log;

	@Parameter
	private SciView sciView;


	//----------------------------------------------------------------------------
	// visible params:
	@Parameter(choices = { "No: Works on any HW but huger and slower",
	                       "Partial: Works on most HW, slimmer, faster but without colors",
	                       "Full: Works on recent HW, slimmer, faster and with colors" })
	private String instancing = "No";

	@Parameter(min="1024")
	private int receivingPort = 8765;

	@Parameter(required = false)
	private String initSequence = "";

	@Parameter(required = false)
	private boolean addOwnLights = true;


	@Parameter(stepSize = "10", callback = "checkMinX")
	private float minX = 0;

	@Parameter(stepSize = "10", callback = "checkMaxX")
	private float maxX = 500;

	@Parameter(stepSize = "10", callback = "checkMinY")
	private float minY = 0;

	@Parameter(stepSize = "10", callback = "checkMaxY")
	private float maxY = 500;

	@Parameter(stepSize = "10", callback = "checkMinZ")
	private float minZ = 0;

	@Parameter(stepSize = "10", callback = "checkMaxZ")
	private float maxZ = 500;

	void checkMinX() { if (minX >= maxX) minX = maxX-1; }
	void checkMaxX() { if (minX >= maxX) maxX = minX+1; }
	void checkMinY() { if (minY >= maxY) minY = maxY-1; }
	void checkMaxY() { if (minY >= maxY) maxY = minY+1; }
	void checkMinZ() { if (minZ >= maxZ) minZ = maxZ-1; }
	void checkMaxZ() { if (minZ >= maxZ) maxZ = minZ+1; }


	//----------------------------------------------------------------------------
	@Override
	public void run()
	{
		log.info("SimViewer initializing");

		//prepare the original SciView scene
		disableFloorAndVisibleLights();

		//get the scene offset and "diagonal" (size)
		final float[] sOffset = new float[] {minX,minY,minZ};
		final float[] sSize   = new float[] {maxX-minX,maxY-minY,maxZ-minZ};

		//setup the SimViewer's playground..
		if (instancing.startsWith("No"))
			scene = new DisplaySceneNoInstancing(sciView,sOffset,sSize);
		else if (instancing.startsWith("Part"))
			scene = new DisplaySceneAllInstancing(sciView,false,sOffset,sSize);
		else
			scene = new DisplaySceneAllInstancing(sciView,true,sOffset,sSize);

		scene.setSceneName("SimViewer @ port "+receivingPort);

		//setup our own lights
		if (addOwnLights)
		{
			scene.CreateFixedLightsRamp();
			scene.ToggleFixedLights();
		}

		//populate the scene initially
		scene.CreateDisplayAxes();
		scene.CreateDisplaySceneBorder();

		//start aux services:
		//the shared, messages processor and its "wrapping classes"
		final NetMessagesProcessor netMsgProcessor = new NetMessagesProcessor(scene);
		//
		final CommandFromNetwork        cmdNet = new CommandFromNetwork(netMsgProcessor, receivingPort);
		final CommandFromFlightRecorder cmdFR  = new CommandFromFlightRecorder(netMsgProcessor);

		//the user-commands processor
		final CommandFromCLI            cmdCLI = new CommandFromCLI(scene, initSequence);

		//notes:
		//cmdCLI interacts with the 'scene' directly for some of its functionalities, however,
		//functionality related to the FlightRecording is outsourced to the cmdFR
		//
		//cmdFR recieves its messages from a file and it is the user (via 'scene' or cmdCLI) that triggers its activity,
		//cmdNet recieves its messages from a network and it is this event that triggers its activity
		//
		//cmdNet and cmdCLI are therefore "living" in separate threads (coded later)
		//
		//the cmdCLI as well as the 'scene' itself can also influence/command the cmdFR (they trigger its activity),
		//and so we "inject" the reference on it
		 scene.flightRecorder = cmdFR;
		cmdCLI.flightRecorder = cmdFR;

		//only now start the additional controls (console and network)
		CLIcontrol = new Thread( cmdCLI );
		NETcontrol = new Thread( cmdNet );

		CLIcontrol.start();
		NETcontrol.start();

		GUIcontrol = new CommandFromGUI(scene,
			"Controls of SimViewer @ port "+receivingPort,
			(action) -> { CLIcontrol.interrupt(); });
		GUIcontrol.flightRecorder = cmdFR;
		GUIcontrol.refreshPanelState();

		log.info("SimViewer started");

		//and wait until the command line signals the SimViewer to stop
		try {
			CLIcontrol.join();
		}
		catch (InterruptedException e) {
			System.out.println("SimViewer interrupted: "+e.getMessage());
			e.printStackTrace();
		}

		this.stop();
	}

	private DisplayScene scene = null;
	private CommandFromGUI GUIcontrol = null;
	private Thread CLIcontrol = null;
	private Thread NETcontrol = null;

	public void stop()
	{
		//this makes sure that the SciView is not left with some of the SimViewer's node
		//chosen as active (which would result in an ugly "null" message in the log)
		sciView.setActiveNode(sciView.getFloor());

		//close SimViewer's aux services
		if (NETcontrol != null && NETcontrol.isAlive()) NETcontrol.interrupt();
		NETcontrol = null;

		if (CLIcontrol != null && CLIcontrol.isAlive()) CLIcontrol.interrupt();
		CLIcontrol = null;

		if (GUIcontrol != null) GUIcontrol.closePanel();
		GUIcontrol = null;

		//close SimViewer
		if (scene != null)
		{
			//removes all lights, and objects registered with the SimViewer
			scene.stop();
			scene = null;
		}

		//restore the original SciView scene
		enableFloorAndDisabledLights();

		log.info("SimViewer stopped");
	}


	//----------------------------------------------------------------------------
	private boolean floorOriginalVisibilityState = true;
	private final List<Node> disabledLights = new ArrayList<>(10);

	private void disableFloorAndVisibleLights()
	{
		floorOriginalVisibilityState = sciView.getFloor().getVisible();
		sciView.getFloor().setVisible(false);

		for (Node n : sciView.getAllSceneNodes())
		{
			//disable lights, that are enabled and that are not ours
			if (n instanceof Light && n.getVisible() && !n.getName().startsWith("PointLight Ramp"))
			{
				n.setVisible(false);
				disabledLights.add(n);
			}
		}

		//debug:
		for (Node n : disabledLights) { log.info("disabled light: "+n.getName()); }
	}

	private void enableFloorAndDisabledLights()
	{
		Iterator<Node> i = disabledLights.iterator();
		while (i.hasNext())
		{
			i.next().setVisible(true);
			i.remove();
		}

		sciView.getFloor().setVisible(floorOriginalVisibilityState);
	}


	//----------------------------------------------------------------------------
	/**
	 * Entry point for testing SimViewer functionality.,
	 */
	public static void main( String... args ) throws Exception
	{
	    sc.iview.Main.main();
	}
}
