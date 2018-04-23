package com.greenaddress.greenbits.ui;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.btchip.BTChipDongle;
import com.btchip.BTChipDongle.BTChipPublicKey;
import com.btchip.BTChipDongle.BTChipFirmware;
import com.btchip.BTChipException;
import com.btchip.comm.BTChipTransport;
import com.btchip.comm.android.BTChipTransportAndroid;
import com.btchip.comm.android.BTChipTransportAndroidNFC;
import com.btchip.utils.KeyUtils;
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.greenaddress.greenapi.LoginData;
import com.greenaddress.greenapi.LoginFailed;
import com.greenaddress.greenbits.wallets.BTChipHWWallet;
import com.greenaddress.greenbits.wallets.TrezorHWWallet;
import com.satoshilabs.trezor.Trezor;
import com.satoshilabs.trezor.TrezorGUICallback;

import org.bitcoinj.crypto.DeterministicKey;

import java.util.List;
import java.util.concurrent.ExecutionException;

import nordpol.android.AndroidCard;
import nordpol.android.OnDiscoveredTagListener;
import nordpol.android.TagDispatcher;

public class RequestLoginActivity extends LoginActivity implements OnDiscoveredTagListener {

    private static final String TAG = RequestLoginActivity.class.getSimpleName();
    private static final byte DUMMY_COMMAND[] = { (byte)0xE0, (byte)0xC4, (byte)0x00, (byte)0x00, (byte)0x00 };

    private Dialog mBTChipDialog;
    private BTChipHWWallet mHwWallet;
    private TagDispatcher mTagDispatcher;
    private Tag mTag;
    private SettableFuture<BTChipTransport> mTransportFuture;
    private MaterialDialog mNfcWaitDialog;
    private BTChipFirmware mFirmwareVersion = null;

    @Override
    protected int getMainViewId() { return R.layout.activity_first_login_requested; }

    @Override
    protected void onCreateWithService(final Bundle savedInstanceState) {}

    private boolean onTrezor() {
        final Trezor t;
        t = Trezor.getDevice(this, new TrezorGUICallback() {
            @Override
            public String pinMatrixRequest() {
                final SettableFuture<String> ret = SettableFuture.create();
                RequestLoginActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        final View v = getLayoutInflater().inflate(R.layout.dialog_trezor_pin, null, false);
                        final Button[] buttons = new Button[]{
                                // upside down
                                UI.find(v, R.id.trezorPinButton7),
                                UI.find(v, R.id.trezorPinButton8),
                                UI.find(v, R.id.trezorPinButton9),
                                UI.find(v, R.id.trezorPinButton4),
                                UI.find(v, R.id.trezorPinButton5),
                                UI.find(v, R.id.trezorPinButton6),
                                UI.find(v, R.id.trezorPinButton1),
                                UI.find(v, R.id.trezorPinButton2),
                                UI.find(v, R.id.trezorPinButton3)
                        };
                        final EditText pinValue = UI.find(v, R.id.trezorPinValue);
                        for (int i = 0; i < 9; ++i) {
                            final int ii = i;
                            buttons[i].setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(final View v) {
                                    pinValue.setText(UI.getText(pinValue) + (ii + 1));
                                    pinValue.setSelection(UI.getText(pinValue).length());
                                }
                            });
                        }
                        UI.popup(RequestLoginActivity.this, "Hardware Wallet PIN")
                                .customView(v, true)
                                .onPositive(new MaterialDialog.SingleButtonCallback() {
                                    @Override
                                    public void onClick(final MaterialDialog dialog, final DialogAction which) {
                                        ret.set(UI.getText(pinValue));
                                    }
                                }).build().show();
                    }
                });
                try {
                    return ret.get();
                } catch (final InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                    return "";
                }
            }

            @Override
            public String passphraseRequest() {
                final SettableFuture<String> ret = SettableFuture.create();
                RequestLoginActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        final View v = getLayoutInflater().inflate(R.layout.dialog_trezor_passphrase, null, false);
                        final EditText passphraseValue = UI.find(v, R.id.trezorPassphraseValue);
                        UI.popup(RequestLoginActivity.this, "Hardware Wallet passphrase")
                                .customView(v, true)
                                .onPositive(new MaterialDialog.SingleButtonCallback() {
                                    @Override
                                    public void onClick(final MaterialDialog dialog, final DialogAction which) {
                                        ret.set(UI.getText(passphraseValue));
                                    }
                                }).build().show();
                    }
                });
                try {
                    return ret.get();
                } catch (final InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                    return "";
                }
            }
        });

        if (t == null)
            return false;

        final int VENDOR_TREZOR = 21324;

        final List<Integer> version = t.getFirmwareVersion();
        boolean isFirmwareOutdated = false;
        if (t.getVendorId() == VENDOR_TREZOR) {
            isFirmwareOutdated = version.get(0) < 1 ||
                                 (version.get(0) == 1 && version.get(1) < 6) ||
                                 (version.get(0) == 1 && version.get(1) == 6 && version.get(2) < 0);
        }

        if (isFirmwareOutdated) {
            final TextView instructions = UI.find(this, R.id.first_login_instructions);
            instructions.setText(R.string.trezor_firmware_outdated);
            return true;
        }
        final TrezorHWWallet trezor = new TrezorHWWallet(t);

        Futures.addCallback(Futures.transform(mService.onConnected, new AsyncFunction<Void, LoginData>() {
            @Override
            public ListenableFuture<LoginData> apply(final Void input) throws Exception {
                return mService.login(trezor);
            }
        }), new FutureCallback<LoginData>() {
            @Override
            public void onSuccess(final LoginData result) {
                RequestLoginActivity.this.onLoginSuccess();
            }

            @Override
            public void onFailure(final Throwable t) {
                t.printStackTrace();
                if (Throwables.getRootCause(t) instanceof LoginFailed) {
                    // Attempt auto register
                    try {

                        final DeterministicKey hdkey = trezor.getPubKey();
                        Futures.addCallback(mService.signup(trezor, /*mnemonic*/ null, "HW", hdkey.getPubKey(), hdkey.getChainCode()),
                                new FutureCallback<LoginData>() {
                                    @Override
                                    public void onSuccess(final LoginData result) {
                                        RequestLoginActivity.this.onLoginSuccess();
                                    }

                                    @Override
                                    public void onFailure(final Throwable t) {
                                        t.printStackTrace();
                                        finishOnUiThread();
                                    }
                                });
                        return;
                    } catch (final Exception e) {
                        e.printStackTrace();
                    }
                }
                finishOnUiThread();
            }
        });
        return true;
    }

    private boolean checkLedgerFirmwareVersion(final BTChipFirmware firmwareVersion) {
        final int major = firmwareVersion.getMajor();
        final int minor = firmwareVersion.getMinor();
        final int patch = firmwareVersion.getPatch();

        Log.d(TAG, "BTChip/Ledger firmware version " + firmwareVersion.toString());

        boolean isFirmwareOutdated = false;
        if (major == 0x3001)
            isFirmwareOutdated = minor < 1 || (minor == 1 && patch < 9);
        else if (major == 0x2001)
            isFirmwareOutdated = minor == 0 && patch < 4;

        if (isFirmwareOutdated) {
            final TextView instructions = UI.find(this, R.id.first_login_instructions);
            runOnUiThread(new Runnable() {
                public void run() {
                    instructions.setText(R.string.ledger_firmware_outdated);
                    UI.show(instructions);
                }
            });
        }

        return !isFirmwareOutdated;
    }

    private void onLedger(final Intent intent) {
        final TextView instructions = UI.find(this, R.id.first_login_instructions);
        UI.clear(instructions);
        UI.hide(instructions);
        // not TREZOR/KeepKey/BWALLET/AvalonWallet, so must be BTChip
        if (mTag != null)
            showPinDialog(null);
        else {
            final UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            if (device != null)
                if (BTChipTransportAndroid.isLedgerWithScreen(device))
                    login(device);
                else
                    showPinDialog(device);
        }
    }

    private void onUsbDeviceDetected(final Intent intent) {
        if (!onTrezor())
            onLedger(intent);
    }

    private void login(final UsbDevice device) {

        Futures.addCallback(Futures.transform(mService.onConnected, new AsyncFunction<Void, LoginData>() {
                    @Override
                    public ListenableFuture<LoginData> apply(final Void nada) throws Exception {
                        final BTChipTransport transport;
                        if (device != null) {
                            final UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
                            transport = BTChipTransportAndroid.open(manager, device);
                            if (transport == null) {
                                if (mFirmwareVersion != null)
                                    checkLedgerFirmwareVersion(mFirmwareVersion);
                                return Futures.immediateFuture(null);
                            }
                        } else {
                            // If the tag was already tapped, work with it
                            transport = getTransport(mTag);
                            if (transport == null) {
                                // Prompt the user to tap
                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        mNfcWaitDialog = new MaterialDialog.Builder(RequestLoginActivity.this)
                                                .title("BTChip")
                                                .content("Please tap card")
                                                .build();
                                        mNfcWaitDialog.show();
                                    }
                                });
                                return Futures.immediateFuture(null);
                            }
                        }

                        transport.setDebug(BuildConfig.DEBUG);
                        try {
                            final BTChipDongle dongle = new BTChipDongle(transport, true);
                            final BTChipFirmware firmwareVersion = dongle.getFirmwareVersion();
                            if (!checkLedgerFirmwareVersion(firmwareVersion)) {
                                mFirmwareVersion = firmwareVersion;
                                return Futures.immediateFuture(null);
                            }
                        } catch (final BTChipException e) {
                            e.printStackTrace();
                            // we are in dashboard mode ignore usb
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    final TextView instructions = UI.find(RequestLoginActivity.this, R.id.first_login_instructions);
                                    instructions.setText(R.string.ledger_open_bitcoin_app);
                                    UI.show(instructions);
                                }
                            });
                            return Futures.immediateFuture(null);
                        }
                        mHwWallet = new BTChipHWWallet(transport);
                        final ProgressBar loginProgress = UI.find(RequestLoginActivity.this, R.id.signingLogin);
                        runOnUiThread(new Runnable() {
                            public void run() {
                                UI.show(loginProgress);
                            }
                        });
                        return mService.login(mHwWallet);
                    }

        }), mOnLoggedIn);
    }

    private void showPinDialog(final UsbDevice device) {
        final SettableFuture<String> pinFuture = SettableFuture.create();
        RequestLoginActivity.this.runOnUiThread(new Runnable() {
            public void run() {
                final View v = getLayoutInflater().inflate(R.layout.dialog_btchip_pin, null, false);
                final EditText pinValue = UI.find(v, R.id.btchipPINValue);
                final ProgressBar loginProgress = UI.find(RequestLoginActivity.this, R.id.signingLogin);
                mBTChipDialog = UI.popup(RequestLoginActivity.this, "BTChip PIN")
                        .customView(v, true)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(final MaterialDialog dialog, final DialogAction which) {
                                UI.show(loginProgress);
                                pinFuture.set(UI.getText(pinValue));
                            }
                        })
                        .onNegative(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(final MaterialDialog dialog, final DialogAction which) {
                                RequestLoginActivity.this.toast(R.string.err_request_login_no_pin);
                                RequestLoginActivity.this.finish();
                            }
                        }).build();

                pinValue.requestFocus();
                pinValue.setOnEditorActionListener(
                        UI.getListenerRunOnEnter(new Runnable() {
                            public void run() {
                                UI.show(loginProgress);
                                mBTChipDialog.hide();
                                pinFuture.set(UI.getText(pinValue));
                            }
                        })
                );
                UI.showDialog(mBTChipDialog);
            }
        });
        Futures.addCallback(Futures.transform(mService.onConnected, new AsyncFunction<Void, LoginData>() {
            @Override
            public ListenableFuture<LoginData> apply(final Void input) throws Exception {
                return Futures.transform(pinFuture, new AsyncFunction<String, LoginData>() {
                    @Override
                    public ListenableFuture<LoginData> apply(final String pin) throws Exception {

                        mTransportFuture = SettableFuture.create();
                        if (device != null) {
                            final UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
                            mTransportFuture.set(BTChipTransportAndroid.open(manager, device));
                        } else {
                            // If the tag was already tapped, work with it
                            final BTChipTransport transport = getTransport(mTag);
                            if (transport != null)
                                mTransportFuture.set(transport);
                            else {
                                // Prompt the user to tap
                                mNfcWaitDialog = new MaterialDialog.Builder(RequestLoginActivity.this)
                                        .title("BTChip")
                                        .content("Please tap card")
                                        .build();
                                mNfcWaitDialog.show();
                            }
                        }
                        return Futures.transform(mTransportFuture, new AsyncFunction<BTChipTransport, LoginData>() {
                            @Override
                            public ListenableFuture<LoginData> apply(final BTChipTransport transport) {
                                try {
                                    if (device != null) {
                                        final BTChipDongle dongle = new BTChipDongle(transport, true);
                                        final BTChipFirmware firmwareVersion = dongle.getFirmwareVersion();
                                        if (!checkLedgerFirmwareVersion(firmwareVersion)) {
                                            return Futures.immediateFuture(null);
                                        }
                                    }
                                } catch (final BTChipException e) {
                                    Log.d(TAG, "BTChip/Ledger firmware version check failed");
                                }
                                transport.setDebug(BuildConfig.DEBUG);
                                final SettableFuture<Integer> remainingAttemptsFuture = SettableFuture.create();
                                mHwWallet = new BTChipHWWallet(transport, pin, remainingAttemptsFuture);
                                return Futures.transform(remainingAttemptsFuture, new AsyncFunction<Integer, LoginData>() {
                                    @Override
                                    public ListenableFuture<LoginData> apply(final Integer remainingAttempts) {

                                        if (remainingAttempts == -1)
                                            return mService.login(mHwWallet); // -1 means success, so login

                                        final String msg;
                                        if (remainingAttempts > 0)
                                            msg = getString(R.string.btchipInvalidPIN, remainingAttempts);
                                        else
                                            msg = getString(R.string.btchipNotSetup);

                                        RequestLoginActivity.this.runOnUiThread(new Runnable() {
                                            public void run() {
                                                RequestLoginActivity.this.toast(msg);
                                                RequestLoginActivity.this.finish();
                                            }
                                        });
                                        return Futures.immediateFuture(null);
                                    }
                                });
                            }
                        });
                    }
                });
            }
        }), mOnLoggedIn);
    }

    private final FutureCallback<LoginData> mOnLoggedIn = new FutureCallback<LoginData>() {
        @Override
        public void onSuccess(final LoginData result) {
            if (result != null)
                RequestLoginActivity.this.onLoginSuccess();
        }

        @Override
        public void onFailure(final Throwable t) {
            t.printStackTrace();
            if (Throwables.getRootCause(t) instanceof LoginFailed) {
                // Attempt auto register
                try {
                    final BTChipPublicKey masterPublicKey = mHwWallet.getDongle().getWalletPublicKey("");
                    Futures.addCallback(mService.signup(mHwWallet, /*mnemonic*/ null, "HW", KeyUtils.compressPublicKey(masterPublicKey.getPublicKey()), masterPublicKey.getChainCode()),
                            new FutureCallback<LoginData>() {
                                @Override
                                public void onSuccess(final LoginData result) {
                                    RequestLoginActivity.this.onLoginSuccess();
                                }

                                @Override
                                public void onFailure(final Throwable t) {
                                    t.printStackTrace();
                                    finishOnUiThread();
                                }
                            });
                    return;
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            }
            finishOnUiThread();
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        mBTChipDialog = UI.dismiss(this, mBTChipDialog);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        setResult(resultCode, data);
        finish();
    }

    @Override
    public void onResumeWithService() {
        registerReceiver(mOnUsb, new IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED));

        mTag = getIntent().getParcelableExtra(NfcAdapter.EXTRA_TAG);
        mTagDispatcher = TagDispatcher.get(this, this);

        if (((mTag != null) && (NfcAdapter.ACTION_TECH_DISCOVERED.equals(getIntent().getAction()))) ||
                (getIntent().getAction() != null &&
                        getIntent().getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED))) {
            onUsbDeviceDetected(getIntent());
            return;
        }

        if (mService.cfg("pin").getString("ident", null) != null)
            startActivityForResult(new Intent(this, PinActivity.class), 0);
        else
            startActivityForResult(new Intent(this, MnemonicActivity.class), 0);

        mTagDispatcher.enableExclusiveNfc();
    }

    @Override
    public void onPauseWithService() {
        unregisterReceiver(mOnUsb);
        mTagDispatcher.disableExclusiveNfc();
    }

    private final BroadcastReceiver mOnUsb = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            onUsbDeviceDetected(intent);
        }
    };

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
            }
            catch (final Exception e) {
                Log.d(TAG, "Tag was lost", e);
                if (card != null) {
                    try {
                        transport.close();
                    }
                    catch (final Exception e1) {
                    }
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
}
