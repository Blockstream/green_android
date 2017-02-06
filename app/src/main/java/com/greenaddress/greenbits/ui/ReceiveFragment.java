package com.greenaddress.greenbits.ui;


import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.greenaddress.greenapi.Network;
import com.greenaddress.greenbits.QrBitmap;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.uri.BitcoinURI;

import nordpol.android.OnDiscoveredTagListener;
import nordpol.android.TagDispatcher;


public class ReceiveFragment extends SubaccountFragment implements OnDiscoveredTagListener, AmountFields.OnConversionFinishListener {
    private static final String TAG = ReceiveFragment.class.getSimpleName();

    private FutureCallback<QrBitmap> mNewAddressCallback;
    private QrBitmap mQrCodeBitmap;
    private int mSubaccount;
    private Dialog mQrCodeDialog;
    private TagDispatcher mTagDispatcher;
    private TextView mAddressText;
    private ImageView mAddressImage;
    private TextView mCopyIcon;
    private final Runnable mDialogCB = new Runnable() { public void run() { mQrCodeDialog = null; } };

    private EditText mAmountEdit;
    private EditText mAmountFiatEdit;
    private String mCurrentAddress = "";
    private Coin mCurrentAmount;
    private BitmapWorkerTask mBitmapWorkerTask;
    private AmountFields mAmountFields;

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume -> " + TAG);
        if (mAmountFields != null)
            mAmountFields.setIsPausing(false);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mAmountFields != null)
            mAmountFields.setIsPausing(true);
        Log.d(TAG, "onPause -> " + TAG);
        if (mQrCodeDialog != null) {
            mQrCodeDialog.dismiss();
            mQrCodeDialog = null;
        }
        if (mTagDispatcher != null)
            mTagDispatcher.disableExclusiveNfc();
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView -> " + TAG);

        if (isZombieNoView())
            return null;

        popupWaitDialog(R.string.generating_address);

        final GaActivity gaActivity = getGaActivity();

        mTagDispatcher = TagDispatcher.get(gaActivity, this);
        mTagDispatcher.enableExclusiveNfc();

        mSubaccount = getGAService().getCurrentSubAccount();

        mView = inflater.inflate(R.layout.fragment_receive, container, false);

        mAmountFields = new AmountFields(getGAService(), getContext(), mView, this);
        if (savedInstanceState != null) {
            final Boolean pausing = savedInstanceState.getBoolean("pausing", false);
            mAmountFields.setIsPausing(pausing);
        }

        mAddressText = UI.find(mView, R.id.receiveAddressText);
        mAddressImage = UI.find(mView, R.id.receiveQrImageView);

        mCopyIcon = UI.find(mView, R.id.receiveCopyIcon);
        UI.disable(mCopyIcon);
        mCopyIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                onCopyClicked();
            }
        });

        mNewAddressCallback = new FutureCallback<QrBitmap>() {
            @Override
            public void onSuccess(final QrBitmap result) {
                if (getActivity() != null)
                    getActivity().runOnUiThread(new Runnable() {
                        public void run() {
                            onNewAddressGenerated(result);
                        }
                    });
            }

            @Override
            public void onFailure(final Throwable t) {
                t.printStackTrace();
                if (getActivity() != null)
                    getActivity().runOnUiThread(new Runnable() {
                        public void run() {
                            hideWaitDialog();
                            UI.enable(mCopyIcon);
                        }
                    });
            }
        };

        final TextView newAddressIcon = UI.find(mView, R.id.receiveNewAddressIcon);
        newAddressIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                getNewAddress();
            }
        });
        mAmountEdit = UI.find(mView, R.id.sendAmountEditText);
        mAmountFiatEdit = UI.find(mView, R.id.sendAmountFiatEditText);
        final View amountFields = UI.find(mView, R.id.amountFields);
        UI.showIf(getGAService().cfg().getBoolean("showAmountInReceive", false), amountFields);

        registerReceiver();
        return mView;
    }

    @Override
    public void conversionFinish() {
        if (mBitmapWorkerTask != null)
            mBitmapWorkerTask.cancel(true);
        mBitmapWorkerTask = new BitmapWorkerTask();
        mBitmapWorkerTask.execute();
    }

    class BitmapWorkerTask extends AsyncTask<Object, Object, Bitmap> {

        @Override
        protected Bitmap doInBackground(final Object... integers) {
            final String amount = UI.getText(mAmountEdit);
            mCurrentAmount = null;
            if (amount.isEmpty())
                return mQrCodeBitmap == null ? null : resetBitmap(mCurrentAddress);

            try {
                mCurrentAmount = UI.parseCoinValue(getGAService(), amount);

                final Address address = Address.fromBase58(Network.NETWORK, mCurrentAddress);
                final String qrCodeText = BitcoinURI.convertToBitcoinURI(address, mCurrentAmount, null, null);
                return resetBitmap(qrCodeText);
            } catch (final ArithmeticException | IllegalArgumentException e) {
                return resetBitmap(mCurrentAddress);
            }
        }

        @Override
        protected void onPostExecute(final Bitmap bitmap) {
            if (bitmap == null)
                return;
            final BitmapDrawable bitmapDrawable = new BitmapDrawable(getResources(), bitmap);
            bitmapDrawable.setFilterBitmap(false);
            mAddressImage.setImageDrawable(bitmapDrawable);
            mAddressImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    onAddressImageClicked(bitmapDrawable);
                }
            });
        }

        private Bitmap resetBitmap(final String address) {
            final int TRANSPARENT = 0; // Transparent background
            mQrCodeBitmap = new QrBitmap(address, TRANSPARENT);
            return mQrCodeBitmap.getQRCode();
        }
    }

    private void getNewAddress() {
        Log.d(TAG, "Generating new address for subaccount " + mSubaccount);
        if (isZombie())
            return;
        mAmountEdit.setText("");
        mAmountFiatEdit.setText("");
        mCurrentAddress = "";
        popupWaitDialog(R.string.generating_address);
        UI.disable(mCopyIcon);
        destroyCurrentAddress();
        Futures.addCallback(getGAService().getNewAddressBitmap(mSubaccount),
                            mNewAddressCallback, getGAService().getExecutor());
    }

    private void destroyCurrentAddress() {
        Log.d(TAG, "Destroying address for subaccount " + mSubaccount);
        if (isZombie())
            return;
        mAmountEdit.setText("");
        mAmountFiatEdit.setText("");
        mCurrentAddress = "";
        mAddressText.setText("");
        mAddressImage.setImageBitmap(null);
        mView.setVisibility(View.GONE);
    }

    private void onCopyClicked() {
        // Gets a handle to the clipboard service.
        final GaActivity gaActivity = getGaActivity();
        final ClipboardManager cm;
        cm = (ClipboardManager) gaActivity.getSystemService(Context.CLIPBOARD_SERVICE);
        final ClipData data = ClipData.newPlainText("data", mQrCodeBitmap.getData());
        cm.setPrimaryClip(data);
        final String text = gaActivity.getString(R.string.toastOnCopyAddress) +
                            " " + gaActivity.getString(R.string.warnOnPaste);
        gaActivity.toast(text);
    }

    private void onAddressImageClicked(final BitmapDrawable bd) {
        if (mQrCodeDialog != null)
            mQrCodeDialog.dismiss();

        final View v = getActivity().getLayoutInflater().inflate(R.layout.dialog_qrcode, null, false);

        final ImageView qrCode = UI.find(v, R.id.qrInDialogImageView);
        qrCode.setLayoutParams(UI.getScreenLayout(getActivity(), 0.8));

        final Dialog dialog = new Dialog(getActivity());
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(v);
        UI.setDialogCloseHandler(dialog, mDialogCB);

        qrCode.setImageDrawable(bd);
        mQrCodeDialog = dialog;
        mQrCodeDialog.show();
    }

    private void onNewAddressGenerated(final QrBitmap result) {
        if (getActivity() == null)
            return;

        if (mBitmapWorkerTask != null) {
            mBitmapWorkerTask.cancel(true);
            mBitmapWorkerTask = null;
        }

        mQrCodeBitmap = result;
        final BitmapDrawable bd = new BitmapDrawable(getResources(), result.getQRCode());
        bd.setFilterBitmap(false);
        mAddressImage.setImageDrawable(bd);

        final String qrData = result.getData();
        mAddressText.setText(String.format("%s\n%s\n%s", qrData.substring(0, 12),
                             qrData.substring(12, 24), qrData.substring(24)));
        mCurrentAddress = result.getData();

        mAddressImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                onAddressImageClicked(bd);
            }
        });

        hideWaitDialog();
        UI.enable(mCopyIcon);
        mView.setVisibility(View.VISIBLE);
    }

    @Override
    public void tagDiscovered(final Tag t) {
        Log.d(TAG, "Tag discovered " + t);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate -> " + TAG);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.receive, menu);
    }

    @Override
    protected void onSubaccountChanged(final int newSubAccount) {
        mSubaccount = newSubAccount;
        if (IsPageSelected())
            getNewAddress();
        else
            destroyCurrentAddress();
    }

    private String getAddressUri() {
        final Address address = Address.fromBase58(Network.NETWORK, mCurrentAddress);
        return BitcoinURI.convertToBitcoinURI(address, mCurrentAmount, null, null);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {

        if (item.getItemId() == R.id.action_share) {
            if (mQrCodeBitmap != null && !mQrCodeBitmap.getData().isEmpty()) {
                // SHARE intent
                final Intent intent = new Intent();
                intent.setAction(Intent.ACTION_SEND);
                intent.putExtra(Intent.EXTRA_TEXT, getAddressUri());
                intent.setType("text/plain");
                startActivity(intent);
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void setPageSelected(final boolean isSelected) {
        final boolean needToRegenerate = isSelected && !IsPageSelected();
        super.setPageSelected(isSelected);
        if (needToRegenerate)
            getNewAddress();
        else if (!isSelected)
            destroyCurrentAddress();
    }

    @Override
    public void onViewStateRestored(final Bundle savedInstanceState) {
        Log.d(TAG, "onViewStateRestored -> " + TAG);
        super.onViewStateRestored(savedInstanceState);
        if (mAmountFields != null)
            mAmountFields.setIsPausing(false);
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mAmountFields != null)
            outState.putBoolean("pausing", mAmountFields.isPausing());
    }
}
