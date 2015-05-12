package com.greenaddress.greenbits.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.btchip.comm.BTChipTransport;
import com.btchip.comm.android.BTChipTransportAndroid;
import com.btchip.comm.android.BTChipTransportAndroidNFC;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.greenaddress.greenapi.LoginData;
import com.greenaddress.greenapi.LoginFailed;
import com.greenaddress.greenbits.GaService;
import com.greenaddress.greenbits.GreenAddressApplication;
import com.satoshilabs.trezor.Trezor;
import com.satoshilabs.trezor.TrezorGUICallback;

import java.util.Formatter;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ExecutionException;

import javax.annotation.Nullable;

public class RequestLoginActivity extends Activity implements Observer {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first_login_requested);

        getGAApp().getConnectionObservable().addObserver(this);

        Tag tag = (Tag) getIntent().getParcelableExtra(NfcAdapter.EXTRA_TAG);

        if (((tag != null) && (NfcAdapter.ACTION_TECH_DISCOVERED.equals(getIntent().getAction()))) ||
                (getIntent().getAction() != null &&
                 getIntent().getAction().equals("android.hardware.usb.action.USB_DEVICE_ATTACHED"))) {
            final Trezor t;
            if (tag != null) {
                t = null;
            } else {
                t = Trezor.getDevice(this, new TrezorGUICallback() {
                    @Override
                    public String PinMatrixRequest() {
                        final SettableFuture<String> ret = SettableFuture.create();
                        RequestLoginActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                final View inflatedLayout = getLayoutInflater().inflate(R.layout.dialog_trezor_pin, null, false);
                                final Button[] buttons = new Button[]{
                                        // upside down
                                        (Button) inflatedLayout.findViewById(R.id.trezorPinButton7),
                                        (Button) inflatedLayout.findViewById(R.id.trezorPinButton8),
                                        (Button) inflatedLayout.findViewById(R.id.trezorPinButton9),
                                        (Button) inflatedLayout.findViewById(R.id.trezorPinButton4),
                                        (Button) inflatedLayout.findViewById(R.id.trezorPinButton5),
                                        (Button) inflatedLayout.findViewById(R.id.trezorPinButton6),
                                        (Button) inflatedLayout.findViewById(R.id.trezorPinButton1),
                                        (Button) inflatedLayout.findViewById(R.id.trezorPinButton2),
                                        (Button) inflatedLayout.findViewById(R.id.trezorPinButton3)
                                };
                                final EditText pinValue = (EditText) inflatedLayout.findViewById(R.id.trezorPinValue);
                                for (int i = 0; i < 9; ++i) {
                                    final int ii = i;
                                    buttons[i].setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            pinValue.setText(pinValue.getText().toString() + (ii + 1));
                                            pinValue.setSelection(pinValue.getText().toString().length());
                                        }
                                    });
                                }
                                new MaterialDialog.Builder(RequestLoginActivity.this)
                                        .title("TREZOR PIN")
                                        .customView(inflatedLayout, true)
                                        .positiveText("OK")
                                        .negativeText("CANCEL")
                                        .positiveColorRes(R.color.accent)
                                        .negativeColorRes(R.color.accent)
                                        .titleColorRes(R.color.white)
                                        .contentColorRes(android.R.color.white)
                                        .theme(Theme.DARK)
                                        .callback(new MaterialDialog.ButtonCallback() {
                                            @Override
                                            public void onPositive(MaterialDialog materialDialog) {
                                                ret.set(pinValue.getText().toString());
                                            }
                                        })
                                        .build().show();
                            }
                        });
                        try {
                            return ret.get();
                        } catch (InterruptedException | ExecutionException e) {
                            e.printStackTrace();
                            return "";
                        }
                    }

                    @Override
                    public String PassphraseRequest() {
                        final SettableFuture<String> ret = SettableFuture.create();
                        RequestLoginActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                final View inflatedLayout = getLayoutInflater().inflate(R.layout.dialog_trezor_passphrase, null, false);
                                final EditText passphraseValue = (EditText) inflatedLayout.findViewById(R.id.trezorPassphraseValue);
                                new MaterialDialog.Builder(RequestLoginActivity.this)
                                        .title("TREZOR passphrase")
                                        .customView(inflatedLayout, true)
                                        .positiveText("OK")
                                        .negativeText("CANCEL")
                                        .positiveColorRes(R.color.accent)
                                        .negativeColorRes(R.color.accent)
                                        .titleColorRes(R.color.white)
                                        .contentColorRes(android.R.color.white)
                                        .theme(Theme.DARK)
                                        .callback(new MaterialDialog.ButtonCallback() {
                                            @Override
                                            public void onPositive(MaterialDialog materialDialog) {
                                                ret.set(passphraseValue.getText().toString());
                                            }
                                        })
                                        .build().show();
                            }
                        });
                        try {
                            return ret.get();
                        } catch (InterruptedException | ExecutionException e) {
                            e.printStackTrace();
                            return "";
                        }
                    }
                });
            }
            if (t != null) {
                final List<Integer> version = t.getFirmwareVersion();
                if (version.get(0) < 1 ||
                    (version.get(0) == 1) && (version.get(1) < 3)) {
                    final TextView instructions = (TextView)findViewById(R.id.firstLoginRequestedInstructionsText);
                    instructions.setText(getResources().getString(R.string.firstLoginRequestedInstructionsOldTrezor));
                    return;
                }
                Futures.addCallback(getGAApp().onServiceConnected, new FutureCallback<Void>() {
                    @Override
                    public void onSuccess(@Nullable Void result) {
                        final GaService gaService = getGAService();

                        Futures.addCallback(Futures.transform(gaService.onConnected, new AsyncFunction<Void, LoginData>() {
                            @Override
                            public ListenableFuture<LoginData> apply(final Void input) throws Exception {
                                return gaService.login(new TrezorHWWallet(t));
                            }
                        }), new FutureCallback<LoginData>() {
                            @Override
                            public void onSuccess(@Nullable final LoginData result) {
                                final Intent main = new Intent(RequestLoginActivity.this, TabbedMainActivity.class);
                                startActivity(main);
                                RequestLoginActivity.this.finish();
                            }

                            @Override
                            public void onFailure(final Throwable t) {
                                if (t instanceof LoginFailed) {
                                    // login failed - most likely TREZOR not paired
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            new MaterialDialog.Builder(RequestLoginActivity.this)
                                                    .title(getResources().getString(R.string.trezor_login_failed))
                                                    .content(getResources().getString(R.string.trezor_login_failed_details))
                                                    .build().show();
                                        }
                                    });
                                } else {
                                    RequestLoginActivity.this.finish();
                                }
                            }
                        });
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        t.printStackTrace();
                    }
                });
            } else {
                final TextView edit = (TextView) findViewById(R.id.firstLoginRequestedInstructionsText);
                edit.setVisibility(View.GONE);
                // not TREZOR, so must be BTChip
                if (tag != null) {
                    showPinDialog(IsoDep.get(tag));
                } else {
                    UsbDevice device = getIntent().getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (device != null) {
                        showPinDialog(device);
                    }
                }
            }
            return;
        }

        if (getSharedPreferences("pin", MODE_PRIVATE).getString("ident", null) != null) {
            final Intent pin = new Intent(this, PinActivity.class);
            startActivityForResult(pin, 0);
        } else {
            final Intent mnemonic = new Intent(this, MnemonicActivity.class);
            startActivityForResult(mnemonic, 0);
        }
    }

    private void showPinDialog(final UsbDevice device) {
        showPinDialog(device, null, -1);
    }

    private void showPinDialog(final IsoDep device) {
        showPinDialog(null, device, -1);
    }
    Dialog btchipDialog = null;
    private void showPinDialog(final UsbDevice device, final IsoDep isoDep, final int remainingAttempts) {
        final SettableFuture<String> pinFuture = SettableFuture.create();
        RequestLoginActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final View inflatedLayout = getLayoutInflater().inflate(R.layout.dialog_btchip_pin, null, false);
                final EditText pinValue = (EditText) inflatedLayout.findViewById(R.id.btchipPINValue);
                if (remainingAttempts != -1) {
                    TextView invalidPIN = (TextView) inflatedLayout.findViewById(R.id.btchipPinPrompt);
                    if (remainingAttempts > 0) {
                        invalidPIN.setText(new Formatter().format(
                                getResources().getString(R.string.btchipInvalidPIN), remainingAttempts).toString());
                    } else {
                        invalidPIN.setText(getResources().getString(R.string.btchipNotSetup));
                    }
                    pinValue.setVisibility(View.GONE);
                }

                MaterialDialog.Builder builder = new MaterialDialog.Builder(RequestLoginActivity.this)
                        .title("BTChip PIN")
                        .customView(inflatedLayout, true)
                        .positiveColorRes(R.color.accent)
                        .negativeColorRes(R.color.accent)
                        .titleColorRes(R.color.white)
                        .contentColorRes(android.R.color.white)
                        .theme(Theme.DARK)
                        .callback(new MaterialDialog.ButtonCallback() {
                            @Override
                            public void onPositive(MaterialDialog materialDialog) {
                                final ProgressBar prog = (ProgressBar) findViewById(R.id.signingLogin);
                                prog.setVisibility(View.VISIBLE);
                                pinFuture.set(pinValue.getText().toString());
                            }
                        });
                if (remainingAttempts == -1) {
                    builder = builder
                            .positiveText("OK")
                            .negativeText("CANCEL");
                }
                if (btchipDialog == null) {
                    btchipDialog = builder.build();
                }
                // (FIXME not sure if there's any smaller subset of these 3 calls below which works too)
                pinValue.requestFocus();
                btchipDialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
                btchipDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                pinValue.setOnEditorActionListener(
                        new EditText.OnEditorActionListener() {
                            @Override
                            public boolean onEditorAction(final TextView v, final int actionId, final KeyEvent event) {
                                if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                                        actionId == EditorInfo.IME_ACTION_DONE ||
                                        event.getAction() == KeyEvent.ACTION_DOWN &&
                                                event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                                    if (event == null || !event.isShiftPressed()) {
                                        // the user is done typing.
                                        final ProgressBar prog = (ProgressBar) findViewById(R.id.signingLogin);
                                        prog.setVisibility(View.VISIBLE);
                                        btchipDialog.hide();
                                        pinFuture.set(pinValue.getText().toString());
                                        return true; // consume.
                                    }
                                }
                                return false; // pass on to other listeners.
                            }
                        }
                );
                btchipDialog.show();
            }
        });
        final BTChipTransport transport;
        if (device != null) {
            UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
            transport = BTChipTransportAndroid.open(manager, device);
        } else {
            transport = new BTChipTransportAndroidNFC(isoDep);
            transport.setDebug(true);
        }
        Futures.addCallback(getGAApp().onServiceConnected, new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
                final GaService gaService = getGAService();

                Futures.addCallback(Futures.transform(gaService.onConnected, new AsyncFunction<Void, LoginData>() {
                    @Override
                    public ListenableFuture<LoginData> apply(final Void input) throws Exception {
                        return Futures.transform(pinFuture, new AsyncFunction<String, LoginData>() {
                            @Override
                            public ListenableFuture<LoginData> apply(String pin) throws Exception {
                                SettableFuture<Integer> remainingAttemptsFuture = SettableFuture.create();
                                final BTChipHWWallet hwWallet = new BTChipHWWallet(transport, RequestLoginActivity.this, pin, remainingAttemptsFuture);
                                return Futures.transform(remainingAttemptsFuture, new AsyncFunction<Integer, LoginData>() {
                                    @Nullable
                                    @Override
                                    public ListenableFuture<LoginData> apply(@Nullable Integer input) {
                                        if (input.intValue() == -1) {
                                            // -1 means success
                                            return gaService.login(hwWallet);
                                        } else {
                                            showPinDialog(device, isoDep, input.intValue());
                                            return Futures.immediateFuture(null);
                                        }
                                    }
                                });
                            }
                        });
                    }
                }), new FutureCallback<LoginData>() {
                    @Override
                    public void onSuccess(@Nullable final LoginData result) {
                        if (result != null) {
                            final Intent main = new Intent(RequestLoginActivity.this, TabbedMainActivity.class);
                            startActivity(main);
                            RequestLoginActivity.this.finish();
                        }
                    }

                    @Override
                    public void onFailure(final Throwable t) {
                        t.printStackTrace();
                        RequestLoginActivity.this.finish();
                    }
                });
            }

            @Override
            public void onFailure(Throwable t) {
                t.printStackTrace();
            }
        });
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (btchipDialog != null) {
            btchipDialog.dismiss();
        }
    }
    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        setResult(resultCode, data);
        finish();
    }

    @Override
    public void onResume() {
        super.onResume();

        getGAApp().getConnectionObservable().addObserver(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getGAApp().getConnectionObservable().deleteObserver(this);
    }

    @Override
    public void update(Observable observable, Object data) {

    }

    protected GreenAddressApplication getGAApp() {
        return (GreenAddressApplication) getApplication();
    }

    protected GaService getGAService() {
        return getGAApp().gaService;
    }
}
