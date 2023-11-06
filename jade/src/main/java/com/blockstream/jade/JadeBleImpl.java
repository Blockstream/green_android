package com.blockstream.jade;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import com.jakewharton.rx3.ReplayingShare;
import com.polidea.rxandroidble3.NotificationSetupMode;
import com.polidea.rxandroidble3.RxBleConnection;
import com.polidea.rxandroidble3.RxBleDevice;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import kotlinx.coroutines.flow.MutableStateFlow;
import kotlinx.coroutines.flow.StateFlow;

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

    private static final int JADE_MTU = 515;

    private final RxBleDevice device;
    private final PublishSubject<Boolean> disconnectTrigger;
    private final Context context;

    private CompositeDisposable disposable;
    private Observable<RxBleConnection> connection;
    private MutableStateFlow<Boolean> disconnectEvent = ExtensionsKt.createDisconnectEvent();

    JadeBleImpl(final Context context, final RxBleDevice device) {
        this.context = context;
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
    public boolean isConnected() {
        return this.disposable != null && !this.disposable.isDisposed()
                && this.connection != null && isBleConnected();
    }

    private Completable createBleConnection(Single<Boolean> bondingEvent){
        return Completable.create(complete -> {

            // Log connection state changes
            this.disposable.add(this.device.observeConnectionStateChanges()
                    .subscribe(state -> {
                        Log.i(TAG, "Connection State Change: " + state);

                        if (state == RxBleConnection.RxBleConnectionState.DISCONNECTING) {
                            // Trigger Disconnect
                            Log.i(TAG, "Send BLE disconnect event");
                            disconnectEvent.setValue(true);
                        }
                    }, Throwable::printStackTrace)
            );

            // Create connection, set mtu, etc.
            this.connection = this.device.establishConnection(false)
                    // Set a condition to stop the connection/subscriptions
                    .takeUntil(disconnectTrigger)

                    .doOnError(throwable -> {
                        complete.tryOnError(throwable);
                    })

                    .doOnDispose(() -> {
                        complete.tryOnError(new Exception("Closed"));
                    })

                    // wait until bondingEvent fires
                    .delay((rxBleConnection) -> {
                        Log.d(TAG, "Delay bonding event");
                        return bondingEvent.toObservable();
                    })

                    // stabilization delay
                    .delay(1000, TimeUnit.MILLISECONDS)

                    // Set the MTU to consistent with Jade stack
                    .doOnNext(rxConn -> {
                        Log.d(TAG, "Requesting MTU: " + JADE_MTU);
                    })

                    .flatMapSingle(rxConn -> rxConn.requestMtu(JADE_MTU)
                            .doOnSuccess(mtu -> {
                                Log.i(TAG, "Successfully set the MTU to: " + mtu);
                            })
                            .ignoreElement()
                            .andThen(Single.just(rxConn))
                    )

                    // Compose with ReplayingShare to make this connection reusable from here, rather
                    // than running the above code every time we try to subscribe/use the connection.
                    .compose(ReplayingShare.instance());

            this.disposable.add(this.connection
                    .doOnNext(rxBleConnection -> Log.d(TAG, "Setting up characteristic indication"))
                    .flatMap(rxConn -> rxConn.setupIndication(IO_RX_CHAR_UUID, NotificationSetupMode.QUICK_SETUP))
                    .doOnNext(observableBytes -> {
                        Log.i(TAG, "Indication setup complete");
                        complete.onComplete();
                    })
                    .flatMap(observableBytes -> observableBytes)  // flatten
                    .subscribe(super::onDataReceived,
                            this::onReceiveFailure)
            );
        });
    }

    @Override
    public void connect() {
        this.disposable = new CompositeDisposable();

        Single<Boolean> bondingEvent = JadePairingManager.INSTANCE.pairWithDevice(context, device);

        // Block until BLE is bootstrapped properly
        createBleConnection(bondingEvent).blockingAwait();
    }

    @Override
    public void disconnect() {
        Log.i(TAG, "Disconnecting");
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
                        .setMaxBatchSize(512)
                        .setCharacteristicUuid(IO_TX_CHAR_UUID)
                        .setBytes(bytes)
                        .build())
                .subscribe(this::onBytesSent,
                           this::onSendFailure)
        );
        return bytes.length;
    }

    @Nullable
    @Override
    public StateFlow<Boolean> getDisconnectEvent() {
        return disconnectEvent;
    }
}
