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
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.greenaddress.greenapi.data.TransactionData;
import com.greenaddress.greenapi.model.ActiveAccountObservable;
import com.greenaddress.greenapi.model.BalanceDataObservable;
import com.greenaddress.greenapi.model.Model;
import com.greenaddress.greenapi.model.ReceiveAddressObservable;
import com.greenaddress.greenapi.model.TransactionDataObservable;
import com.greenaddress.greenbits.QrBitmap;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.SubaccountFragment;
import com.greenaddress.greenbits.ui.UI;
import com.greenaddress.greenbits.ui.components.FontFitEditText;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Observer;

import static com.greenaddress.gdk.GDKSession.getSession;

public class ReceiveFragment extends SubaccountFragment implements TextWatcher, View.OnClickListener {
    private static final String TAG = ReceiveFragment.class.getSimpleName();

    private TextView mAddressText;
    private ImageView mAddressImage;
    private FontFitEditText mAmountText;
    private Button mUnitButton;
    private Boolean mIsFiat = false;

    private String mCurrentAddress = "";
    private ObjectNode mCurrentAmount;  // output from GA_convert_amount
    private List<TransactionData> mTxList = new ArrayList<>();
    private BitmapWorkerTask mBitmapWorkerTask;

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume -> " + TAG);
        if (getModel() == null)
            return;
        if (!isDisconnected()) {
            final int subaccount = getModel().getCurrentSubaccount();
            onUpdateReceiveAddress(getModel().getReceiveAddressObservable(subaccount));
            onUpdateTransactions(getModel().getTransactionDataObservable(subaccount));
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause -> " + TAG);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView -> " + TAG);

        if (isZombieNoView())
            return null;

        mView = inflater.inflate(R.layout.fragment_receive, container, false);

        mAddressImage = UI.find(mView, R.id.receiveQrImageView);
        mAddressText = UI.find(mView, R.id.receiveAddressText);
        mAmountText = UI.find(mView, R.id.amountEditText);
        mUnitButton = UI.find(mView, R.id.unitButton);

        UI.find(mView, R.id.shareAddressButton).setOnClickListener((final View v) -> { onShareClicked(); });

        mAmountText.addTextChangedListener(this);
        mUnitButton.setOnClickListener(this);

        mUnitButton.setText(mIsFiat ? getFiatCurrency() : getBitcoinOrLiquidUnit());
        mUnitButton.setPressed(!mIsFiat);
        mUnitButton.setSelected(!mIsFiat);

        final int subaccount = getModel().getCurrentSubaccount();
        mTxList = getModel().getTransactionDataObservable(subaccount).getTransactionDataList();
        UI.attachHideKeyboardListener(getActivity(), mView);

        UI.hideIf(getNetwork().getLiquid(), UI.find(mView, R.id.amountLayout));

        return mView;
    }

    @Override
    public void onUpdateTransactions(final TransactionDataObservable observable) {
        final List<TransactionData> newTxList = observable.getTransactionDataList();
        if (newTxList != null && mTxList != null) {
            mTxList = newTxList;
        }
    }

    @Override
    public void onUpdateActiveSubaccount(final ActiveAccountObservable observable) {}

    @Override
    public void onNewTx(Observer observable) {
        //onNewTxBlock(false);
    }

    @Override
    public void onVerifiedTx(Observer observable) {
        //onNewTxBlock(false);
    }

    @Override
    public void onUpdateReceiveAddress(final ReceiveAddressObservable observable) {
        if (isZombie())
            return;
        getGaActivity().runOnUiThread(() -> {
            mCurrentAddress = observable.getReceiveAddress();
            if (mCurrentAddress != null && !mCurrentAddress.isEmpty())
                update();
        });
    }

    @Override
    public void onUpdateBalance(final BalanceDataObservable observable) {}

    public void update() {
        if (mBitmapWorkerTask != null)
            mBitmapWorkerTask.cancel(true);
        mBitmapWorkerTask = new BitmapWorkerTask();
        mBitmapWorkerTask.execute();
        if (mCurrentAmount == null || mCurrentAmount.get("satoshi").asLong() == 0)
            mAddressText.setText(mCurrentAddress);
        else
            mAddressText.setText(getAddressUri());
    }

    public boolean isFiat() { return mIsFiat; }

    private String getFiatCurrency() {
        return getModel().getFiatCurrency();
    }

    private String getBitcoinOrLiquidUnit() {
        return getModel().getBitcoinOrLiquidUnit();
    }

    private String getBitcoinUnitClean() {
        final String unit = getBitcoinOrLiquidUnit();
        return Model.toUnitKey(unit);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        final String key = isFiat() ? "fiat" : getBitcoinUnitClean();
        final String value = mAmountText.getText().toString();
        final ObjectMapper mapper = new ObjectMapper();
        final ObjectNode amount = mapper.createObjectNode();
        amount.put(key, value.isEmpty() ? "0" : value);
        try {
            // avoid updating the view if changing from fiat to btc or vice versa
            if (mCurrentAmount == null || !mCurrentAmount.get(key).asText().equals(value)) {
                mCurrentAmount = getSession().convert(amount);
                update();
            }
        } catch (final RuntimeException | IOException e) {
            Log.e(TAG, "Conversion error: " + e.getLocalizedMessage());
        }
    }

    @Override
    public void afterTextChanged(Editable s) {}

    @Override
    public void onClick(final View view) {
        // Toggle unit display and selected state
        mIsFiat = !mIsFiat;
        mUnitButton.setText(mIsFiat ? getFiatCurrency() : getBitcoinOrLiquidUnit());
        mUnitButton.setPressed(!mIsFiat);
        mUnitButton.setSelected(!mIsFiat);

        if (mCurrentAmount != null) {
            mAmountText.setText(mIsFiat ? mCurrentAmount.get("fiat").asText() : mCurrentAmount.get(
                                    getBitcoinUnitClean()).asText());
            update();
        }
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
            if (isZombieNoView())
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
                mAddressImage.setOnClickListener((final View v) -> onCopyClicked());
                mAddressText.setOnClickListener((final View v) -> onCopyClicked());
            }
        }
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate -> " + TAG);
        setHasOptionsMenu(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.action_generate_new:
            onNewAddressClicked();
            return true;
        default:
            break;
        }
        return false;
    }

    private String getAddressUri() {
        return getAddressUri(mCurrentAddress, mCurrentAmount);
    }
    private String getAddressUri(final String address, final ObjectNode amount) {
        String qrCodeText;
        if (amount == null || amount.get("satoshi").asLong() == 0 || TextUtils.isEmpty(address)) {
            qrCodeText = address;
        } else {
            String s = amount.get("btc").asText();
            s = s.contains(".") ? s.replaceAll("0*$","").replaceAll("\\.$","") : s;
            qrCodeText = String.format(Locale.US,"bitcoin:%s?amount=%s", address, s);
        }
        return qrCodeText;
    }

    @Override
    public void onShareClicked() {
        if (TextUtils.isEmpty(mCurrentAddress))
            return;

        final Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT, getAddressUri());
        intent.setType("text/plain");
        startActivity(intent);
    }

    public void onCopyClicked() {
        if (TextUtils.isEmpty(mCurrentAddress))
            return;

        final ClipboardManager cm = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("address", UI.getText(mAddressText)));
        UI.toast(getGaActivity(), R.string.id_address_copied_to_clipboard, Toast.LENGTH_LONG);
    }

    public void onNewAddressClicked() {
        if (getConnectionManager().isOffline()) {
            UI.toast(getGaActivity(), R.string.id_you_are_not_connected_to_the, Toast.LENGTH_LONG);
        } else {
            final int subaccount = getModel().getCurrentSubaccount();
            getModel().getReceiveAddressObservable(subaccount).refresh();
        }
    }

    @Override
    public void onViewStateRestored(final Bundle savedInstanceState) {
        Log.d(TAG, "onViewStateRestored -> " + TAG);
        super.onViewStateRestored(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
    }
}
