package com.greenaddress.greenbits.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
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
import com.btchip.BTChipDongle.BTChipPublicKey;
import com.btchip.comm.BTChipTransport;
import com.btchip.comm.android.BTChipTransportAndroid;
import com.btchip.comm.android.BTChipTransportAndroidNFC;
import com.btchip.utils.KeyUtils;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.greenaddress.greenapi.LoginData;
import com.greenaddress.greenapi.LoginFailed;
import com.greenaddress.greenbits.GaService;
import com.greenaddress.greenbits.wallets.BTChipHWWallet;
import com.greenaddress.greenbits.wallets.TrezorHWWallet;
import com.satoshilabs.trezor.Trezor;
import com.satoshilabs.trezor.TrezorGUICallback;

import java.util.List;
import java.util.concurrent.ExecutionException;

import nordpol.android.AndroidCard;
import nordpol.android.OnDiscoveredTagListener;
import nordpol.android.TagDispatcher;

public class RequestLoginActivity extends GaActivity implements OnDiscoveredTagListener {

    private static final String TAG = RequestLoginActivity.class.getSimpleName();
    private static final byte DUMMY_COMMAND[] = { (byte)0xE0, (byte)0xC4, (byte)0x00, (byte)0x00, (byte)0x00 };

    private Dialog mBTChipDialog = null;
    private BTChipHWWallet hwWallet = null;
    private TagDispatcher tagDispatcher;
    private Tag tag;
    private SettableFuture<BTChipTransport> transportFuture;
    private MaterialDialog nfcWaitDialog;

    @Override
    protected int getMainViewId() { return R.layout.activity_first_login_requested; }

    @Override
    protected void onCreateWithService(final Bundle savedInstanceState) {

        final GaService service = mService;
        tag = getIntent().getParcelableExtra(NfcAdapter.EXTRA_TAG);
        tagDispatcher = TagDispatcher.get(this, this);

        if (((tag != null) && (NfcAdapter.ACTION_TECH_DISCOVERED.equals(getIntent().getAction()))) ||
                (getIntent().getAction() != null &&
                        getIntent().getAction().equals("android.hardware.usb.action.USB_DEVICE_ATTACHED"))) {
            final Trezor t;
            if (tag != null) {
                t = null;
            } else {
                t = Trezor.getDevice(this, new TrezorGUICallback() {
                    @Override
                    public String pinMatrixRequest() {
                        final SettableFuture<String> ret = SettableFuture.create();
                        RequestLoginActivity.this.runOnUiThread(new Runnable() {
                            @Override
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
                                        public void onClick(View v) {
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
                            @Override
                            public void run() {
                                final View v = getLayoutInflater().inflate(R.layout.dialog_trezor_passphrase, null, false);
                                final EditText passphraseValue = UI.find(v, R.id.trezorPassphraseValue);
                                UI.popup(RequestLoginActivity.this, "Hardware Wallet passphrase")
                                    .customView(v, true)
                                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                                        @Override
                                        public void onClick(MaterialDialog dialog, DialogAction which) {
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
            }
            if (t != null) {
                final List<Integer> version = t.getFirmwareVersion();
                if (t.getVendorId() == 21324 &&
                    (version.get(0) < 1 || (version.get(0) == 1 && version.get(1) < 3))) {
                    final TextView instructions = UI.find(this, R.id.firstLoginRequestedInstructionsText);
                    instructions.setText(R.string.firstLoginRequestedInstructionsOldTrezor);
                    return;
                }
                if (t.getVendorId() == 11044 && (version.get(0) < 1)) {
                    final TextView instructions = UI.find(this, R.id.firstLoginRequestedInstructionsText);
                    instructions.setText(R.string.firstLoginRequestedInstructionsOldTrezor);
                    return;
                }

                Futures.addCallback(Futures.transform(service.onConnected, new AsyncFunction<Void, LoginData>() {
                    @Override
                    public ListenableFuture<LoginData> apply(final Void input) throws Exception {
                        return service.login(new TrezorHWWallet(t));
                    }
                }), new FutureCallback<LoginData>() {
                    @Override
                    public void onSuccess(final LoginData result) {
                        startActivity(new Intent(RequestLoginActivity.this, TabbedMainActivity.class));
                        finishOnUiThread();
                    }

                    @Override
                    public void onFailure(final Throwable t) {
                        if (t instanceof LoginFailed) {
                            // login failed - most likely TREZOR/KeepKey/BWALLET/AvalonWallet not paired
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    new MaterialDialog.Builder(RequestLoginActivity.this)
                                            .title(R.string.trezor_login_failed)
                                            .content(R.string.trezor_login_failed_details)
                                            .build().show();
                                }
                            });
                        } else {
                            finishOnUiThread();
                        }
                    }
                });
            } else {
                final TextView edit = UI.find(this, R.id.firstLoginRequestedInstructionsText);
                UI.hide(edit);
                // not TREZOR/KeepKey/BWALLET/AvalonWallet, so must be BTChip
                if (tag != null) {
                    showPinDialog();
                } else {
                    final UsbDevice device = getIntent().getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (device != null) {
                        showPinDialog(device);
                    }
                }
            }
            return;
        }

        if (service.cfg("pin").getString("ident", null) != null) {
            final Intent pin = new Intent(this, PinActivity.class);
            startActivityForResult(pin, 0);
        } else {
            final Intent mnemonic = new Intent(this, MnemonicActivity.class);
            startActivityForResult(mnemonic, 0);
        }
    }

    private void showPinDialog() {
        showPinDialog(null);
    }

    private void showPinDialog(final UsbDevice device) {
        final GaService service = mService;
        final SettableFuture<String> pinFuture = SettableFuture.create();
        RequestLoginActivity.this.runOnUiThread(new Runnable() {
            @Override
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
                            @Override
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
        Futures.addCallback(Futures.transform(service.onConnected, new AsyncFunction<Void, LoginData>() {
            @Override
            public ListenableFuture<LoginData> apply(final Void input) throws Exception {
                return Futures.transform(pinFuture, new AsyncFunction<String, LoginData>() {
                    @Override
                    public ListenableFuture<LoginData> apply(final String pin) throws Exception {

                        transportFuture = SettableFuture.create();
                        if (device != null) {
                            final UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
                            transportFuture.set(BTChipTransportAndroid.open(manager, device));
                        } else {
                            // If the tag was already tapped, work with it
                            final BTChipTransport transport = getTransport(tag);
                            if (transport != null) {
                                transportFuture.set(transport);
                            } else {
                                // Prompt the user to tap
                                nfcWaitDialog = new MaterialDialog.Builder(RequestLoginActivity.this)
                                        .title("BTChip")
                                        .content("Please tap card")
                                        .build();
                                nfcWaitDialog.show();
                            }
                        }
                        return Futures.transform(transportFuture, new AsyncFunction<BTChipTransport, LoginData>() {
                            @Override
                            public ListenableFuture<LoginData> apply(final BTChipTransport transport) {
                                final SettableFuture<Integer> remainingAttemptsFuture = SettableFuture.create();
                                hwWallet = new BTChipHWWallet(transport, RequestLoginActivity.this, pin, remainingAttemptsFuture);
                                return Futures.transform(remainingAttemptsFuture, new AsyncFunction<Integer, LoginData>() {
                                    @Override
                                    public ListenableFuture<LoginData> apply(final Integer remainingAttempts) {

                                        if (remainingAttempts == -1)
                                            return service.login(hwWallet); // -1 means success, so login

                                        final String msg;
                                        if (remainingAttempts > 0)
                                            msg = getString(R.string.btchipInvalidPIN, remainingAttempts);
                                        else
                                            msg = getString(R.string.btchipNotSetup);

                                        RequestLoginActivity.this.runOnUiThread(new Runnable() {
                                            @Override
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
        }), new FutureCallback<LoginData>() {
            @Override
            public void onSuccess(final LoginData result) {
                if (result != null) {
                    startActivity(new Intent(RequestLoginActivity.this, TabbedMainActivity.class));
                    finishOnUiThread();
                }
            }

            @Override
            public void onFailure(final Throwable t) {
                t.printStackTrace();
                if (t instanceof LoginFailed) {
                    // Attempt auto register
                    try {
                        final BTChipPublicKey masterPublicKey = hwWallet.getDongle().getWalletPublicKey("");
                        final BTChipPublicKey loginPublicKey = hwWallet.getDongle().getWalletPublicKey("18241'");
                        Futures.addCallback(service.signup(hwWallet, KeyUtils.compressPublicKey(masterPublicKey.getPublicKey()), masterPublicKey.getChainCode(), KeyUtils.compressPublicKey(loginPublicKey.getPublicKey()), loginPublicKey.getChainCode()),
                                new FutureCallback<LoginData>() {
                                    @Override
                                    public void onSuccess(final LoginData result) {
                                        startActivity(new Intent(RequestLoginActivity.this, TabbedMainActivity.class));
                                        finishOnUiThread();
                                    }

                                    @Override
                                    public void onFailure(final Throwable t) {
                                        t.printStackTrace();
                                        finishOnUiThread();
                                    }
                                });
                    } catch (final Exception e) {
                        e.printStackTrace();
                        finishOnUiThread();
                    }
                } else {
                    finishOnUiThread();
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mBTChipDialog != null)
            mBTChipDialog.dismiss();
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        setResult(resultCode, data);
        finish();
    }

    @Override
    public void onResumeWithService() {
        tagDispatcher.enableExclusiveNfc();
    }

    @Override
    public void onPauseWithService() {
        tagDispatcher.disableExclusiveNfc();
    }

    private BTChipTransport getTransport(final Tag t) {
        BTChipTransport transport = null;
        if (t != null) {
            AndroidCard card = null;
            Log.d(TAG, "Start checking NFC transport");
            try {
                card = AndroidCard.get(t);
                transport = new BTChipTransportAndroidNFC(card);
                transport.setDebug(true);
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
    public void tagDiscovered(Tag t) {
        Log.d(TAG, "tagDiscovered " + t);
        this.tag = t;
        if (transportFuture == null)
            return;

        BTChipTransport transport = getTransport(t);
        if (transport == null)
            return;

        if (transportFuture.set(transport)) {
            if (nfcWaitDialog == null)
                return;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    nfcWaitDialog.hide();
                }
            });
        }
    }
}
