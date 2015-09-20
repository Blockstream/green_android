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
import java.lang.ref.WeakReference;
import java.net.Socket;
import java.net.URI;

import javax.net.SocketFactory;

import android.net.SSLCertificateSocketFactory;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import de.tavendo.autobahn.secure.WebSocket.WebSocketConnectionObserver.WebSocketCloseNotification;
import de.tavendo.autobahn.secure.WebSocketMessage.WebSocketCloseCode;

public class WebSocketConnection implements WebSocket {
	private static final String TAG = WebSocketConnection.class.getName();
	private static final String WS_URI_SCHEME = "ws";
	private static final String WSS_URI_SCHEME = "wss";
	private static final String WS_WRITER = "WebSocketWriter";
	private static final String WS_READER = "WebSocketReader";

	protected final Handler mHandler;

	protected WebSocketReader mWebSocketReader;
	protected WebSocketWriter mWebSocketWriter;

	protected Socket mSocket;
	private SocketThread mSocketThread;

	private URI mWebSocketURI;
	private String[] mWebSocketSubprotocols;

	private WebSocket.WebSocketConnectionObserver mWebSocketConnectionObserver;

	protected WebSocketOptions mWebSocketOptions;
	private boolean mPreviousConnection = false;



	public WebSocketConnection() {
		Log.d(TAG, "WebSocket connection created.");

		this.mHandler = new ThreadHandler(this);
	}



	//
	// Forward to the writer thread
	public void sendTextMessage(String payload) {
		mWebSocketWriter.forward(new WebSocketMessage.TextMessage(payload));
	}


	public void sendRawTextMessage(byte[] payload) {
		mWebSocketWriter.forward(new WebSocketMessage.RawTextMessage(payload));
	}


	public void sendBinaryMessage(byte[] payload) {
		mWebSocketWriter.forward(new WebSocketMessage.BinaryMessage(payload));
	}



	public boolean isConnected() {
		return mSocket != null && mSocket.isConnected() && !mSocket.isClosed();
	}



	private void failConnection(WebSocketCloseNotification code, String reason) {
		Log.d(TAG, "fail connection [code = " + code + ", reason = " + reason);

		if (mWebSocketReader != null) {
			mWebSocketReader.quit();

			try {
				mWebSocketReader.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} else {
			Log.d(TAG, "mReader already NULL");
		}

		if (mWebSocketWriter != null) {
			mWebSocketWriter.forward(new WebSocketMessage.Quit());

			try {
				mWebSocketWriter.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} else {
			Log.d(TAG, "mWriter already NULL");
		}

		if (mSocket != null) {
			mSocketThread.getHandler().post(new Runnable() {

				@Override
				public void run() {
					mSocketThread.stopConnection();
				}
			});
		} else {
			Log.d(TAG, "mTransportChannel already NULL");
		}

		mSocketThread.getHandler().post(new Runnable() {

			@Override
			public void run() {
				Looper.myLooper().quit();
			}
		});

		onClose(code, reason);

		Log.d(TAG, "worker threads stopped");
	}



	public void connect(URI webSocketURI, WebSocket.WebSocketConnectionObserver connectionObserver) throws WebSocketException {
		connect(webSocketURI, connectionObserver, new WebSocketOptions());
	}

	public void connect(URI webSocketURI, WebSocket.WebSocketConnectionObserver connectionObserver, WebSocketOptions options) throws WebSocketException {
		connect(webSocketURI, null, connectionObserver, options);
	}

	public void connect(URI webSocketURI, String[] subprotocols, WebSocket.WebSocketConnectionObserver connectionObserver, WebSocketOptions options) throws WebSocketException {
		if (mSocket != null && mSocket.isConnected()) {
			throw new WebSocketException("already connected");
		}

		if (webSocketURI == null) {
			throw new WebSocketException("WebSockets URI null.");
		} else {
			this.mWebSocketURI = webSocketURI;
			if (!mWebSocketURI.getScheme().equals(WS_URI_SCHEME) && !mWebSocketURI.getScheme().equals(WSS_URI_SCHEME)) {
				throw new WebSocketException("unsupported scheme for WebSockets URI");
			}

			this.mWebSocketSubprotocols = subprotocols;
			this.mWebSocketConnectionObserver = connectionObserver;
			this.mWebSocketOptions = new WebSocketOptions(options);

			connect();
		}
	}

	public void disconnect() {
		if (mWebSocketWriter != null && mWebSocketWriter.isAlive()) {
			mWebSocketWriter.forward(new WebSocketMessage.Close());
		} else {
			Log.d(TAG, "Could not send WebSocket Close .. writer already null");
		}

		this.mPreviousConnection = false;
	}

	/**
	 * Reconnect to the server with the latest options
	 * @return true if reconnection performed
	 */
	public boolean reconnect() {
		if (!isConnected() && (mWebSocketURI != null)) {
			connect();
			return true;
		}
		return false;
	}

	private void connect() {
		mSocketThread = new SocketThread(mWebSocketURI, mWebSocketOptions);

		mSocketThread.start();
		synchronized (mSocketThread) {
			try {
				mSocketThread.wait();
			} catch (InterruptedException e) {
			}
		}
		mSocketThread.getHandler().post(new Runnable() {

			@Override
			public void run() {
				mSocketThread.startConnection();
			}
		});

		synchronized (mSocketThread) {
			try {
				mSocketThread.wait();
			} catch (InterruptedException e) {
			}
		}

		this.mSocket = mSocketThread.getSocket();

		if (mSocket == null) {
			onClose(WebSocketCloseNotification.CANNOT_CONNECT, mSocketThread.getFailureMessage());
		} else if (mSocket.isConnected()) {
			try {
				createReader();
				createWriter();

				WebSocketMessage.ClientHandshake clientHandshake = new WebSocketMessage.ClientHandshake(mWebSocketURI, null, mWebSocketSubprotocols);
				mWebSocketWriter.forward(clientHandshake);
			} catch (Exception e) {
				onClose(WebSocketCloseNotification.INTERNAL_ERROR, e.getLocalizedMessage());
			}
		} else {
			onClose(WebSocketCloseNotification.CANNOT_CONNECT, "could not connect to WebSockets server");
		}
	}

	/**
	 * Perform reconnection
	 *
	 * @return true if reconnection was scheduled
	 */
	protected boolean scheduleReconnect() {
		/**
		 * Reconnect only if:
		 *  - connection active (connected but not disconnected)
		 *  - has previous success connections
		 *  - reconnect interval is set
		 */
		int interval = mWebSocketOptions.getReconnectInterval();
		boolean shouldReconnect = mSocket.isConnected() && mPreviousConnection && (interval > 0);
		if (shouldReconnect) {
			Log.d(TAG, "WebSocket reconnection scheduled");
			mHandler.postDelayed(new Runnable() {

				public void run() {
					Log.d(TAG, "WebSocket reconnecting...");
					reconnect();
				}
			}, interval);
		}
		return shouldReconnect;
	}

	/**
	 * Common close handler
	 *
	 * @param code       Close code.
	 * @param reason     Close reason (human-readable).
	 */
	private void onClose(WebSocketCloseNotification code, String reason) {
		boolean reconnecting = false;

		if ((code == WebSocketCloseNotification.CANNOT_CONNECT) || (code == WebSocketCloseNotification.CONNECTION_LOST)) {
			reconnecting = scheduleReconnect();
		}

		WebSocket.WebSocketConnectionObserver webSocketObserver = mWebSocketConnectionObserver;
		if (webSocketObserver != null) {
			try {
				if (reconnecting) {
					webSocketObserver.onClose(WebSocketConnectionObserver.WebSocketCloseNotification.RECONNECT, reason);
				} else {
					webSocketObserver.onClose(code, reason);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			Log.d(TAG, "WebSocketObserver null");
		}
	}




	protected void processAppMessage(Object message) {
	}


	/**
	 * Create WebSockets background writer.
	 */
	protected void createWriter() {
		mWebSocketWriter = new WebSocketWriter(mHandler, mSocket, mWebSocketOptions, WS_WRITER);
		mWebSocketWriter.start();

		synchronized (mWebSocketWriter) {
			try {
				mWebSocketWriter.wait();
			} catch (InterruptedException e) {
			}
		}

		Log.d(TAG, "WebSocket writer created and started.");
	}


	/**
	 * Create WebSockets background reader.
	 */
	protected void createReader() {

		mWebSocketReader = new WebSocketReader(mHandler, mSocket, mWebSocketOptions, WS_READER);
		mWebSocketReader.start();

		synchronized (mWebSocketReader) {
			try {
				mWebSocketReader.wait();
			} catch (InterruptedException e) {
			}
		}

		Log.d(TAG, "WebSocket reader created and started.");
	}

	private void handleMessage(Message message) {
		WebSocket.WebSocketConnectionObserver webSocketObserver = mWebSocketConnectionObserver;

		if (message.obj instanceof WebSocketMessage.TextMessage) {
			WebSocketMessage.TextMessage textMessage = (WebSocketMessage.TextMessage) message.obj;

			if (webSocketObserver != null) {
				webSocketObserver.onTextMessage(textMessage.mPayload);
			} else {
				Log.d(TAG, "could not call onTextMessage() .. handler already NULL");
			}

		} else if (message.obj instanceof WebSocketMessage.RawTextMessage) {
			WebSocketMessage.RawTextMessage rawTextMessage = (WebSocketMessage.RawTextMessage) message.obj;

			if (webSocketObserver != null) {
				webSocketObserver.onRawTextMessage(rawTextMessage.mPayload);
			} else {
				Log.d(TAG, "could not call onRawTextMessage() .. handler already NULL");
			}

		} else if (message.obj instanceof WebSocketMessage.BinaryMessage) {
			WebSocketMessage.BinaryMessage binaryMessage = (WebSocketMessage.BinaryMessage) message.obj;

			if (webSocketObserver != null) {
				webSocketObserver.onBinaryMessage(binaryMessage.mPayload);
			} else {
				Log.d(TAG, "could not call onBinaryMessage() .. handler already NULL");
			}

		} else if (message.obj instanceof WebSocketMessage.Ping) {
			WebSocketMessage.Ping ping = (WebSocketMessage.Ping) message.obj;
			Log.d(TAG, "WebSockets Ping received");

			WebSocketMessage.Pong pong = new WebSocketMessage.Pong();
			pong.mPayload = ping.mPayload;
			mWebSocketWriter.forward(pong);

		} else if (message.obj instanceof WebSocketMessage.Pong) {
			WebSocketMessage.Pong pong = (WebSocketMessage.Pong) message.obj;

			Log.d(TAG, "WebSockets Pong received" + pong.mPayload);

		} else if (message.obj instanceof WebSocketMessage.Close) {
			WebSocketMessage.Close close = (WebSocketMessage.Close) message.obj;

			Log.d(TAG, "WebSockets Close received (" + close.getCode() + " - " + close.getReason() + ")");

			mWebSocketWriter.forward(new WebSocketMessage.Close(WebSocketCloseCode.NORMAL));

			if (webSocketObserver != null) {
				webSocketObserver.onCloseMessage(close);
			} else {
				Log.d(TAG, "could not call onCloseMessage() .. handler already NULL");
			}

		} else if (message.obj instanceof WebSocketMessage.ServerHandshake) {
			WebSocketMessage.ServerHandshake serverHandshake = (WebSocketMessage.ServerHandshake) message.obj;

			Log.d(TAG, "opening handshake received");

			if (serverHandshake.mSuccess) {
				if (webSocketObserver != null) {
					webSocketObserver.onOpen();
				} else {
					Log.d(TAG, "could not call onOpen() .. handler already NULL");
				}
				mPreviousConnection = true;
			}

		} else if (message.obj instanceof WebSocketMessage.ConnectionLost) {
			//			WebSocketMessage.ConnectionLost connectionLost = (WebSocketMessage.ConnectionLost) message.obj;
			failConnection(WebSocketCloseNotification.CONNECTION_LOST, "WebSockets connection lost");

		} else if (message.obj instanceof WebSocketMessage.ProtocolViolation) {
			//			WebSocketMessage.ProtocolViolation protocolViolation = (WebSocketMessage.ProtocolViolation) message.obj;
			failConnection(WebSocketCloseNotification.PROTOCOL_ERROR, "WebSockets protocol violation");

		} else if (message.obj instanceof WebSocketMessage.Error) {
			WebSocketMessage.Error error = (WebSocketMessage.Error) message.obj;
			failConnection(WebSocketCloseNotification.INTERNAL_ERROR, "WebSockets internal error (" + error.mException.toString() + ")");

		} else if (message.obj instanceof WebSocketMessage.ServerError) {
			WebSocketMessage.ServerError error = (WebSocketMessage.ServerError) message.obj;
			failConnection(WebSocketCloseNotification.SERVER_ERROR, "Server error " + error.mStatusCode + " (" + error.mStatusMessage + ")");

		} else {
			processAppMessage(message.obj);

		}
	}



	public static class SocketThread extends Thread {
		private static final String WS_CONNECTOR = "WebSocketConnector";

		private final URI mWebSocketURI;

		private Socket mSocket = null;
		private String mFailureMessage = null;

		private Handler mHandler;



		public SocketThread(URI uri, WebSocketOptions options) {
			this.setName(WS_CONNECTOR);

			this.mWebSocketURI = uri;
		}



		@Override
		public void run() {
			Looper.prepare();
			this.mHandler = new Handler();
			synchronized (this) {
				notifyAll();
			}

			Looper.loop();
			Log.d(TAG, "SocketThread exited.");
		}



		public void startConnection() {
			try {
				String host = mWebSocketURI.getHost();
				int port = mWebSocketURI.getPort();

				if (port == -1) {
					if (mWebSocketURI.getScheme().equals(WSS_URI_SCHEME)) {
						port = 443;
					} else {
						port = 80;
					}
				}

				SocketFactory factory = null;
				if (mWebSocketURI.getScheme().equalsIgnoreCase(WSS_URI_SCHEME)) {
					factory = SSLCertificateSocketFactory.getDefault();
				} else {
					factory = SocketFactory.getDefault();
				}

				// Do not replace host string with InetAddress or you lose automatic host name verification
				this.mSocket = factory.createSocket(host, port);
			} catch (IOException e) {
				this.mFailureMessage = e.getLocalizedMessage();
			}

			synchronized (this) {
				notifyAll();
			}
		}

		public void stopConnection() {
			try {
				mSocket.close();
				this.mSocket = null;
			} catch (IOException e) {
				this.mFailureMessage = e.getLocalizedMessage();
			}
		}

		public Handler getHandler() {
			return mHandler;
		}
		public Socket getSocket() {
			return mSocket;
		}
		public String getFailureMessage() {
			return mFailureMessage;
		}
	}



	private static class ThreadHandler extends Handler {
		private final WeakReference<WebSocketConnection> mWebSocketConnection;



		public ThreadHandler(WebSocketConnection webSocketConnection) {
			super();

			this.mWebSocketConnection = new WeakReference<WebSocketConnection>(webSocketConnection);
		}



		@Override
		public void handleMessage(Message message) {
			WebSocketConnection webSocketConnection = mWebSocketConnection.get();
			if (webSocketConnection != null) {
				webSocketConnection.handleMessage(message);
			}
		}
	}

	public static class MasterHandler extends Handler {
      public boolean mWriterHasData = false;

      public void setWriterHasData(boolean hasWriterData) {
        synchronized (this) {
          mWriterHasData = hasWriterData;
        }
      }
      public boolean getWriterHasData() {
        synchronized (this) {
          return mWriterHasData;
        }
      }
   }
}
