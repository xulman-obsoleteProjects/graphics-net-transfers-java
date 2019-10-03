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

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * Creates a (Java Swing?) panel with a control buttons, switches etc. to
 * issue commands to the SimViewer. The commands typically show/hide some
 * type of the recognized graphics that the SimViewer can display.
 *
 * This file was created and is being developed by Vladimir Ulman, 2019.
 */
public class CommandFromGUI
{
	/** constructor that shows and manages a window with the control panel,
	    user has to provide a listener that will be notified when "close" button is pressed */
	public CommandFromGUI(final DisplayScene _scene,
	                      final String showOwnControlPanelWithThisTitle,
	                      final ActionListener onClose)
	{
		scene = _scene;
		if (showOwnControlPanelWithThisTitle != null)
		{
			frame = new JFrame(showOwnControlPanelWithThisTitle);
			frame.add( createPanel(onClose) );
			frame.setMinimumSize( new Dimension(300, 500) );
			showPanel();
		}
		else frame = null;
	}

	/** constructor that does not show and will not manage a window with the control panel,
	    caller must use createPanel() and display it on her side (in her GUI) */
	public CommandFromGUI(final DisplayScene _scene)
	{
		scene = _scene;
		frame = null;
	}

	private final JFrame frame;

	public void showPanel() { if (frame != null) frame.setVisible(true);  }
	public void hidePanel() { if (frame != null) frame.setVisible(false); }

	public void closePanel()
	{
		if (frame != null)
		{
			hidePanel();
			frame.dispose();
		}
	}
	//----------------------------------------------------------------------------

	/** creates the panel with switches to adjust the behaviour of the SimViewer,
	    user has to provide a listener that will be notified when "close" button is pressed */
	JPanel createPanel(final ActionListener onClose)
	{
		final JPanel mainPanel = new JPanel();

		final JPanel SVcontrol = new JPanel(); //SimViewer
		final JPanel EGcontrol = new JPanel(); //EmbryoGen
		final ControlPanelLogger logger = new ControlPanelLogger();

		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		mainPanel.add( SVcontrol );         //, BorderLayout.NORTH );
		mainPanel.add( EGcontrol );
		mainPanel.add( logger.getList() );  //, BorderLayout.SOUTH );

		SVcontrol.setLayout(new GridLayout(8,2));
		EGcontrol.setVisible(false);

		//controls:
		//q, o(UpdatePanel and then OverView),
		//A, B,
		//R, I,
		//1,2, (TextArea for current value)
		//S, D,
		//d,W,
		//v,V,
		//m,M (boolean), showEGpanel
		//
		//EmbryoGenExtra - boolean
		//P, booleans: c,C, l,L,f,F,g,G
		//FR: 7,8,9,0, and TextArea for O
		//
		//logConsole

		Button btn = new Button("Close the SimViewer");
		btn.addActionListener( onClose );
		SVcontrol.add(btn);

		btn = new Button("Update and report settings");
		btn.addActionListener( (action) -> { refreshPanelState(); scene.reportSettings(); } );
		SVcontrol.add(btn);

		return mainPanel;
	}
	//----------------------------------------------------------------------------

	/** updates the panel switches to reflect the current state of the SimViewer */
	public void refreshPanelState()
	{
	}
	//----------------------------------------------------------------------------

	/** reference on the controlled rendering display */
	private final DisplayScene scene;

	/** reference on the currently available FlightRecording: the object
	    must initialized outside and reference on it is given here, otherwise
	    the reference must be null */
	CommandFromFlightRecorder flightRecorder = null;


	class ControlPanelLogger //implements System.out-like functionality
	{
		final List log;

		ControlPanelLogger()
		{
			log = new List(10);
			log.add("Reported status:");
		}

		List getList() { return log; }

		void println(final String msg) { log.add(msg); }
	}
}
