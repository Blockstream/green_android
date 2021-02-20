package com.greenaddress.greenbits.ui.receive;

import android.R.color;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
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
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.greenaddress.gdk.GDKTwoFactorCall;
import com.greenaddress.greenapi.HWWallet;
import com.greenaddress.greenapi.data.HWDeviceData;
import com.greenaddress.greenapi.data.HWDeviceData.HWDeviceDataLiquidSupport;
import com.greenaddress.greenapi.data.HWDeviceDetailData;
import com.greenaddress.greenapi.data.SubaccountData;
import com.greenaddress.greenapi.model.Conversion;
import com.greenaddress.greenbits.QrBitmap;
import com.greenaddress.greenbits.ui.LoggedActivity;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.R.drawable;
import com.greenaddress.greenbits.ui.R.id;
import com.greenaddress.greenbits.ui.R.layout;
import com.greenaddress.greenbits.ui.R.string;
import com.greenaddress.greenbits.ui.UI;
import com.greenaddress.greenbits.ui.accounts.SubaccountPopup;
import com.greenaddress.greenbits.ui.components.AmountTextWatcher;
import com.greenaddress.greenbits.wallets.HardwareCodeResolver;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;


import static com.greenaddress.greenbits.ui.accounts.SubaccountAddFragment.ACCOUNT_TYPES;
import static com.greenaddress.greenbits.ui.accounts.SubaccountAddFragment.AUTHORIZED_ACCOUNT;

public class ReceiveActivity extends LoggedActivity implements TextWatcher {

    final ObjectMapper mObjectMapper = new ObjectMapper();

    private TextView mAddressText;
    private ImageView mAddressImage;
    private EditText mAmountText;
    private Button mUnitButton;

    private Boolean mIsFiat = false;
    private String mCurrentAddress = "";
    private ObjectNode mCurrentAmount;
    private BitmapWorkerTask mBitmapWorkerTask;
    private SubaccountData mSubaccountData;
    private boolean isGenerationOnProgress = false;
    private Disposable generateDisposte, validateDisposte;
    private AmountTextWatcher mAmountTextWatcher;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(layout.activity_receive);
        setTitleBackTransparent();

        mAddressImage = UI.find(this, id.receiveQrImageView);
        mAddressText = UI.find(this, id.receiveAddressText);
        mUnitButton = UI.find(this, id.unitButton);

        mAmountText = UI.find(this, id.amountEditText);
        mAmountTextWatcher = new AmountTextWatcher(mAmountText);
        mAmountText.setHint(String.format("0%s00", mAmountTextWatcher.getDefaultSeparator()));
        mAmountText.addTextChangedListener(mAmountTextWatcher);

        mUnitButton.setOnClickListener((final View v) -> {
            onCurrencyClick();
        });

        UI.find(this, id.shareAddressButton).setOnClickListener((final View v) -> {
            onShareClicked();
        });

        try {
            final String subaccount = getIntent().getStringExtra("SUBACCOUNT");
            mSubaccountData = mObjectMapper.readValue(subaccount, SubaccountData.class);
        } catch (final Exception e) {
            Toast.makeText(this, string.id_operation_failure, Toast.LENGTH_LONG).show();
            finishOnUiThread();
            return;
        }

        UI.attachHideKeyboardListener(this, UI.find(this, id.content));
        UI.hideIf(getNetwork().getLiquid(), UI.find(this, id.amountLayout));
        final TextView receivingIdValue = UI.find(this, id.receivingIdValue);

        // Show information only for authorized accounts
        if (mSubaccountData != null && mSubaccountData.getType() != null &&
            mSubaccountData.getType().equals(ACCOUNT_TYPES[AUTHORIZED_ACCOUNT])) {
            final String receivingID = mSubaccountData.getReceivingId();
            if (receivingID == null || receivingID.isEmpty()) {
                Toast.makeText(this, string.id_operation_failure, Toast.LENGTH_LONG).show();
                finishOnUiThread();
                return;
            }
            receivingIdValue.setText(receivingID);
            receivingIdValue.setOnClickListener(
                v -> onCopyClicked("auth_code", receivingID, string.id_address_copied_to_clipboard));

            UI.find(this, id.copy).setOnClickListener(
                v -> onCopyClicked("auth_code", receivingID, string.id_address_copied_to_clipboard));     // FIXME fix string

            UI.find(this, id.receivingIdTitle).setOnClickListener(v -> {
                final SubaccountPopup s = SubaccountPopup.getInstance(getString(string.id_account_id),
                                                                      getString(
                                                                          string.id_provide_this_id_to_the_asset));
                final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.addToBackStack(null);
                s.show(ft, "dialog");
            });
        } else {
            UI.hide(UI.find(this, id.receivingIdLayout));
        }


        String hwDeviceName = null;
        if (getSession().getHWWallet() != null) {
            hwDeviceName = getSession().getHWWallet().getHWDeviceData().getDevice().getName();
        }

        // only show if we are on Liquid and we are using Ledger
        UI.showIf(getNetwork().getLiquid() && "Ledger".equals(hwDeviceName),
                  UI.find(this, id.assetWhitelistWarning));
        UI.showIf(getNetwork().getLiquid() && "Ledger".equals(hwDeviceName),
                  UI.find(this, id.addressWarning));

        findViewById(R.id.assetWhitelistWarning).setOnClickListener(v -> {
            final Uri uri = Uri.parse("https://docs.blockstream.com/green/hww/hww-index.html#ledger-supported-assets");
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        });

        generateAddress();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (generateDisposte != null)
            generateDisposte.dispose();
        if (validateDisposte != null)
            validateDisposte.dispose();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isFinishing())
            return;

        try {
            mAmountText.addTextChangedListener(this);
            mUnitButton.setText(mIsFiat ? Conversion.getFiatCurrency(getSession()) : Conversion.getBitcoinOrLiquidUnit(getSession()));
            mUnitButton.setPressed(!mIsFiat);
            mUnitButton.setSelected(!mIsFiat);
        } catch (final Exception e) {}
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        final int id = R.menu.receive_menu;
        getMenuInflater().inflate(id, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (isGenerationOnProgress) {
            showWaitingToast();
            return true;
        }
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            return true;
        case id.action_generate_new:
            generateAddress();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        if (isGenerationOnProgress) {
            showWaitingToast();
            return;
        }
        super.onBackPressed();
    }

    private void showWaitingToast() {
        if (getSession() != null && getSession().getHWWallet() != null) {
            final String hwDeviceName = getSession().getHWWallet().getHWDeviceData().getDevice().getName();
            if (getNetwork().getLiquid() && "Ledger".equals(hwDeviceName)) {
                UI.toast(this, string.id_please_wait_until_your_ledger, Toast.LENGTH_LONG);
                return;
            }
        }
        UI.toast(this, R.string.id_please_hold_on_while_your, Toast.LENGTH_LONG);
    }

    private void updateAddressText() {
        final Integer satoshi = mCurrentAmount != null ? mCurrentAmount.get("satoshi").asInt(0) : 0;
        mAddressText.setText(satoshi == 0 ? mCurrentAddress : getAddressUri(mCurrentAddress, mCurrentAmount));
    }

    private void updateQR() {
        if (mBitmapWorkerTask != null)
            mBitmapWorkerTask.cancel(true);
        mBitmapWorkerTask = new BitmapWorkerTask();
        mBitmapWorkerTask.execute();
    }

    public void generateAddress() {
        // mark generation new address as ongoing
        isGenerationOnProgress = true;
        generateDisposte = Observable.just(getSession())
                           .subscribeOn(Schedulers.computation())
                           .map((session) -> {
            final int subaccount = getActiveAccount();
            final GDKTwoFactorCall call = getSession().getReceiveAddress(subaccount);
            final JsonNode jsonResp = call.resolve(null, new HardwareCodeResolver(this));
            return jsonResp;
        })
                           .observeOn(AndroidSchedulers.mainThread())
                           .subscribe((res) -> {
            mCurrentAddress = res.get("address").asText();
            try {
                updateAddressText();
                updateQR();
            } catch (final Exception e) {
                Log.e(TAG, "Conversion error: " + e.getLocalizedMessage());
            }

            // Optionally validate address on hardware (on Jade, and ledger [on liquid])
            boolean hwValidation = false;
            if (getSession().getHWWallet() != null) {
                final HWDeviceDetailData hwDevice = getSession().getHWWallet().getHWDeviceData().getDevice();
                if (hwDevice.getSupportsLiquid() != HWDeviceDataLiquidSupport.None) {
                    final String hwDeviceName = hwDevice.getName();
                    if ("Jade".equals(hwDeviceName) ||
                        ("Ledger".equals(hwDeviceName) && getNetwork().getLiquid())) {
                        hwValidation = true;
                    }
                }
            }

            if (hwValidation) {
                // Await hardware address validation
                validateAddress(res);
            } else {
                // Address generation complete
                isGenerationOnProgress = false;
            }
        }, (final Throwable e) -> {
            UI.toast(this, string.id_operation_failure, Toast.LENGTH_LONG);
            isGenerationOnProgress = false;
        });
    }

    // Get address for the given path from the hardware, and compare to that in hand.
    // Note: the user may have a chance to reject the address from the hardware device.
    private void validateAddress(final JsonNode addressData) {
        final String address = addressData.get("address").asText();

        final long branch = addressData.get("branch").asLong(0);
        final long pointer = addressData.get("pointer").asLong(0);

        final boolean csv = mSubaccountData.getType() != null &&
                            !mSubaccountData.getType().equals(ACCOUNT_TYPES[AUTHORIZED_ACCOUNT]);
        final long csvBlocks = csv ? addressData.get("subtype").asLong(0) : 0;

        validateDisposte = Observable.just(getSession().getHWWallet())
                           .subscribeOn(Schedulers.computation())
                           .timeout(30, TimeUnit.SECONDS)
                           .map(hwWallet -> hwWallet.getGreenAddress(mSubaccountData, branch, pointer, csvBlocks))
                           .observeOn(AndroidSchedulers.mainThread())
                           .subscribe((addressHW) -> {
            Log.d(TAG, "HWWallet address: " + address);
            if (addressHW == null)
                throw new Exception();
            else if (addressHW.equals(address))
                UI.toast(this, R.string.id_the_address_is_valid, Toast.LENGTH_LONG);
            else
                UI.popup(this, R.string.id_the_addresses_dont_match).show();
            isGenerationOnProgress = false;
        }, (err) -> {
            UI.toast(this, string.id_operation_failure, Toast.LENGTH_LONG);
            isGenerationOnProgress = false;
        });
    }

    @Override
    public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {}

    @Override
    public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
        try {
            final String key = mIsFiat ? "fiat" : getBitcoinUnitClean();
            final NumberFormat us = Conversion.getNumberFormat(8);
            final String numberText = mAmountText.getText().toString();
            final Number number = us.parse(numberText.isEmpty() ? "0" : numberText);
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

        mIsFiat = !mIsFiat;
        if (mCurrentAmount != null) {
            try {
                mAmountText.removeTextChangedListener(mAmountTextWatcher);
                setAmountText(mAmountText, mIsFiat, mCurrentAmount);
                mAmountText.addTextChangedListener(mAmountTextWatcher);
            } catch(final Exception e) {
                mIsFiat = !mIsFiat;
                UI.popup(this, R.string.id_your_favourite_exchange_rate_is).show();
                return;
            }
        }

        // Toggle unit display and selected state
        try {
            mUnitButton.setText(mIsFiat ? Conversion.getFiatCurrency(getSession()) : Conversion.getBitcoinOrLiquidUnit(getSession()));
            mUnitButton.setPressed(!mIsFiat);
            mUnitButton.setSelected(!mIsFiat);
        } catch (final Exception e) {}
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
