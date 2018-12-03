package com.greenaddress.greenbits.ui;


import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.greenaddress.greenapi.ConfidentialAddress;
import com.greenaddress.greenapi.data.TransactionData;
import com.greenaddress.greenapi.model.ActiveAccountObservable;
import com.greenaddress.greenapi.model.BalanceDataObservable;
import com.greenaddress.greenapi.model.ReceiveAddressObservable;
import com.greenaddress.greenapi.model.TransactionDataObservable;
import com.greenaddress.greenbits.QrBitmap;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.uri.BitcoinURI;

import java.util.ArrayList;
import java.util.List;
import java.util.Observer;

import nordpol.android.OnDiscoveredTagListener;
import nordpol.android.TagDispatcher;


public class ReceiveFragment extends SubaccountFragment implements OnDiscoveredTagListener,
    CurrencyView.OnConversionFinishListener {
    private static final String TAG = ReceiveFragment.class.getSimpleName();

    private QrBitmap mQrCodeBitmap;
    private Dialog mQrCodeDialog;
    private TagDispatcher mTagDispatcher;
    private TextView mAddressText;
    private ImageView mAddressImage;
    private final Runnable mDialogCB = new Runnable() {
        public void run() { mQrCodeDialog = null; }
    };
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

        final int subaccount = getGAService().getSession().getCurrentSubaccount();
        onUpdateReceiveAddress(getGAService().getModel().getReceiveAddressObservable(subaccount));
        onUpdateTransactions(getGAService().getModel().getTransactionDataObservable(subaccount));
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mCurrency != null)
            mCurrency.setIsPausing(true);
        Log.d(TAG, "onPause -> " + TAG);
        mQrCodeDialog = UI.dismiss(getActivity(), mQrCodeDialog);
        if (mTagDispatcher != null)
            mTagDispatcher.disableExclusiveNfc();
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView -> " + TAG);

        if (isZombieNoView())
            return null;

        final GaActivity gaActivity = getGaActivity();

        mTagDispatcher = TagDispatcher.get(gaActivity, this);
        mTagDispatcher.enableExclusiveNfc();

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

        UI.find(mView, R.id.shareButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onShareClicked();
            }
        });

        final int subaccount = getGAService().getSession().getCurrentSubaccount();
        mTxList = getGAService().getModel().getTransactionDataObservable(subaccount).getTransactionDataList();
        return mView;
    }

    @Override
    public void onUpdateTransactions(final TransactionDataObservable observable) {
        final List<TransactionData> newTxList = observable.getTransactionDataList();
        if (newTxList != null) {
            if (newTxList.size() != mTxList.size())
                toast(R.string.id_a_new_transaction_has_just);
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
        getGaActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mCurrentAddress = observable.getReceiveAddress();
                mCurrentAmount = mCurrency.getCoin();
                mAddressText.setText(mCurrentAddress);
                conversionFinish();
            }
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
    }

    class BitmapWorkerTask extends AsyncTask<Object, Object, Bitmap> {
        final Coin amount;
        final String address;
        final NetworkParameters networkParameters;
        final int qrCodeBackground = 0; // Transparent background

        BitmapWorkerTask() {
            amount = mCurrentAmount;
            address = mCurrentAddress;
            networkParameters = getGAService().getNetworkParameters();
        }

        @Override
        protected Bitmap doInBackground(final Object ... integers) {
            String qrCodeText;
            try {
                if (amount == null || amount.value == 0)
                    throw new NullPointerException();
                final Address addr = Address.fromBase58(networkParameters, address);
                qrCodeText = BitcoinURI.convertToBitcoinURI(addr, amount, null, null);
            } catch (final Exception e) {
                qrCodeText = address;
            }
            return new QrBitmap(qrCodeText, qrCodeBackground).getQRCode();
        }

        @Override
        protected void onPostExecute(final Bitmap bitmap) {
            if (bitmap == null)
                return;
            if (isZombieNoView())
                return;

            final BitmapDrawable bitmapDrawable = new BitmapDrawable(getResources(), bitmap);
            bitmapDrawable.setFilterBitmap(false);
            mAddressImage.setImageDrawable(bitmapDrawable);
            mAddressImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    // Show address fragment
                    final Bundle bundle = new Bundle();
                    bundle.putLong("amount", amount != null ? amount.getValue() : 0);
                    final DialogFragment addressDialog = new AddressDialog();
                    addressDialog.setArguments(bundle);
                    addressDialog.show(getFragmentManager(), "addressDialog");
                }
            });
        }
    }

    @Override
    public void tagDiscovered(final Tag t) {
        Log.d(TAG, "Tag discovered " + t);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate -> " + TAG);
    }

    private String getAddressUri() {
        final String addr;
        final NetworkParameters params = getGAService().getNetworkParameters();
        if (getGAService().isElements())
            addr = ConfidentialAddress.fromBase58(params, mCurrentAddress).toString();
        else
            addr = Address.fromBase58(params, mCurrentAddress).toString();
        return BitcoinURI.convertToBitcoinURI(params, addr, mCurrentAmount, null, null);
    }

    @Override
    public void onShareClicked() {
        if (mCurrentAddress == null)
            return;

        final Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT, getAddressUri());
        intent.setType("text/plain");
        startActivity(intent);
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
