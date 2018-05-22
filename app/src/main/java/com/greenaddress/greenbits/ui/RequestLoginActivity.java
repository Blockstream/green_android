package com.greenaddress.greenbits.ui;

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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.btchip.BTChipConstants;
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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import nordpol.android.AndroidCard;
import nordpol.android.OnDiscoveredTagListener;
import nordpol.android.TagDispatcher;

public class RequestLoginActivity extends LoginActivity implements OnDiscoveredTagListener {

    private static final String TAG = RequestLoginActivity.class.getSimpleName();
    private static final byte DUMMY_COMMAND[] = { (byte)0xE0, (byte)0xC4, (byte)0x00, (byte)0x00, (byte)0x00 };

    private static final int VENDOR_BTCHIP = 0x2581;
    private static final int VENDOR_LEDGER = 0x2c97;
    private static final int VENDOR_TREZOR = 0x534c;

    private UsbManager mUsbManager;
    private UsbDevice mUsb;

    private TextView mInstructionsText;
    private ProgressBar mLoginProgress;
    private Dialog mPinDialog;
    private String mPin;
    private Integer mVendorId;


    private BTChipHWWallet mHwWallet;
    private TagDispatcher mTagDispatcher;
    private Tag mTag;
    private SettableFuture<BTChipTransport> mTransportFuture;
    private MaterialDialog mNfcWaitDialog;

    @Override
    protected int getMainViewId() { return R.layout.activity_first_login_requested; }

    @Override
    protected void onCreateWithService(final Bundle savedInstanceState) {
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        mInstructionsText = UI.find(this, R.id.first_login_instructions);
        mLoginProgress = UI.find(this, R.id.signingLogin);

        final Intent intent = getIntent();
        if (intent != null && ACTION_USB_ATTACHED.equalsIgnoreCase(intent.getAction())) {
            // A new USB device was plugged in and the app wasn't running
            onUsbAttach((UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE));
        }
    }

    @Override
    protected void onNewIntent(final Intent intent) {
        Log.d(TAG, "onNewIntent");
        setIntent(intent);
        if (ACTION_USB_ATTACHED.equalsIgnoreCase(intent.getAction())) {
            // A new USB device was plugged in
            onUsbAttach((UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE));
        }
    }

    protected void onUsbAttach(final UsbDevice usb) {
        Log.d(TAG, "onUsbAttach");
        mUsb = usb;
        if (usb == null)
            return;

        mVendorId = usb.getVendorId();
        Log.d(TAG, "Vendor: " + mVendorId + " Product: " + usb.getProductId());

        if (mVendorId == VENDOR_TREZOR) {
            onTrezor();
        } else if (mVendorId == VENDOR_BTCHIP || mVendorId == VENDOR_LEDGER) {
            if (BTChipTransportAndroid.isLedgerWithScreen(usb)) {
                // User entered PIN on-device
                setupLedgerConnection();
            } else {
                // Prompt for PIN to unlock device before setting it up
                runOnUiThread(new Runnable() { public void run() { showPinDialog(); }});
            }
        }
    }

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

        final List<Integer> version = t.getFirmwareVersion();
        boolean isFirmwareOutdated = false;
        if (t.getVendorId() == VENDOR_TREZOR) {
            isFirmwareOutdated = version.get(0) < 1 ||
                                 (version.get(0) == 1 && version.get(1) < 6) ||
                                 (version.get(0) == 1 && version.get(1) == 6 && version.get(2) < 0);
        }

        if (!isFirmwareOutdated) {
            onTrezorConnected(t);
            return true;
        }

        showFirmwareOutdated(R.string.trezor_firmware_outdated,
                             new Runnable() { public void run() { onTrezorConnected(t); } });
        return true;
    }

    private void onTrezorConnected(final Trezor t) {
        final TrezorHWWallet trezor = new TrezorHWWallet(t);

        Futures.addCallback(Futures.transformAsync(mService.onConnected, new AsyncFunction<Void, LoginData>() {
            @Override
            public ListenableFuture<LoginData> apply(final Void input) {
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
    }

    private void showPinDialog() {
        mPinDialog = UI.dismiss(this, mPinDialog);

        final View v = getLayoutInflater().inflate(R.layout.dialog_btchip_pin, null, false);

        mPinDialog = UI.popup(this, R.string.pinTitleText)
            .customView(v, true)
            .onPositive(new MaterialDialog.SingleButtonCallback() {
                @Override
                public void onClick(final MaterialDialog dialog, final DialogAction which) {
                    UI.show(UI.find(RequestLoginActivity.this, R.id.signingLogin));
                    mPin = UI.getText(v, R.id.btchipPINValue);
                    mService.getExecutor().submit(new Callable<Void>() {
                        @Override
                        public Void call() { setupLedgerConnection(); return null; }
                    });
                }
            })
            .onNegative(new MaterialDialog.SingleButtonCallback() {
                @Override
                public void onClick(final MaterialDialog dialog, final DialogAction which) {
                    toast(R.string.err_request_login_no_pin);
                    finish();
                }
            }).build();

        UI.mapEnterToPositive(mPinDialog, R.id.btchipPINValue);
        UI.showDialog(mPinDialog);
    }

    private void setupLedgerConnection() {
        showInstructions(R.string.logging_in);
        final String pin = mPin;
        mPin = null;

        final BTChipTransport transport;
        if (mUsb != null) {
            transport = BTChipTransportAndroid.open(mUsbManager, mUsb);
            if (transport == null) {
                showInstructions(R.string.hw_wallet_reconnect);
                return;
            }
        } else if ((transport = getTransport(mTag)) == null) {
            showInstructions(R.string.hw_wallet_headline);

            // Prompt the user to tap
            runOnUiThread(new Runnable() {
                public void run() {
                    mNfcWaitDialog = new MaterialDialog.Builder(RequestLoginActivity.this)
                        .title(R.string.btchip).content(R.string.please_tap_card).build();
                    mNfcWaitDialog.show();
                }
            });
            return;
        }

        transport.setDebug(BuildConfig.DEBUG);
        try {
            final BTChipFirmware fw = (new BTChipDongle(transport, true)).getFirmwareVersion();
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
                onLedgerConnected(transport, pin);
                return;
            }

            showFirmwareOutdated(R.string.ledger_firmware_outdated,
                                 new Runnable() { public void run() { onLedgerConnected(transport, pin); } });
        } catch (final BTChipException e) {
            if (e.getSW() != BTChipConstants.SW_INS_NOT_SUPPORTED)
                e.printStackTrace();
            // We are in dashboard mode, prompt the user to open the btcoin app.
            showInstructions(R.string.ledger_open_bitcoin_app);
        }
    }

    private void onLedgerConnected(final BTChipTransport transport, final String pin) {
        runOnUiThread(new Runnable() { public void run() { UI.show(mLoginProgress); } });

        final SettableFuture<Integer> pinCB = SettableFuture.create();

        final boolean havePin = !TextUtils.isEmpty(pin);
        Log.d(TAG, "Creating HW wallet" + (havePin ? " with PIN" : ""));
        if (havePin)
            mHwWallet = new BTChipHWWallet(transport, pin, pinCB);
        else
            mHwWallet = new BTChipHWWallet(transport);

        // Try to log in once we are connected
        Futures.addCallback(Futures.transformAsync(mService.onConnected, new AsyncFunction<Void, LoginData>() {
            @Override
            public ListenableFuture<LoginData> apply(final Void input) {
                if (!havePin)
                    return mService.login(mHwWallet); // Login directly

                // Otherwise, log in once the users PIN is correct
                return Futures.transformAsync(pinCB, new AsyncFunction<Integer, LoginData>() {
                    @Override
                    public ListenableFuture<LoginData> apply(final Integer remainingAttempts) {
                        if (remainingAttempts == -1)
                            return mService.login(mHwWallet); // -1 means success, so login

                        final String msg;
                        if (remainingAttempts > 0)
                            msg = getString(R.string.btchipInvalidPIN, remainingAttempts);
                        else
                            msg = getString(R.string.btchipNotSetup);

                        runOnUiThread(new Runnable() {
                            public void run() {
                                toast(msg);
                                finish();
                            }
                        });
                        return Futures.immediateFuture(null);
                    }
                });
            }
        }), mOnLoggedIn);
    }

    private final FutureCallback<LoginData> mOnLoggedIn = new FutureCallback<LoginData>() {
        @Override
        public void onSuccess(final LoginData result) {
            if (result != null)
                onLoginSuccess();
        }

        @Override
        public void onFailure(final Throwable t) {
            t.printStackTrace();
            if (Throwables.getRootCause(t) instanceof LoginFailed) {
                try {
                    autoRegister(); // Attempt to auto register the user
                    return;
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            }
            finishOnUiThread();
        }
    };

    private void autoRegister() throws BTChipException {
        showInstructions(R.string.hw_wallet_registering);

        final BTChipPublicKey hdkey = mHwWallet.getDongle().getWalletPublicKey("");
        final byte[] pubkey = KeyUtils.compressPublicKey(hdkey.getPublicKey());
        Futures.addCallback(mService.signup(mHwWallet, null, "HW", pubkey, hdkey.getChainCode()),
            new FutureCallback<LoginData>() {
                @Override
                public void onSuccess(final LoginData result) {
                    onLoginSuccess();
                }

                @Override
                public void onFailure(final Throwable t) {
                    t.printStackTrace();
                    finishOnUiThread();
                }
            });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mPinDialog = UI.dismiss(this, mPinDialog);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        setResult(resultCode, data);
        finish();
    }

    @Override
    public void onResumeWithService() {
        if (mService.isLoggedOrLoggingIn() && mService.getSigningWallet() != null) {
            // Already logged in, could be from different app via intent.
            // FIXME: also call if trezor equal to current service wallet
            if (mHwWallet == null || mService.getSigningWallet().equals(mHwWallet))
                onLoginSuccess();
        }

        mTag = getIntent().getParcelableExtra(NfcAdapter.EXTRA_TAG);
        mTagDispatcher = TagDispatcher.get(this, this);

        if (mTag != null && NfcAdapter.ACTION_TECH_DISCOVERED.equals(getIntent().getAction())) {
            // FIXME: Can this be done in onCreate/onNewIntent?
            onUsbAttach((UsbDevice) getIntent().getParcelableExtra(UsbManager.EXTRA_DEVICE));
            return;
        }

        if (mUsb == null) {
            // No hardware wallet, jump to PIN or mnemonic entry
            if (mService.cfg("pin").getString("ident", null) != null)
                startActivityForResult(new Intent(this, PinActivity.class), 0);
            else
                startActivityForResult(new Intent(this, MnemonicActivity.class), 0);
        }

        mTagDispatcher.enableExclusiveNfc();
    }

    @Override
    public void onPauseWithService() {
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

        final Runnable closeCB = new Runnable() { public void run() { finishOnUiThread(); } };
        runOnUiThread(new Runnable() {
            public void run() {
                final MaterialDialog d;
                d = UI.popup(RequestLoginActivity.this, R.string.warning, R.string.continueText, R.string.cancel)
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
