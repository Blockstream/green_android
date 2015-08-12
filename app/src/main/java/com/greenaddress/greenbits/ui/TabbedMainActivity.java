package com.greenaddress.greenbits.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.greenaddress.greenapi.Network;
import com.greenaddress.greenbits.ConnectivityObservable;

import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.DumpedPrivateKey;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.crypto.BIP38PrivateKey;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.uri.BitcoinURI;
import org.bitcoinj.utils.MonetaryFormat;
import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import javax.annotation.Nullable;

import de.schildbach.wallet.ui.ScanActivity;

// Problem with the above is that in the horizontal orientation the tabs don't go in the top bar
public class TabbedMainActivity extends ActionBarActivity implements ActionBar.TabListener, Observer {
    public static final int
            REQUEST_SEND_QR_SCAN = 0,
            REQUEST_SWEEP_PRIVKEY = 1,
            REQUEST_BITCOIN_URL_LOGIN = 2;

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    SectionsPagerAdapter mSectionsPagerAdapter;
    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;
    private Menu menu;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        boolean isBitcoinURL = getIntent().hasCategory(Intent.CATEGORY_BROWSABLE) ||
                NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction());

        if (isBitcoinURL) {
            if (!getGAApp().getConnectionObservable().getState().equals(ConnectivityObservable.State.LOGGEDIN) || getGAApp().getConnectionObservable().getState().equals(ConnectivityObservable.State.LOGGINGIN)) {
                // login required
                final Intent loginActivity = new Intent(this, RequestLoginActivity.class);
                startActivityForResult(loginActivity, REQUEST_BITCOIN_URL_LOGIN);
                return;
            }
        }

        launch(isBitcoinURL);
    }


    @SuppressLint("NewApi") // NdefRecord#toUri disabled for API < 16
    private void launch(boolean isBitcoinURL) {
        setContentView(R.layout.activity_tabbed_main);

        // Set up the action bar.
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        // When swiping between different sections, select the corresponding
        // tab. We can also use ActionBar.Tab#select() to do this if we have
        // a reference to the Tab.
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(final int position) {
                actionBar.setSelectedNavigationItem(position);
                setIdVisible(position == 0, R.id.action_share);
            }
        });


        // For each of the sections in the app, add a tab to the action bar.
        for (int i = 0; i < mSectionsPagerAdapter.getCount(); ++i) {
            // Create a tab with text corresponding to the page title defined by
            // the adapter. Also specify this Activity object, which implements
            // the TabListener interface, as the callback (listener) for when
            // this tab is selected.
            actionBar.addTab(
                    actionBar.newTab()
                            .setText(mSectionsPagerAdapter.getPageTitle(i))
                            .setTabListener(this));
        }
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

        setIdVisible(getGAApp().getConnectionObservable().getState() != ConnectivityObservable.State.LOGGEDIN, R.id.action_share);
    }

    @Override
    public void onPause() {
        super.onPause();
        getGAApp().getConnectionObservable().deleteObserver(this);
    }

    @Override
    public void onTabSelected(final ActionBar.Tab tab, final FragmentTransaction fragmentTransaction) {
        // When the given tab is selected, switch to the corresponding page in
        // the ViewPager.
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(final ActionBar.Tab tab, final FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabReselected(final ActionBar.Tab tab, final FragmentTransaction fragmentTransaction) {
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_SEND_QR_SCAN) {
            if (data != null && data.getStringExtra("com.greenaddress.greenbits.QrText") != null) {
                String scanned = data.getStringExtra("com.greenaddress.greenbits.QrText");
                if (!(scanned.length() >= 8 && scanned.substring(0, 8).equalsIgnoreCase("bitcoin:"))) {
                    scanned = "bitcoin:" + scanned;
                }
                Intent browsable = new Intent(this, TabbedMainActivity.class);
                browsable.setData(Uri.parse(scanned));
                browsable.addCategory(Intent.CATEGORY_BROWSABLE);
                startActivity(browsable);
            }
        } else if (requestCode == REQUEST_SWEEP_PRIVKEY) {
            if (data == null) {
                return;
            }
            ECKey keyNonFinal = null;
            BIP38PrivateKey keyBip38NonFinal = null;
            try {
                keyNonFinal = new DumpedPrivateKey(Network.NETWORK,
                        data.getStringExtra("com.greenaddress.greenbits.QrText")).getKey();
            } catch (final AddressFormatException e) {
                try {
                    keyBip38NonFinal = new BIP38PrivateKey(Network.NETWORK,
                            data.getStringExtra("com.greenaddress.greenbits.QrText"));
                } catch (final AddressFormatException e1) {
                    Toast.makeText(TabbedMainActivity.this, getResources().getString(R.string.invalid_key), Toast.LENGTH_LONG).show();
                    return;
                }

            }
            final ECKey keyNonBip38 = keyNonFinal;
            final BIP38PrivateKey keyBip38 = keyBip38NonFinal;
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
                    if (keyBip38 == null) {
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


                    addressText.setText(address.substring(0, 12) + "\n" + address.substring(12, 24) + "\n" + address.substring(24));

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
                            .callback(new MaterialDialog.ButtonCallback() {

                                Transaction tx;
                                ECKey key;

                                private void doSweep() {
                                    final ArrayList<String> scripts = (ArrayList<String>) result.get("prevout_scripts");
                                    Futures.addCallback(getGAService().verifySpendableBy(
                                            tx.getOutputs().get(0),
                                            Long.valueOf(0),
                                            ((Number) result.get("out_pointer")).longValue()
                                    ), new FutureCallback<Boolean>() {
                                        @Override
                                        public void onSuccess(@Nullable Boolean result) {
                                            if (result.booleanValue()) {
                                                final List<TransactionSignature> signatures = new ArrayList<>();
                                                for (int i = 0; i < tx.getInputs().size(); ++i) {
                                                    signatures.add(tx.calculateSignature(i, key, Hex.decode(scripts.get(i)), Transaction.SigHash.ALL, false));
                                                }
                                                Futures.addCallback(getGAService().sendTransaction(signatures, null), new FutureCallback<String>() {
                                                    @Override
                                                    public void onSuccess(final @Nullable String result) {

                                                    }

                                                    @Override
                                                    public void onFailure(final Throwable t) {
                                                        t.printStackTrace();
                                                        Toast.makeText(TabbedMainActivity.this, t.getMessage(), Toast.LENGTH_LONG).show();
                                                    }
                                                });
                                            } else {
                                                Toast.makeText(TabbedMainActivity.this, "Verification failed: Invalid output address", Toast.LENGTH_LONG).show();
                                            }
                                        }

                                        @Override
                                        public void onFailure(Throwable t) {
                                            t.printStackTrace();
                                            Toast.makeText(TabbedMainActivity.this, t.getMessage(), Toast.LENGTH_LONG).show();
                                        }
                                    });
                                }

                                @Override
                                public void onPositive(final MaterialDialog dialog) {
                                    if (keyBip38 != null) {
                                        try {
                                            key = keyBip38.decrypt(passwordEdit.getText().toString());
                                            Futures.addCallback(getGAService().prepareSweepSocial(
                                                    key.getPubKey(), true), new FutureCallback<Map<?, ?>>() {
                                                @Override
                                                public void onSuccess(@Nullable final Map<?, ?> result) {
                                                    tx = new Transaction(Network.NETWORK,
                                                            Hex.decode((String) result.get("tx")));
                                                    doSweep();
                                                }

                                                @Override
                                                public void onFailure(final Throwable t) {
                                                    Toast.makeText(TabbedMainActivity.this, t.getMessage(), Toast.LENGTH_LONG).show();
                                                }
                                            });
                                        } catch (final BIP38PrivateKey.BadPassphraseException e) {
                                            Toast.makeText(TabbedMainActivity.this, getResources().getString(R.string.invalid_passphrase), Toast.LENGTH_LONG).show();
                                        }

                                    } else {
                                        tx = txNonBip38;
                                        key = keyNonBip38;
                                        doSweep();
                                    }

                                }

                                @Override
                                public void onNegative(final MaterialDialog dialog) {
                                }
                            })
                            .build().show();
                }

                @Override
                public void onFailure(final Throwable t) {
                    Toast.makeText(TabbedMainActivity.this, t.getMessage(), Toast.LENGTH_LONG).show();
                }
            };
            if (keyBip38 == null) {
                Futures.addCallback(getGAService().prepareSweepSocial(
                        keyNonBip38.getPubKey(), false), callback);
            } else {
                callback.onSuccess(null);
            }

        } else if (requestCode == REQUEST_BITCOIN_URL_LOGIN) {
            if (resultCode == RESULT_OK) {
                launch(true);
            } else {
                finish();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        this.menu = menu;

        final String country = getGAService().getCountry();
        if (!Network.NETWORK.getId().equals(NetworkParameters.ID_MAINNET) || country == null ||
                !(country.equals("IT") || country.equals("FR"))) {
            setIdVisible(false, R.id.action_bitboat);
        }

        if (!Network.NETWORK.getId().equals(NetworkParameters.ID_MAINNET)) {
            setIdVisible(false, R.id.action_sweep);
        }

        return true;
    }

    public void setIdVisible(final boolean visible, final int id) {
        if (menu != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    final MenuItem item = menu.findItem(id);
                    item.setVisible(visible);
                }
            });
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        final int id = item.getItemId();
        if (id == R.id.action_settings) {
            final Intent settingsActivity = new Intent(TabbedMainActivity.this, SettingsActivity.class);
            startActivity(settingsActivity);
            return true;
        } else if (id == R.id.action_sweep) {
            final Intent scanner = new Intent(TabbedMainActivity.this, ScanActivity.class);
            startActivityForResult(scanner, REQUEST_SWEEP_PRIVKEY);
            return true;
        } else if (id == R.id.network_unavailable) {
            Toast.makeText(TabbedMainActivity.this, getGAApp().getConnectionObservable().getState().toString(), Toast.LENGTH_LONG).show();
            return true;
        } else if (id == R.id.action_bitboat) {
            startActivity(new Intent(TabbedMainActivity.this, BitBoatActivity.class));
        } else if (id == R.id.action_exit) {
            //FIXME logout and exit logic
            getGAService().disconnect(false);
            finish();
            return true;
        } else if (id == R.id.action_share) {
            final TextView receiveAddress = (TextView) findViewById(R.id.receiveAddressText);
            if (receiveAddress != null) {
                final String address = receiveAddress.getText().toString();
                if (address != null && !address.isEmpty()) {
                    //SHARE intent
                    final Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_TEXT, BitcoinURI.convertToBitcoinURI(address.replace("\n", "").replace("\r", ""), null, null, null));
                    sendIntent.setType("text/plain");
                    startActivity(sendIntent);
                }
            }

            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void testKickedOut() {
        if (getGAApp().getConnectionObservable().getIsForcedLoggedOut() || getGAApp().getConnectionObservable().getIsForcedTimeout()) {
            // FIXME: Should pass flag to activity so it shows it was forced logged out
            final Intent firstScreenActivity = new Intent(TabbedMainActivity.this, FirstScreenActivity.class);
            startActivity(firstScreenActivity);
            finish();
        }
    }

    private Boolean exit = false;
    @Override
    public void onBackPressed() {
        if (exit) {
            finish(); // finish activity
        } else {
            Toast.makeText(this, "Press Back again to Exit.",
                    Toast.LENGTH_SHORT).show();
            exit = true;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    exit = false;
                }
            }, 3 * 1000);

        }

    }

    @Override
    public void update(final Observable observable, final Object data) {
        if (getGAApp().getConnectionObservable().getIsForcedLoggedOut() || getGAApp().getConnectionObservable().getIsForcedTimeout()) {
            // FIXME: Should pass flag to activity so it shows it was forced logged out
            final Intent firstScreenActivity = new Intent(TabbedMainActivity.this, FirstScreenActivity.class);
            startActivity(firstScreenActivity);
        }
        final ConnectivityObservable.State currentState = getGAApp().getConnectionObservable().getState();
        setIdVisible(currentState != ConnectivityObservable.State.LOGGEDIN, R.id.network_unavailable);
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(final FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(final int index) {

            switch (index) {
                case 0:
                    setIdVisible(true, R.id.action_share);
                    return new ReceiveFragment();
                case 1:
                    setIdVisible(false, R.id.action_share);
                    return new MainFragment();
                case 2:
                    setIdVisible(false, R.id.action_share);
                    return new SendFragment();
            }

            return null;
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }

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
