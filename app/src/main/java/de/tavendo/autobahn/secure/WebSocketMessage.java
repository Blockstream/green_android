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

import java.net.URI;

/**
 * WebSockets message classes.
 * The master thread and the background reader/writer threads communicate using these messages
 * for WebSockets connections.
 */
public class WebSocketMessage {
	public static class WebSocketCloseCode {
		public static final int NORMAL = 1000;
		public static final int ENDPOINT_GOING_AWAY = 1001;		 
		public static final int ENDPOINT_PROTOCOL_ERROR = 1002;
		public static final int ENDPOINT_UNSUPPORTED_DATA_TYPE = 1003;
		public static final int RESERVED = 1004;
		public static final int RESERVED_NO_STATUS = 1005;
		public static final int RESERVED_NO_CLOSING_HANDSHAKE = 1006;
		public static final int ENDPOINT_BAD_DATA = 1007;
		public static final int POLICY_VIOLATION = 1008;
		public static final int MESSAGE_TOO_BIG = 1009;
		public static final int ENDPOINT_NEEDS_EXTENSION = 1010;
		public static final int UNEXPECTED_CONDITION = 1011;
		public static final int RESERVED_TLS_REQUIRED = 1015;
	}


	/// Base message class.
	public static class Message {
	}

	/// Quite background thread.
	public static class Quit extends Message {
	}

	/// Initial WebSockets handshake (client request).
	public static class ClientHandshake extends Message {
		private final URI mURI;
		private final URI mOrigin;
		private final String[] mSubprotocols;



		ClientHandshake(URI uri) {
			this.mURI = uri;
			this.mOrigin = null;
			this.mSubprotocols = null;
		}

		ClientHandshake(URI uri, URI origin, String[] subprotocols) {
			this.mURI = uri;
			this.mOrigin = origin;
			this.mSubprotocols = subprotocols;
		}



		public URI getURI() {
			return mURI;
		}
		public URI getOrigin() {
			return mOrigin;
		}
		public String[] getSubprotocols() {
			return mSubprotocols;
		}
	}

	/// Initial WebSockets handshake (server response).
	public static class ServerHandshake extends Message {
		public boolean mSuccess;

		public ServerHandshake(boolean success) {
			mSuccess = success;
		}
	}

	/// WebSockets connection lost
	public static class ConnectionLost extends Message {
	}

	public static class ServerError extends Message {
		public int mStatusCode;
		public String mStatusMessage;

		public ServerError(int statusCode, String statusMessage) {
			mStatusCode = statusCode;
			mStatusMessage = statusMessage;
		}

	}

	/// WebSockets reader detected WS protocol violation.
	public static class ProtocolViolation extends Message {

		public WebSocketException mException;

		public ProtocolViolation(WebSocketException e) {
			mException = e;
		}
	}

	/// An exception occured in the WS reader or WS writer.
	public static class Error extends Message {

		public Exception mException;

		public Error(Exception e) {
			mException = e;
		}
	}

	/// WebSockets text message to send or received.
	public static class TextMessage extends Message {

		public String mPayload;

		TextMessage(String payload) {
			mPayload = payload;
		}
	}

	/// WebSockets raw (UTF-8) text message to send or received.
	public static class RawTextMessage extends Message {

		public byte[] mPayload;

		RawTextMessage(byte[] payload) {
			mPayload = payload;
		}
	}

	/// WebSockets binary message to send or received.
	public static class BinaryMessage extends Message {

		public byte[] mPayload;

		BinaryMessage(byte[] payload) {
			mPayload = payload;
		}
	}

	/// WebSockets close to send or received.
	public static class Close extends Message {
		private int mCode;
		private String mReason;


		Close() {
			mCode = WebSocketCloseCode.UNEXPECTED_CONDITION;
			mReason = null;
		}

		Close(int code) {
			mCode = code;
			mReason = null;
		}

		Close(int code, String reason) {
			mCode = code;
			mReason = reason;
		}


		public int getCode() {
			return mCode;
		}
		public String getReason() {
			return mReason;
		}
	}

	/// WebSockets ping to send or received.
	public static class Ping extends Message {

		public byte[] mPayload;

		Ping() {
			mPayload = null;
		}

		Ping(byte[] payload) {
			mPayload = payload;
		}
	}

	/// WebSockets pong to send or received.
	public static class Pong extends Message {

		public byte[] mPayload;

		Pong() {
			mPayload = null;
		}

		Pong(byte[] payload) {
			mPayload = payload;
		}
	}

}
