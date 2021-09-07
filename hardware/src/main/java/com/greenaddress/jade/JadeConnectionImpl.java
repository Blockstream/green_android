package com.greenaddress.jade;

import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.subjects.BehaviorSubject;
import io.reactivex.rxjava3.subjects.PublishSubject;

/**
 * Abstract a low-level Jade connection - eg. over serial or ble.
 */
public abstract class JadeConnectionImpl {
    private final static String TAG = "JadeConnectionImpl";

    // Derived classes push incoming/received data into this queue
    private final BlockingQueue<byte[]> dataReceived;
    private ByteArrayInputStream dataToRead;

    JadeConnectionImpl() {
        this.dataReceived = new LinkedBlockingQueue<>();
        this.dataToRead = new ByteArrayInputStream(new byte[0]);
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            this.disconnect();
        } catch(final Throwable t){
            throw t;
        } finally {
            super.finalize();
        }
    }

    abstract public PublishSubject<Boolean> getBleDisconnectEvent();

    abstract public boolean isConnected();

    abstract public void connect();

    abstract public void disconnect();

    abstract public int write(final byte[] bytes);

    // Function to push data into the dataReceived queue
    protected void onDataReceived(final byte[] data) {
        Log.d(TAG, "Received " + data.length + " bytes");
        this.dataReceived.add(data);
    }
/*
    // Expose the data received as an InputStream.  The timeout refers to the timeout between
    // byte fetches from the underlying stream/interface.
    // If the timeout is zero bytes must be present immediately.
    // A timeout of less than zero and the calls block until a byte is received.
    // The read call will return -1 if the timeout expires and no byte is available.
    public InputStream getInputStream(final int timeout) {
        final JadeConnectionImpl parent = this;
        return new InputStream() {
            @Override
            public int read() throws IOException {
                final Byte next = parent.read(timeout);
                return next != null ? next : -1;
            }
        };
    }
*/
    // Read a single byte, waiting for the timeout period (ms) if necessary.
    // If the timeout is zero data must be present immediately.
    // A timeout of less than zero and the call blocks until a byte is received.
    // Returns null if the timeout expires and no byte is available.
    public Byte read(final int timeout) {
        // If available, just read from output stream
        if (this.dataToRead.available() > 0) {
            return (byte)this.dataToRead.read();
        }

        // Otherwise, reload output stream from queue
        final int pollTimeout = (timeout >= 0) ? timeout : 10000;
        while (true) {
            try {
                final byte[] data = this.dataReceived.poll(pollTimeout, TimeUnit.MILLISECONDS);
                if (data != null) {
                    // Refresh byte stream from next byte array, and call self again
                    this.dataToRead = new ByteArrayInputStream(data);
                    return read(timeout);
                } else if (timeout >= 0) {
                    // Timed out
                    Log.w(TAG, "read() timed-out - timeout(ms): " + timeout);
                    return null;
                }
                // else no timeout, so loop waiting
            } catch (final InterruptedException e) {
                // Just go round the while loop again
                Log.w(TAG, "Ignoring read() loop interruption - " + e.getMessage());
            }
        }
    }

    // Reads all the data that is currently outstanding (ie. that has been received).
    // Returns empty byte array if no data received/present.
    public byte[] drain() {
        try {
            final ByteArrayOutputStream drained = new ByteArrayOutputStream();

            // Drain the 'to read next' stream
            if (this.dataToRead != null && dataToRead.available() > 0) {
                IOUtils.copy(this.dataToRead, drained);
            }

            // Drain the queue of data received
            final List<byte[]> received = new ArrayList<>(this.dataReceived.size());
            this.dataReceived.drainTo(received);
            for (final byte[] bytes : received) {
                drained.write(bytes);
            }

            // Return as one large byte array
            return drained.toByteArray();
        } catch (final IOException e) {
            // This exception can happen with IO-Streams in general and hence is in the signature,
            // but should not occur with Byte-Streams as there is no real underlying IO happening.
            Log.e(TAG, "IOException should not occur with ByteStream ! - " + e.getMessage());
            return new byte[0];
        }
    }
}
