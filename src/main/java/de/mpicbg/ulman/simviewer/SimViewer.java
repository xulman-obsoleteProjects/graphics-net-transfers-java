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

import graphics.scenery.Node;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import sc.iview.SciView;
import graphics.scenery.Light;

import java.util.ArrayList;
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

	@Override
	public void run()
	{
		log.info("SimViewer started");

		//setup the original sciView scene
		sciView.getFloor().setVisible(false);
		disableVisibleLights();

		//setup the SimViewer's playground..
		DisplayScene scene = new DisplayScene(sciView,
		                                      new float[] {  0.f,  0.f,  0.f},
		                                      new float[] {480.f,220.f,220.f});

		//setup the lights
		scene.CreateFixedLightsRamp();
		scene.ToggleFixedLights();
		scene.ToggleFixedLights();
		scene.ToggleFixedLights();

		//populate the scene initially
		scene.ToggleDisplayAxes();
		scene.ToggleDisplaySceneBorder();
		(new CommandFromCLI(scene)).CreateFakeCells();

		//close SimViewer and restore the original sciView scene
		//scene.stop();
		//enableDisabledLights();
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
}
