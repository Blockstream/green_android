package com.greenaddress.jade;

import android.util.Log;

import com.jakewharton.rx.ReplayingShare;
import com.polidea.rxandroidble2.NotificationSetupMode;
import com.polidea.rxandroidble2.RxBleConnection;
import com.polidea.rxandroidble2.RxBleDevice;

import java.util.UUID;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.subjects.PublishSubject;

/**
 * Low-level BLE backend interface to Jade
 * Calls to send and receive bytes bytes to/from Jade.
 * Intended for use wrapped by JadeInterface (see JadeInterface.createBle()).
 */
public class JadeBleImpl extends JadeConnectionImpl {
    private static final String TAG = "JadeBleImpl";

    public static final UUID IO_SERVICE_UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    private static final UUID IO_TX_CHAR_UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
    private static final UUID IO_RX_CHAR_UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");

    // Set the MTU/MRU to something large enough to receive jade responses.
    // I believe the minimum would be 240+3 bytes - 240 is jade chunk size, and 3 for header info.
    // Anything less and read data is truncated and lost.  Larger values seem ok.
    // Set to match Jade's preferred MTU of 256.
    private static final int JADE_MTU = 256;

    private final RxBleDevice device;
    private final PublishSubject<Boolean> disconnectTrigger;

    private CompositeDisposable disposable;
    private Observable<RxBleConnection> connection;
    private io.reactivex.rxjava3.subjects.PublishSubject<Boolean> bleDisconnectEvent = io.reactivex.rxjava3.subjects.PublishSubject.create();

    JadeBleImpl(final RxBleDevice device) {
        this.device = device;

        // Set a condition to stop the connection/subscriptions
        this.disconnectTrigger = PublishSubject.create();
    }

    private boolean isBleConnected() {
        return this.device.getConnectionState() == RxBleConnection.RxBleConnectionState.CONNECTED;
    }

    private  void onBytesSent(final byte[] bytes) {
        Log.d(TAG, "Sent " + bytes.length + " bytes");
    }

    private  void onSendFailure(final Throwable t) {
        Log.e(TAG, "Send Failure: " + t);
        t.printStackTrace();
    }

    private  void onReceiveFailure(final Throwable t) {
        Log.e(TAG, "Receive Failure: " + t );
        t.printStackTrace();
    }

    @Override
    public io.reactivex.rxjava3.subjects.PublishSubject<Boolean> getBleDisconnectEvent() {
        return bleDisconnectEvent;
    }

    @Override
    public boolean isConnected() {
        return this.disposable != null && !this.disposable.isDisposed()
                && this.connection != null && isBleConnected();
    }

    @Override
    public void connect() {
        this.disposable = new CompositeDisposable();

        // Log connection state changes
        this.disposable.add(this.device.observeConnectionStateChanges()
                .subscribe(state -> {
                    Log.i(TAG, "Connection State Change: " + state);

                    if(state == RxBleConnection.RxBleConnectionState.DISCONNECTING){
                        // Trigger Disconnect
                        Log.i(TAG, "Send BLE disconnect event");
                        bleDisconnectEvent.onNext(true);
                    }
                })
        );

        // Create connection, set mtu, etc.
        this.connection = this.device.establishConnection(false)
                // Set a condition to stop the connection/subscriptions
                .takeUntil(disconnectTrigger)

                // Set the MTU to consistent with Jade stack
                .doOnNext(rxConn-> Log.d(TAG,"Requesting MTU: " + JADE_MTU))
                .flatMapSingle(rxConn ->
                        rxConn.requestMtu(JADE_MTU)
                                .doOnSuccess(mtu -> Log.i(TAG, "Successfully set the MTU to: " + mtu))
                                .ignoreElement()
                                .andThen(Single.just(rxConn))
                )

                // Compose with ReplayingShare to make this connection reusable from here, rather
                // than running the above code every time we try to subscribe/use the connection.
                .compose(ReplayingShare.instance());

        // Set the callback for receiving data over the ble connection
        // (Just collect into base-class queue of byte-arrays.)
        this.disposable.add(this.connection
                .doOnNext(observableBytes -> Log.d(TAG, "Setting up characteristic indication"))
                .flatMap(rxConn -> rxConn.setupIndication(IO_RX_CHAR_UUID, NotificationSetupMode.QUICK_SETUP))
                .doOnNext(observableBytes -> Log.i(TAG, "Indication setup complete"))
                .flatMap(observableBytes -> observableBytes)  // flatten
                .subscribe(super::onDataReceived,
                           this::onReceiveFailure)
        );
    }

    @Override
    public void disconnect() {
        if (this.disposable != null) {
            if (isConnected()) {
                this.disconnectTrigger.onNext(true);
            }
            this.disposable.dispose();
            this.disposable = null;
        }
        this.connection = null;
    }

    @Override
    public int write(final byte[] bytes) {
        this.disposable.add(this.connection
                .flatMap(rxConn -> rxConn.createNewLongWriteBuilder()
                        .setCharacteristicUuid(IO_TX_CHAR_UUID)
                        .setBytes(bytes)
                        .build())
                .subscribe(this::onBytesSent,
                           this::onSendFailure)
        );
        return bytes.length;
    }
}
