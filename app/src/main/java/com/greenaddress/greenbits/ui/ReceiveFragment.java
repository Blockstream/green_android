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
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.blockstream.libwally.Wally;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.greenaddress.greenapi.ConfidentialAddress;
import com.greenaddress.greenapi.JSONMap;
import com.greenaddress.greenapi.Network;
import com.greenaddress.greenbits.GaService;
import com.greenaddress.greenbits.QrBitmap;

import java.util.concurrent.Callable;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.uri.BitcoinURI;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Observer;

import nordpol.android.OnDiscoveredTagListener;
import nordpol.android.TagDispatcher;


public class ReceiveFragment extends SubaccountFragment implements OnDiscoveredTagListener, AmountFields.OnConversionFinishListener, Exchanger.OnCalculateCommissionFinishListener {
    private static final String TAG = ReceiveFragment.class.getSimpleName();

    private FutureCallback<QrBitmap> mNewAddressCallback;
    private FutureCallback<Void> mNewAddressFinished;
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
    private TextView mAmountFiatWithCommission;
    private String mCurrentAddress = "";
    private Coin mCurrentAmount;
    private BitmapWorkerTask mBitmapWorkerTask;
    private AmountFields mAmountFields;

    private Exchanger mExchanger;
    private boolean mIsExchanger;
    private Button mShowQrCode;
    private Observer mNewTxObserver;

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume -> " + TAG);

	if (mIsExchanger && getGAService() != null)
            attachObservers();

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
        detachObservers();
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

        mSubaccount = getGAService().getCurrentSubAccount();

        if (savedInstanceState != null)
            mIsExchanger = savedInstanceState.getBoolean("isExchanger", false);

        if (mIsExchanger)
            mView = inflater.inflate(R.layout.fragment_buy, container, false);
        else
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
                generateNewAddress();
            }
        });
        mAmountEdit = UI.find(mView, R.id.sendAmountEditText);
        mAmountFiatEdit = UI.find(mView, R.id.sendAmountFiatEditText);
        final View amountFields = UI.find(mView, R.id.amountFields);
        UI.showIf(getGAService().cfg().getBoolean("showAmountInReceive", false) || mIsExchanger, amountFields);

        if (mIsExchanger) {
            setPageSelected(true);
            mAmountFiatWithCommission = UI.find(mView, R.id.amountFiatWithCommission);
            mExchanger = new Exchanger(getContext(), getGAService(), mView, true, this);
            mShowQrCode = UI.find(mView, R.id.showQrCode);
            mShowQrCode.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final String amountStr = UI.getText(mAmountFiatWithCommission);
                    final float amount = Float.valueOf(amountStr);
                    if (amount > mExchanger.getFiatInBill()) {
                        UI.toast(getGaActivity(), R.string.noEnoughMoneyInPocket, Toast.LENGTH_LONG);
                        return;
                    }
                    final String amountBtc = mAmountEdit.getText().toString();
                    if (amountBtc.isEmpty() || Float.valueOf(amountBtc) <= 0) {
                        UI.toast(getGaActivity(), R.string.invalidAmount, Toast.LENGTH_LONG);
                        return;
                    }
                    generateNewAddress(false, new FutureCallback<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            String exchangerAddress = mCurrentAddress;
                            if (GaService.IS_ELEMENTS) {
                                final String currentBtcAddress = mCurrentAddress.replace("bitcoin:", "").split("\\?")[0];
                                exchangerAddress = ConfidentialAddress.fromBase58(Network.NETWORK, currentBtcAddress).getBitcoinAddress().toString();
                            }
                            getGAService().cfg().edit().putBoolean("exchanger_address_" + exchangerAddress, true).apply();
                            final BitmapDrawable bitmapDrawable = new BitmapDrawable(getResources(), mQrCodeBitmap.getQRCode());
                            bitmapDrawable.setFilterBitmap(false);
                            mAddressImage.setImageDrawable(bitmapDrawable);
                            onAddressImageClicked(bitmapDrawable);
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            t.printStackTrace();
                        }
                    });
                }
            });
        }

        registerReceiver();
        return mView;
    }

    @Override
    public void conversionFinish() {
        if (mIsExchanger && mExchanger != null) {
            mExchanger.conversionFinish();
        } else {
            if (mBitmapWorkerTask != null)
                mBitmapWorkerTask.cancel(true);
            mBitmapWorkerTask = new BitmapWorkerTask();
            mBitmapWorkerTask.execute();
        }
    }

    @Override
    public void calculateCommissionFinish() {
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

    private void generateNewAddress() {
        generateNewAddress(true, null);
    }

    private void generateNewAddress(boolean clear, FutureCallback<Void> onDone) {
        Log.d(TAG, "Generating new address for subaccount " + mSubaccount);
        if (isZombie())
            return;
        final GaActivity gaActivity = getGaActivity();

        Long amount = null;
        if (clear)
            UI.clear(mAmountEdit, mAmountFiatEdit);
        if (mIsExchanger && GaService.IS_ELEMENTS) {
            // TODO: non-fiat / non-assets values
            if (mAmountEdit.getText().toString().isEmpty())
                return;
            amount = (long) (Float.valueOf(mAmountEdit.getText().toString()).floatValue() * 100);
        }
        mCurrentAddress = "";
        UI.disable(mCopyIcon);
        destroyCurrentAddress(clear);
        mNewAddressFinished = onDone;
        Callable waitFn = new Callable<Void>() {
            @Override
            public Void call() {
                popupWaitDialog(R.string.generating_address);
                return null;
            }
        };
        Futures.addCallback(getGAService().getNewAddressBitmap(mSubaccount, waitFn, amount),
                            mNewAddressCallback, getGAService().getExecutor());
    }

    private void destroyCurrentAddress(boolean clear) {
        Log.d(TAG, "Destroying address for subaccount " + mSubaccount);
        if (isZombie())
            return;
        mCurrentAddress = "";
        if (clear)
            UI.clear(mAmountEdit, mAmountFiatEdit, mAddressText);
        mAddressImage.setImageBitmap(null);
        UI.hide(mView);
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

        final View v;
        if (mIsExchanger) {
            v = getActivity().getLayoutInflater().inflate(R.layout.dialog_qrcode_exchanger, null, false);
            final Button cancelBtn = UI.find(v, R.id.cancelBtn);
            cancelBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mQrCodeDialog.dismiss();
                }
            });
        } else {
            v = getActivity().getLayoutInflater().inflate(R.layout.dialog_qrcode, null, false);
        }

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
        if (GaService.IS_ELEMENTS) {
            mAddressText.setText(String.format("%s\n" +
                            "%s\n%s\n" +
                            "%s\n%s\n" +
                            "%s\n%s",
                    qrData.substring(0, 12),
                    qrData.substring(12, 24),
                    qrData.substring(24, 36),
                    qrData.substring(36, 48),
                    qrData.substring(48, 60),
                    qrData.substring(60, 72),
                    qrData.substring(72)
            ));
            mAddressText.setLines(7);
            mAddressText.setMaxLines(7);
        } else {
            mAddressText.setText(String.format("%s\n%s\n%s", qrData.substring(0, 12),
                    qrData.substring(12, 24), qrData.substring(24)));
        }
        mCurrentAddress = result.getData();

        mAddressImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                onAddressImageClicked(bd);
            }
        });

        hideWaitDialog();
        UI.enable(mCopyIcon);
        UI.show(mView);

        if (mNewAddressFinished != null)
            mNewAddressFinished.onSuccess(null);
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

    @Override
    protected void onSubaccountChanged(final int newSubAccount) {
        mSubaccount = newSubAccount;
        if (IsPageSelected())
            generateNewAddress();
        else
            destroyCurrentAddress(true);
    }

    private String getAddressUri() {
        final String addr;
        if (GaService.IS_ELEMENTS)
            addr = ConfidentialAddress.fromBase58(Network.NETWORK, mCurrentAddress).toString();
        else
            addr = Address.fromBase58(Network.NETWORK, mCurrentAddress).toString();
        return BitcoinURI.convertToBitcoinURI(Network.NETWORK, addr, mCurrentAmount, null, null);
    }

    @Override
    public void onShareClicked() {
        if (mQrCodeBitmap == null || mQrCodeBitmap.getData().isEmpty())
            return;

        final Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT, getAddressUri());
        intent.setType("text/plain");
        startActivity(intent);
    }

    public void setPageSelected(final boolean isSelected) {
        final boolean needToRegenerate = isSelected && !IsPageSelected();
        super.setPageSelected(isSelected);
        if (needToRegenerate)
            generateNewAddress();
        else if (!isSelected)
            destroyCurrentAddress(true);
    }

    @Override
    public void onViewStateRestored(final Bundle savedInstanceState) {
        Log.d(TAG, "onViewStateRestored -> " + TAG);
        super.onViewStateRestored(savedInstanceState);
        if (mAmountFields != null)
            mAmountFields.setIsPausing(false);
        if (mIsExchanger)
            mExchanger.conversionFinish();
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mAmountFields != null)
            outState.putBoolean("pausing", mAmountFields.isPausing());
        outState.putBoolean("isExchanger", mIsExchanger);
    }

    public void setIsExchanger(final boolean isExchanger) {
        mIsExchanger = isExchanger;
    }

    @Override
    public void attachObservers() {
        if (mNewTxObserver == null) {
            mNewTxObserver = makeUiObserver(new Runnable() { public void run() { onNewTx(); } });
            getGAService().addNewTxObserver(mNewTxObserver);
        }
        super.attachObservers();
    }

    @Override
    public void detachObservers() {
        super.detachObservers();
        if (mNewTxObserver!= null) {
            getGAService().deleteNewTxObserver(mNewTxObserver);
            mNewTxObserver = null;
        }
    }

    private void onNewTx() {
        final GaService service = getGAService();
        Futures.addCallback(service.getMyTransactions(mSubaccount),
                new FutureCallback<Map<String, Object>>() {
                    @Override
                    public void onSuccess(final Map<String, Object> result) {
                        final List txList = (List) result.get("list");
                        final int currentBlock = ((Integer) result.get("cur_block"));
                        for (Object tx : txList) {
                            try {
                                final JSONMap txJSON = (JSONMap) tx;
                                ArrayList<String> replacedList = (ArrayList) txJSON.get("replaced_by");

                                if (replacedList == null) {
                                    final TransactionItem txItem = new TransactionItem(service, txJSON, currentBlock);
                                    boolean matches;
                                    if (!GaService.IS_ELEMENTS)
                                        matches = txItem.receivedOn != null && txItem.receivedOn.equals(mCurrentAddress);
                                    else {
                                        final int subaccount = txItem.receivedOnEp.getInt("subaccount", 0);
                                        final int pointer = txItem.receivedOnEp.getInt("pubkey_pointer");
                                        final String receivedOn = ConfidentialAddress.fromP2SHHash(
                                            Network.NETWORK,
                                            Wally.hash160(service.createOutScript(subaccount, pointer)),
                                            service.getBlindingPubKey(subaccount, pointer)
                                        ).toString();
                                        final String currentBtcAddress = mCurrentAddress.replace("bitcoin:", "").split("\\?")[0];
                                        matches = receivedOn.equals(currentBtcAddress);
                                    }
                                    if (matches) {
                                        final float amountFiat = Float.valueOf(mExchanger.getAmountWithCommission());
                                        mExchanger.buyBtc(amountFiat);
                                        getGaActivity().toast(R.string.transactionCompleted);
                                        getGaActivity().finish();
                                    }
                                }
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        throwable.printStackTrace();
                    }
                });
    }
}
