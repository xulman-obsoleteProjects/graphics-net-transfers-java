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

import net.imglib2.img.planar.PlanarImgs;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.Cursor;

//for the main:
import net.imagej.ImageJ;


/**
 * ImgViewer.
 *
 * @author Vladimir Ulman
 */
@Plugin(type = Command.class, menuPath = "ImgViewer", name = "ImgViewer")
public class ImgViewer implements Command
{
	@Parameter
	private LogService log;

	@Parameter(label = "TCP/IP port to listen at:", min="1024")
	private int port = 5678;

	@Parameter(label = "Title of the displayed window:")
	private String windowTitle = "ImgViewer @ localhost:"+port;

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


	@Override
	public void run()
	{
		//create a "voxel container" image
		final ImgPlus<UnsignedShortType> img
			= new ImgPlus<>( PlanarImgs.unsignedShorts(xSize, ySize, zSize, tSize),
			windowTitle, new AxisType[] { Axes.X, Axes.Y, Axes.Z, Axes.TIME} );

		//create and associate the container with a soon-to-be-displayed Dataset
		d = new DefaultDataset(log.getContext(),img);

		//final remark, the initialization is done by now
		log.info("ImgViewer started @ localhost:"+port);


		//place some initial fake values
		final Cursor<UnsignedShortType> c = img.cursor();
		int cnt = 0;
		while (c.hasNext())
			c.next().set(cnt++);


		//create some fake updater to see the Dataset window changing content "randomly"
		final Thread imgUpdater = new Thread("ImgViewer Updater")
		{
			@Override
			public void run()
			{
			    int changesCnt=0;
			    int cnt = 0;
			    while (changesCnt < 10)
				{
					try {
						Thread.sleep(4000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

					c.reset();
					while (c.hasNext())
						c.next().set(cnt++);
					d.update();

					++changesCnt;
				}
			}
		};
		imgUpdater.start();

		//start up another thread that
		//  - will be receiving images via network connection
		//  - will be storing a buffer of last N images
		//  - will be pushing image chosen from the buffer to the PlanarImg and asking Fiji to re-draw
		//
	}

	//----------------------------------------------------------------------------
	/**
	 * Entry point for testing SciView functionality.
	 *
	 * @author Kyle Harrington
	 */
	public static void main( String... args )
	{
		//start up our own Fiji/Imagej2
		final ImageJ ij = new net.imagej.ImageJ();
		ij.ui().showUI();

		//run this class as if from GUI
		ij.command().run(ImgViewer.class, true);
	}
}
