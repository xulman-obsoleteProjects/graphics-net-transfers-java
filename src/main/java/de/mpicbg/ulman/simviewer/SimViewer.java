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

// ------- to satisfy the main() method -------
import graphics.scenery.SceneryBase;
import io.scif.SCIFIOService;
import net.imagej.ImageJService;
import org.scijava.Context;
import org.scijava.service.SciJavaService;
import org.scijava.thread.ThreadService;
import org.scijava.ui.UIService;
import sc.iview.SciViewService;

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
	@Parameter(required = false)
	private String initSequence = "";

	@Parameter(min="1024")
	private int receivingPort = 8765;
	@Override
	public void run()
	{
		log.info("SimViewer initializing");

		//setup the original sciView scene
		sciView.getFloor().setVisible(false);
		disableVisibleLights();

		//setup the SimViewer's playground..
		DisplayScene scene = new DisplayScene(sciView,
		                                      new float[] {  0.f,  0.f,  0.f},
		                                      new float[] {480.f,220.f,220.f});

		//setup our own lights
		scene.CreateFixedLightsRamp();
		scene.ToggleFixedLights();

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

		log.info("SimViewer started");

		//and wait until the command line signals the SimView to stop
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
	private Thread CLIcontrol = null;
	private Thread NETcontrol = null;

	public void stop()
	{
		//close SimViewer's aux services
		if (NETcontrol != null && NETcontrol.isAlive()) NETcontrol.interrupt();
		NETcontrol = null;

		if (CLIcontrol != null && CLIcontrol.isAlive()) CLIcontrol.interrupt();
		CLIcontrol = null;

		//close SimViewer
		if (scene != null)
		{
			//removes all lights, and objects registered with the SimViewer
			scene.stop();
			scene = null;
		}

		log.info("SimViewer stopped");
	}

	//----------------------------------------------------------------------------
	List<Node> disabledLights = new ArrayList<>(10);

	private void disableVisibleLights()
	{
		for (Node n : sciView.getAllSceneNodes())
		{
			//debug:
			//log.info("considering "+n.getName()+" of class "+n.getClass().getSimpleName());
			if (n instanceof Light && n.getVisible())
			{
				n.setVisible(false);
				disabledLights.add(n);
			}
		}

		//debug:
		for (Node n : disabledLights) { log.info("disabled light: "+n.getName()); }
	}

	private void enableDisabledLights()
	{
		for (Node n : disabledLights)
		{
			n.setVisible(true);
			disabledLights.remove(n);
		}
	}

	//----------------------------------------------------------------------------
	/**
	 * Entry point for testing SciView functionality.
	 *
	 * @author Kyle Harrington
	 */
	public static void main( String... args )
	{
		SceneryBase.xinitThreads();

		System.setProperty( "scijava.log.level:sc.iview", "info" );
		Context context = new Context( ImageJService.class, SciJavaService.class, SCIFIOService.class, ThreadService.class);

		UIService ui = context.service( UIService.class );
		if( !ui.isVisible() ) ui.showUI();

		context.service( SciViewService.class ).getOrCreateActiveSciView();
	}
}
