/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2021 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.ulman.graphics_net_transfers_java.server;

import java.util.Scanner;

import org.joml.Vector3f;

public class ProtocolV2MessagesProcessor extends ProtocolV1MessagesProcessor {

	public ProtocolV2MessagesProcessor() {
		this(new ProtocolV1MessagesProcessor(), "v2");
	}

	protected ProtocolV2MessagesProcessor(AbstractProtocolMessagesProcessor next,
		String prefix)
	{
		super(next, prefix);
	}

	@Override
	protected void readColor(Scanner s, Vector3f color) {
		color.x = s.nextFloat();
		color.y = s.nextFloat();
		color.z = s.nextFloat();
	}
}
