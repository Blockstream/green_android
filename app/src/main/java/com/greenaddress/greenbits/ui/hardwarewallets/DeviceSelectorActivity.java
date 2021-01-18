package com.greenaddress.greenbits.ui.hardwarewallets;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.os.SystemClock;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.btchip.comm.LedgerDeviceBLE;
import com.greenaddress.greenbits.ui.LoginActivity;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.UI;
import com.greenaddress.greenbits.ui.authentication.RequestLoginActivity;
import com.greenaddress.jade.JadeBleImpl;
import com.polidea.rxandroidble2.RxBleClient;
import com.polidea.rxandroidble2.RxBleConnection;
import com.polidea.rxandroidble2.exceptions.BleScanException;
import com.polidea.rxandroidble2.scan.ScanFilter;
import com.polidea.rxandroidble2.scan.ScanRecord;
import com.polidea.rxandroidble2.scan.ScanResult;
import com.polidea.rxandroidble2.scan.ScanSettings;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;

public class DeviceSelectorActivity extends LoginActivity implements DeviceAdapter.OnAdapterInterface {

    public static final String ACTION_BLE_SELECTED = "android.hardware.ble.action.ACTION_BLE_SELECTED";

    private static final String TAG = DeviceSelectorActivity.class.getSimpleName();

    // NOTE: BLE_LOCATION_PERMISSION should be set to FINE for Android 10 and above, or COARSE for 9 and below
    // See: https://developer.android.com/about/versions/10/privacy/changes#location-telephony-bluetooth-wifi
    private static final String BLE_LOCATION_PERMISSION = (android.os.Build.VERSION.SDK_INT > Build.VERSION_CODES.P)
            ? android.Manifest.permission.ACCESS_FINE_LOCATION : android.Manifest.permission.ACCESS_COARSE_LOCATION;

    private static final int REQUEST_PERMISSION_LOCATION = 101;

    // Supported hw
    private static ParcelUuid PARCEL_SERVICE_UUID_LEDGER = new ParcelUuid(LedgerDeviceBLE.SERVICE_UUID);
    private static ParcelUuid PARCEL_SERVICE_UUID_JADE = new ParcelUuid(JadeBleImpl.IO_SERVICE_UUID);

    private RecyclerView mRecyclerView;
    private DeviceAdapter mAdapter;
    private RxBleClient mRxBleClient;
    private CompositeDisposable mDisposables;

    @Override
    protected int getMainViewId() {
        return R.layout.activity_hardware_selector;
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitleBackTransparent();

        mAdapter = new DeviceAdapter();
        mAdapter.setOnAdapterInterface(this);

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.addItemDecoration(new DividerItemDecoration(mRecyclerView.getContext(),
                                                                  DividerItemDecoration.VERTICAL));
        mRecyclerView.setAdapter(mAdapter);
        mRxBleClient = RxBleClient.create(this);

        mDisposables = new CompositeDisposable();
    }

    @Override
    public void onPause() {
        super.onPause();
        dispose();
    }

    @Override
    public void onResume() {
        super.onResume();
        reload();
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, final String permissions[],
                                           final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_LOCATION) {
            for (final String permission : permissions) {
                if (BLE_LOCATION_PERMISSION.equals(permission)) {
                    // Do stuff if permission granted
                    scanBleDevices();
                }
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void reload() {
        dispose();

        //reloadUsbDevices();

        // NOTE: BLE_LOCATION_PERMISSION should be set to COARSE or FINE as appropriate for running android version
        if (ContextCompat.checkSelfPermission(this, BLE_LOCATION_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {BLE_LOCATION_PERMISSION},
                                              REQUEST_PERMISSION_LOCATION);
            return;
        }

        // Add bonded ble devices
        // addBleBondedDevices();

        // Add already-connected ble devices
        addBleConnectedDevices();

        // Scan for nearby ble devices
        scanBleDevices();
    }

    /*
       private void reloadUsbDevices() {
        final UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        final HashMap<String, UsbDevice> devices = manager.getDeviceList();
        for (final UsbDevice device : devices.values()) {
            final String name = device.getDeviceName();
            //mDevices.add(name);
        }
        mAdapter.notifyDataSetChanged();
       }
       /*

       /* private void reloadBluetoothDevices() {
         // check if bluetooth is supported on your hardware
         if (!mRxBluetooth.isBluetoothAvailable())
             return;
         if (!mRxBluetooth.isBluetoothEnabled()) {
             mRxBluetooth.enableBluetooth(this, REQUEST_ENABLE_BT);
             return;
         }
         if (mRxBluetooth != null)
             mRxBluetooth.cancelDiscovery();
         mRxBluetooth.startDiscovery();

         if (mRxBluetoothSubscription != null)
             mRxBluetoothSubscription.dispose();

         mRxBluetoothSubscription = mRxBluetooth.observeDevices()
                 .observeOn(AndroidSchedulers.mainThread())
                 .subscribeOn(Schedulers.computation())
                 .subscribe(new Consumer<BluetoothDevice>() {
                     @Override
                     public void accept(final BluetoothDevice bluetoothDevice) throws Exception {
                         if (bluetoothDevice == null)
                             return;
                         Log.d(TAG, bluetoothDevice.getName());
                         mDevices.add(bluetoothDevice.getName());
                         mAdapter.notifyDataSetChanged();
                     }
                 });
       }*/

    // Traverse the passed devices, and add any supported devices to the list view
    private void addSupportedDevices(final Collection<BluetoothDevice> devices) {
        final long ts = SystemClock.elapsedRealtime();
        for (final BluetoothDevice device : devices) {
            mDisposables.add(mRxBleClient.getBleDevice(device.getAddress())
                    .establishConnection(false)
                    .map(RxBleConnection::discoverServices)
                    .observeOn(AndroidSchedulers.mainThread())
                    .map(Single::blockingGet)
                    .subscribe(services -> {
                        // Show devices which offer a recognised/supported service
                        for (final BluetoothGattService service : services.getBluetoothGattServices()) {
                            final UUID serviceId = service.getUuid();

                            // Blockstream Jade
                            if (JadeBleImpl.IO_SERVICE_UUID.equals(serviceId)) {
                                mAdapter.add(PARCEL_SERVICE_UUID_JADE, R.drawable.ic_jade, device, ts);
                                break;
                            }

                            // Ledger (Nano X)
                            if (LedgerDeviceBLE.SERVICE_UUID.equals(serviceId)) {
                                mAdapter.add(PARCEL_SERVICE_UUID_LEDGER, R.drawable.ic_ledger, device, ts);
                                break;
                            }
                        }
                    },
                    throwable -> {
                        Log.e(TAG, "Error trying to browse services for " + device.getName() + " (" + device + ") : " + throwable.getMessage());
                        throwable.printStackTrace();
                    })
            );
        }
    }

    private void addBleBondedDevices() {
        final BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        if (manager == null)
            return;

        // Add bonded devices
        addSupportedDevices(manager.getAdapter().getBondedDevices());
    }

    private void addBleConnectedDevices() {
        final BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        if (manager == null)
            return;

        // Add supported devices that are already connected (and may not be 'advertising')
        addSupportedDevices(manager.getConnectedDevices(BluetoothProfile.GATT));
    }

    private void scanBleDevices() {
        // Timer to sweep stale items from the scan result every few seconds
        mDisposables.add(Observable.interval(3, 1, TimeUnit.SECONDS)
                .doOnNext(v -> addBleConnectedDevices())
                .map(v -> SystemClock.elapsedRealtime())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(ts -> mAdapter.removeStale(ts - 2000))
        );

        // Scan for nearby ble devices
        mDisposables.add(mRxBleClient.scanBleDevices(
                new ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                        .build(),
                // add custom filters - filter to just supported hw
                // Blockstream Jade
                new ScanFilter.Builder()
                        .setServiceUuid(PARCEL_SERVICE_UUID_JADE)
                        .build(),
                // Ledger (Nano X)
                new ScanFilter.Builder()
                        .setServiceUuid(PARCEL_SERVICE_UUID_LEDGER)
                        .build()
                )
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally(this::dispose)
                .subscribe(this::onScan, this::onScanFailure)
        );
    }

    private void onScan(final ScanResult rslt) {
        // Filter to supported hw, and associate with service uuid
        final long ts = TimeUnit.MILLISECONDS.convert(rslt.getTimestampNanos(), TimeUnit.NANOSECONDS);
        final ScanRecord rec = rslt.getScanRecord();
        if (rec != null && rec.getServiceUuids() != null && !rec.getServiceUuids().isEmpty()) {
            // Blockstream Jade
            if (rec.getServiceUuids().contains(PARCEL_SERVICE_UUID_JADE)) {
                mAdapter.add(PARCEL_SERVICE_UUID_JADE, R.drawable.ic_jade, rslt.getBleDevice().getBluetoothDevice(), ts);
            }

            // Ledger (Nano X)
            else if (rec.getServiceUuids().contains(PARCEL_SERVICE_UUID_LEDGER)) {
                mAdapter.add(PARCEL_SERVICE_UUID_LEDGER, R.drawable.ic_ledger, rslt.getBleDevice().getBluetoothDevice(), ts);
            }
        }
    }

    private void onScanFailure(final Throwable throwable) {
        if (throwable instanceof BleScanException) {
            UI.toast(this, ((BleScanException) throwable).getLocalizedMessage(), Toast.LENGTH_LONG);
        }
    }

    private void dispose() {
        if (mDisposables != null) {
            mDisposables.clear();
        }
        mAdapter.clear();
    }

    private void loginWithDevice(final DeviceAdapter.DeviceEntry entry) {
        // Stop scanning
        dispose();

        // Send selected device to RequestLoginActivity
        startActivity(new Intent(this, RequestLoginActivity.class)
                .setAction(ACTION_BLE_SELECTED)
                .putExtra(BluetoothDevice.EXTRA_UUID, entry.serviceId)
                .putExtra(BluetoothDevice.EXTRA_DEVICE, entry.device));
    }

    private void handleBonding(final DeviceAdapter.DeviceEntry entry) {
        Log.i(TAG, "Handling bonding events ...");
        final LoginActivity activity = this;
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);

        this.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, final Intent intent) {
                Log.i(TAG, "handleBonding() event: " + intent.getAction());
                if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(intent.getAction())) {
                    final BluetoothDevice btDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (btDevice != null && entry.device.getAddress().equals(btDevice.getAddress())) {
                        final int prevState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, 0);
                        if (prevState == BluetoothDevice.BOND_BONDING || prevState == BluetoothDevice.BOND_BONDED) {
                            final int currState = btDevice.getBondState();

                            if (currState == BluetoothDevice.BOND_BONDED) {
                                // Bonding succeeded, can now connect/login with this device
                                Log.i(TAG,
                                        "Bond established - initiating connect/login : " + btDevice.getName() + " (" +
                                                btDevice.getAddress() + ")");
                                context.unregisterReceiver(this);
                                loginWithDevice(entry);
                            } else if (currState == BluetoothDevice.BOND_NONE) {
                                // error
                                Log.e(TAG, "Bonding failed for " + btDevice.getName() + " (" + btDevice.getAddress() + ")");
                                context.unregisterReceiver(this);
                                UI.popup(activity, R.string.id_please_reconnect_your_hardware).show();
                            }
                        }
                    }
                }
            }
        }, intentFilter);
    }

    private boolean initiateBonding(final DeviceAdapter.DeviceEntry entry) {
        Log.i(TAG, "Initiating bonding.");
        if (!entry.device.createBond()) {
            Log.e(TAG, "Initiating bonding failed.");
            UI.popup(this, R.string.id_please_reconnect_your_hardware).show();
            return false;
        }

        // Initiated creating bond, handle responses
        handleBonding(entry);
        return true;
    }

    @Override
    public void onItemClick(final DeviceAdapter.DeviceEntry entry) {
        Log.d(TAG, "Clicked: " + entry.device.getName() + "  (" + entry.device.getAddress() + ")");

        if (entry.device.getBondState() == BluetoothDevice.BOND_BONDED) {
            // Already bonded, connect/login with device
            Log.i(TAG, "Existing bond found for device - initiating connect/login");
            loginWithDevice(entry);
        } else {
            Log.i(TAG, "Existing bond not found for device - initiating bonding");
            initiateBonding(entry);
        }
    }
}
