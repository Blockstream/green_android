package com.greenaddress.greenbits.ui;


import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.greenaddress.greenapi.data.TransactionData;
import com.greenaddress.greenapi.model.ActiveAccountObservable;
import com.greenaddress.greenapi.model.BalanceDataObservable;
import com.greenaddress.greenapi.model.ReceiveAddressObservable;
import com.greenaddress.greenapi.model.TransactionDataObservable;
import com.greenaddress.greenbits.QrBitmap;

import org.bitcoinj.core.Coin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Observer;


public class ReceiveFragment extends SubaccountFragment implements
    CurrencyView.OnConversionFinishListener {
    private static final String TAG = ReceiveFragment.class.getSimpleName();

    private TextView mAddressText;
    private ImageView mAddressImage;
    private CurrencyView mCurrency;

    private String mCurrentAddress = "";
    private Coin mCurrentAmount;
    private List<TransactionData> mTxList = new ArrayList<>();
    private BitmapWorkerTask mBitmapWorkerTask;

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume -> " + TAG);
        if (mCurrency != null)
            mCurrency.setIsPausing(false);
        if (!isDisconnected()) {
            final int subaccount = getGAService().getModel().getCurrentSubaccount();
            onUpdateReceiveAddress(getGAService().getModel().getReceiveAddressObservable(subaccount));
            onUpdateTransactions(getGAService().getModel().getTransactionDataObservable(subaccount));
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mCurrency != null)
            mCurrency.setIsPausing(true);
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
        mCurrency = UI.find(mView, R.id.currency_view);
        mCurrency.setService(getGAService());
        mCurrency.setOnConversionFinishListener(this);
        if (savedInstanceState != null) {
            final Boolean pausing = savedInstanceState.getBoolean("pausing", false);
            mCurrency.setIsPausing(pausing);
        }

        UI.find(mView, R.id.shareAddressButton).setOnClickListener((final View v) -> { onShareClicked(); });

        final int subaccount = getGAService().getModel().getCurrentSubaccount();
        mTxList = getGAService().getModel().getTransactionDataObservable(subaccount).getTransactionDataList();
        UI.attachHideKeyboardListener(getActivity(), mView);
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
                conversionFinish();
        });
    }

    @Override
    public void onUpdateBalance(final BalanceDataObservable observable) {}

    @Override
    public void conversionFinish() {
        if (mBitmapWorkerTask != null)
            mBitmapWorkerTask.cancel(true);
        mCurrentAmount = mCurrency.getCoin();
        mBitmapWorkerTask = new BitmapWorkerTask();
        mBitmapWorkerTask.execute();
        if (mCurrentAmount == null || mCurrentAmount.value == 0)
            mAddressText.setText(mCurrentAddress);
        else
            mAddressText.setText(getAddressUri());
    }

    class BitmapWorkerTask extends AsyncTask<Object, Object, Bitmap> {
        final Coin amount;
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
    private String getAddressUri(final String address, final Coin amount) {
        String qrCodeText;
        if (amount == null || amount.value == 0 || TextUtils.isEmpty(address)) {
            qrCodeText = address;
        } else {
            qrCodeText = String.format(Locale.US,"bitcoin:%s?amount=%s",address,amount.toPlainString());
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
        if (getGAService().getConnectionManager().isOffline()) {
            UI.toast(getGaActivity(), R.string.id_you_are_not_connected_to_the, Toast.LENGTH_LONG);
        } else {
            final int subaccount = getGAService().getModel().getCurrentSubaccount();
            getGAService().getModel().getReceiveAddressObservable(subaccount).refresh();
        }
    }

    @Override
    public void onViewStateRestored(final Bundle savedInstanceState) {
        Log.d(TAG, "onViewStateRestored -> " + TAG);
        super.onViewStateRestored(savedInstanceState);
        if (mCurrency != null)
            mCurrency.setIsPausing(false);
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mCurrency != null)
            outState.putBoolean("pausing", mCurrency.isPausing());
    }
}
