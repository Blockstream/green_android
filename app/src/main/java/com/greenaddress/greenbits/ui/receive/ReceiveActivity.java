package com.greenaddress.greenbits.ui.receive;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.greenaddress.greenapi.data.SubaccountData;
import com.greenaddress.greenbits.QrBitmap;
import com.greenaddress.greenbits.ui.LoggedActivity;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.UI;
import com.greenaddress.greenbits.ui.accounts.SubaccountPopup;

import java.util.Locale;
import java.util.Observable;

import androidx.fragment.app.FragmentTransaction;

import static com.greenaddress.gdk.GDKSession.getSession;
import static com.greenaddress.greenbits.ui.accounts.SubaccountAddFragment.ACCOUNT_TYPES;
import static com.greenaddress.greenbits.ui.accounts.SubaccountAddFragment.AUTHORIZED_ACCOUNT;

public class ReceiveActivity extends LoggedActivity implements TextWatcher {

    private TextView mAddressText;
    private ImageView mAddressImage;
    private EditText mAmountText;
    private Button mUnitButton;

    private Boolean mIsFiat = false;
    private String mCurrentAddress = "";
    private ObjectNode mCurrentAmount;
    private BitmapWorkerTask mBitmapWorkerTask;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_receive);
        UI.preventScreenshots(this);
        setTitleBackTransparent();

        mAddressImage = UI.find(this, R.id.receiveQrImageView);
        mAddressText = UI.find(this, R.id.receiveAddressText);
        mAmountText = UI.find(this, R.id.amountEditText);
        UI.localeDecimalInput(mAmountText);
        mUnitButton = UI.find(this, R.id.unitButton);

        mAmountText.addTextChangedListener(this);
        mUnitButton.setOnClickListener((final View v) -> { onCurrencyClick(); });

        if (getModel() == null)
            return;
        mUnitButton.setText(mIsFiat ? getModel().getFiatCurrency() : getModel().getBitcoinOrLiquidUnit());
        mUnitButton.setPressed(!mIsFiat);
        mUnitButton.setSelected(!mIsFiat);

        UI.find(this, R.id.shareAddressButton).setOnClickListener((final View v) -> { onShareClicked(); });

        final int subaccount = getModel().getCurrentSubaccount();
        final SubaccountData subaccountData = getModel().getSubaccountsData(subaccount);

        UI.attachHideKeyboardListener(this, UI.find(this, R.id.content));
        UI.hideIf(getNetwork().getLiquid(), UI.find(this, R.id.amountLayout));
        final TextView receivingIdValue = UI.find(this, R.id.receivingIdValue);

        if (subaccountData.getType().equals(ACCOUNT_TYPES[AUTHORIZED_ACCOUNT])) {
            final String receivingID = subaccountData.getReceivingId();
            receivingIdValue.setText(receivingID);
            receivingIdValue.setOnClickListener(
                v -> onCopyClicked("auth_code", receivingID, R.string.id_address_copied_to_clipboard));

            UI.find(this, R.id.copy).setOnClickListener(
                v -> onCopyClicked("auth_code", receivingID, R.string.id_address_copied_to_clipboard)); // FIXME fix string

            UI.find(this, R.id.receivingIdTitle).setOnClickListener(v -> {
                final SubaccountPopup s = SubaccountPopup.getInstance(getString(R.string.id_account_id),
                                                                      getString(
                                                                          R.string.id_provide_this_id_to_the_issuer));
                final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.addToBackStack(null);
                s.show(ft, "dialog");
            });
        } else {
            UI.hide(UI.find(this, R.id.receivingIdLayout));
        }

        String hwDeviceName = null;
        if (getConnectionManager().getHWDeviceData() != null) {
            hwDeviceName = getConnectionManager().getHWDeviceData().getDevice().getName();
        }

        // only show if we are on Liquid and we are using Ledger
        UI.showIf(getNetwork().getLiquid() && "Ledger".equals(hwDeviceName),
                  UI.find(this, R.id.assetWhitelistWarning));

        update(null, null);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        final int id = R.menu.receive_menu;
        getMenuInflater().inflate(id, menu);
        menu.findItem(R.id.action_generate_new).setIcon(R.drawable.ic_refresh);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            return true;
        case R.id.action_generate_new:
            onNewAddressClicked();
            return false;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isFinishing())
            return;

        final int subaccount = getModel().getCurrentSubaccount();
        getModel().getReceiveAddressObservable(subaccount).addObserver(this);
        getModel().getTransactionDataObservable(subaccount).addObserver(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (isFinishing())
            return;

        final int subaccount = getModel().getCurrentSubaccount();
        getModel().getReceiveAddressObservable(subaccount).deleteObserver(this);
        getModel().getTransactionDataObservable(subaccount).deleteObserver(this);
    }

    @Override
    public void update(final Observable observable, final Object o) {
        final int subaccount = getModel().getCurrentSubaccount();
        mCurrentAddress = getModel().getReceiveAddressObservable(subaccount).getReceiveAddress();
        if (mCurrentAmount == null || mCurrentAmount.get("satoshi").asLong() == 0)
            mAddressText.setText(mCurrentAddress);
        else
            mAddressText.setText(getAddressUri());
        if (mBitmapWorkerTask != null)
            mBitmapWorkerTask.cancel(true);
        mBitmapWorkerTask = new BitmapWorkerTask();
        mBitmapWorkerTask.execute();
    }

    @Override
    public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {}

    @Override
    public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
        final String key = mIsFiat ? "fiat" : getBitcoinUnitClean();
        final String value = mAmountText.getText().toString().replace(",",".");
        final ObjectMapper mapper = new ObjectMapper();
        final ObjectNode amount = mapper.createObjectNode();
        amount.put(key, value.isEmpty() ? "0" : value);
        try {
            // avoid updating the view if changing from fiat to btc or vice versa
            if (mCurrentAmount == null || !mCurrentAmount.get(key).asText().equals(value)) {

                mCurrentAmount = getSession().convert(amount);
                update(null, null);
            }
        } catch (final Exception e) {
            Log.e(TAG, "Conversion error: " + e.getLocalizedMessage());
        }
    }

    @Override
    public void afterTextChanged(final Editable s) {}

    public void onCurrencyClick() {
        // Toggle unit display and selected state
        mIsFiat = !mIsFiat;
        mUnitButton.setText(mIsFiat ? getModel().getFiatCurrency() : getModel().getBitcoinOrLiquidUnit());
        mUnitButton.setPressed(!mIsFiat);
        mUnitButton.setSelected(!mIsFiat);

        if (mCurrentAmount != null) {
            setAmountText(mAmountText, mIsFiat, mCurrentAmount);
            update(null, null);
        }
    }

    public void onShareClicked() {
        if (TextUtils.isEmpty(mCurrentAddress))
            return;

        final Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT, getAddressUri());
        intent.setType("text/plain");
        startActivity(intent);
    }

    public void onCopyClicked(final String label, final String data, final int toast) {
        if (data == null || data.isEmpty())
            return;

        final ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText(label, data));
        UI.toast(this, toast, Toast.LENGTH_LONG);
    }

    public void onNewAddressClicked() {
        if (getConnectionManager().isOffline()) {
            UI.toast(this, R.string.id_you_are_not_connected_to_the, Toast.LENGTH_LONG);
        } else {
            final int subaccount = getModel().getCurrentSubaccount();
            getModel().getReceiveAddressObservable(subaccount).refresh();
        }
    }

    private String getAddressUri() {
        return getAddressUri(mCurrentAddress, mCurrentAmount);
    }
    private String getAddressUri(final String address, final ObjectNode amount) {
        final String qrCodeText;
        if (amount == null || amount.get("satoshi").asLong() == 0 || TextUtils.isEmpty(address)) {
            qrCodeText = address;
        } else {
            String s = amount.get("btc").asText();
            s = s.contains(".") ? s.replaceAll("0*$","").replaceAll("\\.$","") : s;
            qrCodeText = String.format(Locale.US,"bitcoin:%s?amount=%s", address, s);
        }
        return qrCodeText;
    }

    class BitmapWorkerTask extends AsyncTask<Object, Object, Bitmap> {
        final ObjectNode amount;
        final String address;
        final int qrCodeBackground = 0; // Transparent background

        BitmapWorkerTask() {
            amount = mCurrentAmount;
            address = mCurrentAddress;
        }

        @Override
        protected Bitmap doInBackground(final Object ... integers) {
            Log.d(TAG, " doInBackground(" + address + ")");
            return new QrBitmap(getAddressUri(address,amount), qrCodeBackground).getQRCode();
        }

        @Override
        protected void onPostExecute(final Bitmap bitmap) {
            if (bitmap == null)
                return;
            Log.d(TAG, "onPostExecute (" + address + ")");
            if (TextUtils.isEmpty(address)) {
                mAddressImage.setImageDrawable(getResources().getDrawable(android.R.color.transparent));
                mAddressImage.setOnClickListener(null);
                mAddressText.setOnClickListener(null);
            } else {
                final BitmapDrawable bitmapDrawable = new BitmapDrawable(getResources(), bitmap);
                bitmapDrawable.setFilterBitmap(false);
                mAddressImage.setImageDrawable(bitmapDrawable);
                mAddressImage.setOnClickListener((final View v) -> onCopyClicked("address", UI.getText(
                                                                                     mAddressText),
                                                                                 R.string.id_address_copied_to_clipboard));
                mAddressText.setOnClickListener((final View v) -> onCopyClicked("address", UI.getText(mAddressText),
                                                                                R.string.id_address_copied_to_clipboard));
            }
        }
    }

    @Override
    protected void onRestoreInstanceState(final Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
    }
}
