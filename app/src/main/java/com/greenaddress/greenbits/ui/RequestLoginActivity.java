package com.greenaddress.greenbits.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.btchip.BTChipConstants;
import com.btchip.BTChipDongle;
import com.btchip.BTChipDongle.BTChipFirmware;
import com.btchip.BTChipException;
import com.btchip.comm.BTChipTransport;
import com.btchip.comm.android.BTChipTransportAndroid;
import com.btchip.comm.android.BTChipTransportAndroidNFC;
import com.google.common.util.concurrent.SettableFuture;
import com.greenaddress.gdk.CodeResolver;
import com.greenaddress.greenapi.ConnectionManager;
import com.greenaddress.greenapi.HWWallet;
import com.greenaddress.greenapi.data.HWDeviceData;
import com.greenaddress.greenbits.wallets.BTChipHWWallet;
import com.greenaddress.greenbits.wallets.TrezorHWWallet;
import com.satoshilabs.trezor.Trezor;

import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.Callable;

import nordpol.android.AndroidCard;
import nordpol.android.OnDiscoveredTagListener;
import nordpol.android.TagDispatcher;

public class RequestLoginActivity extends LoginActivity implements Observer, OnDiscoveredTagListener {

    private static final String TAG = RequestLoginActivity.class.getSimpleName();
    private static final byte DUMMY_COMMAND[] = { (byte)0xE0, (byte)0xC4, (byte)0x00, (byte)0x00, (byte)0x00 };

    private static final int VENDOR_BTCHIP    = 0x2581;
    private static final int VENDOR_LEDGER    = 0x2c97;
    private static final int VENDOR_TREZOR    = 0x534c;
    private static final int VENDOR_TREZOR_V2 = 0x1209;

    private UsbManager mUsbManager;
    private UsbDevice mUsb;

    private TextView mInstructionsText;
    private Dialog mPinDialog;
    private String mPin;
    private Integer mVendorId;
    private Boolean mInLedgerDashboard;

    private HWWallet mHwWallet;
    private TagDispatcher mTagDispatcher;
    private Tag mTag;
    private SettableFuture<BTChipTransport> mTransportFuture;
    private MaterialDialog mNfcWaitDialog;
    private HWDeviceData mHwDeviceData;
    private CodeResolver mHwResolver;

    @Override
    protected int getMainViewId() { return R.layout.activity_first_login_requested; }

    @Override
    protected void onCreateWithService(final Bundle savedInstanceState) {
        mInLedgerDashboard = false;
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        mInstructionsText = UI.find(this, R.id.first_login_instructions);
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
        case VENDOR_TREZOR:
        case VENDOR_TREZOR_V2:
            hardwareIcon.setImageResource(R.drawable.trezor);
            onTrezor();
            return;
        case VENDOR_BTCHIP:
        case VENDOR_LEDGER:
            hardwareIcon.setImageResource(R.drawable.ledger);
            if (BTChipTransportAndroid.isLedgerWithScreen(usb)) {
                // User entered PIN on-device
                onLedger(true);
            } else {
                // Prompt for PIN to unlock device before setting it up
                showPinDialog();
            }
            return;
        }
    }

    private boolean onTrezor() {
        final Trezor t;
        t = Trezor.getDevice(this);

        if (t == null)
            return false;

        final List<Integer> version = t.getFirmwareVersion();
        boolean isFirmwareOutdated = false;
        final int vendorId = t.getVendorId();
        if (vendorId == VENDOR_TREZOR) {
            isFirmwareOutdated = version.get(0) < 1 ||
                                 (version.get(0) == 1 && version.get(1) < 6) ||
                                 (version.get(0) == 1 && version.get(1) == 6 && version.get(2) < 0);
        } else if (vendorId == VENDOR_TREZOR_V2) {
            isFirmwareOutdated = version.get(0) < 2 ||
                                 (version.get(0) == 2 && version.get(1) < 0) ||
                                 (version.get(0) == 2 && version.get(1) == 0 && version.get(2) < 7);
        }

        if (!isFirmwareOutdated) {
            onTrezorConnected(t);
            return true;
        }

        showFirmwareOutdated(R.string.id_outdated_hardware_wallet,
                             new Runnable() { public void run() { onTrezorConnected(t); } });
        return true;
    }

    private void onTrezorConnected(final Trezor t) {

        Log.d(TAG, "Creating Trezor HW wallet");
        mHwWallet = new TrezorHWWallet(t, mService.getNetwork());

        mHwDeviceData = new HWDeviceData("Trezor", false, false);
        mHwResolver = new HardwareCodeResolver(mHwWallet);

        if (mService.getConnectionManager().isConnected()) {
            doLogin(this);
        } else {
            Log.e(TAG, "Not connected yet, will be called back by the update method");
        }
    }

    private void showPinDialog() {
        mPinDialog = UI.dismiss(this, mPinDialog);

        final View v = UI.inflateDialog(this, R.layout.dialog_btchip_pin);

        mPinDialog = UI.popup(this, R.string.id_pin)
                     .customView(v, true)
                     .onPositive(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(final MaterialDialog dialog, final DialogAction which) {
                mPin = UI.getText(v, R.id.btchipPINValue);
                mService.getExecutor().submit(new Callable<Void>() {
                    @Override
                    public Void call() { onLedger(false); return null; }
                });
            }
        })
                     .onNegative(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(final MaterialDialog dialog, final DialogAction which) {
                toast(R.string.id_no_pin_provided_exiting);
                finish();
            }
        }).build();

        UI.mapEnterToPositive(mPinDialog, R.id.btchipPINValue);
        UI.showDialog(mPinDialog);
    }

    private void onLedger(final boolean hasScreen) {
        showInstructions(R.string.id_logging_in);
        final String pin = mPin;
        mPin = null;

        BTChipTransport transport = null;
        if (mUsb != null) {
            try {
                transport = BTChipTransportAndroid.open(mUsbManager, mUsb);
            } catch (final Exception e) {
                e.printStackTrace();
            }
            if (transport == null) {
                showInstructions(R.string.id_please_reconnect_your_hardware);
                return;
            }
        } else if ((transport = getTransport(mTag)) == null) {
            showInstructions(R.string.id_please_follow_the_instructions);

            // Prompt the user to tap
            runOnUiThread(new Runnable() {
                public void run() {
                    mNfcWaitDialog = new MaterialDialog.Builder(RequestLoginActivity.this)
                                     .title(R.string.btchip).content(R.string.id_please_tap_card).build();
                    mNfcWaitDialog.show();
                }
            });
            return;
        }

        transport.setDebug(BuildConfig.DEBUG);
        try {
            final BTChipDongle dongle = new BTChipDongle(transport, hasScreen);
            final BTChipFirmware fw = dongle.getFirmwareVersion();
            final int major = fw.getMajor(), minor = fw.getMinor(), patch = fw.getPatch();

            Log.d(TAG, "BTChip/Ledger firmware version " + fw.toString() + '(' +
                  major + '.' + minor + '.' + patch + ')');

            boolean isFirmwareOutdated = false;
            if (mVendorId == VENDOR_BTCHIP) {
                isFirmwareOutdated = major < 0x2001 ||
                                     (major == 0x2001 && minor < 0) || // Just for consistency in checking code
                                     (major == 0x2001 && minor == 0 && patch < 4);
            } else if (mVendorId == VENDOR_LEDGER) {
                isFirmwareOutdated = major < 0x3001 ||
                                     (major == 0x3001 && minor < 2) ||
                                     (major == 0x3001 && minor == 2 && patch < 5);
            }

            if (!isFirmwareOutdated) {
                onLedgerConnected(dongle, pin);
                return;
            }

            showFirmwareOutdated(R.string.id_outdated_hardware_wallet,
                                 new Runnable() { public void run() { onLedgerConnected(dongle, pin); } });
        } catch (final BTChipException e) {
            if (e.getSW() != BTChipConstants.SW_INS_NOT_SUPPORTED)
                e.printStackTrace();
            try {
                transport.close();
            } catch (final BTChipException bte) {}
            // We are in dashboard mode, prompt the user to open the btcoin app.
            mUsb = null;
            mInLedgerDashboard = true;
            showInstructions(R.string.id_ledger_dashboard_detected);
        }
    }

    private void onLedgerConnected(final BTChipDongle dongle, final String pin) {
        final SettableFuture<Integer> pinCB = SettableFuture.create();

        final boolean havePin = !TextUtils.isEmpty(pin);
        Log.d(TAG, "Creating Ledger HW wallet" + (havePin ? " with PIN" : ""));
        mHwWallet = new BTChipHWWallet(dongle, havePin ? pin : null, pinCB, mService.getNetwork());

        mHwDeviceData = new HWDeviceData("Ledger", false, true);
        mHwResolver = new HardwareCodeResolver(mHwWallet);

        if (mService.getConnectionManager().isConnected()) {
            doLogin(this);
        } else {
            Log.e(TAG, "Not connected yet, will be called back by the update method");
        }
    }

    private void doLogin(final Activity parent) {
        final ConnectionManager cm = mService.getConnectionManager();
        cm.addObserver(this);
        mService.getExecutor().execute(() -> {
            try {
                // FIXME: Dont register up front, only do it if login fails
                mService.getSession().registerUser(this, mHwDeviceData, "").resolve(null, mHwResolver);
                cm.login(parent, mHwDeviceData, mHwResolver);
            } catch (final Exception e) {
                e.printStackTrace();
                onLoginFailure();
            }
        });
    }

    @Override
    public void update(Observable observable, Object o) {
        super.update(observable, o);
        if (observable instanceof ConnectionManager) {
            final ConnectionManager cm = (ConnectionManager) observable;
            if (cm.isConnected()) {
                doLogin(this);
            }
        }
    }

    @Override
    protected void onLoginSuccess() {
        stopLoading();
        onLoggedIn();
    }

    @Override
    protected void onLoginFailure() {
        stopLoading();
        runOnUiThread(() -> {
            toast("Error logging in with Hardware Wallet");
            showInstructions(R.string.id_please_reconnect_your_hardware);
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mPinDialog = UI.dismiss(this, mPinDialog);
    }

    @Override
    public void onResumeWithService() {
        super.onResumeWithService();

        mTagDispatcher = TagDispatcher.get(this, this);
        mTagDispatcher.enableExclusiveNfc();

        final Intent intent = getIntent();
        mTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

        if (ACTION_USB_ATTACHED.equalsIgnoreCase(intent.getAction())) {
            // A new USB device was plugged in
            if (!mService.getConnectionManager().isConnected()) {
                // The user previously manually logged out, connect again
                mService.getConnectionManager().connect();
            }
            onUsbAttach(intent.getParcelableExtra(UsbManager.EXTRA_DEVICE));
        } else {
            if (mTag != null && NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction())) {
                onUsbAttach(intent.getParcelableExtra(UsbManager.EXTRA_DEVICE));
            }
        }

        if (mUsb != null || mInLedgerDashboard) {
            // Continue displaying instructions until the user opens the
            // correct wallet app, or log in completes/errors out
            return;
        }

        // No hardware wallet, jump to PIN or mnemonic entry
        if (mService.cfgPin().getString("ident", null) != null)
            startActivityForResult(new Intent(this, PinActivity.class), 0);
        else
            startActivityForResult(new Intent(this, MnemonicActivity.class), 0);
    }

    @Override
    public void onPauseWithService() {
        super.onPauseWithService();
        mTagDispatcher.disableExclusiveNfc();
    }

    private BTChipTransport getTransport(final Tag t) {
        BTChipTransport transport = null;
        if (t != null) {
            AndroidCard card = null;
            Log.d(TAG, "Start checking NFC transport");
            try {
                card = AndroidCard.get(t);
                transport = new BTChipTransportAndroidNFC(card);
                transport.setDebug(BuildConfig.DEBUG);
                transport.exchange(DUMMY_COMMAND).get();
                Log.d(TAG, "NFC transport checked");
            }catch (final Exception e)  {
                Log.d(TAG, "Tag was lost", e);
                if (card != null) {
                    try {
                        transport.close();
                    } catch (final Exception e1) {}
                    transport = null;
                }
            }
        }
        return transport;
    }

    @Override
    public void tagDiscovered(final Tag t) {
        Log.d(TAG, "tagDiscovered " + t);
        mTag = t;
        if (mTransportFuture == null)
            return;

        final BTChipTransport transport = getTransport(t);
        if (transport == null)
            return;

        if (mTransportFuture.set(transport)) {
            if (mNfcWaitDialog == null)
                return;

            runOnUiThread(new Runnable() { public void run() { mNfcWaitDialog.hide(); } });
        }
    }

    private void showInstructions(final int resId) {
        runOnUiThread(new Runnable() {
            public void run() {
                mInstructionsText.setText(resId);
                UI.show(mInstructionsText);
            }
        });
    }

    private void showFirmwareOutdated(final int resId, final Runnable onContinue) {
        // FIXME: Close and set mUsb to null for ledger in onNegative/onCancel

        if (!BuildConfig.DEBUG) {
            // Only allow the user to skip firmware checks in debug builds.
            showInstructions(resId);
            return;
        }

        final Runnable closeCB = new Runnable() { public void run() { finishOnUiThread(); }
        };
        runOnUiThread(new Runnable() {
            public void run() {
                final MaterialDialog d;
                d = UI.popup(RequestLoginActivity.this, R.string.id_warning, R.string.id_continue, R.string.id_cancel)
                    .content(resId)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(final MaterialDialog dialog, final DialogAction which) {
                        onContinue.run();
                    }
                }).build();
                UI.setDialogCloseHandler(d, closeCB);
                d.show();
            }
        });
    }
}