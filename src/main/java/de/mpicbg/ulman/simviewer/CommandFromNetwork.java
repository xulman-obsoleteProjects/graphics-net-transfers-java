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

import org.zeromq.SocketType;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

import de.mpicbg.ulman.simviewer.util.NetMessagesProcessor;

/**
 * Operates on a network socket and listens for incoming messages.
 * Owing to this class, the messages can be extracted from the network, and
 * be processed with the NetMessagesProcessor to update the SimViewer's
 * displayed content. This way, an online view on the current state of
 * a running simulation can be observed provided the simulator is "broadcasting"
 * the relevant messages (including the "tick" messages to denote between
 * individual simulated time points).
 *
 * This file was created and is being developed by Vladimir Ulman, 2018.
 */
public class CommandFromNetwork implements Runnable
{
	/** constructor to create connection (listening at the 8765 port),
	    and link it to a shared NetMessagesProcessor (that connects this
	    'commander' to the displayed window */
	public CommandFromNetwork(final NetMessagesProcessor nmp)
	{
		netMsgProcessor = nmp;
		listenOnPort = 8765;
	}

	/** constructor to create connection (listening at the given port),
	    and link it to a shared NetMessagesProcessor (that connects this
	    'commander' to the displayed window */
	public CommandFromNetwork(final NetMessagesProcessor nmp, final int _port)
	{
		netMsgProcessor = nmp;
		listenOnPort = _port;
	}

	/** reference on the messages processor */
	private
	final NetMessagesProcessor netMsgProcessor;

	/** the port to listen at */
	private
	final int listenOnPort;

	//--------------------------------------------

	/** listens on the network and dispatches the commands */
	public void run()
	{
		//start receiver in an infinite loop
		System.out.println("Network listener: Started on port "+listenOnPort+".");

		//init the communication side
		final ZMQ.Context zmqContext = ZMQ.context(1);
		ZMQ.Socket socket = null;
		try {
			socket = zmqContext.socket(SocketType.PAIR);
			if (socket == null)
				throw new Exception("Network listener: Cannot obtain local socket.");

			//port to listen for incoming data
			//socket.subscribe(new byte[] {});
			socket.bind("tcp://*:"+listenOnPort);

			//the incoming data buffer
			String msg;

			while (true)
			{
				msg = socket.recvStr(ZMQ.NOBLOCK);
				if (msg != null)
					netMsgProcessor.processMsg(msg);
				else
					Thread.sleep(1000);
			}
		}
		catch (ZMQException e) {
			System.out.println("Network listener crashed with ZeroMQ error: " + e.getMessage());
		}
		catch (InterruptedException e) {
			System.out.println("Network listener interrupted: "+e.getMessage());
		}
		catch (Exception e) {
			System.out.println("Network listener stopped, error: " + e.getMessage());
			e.printStackTrace();
		}
		finally {
			if (socket != null)
			{
				socket.unbind("tcp://*:8765");
				socket.close();
			}
			//zmqContext.close();
			//zmqContext.term();

			System.out.println("Network listener: Stopped.");
		}
	}
}
