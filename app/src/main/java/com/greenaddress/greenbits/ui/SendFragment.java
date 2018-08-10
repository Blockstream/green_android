package com.greenaddress.greenbits.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.util.Pair;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.blockstream.libwally.Wally;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.InvalidProtocolBufferException;
import com.greenaddress.greenapi.ConfidentialAddress;
import com.greenaddress.greenapi.CryptoHelper;
import com.greenaddress.greenapi.ElementsTransaction;
import com.greenaddress.greenapi.ElementsTransactionOutput;
import com.greenaddress.greenapi.GAException;
import com.greenaddress.greenapi.GATx;
import com.greenaddress.greenapi.JSONMap;
import com.greenaddress.greenapi.Output;
import com.greenaddress.greenapi.PreparedTransaction;
import com.greenaddress.greenbits.GaService;

import org.bitcoin.protocols.payments.Protos;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.TransactionWitness;
import org.bitcoinj.protocols.payments.PaymentProtocol;
import org.bitcoinj.protocols.payments.PaymentProtocolException;
import org.bitcoinj.protocols.payments.PaymentSession;
import org.bitcoinj.script.Script;
import org.bitcoinj.uri.BitcoinURI;
import org.bitcoinj.uri.BitcoinURIParseException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
    private Spinner mFeeTargetCombo;
    private EditText mFeeTargetEdit;
    private TextView mNoteIcon;
    private Button mSendButton;
    private Switch mMaxButton;
    private TextView mMaxLabel;
    private TextView mScanIcon;
    private ProgressBar mBip70Progress;

    private Protos.PaymentRequest mPayreqData;
    private Protos.PaymentDetails mPayreqDetails;
    private boolean mFromIntentURI;
    private final boolean mSummaryInBtc[] = new boolean[1]; // State for fiat/btc toggle

    private int mSubaccount;
    private int mTwoFactorAttemptsRemaining;
    private AmountFields mAmountFields;

    private boolean mIsExchanger;
    private Exchanger mExchanger;

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
        mFeeTargetEdit = UI.find(mView, R.id.feerateTextEdit);
        mFeeTargetCombo = UI.find(mView, R.id.feeTargetCombo);
        mBip70Progress = UI.find(mView, R.id.sendBip70ProgressBar);
        UI.hide(mBip70Progress);
        populateFeeCombo();

        if (Build.VERSION.SDK_INT < 21) {
            // pre-Material Design the label was already a part of the switch
            UI.hide(mMaxLabel);
        }

        mAmountEdit = UI.find(mView, R.id.sendAmountEditText);
        mAmountFiatEdit = UI.find(mView, R.id.sendAmountFiatEditText);
        if (mIsExchanger)
            mAmountBtcWithCommission = UI.find(mView, R.id.amountBtcWithCommission);
        mRecipientEdit = UI.find(mView, R.id.sendToEditText);
        mScanIcon = UI.find(mView, R.id.sendScanIcon);

        if (mIsExchanger && service.isElements()) {
            mRecipientEdit.setHint(R.string.send_to_address);
            UI.hide(mAmountFiatEdit);
        }

        final FontAwesomeTextView bitcoinUnitText = UI.find(mView, R.id.sendBitcoinUnitText);
        UI.setCoinText(service, bitcoinUnitText, null, null);

        if (container.getTag(R.id.tag_amount) != null)
            mAmountEdit.setText((String) container.getTag(R.id.tag_amount));

        if (container.getTag(R.id.tag_bitcoin_uri) != null) {
            final String uri = ((Uri) container.getTag(R.id.tag_bitcoin_uri)).toString();
            try {
                if (!service.isElements())
                    processBitcoinURI(new BitcoinURI(uri), null, null);
                else {
                    final Pair<String, Coin> res = ConfidentialAddress.parseBitcoinURI(service.getNetworkParameters(), uri);
                    if (res.first != null)
                        processBitcoinURI(null, res.first, res.second);
                }
            } catch (final BitcoinURIParseException e) {
                // bitcoinj doesn't understand the address, if its valid (e.g. bech32), use it
                int errId = R.string.err_send_invalid_bitcoin_uri;
                if (uri.toLowerCase(Locale.US).startsWith("bitcoin:")) {
                    final String inner = uri.substring(8);
                    if (service.isValidAddress(inner, service.getNetwork())) {
                        processBitcoinURIDetails(inner, null, null);
                        errId = 0;
                    }
                }
                if (errId != 0)
                    gaActivity.toast(errId);
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
                if (TextUtils.isEmpty(recipient)) {
                    gaActivity.toast(R.string.err_send_need_recipient);
                    return;
                }
                onSendButtonClicked(recipient);
            }
        });

        if (service.isElements()) {
            UI.disable(mMaxButton); // FIXME: Sweeping not available in elements
            UI.hide(mMaxButton, mMaxLabel, mFeeTargetCombo);
        } else {
            mMaxButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(final CompoundButton v, final boolean isChecked) {
                    UI.disableIf(isChecked, mAmountEdit, mAmountFiatEdit);
                    mAmountEdit.setText(isChecked ? R.string.all : R.string.empty);
                }
            });
        }

        mScanIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                onScanIconClicked();
            }
        });

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

    @Override
    protected void onBalanceUpdated() {
        final GaService service = getGAService();
        final FontAwesomeTextView sendSubAccountBalanceUnit = UI.find(mView, R.id.sendSubAccountBalanceUnit);
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView -> " + TAG);
        mSummary = UI.dismiss(getActivity(), mSummary);
        mTwoFactor = UI.dismiss(getActivity(), mTwoFactor);
    }

    @Override
    protected void onSubaccountChanged(final int newSubAccount) {
        mSubaccount = newSubAccount;

        if (!isPageSelected()) {
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
        makeBalanceObserver(mSubaccount);
        getGAService().updateBalance(mSubaccount);
        populateFeeCombo();
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
        if (!isSelected && mPayreqData != null) {
            // When the page is changed with a payment request active, reset
            // all data. Failing to do so can result in the user typing a
            // new address but the payment still going to the previous payment
            // merchant instead
            resetAllFields();
        }
    }

    public void setIsExchanger(final boolean isExchanger) {
        mIsExchanger = isExchanger;
    }

    private void populateFeeCombo() {
        final GaService service = getGAService();
        if (service.isElements())
            return; // FIXME: No custom fees for elements

        final int id = R.array.send_fee_target_choices;
        final ArrayAdapter<CharSequence> a;
        a = ArrayAdapter.createFromResource(getActivity(), id, android.R.layout.simple_spinner_item);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mFeeTargetCombo.setAdapter(a);

        mFeeTargetCombo.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int pos, long id) {
                onNewFeeTargetSelected(pos);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        // Default priority to the users default priority from settings
        final int currentPriority = service.getDefaultTransactionPriority();
        for (int i = 0; i < UI.FEE_TARGET_VALUES.length; ++i) {
            if (currentPriority == UI.FEE_TARGET_VALUES[i].getBlock())
                mFeeTargetCombo.setSelection(i);
        }
    }

    private void onNewFeeTargetSelected(final int index) {
        // Show custom fee entry when custom fee is selected
        final boolean isCustom = UI.FEE_TARGET_VALUES[index].equals(UI.FEE_TARGET.CUSTOM);
        UI.showIf(isCustom, mFeeTargetEdit);
        if (isCustom)
            mFeeTargetEdit.setText(getGAService().cfg().getString("default_feerate", ""));
    }

    private void resetAllFields() {
        mMaxButton.setChecked(false);
        mNoteIcon.setText(R.string.fa_pencil);
        UI.clear(mAmountEdit, mAmountFiatEdit, mRecipientEdit, mNoteText, mFeeTargetEdit);
        UI.enable(mAmountEdit, mAmountFiatEdit,  mRecipientEdit, mSendButton);
        UI.show(mMaxButton, mMaxLabel, mNoteIcon);
        UI.hide(mBip70Progress, mFeeTargetEdit);
        mPayreqData = null;
        mPayreqDetails = null;
    }

    private Coin getSendAmount() {
        try {
            final TextView amountEdit = mIsExchanger ? mAmountBtcWithCommission : mAmountEdit;
            return UI.parseCoinValue(getGAService(), UI.getText(amountEdit));
        } catch (final IllegalArgumentException e) {
            return Coin.ZERO;
        }
    }

    private void processBitcoinURI(final BitcoinURI URI, final String confidentialAddress, Coin amount) {
        if (URI != null && URI.getPaymentRequestUrl() != null) {
            processPaymentRequestUrl(URI.getPaymentRequestUrl());
            return;
        }
        final String address = URI == null ? null : URI.getAddress().toString();
        if (confidentialAddress == null)
            amount = URI.getAmount();
        processBitcoinURIDetails(address, confidentialAddress, amount);
    }

    private void processBitcoinURIDetails(final String address, final String confidentialAddress, Coin amount) {
        final GaService service = getGAService();
        final GaActivity gaActivity = getGaActivity();

        mRecipientEdit.setText(confidentialAddress == null ? address : confidentialAddress);
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

    private void processPaymentRequestUrl(final String requestUrl) {
        final GaService service = getGAService();
        final GaActivity gaActivity = getGaActivity();

        UI.show(mBip70Progress);
        UI.disable(mRecipientEdit, mSendButton);
        UI.hide(mNoteIcon);
        Log.d(TAG, "BIP70 url: " + requestUrl);

        Futures.addCallback(service.fetchPaymentRequest(requestUrl),
                new CB.Toast<PaymentSession>(gaActivity) {
                    @Override
                    public void onSuccess(final PaymentSession session) {
                        onPaymentSessionInitiated(session, requestUrl);
                    }

                    @Override
                    public void onFailure(final Throwable t) {
                        super.onFailure(t);
                        gaActivity.runOnUiThread(new Runnable() {
                            public void run() {
                                UI.hide(mBip70Progress);
                                UI.enable(mRecipientEdit, mSendButton);
                                UI.show(mNoteIcon);
                            }
                        });
                    }
                });
    }

    private void onPaymentSessionInitiated(final PaymentSession session, final String requestUrl) {
        final GaService service = getGAService();
        final GaActivity gaActivity = getGaActivity();

        int msgId = 0;
        if (session == null)
            msgId = R.string.bip70_invalid_payment;
        else if (session.isExpired())
            msgId = R.string.bip70_session_expired;

        if (msgId != 0) {
            Log.e(TAG, "BIP70 session invalid: " + (session == null ? "null" : "expired"));
            UI.toast(gaActivity, msgId, Toast.LENGTH_LONG);
            gaActivity.runOnUiThread(new Runnable() {
                public void run() {
                    resetAllFields();
                }
            });
            return;
        }

        PaymentProtocol.PkiVerificationData pkiData = null;
        try {
            pkiData = session.verifyPki();
        } catch (final Exception e) {
            // Don't show errors that occur during PKI verification to the user!
            // Just treat such payment requests as if they were unsigned. This way
            // new features and PKI roots can be introduced in future without
            // being disruptive.
        }
        if (pkiData == null) {
            gaActivity.runOnUiThread(new Runnable() {
                public void run() {
                    final String host = Uri.parse(requestUrl).getHost();
                    final String text = getString(R.string.bip70_invalid_pki, host);
                    UI.popup(gaActivity, text, R.string.continueText, R.string.cancel)
                            .cancelable(false)
                            .onNegative(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(final MaterialDialog dialog, final DialogAction which) {
                                    resetAllFields();
                                }
                            }).build().show();
                }
            });
        }

        mPayreqData = session.getPaymentRequest();
        mPayreqDetails = null;
        try {
            mPayreqDetails = Protos.PaymentDetails.parseFrom(mPayreqData.getSerializedPaymentDetails());
        } catch (final InvalidProtocolBufferException e) {
            e.printStackTrace();
        }

        final String recipient;
        if (mPayreqDetails == null)
            recipient = Uri.parse(requestUrl).getHost();
        else {
            if (pkiData != null)
                recipient = pkiData.displayName;
            else
                recipient = Uri.parse(mPayreqDetails.getPaymentUrl()).getHost();
        }

        long total = 0;
        for (final Protos.Output output : mPayreqDetails.getOutputsList())
            total += output.getAmount();
        final long totalAmount = total;

        gaActivity.runOnUiThread(new Runnable() {
            public void run() {
                mRecipientEdit.setText(recipient);
                UI.enable(mSendButton);

                String amount = "";
                if (totalAmount > 0)
                    amount = UI.setCoinText(service, null, null, Coin.valueOf(totalAmount));

                if (!TextUtils.isEmpty(amount)) {
                    mAmountEdit.setText(amount);
                    mAmountFields.convertBtcToFiat();
                    UI.disable(mAmountEdit, mAmountFiatEdit);
                    UI.hide(mMaxButton, mMaxLabel);
                }
                UI.hide(mBip70Progress);
            }
        });
    }

    private void onScanIconClicked() {
        final GaActivity gaActivity = getGaActivity();
        final String[] perms = { "android.permission.CAMERA" };
        if (Build.VERSION.SDK_INT > 22 &&
                gaActivity.checkSelfPermission(perms[0]) != PackageManager.PERMISSION_GRANTED) {
            final int permsRequestCode = 100;
            gaActivity.requestPermissions(perms, permsRequestCode);
        } else {
            final Intent qrcodeScanner = new Intent(gaActivity, ScanActivity.class);
            qrcodeScanner.putExtra("sendAmount", UI.getText(mAmountEdit));
            int requestCode = TabbedMainActivity.REQUEST_SEND_QR_SCAN;
            if (mIsExchanger)
                requestCode = TabbedMainActivity.REQUEST_SEND_QR_SCAN_EXCHANGER;
            gaActivity.startActivityForResult(qrcodeScanner, requestCode);
        }
    }

    private void onSendButtonClicked(final String editRecipient) {
        final GaService service = getGAService();
        final GaActivity gaActivity = getGaActivity();

        final JSONMap privateData = new JSONMap();
        if (mIsExchanger)
            privateData.mData.put("memo", Exchanger.TAG_EXCHANGER_TX_MEMO);
        else {
            final String memo = UI.getText(mNoteText);
            if (!TextUtils.isEmpty(memo))
                privateData.mData.put("memo", memo);
        }

        if (mSubaccount != 0)
            privateData.mData.put("subaccount", mSubaccount);


        final Coin amount;
        final String recipient;

        if (mPayreqData != null) {
            final List<Protos.Output> outputs = mPayreqDetails.getOutputsList();
            if (outputs.size() != 1) {
                // gaActivity.toast("Only payment requests with 1 output are supported", mSendButton);
                // TODO manage outputs > 1
                Log.e(TAG, "Only bip70 payment requests with 1 output are supported");
                return;
            }
            final Protos.Output output = mPayreqDetails.getOutputs(0);
            amount = Coin.valueOf(output.getAmount());
            recipient =  new Script(output.getScript().toByteArray()).getToAddress(service.getNetworkParameters()).toString();
            final String bip70memo = mPayreqDetails.getMemo();
            if (!TextUtils.isEmpty(bip70memo))
                privateData.mData.put("memo", bip70memo);
            privateData.mData.put("social_destination", ImmutableMap.of("name", editRecipient));
            privateData.mData.put("social_destination_type", 110); // 110 = PAYMENTREQUEST
        } else {
            recipient = editRecipient;
            amount = getSendAmount();
        }

        final boolean sendAll = mMaxButton.isChecked();
        final boolean validAddress = GaService.isValidAddress(recipient, service.getNetwork());
        final boolean validAmount = sendAll || amount.isGreaterThan(Coin.ZERO);

        int messageId = 0;
        if (!validAddress && !validAmount)
            messageId = R.string.invalidAmountAndAddress;
        else if (!validAddress)
            messageId = R.string.invalidAddress;
        else if (!validAmount)
            messageId = R.string.invalidAmount;
        else if (!sendAll && amount.isLessThan(service.getDustThreshold()))
            messageId = R.string.invalidAmountTooSmall;

        if (messageId != 0) {
            gaActivity.toast(messageId);
            return;
        }

        UI.disable(mSendButton);
        final int numConfs;
        if (service.isMainnet())
            numConfs = 1; // Require 1 conf before spending on mainnet
        else
            numConfs = 0; // Allow 0 conf for networks with no real-world value

        // For 2of2 accounts we first try to spend older coins to avoid
        // having to re-deposit them. If that fails (and always for 2of3
        // accounts) we try to use the minimum number of utxos instead.
        final boolean is2Of3 = service.findSubaccountByType(mSubaccount, "2of3") != null;
        final boolean minimizeInputs = is2Of3;
        final boolean filterAsset = true;

        final UI.FEE_TARGET feeTarget = UI.FEE_TARGET_VALUES[mFeeTargetCombo.getSelectedItemPosition()];
        final Coin feeRate;
        try {
            final String userRate = UI.getText(mFeeTargetEdit);
            if (feeTarget.equals(UI.FEE_TARGET.CUSTOM)) {
                final Object rbf_optin = service.getUserConfig("replace_by_fee");
                if (rbf_optin == null || !((Boolean) rbf_optin)) {
                    gaActivity.toast(R.string.custom_requires_rbf, mSendButton);
                    return;
                }
            }

            if (feeTarget.equals(UI.FEE_TARGET.CUSTOM) &&
                (TextUtils.isEmpty(userRate) || !service.isValidFeeRate(userRate))) {
                // Change invalid feerates to the minimum
                feeRate = service.getMinFeeRate();
                gaActivity.toast(getString(R.string.feerate_changed, feeRate.longValue()));
            } else
                feeRate = getFeeRate(feeTarget);
        } catch (final GAException e) {
            return;
        }

        CB.after(service.getAllUnspentOutputs(numConfs, mSubaccount, filterAsset),
                 new CB.Toast<List<JSONMap>>(gaActivity, mSendButton) {
            @Override
            public void onSuccess(final List<JSONMap> utxos) {
                int ret = R.string.insufficientFundsText;
                if (!utxos.isEmpty()) {
                    GATx.sortUtxos(utxos, minimizeInputs);
                    ret = createRawTransaction(utxos, recipient, amount, privateData, sendAll, feeRate);
                    if (ret == R.string.insufficientFundsText && !minimizeInputs && utxos.size() > 1) {
                        // Not enough money using nlocktime outputs first:
                        // Try again using the largest values first
                        GATx.sortUtxos(utxos, true);
                        ret = createRawTransaction(utxos, recipient, amount, privateData, sendAll, feeRate);
                    }
                }
                if (ret != 0)
                    gaActivity.toast(ret, mSendButton);
            }
        });
    }

    Coin getFeeRate(final UI.FEE_TARGET feeTarget) throws GAException {
        GaService service=getGAService();
        if (!service.isElements() && feeTarget.equals(UI.FEE_TARGET.CUSTOM)) {
            // FIXME: Custom fees for elements
            final Double feeRate = Double.valueOf(UI.getText(mFeeTargetEdit));
            return Coin.valueOf(feeRate.longValue());
        }

        // 1 is not possible yet as we always get 2 as the fastest estimate,
        // but try it anyway in case that improves in the future.
        final int forBlock;
        if (service.isElements())
            forBlock = 6; // FIXME: feeTarget for elements
        else
            forBlock = feeTarget.getBlock();
        return GATx.getFeeEstimate(getGAService(), forBlock);
    }

    private int createRawTransaction(final List<JSONMap> utxos, final String recipient,
                                     final Coin amount, final JSONMap privateData,
                                     final boolean sendAll, final Coin feeRate) {
        final GaService service = getGAService();

        if (service.isElements())
            return createRawElementsTransaction(utxos, recipient, amount, privateData, sendAll, feeRate);

        final GaActivity gaActivity = getGaActivity();

        final List<JSONMap> usedUtxos = new ArrayList<>();

        final Transaction tx = new Transaction(service.getNetworkParameters());

        if (!GATx.addTxOutput(service, tx, amount, recipient))
            return R.string.invalidAddress;

        Coin total = Coin.ZERO;
        Coin fee;
        boolean randomizedChange = false;
        GATx.ChangeOutput changeOutput = null;

        // First add inputs until we cover the amount to send
        while ((sendAll || total.isLessThan(amount)) && !utxos.isEmpty())
            total = total.add(GATx.addUtxo(service, tx, utxos, usedUtxos));

        // Then add inputs until we cover amount + fee/change
        while (true) {
            fee = GATx.getTxFee(service, tx, feeRate);

            final Coin minChange = changeOutput == null ? Coin.ZERO : service.getDustThreshold();
            final int cmp = sendAll ? 0 : total.compareTo(amount.add(fee).add(minChange));
            if (cmp < 0) {
                // Need more inputs to cover amount + fee/change
                if (utxos.isEmpty())
                    return R.string.insufficientFundsText; // None left, fail

                total = total.add(GATx.addUtxo(service, tx, utxos, usedUtxos));
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
            changeOutput.mOutput.setValue(total.subtract(amount).subtract(fee));
            randomizedChange = GATx.randomizeChangeOutput(tx);
        }

        final Coin actualAmount;
        if (!sendAll)
            actualAmount = amount;
        else {
            actualAmount = total.subtract(fee);
            if (!actualAmount.isGreaterThan(Coin.ZERO))
                return R.string.insufficientFundsText;
            tx.getOutput(randomizedChange ? 1 : 0).setValue(actualAmount);
        }

        tx.setLockTime(service.getCurrentBlock()); // Prevent fee sniping

        final PreparedTransaction ptx;
        ptx = GATx.signTransaction(service, tx, usedUtxos, mSubaccount, changeOutput);

        final int changeIndex = changeOutput == null ? -1 : (randomizedChange ? 0 : 1);
        final JSONMap underLimits = GATx.makeLimitsData(actualAmount.add(fee), fee, changeIndex);

        final boolean skipChoice = service.isUnderLimit(underLimits.getCoin("amount"));
        final Coin sendFee = fee;
        gaActivity.runOnUiThread(new Runnable() {
            public void run() {
                mSendButton.setEnabled(true);
                mTwoFactor = UI.popupTwoFactorChoice(gaActivity, service, skipChoice,
                                                     new CB.Runnable1T<String>() {
                    public void run(String method) {
                        if (skipChoice && service.hasAnyTwoFactor())
                            method = "limit";
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

    private static void arraycpy(final byte[] dest, final int i, final byte[] src) {
        System.arraycopy(src, 0, dest, src.length * i, src.length);
    }

    private int createRawElementsTransaction(final List<JSONMap> utxos, final String recipient,
                                             final Coin amount, final JSONMap privateData,
                                             final boolean sendAll, final Coin feeRate) {
        // FIXME: sendAll
        final GaService service = getGAService();
        final GaActivity gaActivity = getGaActivity();

        final List<JSONMap> usedUtxos = new ArrayList<>();

        final ElementsTransaction tx = new ElementsTransaction(service.getNetworkParameters());

        final ElementsTransactionOutput feeOutput = new ElementsTransactionOutput(service.getNetworkParameters(), tx, Coin.ZERO);

        feeOutput.setUnblindedAssetTagFromAssetId(service.mAssetId);
        feeOutput.setValue(Coin.valueOf(1));  // updated below, necessary for serialization for fee calculation
        tx.addOutput(feeOutput);
        TransactionOutput changeOutput = null;

        tx.addOutput(service.mAssetId, amount, ConfidentialAddress.fromBase58(service.getNetworkParameters(), recipient));

        Coin total = Coin.ZERO;
        Coin fee;

        final List<Long> inValues = new ArrayList<>();
        final List<byte[]> inAssetIds = new ArrayList<>();
        final List<byte[]> inAbfs = new ArrayList<>();
        final List<byte[]> inVbfs = new ArrayList<>();

        // First add inputs until we cover the amount to send
        while (total.isLessThan(amount) && !utxos.isEmpty())
            total = total.add(GATx.addUtxo(service, tx, utxos, usedUtxos, inValues, inAssetIds, inAbfs, inVbfs));

        // Then add inputs until we cover amount + fee/change
        while (true) {
            fee = GATx.getTxFee(service, tx, feeRate);

            final Coin minChange = changeOutput == null ? Coin.ZERO : service.getDustThreshold();
            final int cmp = total.compareTo(amount.add(fee).add(minChange));
            if (cmp < 0) {
                // Need more inputs to cover amount + fee/change
                if (utxos.isEmpty())
                    return R.string.insufficientFundsText; // None left, fail
                total = total.add(GATx.addUtxo(service, tx, utxos, usedUtxos, inValues, inAssetIds, inAbfs, inVbfs));
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
                            service.getNetworkParameters(), Wally.hash160(script),
                            service.getBlindingPubKey(mSubaccount, addr.getInt("pointer"))
                    )
            );
        }

        if (changeOutput != null) {
            // Set the value of the change output
            ((ElementsTransactionOutput)changeOutput).setUnblindedValue(total.subtract(amount).subtract(fee).getValue());
            // TODO: randomize change
            // GATx.randomizeChangeOutput(tx);
        }

        feeOutput.setValue(fee);

        // FIXME: tx.setLockTime(latestBlock); // Prevent fee sniping

        // Fetch previous outputs
        final List<Output> prevOuts = GATx.createPrevouts(service, usedUtxos);

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
                    out.getCommitment(), null, out.getAssetTag(), 1
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
                changeOutput, mSubaccount, tx, service.findSubaccountByType(mSubaccount, "2of3")
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
            final JSONMap utxo = usedUtxos.get(i);
            final int scriptType = utxo.getInt("script_type");
            final byte[] outscript = GATx.createOutScript(service, utxo);
            final List<byte[]> userSigs = ImmutableList.of(new byte[]{0}, sig);
            final byte[] inscript = GATx.createInScript(userSigs, outscript, scriptType);

            tx.getInput(i).setScriptSig(new Script(inscript));
            if (scriptType == GATx.P2SH_P2WSH_FORTIFIED_OUT) {
                final TransactionWitness witness = new TransactionWitness(1);
                witness.setPush(0, sig);
                tx.setWitness(i, witness);
            }
        }

        final int changeIndex = changeOutput == null ? -1 : 2;
        final JSONMap underLimits = GATx.makeLimitsData(amount.add(fee), fee, changeIndex);
        underLimits.putBytes("asset_id", service.mAssetId); // FIXME: Others
        underLimits.mData.put("ephemeral_privkeys", ephemeralKeys);
        underLimits.mData.put("blinding_pubkeys", blindingKeys);
        final boolean skipChoice = service.isUnderLimit(underLimits.getCoin("amount"));
        final Coin sendFee = fee;

        gaActivity.runOnUiThread(new Runnable() {
            public void run() {
                mSendButton.setEnabled(true);
                mTwoFactor = UI.popupTwoFactorChoice(gaActivity, service, skipChoice,
                        new CB.Runnable1T<String>() {
                            public void run(String method) {
                                if (skipChoice && service.hasAnyTwoFactor())
                                    method = "limit";
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

    private void onTransactionValidated(final PreparedTransaction ptx,
                                        final Transaction signedRawTx,
                                        final String recipient, final Coin amount,
                                        final String method, final Coin fee,
                                        final JSONMap privateData, final JSONMap underLimits) {
        Log.i(TAG, "onTransactionValidated( params " + method + ' ' + fee + ' ' + amount + ' ' + recipient + ')');
        final GaService service = getGAService();
        final GaActivity gaActivity = getGaActivity();

        final Map<String, Object> twoFacData;

        if (method == null)
            twoFacData = null;
        else if (method.equals("limit")) {
            twoFacData = new HashMap<>();
            twoFacData.put("try_under_limits_spend", underLimits.mData);
        } else {
            twoFacData = new HashMap<>();
            twoFacData.put("method", method);
            if (!method.equals("gauth")) {
                if (underLimits != null)
                    for (final String key : underLimits.mData.keySet())
                        twoFacData.put("send_raw_tx_" + key, underLimits.get(key));
                if (service.isElements()) {
                    underLimits.mData.remove("ephemeral_privkeys");
                    underLimits.mData.remove("blinding_pubkeys");
                }
                final Map<String, Object> twoFactorData;
                twoFactorData = underLimits == null ? null : underLimits.mData;
                service.requestTwoFacCode(method, ptx == null ? "send_raw_tx" : "send_tx", twoFactorData);
            }
        }

        final View v = gaActivity.getLayoutInflater().inflate(R.layout.dialog_new_transaction, null, false);
        final Button showFiatBtcButton = UI.find(v, R.id.newTxShowFiatBtcButton);
        final TextView recipientText = UI.find(v, R.id.newTxRecipientText);
        final EditText newTx2FACodeText = UI.find(v, R.id.newTx2FACodeText);
        final String fiatAmount = service.coinToFiat(amount);
        final String fiatFee = service.coinToFiat(fee);
        final String fiatCurrency = service.getFiatCurrency();

        mSummaryInBtc[0] = true;
        UI.setCoinText(service, v, R.id.newTxAmountUnitText, R.id.newTxAmountText, amount);
        UI.setCoinText(service, v, R.id.newTxFeeUnit, R.id.newTxFeeText, fee);
        if (!service.isElements()) {
            showFiatBtcButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View btn) {
                    // Toggle display between fiat and BTC
                    if (mSummaryInBtc[0]) {
                        AmountFields.changeFiatIcon((FontAwesomeTextView) UI.find(v, R.id.newTxAmountUnitText), fiatCurrency);
                        AmountFields.changeFiatIcon((FontAwesomeTextView) UI.find(v, R.id.newTxFeeUnit), fiatCurrency);
                        UI.setAmountText((TextView) UI.find(v, R.id.newTxAmountText), fiatAmount);
                        UI.setAmountText((TextView) UI.find(v, R.id.newTxFeeText), fiatFee);
                    } else {
                        UI.setCoinText(service, v, R.id.newTxAmountUnitText, R.id.newTxAmountText, amount);
                        UI.setCoinText(service, v, R.id.newTxFeeUnit, R.id.newTxFeeText, fee);
                    }
                    mSummaryInBtc[0] = !mSummaryInBtc[0];
                    showFiatBtcButton.setText(mSummaryInBtc[0] ? R.string.show_fiat : R.string.show_btc);
                }
            });
        }

        if (mPayreqData != null)
            recipientText.setText(recipient);
        else
            recipientText.setText(String.format("%s\n%s\n%s",
                                  recipient.substring(0, 12),
                                  recipient.substring(12, 24),
                                  recipient.substring(24)));

        if (method != null && !method.equals("limit")) {
            final TextView twoFAText = UI.find(v, R.id.newTx2FATypeText);
            UI.show(twoFAText, newTx2FACodeText);
            twoFAText.setText(String.format("2FA %s code", method));
        }

        mTwoFactorAttemptsRemaining = 3;
        mSummary = UI.popup(gaActivity, R.string.newTxTitle, R.string.send, R.string.cancel)
                .customView(v, true)
                .autoDismiss(false)
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(final MaterialDialog dialog, final DialogAction which) {
                        UI.dismiss(null, SendFragment.this.mSummary);
                    }
                })
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(final MaterialDialog dialog, final DialogAction which) {
                        final String code = UI.getText(newTx2FACodeText);
                        sendTransaction(code, twoFacData, method, signedRawTx, privateData);
                    }
                }).build();
        UI.mapEnterToPositive(mSummary, R.id.newTx2FACodeText);
        mSummary.show();
    }

    /**
     * Send transaction to GA server. On BIP70, manage the payment data
     * @param code new TX 2FA code
     * @param twoFacData 2FA data
     * @param twoFacMethod 2FA method
     * @param signedRawTx the client side signed transaction
     * @param privateData private data to send to GA server
     */
    private void sendTransaction(final String code, final Map<String, Object> twoFacData,
                                 final String twoFacMethod, final Transaction signedRawTx,
                                 final JSONMap privateData) {

        final GaActivity gaActivity = getGaActivity();
        final GaService service = getGAService();
        final boolean isRBF = service.isRBFEnabled();

        if (twoFacData != null && !twoFacMethod.equals("limit")) {
            if (code.length() < 6) {
                UI.toast(gaActivity, R.string.malformed_code, mSendButton);
                return;
            }
            twoFacData.put("code", code);
        }

        PaymentSession session = null;
        if (mPayreqData != null) {
            try {
                session = new PaymentSession(mPayreqData, true);
                if (session.isExpired()) {
                    UI.toast(gaActivity, R.string.bip70_session_expired, mSendButton);
                    Log.e(TAG, "BIP70 payment failure: PaymentSession Expired");
                    return;
                }
            } catch (final PaymentProtocolException e) {
                UI.toast(gaActivity, R.string.bip70_payment_failure, mSendButton);
                Log.e(TAG, "BIP70 payment failure (PaymentSession): " + e.toString());
                return;
            }
            // Store the payment request details in private data so the server
            // can process it appropriately
            final String payReqHex = Wally.hex_from_bytes(GaService.serializeProtobuf(mPayreqData));
            privateData.mData.put("payreq", payReqHex);
        }

        final boolean returnTx = mPayreqData != null; // Return tx hex for BIP70
        final ListenableFuture<Pair<String, String>> sendFn;
        sendFn = service.sendRawTransaction(signedRawTx, twoFacData, privateData, returnTx);

        // Send tx to GA server to broadcast to bitcoin network
        final PaymentSession sendSession = session;
        Futures.addCallback(sendFn,
            new CB.Toast<Pair<String, String>>(gaActivity, mSendButton) {
            @Override
            public void onSuccess(final Pair<String, String> txAndHash) {
                if (mPayreqData == null) {
                    onTransactionSent();
                    return;
                }

                final byte[] txbin = Wally.hex_to_bytes(txAndHash.second);
                final Transaction tx = new Transaction(service.getNetworkParameters(), txbin);

                // Generate new address to refund
                Futures.addCallback(service.getNewAddress(service.getCurrentSubAccount(), null),
                    new CB.Toast<String>(gaActivity, mSendButton) {
                    @Override
                    public void onSuccess(final String refundAddr) {
                        try {
                            final Address address = Address.fromBase58(service.getNetworkParameters(), refundAddr);
                            final String memo = privateData.getString("memo");
                            final ListenableFuture<PaymentProtocol.Ack> sendAckFn =
                                    service.sendPayment(sendSession, ImmutableList.of(tx), address, memo);

                            if (sendAckFn == null) {
                                Log.e(TAG, "BIP70 payment failure (sendAckFn)");
                                UI.toast(gaActivity, R.string.bip70_invalid_payment, mSendButton);
                                return;
                            }

                            // send payment to BIP70 server
                            Futures.addCallback(sendAckFn, new CB.Toast<PaymentProtocol.Ack>(gaActivity, mSendButton) {
                                @Override
                                public void onSuccess(final PaymentProtocol.Ack ack) {
                                    if (ack == null) {
                                        Log.e(TAG, "BIP70 payment failure (null PaymentProtocol.Ack)");
                                        if (isRBF && sendSession.getPaymentUrl().contains("bitpay.com")) {
                                            // Ignore failure to ack from bitpay.com for RBF txs.
                                            // Bitpay process RBF txs fine but for political
                                            // reasons refuse to ack them. Ignore their failure
                                            // to ack in this case since the payment is sent.
                                            onTransactionSent();
                                        } else
                                            UI.toast(gaActivity, R.string.bip70_payment_failure, mSendButton);
                                        return;
                                    }
                                    if (TextUtils.isEmpty(ack.getMemo())) {
                                        Log.d(TAG, "BIP70 payment OK (no memo)");
                                        onTransactionSent();
                                    } else {
                                        Log.d(TAG, "BIP70 payment OK: " + ack.getMemo());
                                        // Set the tx memo to the ack memo
                                        CB.after(service.changeMemo(txAndHash.first, ack.getMemo(), "payreq"),
                                            new CB.Toast<Boolean>(gaActivity, mSendButton) {
                                                public void onSuccess(final Boolean unused) {
                                                    onTransactionSent();
                                                }
                                            });
                                    }
                                }

                                @Override
                                public void onFailure(final Throwable t) {
                                    super.onFailure(t);
                                    Log.e(TAG, "BIP70 payment failure (PaymentProtocol.Ack): " + t.getMessage());
                                }
                            }, service.getExecutor());
                        } catch (final PaymentProtocolException | IOException e) {
                            e.printStackTrace();
                            Log.e(TAG, "BIP70 payment failure: " + e.getMessage());
                            UI.toast(gaActivity, e.getMessage(), mSendButton);
                        }
                    }
                }, service.getExecutor());
            }

            @Override
            public void onFailure(final Throwable t) {
                final SendFragment fragment = SendFragment.this;
                final Activity activity = fragment.getActivity();
                if (t instanceof GAException) {
                    final GAException e = (GAException) t;
                    if (e.mUri.equals(GAException.AUTH)) {
                        final int n = --fragment.mTwoFactorAttemptsRemaining;
                        if (n > 0) {
                            final Resources r = fragment.getResources();
                            final String msg = r.getQuantityString(R.plurals.attempts_remaining, n, n);
                            UI.toast(activity, e.mMessage + "\n(" + msg + ')', mSendButton);
                            return; // Allow re-trying
                        }
                    }
                }
                UI.toast(activity, t, mSendButton);
                // Out of 2FA attempts, or another exception; give up
                UI.dismiss(activity, fragment.mSummary);
            }
        }, service.getExecutor());
    }

    private void onTransactionSent() {
        final GaActivity gaActivity = getGaActivity();

        if (gaActivity == null)
            return; // App was paused/deleted while callback ran

        UI.dismiss(gaActivity, mSummary);

        gaActivity.runOnUiThread(new Runnable() {
            public void run() {
                UI.toast(gaActivity, R.string.transactionSubmitted, Toast.LENGTH_LONG);

                if (mIsExchanger)
                    mExchanger.sellBtc(Double.valueOf(UI.getText(mAmountFiatEdit)));

                if (mFromIntentURI) {
                    gaActivity.finish();
                    return;
                }

                UI.clear(mAmountEdit, mRecipientEdit);
                UI.enable(mAmountEdit, mRecipientEdit);
                if (!getGAService().isElements()) {
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
                    gaActivity.toast(R.string.transactionSubmitted);
                    gaActivity.finish();
                }
            }
        });
    }
}
