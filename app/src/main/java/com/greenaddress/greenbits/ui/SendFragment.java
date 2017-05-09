package com.greenaddress.greenbits.ui;

import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.util.Pair;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.blockstream.libwally.Wally;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.greenaddress.greenapi.ConfidentialAddress;
import com.greenaddress.greenapi.CryptoHelper;
import com.greenaddress.greenapi.ElementsTransaction;
import com.greenaddress.greenapi.ElementsTransactionOutput;
import com.greenaddress.greenapi.GATx;
import com.greenaddress.greenapi.JSONMap;
import com.greenaddress.greenapi.Network;
import com.greenaddress.greenapi.Output;
import com.greenaddress.greenapi.PreparedTransaction;
import com.greenaddress.greenbits.GaService;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.TransactionWitness;
import org.bitcoinj.script.Script;
import org.bitcoinj.uri.BitcoinURI;
import org.bitcoinj.uri.BitcoinURIParseException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import de.schildbach.wallet.ui.ScanActivity;

public class SendFragment extends SubaccountFragment {

    private static final String TAG = SendFragment.class.getSimpleName();

    private Dialog mSummary;
    private Dialog mTwoFactor;
    private EditText mAmountEdit;
    private EditText mAmountFiatEdit;
    private TextView mAmountBtcWithCommission;
    private EditText mRecipientEdit;
    private EditText mNoteText;
    private CheckBox mInstantConfirmationCheckbox;
    private TextView mNoteIcon;
    private Button mSendButton;
    private Switch mMaxButton;
    private TextView mMaxLabel;
    private TextView mScanIcon;
    private Map<?, ?> mPayreqData;
    private boolean mFromIntentURI;


    private int mSubaccount;
    private AmountFields mAmountFields;

    private boolean mIsExchanger;
    private Exchanger mExchanger;

    private void processBitcoinURI(final BitcoinURI URI) {
        processBitcoinURI(URI, null, null);
    }

    private void processBitcoinURI(final BitcoinURI URI, final String confidentialAddress, Coin amount) {
        final GaService service = getGAService();
        final GaActivity gaActivity = getGaActivity();

        if (URI != null && URI.getPaymentRequestUrl() != null) {
            final ProgressBar bip70Progress = UI.find(mView, R.id.sendBip70ProgressBar);
            UI.show(bip70Progress);
            mRecipientEdit.setEnabled(false);
            mSendButton.setEnabled(false);
            UI.hide(mNoteIcon);
            Futures.addCallback(service.processBip70URL(URI.getPaymentRequestUrl()),
                    new CB.Toast<Map<?, ?>>(gaActivity) {
                        @Override
                        public void onSuccess(final Map<?, ?> result) {
                            mPayreqData = result;

                            final String name;
                            if (result.get("merchant_cn") != null)
                                name = (String) result.get("merchant_cn");
                            else
                                name = (String) result.get("request_url");

                            long amount = 0;
                            for (final Map<?, ?> out : (ArrayList<Map>) result.get("outputs"))
                                amount += ((Number) out.get("amount")).longValue();
                            final CharSequence amountStr;
                            if (amount > 0) {
                                amountStr = UI.setCoinText(service, null, null, Coin.valueOf(amount));
                            } else
                                amountStr = "";

                            gaActivity.runOnUiThread(new Runnable() {
                                public void run() {
                                    mRecipientEdit.setText(name);
                                    mSendButton.setEnabled(true);
                                    if (!amountStr.toString().isEmpty()) {
                                        mAmountEdit.setText(amountStr);
                                        mAmountFields.convertBtcToFiat();
                                        mAmountEdit.setEnabled(false);
                                        mAmountFiatEdit.setEnabled(false);
                                        UI.hide(mMaxButton, mMaxLabel);
                                    }
                                    UI.hide(bip70Progress);
                                }
                            });
                        }

                        @Override
                        public void onFailure(final Throwable t) {
                            super.onFailure(t);
                            gaActivity.runOnUiThread(new Runnable() {
                                public void run() {
                                    UI.hide(bip70Progress);
                                    mRecipientEdit.setEnabled(true);
                                    mSendButton.setEnabled(true);
                                    UI.show(mNoteIcon);
                                }
                            });
                        }
                    });
        } else {
            if (confidentialAddress != null) {
                mRecipientEdit.setText(confidentialAddress);
            } else {
                mRecipientEdit.setText(URI.getAddress().toString());
                amount = URI.getAmount();
            }
            if (amount == null)
                return;

            final Coin uriAmount = amount;

            Futures.addCallback(service.getSubaccountBalance(mSubaccount), new CB.Op<Map<String, Object>>() {
                @Override
                public void onSuccess(final Map<String, Object> result) {
                    gaActivity.runOnUiThread(new Runnable() {
                            public void run() {
                                UI.setCoinText(service, null, mAmountEdit, uriAmount);
                                mAmountFields.convertBtcToFiat();
                                UI.disable(mAmountEdit, mAmountFiatEdit);
                                UI.hide(mMaxButton, mMaxLabel);
                            }
                    });
                }
            }, service.getExecutor());
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {

        Log.d(TAG, "onCreateView -> " + TAG);
        if (isZombieNoView())
            return null;

        final GaService service = getGAService();
        final GaActivity gaActivity = getGaActivity();

        if (savedInstanceState != null)
            mIsExchanger = savedInstanceState.getBoolean("isExchanger", false);

        final int viewId = mIsExchanger ? R.layout.fragment_exchanger_sell : R.layout.fragment_send;
        mView = inflater.inflate(viewId, container, false);

        if (mIsExchanger)
            mExchanger = new Exchanger(getContext(), service, mView, false, null);
        else
            mExchanger = null;
        mAmountFields = new AmountFields(service, getContext(), mView, mExchanger);

        if (savedInstanceState != null) {
            final Boolean pausing = savedInstanceState.getBoolean("pausing", false);
            mAmountFields.setIsPausing(pausing);
        }
        mSubaccount = service.getCurrentSubAccount();

        mSendButton = UI.find(mView, R.id.sendSendButton);
        mMaxButton = UI.find(mView, R.id.sendMaxButton);
        mMaxLabel = UI.find(mView, R.id.sendMaxLabel);
        mNoteText = UI.find(mView, R.id.sendToNoteText);
        mNoteIcon = UI.find(mView, R.id.sendToNoteIcon);
        mInstantConfirmationCheckbox = UI.find(mView, R.id.instantConfirmationCheckBox);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            // pre-Material Design the label was already a part of the switch
            UI.hide(mMaxLabel);
        }

        mAmountEdit = UI.find(mView, R.id.sendAmountEditText);
        mAmountFiatEdit = UI.find(mView, R.id.sendAmountFiatEditText);
        if (mIsExchanger)
            mAmountBtcWithCommission = UI.find(mView, R.id.amountBtcWithCommission);
        mRecipientEdit = UI.find(mView, R.id.sendToEditText);
        mScanIcon = UI.find(mView, R.id.sendScanIcon);

        if (mIsExchanger && GaService.IS_ELEMENTS) {
            mRecipientEdit.setHint(R.string.send_to_address);
            UI.hide(mAmountFiatEdit);
        }

        final TextView bitcoinUnitText = UI.find(mView, R.id.sendBitcoinUnitText);

        UI.setCoinText(service, bitcoinUnitText, null, null);

        if (container.getTag(R.id.tag_amount) != null)
            mAmountEdit.setText((String) container.getTag(R.id.tag_amount));

        if (container.getTag(R.id.tag_bitcoin_uri) != null) {
            final Uri uri = (Uri) container.getTag(R.id.tag_bitcoin_uri);
            BitcoinURI bitcoinUri = null;
            if (GaService.IS_ELEMENTS) {
                String addr = null;
                Coin amount = null;
                try {
                    final Pair<String, Coin> res = ConfidentialAddress.parseBitcoinURI(Network.NETWORK, uri.toString());
                    addr = res.first;
                    amount = res.second;
                } catch (final BitcoinURIParseException e) {
                    gaActivity.toast(R.string.err_send_invalid_bitcoin_uri);
                }
                if (addr != null)
                    processBitcoinURI(null, addr, amount);
            } else {
                try {
                    bitcoinUri = new BitcoinURI(uri.toString());
                } catch (final BitcoinURIParseException e) {
                    gaActivity.toast(R.string.err_send_invalid_bitcoin_uri);
                }
                if (bitcoinUri != null)
                    processBitcoinURI(bitcoinUri);
            }
            // set intent uri flag only if the call arrives from non internal qr scan
            if (container.getTag(R.id.internal_qr) == null) {
                mFromIntentURI = true;
                container.setTag(R.id.internal_qr, null);
            }
            container.setTag(R.id.tag_bitcoin_uri, null);
        }

        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                // FIXME: Instead of checking the state here, enable/disable sendButton when state changes
                if (!service.isLoggedIn()) {
                    gaActivity.toast(R.string.err_send_not_connected_will_resume);
                    return;
                }
                final String recipient = UI.getText(mRecipientEdit);

                if (recipient.isEmpty()) {
                    gaActivity.toast(R.string.err_send_need_recipient);
                    return;
                }
                onSendButtonClicked(recipient);
            }
        });

        if (GaService.IS_ELEMENTS) {
            UI.disable(mMaxButton); // FIXME: Sweeping not available in elements
            UI.hide(mMaxButton, mMaxLabel, mInstantConfirmationCheckbox);
        } else {
            mMaxButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(final CompoundButton v, final boolean isChecked) {
                    UI.disableIf(isChecked, mAmountEdit, mAmountFiatEdit);
                    mAmountEdit.setText(isChecked ? R.string.send_max_amount : R.string.empty);
                }
            });
        }

        mScanIcon.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(final View v) {
                                            //New Marshmallow permissions paradigm
                                            final String[] perms = {"android.permission.CAMERA"};
                                            if (Build.VERSION.SDK_INT>Build.VERSION_CODES.LOLLIPOP_MR1 &&
                                                    gaActivity.checkSelfPermission(perms[0]) != PackageManager.PERMISSION_GRANTED) {
                                                final int permsRequestCode = 100;
                                                gaActivity.requestPermissions(perms, permsRequestCode);
                                            } else {
                                                final Intent qrcodeScanner = new Intent(gaActivity, ScanActivity.class);
                                                qrcodeScanner.putExtra("sendAmount", mAmountEdit.getText().toString());
                                                int requestCode = TabbedMainActivity.REQUEST_SEND_QR_SCAN;
                                                if (mIsExchanger)
                                                    requestCode = TabbedMainActivity.REQUEST_SEND_QR_SCAN_EXCHANGER;
                                                gaActivity.startActivityForResult(qrcodeScanner, requestCode);
                                            }
                                        }
                                    }
        );

        mNoteIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                if (mNoteText.getVisibility() == View.VISIBLE) {
                    mNoteIcon.setText(R.string.fa_pencil);
                    UI.clear(mNoteText);
                    UI.hide(mNoteText);
                } else {
                    mNoteIcon.setText(R.string.fa_remove);
                    UI.show(mNoteText);
                    mNoteText.requestFocus();
                }
            }
        });

        hideInstantIf2of3();

        makeBalanceObserver(mSubaccount);
        if (service.getCoinBalance(mSubaccount) != null)
            onBalanceUpdated();

        registerReceiver();
        return mView;
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

    private void hideInstantIf2of3() {
        if (getGAService().findSubaccountByType(mSubaccount, "2of3") == null) {
            if (!GaService.IS_ELEMENTS)
                UI.show(mInstantConfirmationCheckbox);
            return;
        }
        UI.hide(mInstantConfirmationCheckbox);
        mInstantConfirmationCheckbox.setChecked(false);
    }

    @Override
    protected void onBalanceUpdated() {
        final GaService service = getGAService();
        final TextView sendSubAccountBalanceUnit = UI.find(mView, R.id.sendSubAccountBalanceUnit);
        final TextView sendSubAccountBalance = UI.find(mView, R.id.sendSubAccountBalance);
        final Coin balance = service.getCoinBalance(mSubaccount);
        UI.setCoinText(service, sendSubAccountBalanceUnit, sendSubAccountBalance, balance);

        final int nChars = sendSubAccountBalance.getText().length() + sendSubAccountBalanceUnit.getText().length();
        final int size = Math.min(50 - nChars, 34);
        sendSubAccountBalance.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
        sendSubAccountBalanceUnit.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
        if (service.showBalanceInTitle())
            UI.hide(sendSubAccountBalance, sendSubAccountBalanceUnit);
    }

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
        Log.d(TAG, "onPause -> " + TAG);
        if (mAmountFields != null)
            mAmountFields.setIsPausing(true);
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mAmountFields != null)
            outState.putBoolean("pausing", mAmountFields.isPausing());
        outState.putBoolean("isExchanger", mIsExchanger);
    }

    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView -> " + TAG);
        if (mSummary != null)
            mSummary.dismiss();
        if (mTwoFactor != null)
            mTwoFactor.dismiss();
    }

    @Override
    protected void onSubaccountChanged(final int newSubAccount) {
        mSubaccount = newSubAccount;

        if (!IsPageSelected()) {
            Log.d(TAG, "Subaccount changed while page hidden");
            setIsDirty(true);
            return;
        }
        updateBalance();
    }

    private void updateBalance() {
        Log.d(TAG, "Updating balance");
        if (isZombie())
            return;
        hideInstantIf2of3();
        makeBalanceObserver(mSubaccount);
        getGAService().updateBalance(mSubaccount);
    }

    public void setPageSelected(final boolean isSelected) {
        final boolean needReload = isDirty();
        super.setPageSelected(isSelected);
        if (needReload && isSelected) {
            Log.d(TAG, "Dirty, reloading");
            updateBalance();
            if (!isZombie())
                setIsDirty(false);
        }
    }

    private Coin getSendAmount() {
        try {
            final TextView amountEdit = mIsExchanger ? mAmountBtcWithCommission : mAmountEdit;
            return UI.parseCoinValue(getGAService(), UI.getText(amountEdit));
        } catch (final IllegalArgumentException e) {
            return Coin.ZERO;
        }
    }

    private void onSendButtonClicked(final String recipient) {
        final GaService service = getGAService();
        final GaActivity gaActivity = getGaActivity();

        final JSONMap privateData = new JSONMap();
        final String memo = UI.getText(mNoteText);
        if (!memo.isEmpty())
            privateData.mData.put("memo", memo);

        if (mIsExchanger)
            privateData.mData.put("memo", Exchanger.TAG_EXCHANGER_TX_MEMO);

        if (mSubaccount != 0)
            privateData.mData.put("subaccount", mSubaccount);

        final boolean isInstant = mInstantConfirmationCheckbox.isChecked();
        if (isInstant)
            privateData.mData.put("instant", true);

        final Coin amount = getSendAmount();

        if (mPayreqData != null) {
            final ListenableFuture<PreparedTransaction> ptxFn;
            ptxFn = service.preparePayreq(amount, mPayreqData, privateData);

            UI.disable(mSendButton);
            CB.after(ptxFn, new CB.Toast<PreparedTransaction>(gaActivity, mSendButton) {
                @Override
                public void onSuccess(final PreparedTransaction ptx) {
                    onTransactionPrepared(ptx, recipient, amount, privateData);
                }
            });
        } else {
            final boolean sendAll = mMaxButton.isChecked();
            final boolean validAddress = GaService.isValidAddress(recipient);
            final boolean validAmount = sendAll || amount.isGreaterThan(Coin.ZERO);

            int messageId = 0;
            if (!validAddress && !validAmount)
                messageId = R.string.invalidAmountAndAddress;
            else if (!validAddress)
                messageId = R.string.invalidAddress;
            else if (!validAmount)
                messageId = R.string.invalidAmount;

            if (messageId != 0) {
                gaActivity.toast(messageId);
                return;
            }

            UI.disable(mSendButton);
            final int numConfs;
            if (isInstant)
                numConfs = 6; // Instant requires at least 6 confs
            else if (Network.NETWORK == MainNetParams.get())
                numConfs = 1; // Require 1 conf before spending on mainnet
            else
                numConfs = 0; // Allow 0 conf for networks with no real-world value

            // For 2of2 accounts we first try to spend older coins to avoid
            // having to re-deposit them. If that fails (and always for 2of3
            // accounts) we try to use the minimum number of utxos instead.
            final boolean is2Of3 = service.findSubaccountByType(mSubaccount, "2of3") != null;
            final boolean minimizeInputs = is2Of3;
            final boolean filterAsset = true;

            CB.after(service.getAllUnspentOutputs(numConfs, mSubaccount, filterAsset),
                     new CB.Toast<List<JSONMap>>(gaActivity, mSendButton) {
                @Override
                public void onSuccess(final List<JSONMap> utxos) {
                    int ret = R.string.insufficientFundsText;
                    if (!utxos.isEmpty()) {
                        GATx.sortUtxos(utxos, minimizeInputs);
                        ret = createRawTransaction(utxos, recipient, amount, privateData, sendAll);
                        if (ret == R.string.insufficientFundsText && !minimizeInputs) {
                            // Not enough money using nlocktime outputs first:
                            // Try again using the largest values first
                            GATx.sortUtxos(utxos, true);
                            ret = createRawTransaction(utxos, recipient, amount, privateData, sendAll);
                        }
                    }
                    if (ret != 0)
                        gaActivity.toast(ret, mSendButton);
                }
            });
        }
    }

    private void onTransactionPrepared(final PreparedTransaction ptx,
                                       final String recipient, final Coin amount,
                                       final JSONMap privateData) {
        final GaService service = getGAService();
        final GaActivity gaActivity = getGaActivity();

        final Coin verifyAmount = mMaxButton.isChecked() ? null : amount;
        CB.after(service.validateTx(ptx, recipient, verifyAmount), new CB.Toast<Coin>(gaActivity, mSendButton) {
            @Override
            public void onSuccess(final Coin fee) {
                final Map<?, ?> twoFacConfig = service.getTwoFactorConfig();
                // can be non-UI because validation talks to USB if hw wallet is used
                gaActivity.runOnUiThread(new Runnable() {
                    public void run() {
                        mSendButton.setEnabled(true);
                        final Coin sendAmount, sendFee;
                        if (mMaxButton.isChecked()) {
                            // 'fee' is actually the sent amount when passed amount=null
                            sendAmount = fee;
                            sendFee = service.getCoinBalance(mSubaccount).subtract(sendAmount);
                        } else {
                            sendAmount = amount;
                            sendFee = fee;
                        }
                        final boolean skipChoice = !ptx.mRequiresTwoFactor ||
                                                    twoFacConfig == null || !((Boolean) twoFacConfig.get("any"));
                        mTwoFactor = UI.popupTwoFactorChoice(gaActivity, service, skipChoice,
                                                             new CB.Runnable1T<String>() {
                            @Override
                            public void run(final String method) {
                                onTransactionValidated(ptx, null, recipient, sendAmount, method,
                                                       sendFee, privateData, null);
                            }
                        });
                        if (mTwoFactor != null)
                            mTwoFactor.show();
                    }
                });
            }
        });
    }

    private void onTransactionValidated(final PreparedTransaction ptx,
                                        final Transaction signedRawTx,
                                        final String recipient, final Coin amount,
                                        final String method, final Coin fee,
                                        final JSONMap privateData,
                                        final Map<String, Object> underLimits) {
        Log.i(TAG, "onTransactionValidated( params " + method + ' ' + fee + ' ' + amount + ' ' + recipient + ')');
        final GaService service = getGAService();
        final GaActivity gaActivity = getGaActivity();

        final View v = gaActivity.getLayoutInflater().inflate(R.layout.dialog_new_transaction, null, false);

        UI.setCoinText(service, v, R.id.newTxAmountUnitText, R.id.newTxAmountText, amount);
        UI.setCoinText(service, v, R.id.newTxFeeUnit, R.id.newTxFeeText, fee);

        final TextView recipientText = UI.find(v, R.id.newTxRecipientText);
        final TextView twoFAText = UI.find(v, R.id.newTx2FATypeText);
        final EditText newTx2FACodeText = UI.find(v, R.id.newTx2FACodeText);

        if (mPayreqData != null)
            recipientText.setText(recipient);
        else
            recipientText.setText(String.format("%s\n%s\n%s",
                                  recipient.substring(0, 12),
                                  recipient.substring(12, 24),
                                  recipient.substring(24)));

        UI.showIf(method != null && !method.equals("limit"), twoFAText, newTx2FACodeText);

        final Map<String, Object> twoFacData;

        if (method == null)
            twoFacData = null;
        else if (method.equals("limit")) {
            twoFacData = new HashMap<>();
            twoFacData.put("try_under_limits_spend", underLimits);
        } else {
            twoFacData = new HashMap<>();
            twoFacData.put("method", method);
            twoFAText.setText(String.format("2FA %s code", method));
            if (!method.equals("gauth")) {
                if (underLimits != null)
                    for (final String key : underLimits.keySet())
                        twoFacData.put("send_raw_tx_" + key, underLimits.get(key));
                if (GaService.IS_ELEMENTS) {
                    underLimits.remove("ephemeral_privkeys");
                    underLimits.remove("blinding_pubkeys");
                }
                service.requestTwoFacCode(method, ptx == null ? "send_raw_tx" : "send_tx", underLimits);
            }
        }

        mSummary = UI.popup(gaActivity, R.string.newTxTitle, R.string.send, R.string.cancel)
                .customView(v, true)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(final MaterialDialog dialog, final DialogAction which) {
                        if (twoFacData != null && !method.equals("limit"))
                            twoFacData.put("code", UI.getText(newTx2FACodeText));

                        if (signedRawTx != null) {
                            final ListenableFuture<Map<String,Object>> sendFuture;
                            sendFuture = service.sendRawTransaction(signedRawTx, twoFacData, privateData, false);
                            Futures.addCallback(sendFuture, new CB.Toast<Map<String,Object>>(gaActivity, mSendButton) {
                                @Override
                                public void onSuccess(final Map result) {
                                    if (GaService.IS_ELEMENTS && twoFacData != null && method.equals("limit")) {
                                        // FIXME: Store limits for non-elements w/configurable m/u/bits units
                                        service.cfg().edit().putString(
                                            "twoFacLimits",
                                            UI.formatCoinValue(
                                                service, Coin.valueOf(((Number) result.get("new_limit")).longValue())
                                            )
                                        ).apply();
                                    }
                                    onTransactionSent();
                                }
                            }, service.getExecutor());
                        } else {
                            final ListenableFuture<String> sendFuture = service.signAndSendTransaction(ptx, twoFacData);
                            Futures.addCallback(sendFuture, new CB.Toast<String>(gaActivity, mSendButton) {
                                @Override
                                public void onSuccess(final String result) {
                                    onTransactionSent();
                                }
                            }, service.getExecutor());
                        }
                    }
                }).build();
        UI.mapEnterToPositive(mSummary, R.id.newTx2FACodeText);
        mSummary.show();
    }

    private void onTransactionSent() {
        final GaActivity gaActivity = getGaActivity();

        gaActivity.runOnUiThread(new Runnable() {
            public void run() {
                UI.toast(gaActivity, R.string.transactionCompleted, Toast.LENGTH_LONG);

                if (mIsExchanger) {
                    final float fiatAmount = Float.valueOf(mAmountFiatEdit.getText().toString());
                    mExchanger.sellBtc(fiatAmount);
                }

                if (mFromIntentURI) {
                    gaActivity.finish();
                    return;
                }

                UI.clear(mAmountEdit, mRecipientEdit);
                UI.enable(mAmountEdit, mRecipientEdit);
                if (!GaService.IS_ELEMENTS) {
                    mMaxButton.setChecked(false);
                    UI.show(mMaxButton, mMaxLabel);
                }

                mNoteIcon.setText(R.string.fa_pencil);
                UI.clear(mNoteText);
                UI.hide(mNoteText);

                if (!mIsExchanger) {
                    final ViewPager viewPager = UI.find(gaActivity, R.id.container);
                    viewPager.setCurrentItem(1);
                } else {
                    gaActivity.toast(R.string.transactionCompleted);
                    gaActivity.finish();
                }
            }
        });
    }

    private static double extractRate(final Map feeEstimates, final Integer blockNum) {
        final Map estimate = (Map) feeEstimates.get(Integer.toString(blockNum));
        return Double.parseDouble(estimate.get("feerate").toString());
    }

    // FIXME: Duplicated in TransactionActivity.java
    // Return the best estimate of the fee rate in satoshi/1000 bytes
    private static Coin getFeeEstimate(final GaService service, final boolean isInstant) {
        final Map<String, Object> feeEstimates = service.getFeeEstimates();
        Double bestInstant = null;

        // Iterate the estimates from shortest to longest confirmation time
        final SortedSet<Integer> keys = new TreeSet<>();
        for (final String block : feeEstimates.keySet())
            keys.add(Integer.parseInt(block));

        for (final Integer blockNum : keys) {
            if (!isInstant && blockNum < 6)
                continue; // Non-instant: Use 6 confirmation rate and later only

            double rate = extractRate(feeEstimates, blockNum);
            if (rate <= 0.0)
                continue; // No estimate available: Try next confirmation rate

            if (isInstant) {
                // For instant, increase the rate to increase the likelyhood of confirmation.
                // We use the lowest value of:
                // a) 1.1 * the 1st or 2nd block fee rate
                // b) 2.0 * the first rate later than 2 blocks
                if (blockNum <= 2) {
                    if (bestInstant == null)
                       bestInstant = rate * 1.1; // Save earliest fast confirmation rate
                    continue; // Continue to find the first non-fast rate
                } else
                    rate *= 2.0;
            }

            if (bestInstant != null && bestInstant < rate)
                rate = bestInstant; // Use the lowest instant rate found

            return Coin.valueOf((long) (rate * 1000 * 1000 * 100));
        }

        if (bestInstant != null) {
            // No non-fast confirmation rate, return the fast confirmation rate
            return Coin.valueOf((long) (bestInstant * 1000 * 1000 * 100));
        }

        // We don't have a usable fee rate estimate, use a default.
        if (GaService.IS_ELEMENTS)
            return Coin.valueOf(1);
        if (Network.NETWORK == MainNetParams.get())
            return Coin.valueOf((isInstant ? 200 : 120) * 1000);
        return Coin.valueOf((isInstant ? 75 : 60) * 1000);
    }


    private Coin addUtxo(final Transaction tx,
                         final List<JSONMap> utxos, final List<JSONMap> used) {
        return addUtxo(tx, utxos, used, null, null, null, null);
    }

    private Coin addUtxo(final Transaction tx,
                         final List<JSONMap> utxos, final List<JSONMap> used,
                         final List<Long> inValues, final List<byte[]> inAssetIds,
                         final List<byte[]> inAbfs, final List<byte[]> inVbfs) {
        final JSONMap utxo = utxos.get(0);
        final GaService service = getGAService();
        utxos.remove(0);
        if (utxo.getBool("confidential")) {
            inAssetIds.add(utxo.getBytes("assetId"));
            inAbfs.add(utxo.getBytes("abf"));
            inVbfs.add(utxo.getBytes("vbf"));
        }
        used.add(utxo);
        GATx.addInput(service, tx, utxo);
        if (inValues != null)
            inValues.add(utxo.getLong("value"));
        return utxo.getCoin("value");
    }

    private int createRawTransaction(final List<JSONMap> utxos, final String recipient,
                                     final Coin amount, final JSONMap privateData,
                                     final boolean sendAll) {

        if (GaService.IS_ELEMENTS)
            return createRawElementsTransaction(utxos, recipient, amount, privateData, sendAll);

        final GaActivity gaActivity = getGaActivity();
        final GaService service = getGAService();
        final List<JSONMap> used = new ArrayList<>();
        final Coin feeRate = getFeeEstimate(service, privateData.getBool("instant"));

        final Transaction tx = new Transaction(Network.NETWORK);
        tx.addOutput(amount, Address.fromBase58(Network.NETWORK, recipient));

        Coin total = Coin.ZERO;
        Coin fee;
        boolean randomizedChange = false;
        Pair<TransactionOutput, Integer> changeOutput = null;

        // First add inputs until we cover the amount to send
        while ((sendAll || total.isLessThan(amount)) && !utxos.isEmpty())
            total = total.add(addUtxo(tx, utxos, used));

        // Then add inputs until we cover amount + fee/change
        while (true) {
            fee = GATx.getTxFee(service, tx, feeRate);

            final Coin minChange = changeOutput == null ? Coin.ZERO : service.getDustThreshold();
            final int cmp = sendAll ? 0 : total.compareTo(amount.add(fee).add(minChange));
            if (cmp < 0) {
                // Need more inputs to cover amount + fee/change
                if (utxos.isEmpty())
                    return R.string.insufficientFundsText; // None left, fail

                total = total.add(addUtxo(tx, utxos, used));
                continue;
            }

            if (cmp == 0 || changeOutput != null) {
                // Inputs exactly match amount + fee/change, or are greater
                // and we have a change output for the excess
                break;
            }

            // Inputs greater than amount + fee, add a change output and try again
            changeOutput = GATx.addChangeOutput(service, tx, mSubaccount);
            if (changeOutput == null)
                return R.string.unable_to_create_change;
        }

        if (changeOutput != null) {
            // Set the value of the change output
            changeOutput.first.setValue(total.subtract(amount).subtract(fee));
            randomizedChange = GATx.randomizeChange(tx);
        }

        final Coin actualAmount;
        if (!sendAll)
            actualAmount = amount;
        else {
            actualAmount = total.subtract(fee);
            if (!actualAmount.isGreaterThan(Coin.ZERO))
                return R.string.insufficientFundsText;
            tx.getOutputs().get(0).setValue(actualAmount);
        }

        tx.setLockTime(service.getCurrentBlock()); // Prevent fee sniping

        // Fetch previous outputs
        final List<Output> prevOuts = GATx.createPrevouts(service, used);
        final PreparedTransaction ptx = new PreparedTransaction(
                changeOutput == null ? null : changeOutput.second,
                mSubaccount, tx, service.findSubaccountByType(mSubaccount, "2of3")
        );
        ptx.mPrevoutRawTxs = new HashMap<>();
        for (final Transaction prevTx : GATx.getPreviousTransactions(service, tx))
            ptx.mPrevoutRawTxs.put(Wally.hex_from_bytes(prevTx.getHash().getBytes()), prevTx);

        final boolean isSegwitEnabled = service.isSegwitEnabled();

        // Sign the tx
        final List<byte[]> signatures = service.signTransaction(tx, ptx, prevOuts);
        for (int i = 0; i < signatures.size(); ++i) {
            final byte[] sig = signatures.get(i);
            // FIXME: Massive duplication with TransactionActivity
            final JSONMap utxo = used.get(i);
            final int scriptType = utxo.getInt("script_type");
            final byte[] outscript = GATx.createOutScript(service, utxo);
            final List<byte[]> userSigs = ImmutableList.of(new byte[]{0}, sig);
            final byte[] inscript = GATx.createInScript(userSigs, outscript, scriptType);

            tx.getInput(i).setScriptSig(new Script(inscript));
            if (isSegwitEnabled && scriptType == GATx.P2SH_P2WSH_FORTIFIED_OUT) {
                final TransactionWitness witness = new TransactionWitness(1);
                witness.setPush(0, sig);
                tx.setWitness(i, witness);
            }
        }

        final Map<?, ?> twoFacConfig = service.getTwoFactorConfig();
        final Coin sendFee = fee;

        final Map underLimits = new HashMap();
        underLimits.put("asset", "BTC");
        underLimits.put("amount", amount.add(sendFee).getValue());
        underLimits.put("fee", sendFee.getValue());
        underLimits.put("change_idx", changeOutput == null ? -1 : (randomizedChange ? 0 : 1));

        gaActivity.runOnUiThread(new Runnable() {
            public void run() {
                mSendButton.setEnabled(true);
                final boolean skipChoice = /* FIXME: !ptx.mRequiresTwoFactor || */
                                            twoFacConfig == null || !((Boolean) twoFacConfig.get("any"));
                mTwoFactor = UI.popupTwoFactorChoice(gaActivity, service, skipChoice,
                                                     new CB.Runnable1T<String>() {
                    @Override
                    public void run(final String method) {
                        onTransactionValidated(null, tx, recipient, actualAmount,
                                               method, sendFee, privateData, underLimits);
                    }
                });
                if (mTwoFactor != null)
                    mTwoFactor.show();
            }
        });
        return 0;
    }

    private void arraycpy(final byte[] dest, final int i, final byte[] src) {
        System.arraycopy(src, 0, dest, src.length * i, src.length);
    }

    private int createRawElementsTransaction(final List<JSONMap> utxos, final String recipient,
                                             final Coin amount, final JSONMap privateData,
                                             final boolean sendAll) {
        // FIXME: sendAll
        final GaService service = getGAService();
        final GaActivity gaActivity = getGaActivity();

        final List<JSONMap> used = new ArrayList<>();
        final Coin feeRate = getFeeEstimate(service, privateData.getBool("instant"));

        final ElementsTransaction tx = new ElementsTransaction(Network.NETWORK);

        final ElementsTransactionOutput feeOutput = new ElementsTransactionOutput(Network.NETWORK, tx, Coin.ZERO);

        feeOutput.setUnblindedAssetTagFromAssetId(service.mAssetId);
        feeOutput.setValue(Coin.valueOf(1));  // updated below, necessary for serialization for fee calculation
        tx.addOutput(feeOutput);
        TransactionOutput changeOutput = null;

        tx.addOutput(service.mAssetId, amount, ConfidentialAddress.fromBase58(Network.NETWORK, recipient));

        Coin total = Coin.ZERO;
        Coin fee;

        final List<Long> inValues = new ArrayList<>();
        final List<byte[]> inAssetIds = new ArrayList<>();
        final List<byte[]> inAbfs = new ArrayList<>();
        final List<byte[]> inVbfs = new ArrayList<>();

        // First add inputs until we cover the amount to send
        while (total.isLessThan(amount) && !utxos.isEmpty())
            total = total.add(addUtxo(tx, utxos, used, inValues, inAssetIds, inAbfs, inVbfs));

        // Then add inputs until we cover amount + fee/change
        while (true) {
            fee = GATx.getTxFee(service, tx, feeRate);

            final Coin minChange = changeOutput == null ? Coin.ZERO : service.getDustThreshold();
            final int cmp = total.compareTo(amount.add(fee).add(minChange));
            if (cmp < 0) {
                // Need more inputs to cover amount + fee/change
                if (utxos.isEmpty())
                    return R.string.insufficientFundsText; // None left, fail
                total = total.add(addUtxo(tx, utxos, used, inValues, inAssetIds, inAbfs, inVbfs));
                continue;
            }

            if (cmp == 0 || changeOutput != null) {
                // Inputs exactly match amount + fee/change, or are greater
                // and we have a change output for the excess
                break;
            }

            // Inputs greater than amount + fee, add a change output and try again
            final JSONMap addr = service.getNewAddress(mSubaccount);
            if (addr == null)
                return R.string.unable_to_create_change;

            final byte[] script = addr.getBytes("script");
            changeOutput = tx.addOutput(
                    service.mAssetId, Coin.ZERO,
                    ConfidentialAddress.fromP2SHHash(
                            Network.NETWORK, Wally.hash160(script),
                            service.getBlindingPubKey(mSubaccount, addr.getInt("pointer"))
                    )
            );
        }

        if (changeOutput != null) {
            // Set the value of the change output
            ((ElementsTransactionOutput)changeOutput).setUnblindedValue(total.subtract(amount).subtract(fee).getValue());
            // TODO: randomize change
            // GATx.randomizeChange(tx);
        }

        feeOutput.setValue(fee);

        // FIXME: tx.setLockTime(latestBlock); // Prevent fee sniping

        // Fetch previous outputs
        final List<Output> prevOuts = GATx.createPrevouts(service, used);

        final Map<?, ?> twoFacConfig = service.getTwoFactorConfig();
        final Coin sendFee = fee;

        final int numInputs = tx.getInputs().size();
        final int numOutputs = tx.getOutputs().size();
        final int numInOuts = numInputs + numOutputs;

        final long[] values = new long[numInOuts];
        final byte[] abfs = new byte[32 * (numInOuts)];
        final byte[] vbfs = new byte[32 * (numInOuts - 1)];
        final byte[] assetids = new byte[32 * numInputs];
        final byte[] ags = new byte[33 * numInputs];

        for (int i = 0; i < numInputs; ++i) {
            values[i] = inValues.get(i);
            arraycpy(abfs, i, inAbfs.get(i));
            arraycpy(vbfs, i, inVbfs.get(i));
            arraycpy(assetids, i, inAssetIds.get(i));
            arraycpy(ags, i, Wally.asset_generator_from_bytes(inAssetIds.get(i), inAbfs.get(i)));
        }

        for (int i = 0; i < numOutputs; ++i) {
            final ElementsTransactionOutput output = (ElementsTransactionOutput) tx.getOutput(i);

            // Fee: FIXME: Assumes fee is the first output
            values[numInputs + i] = i == 0 ? output.getValue().getValue() : output.getUnblindedValue();
            arraycpy(abfs, numInputs + i, output.getAbf());
            if (i == numOutputs - 1) {
                // Compute the final VBF
                output.setAbfVbf(null, Wally.asset_final_vbf(values, numInputs, abfs, vbfs), service.mAssetId);
            } else
                arraycpy(vbfs, numInputs + i, output.getVbf());
        }

        final boolean isSegwitEnabled = service.isSegwitEnabled();

        // fee output:
        tx.addOutWitness(new byte[0], new byte[0], new byte[0]);

        final ArrayList<String> ephemeralKeys = new ArrayList<>();
        final ArrayList<String> blindingKeys = new ArrayList<>();

        ephemeralKeys.add("00");
        blindingKeys.add("00");

        for (int i = 1; i < numOutputs; ++i) {
            final ElementsTransactionOutput out = (ElementsTransactionOutput) tx.getOutput(i);

            final byte[] ephemeral = CryptoHelper.randomBytes(32);
            ephemeralKeys.add(Wally.hex_from_bytes(ephemeral));
            blindingKeys.add(Wally.hex_from_bytes(out.getBlindingPubKey()));
            final byte[] rangeproof = Wally.asset_rangeproof(
                    out.getUnblindedValue(), out.getBlindingPubKey(), ephemeral,
                    out.getAssetId(), out.getAbf(), out.getVbf(),
                    out.getCommitment(), out.getAssetTag()
            );
            final byte[] surjectionproof = Wally.asset_surjectionproof(
                    out.getAssetId(), out.getAbf(), out.getAssetTag(),
                    CryptoHelper.randomBytes(32),
                    assetids, Arrays.copyOf(abfs, 32 * numInputs), ags
            );
            final byte[] nonceCommitment = Wally.ec_public_key_from_private_key(ephemeral);
            tx.addOutWitness(surjectionproof, rangeproof, nonceCommitment);
        }

        // FIXME: Implement HW Signing
        /*
        final PreparedTransaction ptx = new PreparedTransaction(
                changeOutput == null ? null : changeOutput.second,
                mSubaccount, tx, service.findSubaccountByType(mSubaccount, "2of3")
        );
        ptx.mPrevoutRawTxs = new HashMap<>();
        for (final Transaction prevTx : GATx.getPreviousTransactions(service, tx))
            ptx.mPrevoutRawTxs.put(Wally.hex_from_bytes(prevTx.getHash().getBytes()), prevTx);
        */
        final PreparedTransaction ptx = null;

        // Sign the tx
        final List<byte[]> signatures = service.signTransaction(tx, ptx, prevOuts);
        for (int i = 0; i < signatures.size(); ++i) {
            final byte[] sig = signatures.get(i);
            // FIXME: Massive duplication with TransactionActivity
            final JSONMap utxo = used.get(i);
            final int scriptType = utxo.getInt("script_type");
            final byte[] outscript = GATx.createOutScript(service, utxo);
            final List<byte[]> userSigs = ImmutableList.of(new byte[]{0}, sig);
            final byte[] inscript = GATx.createInScript(userSigs, outscript, scriptType);

            tx.getInput(i).setScriptSig(new Script(inscript));
            if (isSegwitEnabled && scriptType == GATx.P2SH_P2WSH_FORTIFIED_OUT) {
                final TransactionWitness witness = new TransactionWitness(1);
                witness.setPush(0, sig);
                tx.setWitness(i, witness);
            }
        }

        final Map underLimits = new HashMap();
        underLimits.put("asset_id", Wally.hex_from_bytes(service.mAssetId)); // FIXME: Others
        underLimits.put("amount", amount.add(sendFee).getValue());
        underLimits.put("fee", sendFee.getValue());
        underLimits.put("change_idx", changeOutput == null ? -1 : 2);
        underLimits.put("ephemeral_privkeys", ephemeralKeys);
        underLimits.put("blinding_pubkeys", blindingKeys);
        final Coin finalFee = fee;

        gaActivity.runOnUiThread(new Runnable() {
            public void run() {
                mSendButton.setEnabled(true);
                final String limit = service.cfg().getString("twoFacLimits", "0");
                final boolean skipChoice = /* FIXME: !ptx.mRequiresTwoFactor || */
                        twoFacConfig == null || !((Boolean) twoFacConfig.get("any")) ||
                        amount.add(finalFee).getValue() < Float.valueOf(limit)*100;
                mTwoFactor = UI.popupTwoFactorChoice(gaActivity, service, skipChoice,
                        new CB.Runnable1T<String>() {
                            @Override
                            public void run(String method) {
                                if (twoFacConfig != null && ((Boolean) twoFacConfig.get("any")) &&
                                    amount.add(finalFee).getValue() < Float.valueOf(limit) * 100) {
                                    method = "limit";
                                }
                                onTransactionValidated(null, tx, recipient, amount, method,
                                                       sendFee, privateData, underLimits);
                            }
                        });
                if (mTwoFactor != null)
                    mTwoFactor.show();
            }
        });
        return 0;
    }

    public void setIsExchanger(final boolean isExchanger) {
        mIsExchanger = isExchanger;
    }
}
