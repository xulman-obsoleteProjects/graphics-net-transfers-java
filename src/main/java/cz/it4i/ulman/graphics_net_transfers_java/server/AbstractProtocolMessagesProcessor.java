/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2021 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.ulman.graphics_net_transfers_java.server;

import java.util.Scanner;
import java.util.function.Function;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@AllArgsConstructor
public abstract class AbstractProtocolMessagesProcessor implements
	MessagesProcessor
{

	final private AbstractProtocolMessagesProcessor next;

	final private String prefix;

	@Override
	public void processMsg(String msg) throws InterruptedException {
		try {
			if (!processingMsg(msg) && next != null) {
				next.processMsg(msg);
			}
		}
		catch (ProcessingException exc) {
			log.info(exc.getText(msg));
		}

	}

	private boolean processingMsg(String msg) throws ProcessingException,
		InterruptedException
	{
		if (msg.startsWith(prefix)) {
			Scanner scn = new Scanner(msg);
			scn.next();
			return processingMsg(scn);
		}
		return false;
	}


	abstract protected boolean processingMsg(Scanner scn)
		throws ProcessingException, InterruptedException;
	
	@AllArgsConstructor
	protected static class ProcessingException extends Exception {

		private static final long serialVersionUID = -5466391441717466941L;

		private final Function<String, String> textFunction;

		public String getText(String msg) {
			return textFunction.apply(msg);
		}
	}

}
