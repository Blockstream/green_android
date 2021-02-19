package com.greenaddress.greenbits.ui;

import android.content.Intent;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentTransaction;

import com.afollestad.materialdialogs.MaterialDialog;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.greenaddress.gdk.GDKTwoFactorCall;
import com.greenaddress.greenapi.data.BalanceData;
import com.greenaddress.greenbits.ui.accounts.SwitchNetworkFragment;
import com.greenaddress.greenbits.ui.authentication.RequestLoginActivity;
import com.greenaddress.greenbits.ui.notifications.NotificationsActivity;
import com.greenaddress.greenbits.ui.preferences.PrefKeys;
import com.greenaddress.greenbits.ui.preferences.SettingsActivity;
import com.greenaddress.greenbits.ui.send.SendAmountActivity;
import com.greenaddress.greenbits.ui.transactions.MainFragment;
import com.greenaddress.greenbits.wallets.HardwareCodeResolver;

import java.util.Arrays;

import static com.greenaddress.greenapi.Session.getSession;

// Problem with the above is that in the horizontal orientation the tabs don't go in the top bar
public class TabbedMainActivity extends LoggedActivity  {

    private static final String TAG = TabbedMainActivity.class.getSimpleName();

    public static final int
        REQUEST_BITCOIN_URL_LOGIN = 1,
        REQUEST_TX_DETAILS = 2,
        REQUEST_BITCOIN_URL_SEND = 3,
        REQUEST_SELECT_ASSET = 4,
        REQUEST_SELECT_SUBACCOUNT = 5;

    private MaterialDialog mSubaccountDialog;
    private boolean mIsBitcoinUri = false;

    static boolean isBitcoinScheme(final Intent intent) {
        final Uri uri = intent.getData();
        return uri != null && uri.getScheme() != null && uri.getScheme().equals("bitcoin");
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Intent intent = getIntent();
        mIsBitcoinUri = isBitcoinScheme(intent) ||
                        intent.hasCategory(Intent.CATEGORY_BROWSABLE) ||
                        NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction());

        if (mIsBitcoinUri) {
            // Not logged in, force the user to login
            final Intent login = new Intent(this, RequestLoginActivity.class);
            startActivityForResult(login, REQUEST_BITCOIN_URL_LOGIN);
            return;
        }
        launch();
        final boolean isResetActive = getSession().isTwoFAReset();
        if (mIsBitcoinUri && !isResetActive) {
            // If logged in, open send activity
            onBitcoinUri();
        }

        if(savedInstanceState == null){
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.add(R.id.container, new MainFragment()).commit();
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);

        // Update notification Icon
        MenuItem notificationMenuItem = menu.findItem(R.id.action_notifications);
        notificationMenuItem.setIcon(!getSession().getNotificationModel().getEvents().isEmpty() ? R.drawable.bottom_navigation_notifications_2 : R.drawable.bottom_navigation_notifications);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_notifications:
                startActivity(new Intent(this, NotificationsActivity.class));
                break;
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void onBitcoinUri() {

        Uri uri = null;
        if (!NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction()))
            uri = getIntent().getData();
        else {
            final Parcelable[] rawMessages;
            rawMessages = getIntent().getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            for (final Parcelable parcel : rawMessages) {
                final NdefMessage ndefMsg = (NdefMessage) parcel;
                for (final NdefRecord record : ndefMsg.getRecords())
                    if (record.getTnf() == NdefRecord.TNF_WELL_KNOWN &&
                        Arrays.equals(record.getType(), NdefRecord.RTD_URI))
                        uri = record.toUri();
            }
        }
        if (uri == null)
            return;

        final Intent intent = new Intent(this, SendAmountActivity.class);
        final String text = uri.toString();
        try {
            final int subaccount = getActiveAccount();
            final GDKTwoFactorCall call = getSession().createTransactionFromUri(null, text, subaccount);
            final ObjectNode transactionFromUri = call.resolve(null, new HardwareCodeResolver(this));
            final String error = transactionFromUri.get("error").asText();
            if ("id_invalid_address".equals(error)) {
                UI.toast(this, R.string.id_invalid_address, Toast.LENGTH_SHORT);
                return;
            }
            removeUtxosIfTooBig(transactionFromUri);
            intent.putExtra(PrefKeys.INTENT_STRING_TX, transactionFromUri.toString());
        } catch (final Exception e) {
            e.printStackTrace();
            if (e.getMessage() != null)
                UI.toast(this, e.getMessage(), Toast.LENGTH_SHORT);
            return;
        }
        intent.putExtra("internal_qr", getIntent().getBooleanExtra("internal_qr", false));
        startActivityForResult(intent, REQUEST_BITCOIN_URL_SEND);
    }

    private void launch() {
        setContentView(R.layout.activity_tabbed_main);
        final Toolbar toolbar = UI.find(this, R.id.toolbar);
        setSupportActionBar(toolbar);
        // Set network Icon
        setTitleWithNetwork(R.string.id_wallets);
        // Set title as null as we use a custom title element with an arrow and clicklistener
        setTitle(null);

        TextView toolbarTitle = UI.find(this, R.id.toolbarTitle);
        toolbarTitle.setText(getNetwork().getName());
        toolbarTitle.setOnClickListener(v -> {
            showDialog();
        });
    }

    private void showDialog() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.addToBackStack(null);

        // Create and show the dialog.
        DialogFragment newFragment = SwitchNetworkFragment.newInstance();
        newFragment.show(ft, "dialog");
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isFinishing())
            return;

        // check available preferred exchange rate
        try {
            final BalanceData balanceData = getSession().convertBalance(0);
            Double.parseDouble(balanceData.getFiat());
        } catch (final Exception e) {
            UI.popup(this, R.string.id_your_favourite_exchange_rate_is).show();
        }

        invalidateOptionsMenu();
    }


    @Override
    public void onPause() {
        super.onPause();
        if (isFinishing())
            return;
        mSubaccountDialog = UI.dismiss(this, mSubaccountDialog);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
        case REQUEST_BITCOIN_URL_SEND:
            mIsBitcoinUri = false;
            break;
        case REQUEST_BITCOIN_URL_LOGIN:
            if (resultCode != RESULT_OK) {
                // The user failed to login after clicking on a bitcoin Uri
                finish();
                return;
            }
            mIsBitcoinUri = true;
            break;
        case REQUEST_TX_DETAILS:
            break;
        }
    }


    @Override
    public void onBackPressed() {
        this.moveTaskToBack(true);
    }
}
