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
import com.greenaddress.greenapi.GATx;
import com.greenaddress.greenapi.JSONMap;
import com.greenaddress.greenapi.Network;
import com.greenaddress.greenapi.Output;
import com.greenaddress.greenapi.PreparedTransaction;
import com.greenaddress.greenbits.GaService;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.TransactionWitness;
import org.bitcoinj.script.Script;
import org.bitcoinj.uri.BitcoinURI;
import org.bitcoinj.uri.BitcoinURIParseException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.schildbach.wallet.ui.ScanActivity;

public class SendFragment extends SubaccountFragment {

    private static final String TAG = SendFragment.class.getSimpleName();
    private static final int REQUEST_SEND_QR_SCAN = 0;

    private Dialog mSummary;
    private Dialog mTwoFactor;
    private EditText mAmountEdit;
    private EditText mAmountFiatEdit;
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

    private void processBitcoinURI(final BitcoinURI URI) {
        final GaService service = getGAService();
        final GaActivity gaActivity = getGaActivity();

        if (URI.getPaymentRequestUrl() != null) {
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
            mRecipientEdit.setText(URI.getAddress().toString());
            if (URI.getAmount() == null)
                return;

            Futures.addCallback(service.getSubaccountBalance(mSubaccount), new CB.Op<Map<?, ?>>() {
                @Override
                public void onSuccess(final Map<?, ?> result) {
                    gaActivity.runOnUiThread(new Runnable() {
                            public void run() {
                                final Coin uriAmount = URI.getAmount();
                                UI.setCoinText(service, null, mAmountEdit, uriAmount);

                                final Float fiatRate = Float.valueOf((String) result.get("fiat_exchange"));
                                mAmountFields.convertBtcToFiat(fiatRate);
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

        mView = inflater.inflate(R.layout.fragment_send, container, false);

        mAmountFields = new AmountFields(service, getContext(), mView, null);
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
        mRecipientEdit = UI.find(mView, R.id.sendToEditText);
        mScanIcon = UI.find(mView, R.id.sendScanIcon);

        final TextView bitcoinUnitText = UI.find(mView, R.id.sendBitcoinUnitText);

        UI.setCoinText(service, bitcoinUnitText, null, null);

        if (container.getTag(R.id.tag_amount) != null)
            mAmountEdit.setText((String) container.getTag(R.id.tag_amount));

        if (container.getTag(R.id.tag_bitcoin_uri) != null) {
            final Uri uri = (Uri) container.getTag(R.id.tag_bitcoin_uri);
            BitcoinURI bitcoinUri = null;
            try {
                bitcoinUri = new BitcoinURI(uri.toString());
            } catch (final BitcoinURIParseException e) {
                gaActivity.toast(R.string.err_send_invalid_bitcoin_uri);
            }
            if (bitcoinUri != null)
                processBitcoinURI(bitcoinUri);
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
                                                gaActivity.startActivityForResult(qrcodeScanner, TabbedMainActivity.REQUEST_SEND_QR_SCAN);
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
            return UI.parseCoinValue(getGAService(), UI.getText(mAmountEdit));
        } catch (final IllegalArgumentException e) {
            return Coin.ZERO;
        }
    }

    private void onSendButtonClicked(final String recipient) {
        final GaService service = getGAService();
        final GaActivity gaActivity = getGaActivity();

        final Map<String, Object> privateData = new HashMap<>();
        final String memo = UI.getText(mNoteText);
        if (!memo.isEmpty())
            privateData.put("memo", memo);

        if (mSubaccount != 0)
            privateData.put("subaccount", mSubaccount);

        if (mInstantConfirmationCheckbox.isChecked())
            privateData.put("instant", true);

        final Coin amount = getSendAmount();

        if (mPayreqData != null) {
            final ListenableFuture<PreparedTransaction> ptxFn;
            ptxFn = service.preparePayreq(amount, mPayreqData, privateData);

            UI.disable(mSendButton);
            CB.after(ptxFn, new CB.Toast<PreparedTransaction>(gaActivity, mSendButton) {
                @Override
                public void onSuccess(final PreparedTransaction ptx) {
                    onTransactionPrepared(ptx, recipient, amount);
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
            CB.after(service.getAllUnspentOutputs(1, mSubaccount), new CB.Toast<ArrayList>(gaActivity, mSendButton) {
                @Override
                public void onSuccess(final ArrayList utxos) {
                    createRawTransaction(utxos, recipient, amount, sendAll);
                }
            });
        }
    }

    private void onTransactionPrepared(final PreparedTransaction ptx,
                                       final String recipient, final Coin amount) {
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
                                onTransactionValidated(ptx, null, recipient, sendAmount, method, sendFee);
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
                                        final String method, final Coin fee) {
        Log.i(TAG, "onTransactionValidated( params " + method + ' ' + fee + ' ' + amount + ' ' + recipient + ")");
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

        UI.showIf(method != null, twoFAText, newTx2FACodeText);

        final Map<String, Object> twoFacData;

        if (method == null)
            twoFacData = null;
        else {
            twoFacData = new HashMap<>();
            twoFacData.put("method", method);
            twoFAText.setText(String.format("2FA %s code", method));
            if (!method.equals("gauth"))
                service.requestTwoFacCode(method, ptx == null ? "send_raw_tx" : "send_tx" , null);
        }

        mSummary = UI.popup(gaActivity, R.string.newTxTitle, R.string.send, R.string.cancel)
                .customView(v, true)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(final MaterialDialog dialog, final DialogAction which) {
                        if (twoFacData != null)
                            twoFacData.put("code", UI.getText(newTx2FACodeText));

                        if (signedRawTx != null) {
                            final ListenableFuture<Map<String,Object>> sendFuture = service.sendRawTransaction(signedRawTx, twoFacData, false);
                            Futures.addCallback(sendFuture, new CB.Toast<Map<String,Object>>(gaActivity, mSendButton) {
                                @Override
                                public void onSuccess(final Map result) {
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

        mSummary.show();
    }

    private void onTransactionSent() {
        final GaActivity gaActivity = getGaActivity();

        gaActivity.runOnUiThread(new Runnable() {
            public void run() {
                UI.toast(gaActivity, R.string.transactionCompleted, Toast.LENGTH_LONG);

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

                final ViewPager viewPager = UI.find(gaActivity, R.id.container);
                viewPager.setCurrentItem(1);
            }
        });
    }

    // FIXME: Duplicated in TransactionActivity.java
    private static Coin getFeeEstimate(final GaService service, final String atBlock) {
        // FIXME: Better estimate?
        final Map<String, Object> feeEstimates = service.getFeeEstimates();

        final double rate = Double.parseDouble(((Map) feeEstimates.get(atBlock)).get("feerate").toString());
        if (rate > 0) {
            return Coin.valueOf((long) (rate * 1000 * 1000 * 100));
        }
        // A negative rate means we don't have a good estimate. Default to
        // 10000 satoshi per 1000 bytes to match the JS wallets.
        // FIXME: This results in overpaying fees
        return Coin.valueOf(10000);
    }

    private Coin addUtxo(final Transaction tx,
                         final List<JSONMap> candidates, final List<JSONMap> used) {
        final JSONMap utxo = candidates.get(0);
        used.add(utxo);
        GATx.addInput(getGAService(), tx, utxo);
        candidates.remove(0);
        return utxo.getCoin("value");
    }

    // FIXME: This uses a terrible utxo selection strategy
    private void createRawTransaction(final ArrayList utxos, final String recipient,
                                      final Coin amount, final boolean sendAll) {
        final GaService service = getGAService();
        final GaActivity gaActivity = getGaActivity();

        if (utxos.isEmpty()) {
            gaActivity.toast(R.string.insufficientFundsText, mSendButton);
            return;
        }

        final List<JSONMap> candidates = JSONMap.fromList(utxos);
        final List<JSONMap> used = new ArrayList<>();
        final Coin feeRate = getFeeEstimate(service, "1");

        final Transaction tx = new Transaction(Network.NETWORK);
        tx.addOutput(amount, Address.fromBase58(Network.NETWORK, recipient));

        Coin total = Coin.ZERO;
        Coin fee;
        boolean randomizedChange = false;
        Pair<TransactionOutput, Integer> changeOutput = null;

        // First add inputs until we cover the amount to send
        while ((sendAll || total.isLessThan(amount)) && !candidates.isEmpty())
            total = total.add(addUtxo(tx, candidates, used));

        // Then add inputs until we cover amount + fee/change
        while (true) {
            fee = GATx.getTxFee(service, tx, feeRate);

            final Coin minChange = changeOutput == null ? Coin.ZERO : service.getDustThreshold();
            final int cmp = sendAll ? 0 : total.compareTo(amount.add(fee).add(minChange));
            if (cmp < 0) {
                // Need more inputs to cover amount + fee/change
                if (candidates.isEmpty()) {
                    gaActivity.toast(R.string.insufficientFundsText, mSendButton); // None left, fail
                    return;
                }
                total = total.add(addUtxo(tx, candidates, used));
                continue;
            }

            if (cmp == 0 || changeOutput != null) {
                // Inputs exactly match amount + fee/change, or are greater
                // and we have a change output for the excess
                break;
            }

            // Inputs greater than amount + fee, add a change output and try again
            changeOutput = GATx.addChangeOutput(service, tx, mSubaccount);
            if (changeOutput == null) {
                gaActivity.toast(R.string.unable_to_create_change, mSendButton);
                return;
            }
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
            if (!actualAmount.isGreaterThan(Coin.ZERO)) {
                gaActivity.toast(R.string.insufficientFundsText, mSendButton);
                return;
            }
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

        gaActivity.runOnUiThread(new Runnable() {
            public void run() {
                mSendButton.setEnabled(true);
                final boolean skipChoice = /* FIXME: !ptx.mRequiresTwoFactor || */
                                            twoFacConfig == null || !((Boolean) twoFacConfig.get("any"));
                mTwoFactor = UI.popupTwoFactorChoice(gaActivity, service, skipChoice,
                                                     new CB.Runnable1T<String>() {
                    @Override
                    public void run(final String method) {
                        onTransactionValidated(null, tx, recipient, actualAmount, method, sendFee);
                    }
                });
                if (mTwoFactor != null)
                    mTwoFactor.show();
            }
        });
    }
}
