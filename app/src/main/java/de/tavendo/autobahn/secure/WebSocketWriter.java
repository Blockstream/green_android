/******************************************************************************
 *
 *  Copyright 2011-2012 Tavendo GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 ******************************************************************************/

package de.tavendo.autobahn.secure;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Random;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Base64;
import android.util.Log;

/**
 * WebSocket writer, the sending leg of a WebSockets connection.
 * This is run on it's background thread with it's own message loop.
 * The only method that needs to be called (from foreground thread) is forward(),
 * which is used to forward a WebSockets message to this object (running on
 * background thread) so that it can be formatted and sent out on the
 * underlying TCP socket.
 */
public class WebSocketWriter extends Thread {
	private static final String TAG = WebSocketWriter.class.getCanonicalName();
	
	private static final int WEB_SOCKETS_VERSION = 13;
	private static final String CRLF = "\r\n";

	private final Random mRandom = new Random();
	private final Handler mWebSocketConnectionHandler;
	private final WebSocketOptions mWebSocketOptions;
	private final ByteBuffer mApplicationBuffer;
	private final Socket mSocket;

	private OutputStream mOutputStream;

	private Handler mHandler;


	/**
	 * Create new WebSockets background writer.
	 *
	 * @param looper    The message looper of the background thread on which
	 *                  this object is running.
	 * @param master    The message handler of master (foreground thread).
	 * @param socket    The socket channel created on foreground thread.
	 * @param options   WebSockets connection options.
	 */
	public WebSocketWriter(Handler master, Socket socket, WebSocketOptions options, String threadName) {
		super(threadName);

		this.mWebSocketConnectionHandler = master;
		this.mWebSocketOptions = options;
		this.mSocket = socket;
		
		this.mApplicationBuffer = ByteBuffer.allocate(options.getMaxFramePayloadSize() + 14);

		Log.d(TAG, "WebSocket writer created.");
	}


	/**
	 * Call this from the foreground (UI) thread to make the writer
	 * (running on background thread) send a WebSocket message on the
	 * underlying TCP.
	 *
	 * @param message       Message to send to WebSockets writer. An instance of the message
	 *                      classes inside WebSocketMessage or another type which then needs
	 *                      to be handled within processAppMessage() (in a class derived from
	 *                      this class).
	 */
	public void forward(Object message) {
		Message msg = mHandler.obtainMessage();
		msg.obj = message;
		mHandler.sendMessage(msg);
	}


	/**
	 * Notify the master (foreground thread).
	 *
	 * @param message       Message to send to master.
	 */
	private void notify(Object message) {
		Message msg = mWebSocketConnectionHandler.obtainMessage();
		msg.obj = message;
		mWebSocketConnectionHandler.sendMessage(msg);
	}


	/**
	 * Create new key for WebSockets handshake.
	 *
	 * @return WebSockets handshake key (Base64 encoded).
	 */
	private String newHandshakeKey() {
		final byte[] ba = new byte[16];
		mRandom.nextBytes(ba);
		return Base64.encodeToString(ba, Base64.NO_WRAP);
	}


	/**
	 * Create new (random) frame mask.
	 *
	 * @return Frame mask (4 octets).
	 */
	private byte[] newFrameMask() {
		final byte[] ba = new byte[4];
		mRandom.nextBytes(ba);
		return ba;
	}


	/**
	 * Send WebSocket client handshake.
	 */
	private void sendClientHandshake(WebSocketMessage.ClientHandshake message) throws IOException {
		String path = message.getURI().getPath();
		if (path == null || path.length() == 0) {
			path = "/";
		}

		mApplicationBuffer.put(("GET " + path + " HTTP/1.1" + CRLF).getBytes());
		String portPart = "";
		if (message.getURI().getPort() != -1) {
			portPart = ":" + String.valueOf(message.getURI().getPort());
		}
		mApplicationBuffer.put(("Host: " + message.getURI().getHost() + portPart + CRLF).getBytes());
		mApplicationBuffer.put(("Upgrade: WebSocket" + CRLF).getBytes());
		mApplicationBuffer.put(("Connection: Upgrade" + CRLF).getBytes());
		mApplicationBuffer.put(("Sec-WebSocket-Key: " + newHandshakeKey() + CRLF).getBytes());

		if (message.getOrigin() != null) {
			mApplicationBuffer.put(("Origin: " + message.getOrigin().toString() + CRLF).getBytes());
		}

		if (message.getSubprotocols() != null && message.getSubprotocols().length > 0) {
			mApplicationBuffer.put(("Sec-WebSocket-Protocol: ").getBytes());
			for (int i = 0; i < message.getSubprotocols().length; ++i) {
				mApplicationBuffer.put((message.getSubprotocols()[i]).getBytes());
				mApplicationBuffer.put((", ").getBytes());
			}
			mApplicationBuffer.put((CRLF).getBytes());
		}

		mApplicationBuffer.put(("Sec-WebSocket-Version: " + WEB_SOCKETS_VERSION + CRLF).getBytes());
		mApplicationBuffer.put((CRLF).getBytes());
	}


	/**
	 * Send WebSockets close.
	 */
	private void sendClose(WebSocketMessage.Close message) throws IOException, WebSocketException {
		if (message.getCode() > 0) {
			byte[] payload = null;

			if (message.getReason() != null && !(message.getReason().length() > 0)) {
				byte[] pReason = message.getReason().getBytes(WebSocket.UTF8_ENCODING);
				payload = new byte[2 + pReason.length];
				for (int i = 0; i < pReason.length; ++i) {
					payload[i + 2] = pReason[i];
				}
			} else {
				payload = new byte[2];
			}

			if (payload != null && payload.length > 125) {
				throw new WebSocketException("close payload exceeds 125 octets");
			}

			payload[0] = (byte)((message.getCode() >> 8) & 0xff);
			payload[1] = (byte)(message.getCode() & 0xff);

			sendFrame(8, true, payload);
		} else {
			sendFrame(8, true, null);
		}
	}


	/**
	 * Send WebSockets ping.
	 */
	private void sendPing(WebSocketMessage.Ping message) throws IOException, WebSocketException {
		if (message.mPayload != null && message.mPayload.length > 125) {
			throw new WebSocketException("ping payload exceeds 125 octets");
		}
		sendFrame(9, true, message.mPayload);
	}


	/**
	 * Send WebSockets pong. Normally, unsolicited Pongs are not used,
	 * but Pongs are only send in response to a Ping from the peer.
	 */
	private void sendPong(WebSocketMessage.Pong message) throws IOException, WebSocketException {
		if (message.mPayload != null && message.mPayload.length > 125) {
			throw new WebSocketException("pong payload exceeds 125 octets");
		}
		sendFrame(10, true, message.mPayload);
	}


	/**
	 * Send WebSockets binary message.
	 */
	private void sendBinaryMessage(WebSocketMessage.BinaryMessage message) throws IOException, WebSocketException {
		if (message.mPayload.length > mWebSocketOptions.getMaxMessagePayloadSize()) {
			throw new WebSocketException("message payload exceeds payload limit");
		}
		sendFrame(2, true, message.mPayload);
	}


	/**
	 * Send WebSockets text message.
	 */
	private void sendTextMessage(WebSocketMessage.TextMessage message) throws IOException, WebSocketException {
		byte[] payload = message.mPayload.getBytes(WebSocket.UTF8_ENCODING);
		if (payload.length > mWebSocketOptions.getMaxMessagePayloadSize()) {
			throw new WebSocketException("message payload exceeds payload limit");
		}
		sendFrame(1, true, payload);
	}


	/**
	 * Send WebSockets binary message.
	 */
	private void sendRawTextMessage(WebSocketMessage.RawTextMessage message) throws IOException, WebSocketException {
		if (message.mPayload.length > mWebSocketOptions.getMaxMessagePayloadSize()) {
			throw new WebSocketException("message payload exceeds payload limit");
		}
		sendFrame(1, true, message.mPayload);
	}


	/**
	 * Sends a WebSockets frame. Only need to use this method in derived classes which implement
	 * more message types in processAppMessage(). You need to know what you are doing!
	 *
	 * @param opcode     The WebSocket frame opcode.
	 * @param fin        FIN flag for WebSocket frame.
	 * @param payload    Frame payload or null.
	 */
	protected void sendFrame(int opcode, boolean fin, byte[] payload) throws IOException {
		if (payload != null) {
			sendFrame(opcode, fin, payload, 0, payload.length);
		} else {
			sendFrame(opcode, fin, null, 0, 0);
		}
	}


	/**
	 * Sends a WebSockets frame. Only need to use this method in derived classes which implement
	 * more message types in processAppMessage(). You need to know what you are doing!
	 *
	 * @param opcode     The WebSocket frame opcode.
	 * @param fin        FIN flag for WebSocket frame.
	 * @param payload    Frame payload or null.
	 * @param offset     Offset within payload of the chunk to send.
	 * @param length     Length of the chunk within payload to send.
	 */
	protected void sendFrame(int opcode, boolean fin, byte[] payload, int offset, int length) throws IOException {
		// first octet
		byte b0 = 0;
		if (fin) {
			b0 |= (byte) (1 << 7);
		}
		b0 |= (byte) opcode;
		mApplicationBuffer.put(b0);

		// second octet
		byte b1 = 0;
		if (mWebSocketOptions.getMaskClientFrames()) {
			b1 = (byte) (1 << 7);
		}

		long len = length;

		// extended payload length
		if (len <= 125) {
			b1 |= (byte) len;
			mApplicationBuffer.put(b1);
		} else if (len <= 0xffff) {
			b1 |= (byte) (126 & 0xff);
			mApplicationBuffer.put(b1);
			mApplicationBuffer.put(new byte[] {(byte)((len >> 8) & 0xff), (byte)(len & 0xff)});
		} else {
			b1 |= (byte) (127 & 0xff);
			mApplicationBuffer.put(b1);
			mApplicationBuffer.put(new byte[] {(byte)((len >> 56) & 0xff),
					(byte)((len >> 48) & 0xff),
					(byte)((len >> 40) & 0xff),
					(byte)((len >> 32) & 0xff),
					(byte)((len >> 24) & 0xff),
					(byte)((len >> 16) & 0xff),
					(byte)((len >> 8)  & 0xff),
					(byte)(len         & 0xff)});
		}

		byte mask[] = null;
		if (mWebSocketOptions.getMaskClientFrames()) {
			// a mask is always needed, even without payload
			mask = newFrameMask();
			mApplicationBuffer.put(mask[0]);
			mApplicationBuffer.put(mask[1]);
			mApplicationBuffer.put(mask[2]);
			mApplicationBuffer.put(mask[3]);
		}

		if (len > 0) {
			if (mWebSocketOptions.getMaskClientFrames()) {
				/// \todo optimize masking
				/// \todo masking within buffer of output stream
				for (int i = 0; i < len; ++i) {
					payload[i + offset] ^= mask[i % 4];
				}
			}
			mApplicationBuffer.put(payload, offset, length);
		}
	}

	/**
	 * Process WebSockets or control message from master. Normally,
	 * there should be no reason to override this. If you do, you
	 * need to know what you are doing.
	 *
	 * @param msg     An instance of the message types within WebSocketMessage
	 *                or a message that is handled in processAppMessage().
	 */
	protected void processMessage(Object msg) throws IOException, WebSocketException {

		if (msg instanceof WebSocketMessage.TextMessage) {
			sendTextMessage((WebSocketMessage.TextMessage) msg);
		} else if (msg instanceof WebSocketMessage.RawTextMessage) {
			sendRawTextMessage((WebSocketMessage.RawTextMessage) msg);
		} else if (msg instanceof WebSocketMessage.BinaryMessage) {
			sendBinaryMessage((WebSocketMessage.BinaryMessage) msg);
		} else if (msg instanceof WebSocketMessage.Ping) {
			sendPing((WebSocketMessage.Ping) msg);
		} else if (msg instanceof WebSocketMessage.Pong) {
			sendPong((WebSocketMessage.Pong) msg);
		} else if (msg instanceof WebSocketMessage.Close) {
			sendClose((WebSocketMessage.Close) msg);
		} else if (msg instanceof WebSocketMessage.ClientHandshake) {
			sendClientHandshake((WebSocketMessage.ClientHandshake) msg);
		} else if (msg instanceof WebSocketMessage.Quit) {
			Looper.myLooper().quit();

			Log.d(TAG, "WebSocket writer ended.");
		} else {
			processAppMessage(msg);
		}
	}

	public void writeMessageToBuffer(Message message) {
		try {
			mApplicationBuffer.clear();
			processMessage(message.obj);
			mApplicationBuffer.flip();

			mOutputStream.write(mApplicationBuffer.array(), mApplicationBuffer.position(), mApplicationBuffer.limit());
		} catch (SocketException e) {
			Log.e(TAG, "run() : SocketException (" + e.toString() + ")");

			notify(new WebSocketMessage.ConnectionLost());
		} catch (IOException e) {
			Log.e(TAG, "run() : IOException (" + e.toString() + ")");

		} catch (Exception e) {
			notify(new WebSocketMessage.Error(e));
		}
	}

	/**
	 * Process message other than plain WebSockets or control message.
	 * This is intended to be overridden in derived classes.
	 *
	 * @param msg      Message from foreground thread to process.
	 */
	protected void processAppMessage(Object msg) throws WebSocketException, IOException {
		throw new WebSocketException("unknown message received by WebSocketWriter");
	}



	// Thread method overrides
	@Override
	public void run() {	
		OutputStream outputStream = null;
		try {
			outputStream = mSocket.getOutputStream();
		} catch (IOException e) {
			Log.e(TAG, e.getLocalizedMessage());
		}
		
		this.mOutputStream = outputStream;
		
		Looper.prepare();

		this.mHandler = new ThreadHandler(this);

		synchronized (this) {
			Log.d(TAG, "WebSocker writer running.");

			notifyAll();
		}

		Looper.loop();
	}



	//
	// Private handler class
	private static class ThreadHandler extends Handler {
		private final WeakReference<WebSocketWriter> mWebSocketWriterReference;



		public ThreadHandler(WebSocketWriter webSocketWriter) {
			super();

			this.mWebSocketWriterReference = new WeakReference<WebSocketWriter>(webSocketWriter);
		}



		@Override
		public void handleMessage(Message message) {
			WebSocketWriter webSocketWriter = mWebSocketWriterReference.get();
			if (webSocketWriter != null) {
				webSocketWriter.writeMessageToBuffer(message);
			}
		}
	}
}
