/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2021 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/

package cz.it4i.ulman.graphics_net_transfers_java.server;

import de.mpicbg.ulman.simviewer.elements.Line;
import de.mpicbg.ulman.simviewer.elements.Point;
import de.mpicbg.ulman.simviewer.elements.Vector;

public interface ProtocolMessagesProcessorListener {

	void startProcessingElements(int number, Class<?> type);

	void endProcessingElements(int number, Class<?> type);

	void processPoints(int iD, Point p);

	void processLines(int iD, Line l);

	void processVectors(int iD, Vector v);

	void processTickMessage(String nextLine) throws InterruptedException;
}
