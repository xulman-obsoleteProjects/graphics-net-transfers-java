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
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.PrintStream;
import java.io.IOException;

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
			frame.add( createPanel(onClose, (action) -> { frame.pack(); }) );
			frame.pack();
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
			logger.textPaneAutoScroller.interrupt();
			logger.textArea.removeMouseListener(logger.mouseReader);
			frame.dispose();
		}
	}
	//----------------------------------------------------------------------------

	/** creates the panel with switches to adjust the behaviour of the SimViewer,
	    user has to provide a listener that will be notified when "close" button is pressed,
	    or when the dialog is resized (by enabling the advanced EG controls); one
	    can however provide nulls for the listeners... */
	JPanel createPanel(final ActionListener onClose, final ActionListener onResize)
	{
		//the root JPanel that will be returned
		final JPanel mainPanel = new JPanel();

		//mainPanel = SVcontrol, optional EGcontrol, logger inside a LogPane
		final JPanel SVcontrol = new JPanel(); //SimViewer
		final JPanel EGcontrol = new JPanel(); //EmbryoGen
		final JPanel Logger    = new JPanel(); //own logs
		//
		//optional, thus initially "disabled"
		EGcontrol.setVisible(false);
		//
		logger = new ControlPanelLogger();
		Logger.add( logger.textPane, BorderLayout.CENTER );
		//
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		mainPanel.add( SVcontrol );
		mainPanel.add( EGcontrol );
		mainPanel.add( Logger );

		//SVcontrol = SVupperButtonsGrid, horizontalScreenSavingLine, SVbottomButtonsGrid
		final JPanel SVupperButtonsGrid  = new JPanel();
		final JPanel SVmiddleSSLine      = new JPanel();
		final JPanel SVbottomButtonsGrid = new JPanel();
		//
		SVupperButtonsGrid.setLayout( new GridLayout(5,2));
		SVmiddleSSLine.setLayout(     new BoxLayout(SVmiddleSSLine,BoxLayout.X_AXIS));
		SVbottomButtonsGrid.setLayout(new GridLayout(2,2));
		//
		SVcontrol.setLayout(new BoxLayout(SVcontrol, BoxLayout.Y_AXIS));
		SVcontrol.add( SVupperButtonsGrid );
		SVcontrol.add( SVmiddleSSLine );
		SVcontrol.add( SVbottomButtonsGrid );

		//EGcontrol = directly a grid of controls
		EGcontrol.setLayout(new GridLayout(5,3));

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
		//v,V,
		//booleans: c,C,l,L,f,F,g,G
		//FR: 7,8,9,0, and TextArea for O
		//
		//logConsole

		//content of the SVcontrol:
		//'q'
		JButton btn = new JButton("Close the SimViewer");
		if (onClose != null) btn.addActionListener( onClose );
		SVupperButtonsGrid.add(btn);

		//'o'
		btn = new JButton("Update and report settings");
		btn.addActionListener( (action) -> {
				refreshPanelState();
				scene.reportSettings(logger);
			} );
		SVupperButtonsGrid.add(btn);

		//'A'
		btnOrientationAxes.setText( scene.IsSceneAxesVisible() ? btnOrientationAxesLabel_Disable : btnOrientationAxesLabel_Enable);
		btnOrientationAxes.addActionListener( (action) -> {
				btnOrientationAxes.setText( scene.ToggleDisplayAxes() ? btnOrientationAxesLabel_Disable : btnOrientationAxesLabel_Enable);
			} );
		SVupperButtonsGrid.add(btnOrientationAxes);

		//'B'
		btnSceneBorder.setText( scene.IsSceneBorderVisible() ? btnSceneBorderLabel_Disable : btnSceneBorderLabel_Enable);
		btnSceneBorder.addActionListener( (action) -> {
				btnSceneBorder.setText( scene.ToggleDisplaySceneBorder() ? btnSceneBorderLabel_Disable : btnSceneBorderLabel_Enable);
			} );
		SVupperButtonsGrid.add(btnSceneBorder);

		//'R'
		btn = new JButton("Re-adapt scene size");
		btn.addActionListener( (action) -> { scene.ResizeScene(); } );
		SVupperButtonsGrid.add(btn);

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
		SVupperButtonsGrid.add(scaleHorizontalGroup);

		//'I'
		btnLightRampsSetLabel();
		btnLightRamps.addActionListener( (action) -> {
				scene.ToggleFixedLights();
				btnLightRampsSetLabel();
			} );
		SVupperButtonsGrid.add(btnLightRamps);

		//'m'/'M'
		btnFacesCulling.setText( scene.IsFrontFacesCullingEnabled() ? btnFacesCullingLabel_Disable : btnFacesCullingLabel_Enable );
		btnFacesCulling.addActionListener( (action) -> {
				if ( scene.IsFrontFacesCullingEnabled() )
				{
					scene.DisableFrontFaceCulling();
					btnFacesCulling.setText( btnFacesCullingLabel_Enable );
				}
				else
				{
					scene.EnableFrontFaceCulling();
					btnFacesCulling.setText( btnFacesCullingLabel_Disable );
				}
			} );
		SVupperButtonsGrid.add(btnFacesCulling);

		//'1'
		btnLightRampsDimmer.addActionListener( (action) -> { scene.DecreaseFixedLightsIntensity(); } );
		SVupperButtonsGrid.add(btnLightRampsDimmer);

		//'2'
		btnLightRampsBrighter.addActionListener( (action) -> { scene.IncreaseFixedLightsIntensity(); } );
		SVupperButtonsGrid.add(btnLightRampsBrighter);

		//'S'
		cbxScreenSaving.setSelected( scene.savingScreenshots );
		cbxScreenSaving.addActionListener( (action) -> { scene.savingScreenshots ^= true; } );
		//
		//path with screen shots
		ssPath.setText( scene.savingScreenshotsFilename );
		ssPath.setToolTipText("Enter empty text to bring up a \"File Save\" dialog.");
		ssPath.addActionListener( (action) -> { updateSSPath(mainPanel); } );
		//
		SVmiddleSSLine.add( cbxScreenSaving );
		SVmiddleSSLine.add( ssPath );

		//'d'
		btn = new JButton("Delete not-recently updated objects");
		btn.addActionListener( (action) -> { scene.garbageCollect(1); } );
		SVbottomButtonsGrid.add(btn);

		//'W'
		btn = new JButton("Delete all SimViewer's objects");
		btn.addActionListener( (action) -> { scene.removeAllObjects(); } );
		SVbottomButtonsGrid.add(btn);

		//'D'
		cbxGarbageCtr.setSelected( scene.garbageCollecting );
		cbxGarbageCtr.addActionListener( (action) -> { scene.garbageCollecting ^= true; } );
		SVbottomButtonsGrid.add(cbxGarbageCtr);

		//show EGcontrol
		JCheckBox cbx = new JCheckBox("Advanced controls panel");
		cbx.setSelected( EGcontrol.isVisible() );
		cbx.addActionListener( (action) -> {
				EGcontrol.setVisible( !EGcontrol.isVisible() );
				//notify the listener only after the resize is done
				if (onResize != null) onResize.actionPerformed(action);
			} );
		SVbottomButtonsGrid.add(cbx);

		//content of the EGcontrol:
		//'v'
		btn = new JButton("Down-scale vectors");
		btn.addActionListener( (action) -> {
				scene.setVectorsStretch(0.80f * scene.getVectorsStretch());
				logger.println("new vector stretch: "+scene.getVectorsStretch());
			} );
		EGcontrol.add(btn);

		//'V'
		btn = new JButton("Up-scale vectors");
		btn.addActionListener( (action) -> {
				scene.setVectorsStretch(1.25f * scene.getVectorsStretch());
				logger.println("new vector stretch: "+scene.getVectorsStretch());
			} );
		EGcontrol.add(btn);

		//DEBUG SWITCHES
		cbxDbgMasterCell.setSelected( scene.IsCellDebugShown() );
		cbxDbgMasterCell.addActionListener( (action) -> { scene.ToggleDisplayCellDebug(); } );

		cbxDbgMasterGlobal.setSelected( scene.IsGeneralDebugShown() );
		cbxDbgMasterGlobal.addActionListener( (action) -> { scene.ToggleDisplayGeneralDebug(); } );

		cbxVisCellSpheres.setSelected( scene.IsCellSpheresShown() );
		cbxVisCellSpheres.addActionListener( (action) -> { scene.ToggleDisplayCellSpheres(); } );

		cbxVisCellLines.setSelected( scene.IsCellLinesShown() );
		cbxVisCellLines.addActionListener( (action) -> { scene.ToggleDisplayCellLines(); } );

		cbxVisCellVectors.setSelected( scene.IsCellVectorsShown() );
		cbxVisCellVectors.addActionListener( (action) -> { scene.ToggleDisplayCellVectors(); } );

		cbxVisGlobalSpheres.setSelected( scene.IsGeneralSpheresShown() );
		cbxVisGlobalSpheres.addActionListener( (action) -> { scene.ToggleDisplayGeneralDebugSpheres(); } );

		cbxVisGlobalLines.setSelected( scene.IsGeneralLinesShown() );
		cbxVisGlobalLines.addActionListener( (action) -> { scene.ToggleDisplayGeneralDebugLines(); } );

		cbxVisGlobalVectors.setSelected( scene.IsGeneralVectorsShown() );
		cbxVisGlobalVectors.addActionListener( (action) -> { scene.ToggleDisplayGeneralDebugVectors(); } );

		//FR: 7,8,9,0
		btnFRfirst.setEnabled( flightRecorder != null );
		btnFRfirst.setToolTipText("shortcut key is '7'");
		btnFRfirst.addActionListener( (action) -> {
				try {
					if (!flightRecorder.rewindAndSendFirstTimepoint())
						logger.println("No FlightRecording file is opened.");
				}
				catch (InterruptedException e) {
					logger.println("Problem processing FlightRecording: "+e.getMessage());
				}
			} );

		btnFRprev.setEnabled( flightRecorder != null );
		btnFRprev.setToolTipText("shortcut key is '8'");
		btnFRprev.addActionListener( (action) -> {
				try {
					if (!flightRecorder.sendPrevTimepointMessages())
						logger.println("No FlightRecording file is opened.");
				}
				catch (InterruptedException e) {
					logger.println("Problem processing FlightRecording: "+e.getMessage());
				}
			} );

		btnFRnext.setEnabled( flightRecorder != null );
		btnFRnext.setToolTipText("shortcut key is '9'");
		btnFRnext.addActionListener( (action) -> {
				try {
					if (!flightRecorder.sendNextTimepointMessages())
						logger.println("No FlightRecording file is opened.");
				}
				catch (InterruptedException e) {
					logger.println("Problem processing FlightRecording: "+e.getMessage());
				}
			} );

		btnFRlast.setEnabled( flightRecorder != null );
		btnFRlast.setToolTipText("shortcut key is '0'");
		btnFRlast.addActionListener( (action) -> {
				try {
					if (!flightRecorder.rewindAndSendLastTimepoint())
						logger.println("No FlightRecording file is opened.");
				}
				catch (InterruptedException e) {
					logger.println("Problem processing FlightRecording: "+e.getMessage());
				}
			} );

		//FR: 'O'
		FRfile.setEnabled( flightRecorder != null );
		FRfile.setText( "some FlightRecording file..." );
		FRfile.setToolTipText("Enter empty text to bring up a \"File Open\" dialog.");
		FRfile.addActionListener( (action) -> {
				try {
					//some file name provided or shall we ask for some?
					if (FRfile.getText().length() > 0 || fc.showOpenDialog(mainPanel) == JFileChooser.APPROVE_OPTION )
					{
						//== 0 -> dialog must have been opened, take the file name from it
						if (FRfile.getText().length() == 0)
							FRfile.setText( fc.getSelectedFile().getAbsolutePath() );

						logger.separator();
						flightRecorder.open(FRfile.getText());
						logger.println("Opened this FlightRecording: "+FRfile.getText());
						flightRecorder.sendNextTimepointMessages();
					}
				}
				catch (IOException | InterruptedException e) {
					logger.println("Problem opening a FlightRecording: "+e.getMessage());
				}
			} );

		//finally, add the control in the right order
		EGcontrol.add(FRfile);

		EGcontrol.add(cbxDbgMasterCell);
		EGcontrol.add(cbxDbgMasterGlobal);
		EGcontrol.add(btnFRfirst);

		EGcontrol.add(cbxVisCellSpheres);
		EGcontrol.add(cbxVisGlobalSpheres);
		EGcontrol.add(btnFRprev);

		EGcontrol.add(cbxVisCellLines);
		EGcontrol.add(cbxVisGlobalLines);
		EGcontrol.add(btnFRnext);

		EGcontrol.add(cbxVisCellVectors);
		EGcontrol.add(cbxVisGlobalVectors);
		EGcontrol.add(btnFRlast);

		return mainPanel;
	}
	//----------------------------------------------------------------------------

	final String btnOrientationAxesLabel_Disable = "Disable orientation axes";
	final String btnOrientationAxesLabel_Enable =  "Enable orientation axes";
	final JButton btnOrientationAxes = new JButton( btnOrientationAxesLabel_Enable );

	final String btnSceneBorderLabel_Disable = "Disable scene border";
	final String btnSceneBorderLabel_Enable =  "Enable scene border";
	final JButton btnSceneBorder = new JButton( btnSceneBorderLabel_Enable );

	final SpinnerNumberModel spinner = new SpinnerNumberModel(1.0, 0.02, 1000.0, 0.1);

	final JButton btnLightRamps = new JButton();
	final JButton btnLightRampsDimmer   = new JButton("Make lights dimmer");
	final JButton btnLightRampsBrighter = new JButton("Make lights brighter");
	final String btnLightRampsLabel_None  = "Use both light ramps";
	final String btnLightRampsLabel_Both  = "Use only front light ramp";
	final String btnLightRampsLabel_Front = "Use only rear light ramp";
	final String btnLightRampsLabel_Rear  = "Use no light ramp";
	//
	private void btnLightRampsSetLabel()
	{
		switch (scene.ReportChosenFixedLights())
		{
		case NONE:
			btnLightRamps.setText(btnLightRampsLabel_None);
			break;
		case BOTH:
			btnLightRamps.setText(btnLightRampsLabel_Both);
			break;
		case FRONT:
			btnLightRamps.setText(btnLightRampsLabel_Front);
			break;
		case REAR:
			btnLightRamps.setText(btnLightRampsLabel_Rear);
			break;
		}

		final boolean lightStatus = scene.IsFixedLightsAvailable();
		btnLightRamps.setEnabled(lightStatus);
		btnLightRampsDimmer.setEnabled(lightStatus);
		btnLightRampsBrighter.setEnabled(lightStatus);
	}

	final String btnFacesCullingLabel_Disable = "Make objects solid again";
	final String btnFacesCullingLabel_Enable =  "Allow to see into objects";
	final JButton btnFacesCulling = new JButton( btnFacesCullingLabel_Enable );

	final JCheckBox cbxScreenSaving = new JCheckBox( "Screen saving into " );
	final JTextField ssPath = new JTextField();
	final JFileChooser fc = new JFileChooser();
	//
	void updateSSPath(final JComponent upstreamComp)
	{
		String newPath = ssPath.getText();
		boolean sepPrinted = false;

		if (newPath.length() == 0)
		{
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

	final JCheckBox cbxGarbageCtr = new JCheckBox( "Garbage collection of old objects" );

	final JCheckBox cbxDbgMasterCell    = new JCheckBox( "Cell debug objects" );
	final JCheckBox cbxDbgMasterGlobal  = new JCheckBox( "Global debug object" );
	//
	final JCheckBox cbxVisCellSpheres   = new JCheckBox( "Cell points" );
	final JCheckBox cbxVisCellLines     = new JCheckBox( "Cell lines" );
	final JCheckBox cbxVisCellVectors   = new JCheckBox( "Cell vectors" );
	//
	final JCheckBox cbxVisGlobalSpheres = new JCheckBox( "Global points" );
	final JCheckBox cbxVisGlobalLines   = new JCheckBox( "Global lines" );
	final JCheckBox cbxVisGlobalVectors = new JCheckBox( "Global vectors" );

	final JButton btnFRfirst = new JButton("FR: First TP");
	final JButton btnFRprev  = new JButton("FR: Previous TP");
	final JButton btnFRnext  = new JButton("FR: Next TP");
	final JButton btnFRlast  = new JButton("FR: Last TP");
	final JTextField FRfile  = new JTextField();
	//----------------------------------------------------------------------------

	/** updates the panel switches to reflect the current state of the SimViewer */
	public void refreshPanelState()
	{
		btnOrientationAxes.setText( scene.IsSceneAxesVisible() ? btnOrientationAxesLabel_Disable : btnOrientationAxesLabel_Enable);
		btnSceneBorder.setText( scene.IsSceneBorderVisible() ? btnSceneBorderLabel_Disable : btnSceneBorderLabel_Enable);
		spinner.setValue( scene.DsFactor );
		btnLightRampsSetLabel();
		btnFacesCulling.setText( scene.IsFrontFacesCullingEnabled() ? btnFacesCullingLabel_Disable : btnFacesCullingLabel_Enable );
		cbxScreenSaving.setSelected( scene.savingScreenshots );
		ssPath.setText( scene.savingScreenshotsFilename );
		cbxGarbageCtr.setSelected( scene.garbageCollecting );
		FRfile.setEnabled( flightRecorder != null );
		btnFRfirst.setEnabled( flightRecorder != null );
		btnFRprev.setEnabled( flightRecorder != null );
		btnFRnext.setEnabled( flightRecorder != null );
		btnFRlast.setEnabled( flightRecorder != null );
		cbxDbgMasterCell.setSelected( scene.IsCellDebugShown() );
		cbxDbgMasterGlobal.setSelected( scene.IsGeneralDebugShown() );
		cbxVisCellSpheres.setSelected( scene.IsCellSpheresShown() );
		cbxVisCellLines.setSelected( scene.IsCellLinesShown() );
		cbxVisCellVectors.setSelected( scene.IsCellVectorsShown() );
		cbxVisGlobalSpheres.setSelected( scene.IsGeneralSpheresShown() );
		cbxVisGlobalLines.setSelected( scene.IsGeneralLinesShown() );
		cbxVisGlobalVectors.setSelected( scene.IsGeneralVectorsShown() );
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
		//content...
		static private final String newline = "\n";
		private final JTextArea textArea;

		//form...
		final JScrollPane        textPane;
		private final Thread     textPaneAutoScroller;
		private final JScrollBar textPaneScrlBar;

		//to aid auto-scrolling
		private boolean contentChanged = false;
		private boolean scrollingEnabled = true;

		ControlPanelLogger()
		{
			super(System.out);

			//create the text container itself
			textArea = new JTextArea(10,48);
			textArea.setEditable(false);
			textArea.addMouseListener(mouseReader);
			textArea.append("Reported status:" +newline);

			//place it into a scrollable Container
			textPane = new JScrollPane( textArea,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED );

			//and setup the autoscrolling, that makes the container to
			//always show bottom of the text area
			textPaneScrlBar = textPane.getVerticalScrollBar();
			textPaneAutoScroller = new Thread( () -> {
					try {
						while (true)
						{
							Thread.sleep(2000);
							//only scroll down if there is something new (we could have always
							//asked to scroll down but we hope testing the flag is way faster)
							//and if the area is not "occupied" by the user (e.g. by reading it)
							if (contentChanged && scrollingEnabled)
							{
								scrollDownThePane();
								contentChanged = false;
							}
						}
					} catch (InterruptedException e) {}
				} );
			textPaneAutoScroller.start();

			//closePanel() of the containing class removes the mouse listener and stops the autoscrolling thread
		}

		final MouseListener mouseReader = new MouseListener() {
			@Override
			public void mouseClicked(MouseEvent e) { }
			@Override
			public void mousePressed(MouseEvent e) { }
			@Override
			public void mouseReleased(MouseEvent e) { }
			@Override
			public void mouseEntered(MouseEvent e) { scrollingEnabled = false; }
			@Override
			public void mouseExited(MouseEvent e) { scrollingEnabled = true; }
		};

		@Override
		public void println(final String msg)
		{
			textArea.append(msg +newline);
			contentChanged = true;
		}

		@Override
		public void println()
		{
			textArea.append(newline);
			contentChanged = true;
		}

		public void separator()
		{
			dateObj.setTime( System.currentTimeMillis() );
			textArea.append("----------------------- " + dateObj.toString() + " -----------------------" +newline);
			contentChanged = true;
		}
		//
		//to avoid re-new()-ing with every new call of separator()
		private java.util.Date dateObj = new java.util.Date();

		public void scrollDownThePane()
		{
			textPaneScrlBar.setValue( textPaneScrlBar.getMaximum() );
		}
	}
}
