package com.greenaddress.greenbits.ui;

import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Log;
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
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.greenaddress.greenapi.PreparedTransaction;
import com.greenaddress.greenbits.GaService;

import org.bitcoinj.core.Coin;
import org.bitcoinj.uri.BitcoinURI;
import org.bitcoinj.uri.BitcoinURIParseException;

import java.util.ArrayList;
import java.util.HashMap;
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

    private void showTransactionSummary(final String method, final Coin fee, final Coin amount, final String recipient, final PreparedTransaction ptx) {
        Log.i(TAG, "showTransactionSummary( params " + method + " " + fee + " " + amount + " " + recipient + ")");
        final GaService service = getGAService();
        final GaActivity gaActivity = getGaActivity();

        final View v = gaActivity.getLayoutInflater().inflate(R.layout.dialog_new_transaction, null, false);

        final TextView amountText = UI.find(v, R.id.newTxAmountText);
        final TextView amountUnit = UI.find(v, R.id.newTxAmountUnitText);
        final TextView feeText = UI.find(v, R.id.newTxFeeText);
        final TextView feeUnit = UI.find(v, R.id.newTxFeeUnit);

        final TextView recipientText = UI.find(v, R.id.newTxRecipientText);
        final TextView twoFAText = UI.find(v, R.id.newTx2FATypeText);
        final EditText newTx2FACodeText = UI.find(v, R.id.newTx2FACodeText);

        UI.setCoinText(service, amountUnit, amountText, amount);
        UI.setCoinText(service, feeUnit, feeText, fee);

        if (mPayreqData != null)
            recipientText.setText(recipient);
        else
            recipientText.setText(String.format("%s\n%s\n%s",
                    recipient.substring(0, 12),
                    recipient.substring(12, 24),
                    recipient.substring(24)));

        UI.showIf(method != null, twoFAText, newTx2FACodeText);

        final Map<String, String> twoFacData;

        if (method == null)
            twoFacData = null;
        else {
            twoFacData = new HashMap<>();
            twoFacData.put("method", method);
            twoFAText.setText(String.format("2FA %s code", method));
            if (!method.equals("gauth"))
                service.requestTwoFacCode(method, "send_tx", null);
        }

        mSummary = UI.popup(gaActivity, R.string.newTxTitle, R.string.send, R.string.cancel)
                .customView(v, true)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(final MaterialDialog dialog, final DialogAction which) {
                        if (twoFacData != null)
                            twoFacData.put("code", UI.getText(newTx2FACodeText));

                        final ListenableFuture<String> sendFuture = service.signAndSendTransaction(ptx, twoFacData);
                        Futures.addCallback(sendFuture, new CB.Toast<String>(gaActivity) {
                            @Override
                            public void onSuccess(final String result) {
                                gaActivity.runOnUiThread(new Runnable() {
                                    public void run() {
                                        UI.toast(gaActivity, R.string.transactionCompleted, Toast.LENGTH_LONG);

                                        if (mFromIntentURI) {
                                            gaActivity.finish();
                                            return;
                                        }

                                        mAmountEdit.setText("");
                                        mRecipientEdit.setText("");
                                        UI.enable(mAmountEdit, mRecipientEdit);
                                        mMaxButton.setChecked(false);
                                        UI.show(mMaxButton, mMaxLabel);

                                        mNoteIcon.setText(R.string.fa_pencil);
                                        mNoteText.setText("");
                                        mNoteText.setVisibility(View.INVISIBLE);

                                        final ViewPager viewPager = UI.find(gaActivity, R.id.container);
                                        viewPager.setCurrentItem(1);
                                    }
                                });
                            }
                        }, service.getExecutor());
                    }
                }).build();

        mSummary.show();
    }

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

                Coin nonFinalAmount;
                try {
                    nonFinalAmount = UI.parseCoinValue(service, UI.getText(mAmountEdit));
                } catch (final IllegalArgumentException e) {
                    nonFinalAmount = Coin.ZERO;
                }
                final Coin amount = nonFinalAmount;

                String message = null;

                final Map<String, Object> privateData = new HashMap<>();
                final String memo = UI.getText(mNoteText);
                if (!memo.isEmpty())
                    privateData.put("memo", memo);

                if (mSubaccount != 0)
                    privateData.put("subaccount", mSubaccount);

                if (mInstantConfirmationCheckbox.isChecked())
                    privateData.put("instant", true);

                final ListenableFuture<PreparedTransaction> ptxFn;
                if (mPayreqData == null) {
                    final boolean validAddress = GaService.isValidAddress(recipient);
                    final boolean validAmount = !(amount.compareTo(Coin.ZERO) <= 0) || mMaxButton.isChecked();

                    if (!validAddress && !validAmount) {
                        message = gaActivity.getString(R.string.invalidAmountAndAddress);
                    } else if (!validAddress) {
                        message = gaActivity.getString(R.string.invalidAddress);
                    } else if (!validAmount) {
                        message = gaActivity.getString(R.string.invalidAmount);
                    }
                    if (message == null) {
                        if (mMaxButton.isChecked()) {
                            // prepareSweepAll again in case some fee estimation
                            // has changed while user was considering the amount,
                            // and to make sure the same algorithm of fee calcualation
                            // is used - 'recipient' fee as opossed to 'sender' fee.
                            // This means the real amount can be different from
                            // the one shown in the edit box, but this way is
                            // safer. If we attempted to send the calculated amount
                            // instead with 'sender' fee algorithm, the transaction
                            // could fail due to differences in calculations.
                            ptxFn = service.prepareSweepAll(mSubaccount, recipient, privateData);
                        } else {
                            ptxFn = service.prepareTx(amount, recipient, privateData);
                        }
                    } else {
                        ptxFn = null;
                    }
                } else {
                    ptxFn = service.preparePayreq(amount, mPayreqData, privateData);
                }

                if (ptxFn != null) {
                    mSendButton.setEnabled(false);
                    CB.after(ptxFn,
                            new CB.Toast<PreparedTransaction>(gaActivity, mSendButton) {
                                @Override
                                public void onSuccess(final PreparedTransaction ptx) {
                                    // final Coin fee = Coin.parseCoin("0.0001");        //FIXME: pass real fee
                                    final Coin verifyAmount = mMaxButton.isChecked() ? null : amount;
                                    CB.after(service.validateTx(ptx, recipient, verifyAmount),
                                            new CB.Toast<Coin>(gaActivity, mSendButton) {
                                                @Override
                                                public void onSuccess(final Coin fee) {
                                                    final Map<?, ?> twoFacConfig = service.getTwoFactorConfig();
                                                    // can be non-UI because validation talks to USB if hw wallet is used
                                                    gaActivity.runOnUiThread(new Runnable() {
                                                        public void run() {
                                                            mSendButton.setEnabled(true);
                                                            final Coin dialogAmount, dialogFee;
                                                            if (mMaxButton.isChecked()) {
                                                                // 'fee' in reality is the sent amount in case passed amount=null
                                                                dialogAmount = fee;
                                                                dialogFee = service.getCoinBalance(mSubaccount).subtract(fee);
                                                            } else {
                                                                dialogAmount = amount;
                                                                dialogFee = fee;
                                                            }
                                                            final boolean skipChoice = !ptx.mRequiresTwoFactor ||
                                                                                        twoFacConfig == null || !((Boolean) twoFacConfig.get("any"));
                                                            mTwoFactor = UI.popupTwoFactorChoice(gaActivity, service, skipChoice,
                                                                                                         new CB.Runnable1T<String>() {
                                                                @Override
                                                                public void run(final String method) {
                                                                    showTransactionSummary(method, dialogFee, dialogAmount, recipient, ptx);
                                                                }
                                                            });
                                                            if (mTwoFactor != null)
                                                                mTwoFactor.show();
                                                        }
                                                    });
                                                }
                                            });
                                }
                            });
                }

                if (message != null)
                    gaActivity.toast(message);
            }
        });

        mMaxButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(final CompoundButton v, final boolean isChecked) {
                if (isChecked) {
                    mAmountEdit.setEnabled(false);
                    mAmountFiatEdit.setEnabled(false);
                    mAmountEdit.setText(getString(R.string.send_max_amount));
                } else {
                    mAmountEdit.setText("");
                    mAmountEdit.setEnabled(true);
                    mAmountFiatEdit.setEnabled(true);
                }
            }
        });

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
                                                gaActivity.startActivityForResult(qrcodeScanner, REQUEST_SEND_QR_SCAN);
                                            }
                                        }
                                    }
        );

        mNoteIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                if (mNoteText.getVisibility() == View.VISIBLE) {
                    mNoteIcon.setText(R.string.fa_pencil);
                    mNoteText.setText("");
                    mNoteText.setVisibility(View.INVISIBLE);
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
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause -> " + TAG);
        if (mAmountFields != null)
            mAmountFields.setIsPausing(true);
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart -> " + TAG);
        if (mAmountFields != null)
            mAmountFields.setIsPausing(false);
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
}
