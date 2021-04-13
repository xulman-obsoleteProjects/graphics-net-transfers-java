/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2021 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/

package cz.it4i.ulman.graphics_net_transfers_java.server;

import de.mpicbg.ulman.simviewer.DisplayScene;
import de.mpicbg.ulman.simviewer.elements.Line;
import de.mpicbg.ulman.simviewer.elements.Point;
import de.mpicbg.ulman.simviewer.elements.Vector;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class NetMessagesProcessorListener implements
	ProtocolMessagesProcessorListener
{

	private DisplayScene scene;

	/**
	 * constructor to store the connection to a displayed window that shall be
	 * commanded from the incoming messages
	 */
	public NetMessagesProcessorListener(final DisplayScene _scene) {
		scene = _scene;
	}

	@Override
	public void startProcessingElements(int number, Class<?> type) {
		if (number > 10) scene.suspendNodesUpdating();
	}

	@Override
	public void endProcessingElements(int number, Class<?> type) {
		if (number > 10) scene.resumeNodesUpdating();
	}

	@Override
	public void processPoints(int iD, Point p) {
		scene.addUpdateOrRemovePoint(iD, p);
	}

	@Override
	public void processLines(int iD, Line l) {
		scene.addUpdateOrRemoveLine(iD, l);
	}

	@Override
	public void processVectors(int iD, Vector v) {
		scene.addUpdateOrRemoveVector(iD, v);
	}

	@Override
	public void processTickMessage(String msg) throws InterruptedException {
		log.info("NetMessagesProcessor: Got tick message: {}", msg);

		// check if we should save the screen
		if (scene.savingScreenshots) {
			// give scenery some grace time to redraw everything
			try {
				Thread.sleep(2000);
			}
			catch (InterruptedException e) {
				// a bit unexpected to be stopped here, so we leave a note and forward
				// the exception upstream
				log.info(
					"NetMessagesProcessor: Interrupted just before requesting a screen shot:",
					e);
				throw e;
			}

			scene.saveNextScreenshot();
		}

		if (scene.garbageCollecting) scene.garbageCollect();

		scene.increaseTickCounter();

	}

}
