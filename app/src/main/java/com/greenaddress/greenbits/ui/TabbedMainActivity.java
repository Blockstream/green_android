package com.greenaddress.greenbits.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.blockstream.libwally.Wally;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.greenaddress.greenapi.Network;
import com.greenaddress.greenbits.ConnectivityObservable;
import com.greenaddress.greenbits.GaService;
import com.greenaddress.greenbits.ui.monitor.NetworkMonitorActivity;
import com.greenaddress.greenbits.ui.preferences.SettingsActivity;

import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.DumpedPrivateKey;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.utils.MonetaryFormat;
import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import de.schildbach.wallet.ui.ScanActivity;

// Problem with the above is that in the horizontal orientation the tabs don't go in the top bar
public class TabbedMainActivity extends ActionBarActivity implements Observer {
    private static final int
            REQUEST_SEND_QR_SCAN = 0,
            REQUEST_SWEEP_PRIVKEY = 1,
            REQUEST_BITCOIN_URL_LOGIN = 2,
            REQUEST_SETTINGS = 3,
            REQUEST_TX_DETAILS = 4;
    @Nullable
    public static TabbedMainActivity instance = null;
    private ViewPager mViewPager;
    private Menu menu;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        boolean isBitcoinURL = getIntent().hasCategory(Intent.CATEGORY_BROWSABLE) ||
                NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction()) ||
                (getIntent().getData() != null && getIntent().getData().getScheme() != null
                        && getIntent().getData().getScheme().equals("bitcoin"));

        if (isBitcoinURL) {
            if (!ConnectivityObservable.State.LOGGEDIN.equals(getGAApp().getConnectionObservable().getState()) || getGAApp().getConnectionObservable().getState().equals(ConnectivityObservable.State.LOGGINGIN)) {
                // login required
                final Intent loginActivity = new Intent(this, RequestLoginActivity.class);
                startActivityForResult(loginActivity, REQUEST_BITCOIN_URL_LOGIN);
                return;
            }
        }

        launch(isBitcoinURL);
    }

    private void configureNoTwoFacFooter() {

        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        final boolean twoFacWarning = sharedPref.getBoolean("twoFacWarning", false);
        final GaService gs = getGAService();
        if (gs == null) {
            return;
        }
        final Map<?, ?> twoFacConfig = gs.getTwoFacConfig();
        if (twoFacConfig == null) {
            return;
        }
        if (!((Boolean) twoFacConfig.get("any") || twoFacWarning)) {
            final Snackbar snackbar = Snackbar
                    .make(findViewById(R.id.main_content), getString(R.string.noTwoFactorWarning), Snackbar.LENGTH_INDEFINITE)
                    .setActionTextColor(Color.RED)
                    .setAction(getString(R.string.set2FA), new View.OnClickListener() {
                        @Override
                        public void onClick(final View view) {
                            startActivityForResult(new Intent(TabbedMainActivity.this, SettingsActivity.class), REQUEST_SETTINGS);
                        }
                    });

            final View snackbarView = snackbar.getView();
            snackbarView.setBackgroundColor(Color.DKGRAY);
            final TextView textView = (TextView) snackbarView.findViewById(android.support.design.R.id.snackbar_text);
            textView.setTextColor(Color.WHITE);
            snackbar.show();
        }
    }

    private void configureSubaccountsFooter(final int curSubaccount) {
        final GaService gs = getGAService();
        if (gs == null) {
            return;
        }
        final ArrayList subs = gs.getSubaccounts();
        if (subs == null || subs.isEmpty()) {
            return;
        }
        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setVisibility(View.VISIBLE);


        String subaccountName = getResources().getText(R.string.main_account).toString();
        for (Object subaccount : subs) {
            final Map<String, ?> subaccountMap = (Map) subaccount;
            final String name = (String) subaccountMap.get("name");
            if (subaccountMap.get("pointer").equals(curSubaccount)) {
                subaccountName = name;
                break;
            }
        }
        setTitle(String.format("%s %s", getResources().getText(R.string.app_name), subaccountName));


        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                final ArrayList<String> subaccounts_list = new ArrayList<>();

                subaccounts_list.add(getResources().getText(R.string.main_account).toString());

                final ArrayList subs = getGAService().getSubaccounts();
                for (final Object subaccount : subs) {
                    subaccounts_list.add(((Map) subaccount).get("name").toString());
                }

                final MaterialDialog.ListCallback lcb = new MaterialDialog.ListCallback() {
                    @Override
                    public void onSelection(final MaterialDialog dialog, final View view, final int which, final CharSequence text) {

                        final SharedPreferences sp = getGAApp().getSharedPreferences("main",
                                Context.MODE_PRIVATE);

                        final int curSubaccount;
                        if (which == 0) {
                            curSubaccount = 0;
                        } else {
                            final ArrayList subaccounts = getGAService().getSubaccounts();
                            curSubaccount = ((Integer) ((Map<String, ?>) subaccounts.get(which - 1)).get("pointer"));
                        }

                        if (sp.getInt("curSubaccount", 0) != curSubaccount) {
                            setTitle(String.format("%s %s", getResources().getText(R.string.app_name), text));
                            onSubaccountUpdate(curSubaccount);
                        }
                    }
                };

                new MaterialDialog.Builder(TabbedMainActivity.this)
                        .title(R.string.footerAccount)
                        .items(subaccounts_list)
                        .autoDismiss(true)
                        .itemsCallback(lcb).show();
            }
        });
    }

    private void onSubaccountUpdate(final int input) {
        final SharedPreferences.Editor editor = getGAApp().getSharedPreferences("main", Context.MODE_PRIVATE).edit();
        editor.putInt("curSubaccount", input);
        editor.apply();

        final Intent data = new Intent("fragmentupdater");
        data.putExtra("sub", input);
        TabbedMainActivity.this.sendBroadcast(data);
    }

    @SuppressLint("NewApi") // NdefRecord#toUri disabled for API < 16
    private void launch(boolean isBitcoinURL) {
        setContentView(R.layout.activity_tabbed_main);
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Set up the action bar.
        final SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(sectionsPagerAdapter);

        final TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);
        final GaService gs = getGAService();
        if (gs == null) {
            Toast.makeText(TabbedMainActivity.this, getString(R.string.err_send_not_connected_will_resume), Toast.LENGTH_SHORT).show();
            return;
        }

        configureNoTwoFacFooter();

        final Handler handler = new Handler();

        gs.getTwoFacConfigObservable().addObserver(new Observer() {
            @Override
            public void update(final Observable observable, final Object data) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        configureNoTwoFacFooter();
                    }
                });
            }
        });

        final int curSubaccount = getGAApp().getSharedPreferences("main", Context.MODE_PRIVATE).getInt("curSubaccount", 0);


        configureSubaccountsFooter(curSubaccount);


        if (isBitcoinURL) {
            if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
                if (Build.VERSION.SDK_INT < 16) {
                    // NdefRecord#toUri not available in API < 16
                    mViewPager.setCurrentItem(1);
                    return;
                }
                final Parcelable[] rawMessages = getIntent().getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
                for (Parcelable ndefMsg_ : rawMessages) {
                    final NdefMessage ndefMsg = (NdefMessage) ndefMsg_;
                    for (NdefRecord record : ndefMsg.getRecords()) {
                        if (record.getTnf() == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(record.getType(), NdefRecord.RTD_URI)) {
                            mViewPager.setTag(R.id.tag_bitcoin_uri, record.toUri());
                        }
                    }
                }
            } else {
                mViewPager.setTag(R.id.tag_bitcoin_uri, getIntent().getData());
            }
            mViewPager.setCurrentItem(2);
        } else {
            mViewPager.setCurrentItem(1);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getGAApp().getConnectionObservable().addObserver(this);
        testKickedOut();
        instance = this;
        setIdVisible(getGAApp().getConnectionObservable().getState() != ConnectivityObservable.State.LOGGEDIN, R.id.action_share);
    }

    @Override
    public void onPause() {
        super.onPause();
        getGAApp().getConnectionObservable().deleteObserver(this);
    }

    private final static int BIP38_FLAGS = (NetworkParameters.fromID(NetworkParameters.ID_MAINNET).equals(Network.NETWORK)
            ? Wally.BIP38_KEY_MAINNET : Wally.BIP38_KEY_TESTNET) | Wally.BIP38_KEY_COMPRESSED;

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, @Nullable final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_TX_DETAILS:
            case REQUEST_SETTINGS:
                final int curSubaccount = getGAApp().getSharedPreferences("main", Context.MODE_PRIVATE).getInt("curSubaccount", 0);
                getGAService().updateBalance(curSubaccount);
                startActivity(new Intent(this, TabbedMainActivity.class));
                finish();
                break;
            case REQUEST_SEND_QR_SCAN:
                if (data != null && data.getStringExtra("com.greenaddress.greenbits.QrText") != null) {
                    String scanned = data.getStringExtra("com.greenaddress.greenbits.QrText");
                    if (!(scanned.length() >= 8 && scanned.substring(0, 8).equalsIgnoreCase("bitcoin:"))) {
                        scanned = String.format("bitcoin:%s", scanned);
                    }
                    final Intent browsable = new Intent(this, TabbedMainActivity.class);
                    browsable.setData(Uri.parse(scanned));
                    browsable.addCategory(Intent.CATEGORY_BROWSABLE);
                    startActivity(browsable);
                }
                break;
            case REQUEST_BITCOIN_URL_LOGIN:
                if (resultCode == RESULT_OK) {
                    launch(true);
                } else {
                    finish();
                }
                break;
            case REQUEST_SWEEP_PRIVKEY:
                if (data == null) {
                    return;
                }
                ECKey keyNonFinal = null;
                final String qrText = data.getStringExtra("com.greenaddress.greenbits.QrText");
                try {
                    keyNonFinal = new DumpedPrivateKey(Network.NETWORK,
                            qrText).getKey();
                } catch (@NonNull final AddressFormatException e) {
                    try {
                        Wally.bip38_to_private_key(qrText, null, Wally.BIP38_KEY_COMPRESSED | Wally.BIP38_KEY_QUICK_CHECK, null);
                    } catch (final IllegalArgumentException e2) {
                        Toast.makeText(TabbedMainActivity.this, getResources().getString(R.string.invalid_key), Toast.LENGTH_LONG).show();
                        return;
                    }
                }
                final ECKey keyNonBip38 = keyNonFinal;
                final FutureCallback<Map<?, ?>> callback = new FutureCallback<Map<?, ?>>() {
                    @Override
                    public void onSuccess(final @Nullable Map<?, ?> result) {
                        final View inflatedLayout = getLayoutInflater().inflate(R.layout.dialog_sweep_address, null, false);
                        final TextView passwordPrompt = (TextView) inflatedLayout.findViewById(R.id.sweepAddressPasswordPromptText);
                        final TextView mainText = (TextView) inflatedLayout.findViewById(R.id.sweepAddressMainText);
                        final TextView addressText = (TextView) inflatedLayout.findViewById(R.id.sweepAddressAddressText);
                        final EditText passwordEdit = (EditText) inflatedLayout.findViewById(R.id.sweepAddressPasswordText);
                        final Transaction txNonBip38;
                        final String address;
                        if (keyNonBip38 != null) {
                            passwordPrompt.setVisibility(View.GONE);
                            passwordEdit.setVisibility(View.GONE);
                            txNonBip38 = new Transaction(Network.NETWORK,
                                    Hex.decode((String) result.get("tx")));
                            final MonetaryFormat format = CurrencyMapper.mapBtcUnitToFormat(
                                    (String) getGAService().getAppearanceValue("unit"));
                            Coin outputsValue = Coin.ZERO;
                            for (final TransactionOutput output : txNonBip38.getOutputs()) {
                                outputsValue = outputsValue.add(output.getValue());
                            }
                            mainText.setText(Html.fromHtml("Are you sure you want to sweep <b>all</b> ("
                                    + format.postfixCode().format(outputsValue) + ") funds from the address below?"));
                            address = keyNonBip38.toAddress(Network.NETWORK).toString();
                        } else {
                            passwordPrompt.setText(getResources().getString(R.string.sweep_bip38_passphrase_prompt));
                            txNonBip38 = null;
                            // amount not known until decrypted
                            mainText.setText(Html.fromHtml("Are you sure you want to sweep <b>all</b> funds from the password protected BIP38 key below?"));
                            address = data.getStringExtra("com.greenaddress.greenbits.QrText");
                        }


                        addressText.setText(String.format("%s\n%s\n%s", address.substring(0, 12), address.substring(12, 24), address.substring(24)));

                        new MaterialDialog.Builder(TabbedMainActivity.this)
                                .title(R.string.sweepAddressTitle)
                                .customView(inflatedLayout, true)
                                .positiveText(R.string.sweep)
                                .negativeText(R.string.cancel)
                                .positiveColorRes(R.color.accent)
                                .negativeColorRes(R.color.accent)
                                .titleColorRes(R.color.white)
                                .contentColorRes(android.R.color.white)
                                .theme(Theme.DARK)
                                .onPositive(new MaterialDialog.SingleButtonCallback() {
                                    @Nullable
                                    Transaction tx;
                                    @Nullable
                                    ECKey key;

                                    private void doSweep() {
                                        final ArrayList<String> scripts = (ArrayList<String>) result.get("prevout_scripts");
                                        Futures.addCallback(getGAService().verifySpendableBy(
                                                tx.getOutputs().get(0),
                                                0,
                                                ((Integer) result.get("out_pointer"))
                                        ), new FutureCallback<Boolean>() {
                                            @Override
                                            public void onSuccess(final @Nullable Boolean result) {
                                                if (result) {
                                                    final List<TransactionSignature> signatures = new ArrayList<>();
                                                    final int size = tx.getInputs().size();
                                                    for (int i = 0; i < size; ++i) {
                                                        signatures.add(tx.calculateSignature(i, key, Hex.decode(scripts.get(i)), Transaction.SigHash.ALL, false));
                                                    }
                                                    Futures.addCallback(getGAService().sendTransaction(signatures), new FutureCallback<String>() {
                                                        @Override
                                                        public void onSuccess(final @Nullable String result) {

                                                        }

                                                        @Override
                                                        public void onFailure(@NonNull final Throwable t) {
                                                            t.printStackTrace();
                                                            Toast.makeText(TabbedMainActivity.this, t.getMessage(), Toast.LENGTH_LONG).show();
                                                        }
                                                    });
                                                } else {
                                                    Toast.makeText(TabbedMainActivity.this, getString(R.string.err_tabbed_sweep_failed), Toast.LENGTH_LONG).show();
                                                }
                                            }

                                            @Override
                                            public void onFailure(@NonNull final Throwable t) {
                                                t.printStackTrace();
                                                Toast.makeText(TabbedMainActivity.this, t.getMessage(), Toast.LENGTH_LONG).show();
                                            }
                                        });
                                    }

                                    @Override
                                    public void onClick(final @NonNull MaterialDialog dialog, final @NonNull DialogAction which) {
                                        if (keyNonBip38 == null) {
                                            try {
                                                final String password = passwordEdit.getText().toString();
                                                final byte[] passbytes = password.getBytes();
                                                final byte[] decryptedPKey = Wally.bip38_to_private_key(qrText, passbytes, BIP38_FLAGS, null);
                                                key = ECKey.fromPrivate(decryptedPKey);

                                                Futures.addCallback(getGAService().prepareSweepSocial(
                                                        key.getPubKey(), true), new FutureCallback<Map<?, ?>>() {
                                                    @Override
                                                    public void onSuccess(@Nullable final Map<?, ?> result) {
                                                        tx = new Transaction(Network.NETWORK,
                                                                Hex.decode((String) result.get("tx")));
                                                        doSweep();
                                                    }

                                                    @Override
                                                    public void onFailure(@NonNull final Throwable t) {
                                                        Toast.makeText(TabbedMainActivity.this, t.getMessage(), Toast.LENGTH_LONG).show();
                                                    }
                                                });
                                            } catch (@NonNull final IllegalArgumentException e) {
                                                Toast.makeText(TabbedMainActivity.this, getResources().getString(R.string.invalid_passphrase), Toast.LENGTH_LONG).show();
                                            }

                                        } else {
                                            tx = txNonBip38;
                                            key = keyNonBip38;
                                            doSweep();
                                        }
                                    }
                                })
                                .build().show();
                    }

                    @Override
                    public void onFailure(@NonNull final Throwable t) {
                        Toast.makeText(TabbedMainActivity.this, t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                };
                if (keyNonBip38 != null) {
                    Futures.addCallback(getGAService().prepareSweepSocial(
                            keyNonBip38.getPubKey(), false), callback);
                } else {
                    callback.onSuccess(null);
                }
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        this.menu = menu;

        return true;
    }

    private void setIdVisible(final boolean visible, final int id) {
        if (menu != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    final MenuItem item = menu.findItem(id);
                    if (item != null) {
                        item.setVisible(visible);
                    }
                }
            });
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivityForResult(new Intent(TabbedMainActivity.this, SettingsActivity.class), REQUEST_SETTINGS);
                return true;
            case R.id.action_sweep:
                final Intent scanner = new Intent(TabbedMainActivity.this, ScanActivity.class);
                //New Marshmallow permissions paradigm
                final String[] perms = {"android.permission.CAMERA"};
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1 &&
                        checkSelfPermission(perms[0]) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(perms, /*permsRequestCode*/ 200);
                } else {
                    startActivityForResult(scanner, REQUEST_SWEEP_PRIVKEY);
                }
                return true;
            case R.id.network_unavailable:
                Toast.makeText(TabbedMainActivity.this, getGAApp().getConnectionObservable().getState().toString(), Toast.LENGTH_LONG).show();
                return true;
            case R.id.action_logout:
                getGAService().disconnect(false);
                finish();
                return true;
            case R.id.action_network:
                startActivity(new Intent(TabbedMainActivity.this, NetworkMonitorActivity.class));
                return true;
            case R.id.action_about:
                startActivity(new Intent(TabbedMainActivity.this, AboutActivity.class));
                return true;

        }
        return super.onOptionsItemSelected(item);
    }

    private void testKickedOut() {
        if (getGAApp().getConnectionObservable().getIsForcedLoggedOut() || getGAApp().getConnectionObservable().getIsForcedTimeout()) {
            // FIXME: Should pass flag to activity so it shows it was forced logged out
            startActivity(new Intent(TabbedMainActivity.this, FirstScreenActivity.class));
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        if (mViewPager.getCurrentItem() == 1) {
            finish();
        } else {
            mViewPager.setCurrentItem(1);
        }
    }

    @Override
    public void update(final Observable observable, final Object data) {
        if (getGAApp().getConnectionObservable().getIsForcedLoggedOut() || getGAApp().getConnectionObservable().getIsForcedTimeout()) {
            // FIXME: Should pass flag to activity so it shows it was forced logged out
            startActivity(new Intent(TabbedMainActivity.this, FirstScreenActivity.class));
        }
        final ConnectivityObservable.State currentState = getGAApp().getConnectionObservable().getState();
        setIdVisible(currentState != ConnectivityObservable.State.LOGGEDIN, R.id.network_unavailable);
    }

    @Override
    public void onRequestPermissionsResult(final int permsRequestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        switch (permsRequestCode) {
            case 200: {
                final boolean cameraPermissionGranted = grantResults[0] == PackageManager.PERMISSION_GRANTED;

                if (cameraPermissionGranted) {
                    final Intent scanner = new Intent(TabbedMainActivity.this, ScanActivity.class);
                    startActivityForResult(scanner, REQUEST_SWEEP_PRIVKEY);
                } else {
                    Toast.makeText(getApplicationContext(), getString(R.string.err_tabbed_sweep_requires_camera_permissions), Toast.LENGTH_SHORT).show();
                }
                break;
            }
            case 100: {
                final boolean cameraPermissionGranted = grantResults[0] == PackageManager.PERMISSION_GRANTED;

                if (cameraPermissionGranted) {
                    final Intent qrcodeScanner = new Intent(TabbedMainActivity.this, ScanActivity.class);
                    startActivityForResult(qrcodeScanner, TabbedMainActivity.REQUEST_SEND_QR_SCAN);
                } else {
                    Toast.makeText(getApplicationContext(), getString(R.string.err_qrscan_requires_camera_permissions), Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(final FragmentManager fm) {
            super(fm);
        }

        @Nullable
        @Override
        public Fragment getItem(final int index) {

            switch (index) {
                case 0:
                    return new ReceiveFragment();
                case 1:
                    return new MainFragment();
                case 2:
                    return new SendFragment();
            }

            return null;
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(final int position) {
            final Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return getString(R.string.receive_title).toUpperCase(l);
                case 1:
                    return getString(R.string.main_title).toUpperCase(l);
                case 2:
                    return getString(R.string.send_title).toUpperCase(l);
            }
            return null;
        }
    }
}
