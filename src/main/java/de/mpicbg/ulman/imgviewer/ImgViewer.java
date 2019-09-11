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
import java.io.IOException;
import java.net.ProtocolException;

import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import static org.scijava.ItemIO.*;

import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.ImgPlus;
import net.imagej.Dataset;
import net.imagej.DefaultDataset;

import net.imglib2.img.Img;
import net.imglib2.img.planar.PlanarImgs;
import net.imglib2.view.Views;
import net.imglib2.Cursor;
import net.imglib2.type.numeric.RealType;

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

	@Parameter(label = "Width of the displayed images (X):", min="1")
	private int xSize = 200;

	@Parameter(label = "Height of the displayed images (Y):", min="1")
	private int ySize = 200;

	@Parameter(label = "Depth of the displayed images (Z):", min="1")
	private int zSize = 100;

	@Parameter(label = "How many images to keep (T):", min="1")
	private int tSize = 10;

	@Parameter(type = OUTPUT)
	public Dataset d;

	@Parameter
	private LogService log;

	private ImgPlus<T> img;   //to be yet instantiated in this.run()
	private Thread imgFeeder; //to be yet instantiated in this.run()


	@Override
	public void run()
	{
		//create a "voxel container" image
		final Img<T> i = pixelTypeStr.startsWith("fl")
			? (Img)PlanarImgs.floats(        xSize, ySize, zSize, tSize)
			: (Img)PlanarImgs.unsignedShorts(xSize, ySize, zSize, tSize);
		img = new ImgPlus<>( i, windowTitle, new AxisType[] {Axes.X, Axes.Y, Axes.Z, Axes.TIME} );

		//create and associate the container with a soon-to-be-displayed Dataset
		d = new DefaultDataset(log.getContext(),img);

		//start the image feeder
		imgFeeder = new Thread( new ImageFeeder() );
		imgFeeder.start();
	}

	@Override
	protected void finalize()
	throws Throwable
	{
		imgFeeder.interrupt();
		log.info("ImgViewer: quitting...");
		super.finalize();
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

		log.info("ImgViewer: added a new image");

		//notify the wrapping Dataset about the new content of the container image
		d.update();
	}


	class ImageFeeder implements Runnable
	{
		@Override
		public void run()
		{
			final int timeOutWhileConnectionOpened = 300;

			ImgTransfer Receiver = new ImgTransfer(port, timeOutWhileConnectionOpened, null);
			log.info("ImgViewer: started @ localhost:"+port);

			//loop and listen for new incoming images
			boolean waitBeforeListen = false;
			boolean keepListening = true;
			while (keepListening)
			{
				try {
					final ImgPlus<?> i = !waitBeforeListen ? Receiver.receiveImage() : null;
					if (i != null)
					{
						//got some, insert it
						insertNextImage( (Img)i );
					}
					else
					{
						//got nothing, that also means that the Receiver automatically closed itself,
						//wait 30 secs (a grace time, essentially) and re-open it again
						log.info("ImgViewer: received no image, waiting 30 secs before listening again");
						Thread.sleep(30000);
						waitBeforeListen = false;
						Receiver = new ImgTransfer(port, timeOutWhileConnectionOpened, null);
						log.info("ImgViewer: started @ localhost:"+port);
					}
				}
				catch (ProtocolException e) {
					log.warn(e.getMessage());
					waitBeforeListen = true;
				}
				catch (IOException | InterruptedException e) {
					e.printStackTrace();
					keepListening = false;
				}
			}

			log.info("ImgViewer: not listening now");
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
