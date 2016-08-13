package com.greenaddress.greenbits.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
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

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.blockstream.libwally.Wally;
import com.google.common.util.concurrent.FutureCallback;
import com.greenaddress.greenapi.Network;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import de.schildbach.wallet.ui.ScanActivity;

// Problem with the above is that in the horizontal orientation the tabs don't go in the top bar
public class TabbedMainActivity extends GaActivity implements Observer {
    private static final int
            REQUEST_SEND_QR_SCAN = 0,
            REQUEST_SWEEP_PRIVKEY = 1,
            REQUEST_BITCOIN_URL_LOGIN = 2,
            REQUEST_SETTINGS = 3,
            REQUEST_TX_DETAILS = 4;
    private ViewPager mViewPager;
    private Menu mMenu;
    private Observer mTwoFactorObserver;

    @Override
    protected void onCreateWithService(final Bundle savedInstanceState) {
        final GaService service = mService;

        boolean isBitcoinURL = getIntent().hasCategory(Intent.CATEGORY_BROWSABLE) ||
                NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction()) ||
                (getIntent().getData() != null && getIntent().getData().getScheme() != null
                        && getIntent().getData().getScheme().equals("bitcoin"));

        if (isBitcoinURL && service.isLoggedOrLoggingIn()) {
            // already logged in, could be from different app via intent
            final Intent loginActivity = new Intent(this, RequestLoginActivity.class);
            startActivityForResult(loginActivity, REQUEST_BITCOIN_URL_LOGIN);
            return;
        }

        launch(isBitcoinURL);
    }

    private void onTwoFactorConfigChange() {
        final GaService service = mService;

        final Map<?, ?> twoFacConfig = service.getTwoFacConfig();
        if (twoFacConfig == null)
            return;

        if (!((Boolean) twoFacConfig.get("any")) &&
            !service.cfg().getBoolean("hideTwoFacWarning", false)) {
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

    private void configureSubaccountsFooter(final int subAccount) {
        final GaService service = mService;

        if (!service.haveSubaccounts())
            return;

        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        UI.show(fab);

        final String subAccountName;
        final Map<String, ?> m = service.findSubaccount(null, subAccount);
        if (m == null)
            subAccountName = getResources().getText(R.string.main_account).toString();
        else
            subAccountName = (String) m.get("name");
        setTitle(String.format("%s %s", getResources().getText(R.string.app_name), subAccountName));


        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                final ArrayList<String> names = new ArrayList<>();
                final ArrayList<Integer> pointers = new ArrayList<>();

                names.add(getResources().getText(R.string.main_account).toString());
                pointers.add(0);

                for (final Object s : service.getSubaccounts()) {
                    final Map<String, ?> m = (Map) s;
                    names.add((String) m.get("name"));
                    pointers.add((Integer) m.get("pointer"));
                }

                final MaterialDialog.ListCallback lcb = new MaterialDialog.ListCallback() {
                    @Override
                    public void onSelection(final MaterialDialog dialog, final View view, final int which, final CharSequence text) {

                        final int subAccount = pointers.get(which);
                        if (subAccount != service.getCurrentSubAccount()) {
                            setTitle(String.format("%s %s", getResources().getText(R.string.app_name), text));
                            onSubaccountUpdate(subAccount);
                        }
                    }
                };

                new MaterialDialog.Builder(TabbedMainActivity.this)
                        .title(R.string.footerAccount)
                        .items(names)
                        .autoDismiss(true)
                        .itemsCallback(lcb).show();
            }
        });
    }

    private void onSubaccountUpdate(final int subAccount) {
        final GaService service = mService;
        service.setCurrentSubAccount(subAccount);

        final Intent data = new Intent("fragmentupdater");
        data.putExtra("sub", subAccount);
        sendBroadcast(data);
    }

    @SuppressLint("NewApi") // NdefRecord#toUri disabled for API < 16
    private void launch(boolean isBitcoinURL) {
        final GaService service = mService;

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

        // Re-show our 2FA warning if config is changed to remove all methods
        mTwoFactorObserver = new Observer() {
            @Override
            public void update(final Observable o, final Object data) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onTwoFactorConfigChange();
                    }
                });
            }
        };
        // Fake a config change to show the warning if no current 2FA method
        mTwoFactorObserver.update(null, null);

        configureSubaccountsFooter(service.getCurrentSubAccount());

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
    public void onResumeWithService() {
        final GaService service = mService;
        service.addConnectionObserver(this);
        service.addTwoFactorObserver(mTwoFactorObserver);

        if (service.isForcedOff()) {
            // FIXME: Should pass flag to activity so it shows it was forced logged out
            startActivity(new Intent(this, FirstScreenActivity.class));
            finish();
            return;
        }

        setMenuItemVisible(mMenu, R.id.action_share, !service.isLoggedIn());
     }

    @Override
    public void onPauseWithService() {
        final GaService service = mService;
        service.deleteTwoFactorObserver(mTwoFactorObserver);
        service.deleteConnectionObserver(this);
    }

    private final static int BIP38_FLAGS = (NetworkParameters.fromID(NetworkParameters.ID_MAINNET).equals(Network.NETWORK)
            ? Wally.BIP38_KEY_MAINNET : Wally.BIP38_KEY_TESTNET) | Wally.BIP38_KEY_COMPRESSED;

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        final GaService service = mService;

        super.onActivityResult(requestCode, resultCode, data);

        final TabbedMainActivity caller = TabbedMainActivity.this;

        switch (requestCode) {
            case REQUEST_TX_DETAILS:
            case REQUEST_SETTINGS:
                service.updateBalance(service.getCurrentSubAccount());
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
                } catch (final AddressFormatException e) {
                    try {
                        Wally.bip38_to_private_key(qrText, null, Wally.BIP38_KEY_COMPRESSED | Wally.BIP38_KEY_QUICK_CHECK);
                    } catch (final IllegalArgumentException e2) {
                        toast(R.string.invalid_key);
                        return;
                    }
                }
                final ECKey keyNonBip38 = keyNonFinal;
                final FutureCallback<Map<?, ?>> callback = new CB.Toast<Map<?, ?>>(caller) {
                    @Override
                    public void onSuccess(final Map<?, ?> sweepResult) {
                        final View inflatedLayout = getLayoutInflater().inflate(R.layout.dialog_sweep_address, null, false);
                        final TextView passwordPrompt = (TextView) inflatedLayout.findViewById(R.id.sweepAddressPasswordPromptText);
                        final TextView mainText = (TextView) inflatedLayout.findViewById(R.id.sweepAddressMainText);
                        final TextView addressText = (TextView) inflatedLayout.findViewById(R.id.sweepAddressAddressText);
                        final EditText passwordEdit = (EditText) inflatedLayout.findViewById(R.id.sweepAddressPasswordText);
                        final Transaction txNonBip38;
                        final String address;

                        if (keyNonBip38 != null) {
                            UI.hide(passwordPrompt, passwordEdit);
                            txNonBip38 = getSweepTx(sweepResult);
                            final MonetaryFormat format;
                            format = CurrencyMapper.mapBtcUnitToFormat( (String) service.getUserConfig("unit"));
                            Coin outputsValue = Coin.ZERO;
                            for (final TransactionOutput output : txNonBip38.getOutputs()) {
                                outputsValue = outputsValue.add(output.getValue());
                            }
                            mainText.setText(Html.fromHtml("Are you sure you want to sweep <b>all</b> ("
                                    + format.postfixCode().format(outputsValue) + ") funds from the address below?"));
                            address = keyNonBip38.toAddress(Network.NETWORK).toString();
                        } else {
                            passwordPrompt.setText(R.string.sweep_bip38_passphrase_prompt);
                            txNonBip38 = null;
                            // amount not known until decrypted
                            mainText.setText(Html.fromHtml("Are you sure you want to sweep <b>all</b> funds from the password protected BIP38 key below?"));
                            address = data.getStringExtra("com.greenaddress.greenbits.QrText");
                        }


                        addressText.setText(String.format("%s\n%s\n%s", address.substring(0, 12), address.substring(12, 24), address.substring(24)));

                        UI.popup(caller, R.string.sweepAddressTitle, R.string.sweep, R.string.cancel)
                            .customView(inflatedLayout, true)
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                Transaction tx;
                                ECKey key;

                                private void doSweep() {
                                    final ArrayList<String> scripts = (ArrayList<String>) sweepResult.get("prevout_scripts");
                                    final Integer outPointer = (Integer) sweepResult.get("out_pointer");
                                    CB.after(service.verifySpendableBy(tx.getOutputs().get(0), 0, outPointer),
                                             new CB.Toast<Boolean>(caller) {
                                        @Override
                                        public void onSuccess(final Boolean isSpendable) {
                                            if (!isSpendable) {
                                                caller.toast(R.string.err_tabbed_sweep_failed);
                                                return;
                                            }
                                            final List<byte[]> signatures = new ArrayList<>();
                                            for (int i = 0; i < tx.getInputs().size(); ++i) {
                                                final byte[] script = Wally.hex_to_bytes(scripts.get(i));
                                                final TransactionSignature sig;
                                                sig = tx.calculateSignature(i, key, script, Transaction.SigHash.ALL, false);
                                                signatures.add(sig.encodeToBitcoin());
                                            }
                                            CB.after(service.sendTransaction(signatures),
                                                     new CB.Toast<String>(caller) { });
                                        }
                                    });
                                }

                                @Override
                                public void onClick(final MaterialDialog dialog, final DialogAction which) {
                                    if (keyNonBip38 != null) {
                                        tx = txNonBip38;
                                        key = keyNonBip38;
                                        doSweep();
                                        return;
                                    }
                                    try {
                                        final String password = passwordEdit.getText().toString();
                                        final byte[] passbytes = password.getBytes();
                                        final byte[] decryptedPKey = Wally.bip38_to_private_key(qrText, passbytes, BIP38_FLAGS);
                                        key = ECKey.fromPrivate(decryptedPKey);

                                        CB.after(service.prepareSweepSocial(key.getPubKey(), true),
                                                 new CB.Toast<Map<?, ?>>(caller) {
                                            @Override
                                            public void onSuccess(final Map<?, ?> sweepResult) {
                                                tx = getSweepTx(sweepResult);
                                                doSweep();
                                            }
                                        });
                                    } catch (final IllegalArgumentException e) {
                                        caller.toast(R.string.invalid_passphrase);
                                    }
                                }
                            }).build().show();
                    }
                };
                if (keyNonBip38 != null) {
                    CB.after(service.prepareSweepSocial(keyNonBip38.getPubKey(), false), callback);
                } else {
                    callback.onSuccess(null);
                }
                break;
        }
    }

    private Transaction getSweepTx(final Map<?, ?> sweepResult) {
        return new Transaction(Network.NETWORK, Wally.hex_to_bytes((String) sweepResult.get("tx")));
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        if (mService.isWatchOnly())
            getMenuInflater().inflate(R.menu.watchonly, menu);
        else
            getMenuInflater().inflate(R.menu.main, menu);
        mMenu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final GaService service = mService;

        final TabbedMainActivity caller = TabbedMainActivity.this;

        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivityForResult(new Intent(caller, SettingsActivity.class), REQUEST_SETTINGS);
                return true;
            case R.id.action_sweep:
                final Intent scanner = new Intent(caller, ScanActivity.class);
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
                return true;
            case R.id.action_logout:
                service.disconnect(false);
                finish();
                return true;
            case R.id.action_network:
                startActivity(new Intent(caller, NetworkMonitorActivity.class));
                return true;
            case R.id.action_about:
                startActivity(new Intent(caller, AboutActivity.class));
                return true;

        }
        return super.onOptionsItemSelected(item);
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
        final GaService.State state = (GaService.State) data;
        if (state.isForcedOff()) {
            // FIXME: Should pass flag to activity so it shows it was forced logged out
            startActivity(new Intent(this, FirstScreenActivity.class));
        }
        setMenuItemVisible(mMenu, R.id.network_unavailable, !state.isLoggedIn());
    }

    private void handlePermissionResult(final int[] granted, int action, int msgId) {
        if (granted[0] == PackageManager.PERMISSION_GRANTED)
            startActivityForResult(new Intent(this, ScanActivity.class), action);
        else
            shortToast(msgId);
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, final String[] permissions, final int[] granted) {
        if (requestCode == 200)
                handlePermissionResult(granted, REQUEST_SWEEP_PRIVKEY,
                                       R.string.err_tabbed_sweep_requires_camera_permissions);
        else if (requestCode == 100)
                handlePermissionResult(granted, REQUEST_SEND_QR_SCAN,
                                       R.string.err_qrscan_requires_camera_permissions);
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
            if (mService.isWatchOnly())
                return 2;
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
