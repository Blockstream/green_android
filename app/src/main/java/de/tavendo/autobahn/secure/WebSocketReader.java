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
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.Pair;
import de.tavendo.autobahn.secure.WebSocketMessage.WebSocketCloseCode;

/**
 * WebSocket reader, the receiving leg of a WebSockets connection.
 * This runs on it's own background thread and posts messages to master
 * thread's message queue for there to be consumed by the application.
 * The only method that needs to be called (from foreground thread) is quit(),
 * which gracefully shuts down the background receiver thread.
 */
public class WebSocketReader extends Thread {
	private static final String TAG = WebSocketReader.class.getCanonicalName();

	private static enum ReaderState { 
		STATE_CLOSED,
		STATE_CONNECTING,
		STATE_CLOSING,
		STATE_OPEN
	}

	private final Handler mWebSocketConnectionHandler;
	private final Socket mSocket;
	private InputStream mInputStream;
	private final WebSocketOptions mWebSocketOptions;

	private volatile boolean mStopped = false;


	private final byte[] mNetworkBuffer;
	private final ByteBuffer mApplicationBuffer;
	private NoCopyByteArrayOutputStream mMessagePayload;

	private ReaderState mState;

	private boolean mInsideMessage = false;
	private int mMessageOpcode;

	private WebSocketFrameHeader mFrameHeader;
	private Utf8Validator mUTF8Validator = new Utf8Validator();




	/**
	 * Create new WebSockets background reader.
	 *
	 * @param master    The message handler of master (foreground thread).
	 * @param socket    The socket channel created on foreground thread.
	 */
	public WebSocketReader(Handler master, Socket socket, WebSocketOptions options, String threadName) {
		super(threadName);

		this.mWebSocketConnectionHandler = master;

		this.mSocket = socket;
		this.mWebSocketOptions = options;

		this.mNetworkBuffer = new byte[4096];
		this.mApplicationBuffer = ByteBuffer.allocateDirect(options.getMaxFramePayloadSize() + 14);
		this.mMessagePayload = new NoCopyByteArrayOutputStream(options.getMaxMessagePayloadSize());

		this.mFrameHeader = null;
		this.mState = ReaderState.STATE_CONNECTING;

		Log.d(TAG, "WebSocket reader created.");
	}


	/**
	 * Graceful shutdown of background reader thread (called from master).
	 */
	public void quit() {

		mStopped = true;

		Log.d(TAG, "quit");
	}


	/**
	 * Notify the master (foreground thread) of WebSockets message received
	 * and unwrapped.
	 *
	 * @param message       Message to send to master.
	 */
	protected void notify(Object message) {

		Message msg = mWebSocketConnectionHandler.obtainMessage();
		msg.obj = message;
		mWebSocketConnectionHandler.sendMessage(msg);
	}


	/**
	 * Process incoming WebSockets data (after handshake).
	 */
	private boolean processData() throws Exception {

		// outside frame?
		if (mFrameHeader == null) {

			// need at least 2 bytes from WS frame header to start processing
			if (mApplicationBuffer.position() >= 2) {

				byte b0 = mApplicationBuffer.get(0);
				boolean fin = (b0 & 0x80) != 0;
				int rsv = (b0 & 0x70) >> 4;
				int opcode = b0 & 0x0f;

				byte b1 = mApplicationBuffer.get(1);
				boolean masked = (b1 & 0x80) != 0;
				int payload_len1 = b1 & 0x7f;

				// now check protocol compliance

				if (rsv != 0) {
					throw new WebSocketException("RSV != 0 and no extension negotiated");
				}

				if (masked) {
					// currently, we don't allow this. need to see whats the final spec.
					throw new WebSocketException("masked server frame");
				}

				if (opcode > 7) {
					// control frame
					if (!fin) {
						throw new WebSocketException("fragmented control frame");
					}
					if (payload_len1 > 125) {
						throw new WebSocketException("control frame with payload length > 125 octets");
					}
					if (opcode != 8 && opcode != 9 && opcode != 10) {
						throw new WebSocketException("control frame using reserved opcode " + opcode);
					}
					if (opcode == 8 && payload_len1 == 1) {
						throw new WebSocketException("received close control frame with payload len 1");
					}
				} else {
					// message frame
					if (opcode != 0 && opcode != 1 && opcode != 2) {
						throw new WebSocketException("data frame using reserved opcode " + opcode);
					}
					if (!mInsideMessage && opcode == 0) {
						throw new WebSocketException("received continuation data frame outside fragmented message");
					}
					if (mInsideMessage && opcode != 0) {
						throw new WebSocketException("received non-continuation data frame while inside fragmented message");
					}
				}

				int mask_len = masked ? 4 : 0;
				int header_len = 0;

				if (payload_len1 < 126) {
					header_len = 2 + mask_len;
				} else if (payload_len1 == 126) {
					header_len = 2 + 2 + mask_len;
				} else if (payload_len1 == 127) {
					header_len = 2 + 8 + mask_len;
				} else {
					// should not arrive here
					throw new Exception("logic error");
				}

				// continue when complete frame header is available
				if (mApplicationBuffer.position() >= header_len) {

					// determine frame payload length
					int i = 2;
					long payload_len = 0;
					if (payload_len1 == 126) {
						payload_len = ((0xff & mApplicationBuffer.get(i)) << 8) | (0xff & mApplicationBuffer.get(i+1));
						if (payload_len < 126) {
							throw new WebSocketException("invalid data frame length (not using minimal length encoding)");
						}
						i += 2;
					} else if (payload_len1 == 127) {
						if ((0x80 & mApplicationBuffer.get(i+0)) != 0) {
							throw new WebSocketException("invalid data frame length (> 2^63)");
						}
						payload_len = ((0xff & mApplicationBuffer.get(i+0)) << 56) |
								((0xff & mApplicationBuffer.get(i+1)) << 48) |
								((0xff & mApplicationBuffer.get(i+2)) << 40) |
								((0xff & mApplicationBuffer.get(i+3)) << 32) |
								((0xff & mApplicationBuffer.get(i+4)) << 24) |
								((0xff & mApplicationBuffer.get(i+5)) << 16) |
								((0xff & mApplicationBuffer.get(i+6)) <<  8) |
								((0xff & mApplicationBuffer.get(i+7))      );
						if (payload_len < 65536) {
							throw new WebSocketException("invalid data frame length (not using minimal length encoding)");
						}
						i += 8;
					} else {
						payload_len = payload_len1;
					}

					// immediately bail out on frame too large
					if (payload_len > mWebSocketOptions.getMaxFramePayloadSize()) {
						throw new WebSocketException("frame payload too large");
					}

					// save frame header metadata
					mFrameHeader = new WebSocketFrameHeader();
					mFrameHeader.setOpcode(opcode);
					mFrameHeader.setFin(fin);
					mFrameHeader.setReserved(rsv);
					mFrameHeader.setPayloadLength((int) payload_len);
					mFrameHeader.setHeaderLength(header_len);
					mFrameHeader.setTotalLen(mFrameHeader.getHeaderLength() + mFrameHeader.getPayloadLength());

					if (masked) {
						byte[] mask = new byte[4];
						for (int j = 0; j < 4; ++j) {
							mask[i] = (byte) (0xff & mApplicationBuffer.get(i + j));
						}
						mFrameHeader.setMask(mask);

						i += 4;						
					} else {
						mFrameHeader.setMask(null);
					}

					// continue processing when payload empty or completely buffered
					return mFrameHeader.getPayloadLength() == 0 || mApplicationBuffer.position() >= mFrameHeader.getTotalLength();

				} else {

					// need more data
					return false;
				}
			} else {

				// need more data
				return false;
			}

		} else {

			/// \todo refactor this for streaming processing, incl. fail fast on invalid UTF-8 within frame already

			// within frame

			// see if we buffered complete frame
			if (mApplicationBuffer.position() >= mFrameHeader.getTotalLength()) {

				// cut out frame payload
				byte[] framePayload = null;
				int oldPosition = mApplicationBuffer.position();
				if (mFrameHeader.getPayloadLength() > 0) {
					framePayload = new byte[mFrameHeader.getPayloadLength()];
					mApplicationBuffer.position(mFrameHeader.getHeaderLength());
					mApplicationBuffer.get(framePayload, 0, (int) mFrameHeader.getPayloadLength());
				}
				mApplicationBuffer.position(mFrameHeader.getTotalLength());
				mApplicationBuffer.limit(oldPosition);
				mApplicationBuffer.compact();

				if (mFrameHeader.getOpcode() > 7) {
					// control frame

					if (mFrameHeader.getOpcode() == 8) {

						int code = WebSocketCloseCode.RESERVED_NO_STATUS;
						String reason = null;

						if (mFrameHeader.getPayloadLength() >= 2) {

							// parse and check close code
							code = (framePayload[0] & 0xff) * 256 + (framePayload[1] & 0xff);
							if (code < 1000
									|| (code >= 1000 && code <= 2999 &&
									code != 1000 && code != 1001 && code != 1002 && code != 1003 && code != 1007 && code != 1008 && code != 1009 && code != 1010 && code != 1011)
									|| code >= 5000) {

								throw new WebSocketException("invalid close code " + code);
							}

							// parse and check close reason
							if (mFrameHeader.getPayloadLength() > 2) {

								byte[] ra = new byte[mFrameHeader.getPayloadLength() - 2];
								System.arraycopy(framePayload, 2, ra, 0, mFrameHeader.getPayloadLength() - 2);

								Utf8Validator val = new Utf8Validator();
								val.validate(ra);
								if (!val.isValid()) {
									throw new WebSocketException("invalid close reasons (not UTF-8)");
								} else {
									reason = new String(ra, WebSocket.UTF8_ENCODING);
								}
							}
						}
						onClose(code, reason);

					} else if (mFrameHeader.getOpcode() == 9) {
						// dispatch WS ping
						onPing(framePayload);

					} else if (mFrameHeader.getOpcode() == 10) {
						// dispatch WS pong
						onPong(framePayload);

					} else {

						// should not arrive here (handled before)
						throw new Exception("logic error");
					}

				} else {
					// message frame

					if (!mInsideMessage) {
						// new message started
						mInsideMessage = true;
						mMessageOpcode = mFrameHeader.getOpcode();
						if (mMessageOpcode == 1 && mWebSocketOptions.getValidateIncomingUtf8()) {
							mUTF8Validator.reset();
						}
					}

					if (framePayload != null) {

						// immediately bail out on message too large
						if (mMessagePayload.size() + framePayload.length > mWebSocketOptions.getMaxMessagePayloadSize()) {
							throw new WebSocketException("message payload too large");
						}

						// validate incoming UTF-8
						if (mMessageOpcode == 1 && mWebSocketOptions.getValidateIncomingUtf8() && !mUTF8Validator.validate(framePayload)) {
							throw new WebSocketException("invalid UTF-8 in text message payload");
						}

						// buffer frame payload for message
						mMessagePayload.write(framePayload);
					}

					// on final frame ..
					if (mFrameHeader.isFin()) {

						if (mMessageOpcode == 1) {

							// verify that UTF-8 ends on codepoint
							if (mWebSocketOptions.getValidateIncomingUtf8() && !mUTF8Validator.isValid()) {
								throw new WebSocketException("UTF-8 text message payload ended within Unicode code point");
							}

							// deliver text message
							if (mWebSocketOptions.getReceiveTextMessagesRaw()) {

								// dispatch WS text message as raw (but validated) UTF-8
								onRawTextMessage(mMessagePayload.toByteArray());

							} else {

								// dispatch WS text message as Java String (previously already validated)
								String s = new String(mMessagePayload.toByteArray(), WebSocket.UTF8_ENCODING);
								onTextMessage(s);
							}

						} else if (mMessageOpcode == 2) {

							// dispatch WS binary message
							onBinaryMessage(mMessagePayload.toByteArray());

						} else {

							// should not arrive here (handled before)
							throw new Exception("logic error");
						}

						// ok, message completed - reset all
						mInsideMessage = false;
						mMessagePayload.reset();
					}
				}

				// reset frame
				mFrameHeader = null;

				// reprocess if more data left
				return mApplicationBuffer.position() > 0;

			} else {

				// need more data
				return false;
			}
		}
	}


	/**
	 * WebSockets handshake reply from server received, default notifies master.
	 * 
	 * @param success	Success handshake flag
	 */
	protected void onHandshake(boolean success) {

		notify(new WebSocketMessage.ServerHandshake(success));
	}


	/**
	 * WebSockets close received, default notifies master.
	 */
	protected void onClose(int code, String reason) {

		notify(new WebSocketMessage.Close(code, reason));
	}


	/**
	 * WebSockets ping received, default notifies master.
	 *
	 * @param payload    Ping payload or null.
	 */
	protected void onPing(byte[] payload) {

		notify(new WebSocketMessage.Ping(payload));
	}


	/**
	 * WebSockets pong received, default notifies master.
	 *
	 * @param payload    Pong payload or null.
	 */
	protected void onPong(byte[] payload) {

		notify(new WebSocketMessage.Pong(payload));
	}


	/**
	 * WebSockets text message received, default notifies master.
	 * This will only be called when the option receiveTextMessagesRaw
	 * HAS NOT been set.
	 *
	 * @param payload    Text message payload as Java String decoded
	 *                   from raw UTF-8 payload or null (empty payload).
	 */
	protected void onTextMessage(String payload) {

		notify(new WebSocketMessage.TextMessage(payload));
	}


	/**
	 * WebSockets text message received, default notifies master.
	 * This will only be called when the option receiveTextMessagesRaw
	 * HAS been set.
	 *
	 * @param payload    Text message payload as raw UTF-8 octets or
	 *                   null (empty payload).
	 */
	protected void onRawTextMessage(byte[] payload) {

		notify(new WebSocketMessage.RawTextMessage(payload));
	}


	/**
	 * WebSockets binary message received, default notifies master.
	 *
	 * @param payload    Binary message payload or null (empty payload).
	 */
	protected void onBinaryMessage(byte[] payload) {

		notify(new WebSocketMessage.BinaryMessage(payload));
	}


	/**
	 * Process WebSockets handshake received from server.
	 */
	private boolean processHandshake() throws UnsupportedEncodingException {

		boolean res = false;
		for (int pos = mApplicationBuffer.position() - 4; pos >= 0; --pos) {
			if (mApplicationBuffer.get(pos+0) == 0x0d &&
					mApplicationBuffer.get(pos+1) == 0x0a &&
					mApplicationBuffer.get(pos+2) == 0x0d &&
					mApplicationBuffer.get(pos+3) == 0x0a) {

				/// \todo process & verify handshake from server
				/// \todo forward subprotocol, if any

				int oldPosition = mApplicationBuffer.position();

				// Check HTTP status code
				boolean serverError = false;
				if (mApplicationBuffer.get(0) == 'H' &&
						mApplicationBuffer.get(1) == 'T' &&
						mApplicationBuffer.get(2) == 'T' &&
						mApplicationBuffer.get(3) == 'P') {

					Pair<Integer, String> status = parseHTTPStatus();
					if (status.first >= 300) {
						// Invalid status code for success connection
						notify(new WebSocketMessage.ServerError(status.first, status.second));
						serverError = true;
					}
				}

				mApplicationBuffer.position(pos + 4);
				mApplicationBuffer.limit(oldPosition);
				mApplicationBuffer.compact();

				if (!serverError) {
					// process further when data after HTTP headers left in buffer
					res = mApplicationBuffer.position() > 0;

					mState = ReaderState.STATE_OPEN;
				} else {
					res = true;
					mState = ReaderState.STATE_CLOSED;
					mStopped = true;
				}

				onHandshake(!serverError);
				break;
			}
		}
		return res;
	}

	private Pair<Integer, String> parseHTTPStatus() throws UnsupportedEncodingException {
		int beg, end;
		// Find first space
		for (beg = 4; beg < mApplicationBuffer.position(); ++beg) {
			if (mApplicationBuffer.get(beg) == ' ') break;
		}
		// Find second space
		for (end = beg + 1; end < mApplicationBuffer.position(); ++end) {
			if (mApplicationBuffer.get(end) == ' ') break;
		}
		// Parse status code between them
		++beg;
		int statusCode = 0;
		for (int i = 0; beg + i < end; ++i) {
			int digit = (mApplicationBuffer.get(beg + i) - 0x30);
			statusCode *= 10;
			statusCode += digit;
		}
		// Find end of line to extract error message
		++end;
		int eol;
		for (eol = end; eol < mApplicationBuffer.position(); ++eol) {
			if (mApplicationBuffer.get(eol) == 0x0d) break;
		}
		int statusMessageLength = eol - end;
		byte[] statusBuf = new byte[statusMessageLength];
		mApplicationBuffer.position(end);
		mApplicationBuffer.get(statusBuf, 0, statusMessageLength);
		String statusMessage = new String(statusBuf, WebSocket.UTF8_ENCODING);
		Log.w(TAG, String.format("Status: %d (%s)", statusCode, statusMessage));
		return new Pair<Integer, String>(statusCode, statusMessage);
	}


	/**
	 * Consume data buffered in mFrameBuffer.
	 */
	private boolean consumeData() throws Exception {
		switch (mState) {
		case STATE_OPEN:
		case STATE_CLOSING:
			return processData();
		case STATE_CLOSED:
			return false;
		case STATE_CONNECTING:
			return processHandshake();
		default:
			return false;
		}
	}


	/**
	 * Run the background reader thread loop.
	 */
	@Override
	public void run() {		
		synchronized (this) {
			notifyAll();
		}
		
		InputStream inputStream = null;
		try {
			inputStream = mSocket.getInputStream();
		} catch (IOException e) {
			Log.e(TAG, e.getLocalizedMessage());
			return;
		}

		this.mInputStream = inputStream;

		Log.d(TAG, "WebSocker reader running.");
		mApplicationBuffer.clear();

		while (!mStopped) {
			try {

				int bytesRead = mInputStream.read(mNetworkBuffer);
				if (bytesRead > 0) {
					mApplicationBuffer.put(mNetworkBuffer, 0, bytesRead);
					while (consumeData()) {
					}
				} else if (bytesRead == -1) {
					Log.d(TAG, "run() : ConnectionLost");

					notify(new WebSocketMessage.ConnectionLost());
					this.mStopped = true;
				} else {
					Log.e(TAG, "WebSocketReader read() failed.");
				}
				
			} catch (WebSocketException e) {
				Log.d(TAG, "run() : WebSocketException (" + e.toString() + ")");

				// wrap the exception and notify master
				notify(new WebSocketMessage.ProtocolViolation(e));
			} catch (SocketException e) {
				Log.d(TAG, "run() : SocketException (" + e.toString() + ")");

				// wrap the exception and notify master
				notify(new WebSocketMessage.ConnectionLost());
			} catch (IOException e) {
				Log.d(TAG, "run() : IOException (" + e.toString() + ")");
				
				notify(new WebSocketMessage.ConnectionLost());
			} catch (Exception e) {
				Log.d(TAG, "run() : Exception (" + e.toString() + ")");

				// wrap the exception and notify master
				notify(new WebSocketMessage.Error(e));
			}
		}


		Log.d(TAG, "WebSocket reader ended.");
	}
}
