package com.greenaddress.greenbits.ui.authentication;

import android.app.Dialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.btchip.BTChipConstants;
import com.btchip.BTChipDongle;
import com.btchip.BTChipDongle.BTChipFirmware;
import com.btchip.BTChipException;
import com.btchip.comm.BTChipTransport;
import com.btchip.comm.LedgerDeviceBLE;
import com.btchip.comm.android.BTChipTransportAndroid;
import com.google.common.util.concurrent.SettableFuture;
import com.greenaddress.gdk.GDKSession;
import com.greenaddress.greenapi.HWWallet;
import com.greenaddress.greenapi.Session;
import com.greenaddress.greenapi.data.HWDeviceData;
import com.greenaddress.greenapi.data.NetworkData;
import com.greenaddress.greenbits.AuthenticationHandler;
import com.greenaddress.greenbits.ui.BuildConfig;
import com.greenaddress.greenbits.ui.LoginActivity;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.UI;
import com.greenaddress.greenbits.ui.hardwarewallets.DeviceSelectorActivity;
import com.greenaddress.greenbits.wallets.BTChipHWWallet;
import com.greenaddress.greenbits.wallets.HardwareCodeResolver;
import com.greenaddress.greenbits.wallets.LedgerBLEAdapter;
import com.greenaddress.greenbits.wallets.JadeHWWallet;
import com.greenaddress.greenbits.wallets.TrezorHWWallet;
import com.greenaddress.jade.entities.JadeError;
import com.greenaddress.jade.JadeBleImpl;
import com.polidea.rxandroidble2.RxBleClient;
import com.polidea.rxandroidble2.RxBleDevice;
import com.greenaddress.jade.JadeAPI;
import com.satoshilabs.trezor.Trezor;

import java.util.List;
import java.util.Objects;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

import static com.greenaddress.greenapi.Session.getSession;

public class RequestLoginActivity extends LoginActivity {

    private static final String TAG = RequestLoginActivity.class.getSimpleName();

    private static final int VENDOR_BTCHIP     = 0x2581;
    private static final int VENDOR_LEDGER     = 0x2c97;
    private static final int VENDOR_TREZOR     = 0x534c;
    private static final int VENDOR_TREZOR_V2  = 0x1209;
    private static final int VENDOR_JADE       = 0x10c4;

    private UsbManager mUsbManager;
    private UsbDevice mUsb;

    private BluetoothDevice mBleDevice;

    private TextView mInstructionsText;
    private Dialog mPinDialog;
    private String mPin;
    private Integer mVendorId;
    private Boolean mInLedgerDashboard;

    private HWWallet mHwWallet;
    private TextView mActiveNetwork;
    private NetworkData networkData;
    private CompositeDisposable mDisposables;

    @Override
    protected int getMainViewId() { return R.layout.activity_first_login_requested; }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mInLedgerDashboard = false;
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        mInstructionsText = UI.find(this, R.id.first_login_instructions);
        mActiveNetwork = UI.find(this, R.id.activeNetwork);
        networkData = getNetwork();

        mDisposables = new CompositeDisposable();
    }

    @Override
    protected void onNewIntent(final Intent intent) {
        Log.d(TAG, "onNewIntent");
        setIntent(intent);
    }

    private void onUsbAttach(final UsbDevice usb) {
        Log.d(TAG, "onUsbAttach");
        if (mUsb != null && mUsb == usb) {
            Log.d(TAG, "onUsbAttach with existing USB");
            return;
        }
        mUsb = usb;
        mInLedgerDashboard = false;
        if (usb == null)
            return;

        final ImageView hardwareIcon = UI.find(this, R.id.hardwareIcon);
        mVendorId = usb.getVendorId();
        Log.d(TAG, "Vendor: " + mVendorId + " Product: " + usb.getProductId());

        switch (mVendorId) {
        case VENDOR_JADE:
            hardwareIcon.setImageResource(R.drawable.ic_jade);
            final JadeAPI jadeAPI = JadeAPI.createSerial(getSession(), mUsbManager, mUsb, 115200);
            onJade(jadeAPI);
            return;

        case VENDOR_TREZOR:
        case VENDOR_TREZOR_V2:
            hardwareIcon.setImageResource(R.drawable.ic_trezor);
            onTrezor();
            return;

        case VENDOR_BTCHIP:
        case VENDOR_LEDGER:
            hardwareIcon.setImageResource(R.drawable.ic_ledger);
            BTChipTransport transport = null;
            try {
                transport = BTChipTransportAndroid.open(mUsbManager, mUsb);
            } catch (final Exception e) {
                e.printStackTrace();
            }
            if (transport == null) {
                showInstructions(R.string.id_please_reconnect_your_hardware);
                return;
            }

            if (BTChipTransportAndroid.isLedgerWithScreen(usb)) {
                // User entered PIN on-device
                onLedger(transport, true);
            } else {
                // Prompt for PIN to unlock device before setting it up
                showLedgerPinDialog(transport);
            }
        }
    }

    private void onBleAttach(final ParcelUuid serviceId, final BluetoothDevice btDevice) {
        final RxBleDevice bleDevice = RxBleClient.create(this).getBleDevice(btDevice.getAddress());
        if (serviceId == null || bleDevice == null || bleDevice.getName() == null) {
            mBleDevice = null;
            onNoHardwareWallet();
            return;
        }

        mBleDevice = btDevice;
        Log.d(TAG, "onBleAttach " + bleDevice.getName() + " (" + bleDevice.getMacAddress() + ")");
        final ImageView hardwareIcon = UI.find(this, R.id.hardwareIcon);

        // Check service id matches a supported hw wallet
        if (JadeBleImpl.IO_SERVICE_UUID.equals(serviceId.getUuid())) {
            // Blockstream Jade
            hardwareIcon.setImageResource(R.drawable.ic_jade);

            // Create JadeAPI on BLE device
            final JadeAPI jadeAPI = JadeAPI.createBle(getSession(), bleDevice);
            onJade(jadeAPI);

        } else if (LedgerDeviceBLE.SERVICE_UUID.equals(serviceId.getUuid())) {
            // Ledger (Nano X)
            hardwareIcon.setImageResource(R.drawable.ic_ledger);

            // Ledger BLE adapter will call the 'onLedger' function when the BLE connection is established
            LedgerBLEAdapter.connectLedgerBLE(this, btDevice, this::onLedger, this::onLedgerError);
        } else {
            mBleDevice = null;
            onNoHardwareWallet();
        }
    }

    private void onJade(final JadeAPI jade) {
        // Connect to jade (using background thread)
        mDisposables.add(Observable.just(jade)
                .subscribeOn(Schedulers.computation())
                .map(JadeAPI::connect)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        rslt -> {
                            if (rslt) {
                                // Connected - ok to proceed to fw check, pin, login etc.
                                onJadeConnected(jade);
                            } else {
                                Log.e(TAG, "Failed to connect to Jade");
                                showInstructions(R.string.id_please_reconnect_your_hardware);
                            }
                        },
                        throwable -> {
                            Log.e(TAG, "Exception connecting to Jade");
                            showInstructions(R.string.id_please_reconnect_your_hardware);
                        }
                )
        );
    }

    private void onJadeConnected(final JadeAPI jade) {
        mDisposables.add(Single.just(getSession())
                .subscribeOn(Schedulers.computation())

                // Connect GDKSession first (on a background thread), as we use httpRequest() as part of
                // Jade login (to access firmware server and to interact with the pinserver).
                // This also acts as a handy check that we have network connectivity before we start.
                .map(this::reconnectSession)
                .doOnError(throwable -> Log.e(TAG, "Exception connecting GDK - " + throwable))

                // Then create JadeHWWallet instance and authenticate (with pinserver) still on background thread
                .doOnSuccess(session -> Log.d(TAG, "Creating Jade HW Wallet)"))
                .map(session -> new HWDeviceData("Jade", true, true, HWDeviceData.HWDeviceDataLiquidSupport.Lite))
                .map(hwDeviceData -> new JadeHWWallet(jade, networkData, hwDeviceData))
                .flatMap(jadeWallet -> jadeWallet.authenticate(this))

                // If all succeeded, set as current hw wallet and login ... otherwise handle error/display error
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        jadeWallet -> {
                            showInstructions(R.string.id_logging_in);
                            mHwWallet = jadeWallet;
                            doLogin(false);
                        },
                        throwable -> {
                            Log.e(TAG, "Connecting to Jade HW wallet got error: " + throwable);
                            if (throwable instanceof JadeError) {
                                final JadeError jaderr = (JadeError)throwable;
                                if (jaderr.getCode()  == JadeError.UNSUPPORTED_FIRMWARE_VERSION) {
                                    showInstructions(R.string.id_outdated_hardware_wallet);
                                } else if (jaderr.getCode()  == JadeError.CBOR_RPC_NETWORK_MISMATCH) {
                                    showInstructions(R.string.id_the_network_selected_on_the);
                                } else {
                                    showInstructions(R.string.id_please_reconnect_your_hardware);
                                }
                            } else if ("GDK_ERROR_CODE -1 GA_connect".equals(throwable.getMessage())) {
                                showInstructions(R.string.id_unable_to_contact_the_green);
                            } else {
                                showInstructions(R.string.id_please_reconnect_your_hardware);
                            }
                            jade.disconnect();
                        }
                )
        );
    }

    private void onTrezor() {
        final Trezor t;
        t = Trezor.getDevice(this);

        if (networkData.getLiquid()) {
            showInstructions(R.string.id_hardware_wallet_support_for);
            return;
        }
        if (t == null)
            return;

        final List<Integer> version = t.getFirmwareVersion();
        final int vendorId = t.getVendorId();
        Log.d(TAG,"Trezor Version: " + version + " vendorid:" + vendorId + " productid:" + t.getProductId());

        // Min allowed: v1.6.0 & v2.1.0
        final boolean isFirmwareOutdated = version.get(0) < 1 ||
                                           (version.get(0) == 1 && version.get(1) < 6) ||
                                           (version.get(0) == 1 && version.get(1) == 6 && version.get(2) < 0) ||
                                           (version.get(0) == 2 && version.get(1) < 1);
        if (isFirmwareOutdated) {
            showFirmwareOutdated(() -> onTrezorConnected(t), null);
            return;
        }

        // All good
        onTrezorConnected(t);
    }

    private void onTrezorConnected(final Trezor t) {

        Log.d(TAG, "Creating Trezor HW wallet");
        final HWDeviceData hwDeviceData = new HWDeviceData("Trezor", false, false,
                                                           HWDeviceData.HWDeviceDataLiquidSupport.None);
        mHwWallet = new TrezorHWWallet(t, networkData, hwDeviceData);

        doLogin(true);
    }

    private void showLedgerPinDialog(final BTChipTransport transport) {
        mPinDialog = UI.dismiss(this, mPinDialog);

        final View v = UI.inflateDialog(this, R.layout.dialog_btchip_pin);

        mPinDialog = UI.popup(this, R.string.id_pin)
                     .customView(v, true)
                     .backgroundColor(getResources().getColor(R.color.buttonJungleGreen))
                     .onPositive((dialog, which) -> {
                         mPin = UI.getText(v, R.id.btchipPINValue);
                         mDisposables.add(Observable.just(getSession())
                                 .observeOn(Schedulers.computation())
                                 .subscribe(session -> onLedger(transport, false))
                         );
                     })
                     .onNegative((dialog, which) -> {
                         UI.toast(this, R.string.id_no_pin_provided_exiting, Toast.LENGTH_LONG);
                         finish();
                     }).build();

        UI.mapEnterToPositive(mPinDialog, R.id.btchipPINValue);
        UI.showDialog(mPinDialog);
    }

    private void closeLedger(final BTChipTransport transport) {
        try {
            transport.close();
        } catch (final BTChipException ignored) {}

        mUsb = null;
        mBleDevice = null;
        mInLedgerDashboard = true;
    }

    private void onLedgerError(final BTChipTransport transport) {
        showInstructions(R.string.id_please_reconnect_your_hardware);
        closeLedger(transport);
    }

    private void onLedger(final BTChipTransport transport, final boolean hasScreen) {
        showInstructions(R.string.id_logging_in);
        final String pin = mPin;
        mPin = null;

        transport.setDebug(BuildConfig.DEBUG);
        try {
            final BTChipDongle dongle = new BTChipDongle(transport, hasScreen);
            try {
                // This should only be supported by the Nano X
                final BTChipDongle.BTChipApplication application = dongle.getApplication();
                Log.d(TAG, "Ledger application:" + application);

                if (application.getName().contains("OLOS")) {
                    showInstructions(R.string.id_ledger_dashboard_detected);
                    closeLedger(transport);
                    return;
                }

                final boolean netMainnet = networkData.getMainnet();
                final boolean netLiquid = networkData.getLiquid();
                final boolean hwMainnet = !application.getName().contains("Test");
                final boolean hwLiquid = application.getName().contains("Liquid");
                Log.d(TAG, "Ledger application:" + application.getName() + ", network is mainnet:"+ netMainnet);

                if (netMainnet != hwMainnet || netLiquid != hwLiquid) {
                    // We using the wrong app, prompt the user to open the right app.
                    showInstructions(R.string.id_the_network_selected_on_the);
                    closeLedger(transport);
                    return;
                }
            } catch (final Exception e) {
                // Log but otherwise ignore
                Log.e(TAG, "Error trying to get Ledger application details: " + e);
            }

            // We don't ask for firmware version while in the dashboard, since the Ledger Nano X would return invalid status
            final BTChipFirmware fw = dongle.getFirmwareVersion();
            Log.d(TAG, "BTChip/Ledger firmware version " + fw);

            boolean isFirmwareOutdated = true;
            if (fw.getArchitecture() == BTChipDongle.BTCHIP_ARCH_LEDGER_1 && fw.getMajor() > 0) {
                // Min allowed: v1.0.4
                isFirmwareOutdated = (fw.getMajor() == 1 && fw.getMinor() < 0) ||
                                     (fw.getMajor() == 1 && fw.getMinor() == 0 && fw.getPatch() < 4);
            } else if (fw.getArchitecture() == BTChipDongle.BTCHIP_ARCH_NANO_SX && fw.getMajor() > 0) {
                // Min allowed: v1.3.7
                isFirmwareOutdated = (fw.getMajor() == 1 && fw.getMinor() < 3) ||
                                     (fw.getMajor() == 1 && fw.getMinor() == 3 && fw.getPatch() < 7);
            }

            if (isFirmwareOutdated) {
                showFirmwareOutdated(() -> onLedgerConnected(dongle, pin),
                                     () -> closeLedger(transport));
                return;
            }

            // All good
            onLedgerConnected(dongle, pin);
        } catch (final BTChipException e) {
            if (e.getSW() != BTChipConstants.SW_INS_NOT_SUPPORTED)
                e.printStackTrace();

            if (e.getSW() == 0x6faa) {
                showInstructions(R.string.id_please_disconnect_your_ledger);
            } else {
                showInstructions(R.string.id_ledger_dashboard_detected);
            }
            closeLedger(transport);
        }
    }

    private void onLedgerConnected(final BTChipDongle dongle, final String pin) {
        final SettableFuture<Integer> pinCB = SettableFuture.create();

        final boolean havePin = !TextUtils.isEmpty(pin);
        Log.d(TAG, "Creating Ledger HW wallet" + (havePin ? " with PIN" : ""));
        final HWDeviceData hwDeviceData = new HWDeviceData("Ledger", false, true,
                                                           HWDeviceData.HWDeviceDataLiquidSupport.Lite);
        mHwWallet = new BTChipHWWallet(dongle, havePin ? pin : null, pinCB, networkData, hwDeviceData);

        doLogin(true);
    }

    private Session reconnectSession(final Session session) throws Exception {
        Log.d(TAG, "(re-)connecting gdk session)");
        session.disconnect();
        this.connect();
        return session;
    }

    private void doLogin(final boolean bReConnectSession) {
        mDisposables.add(Observable.just(getSession())
                .observeOn(Schedulers.computation())
                .map((session) -> {
                    // Reconnect session if required
                    if (bReConnectSession) {
                        reconnectSession(session);
                    }

                    // Register user/hw-wallet and login
                    session.registerUser(mHwWallet.getHWDeviceData(), "").resolve(null,
                                                                                  new HardwareCodeResolver(this, mHwWallet));
                    session.login(mHwWallet.getHWDeviceData(), "", "").resolve(null,
                                                                               new HardwareCodeResolver(this, mHwWallet));
                    session.setHWWallet(mHwWallet);
                    return session;
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((session) -> {
                    onPostLogin();
                    stopLoading();
                    onLoggedIn();
                }, (final Throwable e) -> {
                    stopLoading();
                    GDKSession.get().disconnect();
                    UI.toast(this, R.string.id_error_logging_in_with_hardware, Toast.LENGTH_LONG);
                    showInstructions(R.string.id_please_reconnect_your_hardware);
                })
        );
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mPinDialog = UI.dismiss(this, mPinDialog);
        if (mDisposables != null) {
            mDisposables.dispose();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mActiveNetwork.setText(getString(R.string.id_s_network, networkData.getName()));

        final Intent intent = getIntent();

        if (ACTION_USB_ATTACHED.equalsIgnoreCase(intent.getAction())) {
            onUsbAttach(intent.getParcelableExtra(UsbManager.EXTRA_DEVICE));
        }

        if (DeviceSelectorActivity.ACTION_BLE_SELECTED.equalsIgnoreCase(intent.getAction())) {
            onBleAttach(intent.getParcelableExtra(BluetoothDevice.EXTRA_UUID),
                    Objects.requireNonNull(intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)));
        }

        if (mUsb != null || mBleDevice != null || mInLedgerDashboard) {
            // Continue displaying instructions until the user opens the
            // correct wallet app, or log in completes/errors out
            return;
        }

        // No hardware wallet after all
        onNoHardwareWallet();
    }

    private void onNoHardwareWallet() {
        // No hardware wallet, jump to PIN or 1st screen entry
        final Intent intent = getIntent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        if (AuthenticationHandler.hasPin(this))
            startActivityForResult(new Intent(this, PinActivity.class), 0);
        else
            startActivityForResult(new Intent(this, FirstScreenActivity.class), 0);
    }

    private void showInstructions(final int resId) {
        runOnUiThread(() -> {
            mInstructionsText.setText(resId);
            UI.show(mInstructionsText);
        });
    }

    private void showFirmwareOutdated(final Runnable onContinue, final Runnable onClose) {
        if (!BuildConfig.DEBUG) {
            // Only allow the user to skip firmware checks in debug builds.
            showInstructions(R.string.id_outdated_hardware_wallet);
            if (onClose != null) {
                onClose.run();
            }
            return;
        }

        runOnUiThread(() -> {
            final MaterialDialog d;
            d = UI.popup(RequestLoginActivity.this, R.string.id_warning, R.string.id_continue, R.string.id_cancel)
                .content(R.string.id_outdated_hardware_wallet)
                .onNegative((dialog, which) -> { if (onClose != null) { onClose.run(); } } )
                .onPositive((dialog, which) -> { if (onContinue != null) { onContinue.run(); } } )
                .build();
            UI.setDialogCloseHandler(d, this::finishOnUiThread);
            d.show();
        });
    }
}
