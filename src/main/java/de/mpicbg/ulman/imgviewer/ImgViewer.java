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


package de.mpicbg.ulman.imgviewer;

import de.mpicbg.ulman.imgtransfer.ImgTransfer;
import de.mpicbg.ulman.imgtransfer.ProgressCallback;
import de.mpicbg.ulman.simviewer.CommandFromGUI;

import org.scijava.command.Command; //plugin itself
import org.scijava.command.CommandService;
import org.scijava.display.DisplayService;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import net.imagej.axis.Axes;        //image container - showing
import net.imagej.axis.AxisType;
import net.imagej.ImgPlus;
import net.imagej.Dataset;
import net.imagej.DefaultDataset;

import net.imglib2.Interval;
import net.imglib2.img.Img;         //image container - updating
import net.imglib2.img.planar.PlanarImgs;
import net.imglib2.view.Views;
import net.imglib2.Cursor;
import net.imglib2.type.numeric.RealType;

import java.io.IOException;         //network issues handling
import java.net.ProtocolException;

import javax.swing.*;               //network Control Panel
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

import net.imagej.ImageJ; //for the main()



/**
 * ImgViewer.
 *
 * @author Vladimir Ulman
 */
@Plugin(type = Command.class, menuPath = "Plugins>ImgViewer", name = "ImgViewer")
public class ImgViewer<T extends RealType<T>> implements Command
{
	@Parameter(label = "TCP/IP port to listen at:", min="1024")
	private int port = 54545;

	@Parameter(label = "Title of the displayed window:")
	private String windowTitle = "ImgViewer @ localhost:"+port;

	@Parameter(label = "Pixel type of the displayed images:",
	           choices = {"float","16 bit"})
	private String pixelTypeStr = "float";

	@Parameter(label = "How many images to keep (T):", min="1")
	private long tSize = 10;

	@Parameter
	private LogService log;

	@Parameter
	private CommandService cs;

	@Parameter
	private DisplayService ds;

	//these will be instantiated later:
	public Dataset d = null;     //in createContainerImage(), wrapping around the img
	public ImgPlus<T> img;       //in createContainerImage(), the container image itself
	private Thread imgFeeder;    //in run()


	@Override
	public void run()
	{
		//create our own logger (which happens to be an AWT's List)
		CPlog = new ControlPanelLogger();
		CPlog.println("Internal network status:");

		//start the image feeder (will use the this.CPlog)
		keepListening = true;
		imgFeeder = new Thread( new ImageFeeder() );
		imgFeeder.start();

		//create and associate with it its "control panel" (will also use this.CPlog)
		connectControlPanel(imgFeeder);
	}

	@Override
	protected void finalize()
	throws Throwable
	{
		imgFeeder.interrupt();
		log.info("ImgViewer: quitting...");
		super.finalize();
	}


	void createContainerImage(final Interval spatialDimension)
	{
		if (spatialDimension.numDimensions() != 3)
			throw new RuntimeException("Cannot work with images that are not 3-dimensional.");

		final long xSize = spatialDimension.max(0) - spatialDimension.min(0) + 1;
		final long ySize = spatialDimension.max(1) - spatialDimension.min(1) + 1;
		final long zSize = spatialDimension.max(2) - spatialDimension.min(2) + 1;

		//create a "voxel container" image
		final Img<T> i = pixelTypeStr.startsWith("fl")
			? (Img)PlanarImgs.floats(        xSize, ySize, zSize, tSize)
			: (Img)PlanarImgs.unsignedShorts(xSize, ySize, zSize, tSize);
		img = new ImgPlus<>( i, windowTitle, new AxisType[] {Axes.X, Axes.Y, Axes.Z, Axes.TIME} );

		//create, display and associate the container with a soon-to-be-displayed Dataset
		d = new DefaultDataset(log.getContext(),img);
		ds.createDisplay(d);
	}

	private <NT extends RealType<NT>>
	void insertNextImage(final Img<NT> newImg)
	{
		if (newImg == null)
			throw new RuntimeException("Not adding null-ptr image.");

		//the x,y,z image sizes should match... (at least for now)
		if (img.dimension(0) != newImg.dimension(0)
		 || img.dimension(1) != newImg.dimension(1)
		 || (newImg.numDimensions() == 2 && img.dimension(2) != 1)
		 || (newImg.numDimensions() == 3 && img.dimension(2) != newImg.dimension(2)))
			throw new RuntimeException("Not adding next image of incompatible size.");

		//number of pixels of one image (NB: img is PlanarImg)
		final long pxCnt = img.dimension(0) * img.dimension(1) * img.dimension(2);

		//plan:
		//we move t into t-1 iff img.dimension(3) > 1
		//we move newImg into t

		//NB: img is PlanarImg
		Cursor<T> source = img.cursor();
		Cursor<T> target = img.cursor();

		//advance source to 2nd timepoint (if it is available)
		if (img.dimension(3) > 1)
			for (long c = 0; c < pxCnt; ++c) source.next();

		//"shift" timepoints
		for (long t = 1; t < img.dimension(3); ++t)
			for (long c = 0; c < pxCnt; ++c) target.next().set( source.next() );

		Cursor<NT> nSource = Views.flatIterable(newImg).cursor();
		for (long c = 0; c < pxCnt; ++c) target.next().setReal( nSource.next().getRealFloat() );

		//notify the wrapping Dataset about the new content of the container image
		d.update();
	}


	//----------------------------------------------------------------------------
	//internal yet shared flag to possibly stop the network_listening+image_adding
	private boolean keepListening;

	class ImageFeeder implements Runnable
	{
		@Override
		public void run()
		{
			final int timeOutWhileConnectionOpened = 300;

			ImgTransfer Receiver = new ImgTransfer(port, timeOutWhileConnectionOpened, CPlog);
			log.info("ImgViewer: started @ localhost:"+port);

			//loop and listen for new incoming images
			int addedImgCnt = 0;
			boolean waitBeforeListen = false;
			while (keepListening)
			{
				try {
					final ImgPlus<?> i = !waitBeforeListen ? Receiver.receiveImage() : null;
					if (i != null)
					{
						//first image received? create the container image first
						if (d == null) createContainerImage(i);

						//got some, insert it
						log.info("ImgViewer: added a new image, #" + ++addedImgCnt);
						insertNextImage( (Img)i );
					}
					else
					{
						//got nothing, that also means that the Receiver automatically closed itself,
						//wait 30 secs (a grace time, essentially) and re-open it again
						log.info("ImgViewer: received no image, waiting 30 secs before listening again");
						Thread.sleep(30000);
						waitBeforeListen = false;
						Receiver = new ImgTransfer(port, timeOutWhileConnectionOpened, CPlog);
						log.info("ImgViewer: started @ localhost:"+port);
					}
				}
				catch (ProtocolException e) {
					log.warn(e.getMessage());
					waitBeforeListen = true;
				}
				catch (IOException | InterruptedException e) {
					e.printStackTrace(System.out);
					keepListening = false;
				}
			}

			log.info("ImgViewer: not listening now");

			//close the control panel
			if (frame != null)
			{
				frame.setVisible(false);  //don't show
				frame.dispose();          //clear resources
				frame = null;
			}
		}
	}


	//----------------------------------------------------------------------------
	//the main handle to the Control Panel
	private JFrame frame = null;
	private ControlPanelLogger CPlog = null;

	class ControlPanelLogger extends CommandFromGUI.ControlPanelLogger implements ProgressCallback
	{
		@Override
		public void info(String msg) { println(msg); }
		@Override
		public void setProgress(float howFar) {}
	}


	private
	void connectControlPanel(final Thread worker)
	{
		frame = new JFrame("Control panel of "+windowTitle);

		final ActionListener actionStop    = new ThreadStopper(worker);
		final ActionListener actionRestart = new PluginRestarter(actionStop);

		final Button btnStop = new Button("Stop receiving images");
		final Button btnRest = new Button("Restart in a new instance");

		btnStop.addActionListener( actionStop );
		btnRest.addActionListener( actionRestart );

		frame.setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS));
		frame.add(CPlog.textPane);
		frame.add(btnStop);
		frame.add(btnRest);
		frame.pack();
		frame.setVisible(true);
	}


	//button handler to set this.keepListening = false and send interrupt() to the ImageFeeder
	class ThreadStopper implements ActionListener
	{
		private final Thread threadToSignalToStop;

		ThreadStopper(final Thread threadToBeControlled)
		{ threadToSignalToStop = threadToBeControlled; }

		@Override
		public void actionPerformed(ActionEvent e)
		{
			//sending interrupt() typically works, but to be on the safe side
			//we signal to the main loop (in ImageFeeder.run()) to stop too
			keepListening = false;
			threadToSignalToStop.interrupt();
		}
	}

	//button handler to set this.keepListening = false and send interrupt() to the ImageFeeder
	class PluginRestarter implements ActionListener
	{
		private final ActionListener stopper;

		PluginRestarter(final ActionListener threadStopper)
		{ stopper = threadStopper; }

		@Override
		public void actionPerformed(ActionEvent e)
		{
			//perform as if "Stop" button is pressed
			stopper.actionPerformed(null);

			//(re)start this ImgViewer plugin again, and feed it
			//with current values of all parameters, well, and
			//with a modified windowTitle
			Map<String,Object> newArgs = new HashMap<>(8);
			newArgs.put("port",         port);
			newArgs.put("windowTitle",  windowTitle.concat("#"));
			newArgs.put("pixelTypeStr", pixelTypeStr);
			newArgs.put("tSize",        tSize);
			cs.run(ImgViewer.class, true, newArgs);
		}
	}


	//----------------------------------------------------------------------------
	public static void main( String... args )
	{
		//start up our own Fiji/Imagej2
		final ImageJ ij = new net.imagej.ImageJ();
		ij.ui().showUI();

		//run this class as if from GUI
		ij.command().run(ImgViewer.class, true);
	}
}
