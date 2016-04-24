package com.greenaddress.greenbits.ui;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.BitmapDrawable;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.method.LinkMovementMethod;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.dd.CircularProgressButton;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.greenaddress.greenapi.LoginData;
import com.greenaddress.greenbits.GaService;
import com.greenaddress.greenbits.QrBitmap;

import org.bitcoinj.crypto.MnemonicException;

import java.io.IOException;
import java.util.Observable;
import java.util.Observer;

public class SignUpActivity extends ActionBarActivity implements Observer {
    @NonNull private static final String TAG = SignUpActivity.class.getSimpleName();
    private static final int PINSAVE = 1337;

    private boolean mWriteMode = false;
    private Dialog mnemonicDialog;
    private Dialog nfcDialog;
    private NfcAdapter mNfcAdapter;
    private PendingIntent mNfcPendingIntent;
    private TextView nfcTagsWritten;
    private ImageView signupNfcIcon;
    private TextView mnemonicText;
    @Nullable
    private ListenableFuture<LoginData> onSignUp;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        final CircularProgressButton signupContinueButton = (CircularProgressButton) findViewById(R.id.signupContinueButton);
        final TextView tos = (TextView) findViewById(R.id.textTosLink);
        final CheckBox checkBox = (CheckBox) findViewById(R.id.signupAcceptCheckBox);
        final View nfcLayout = getLayoutInflater().inflate(R.layout.dialog_nfc_write, null, false);
        nfcTagsWritten = (TextView) nfcLayout.findViewById(R.id.nfcTagsWrittenText);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        mNfcPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, SignUpActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        if (onSignUp != null) {
            checkBox.setEnabled(false);
            checkBox.setChecked(true);
        }

        tos.setMovementMethod(LinkMovementMethod.getInstance());

        mnemonicText = (TextView) findViewById(R.id.signupMnemonicText);

        final View inflatedLayout = getLayoutInflater().inflate(R.layout.dialog_qrcode, null, false);

        final TextView qrCodeIcon = (TextView) findViewById(R.id.signupQrCodeIcon);
        final ImageView qrcodeMnemonic = (ImageView) inflatedLayout.findViewById(R.id.qrInDialogImageView);

        Futures.addCallback(getGAApp().onServiceAttached, new FutureCallback<Void>() {

            @Override
            public void onSuccess(@javax.annotation.Nullable final Void result) {
                final ListenableFuture<String> mnemonicPassphrase = getGAService().getMnemonicPassphrase();
                Futures.addCallback(mnemonicPassphrase, new FutureCallback<String>() {
                    @Override
                    public void onSuccess(@Nullable final String result) {
                        SignUpActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mnemonicText.setText(result);
                            }
                        });
                    }

                    @Override
                    public void onFailure(@NonNull final Throwable t) {
                        finish();
                    }
                }, getGAService().es);
            }

            @Override
            public void onFailure(@NonNull final Throwable t) {
                finish();
            }
        });



        qrCodeIcon.setOnClickListener(new View.OnClickListener() {
            public void onClick(final View view) {
                final Animation iconPressed = AnimationUtils.loadAnimation(SignUpActivity.this, R.anim.rotation);
                qrCodeIcon.startAnimation(iconPressed);

                final ListenableFuture<QrBitmap> mnemonicQrcode = getGAService().getQrCodeForMnemonicPassphrase();
                Futures.addCallback(mnemonicQrcode, new FutureCallback<QrBitmap>() {

                    @Override
                    public void onSuccess(@Nullable final QrBitmap result) {
                        SignUpActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                qrCodeIcon.clearAnimation();
                                if (mnemonicDialog == null) {
                                    final DisplayMetrics displaymetrics = new DisplayMetrics();
                                    getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
                                    final int height = displaymetrics.heightPixels;
                                    final int width = displaymetrics.widthPixels;
                                    Log.i(TAG, height + "x" + width);
                                    final int min = (int) (Math.min(height, width) * 0.8);
                                    final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(min, min);
                                    qrcodeMnemonic.setLayoutParams(layoutParams);

                                    mnemonicDialog = new Dialog(SignUpActivity.this);
                                    mnemonicDialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
                                    mnemonicDialog.setContentView(inflatedLayout);
                                }
                                mnemonicDialog.show();
                                final BitmapDrawable bd = new BitmapDrawable(getResources(), result.qrcode);
                                bd.setFilterBitmap(false);
                                qrcodeMnemonic.setImageDrawable(bd);
                            }
                        });
                    }

                    @Override
                    public void onFailure(@NonNull final Throwable t) {

                    }
                }, getGAService().es);
            }
        });

        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(final CompoundButton compoundButton, final boolean isChecked) {
                final GaService gs = getGAService();
                if (onSignUp == null) {
                    if (gs != null && gs.onConnected != null) {
                        signupContinueButton.setEnabled(true);
                        checkBox.setEnabled(false);
                        onSignUp = Futures.transform(gs.onConnected, new AsyncFunction<Void, LoginData>() {
                            @NonNull
                            @Override
                            public ListenableFuture<LoginData> apply(@Nullable final Void input) throws Exception {
                                return gs.signup(mnemonicText.getText().toString());
                            }
                        }, gs.es);
                    } else {
                        if (isChecked) {
                            SignUpActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(SignUpActivity.this, "You are not connected, please wait", Toast.LENGTH_LONG).show();
                                }
                            });
                            checkBox.setChecked(false);
                        }
                    }
                }
            }
        });

        signupContinueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                if (onSignUp != null) {

                    signupContinueButton.setIndeterminateProgressMode(true);
                    signupContinueButton.setProgress(50);
                    Futures.addCallback(onSignUp, new FutureCallback<LoginData>() {

                        @Override
                        public void onSuccess(@Nullable final LoginData result) {
                            SignUpActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    signupContinueButton.setProgress(100);
                                }
                            });

                            final Intent pinSaveActivity = new Intent(SignUpActivity.this, PinSaveActivity.class);

                            pinSaveActivity.putExtra("com.greenaddress.greenbits.NewPinMnemonic", getGAService().getMnemonics());
                            getGAService().resetSignUp();
                            onSignUp = null;
                            startActivityForResult(pinSaveActivity, PINSAVE);
                        }

                        @Override
                        public void onFailure(@NonNull final Throwable t) {
                            t.printStackTrace();
                            SignUpActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    signupContinueButton.setProgress(0);
                                }
                            });
                        }
                    }, getGAService().es);
                } else {
                    if (!checkBox.isChecked()) {
                        SignUpActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(SignUpActivity.this, "Please secure your passphrase and confirm you agree to the Terms of Service", Toast.LENGTH_LONG).show();
                            }
                        });
                    } else {
                        SignUpActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(SignUpActivity.this, "Signup in progress", Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }
            }
        });
        signupNfcIcon = (ImageView) findViewById(R.id.signupNfcIcon);

        nfcDialog = new MaterialDialog.Builder(SignUpActivity.this)
                .title("Hold your NFC tag close to the device")
                .customView(nfcLayout, true)
                .titleColorRes(R.color.white)
                .contentColorRes(android.R.color.white)
                .theme(Theme.DARK).build();

        if (Build.VERSION.SDK_INT < 16) {
            signupNfcIcon.setVisibility(View.GONE);
        } else {
            signupNfcIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    mWriteMode = true;
                    nfcDialog.show();
                }
            });
        }

        nfcDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(final DialogInterface dialog) {
                mWriteMode = false;
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mNfcAdapter != null) {
            mNfcAdapter.enableForegroundDispatch(this, mNfcPendingIntent, new IntentFilter[]{new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)}, null);
        }
        signupNfcIcon.setVisibility(mNfcAdapter != null && mNfcAdapter.isEnabled() ? View.VISIBLE : View.GONE);
        getGAApp().getConnectionObservable().addObserver(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mNfcAdapter != null) {
            mNfcAdapter.disableForegroundDispatch(this);
        }
        getGAApp().getConnectionObservable().deleteObserver(this);

    }

    @Override
    @SuppressLint("NewApi") // signupNfcIcon is hidden for API < 16
    protected void onNewIntent(@NonNull final Intent intent) {
        super.onNewIntent(intent);
        if (mWriteMode && NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {

            final Tag detectedTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            final NdefRecord[] record = new NdefRecord[1];
            try {
                record[0] = NdefRecord.createMime("x-gait/mnc", getGAService().getEntropyFromMnemonics(mnemonicText.getText().toString()));
            } catch (@NonNull final IOException | MnemonicException.MnemonicChecksumException | MnemonicException.MnemonicLengthException | MnemonicException.MnemonicWordException e) {
                return;
            }

            final NdefMessage message = new NdefMessage(record);
            final int size = message.toByteArray().length;
            try {
                final Ndef ndef = Ndef.get(detectedTag);
                if (ndef != null) {
                    ndef.connect();
                    if (!ndef.isWritable()) {
                        Toast.makeText(getApplicationContext(),
                                getString(R.string.err_sign_up_nfc_not_writable),
                                Toast.LENGTH_SHORT).show();
                    }
                    if (ndef.getMaxSize() < size) {
                        Toast.makeText(getApplicationContext(),
                                getString(R.string.err_sign_up_nfc_too_small),
                                Toast.LENGTH_SHORT).show();
                    }
                    ndef.writeNdefMessage(message);
                    nfcTagsWritten.setText(String.valueOf(Integer.parseInt(nfcTagsWritten.getText().toString()) + 1));

                } else {
                    final NdefFormatable format = NdefFormatable.get(detectedTag);
                    if (format != null) {
                        try {
                            format.connect();
                            format.format(message);
                            nfcTagsWritten.setText(String.valueOf(Integer.parseInt(nfcTagsWritten.getText().toString()) + 1));
                        } catch (@NonNull final IOException e) {
                        }
                    }
                }
            } catch (@NonNull final Exception e) {
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mnemonicDialog != null) {
            mnemonicDialog.dismiss();
        }
        if (nfcDialog != null) {
            nfcDialog.dismiss();
        }
    }

    @Override
    public void onBackPressed() {
        if (onSignUp != null) {
            getGAService().resetSignUp();
            onSignUp = null;
            getGAService().disconnect(true);
        }
        super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.common_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return item.getItemId() == R.id.action_settings || super.onOptionsItemSelected(item);
    }

    @Override
    public void update(Observable observable, Object data) {

    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, @android.support.annotation.Nullable final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case PINSAVE:
                final Intent tabbedMainActivity = new Intent(SignUpActivity.this, TabbedMainActivity.class);
                startActivity(tabbedMainActivity);
                finish();
                break;
        }
    }
}
