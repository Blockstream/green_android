package com.greenaddress.greenbits.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.dd.CircularProgressButton;
import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.greenaddress.greenapi.LoginData;
import com.greenaddress.greenbits.ConnectivityObservable;
import com.greenaddress.greenbits.GaService;
import com.lambdaworks.crypto.SCrypt;

import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.crypto.DRMWorkaround;
import org.bitcoinj.crypto.MnemonicCode;
import org.bitcoinj.crypto.MnemonicException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import javax.annotation.Nullable;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import de.schildbach.wallet.ui.ScanActivity;


public class MnemonicActivity extends ActionBarActivity implements Observer {

    private static int countSubStr(final String sub, final String s) {
        int c = 0;
        for (int l = s.indexOf(sub); l != -1;
             l = s.indexOf(sub, l + sub.length())) {
            ++c;
        }
        return c;
    }

    private static int levenshteinDistance(final String inputA, final String inputB) {
        final String strA = inputA.toLowerCase();
        final String strB = inputB.toLowerCase();
        final int[] c = new int[strB.length() + 1];
        for (int i = 0; i < c.length; ++i) {
            c[i] = i;
        }
        for (int i = 1; i <= strA.length(); ++i) {
            c[0] = i;
            int n = i - 1;
            for (int j = 1; j <= strB.length(); ++j) {
                final int cj = Math.min(1 + Math.min(c[j], c[j - 1]), strA.charAt(i - 1) == strB.charAt(j - 1) ? n : n + 1);
                n = c[j];
                c[j] = cj;
            }
        }
        return c[strB.length()];
    }

    private String getClosestWord(final String word) throws IOException {
        InputStream is = null;
        is = getAssets().open("bip39-wordlist.txt");
        final List<String> words = new ArrayList<>();

        String line;

        try {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            while ((line = reader.readLine()) != null) {
                words.add(line);
            }
        } finally {
            is.close();
        }

        final List<Integer> scores = new ArrayList<>();
        for (final String w : words) {
            scores.add(new Integer(Integer.MAX_VALUE));
        }

        for (int i = 0; i < words.size(); ++i) {
            scores.set(i, levenshteinDistance(word, words.get(i)));
        }

        return words.get(scores.indexOf(Collections.min(scores)));
    }

    private boolean validateMnemonic(final String mnemonic) {
        // FIXME: add support for BIP38'ed mnemonics
        // FIXME: add support for different bip39 word lists like Japanese, Spanish, etc
        InputStream closable = null;
        try {
            closable = getAssets().open("bip39-wordlist.txt");
            new MnemonicCode(closable, null).check(Arrays.asList(mnemonic.split(" ")));
        } catch (final IOException e) {
            Toast.makeText(MnemonicActivity.this, "Can't find resources file bip39-wordlist.txt, please contact support.", Toast.LENGTH_LONG).show();
            return false;
        } catch (final MnemonicException.MnemonicWordException e) {
            // show red only if there's a single match as we don't know the position of the failure
            if (countSubStr(e.badWord, mnemonic) == 1) {
                final int start = mnemonic.indexOf(e.badWord);
                final int end = start + e.badWord.length();
                final Spannable spannable = new SpannableString(mnemonic);
                spannable.setSpan(new StyleSpan(Typeface.BOLD), start, end, 0);
                spannable.setSpan(new StyleSpan(Typeface.ITALIC), start, end, 0);
                spannable.setSpan(new UnderlineSpan(), start, end, 0);
                spannable.setSpan(new ForegroundColorSpan(Color.RED), start, end, 0);
                ((EditText) findViewById(R.id.mnemonicText)).setText(spannable);
            }
            String closeworld = null;
            try {
                closeworld = getClosestWord(e.badWord);
            } catch (final IOException eGnore) {
                // ignore
            }
            if (closeworld == null) {
                Toast.makeText(MnemonicActivity.this, "'" + e.badWord + "'" + " is not a valid word", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(MnemonicActivity.this, "'" + e.badWord + "'" + " is not a valid word, did you mean '" + closeworld + "'?", Toast.LENGTH_LONG).show();
            }
            return false;
        } catch (final MnemonicException e) {
            Toast.makeText(MnemonicActivity.this, "Invalid passphrase (has to be 24 or 27 words)", Toast.LENGTH_LONG).show();
            return false;
        } finally {
            if (closable != null) {
                try {
                    closable.close();
                } catch (final IOException e) {
                }
            }
        }
        return true;
    }


    private void login() {
        Futures.addCallback(getGAApp().onServiceConnected, new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
                loginAfterServiceConnected();
            }

            @Override
            public void onFailure(Throwable t) {
                t.printStackTrace();
                Toast.makeText(MnemonicActivity.this, "Not connected, connection will resume automatically", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loginAfterServiceConnected() {
        final GaService gaService = getGAService();
        if (getGAApp().getConnectionObservable().getState() != ConnectivityObservable.State.CONNECTED) {
            Toast.makeText(MnemonicActivity.this, "Not connected", Toast.LENGTH_LONG).show();
            return;
        }
        final EditText edit = (EditText) findViewById(R.id.mnemonicText);
        final CircularProgressButton okButton = (CircularProgressButton) findViewById(R.id.mnemonicOkButton);


        if (!validateMnemonic(edit.getText().toString())) {
            return;
        }

        okButton.setIndeterminateProgressMode(true);
        okButton.setProgress(50);

        final AsyncFunction<Void, LoginData> connectToLogin = new AsyncFunction<Void, LoginData>() {
            @Override
            public ListenableFuture<LoginData> apply(final Void input) {
                if (edit.getText().toString().trim().split(" ").length == 27) {
                    // encrypted mnemonic
                    return Futures.transform(askForPassphrase(), new AsyncFunction<String, LoginData>() {
                        @Nullable
                        @Override
                        public ListenableFuture<LoginData> apply(@Nullable String passphrase) {
                            try {
                                byte[] entropy = new MnemonicCode().toEntropy(Arrays.asList(edit.getText().toString().trim().split(" ")));
                                String normalizedPassphrase = Normalizer.normalize(passphrase, Normalizer.Form.NFC);
                                byte[] decrypted = decryptMnemonic(entropy, normalizedPassphrase);
                                return gaService.login(Joiner.on(" ").join(new MnemonicCode().toMnemonic(decrypted)));
                            } catch (IOException | GeneralSecurityException | MnemonicException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    });
                } else {
                    return gaService.login(edit.getText().toString().trim());
                }
            }
        };

        final ListenableFuture<LoginData> loginFuture = Futures.transform(gaService.onConnected, connectToLogin, gaService.es);

        Futures.addCallback(loginFuture, new FutureCallback<LoginData>() {
            @Override
            public void onSuccess(@Nullable final LoginData result) {
                if (getCallingActivity() == null) {
                    final Intent pinSaveActivity = new Intent(MnemonicActivity.this, PinSaveActivity.class);
                    pinSaveActivity.putExtra("com.greenaddress.greenbits.NewPinMnemonic", gaService.getMnemonics());
                    startActivity(pinSaveActivity);
                } else {
                    setResult(RESULT_OK);
                }
                MnemonicActivity.this.finish();
            }

            @Override
            public void onFailure(final Throwable t) {
                final boolean accountDoesntExist = t instanceof ClassCastException;
                final String message = accountDoesntExist ? "Account doesn't exist" : "Login failed";
                t.printStackTrace();
                MnemonicActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MnemonicActivity.this, message, Toast.LENGTH_LONG).show();
                        okButton.setProgress(0);

                    }
                });
            }
        }, gaService.es);
    }

    private byte[] decryptMnemonic(byte[] entropy, String normalizedPassphrase) throws GeneralSecurityException {
        byte[] salt = Arrays.copyOfRange(entropy, 32, 36);
        byte[] encrypted = Arrays.copyOf(entropy, 32);
        byte[] derived = SCrypt.scrypt(normalizedPassphrase.getBytes(Charsets.UTF_8), salt, 16384, 8, 8, 64);
        byte[] key = Arrays.copyOfRange(derived, 32, 64);
        SecretKeySpec keyspec = new SecretKeySpec(key, "AES");

        DRMWorkaround.maybeDisableExportControls();
        @SuppressLint("GetInstance") // ECB for 256 bits is enough, and is the same that BIP38 uses
        Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");

        cipher.init(Cipher.DECRYPT_MODE, keyspec);
        byte[] decrypted = cipher.doFinal(encrypted, 0, 32);
        for (int i = 0; i < 32; i++)
            decrypted[i] ^= derived[i];

        byte[] hash = Sha256Hash.createDouble(decrypted).getBytes();
        if (!Arrays.equals(Arrays.copyOf(hash, 4), salt)) throw new RuntimeException("Invalid checksum");
        return decrypted;
    }

    private ListenableFuture<String> askForPassphrase() {
        final SettableFuture<String> passphraseFuture = SettableFuture.create();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final View inflatedLayout = getLayoutInflater().inflate(R.layout.dialog_passphrase, null, false);
                final EditText passphraseValue = (EditText) inflatedLayout.findViewById(R.id.passphraseValue);
                MaterialDialog dialog = new MaterialDialog.Builder(MnemonicActivity.this)
                        .title("Encryption passphrase")
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
                                passphraseFuture.set(passphraseValue.getText().toString());
                            }
                        })
                        .build();
                // (FIXME not sure if there's any smaller subset of these 3 calls below which works too)
                passphraseValue.requestFocus();
                dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
                dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                dialog.show();
            }
        });
        return passphraseFuture;
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("MnemonicActivity", getIntent().getType() + "" + getIntent());
        setContentView(R.layout.activity_mnemonic);
        final CircularProgressButton okButton = (CircularProgressButton) findViewById(R.id.mnemonicOkButton);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                MnemonicActivity.this.login();
            }
        });
        final EditText edit = (EditText) findViewById(R.id.mnemonicText);

        edit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(final TextView v, final int actionId, final KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    login();
                    return true;
                }
                return false;
            }
        });

        final TextView scanIcon = (TextView) findViewById(R.id.mnemonicScanIcon);
        scanIcon.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(final View view) {
                                            final Intent scanner = new Intent(MnemonicActivity.this, ScanActivity.class);
                                            startActivityForResult(scanner, 0);
                                        }
                                    }
        );

        final Intent intent = getIntent();
        if (intent != null && NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {

            if (intent.getType().equals("x-gait/mnc")) {
                // not encrypted nfc
                InputStream closable = null;
                try {
                    closable = getAssets().open("bip39-wordlist.txt");
                    final Parcelable[] rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
                    final byte[] array = ((NdefMessage) rawMessages[0]).getRecords()[0].getPayload();
                    final String mnemonics = TextUtils.join(" ",
                            new MnemonicCode(closable, null)
                                    .toMnemonic(array));
                    final GaService gaService = getGAService();
                    edit.setText(mnemonics);

                    if (gaService != null && gaService.onConnected != null && !mnemonics.equals(gaService.getMnemonics())) {

                        Futures.addCallback(gaService.onConnected, new FutureCallback<Void>() {
                            @Override
                            public void onSuccess(@Nullable final Void result) {
                                login();
                            }

                            @Override
                            public void onFailure(final Throwable t) {

                            }
                        });
                    }


                } catch (final IOException | MnemonicException e) {
                    e.printStackTrace();
                } finally {
                    if (closable != null) {
                        try {
                            closable.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else if (intent.getType().equals("x-ga/en")) {
                // encrypted nfc
                final Parcelable[] rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
                final byte[] array = ((NdefMessage) rawMessages[0]).getRecords()[0].getPayload();
                Futures.addCallback(askForPassphrase(), new FutureCallback<String>() {
                    @Override
                    public void onSuccess(@Nullable String passphrase) {
                        try {
                            byte[] decrypted = decryptMnemonic(array, passphrase);
                            final GaService gaService = getGAService();
                            final String mnemonics = Joiner.on(" ").join(new MnemonicCode().toMnemonic(decrypted));
                            edit.setText(mnemonics);
                            if (gaService != null && gaService.onConnected != null && !mnemonics.equals(gaService.getMnemonics())) {
                                Futures.addCallback(gaService.onConnected, new FutureCallback<Void>() {
                                    @Override
                                    public void onSuccess(@Nullable final Void result) {
                                        login();
                                    }

                                    @Override
                                    public void onFailure(final Throwable t) {

                                    }
                                });
                            }
                        } catch (GeneralSecurityException | IOException | MnemonicException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(Throwable t) {

                    }
                });
            }

        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        getGAApp().getConnectionObservable().addObserver(this);

        ConnectivityObservable.State state = getGAApp().getConnectionObservable().getState();
        if (state.equals(ConnectivityObservable.State.LOGGEDIN)) {
            // already logged in, could be from different app via intent
            final Intent mainActivity = new Intent(MnemonicActivity.this, TabbedMainActivity.class);
            startActivity(mainActivity);
            finish();
            return;
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        getGAApp().getConnectionObservable().deleteObserver(this);
    }


    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i("MnemonicActivity", "" + data);
        final EditText edit = (EditText) findViewById(R.id.mnemonicText);
        if (data != null && data.getStringExtra("com.greenaddress.greenbits.QrText") != null) {
            edit.setText(data.getStringExtra("com.greenaddress.greenbits.QrText"));
            login();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        // getMenuInflater().inflate(R.menu.mnemonic, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void update(final Observable observable, final Object data) {

    }
}
