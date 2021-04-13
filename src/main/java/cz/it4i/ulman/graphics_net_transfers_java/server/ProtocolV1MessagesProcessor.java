/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2021 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.ulman.graphics_net_transfers_java.server;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.function.Consumer;

import org.joml.Vector3f;

import de.mpicbg.ulman.simviewer.elements.Line;
import de.mpicbg.ulman.simviewer.elements.Point;
import de.mpicbg.ulman.simviewer.elements.Vector;


public class ProtocolV1MessagesProcessor extends
	AbstractProtocolMessagesProcessor
{

	final private Collection<ProtocolMessagesProcessorListener> listeners =
		new LinkedList<>();

	public ProtocolV1MessagesProcessor()
	{
		this(null, "v1");
	}

	public void addProtocolListener(ProtocolMessagesProcessorListener listener) {
		listeners.add(listener);
	}

	protected ProtocolV1MessagesProcessor(
		AbstractProtocolMessagesProcessor next, String prefix)
	{
		super(next, prefix);
	}


	@Override
	protected boolean processingMsg(Scanner scn) throws ProcessingException,
		InterruptedException
	{
		switch (scn.next()) {
			case "points":
				processPoints(scn);
				break;
			case "lines":
				processLines(scn);
				break;
			case "vectors":
				processVectors(scn);
				break;
			case "triangles":
				processTriangles(scn);
				break;
			case "tick":
				processTickMessage(scn);
				break;
			default:
				return false;
		}
		return true;
	}

	

	protected void readColor(Scanner s, Vector3f color) {
		final int colorIndex = s.nextInt();
		switch (colorIndex) {
			case 1:
				color.x = 1.0f;
				color.y = 0.0f;
				color.z = 0.0f;
				break;
			case 2:
				color.x = 0.0f;
				color.y = 1.0f;
				color.z = 0.0f;
				break;
			case 3:
				color.x = 0.0f;
				color.y = 0.0f;
				color.z = 1.0f;
				break;
			case 4:
				color.x = 0.0f;
				color.y = 1.0f;
				color.z = 1.0f;
				break;
			case 5:
				color.x = 1.0f;
				color.y = 0.0f;
				color.z = 1.0f;
				break;
			case 6:
				color.x = 1.0f;
				color.y = 1.0f;
				color.z = 0.0f;
				break;
			default:
				color.x = 1.0f;
				color.y = 1.0f;
				color.z = 1.0f;
		}
	
	}

	private void processPoints(Scanner s) throws ProcessingException {

		try {
			final int N = s.nextInt();
			startProcessingElements(N, Point.class);
			// is the next token 'dim'?
			if (s.next("dim").startsWith("dim") == false) {
				throw new ProcessingException(
					msg -> "NetMessagesProcessor: Don't understand this msg: " + msg);
			}

			// so the next token is dimensionality of the points
			final int D = s.nextInt();

			// now, point by point is reported
			final Point p = new Point();

			for (int n = 0; n < N; ++n) {
				// extract the point ID
				int ID = s.nextInt();

				// now read and save coordinates
				int d = 0;
				for (; d < D && d < 3; ++d)
					p.centre.setComponent(d, s.nextFloat());
				// read possibly remaining coordinates (for which we have no room to
				// store them)
				for (; d < D; ++d)
					s.nextFloat();
				// NB: all points in the same message (in this function call) are of the
				// same dimensionality

				p.radius.x = s.nextFloat();
				p.radius.y = p.radius.x;
				p.radius.z = p.radius.x;
				readColor(s, p.colorRGB);

				processPoints(ID, p);
			}
			endProcessingElements(N, Point.class);
		}
		finally {
			s.close();
		}
	}

	private void processLines(Scanner s) throws ProcessingException {
		try {
			final int N = s.nextInt();

			startProcessingElements(N, Line.class);

			// is the next token 'dim'?
			if (s.next("dim").startsWith("dim") == false) {
				throw new ProcessingException(
					msg -> "NetMessagesProcessor: Don't understand this msg: " + msg);

			}

			// so the next token is dimensionality of the points
			final int D = s.nextInt();

			// now, point pair by pair is reported
			final Line l = new Line();

			for (int n = 0; n < N; ++n) {
				// extract the point ID
				int ID = s.nextInt();

				// now read the first in the pair and save coordinates
				int d = 0;
				for (; d < D && d < 3; ++d)
					l.base.setComponent(d, s.nextFloat());
				// read possibly remaining coordinates (for which we have no room to
				// store
				// them)
				for (; d < D; ++d)
					s.nextFloat();

				// now read the second in the pair and save sizes
				d = 0;
				for (; d < D && d < 3; ++d)
					l.vector.setComponent(d, s.nextFloat() - l.base.get(d));
				// read possibly remaining coordinates (for which we have no room to
				// store
				// them)
				for (; d < D; ++d)
					s.nextFloat();

				readColor(s, l.colorRGB);

				processLines(ID, l);
			}

			endProcessingElements(N, Line.class);
		}
		finally {
			s.close();
		}

	}

	private void processVectors(Scanner s) throws ProcessingException {
		try {

			final int N = s.nextInt();

			startProcessingElements(N, Vector.class);

			// is the next token 'dim'?
			if (s.next("dim").startsWith("dim") == false) {
				throw new ProcessingException(
					msg -> "NetMessagesProcessor: Don't understand this msg: " + msg);

			}

			// so the next token is dimensionality of the points
			final int D = s.nextInt();

			// now, point pair by pair is reported
			final Vector v = new Vector();

			for (int n = 0; n < N; ++n) {
				// extract the point ID
				int ID = s.nextInt();

				// now read the first in the pair and save coordinates
				int d = 0;
				for (; d < D && d < 3; ++d)
					v.base.setComponent(d, s.nextFloat());
				// read possibly remaining coordinates (for which we have no room to
				// store
				// them)
				for (; d < D; ++d)
					s.nextFloat();

				// now read the second in the pair and save sizes
				d = 0;
				for (; d < D && d < 3; ++d)
					v.vector.setComponent(d, s.nextFloat());
				// read possibly remaining coordinates (for which we have no room to
				// store
				// them)
				for (; d < D; ++d)
					s.nextFloat();

				readColor(s, v.colorRGB);

				processVectors(ID, v);
			}

			endProcessingElements(N, Vector.class);
		}
		finally {
			s.close();
		}
	}

	private void processTickMessage(Scanner scn) throws InterruptedException {
		processingTickMessage(scn.nextLine());
	}

	private void startProcessingElements(int number, Class<?> type) {
		applyOnListeners(l -> l.startProcessingElements(number, type));
	}

	private void endProcessingElements(int number, Class<?> type) {
		applyOnListeners(l -> l.endProcessingElements(number, type));
	}

	private void processPoints(int iD, Point p) {
		applyOnListeners(l -> l.processPoints(iD, p));
	}

	private void processLines(int iD, Line line) {
		applyOnListeners(l -> l.processLines(iD, line));
	}

	private void processVectors(int iD, Vector v) {
		applyOnListeners(l -> l.processVectors(iD, v));
	}

	private void processTriangles(Scanner scn) throws ProcessingException {
		scn.close();
		throw new ProcessingException(
			msg -> "NetMessagesProcessor: not implemented yet: " + msg);
	
	}

	private void processingTickMessage(String nextLine)
		throws InterruptedException
	{
		for (ProtocolMessagesProcessorListener l : listeners) {
			l.processTickMessage(nextLine);
		}
	}

	private void applyOnListeners(
		Consumer<ProtocolMessagesProcessorListener> consumer)
	{
		for (ProtocolMessagesProcessorListener l : listeners) {
			consumer.accept(l);
		}
	}

}
