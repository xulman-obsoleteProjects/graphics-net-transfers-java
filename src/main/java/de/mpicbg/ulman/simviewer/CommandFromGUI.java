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
import java.io.PrintStream;

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
			frame.setMinimumSize( new Dimension(700, 500) );
			refreshPanelState();
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

	public void showPanel()
	{
		if (frame != null)
		{
			frame.setVisible(true);
			System.setOut( logger );
		}
	}
	public void hidePanel()
	{
		if (frame != null)
		{
			System.setOut( System.out );
			frame.setVisible(false);
		}
	}

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
		logger = new ControlPanelLogger();

		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		mainPanel.add( SVcontrol );         //, BorderLayout.NORTH );
		mainPanel.add( EGcontrol );
		mainPanel.add( logger.getList() );  //, BorderLayout.SOUTH );

		SVcontrol.setLayout(new GridLayout(8,2));
		EGcontrol.setVisible(false);

		//controls:
		//q, o(UpdatePanel and then OverView),
		//A, B,
		//R, scale!
		//I, [mM] (boolean)
		//1,2, (TextArea for current value)
		//S, path
		//d,W,
		//D, showEGpanel
		//
		//EmbryoGenExtra - boolean
		//P, booleans: c,C, l,L,f,F,g,G
		//v,V,
		//FR: 7,8,9,0, and TextArea for O
		//
		//logConsole

		//'q'
		Button btn = new Button("Close the SimViewer");
		btn.addActionListener( onClose );
		SVcontrol.add(btn);

		//'o'
		btn = new Button("Update and report settings");
		btn.addActionListener( (action) -> {
				refreshPanelState();
				scene.reportSettings(logger);
			} );
		SVcontrol.add(btn);

		//'A'
		btnOrientationAxes.addActionListener( (action) -> {
				btnOrientationAxes.setLabel( scene.ToggleDisplayAxes() ? btnOrientationAxesLabel_Disable : btnOrientationAxesLabel_Enable);
			} );
		SVcontrol.add(btnOrientationAxes);

		//'B'
		btnSceneBorder.addActionListener( (action) -> {
				btnSceneBorder.setLabel( scene.ToggleDisplaySceneBorder() ? btnSceneBorderLabel_Disable : btnSceneBorderLabel_Enable);
			} );
		SVcontrol.add(btnSceneBorder);

		//'R'
		btn = new Button("Re-adapt scene size");
		btn.addActionListener( (action) -> { scene.ResizeScene(); } );
		SVcontrol.add(btn);

		//overAll scale
		spinner.setValue( scene.DsFactor );
		spinner.addChangeListener( (action) -> { scene.RescaleScene(spinner.getNumber().floatValue()); } );
		//
		final JLabel sLabel = new JLabel("Scale the whole scene: ");
		sLabel.setBorder( BorderFactory.createEmptyBorder(0,8,0,0) );
		//
		final JSpinner sSpin = new JSpinner(spinner);
		sSpin.setBorder( BorderFactory.createEmptyBorder(0,0,0,7) );
		//
		final JPanel scaleHorizontalGroup = new JPanel();
		scaleHorizontalGroup.setLayout( new BoxLayout(scaleHorizontalGroup, BoxLayout.X_AXIS) );
		scaleHorizontalGroup.add( sLabel );
		scaleHorizontalGroup.add( sSpin );
		SVcontrol.add(scaleHorizontalGroup);

		//'I'
		btnLightRampsSetLabel();
		btnLightRamps.addActionListener( (action) -> {
				scene.ToggleFixedLights();
				btnLightRampsSetLabel();
			} );
		SVcontrol.add(btnLightRamps);

		//'m'/'M'
		btnFacesCulling.addActionListener( (action) -> {
				if ( scene.IsFrontFacesCullingEnabled() )
				{
					scene.DisableFrontFaceCulling();
					btnFacesCulling.setLabel( btnFacesCullingLabel_Enable );
				}
				else
				{
					scene.EnableFrontFaceCulling();
					btnFacesCulling.setLabel( btnFacesCullingLabel_Disable );
				}
			} );
		SVcontrol.add(btnFacesCulling);

		//'1'
		btnLightRampsDimmer.addActionListener( (action) -> { scene.DecreaseFixedLightsIntensity(); } );
		SVcontrol.add(btnLightRampsDimmer);

		//'2'
		btnLightRampsBrighter.addActionListener( (action) -> { scene.IncreaseFixedLightsIntensity(); } );
		SVcontrol.add(btnLightRampsBrighter);

		//'S'
		cbxScreenSaving.setSelected( scene.savingScreenshots );
		cbxScreenSaving.addActionListener( (action) -> { scene.savingScreenshots ^= true; } );
		SVcontrol.add( cbxScreenSaving );
		//
		//path with screen shots
		ssPath.setText( scene.savingScreenshotsFilename );
		ssPath.addActionListener( (action) -> { updateSSPath(mainPanel); } );
		SVcontrol.add(ssPath);

		return mainPanel;
	}
	//----------------------------------------------------------------------------

	final String btnOrientationAxesLabel_Disable = "Disable orientation axes";
	final String btnOrientationAxesLabel_Enable =  "Enable orientation axes";
	final Button btnOrientationAxes = new Button( btnOrientationAxesLabel_Enable );

	final String btnSceneBorderLabel_Disable = "Disable scene border";
	final String btnSceneBorderLabel_Enable =  "Enable scene border";
	final Button btnSceneBorder = new Button( btnSceneBorderLabel_Enable );

	final SpinnerNumberModel spinner = new SpinnerNumberModel(1.0, 0.02, 1000.0, 0.1);

	final Button btnLightRamps = new Button();
	final Button btnLightRampsDimmer   = new Button("Make lights dimmer");
	final Button btnLightRampsBrighter = new Button("Make lights brighter");
	final String btnLightRampsLabel_None  = "Use both light ramps";
	final String btnLightRampsLabel_Both  = "Use front light ramp";
	final String btnLightRampsLabel_Front = "Use rear light ramp";
	final String btnLightRampsLabel_Rear  = "Use no light ramp";
	//
	private void btnLightRampsSetLabel()
	{
		switch (scene.ReportChosenFixedLights())
		{
		case NONE:
			btnLightRamps.setLabel(btnLightRampsLabel_None);
			break;
		case BOTH:
			btnLightRamps.setLabel(btnLightRampsLabel_Both);
			break;
		case FRONT:
			btnLightRamps.setLabel(btnLightRampsLabel_Front);
			break;
		case REAR:
			btnLightRamps.setLabel(btnLightRampsLabel_Rear);
			break;
		}

		final boolean lightStatus = scene.IsFixedLightsAvailable();
		btnLightRamps.setEnabled(lightStatus);
		btnLightRampsDimmer.setEnabled(lightStatus);
		btnLightRampsBrighter.setEnabled(lightStatus);
	}

	final String btnFacesCullingLabel_Disable = "Make objects solid again";
	final String btnFacesCullingLabel_Enable =  "Allow to see into objects";
	final Button btnFacesCulling = new Button( btnFacesCullingLabel_Enable );

	final JCheckBox cbxScreenSaving = new JCheckBox( "Screen saving into " );
	final TextField ssPath = new TextField();
	//
	void updateSSPath(final Component upstreamComp)
	{
		String newPath = ssPath.getText();
		boolean sepPrinted = false;

		if (newPath.length() == 0)
		{
			final JFileChooser fc = new JFileChooser();

			if ( fc.showSaveDialog(upstreamComp) == JFileChooser.APPROVE_OPTION )
			{
				newPath = fc.getSelectedFile().getAbsolutePath();
				if ( !isValidSSPath(newPath) )
				{
					//try to insert %04d before the last '.'
					logger.separator(); sepPrinted = true;
					logger.println("Warning, injecting %04d into your filename: "+newPath);
					int i = newPath.lastIndexOf('.');
					if (i > -1)
						newPath = newPath.substring(0,i)+"%04d"+newPath.substring(i);
				}
			}
			else
				newPath = scene.savingScreenshotsFilename;
		}

		if (isValidSSPath(newPath))
		{
			//update only if all tests passed
			scene.savingScreenshotsFilename = newPath;
		}
		else
		{
			//else report how to fix the path
			if (!sepPrinted) logger.separator();
			logger.println("Your filename: "+newPath);
			logger.println("must contain character '%', followed by zero or more digits, followed by 'd', e.g.,");
			logger.println("%03d to obtain zero-padded three-digits-wide numbering or %d numbering w/o padding.");
		}

		//in any case, synchronize the visible text with the current internal one
		ssPath.setText( scene.savingScreenshotsFilename );
	}
	//
	boolean isValidSSPath(final String newPath)
	{
		//test if the newPath is valid, that is, if it contains the '%[0-9]*d'
		int i = newPath.indexOf('%');
		if (i == -1) return false;

		int d = newPath.indexOf('d', i);
		if (d == -1) return false;

		if (d-i > 1)
		{
			//test if there are only digits inbetween the '%' and 'd'
			final char[] chars = new char[d-i-1];
			newPath.getChars(i+1,d,chars,0);
			for (char c : chars) if (c < 48 || c > 57) return false;
		}

		return true;
	}
	//----------------------------------------------------------------------------

	/** updates the panel switches to reflect the current state of the SimViewer */
	public void refreshPanelState()
	{
		btnSceneBorder.setLabel( scene.IsSceneBorderVisible() ? btnSceneBorderLabel_Disable : btnSceneBorderLabel_Enable);
		btnOrientationAxes.setLabel( scene.IsSceneAxesVisible() ? btnOrientationAxesLabel_Disable : btnOrientationAxesLabel_Enable);
		btnLightRampsSetLabel();
		cbxScreenSaving.setSelected( scene.savingScreenshots );
		ssPath.setText( scene.savingScreenshotsFilename );
		spinner.setValue( scene.DsFactor );
	}
	//----------------------------------------------------------------------------

	/** reference on the controlled rendering display */
	private final DisplayScene scene;

	/** reference on the currently available FlightRecording: the object
	    must initialized outside and reference on it is given here, otherwise
	    the reference must be null */
	CommandFromFlightRecorder flightRecorder = null;

	private ControlPanelLogger logger = null;

	/** implements functionality of the System.out.println() to print into the List panel,
	    everything else is routed to the standard current System.out */
	class ControlPanelLogger extends PrintStream
	{
		private final List log;

		ControlPanelLogger()
		{
			super(System.out);

			log = new List(10);
			log.add("Reported status:");
		}

		public List getList() { return log; }

		@Override
		public void println(final String msg) { log.add(msg); }

		@Override
		public void println() { log.add("  "); }

		public void separator()
		{
			dateObj.setTime( System.currentTimeMillis() );
			log.add("-------------------------- " + dateObj.toString() + " --------------------------");
		}
		//
		//to avoid re-new()-ing with every new call of separator()
		private java.util.Date dateObj = new java.util.Date();
	}
}
