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
import android.util.Log;
import android.view.ViewGroup;
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
import com.greenaddress.greenbits.ui.preferences.TwoFactorPreferenceFragment;

import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.DumpedPrivateKey;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.params.MainNetParams;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import de.schildbach.wallet.ui.ScanActivity;

// Problem with the above is that in the horizontal orientation the tabs don't go in the top bar
public class TabbedMainActivity extends GaActivity implements Observer, View.OnClickListener {

    private static final String TAG = TabbedMainActivity.class.getSimpleName();

    private static final boolean IS_MAINNET = Network.NETWORK == MainNetParams.get();
    private static final int BIP32_NETWORK = IS_MAINNET ? Wally.BIP38_KEY_MAINNET : Wally.BIP38_KEY_TESTNET;
    private static final int BIP38_FLAGS = BIP32_NETWORK | Wally.BIP38_KEY_COMPRESSED;

    public static final int
            REQUEST_SEND_QR_SCAN = 0,
            REQUEST_SWEEP_PRIVKEY = 1,
            REQUEST_BITCOIN_URL_LOGIN = 2,
            REQUEST_SETTINGS = 3,
            REQUEST_TX_DETAILS = 4,
            REQUEST_SEND_QR_SCAN_EXCHANGER = 5;
    private ViewPager mViewPager;
    private Menu mMenu;
    private Boolean mInternalQr = false;
    private String mSendAmount;
    private MaterialDialog mSegwitDialog;
    private MaterialDialog mSubaccountDialog;
    private FloatingActionButton mSubaccountButton;

    private final Runnable mSegwitCB = new Runnable() { public void run() { mSegwitDialog = null; } };
    private final Runnable mSubaccountCB = new Runnable() { public void run() { mDialogCB.run(); mSubaccountDialog = null; } };
    private final Runnable mDialogCB = new Runnable() { public void run() { setBlockWaitDialog(false); } };

    private final Observer mTwoFactorObserver = new Observer() {
        @Override
        public void update(final Observable o, final Object data) {
            runOnUiThread(new Runnable() { public void run() { onTwoFactorConfigChange(); } });
        }
    };

    static boolean isBitcoinScheme(final Intent intent) {
        final Uri uri = intent.getData();
        return uri != null && uri.getScheme() != null && uri.getScheme().equals("bitcoin");
    }

    @Override
    protected void onCreateWithService(final Bundle savedInstanceState) {
        final Intent intent = getIntent();
        mInternalQr = intent.getBooleanExtra("internal_qr", false);
        mSendAmount = intent.getStringExtra("sendAmount");
        final boolean isBitcoinUri = isBitcoinScheme(intent) ||
                                     intent.hasCategory(Intent.CATEGORY_BROWSABLE) ||
                                     NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction());

        if (isBitcoinUri && !mService.isLoggedOrLoggingIn()) {
            // Not logged in, force the user to login
            final Intent login = new Intent(this, RequestLoginActivity.class);
            startActivityForResult(login, REQUEST_BITCOIN_URL_LOGIN);
            return;
        }

        launch(isBitcoinUri);
    }

    private void showWarningBanner(final int messageId, final String hideCfgName) {
        if (mService.cfg().getBoolean(hideCfgName, false))
            return;

        final Snackbar snackbar = Snackbar
                .make(findViewById(R.id.main_content), getString(messageId), Snackbar.LENGTH_INDEFINITE)
                .setActionTextColor(Color.RED)
                .setAction(getString(R.string.set2FA), new View.OnClickListener() {
                    @Override
                    public void onClick(final View v) {
                        final Intent intent = new Intent(TabbedMainActivity.this, SettingsActivity.class);
                        intent.putExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT, TwoFactorPreferenceFragment.class.getName());
                        startActivityForResult(intent, REQUEST_SETTINGS);
                    }
                });

        final View snackbarView = snackbar.getView();
        snackbarView.setBackgroundColor(Color.DKGRAY);
        final TextView textView = UI.find(snackbarView, android.support.design.R.id.snackbar_text);
        textView.setTextColor(Color.WHITE);
        snackbar.show();
    }

    private void onTwoFactorConfigChange() {
        if (mService.getTwoFactorConfig() == null)
            return; // Not loaded

        if (!mService.hasAnyTwoFactor())
            showWarningBanner(R.string.noTwoFactorWarning, "hideTwoFacWarning");

        if (mService.getEnabledTwoFactorMethods().size() == 1)
            showWarningBanner(R.string.singleTwoFactorWarning, "hideSingleTwoFacWarning");
    }

    private String formatValuePostfix(final Coin value) {
        final String formatted = UI.setCoinText(mService, null, null, value);
        return String.format("%s %s", formatted, mService.getBitcoinUnit());
    }

    private void setAccountTitle(final int subAccount) {
        final boolean doLookup = subAccount != 0 && mService.haveSubaccounts();
        final Map<String, ?> details;
        details = doLookup ? mService.findSubaccount(subAccount) : null;
        final String accountName;
        if (details == null)
            accountName = getString(R.string.main_account);
        else
            accountName = (String) details.get("name");

        if (!mService.showBalanceInTitle()) {
            setTitle(accountName);
            return;
        }
        Coin balance = mService.getCoinBalance(subAccount);
        if (balance == null)
            balance = Coin.ZERO;
        setTitle(formatValuePostfix(balance) + " (" + accountName + ')');
    }

    private void setBlockWaitDialog(final boolean doBlock) {
        getPagerAdapter().setBlockWaitDialog(doBlock);
    }

    private void configureSubaccountsFooter(final int subAccount) {
        setAccountTitle(subAccount);
        if (!mService.haveSubaccounts())
            return;

        mSubaccountButton = UI.find(this, R.id.fab);
        UI.show(mSubaccountButton);
        mSubaccountButton.setOnClickListener(this);
    }

    @Override
    public void onClick(final View v) {
        if (v == mSubaccountButton)
            onSubaccountButtonClicked();
    }

    public void onSubaccountButtonClicked() {
        setBlockWaitDialog(true);
        final ArrayList subaccounts = mService.getSubaccounts();
        final int subaccount_len = subaccounts.size() + 1;
        final ArrayList<String> names = new ArrayList<>(subaccount_len);
        final ArrayList<Integer> pointers = new ArrayList<>(subaccount_len);

        names.add(getString(R.string.main_account));
        pointers.add(0);

        for (final Object s : subaccounts) {
            final Map<String, ?> m = (Map) s;
            names.add((String) m.get("name"));
            pointers.add((Integer) m.get("pointer"));
        }

        final AccountItemAdapter adapter = new AccountItemAdapter(names, pointers, mService);
        mSubaccountDialog = new MaterialDialog.Builder(TabbedMainActivity.this)
                .title(R.string.footerAccount)
                .adapter(adapter, null)
                .show();
        UI.setDialogCloseHandler(mSubaccountDialog, mSubaccountCB);

        adapter.setCallback(new AccountItemAdapter.OnAccountSelected() {
            @Override
            public void onAccountSelected(final int account) {
                mSubaccountDialog = UI.dismiss(TabbedMainActivity.this, mSubaccountDialog);
                final int pointer = pointers.get(account);
                if (pointer == mService.getCurrentSubAccount())
                    return;
                setAccountTitle(pointer);
                onSubaccountUpdate(pointer);
            }
        });
    }

    private void onSubaccountUpdate(final int subAccount) {
        mService.setCurrentSubAccount(subAccount);

        final Intent data = new Intent("fragmentupdater");
        data.putExtra("sub", subAccount);
        sendBroadcast(data);
    }

    @SuppressLint("NewApi") // NdefRecord#toUri disabled for API < 16
    private void launch(final boolean isBitcoinUri) {

        setContentView(R.layout.activity_tabbed_main);
        final Toolbar toolbar = UI.find(this, R.id.toolbar);
        setSupportActionBar(toolbar);

        // Set up the action bar.
        final SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = UI.find(this, R.id.container);

        // Keep all of our tabs in memory while paging. This helps any races
        // left where broadcasts/callbacks are called on the pager when its not
        // shown.
        mViewPager.setOffscreenPageLimit(3);

        // Re-show our 2FA warning if config is changed to remove all methods
        // Fake a config change to show the warning if no current 2FA method
        mTwoFactorObserver.update(null, null);

        configureSubaccountsFooter(mService.getCurrentSubAccount());

        // by default go to center tab
        int goToTab = 1;

        if (isBitcoinUri) {
            // go to send page tab
            goToTab = 2;

            // Started by clicking on a bitcoin URI, show the send tab initially.
            if (!NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
                mViewPager.setTag(R.id.tag_bitcoin_uri, getIntent().getData());
            } else {
                // NdefRecord#toUri not available in API < 16
                if (Build.VERSION.SDK_INT > 16) {
                    final Parcelable[] rawMessages;
                    rawMessages = getIntent().getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
                    for (final Parcelable parcel : rawMessages) {
                        final NdefMessage ndefMsg = (NdefMessage) parcel;
                        for (final NdefRecord record : ndefMsg.getRecords())
                            if (record.getTnf() == NdefRecord.TNF_WELL_KNOWN &&
                                    Arrays.equals(record.getType(), NdefRecord.RTD_URI)) {
                                mViewPager.setTag(R.id.tag_bitcoin_uri, record.toUri());
                            }
                    }
                }
            }
            // if arrives from internal QR scan
            if (mInternalQr) {
                mViewPager.setTag(R.id.internal_qr, "internal_qr");
            }
            if (mSendAmount != null) {
                mViewPager.setTag(R.id.tag_amount, mSendAmount);
            }
            mInternalQr = false;
            mSendAmount = null;
        }

        // set adapter and tabs only after all setTag in ViewPager container
        mViewPager.setAdapter(sectionsPagerAdapter);
        mViewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(final int index) {
                sectionsPagerAdapter.onViewPageSelected(index);
            }
        });
        final TabLayout tabLayout = UI.find(this, R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

        mViewPager.setCurrentItem(goToTab);

        final boolean segwit = mService.getLoginData().get("segwit_server");
        if (segwit && mService.isSegwitUnconfirmed()) {
            // The user has not yet enabled segwit. Opt them in and display
            // a dialog explaining how to opt-out.
            CB.after(mService.setUserConfig("use_segwit", true, false),
                     new CB.Toast<Boolean>(this) {
                @Override
                public void onSuccess(final Boolean result) {
                    TabbedMainActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            mSegwitDialog = UI.popup(TabbedMainActivity.this, R.string.segwit_dialog_title, 0)
                                              .content(R.string.segwit_dialog_content).build();
                            UI.setDialogCloseHandler(mSegwitDialog, mSegwitCB);
                            mSegwitDialog.show();
                        }
                    });
                }
            });
        }
    }

    @Override
    public void onResumeWithService() {
        mService.addConnectionObserver(this);
        mService.addTwoFactorObserver(mTwoFactorObserver);

        final SectionsPagerAdapter adapter = getPagerAdapter();

        if (adapter == null || mService.isForcedOff()) {
            // FIXME: Should pass flag to activity so it shows it was forced logged out
            startActivity(new Intent(this, FirstScreenActivity.class));
            finish();
            return;
        }

        setMenuItemVisible(mMenu, R.id.action_share,
                           adapter != null && adapter.mSelectedPage == 0);
     }

    @Override
    public void onPauseWithService() {
        mService.deleteTwoFactorObserver(mTwoFactorObserver);
        mService.deleteConnectionObserver(this);
        mSubaccountDialog = UI.dismiss(this, mSubaccountDialog);
        mSegwitDialog = UI.dismiss(this, mSegwitDialog);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        UI.unmapClick(mSubaccountButton);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        final TabbedMainActivity caller = TabbedMainActivity.this;

        switch (requestCode) {
            case REQUEST_TX_DETAILS:
            case REQUEST_SETTINGS:
                mService.updateBalance(mService.getCurrentSubAccount());
                startActivity(new Intent(this, TabbedMainActivity.class));
                finish();
                break;
            case REQUEST_BITCOIN_URL_LOGIN:
                if (resultCode != RESULT_OK) {
                    // The user failed to login after clicking on a bitcoin Uri
                    finish();
                    return;
                }
                final boolean isBitcoinUri = true;
                launch(isBitcoinUri);
                break;
            case REQUEST_SWEEP_PRIVKEY:
                if (data == null)
                    return;
                ECKey keyNonFinal = null;
                final String qrText = data.getStringExtra(ScanActivity.INTENT_EXTRA_RESULT);
                try {
                    keyNonFinal = DumpedPrivateKey.fromBase58(Network.NETWORK,
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
                        final View v = getLayoutInflater().inflate(R.layout.dialog_sweep_address, null, false);
                        final TextView passwordPrompt = UI.find(v, R.id.sweepAddressPasswordPromptText);
                        final TextView mainText = UI.find(v, R.id.sweepAddressMainText);
                        final TextView addressText = UI.find(v, R.id.sweepAddressAddressText);
                        final EditText passwordEdit = UI.find(v, R.id.sweepAddressPasswordText);
                        final Transaction txNonBip38;
                        final String address;

                        if (keyNonBip38 != null) {
                            UI.hide(passwordPrompt, passwordEdit);
                            txNonBip38 = getSweepTx(sweepResult);
                            Coin outputsValue = Coin.ZERO;
                            for (final TransactionOutput output : txNonBip38.getOutputs())
                                outputsValue = outputsValue.add(output.getValue());
                            final String valueStr = formatValuePostfix(outputsValue);
                            mainText.setText(Html.fromHtml("Are you sure you want to sweep <b>all</b> ("
                                    + valueStr + ") funds from the address below?"));
                            address = keyNonBip38.toAddress(Network.NETWORK).toString();
                        } else {
                            passwordPrompt.setText(R.string.sweep_bip38_passphrase_prompt);
                            txNonBip38 = null;
                            // amount not known until decrypted
                            mainText.setText(Html.fromHtml("Are you sure you want to sweep <b>all</b> funds from the password protected BIP38 key below?"));
                            address = data.getStringExtra(ScanActivity.INTENT_EXTRA_RESULT);
                        }


                        addressText.setText(String.format("%s\n%s\n%s", address.substring(0, 12), address.substring(12, 24), address.substring(24)));

                        final MaterialDialog.Builder builder = UI.popup(caller, R.string.sweepAddressTitle, R.string.sweep, R.string.cancel)
                            .customView(v, true)
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                Transaction tx;
                                ECKey key;

                                private void doSweep() {
                                    final ArrayList<String> scripts = (ArrayList) sweepResult.get("prevout_scripts");
                                    final Integer outPointer = (Integer) sweepResult.get("out_pointer");
                                    CB.after(mService.verifySpendableBy(tx.getOutputs().get(0), 0, outPointer),
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
                                            CB.after(mService.sendTransaction(signatures, null),
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
                                        final String password = UI.getText(passwordEdit);
                                        final byte[] passbytes = password.getBytes();
                                        final byte[] decryptedPKey = Wally.bip38_to_private_key(qrText, passbytes, BIP38_FLAGS);
                                        key = ECKey.fromPrivate(decryptedPKey);

                                        CB.after(mService.prepareSweepSocial(key.getPubKey(), true),
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
                            });

                        runOnUiThread(new Runnable() { public void run() { builder.build().show(); } });
                    }
                };
                if (keyNonBip38 != null)
                    CB.after(mService.prepareSweepSocial(keyNonBip38.getPubKey(), true), callback);
                else
                    callback.onSuccess(null);
                break;
        }
    }

    private Transaction getSweepTx(final Map<?, ?> sweepResult) {
        return GaService.buildTransaction((String) sweepResult.get("tx"));
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        final int id = mService.isWatchOnly() ? R.menu.watchonly : R.menu.main;
        getMenuInflater().inflate(id, menu);

        setMenuItemVisible(menu, R.id.action_network,
                           !GaService.IS_ELEMENTS && mService.isSPVEnabled());
        setMenuItemVisible(menu, R.id.action_sweep, !GaService.IS_ELEMENTS);

        final boolean isExchanger = mService.cfg().getBoolean("show_exchanger_menu", false);
        setMenuItemVisible(menu, R.id.action_exchanger, isExchanger);

        mMenu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {

        final TabbedMainActivity caller = TabbedMainActivity.this;

        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivityForResult(new Intent(caller, SettingsActivity.class), REQUEST_SETTINGS);
                return true;
            case R.id.action_exchanger:
                startActivity(new Intent(caller, MainExchanger.class));
                return true;
            case R.id.action_sweep:
                final Intent scanner = new Intent(caller, ScanActivity.class);
                //New Marshmallow permissions paradigm
                final String[] perms = {"android.permission.CAMERA"};
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1 &&
                        checkSelfPermission(perms[0]) != PackageManager.PERMISSION_GRANTED)
                    requestPermissions(perms, /*permsRequestCode*/ 200);
                else
                    startActivityForResult(scanner, REQUEST_SWEEP_PRIVKEY);
                return true;
            case R.id.network_unavailable:
                return true;
            case R.id.action_share:
                getPagerAdapter().onOptionsItemSelected(item);
                return true;
            case R.id.action_logout:
                mService.disconnect(false);
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
        if (mViewPager.getCurrentItem() == 1)
            finish();
        else
            mViewPager.setCurrentItem(1);
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

    @Override
    public void onRequestPermissionsResult(final int requestCode, final String[] permissions, final int[] granted) {
        if (requestCode == 200 &&
            isPermissionGranted(granted, R.string.err_tabbed_sweep_requires_camera_permissions))
            startActivityForResult(new Intent(this, ScanActivity.class), REQUEST_SWEEP_PRIVKEY);
        else if (requestCode == 100 &&
                 isPermissionGranted(granted, R.string.err_qrscan_requires_camera_permissions))
            startActivityForResult(new Intent(this, ScanActivity.class), REQUEST_SEND_QR_SCAN);
    }

    SectionsPagerAdapter getPagerAdapter() {
        if (mViewPager == null)
            return null;
        return (SectionsPagerAdapter) mViewPager.getAdapter();
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {
        private final SubaccountFragment[] mFragments = new SubaccountFragment[3];
        public int mSelectedPage = -1;
        private int mInitialSelectedPage = -1;
        private boolean mInitialPage = true;

        public SectionsPagerAdapter(final FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(final int index) {
            Log.d(TAG, "SectionsPagerAdapter -> getItem " + index);
            switch (index) {
                case 0: return new ReceiveFragment();
                case 1: return new MainFragment();
                case 2: return new SendFragment();
            }
            return null;
        }

        @Override
        public Object instantiateItem(final ViewGroup container, final int index) {
            Log.d(TAG, "SectionsPagerAdapter -> instantiateItem " + index);

            mFragments[index] = (SubaccountFragment) super.instantiateItem(container, index);

            if (mInitialPage && index == mInitialSelectedPage) {
                // Call setPageSelected() on the first page, now that it is created
                Log.d(TAG, "SectionsPagerAdapter -> selecting first page " + index);
                mFragments[index].setPageSelected(true);
                mInitialSelectedPage = -1;
                mInitialPage = false;
            }
            return mFragments[index];
        }

        @Override
        public void destroyItem(final ViewGroup container, final int index, final Object object) {
            Log.d(TAG, "SectionsPagerAdapter -> destroyItem " + index);
            if (index >=0 && index <=2 && mFragments[index] != null) {
                // Make sure the fragment is not kept alive and does not
                // try to process any callbacks it registered for.
                mFragments[index].detachObservers();
                // Make sure any wait dialog being shown is dismissed
                mFragments[index].setPageSelected(false);
                mFragments[index] = null;
            }
            super.destroyItem(container, index, object);
        }

        @Override
        public int getCount() {
            // We don't show the send tab in watch only mode
            return mService.isWatchOnly() ? 2 : 3;
        }

        @Override
        public CharSequence getPageTitle(final int index) {
            final Locale l = Locale.getDefault();
            switch (index) {
                case 0: return getString(R.string.receive_title).toUpperCase(l);
                case 1: return getString(R.string.main_title).toUpperCase(l);
                case 2: return getString(R.string.send_title).toUpperCase(l);
            }
            return null;
        }

        public void onViewPageSelected(final int index) {
            Log.d(TAG, "SectionsPagerAdapter -> onViewPageSelected " + index +
                       " current is " + mSelectedPage + " initial " + mInitialPage);

            if (mInitialPage)
                mInitialSelectedPage = index; // Store so we can notify it when constructed

            if (index == mSelectedPage)
                return; // No change to the selected page

            // Un-select any old selected page
            if (mSelectedPage != -1 && mFragments[mSelectedPage] != null)
                mFragments[mSelectedPage].setPageSelected(false);

            // Select the current page
            mSelectedPage = index;
            if (mFragments[mSelectedPage] != null)
                mFragments[mSelectedPage].setPageSelected(true);

            setMenuItemVisible(mMenu, R.id.action_share, mSelectedPage == 0);
        }

        public void setBlockWaitDialog(final boolean doBlock) {
            for (final SubaccountFragment fragment : mFragments)
                if (fragment != null)
                    fragment.setBlockWaitDialog(doBlock);
        }

        public void onOptionsItemSelected(final MenuItem item) {
            if (item.getItemId() == R.id.action_share)
                if (mSelectedPage == 0 && mFragments[0] != null)
                    mFragments[0].onShareClicked();
        }
    }
}
