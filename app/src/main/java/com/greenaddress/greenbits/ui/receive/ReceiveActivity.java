package com.greenaddress.greenbits.ui.receive;

import android.R.color;
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

import androidx.fragment.app.FragmentTransaction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.greenaddress.gdk.GDKTwoFactorCall;
import com.greenaddress.greenapi.HWWallet;
import com.greenaddress.greenapi.data.HWDeviceData;
import com.greenaddress.greenapi.data.HWDeviceData.HWDeviceDataLiquidSupport;
import com.greenaddress.greenapi.data.SubaccountData;
import com.greenaddress.greenapi.model.Model;
import com.greenaddress.greenbits.QrBitmap;
import com.greenaddress.greenbits.ui.LoggedActivity;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.R.drawable;
import com.greenaddress.greenbits.ui.R.id;
import com.greenaddress.greenbits.ui.R.layout;
import com.greenaddress.greenbits.ui.R.string;
import com.greenaddress.greenbits.ui.UI;
import com.greenaddress.greenbits.ui.accounts.SubaccountPopup;
import com.greenaddress.greenbits.wallets.HardwareCodeResolver;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

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
    private boolean isGenerationOnProgress = false;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (modelIsNullOrDisconnected()) {
            return;
        }

        setContentView(layout.activity_receive);
        UI.preventScreenshots(this);
        setTitleBackTransparent();

        mAddressImage = UI.find(this, id.receiveQrImageView);
        mAddressText = UI.find(this, id.receiveAddressText);
        mAmountText = UI.find(this, id.amountEditText);
        UI.localeDecimalInput(mAmountText);
        mUnitButton = UI.find(this, id.unitButton);

        mAmountText.addTextChangedListener(this);
        mUnitButton.setOnClickListener((final View v) -> { onCurrencyClick(); });

        mUnitButton.setText(mIsFiat ? getModel().getFiatCurrency() : getModel().getBitcoinOrLiquidUnit());
        mUnitButton.setPressed(!mIsFiat);
        mUnitButton.setSelected(!mIsFiat);

        UI.find(this, id.shareAddressButton).setOnClickListener((final View v) -> { onShareClicked(); });

        final int subaccount = getModel().getCurrentSubaccount();
        final SubaccountData subaccountData = getModel().getSubaccountsData(subaccount);

        UI.attachHideKeyboardListener(this, UI.find(this, id.content));
        UI.hideIf(getNetwork().getLiquid(), UI.find(this, id.amountLayout));
        final TextView receivingIdValue = UI.find(this, id.receivingIdValue);

        // Show information only for authorized accounts
        if (subaccountData.getType().equals(ACCOUNT_TYPES[AUTHORIZED_ACCOUNT])) {
            final String receivingID = subaccountData.getReceivingId();
            receivingIdValue.setText(receivingID);
            receivingIdValue.setOnClickListener(
                v -> onCopyClicked("auth_code", receivingID, string.id_address_copied_to_clipboard));

            UI.find(this, id.copy).setOnClickListener(
                v -> onCopyClicked("auth_code", receivingID, string.id_address_copied_to_clipboard)); // FIXME fix string

            UI.find(this, id.receivingIdTitle).setOnClickListener(v -> {
                final SubaccountPopup s = SubaccountPopup.getInstance(getString(string.id_account_id),
                                                                      getString(
                                                                          string.id_provide_this_id_to_the_issuer));
                final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.addToBackStack(null);
                s.show(ft, "dialog");
            });
        } else {
            UI.hide(UI.find(this, id.receivingIdLayout));
        }


        String hwDeviceName = null;
        if (getGAApp().mHWDevice != null) {
            hwDeviceName = getGAApp().mHWDevice.getDevice().getName();
        }

        // only show if we are on Liquid and we are using Ledger
        UI.showIf(getNetwork().getLiquid() && "Ledger".equals(hwDeviceName),
                  UI.find(this, id.assetWhitelistWarning));
        UI.showIf(getNetwork().getLiquid() && "Ledger".equals(hwDeviceName),
                  UI.find(this, id.addressWarning));

        getGAApp().getExecutor().submit(() -> {
            generateAddress();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        final int id = R.menu.receive_menu;
        getMenuInflater().inflate(id, menu);
        menu.findItem(R.id.action_generate_new).setIcon(drawable.ic_refresh);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (isGenerationOnProgress)
            return true;
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            return true;
        case id.action_generate_new:
            getGAApp().getExecutor().submit(() -> {
                generateAddress();
            });
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        if (isGenerationOnProgress)
            return;
        super.onBackPressed();
    }

    private void updateAddressText() {
        final Integer satoshi = mCurrentAmount != null ? mCurrentAmount.get("satoshi").asInt(0) : 0;
        mAddressText.setText( satoshi == 0 ? mCurrentAddress : getAddressUri(mCurrentAddress, mCurrentAmount));
    }

    private void updateQR() {
        if (mBitmapWorkerTask != null)
            mBitmapWorkerTask.cancel(true);
        mBitmapWorkerTask = new BitmapWorkerTask();
        mBitmapWorkerTask.execute();
    }

    public void generateAddress() {
        // check to be online
        if (getConnectionManager().isOffline()) {
            UI.toast(this, string.id_connection_failed, Toast.LENGTH_LONG);
            return;
        }

        // mark generation new address as ongoing
        isGenerationOnProgress = true;

        // generate new address
        final JsonNode jsonResp;
        try {
            final int subaccount = getModel().getCurrentSubaccount();
            final GDKTwoFactorCall call = getSession().getReceiveAddress(subaccount);
            jsonResp = call.resolve(null, new HardwareCodeResolver(this));
        } catch (final Exception e) {
            e.printStackTrace();
            UI.toast(this, string.id_operation_failure, Toast.LENGTH_LONG);
            isGenerationOnProgress = false;
            return;
        }

        final String address = jsonResp.get("address").asText();
        final Long pointer = jsonResp.get("pointer").asLong(0);

        // update UI
        runOnUiThread(() -> {
            try {
                mCurrentAddress = address;
                updateAddressText();
                updateQR();
            } catch (final Exception e) {
                Log.e(TAG, "Conversion error: " + e.getLocalizedMessage());
            }
        });

        // Validate address only for ledger liquid HW
        if (!getConnectionManager().isHW()) {
            isGenerationOnProgress = false;
            return;
        }

        validateAddress(address, pointer);
        isGenerationOnProgress = false;
    }

    private void validateAddress(final String address, final long pointer) {
        boolean isLedger = false;
        if (getGAApp().mHWDevice != null) {
            final String hwDeviceName = getGAApp().mHWDevice.getDevice().getName();
            isLedger = "Ledger".equals(hwDeviceName);
        }

        if (getNetwork().getLiquid() && isLedger) {
            try {
                final String addressHW = generateHW(pointer);
                if (addressHW == null)
                    throw new Exception();
                else if (addressHW.equals(address))
                    UI.toast(this, R.string.id_the_address_is_valid, Toast.LENGTH_LONG);
                else
                    runOnUiThread(() -> {
                        UI.popup(this, R.string.id_the_addresses_dont_match).show();
                    });
            } catch (final Exception e) {
                UI.toast(this, string.id_operation_failure, Toast.LENGTH_LONG);
            }
        }
    }

    private String generateHW(final long pointer) throws Exception {
        final int subaccount = getModel().getCurrentSubaccount();
        final SubaccountData subaccountData = getModel().getSubaccountsData(subaccount);
        final HWDeviceData hwDeviceData = getGAApp().mHWDevice;
        final HWWallet hwWallet = getGAApp().mHWWallet;
        if (hwDeviceData != null &&
            hwDeviceData.getDevice().getSupportsLiquid() != HWDeviceDataLiquidSupport.None) {
            final boolean csv = !subaccountData.getType().equals(ACCOUNT_TYPES[AUTHORIZED_ACCOUNT]);
            final String address = hwWallet.getGreenAddress(csv, subaccountData.getPointer(), 1L, pointer, 65535L);
            Log.d(TAG, "HWWallet address: " + address);
            return address;
        }
        return null;
    }

    @Override
    public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {}

    @Override
    public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
        final String key = mIsFiat ? "fiat" : getBitcoinUnitClean();
        try {
            final NumberFormat us = Model.getNumberFormat(8, Locale.US);
            final Number number = us.parse(mAmountText.getText().toString());
            final String value = String.valueOf(number);
            final ObjectMapper mapper = new ObjectMapper();
            final ObjectNode amount = mapper.createObjectNode();
            amount.put(key, value.isEmpty() ? "0" : value);
            // avoid updating the view if changing from fiat to btc or vice versa
            if (mCurrentAmount == null || !mCurrentAmount.get(key).asText().equals(value)) {
                mCurrentAmount = getSession().convert(amount);
                updateAddressText();
                updateQR();
            }
        } catch (final Exception e) {
            Log.e(TAG, "Conversion error: " + e.getLocalizedMessage());
        }
    }

    @Override
    public void afterTextChanged(final Editable s) {}

    public void onCurrencyClick() {
        if (mCurrentAmount == null)
            return;

        try {
            mIsFiat = !mIsFiat;
            setAmountText(mAmountText, mIsFiat, mCurrentAmount);
        } catch (final ParseException e) {
            mIsFiat = !mIsFiat;
            UI.popup(this, R.string.id_your_favourite_exchange_rate_is).show();
            return;
        }

        // Toggle unit display and selected state
        mUnitButton.setText(mIsFiat ? getModel().getFiatCurrency() : getModel().getBitcoinOrLiquidUnit());
        mUnitButton.setPressed(!mIsFiat);
        mUnitButton.setSelected(!mIsFiat);
    }

    public void onShareClicked() {
        if (TextUtils.isEmpty(mCurrentAddress))
            return;

        final Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT, UI.getText(mAddressText));
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
            try {
                return new QrBitmap(getAddressUri(address, amount), qrCodeBackground).getQRCode();
            } catch (final Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(final Bitmap bitmap) {
            if (bitmap == null)
                return;
            Log.d(TAG, "onPostExecute (" + address + ")");
            if (TextUtils.isEmpty(address)) {
                mAddressImage.setImageDrawable(getResources().getDrawable(color.transparent));
                mAddressImage.setOnClickListener(null);
                mAddressText.setOnClickListener(null);
            } else {
                final BitmapDrawable bitmapDrawable = new BitmapDrawable(getResources(), bitmap);
                bitmapDrawable.setFilterBitmap(false);
                mAddressImage.setImageDrawable(bitmapDrawable);
                mAddressImage.setOnClickListener((final View v) -> onCopyClicked("address", UI.getText(
                                                                                     mAddressText),
                                                                                 string.id_address_copied_to_clipboard));
                mAddressText.setOnClickListener((final View v) -> onCopyClicked("address", UI.getText(mAddressText),
                                                                                string.id_address_copied_to_clipboard));
            }
        }
    }
}
