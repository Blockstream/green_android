package com.greenaddress.greenbits.ui;

import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.text.Editable;
import android.text.Html;
import android.text.TextUtils;
import android.text.TextWatcher;
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

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.greenaddress.greenapi.PreparedTransaction;
import com.greenaddress.greenbits.GaService;

import org.bitcoinj.core.Coin;
import org.bitcoinj.uri.BitcoinURI;
import org.bitcoinj.uri.BitcoinURIParseException;
import org.bitcoinj.utils.ExchangeRate;
import org.bitcoinj.utils.Fiat;
import org.bitcoinj.utils.MonetaryFormat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Observer;

import de.schildbach.wallet.ui.ScanActivity;

public class SendFragment extends SubaccountFragment {

    private static final String TAG = SendFragment.class.getSimpleName();
    private static final int REQUEST_SEND_QR_SCAN = 0;
    private View mView;
    private Dialog mSummary;
    private Dialog mTwoFactor;
    private EditText amountEdit;
    private EditText amountFiatEdit;
    private EditText recipientEdit;
    private EditText noteText;
    private CheckBox instantConfirmationCheckbox;
    private TextView noteIcon;
    private Button sendButton;
    private Switch maxButton;
    private TextView scanIcon;
    private Map<?, ?> payreqData = null;
    private boolean fromIntentURI = false;


    private boolean converting = false;
    private MonetaryFormat bitcoinFormat;
    private int curSubaccount;
    private Observer curBalanceObserver;
    private boolean pausing;

    private void showTransactionSummary(final String method, final Coin fee, final Coin amount, final String recipient, final PreparedTransaction ptx) {
        Log.i(TAG, "showTransactionSummary( params " + method + " " + fee + " " + amount + " " + recipient + ")");
        final GaService service = getGAService();
        final GaActivity gaActivity = getGaActivity();

        final View v = gaActivity.getLayoutInflater().inflate(R.layout.dialog_new_transaction, null, false);

        final TextView amountText = UI.find(v, R.id.newTxAmountText);
        final TextView amountScale = UI.find(v, R.id.newTxAmountScaleText);
        final TextView amountUnit = UI.find(v, R.id.newTxAmountUnitText);
        final TextView feeText = UI.find(v, R.id.newTxFeeText);
        final TextView feeScale = UI.find(v, R.id.newTxFeeScale);
        final TextView feeUnit = UI.find(v, R.id.newTxFeeUnit);

        final TextView recipientText = UI.find(v, R.id.newTxRecipientText);
        final TextView twoFAText = UI.find(v, R.id.newTx2FATypeText);
        final EditText newTx2FACodeText = UI.find(v, R.id.newTx2FACodeText);
        final String prefix = CurrencyMapper.mapBtcFormatToPrefix(bitcoinFormat);

        amountScale.setText(Html.fromHtml(prefix));
        feeScale.setText(Html.fromHtml(prefix));
        if (TextUtils.isEmpty(prefix)) {
            amountUnit.setText("bits ");
            feeUnit.setText("bits ");
        } else {
            amountUnit.setText(Html.fromHtml("&#xf15a; "));
            feeUnit.setText(Html.fromHtml("&#xf15a; "));
        }
        amountText.setText(bitcoinFormat.noCode().format(amount));
        feeText.setText(bitcoinFormat.noCode().format(fee));

        if (payreqData == null) {
            recipientText.setText(String.format("%s\n%s\n%s",
                    recipient.substring(0, 12),
                    recipient.substring(12, 24),
                    recipient.substring(24)));
        } else {
            recipientText.setText(recipient);
        }

        final Map<String, String> twoFacData;

        if (method == null) {
            UI.hide(twoFAText, newTx2FACodeText);
            twoFacData = null;
        } else {
            twoFAText.setText(String.format("2FA %s code", method));
            twoFacData = new HashMap<>();
            twoFacData.put("method", method);
            if (!method.equals("gauth")) {
                service.requestTwoFacCode(method, "send_tx", null);
            }
        }

        mSummary = UI.popup(gaActivity, R.string.newTxTitle, R.string.send, R.string.cancel)
                .customView(v, true)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(final MaterialDialog dialog, final DialogAction which) {
                        if (twoFacData != null) {
                            twoFacData.put("code", UI.getText(newTx2FACodeText));
                        }
                        final ListenableFuture<String> sendFuture = service.signAndSendTransaction(ptx, twoFacData);
                        Futures.addCallback(sendFuture, new CB.Toast<String>(gaActivity) {
                            @Override
                            public void onSuccess(final String result) {
                                gaActivity.runOnUiThread(new Runnable() {
                                    public void run() {
                                        if (fromIntentURI) {
                                            gaActivity.finish();
                                            return;
                                        }

                                        // FIXME: Add notification with "Transaction sent"?
                                        amountEdit.setText("");
                                        recipientEdit.setText("");
                                        maxButton.setChecked(false);

                                        noteIcon.setText(Html.fromHtml("&#xf040"));
                                        noteText.setText("");
                                        noteText.setVisibility(View.INVISIBLE);

                                        final ViewPager mViewPager = UI.find(gaActivity, R.id.container);
                                        mViewPager.setCurrentItem(1);
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
            recipientEdit.setEnabled(false);
            sendButton.setEnabled(false);
            UI.hide(noteIcon);
            Futures.addCallback(service.processBip70URL(URI.getPaymentRequestUrl()),
                    new CB.Toast<Map<?, ?>>(gaActivity) {
                        @Override
                        public void onSuccess(final Map<?, ?> result) {
                            payreqData = result;

                            final String name;
                            if (result.get("merchant_cn") != null) {
                                name = (String) result.get("merchant_cn");
                            } else {
                                name = (String) result.get("request_url");
                            }


                            long amount = 0;
                            for (final Map<?, ?> out : (ArrayList<Map>) result.get("outputs")) {
                                amount += ((Number) out.get("amount")).longValue();
                            }
                            final CharSequence amountStr;
                            if (amount > 0) {
                                amountStr = bitcoinFormat.noCode().format(Coin.valueOf(amount));
                            } else {
                                amountStr = "";
                            }
                            gaActivity.runOnUiThread(new Runnable() {
                                public void run() {
                                    recipientEdit.setText(name);
                                    sendButton.setEnabled(true);
                                    if (!amountStr.toString().isEmpty()) {
                                        amountEdit.setText(amountStr);
                                        convertBtcToFiat();
                                        amountEdit.setEnabled(false);
                                        amountFiatEdit.setEnabled(false);
                                    }
                                    UI.hide(bip70Progress);
                                }
                            });
                        }
                    });
        } else {
            recipientEdit.setText(URI.getAddress().toString());
            if (URI.getAmount() != null) {
                Futures.addCallback(service.getSubaccountBalance(curSubaccount), new CB.Op<Map<?, ?>>() {
                    @Override
                    public void onSuccess(final Map<?, ?> result) {
                        gaActivity.runOnUiThread(new Runnable() {
                                public void run() {
                                    final Float fiatRate = Float.valueOf((String) result.get("fiat_exchange"));
                                    amountEdit.setText(bitcoinFormat.noCode().format(URI.getAmount()));
                                    convertBtcToFiat(fiatRate);
                                    amountEdit.setEnabled(false);
                                    amountFiatEdit.setEnabled(false);
                                }
                        });
                    }
                }, service.getExecutor());
            }
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {

        registerReceiver();

        final GaService service = getGAService();
        final GaActivity gaActivity = getGaActivity();

        if (savedInstanceState != null)
            pausing = savedInstanceState.getBoolean("pausing");

        mView = inflater.inflate(R.layout.fragment_send, container, false);

        curSubaccount = service.getCurrentSubAccount();

        sendButton = UI.find(mView, R.id.sendSendButton);
        maxButton = UI.find(mView, R.id.sendMaxButton);
        noteText = UI.find(mView, R.id.sendToNoteText);
        noteIcon = UI.find(mView, R.id.sendToNoteIcon);
        instantConfirmationCheckbox = UI.find(mView, R.id.instantConfirmationCheckBox);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            // pre-Material Design the label was already a part of the switch
            UI.hide((View) UI.find(mView, R.id.sendMaxLabel));
        }

        amountEdit = UI.find(mView, R.id.sendAmountEditText);
        amountFiatEdit = UI.find(mView, R.id.sendAmountFiatEditText);
        recipientEdit = UI.find(mView, R.id.sendToEditText);
        scanIcon = UI.find(mView, R.id.sendScanIcon);

        final String btcUnit = (String) service.getUserConfig("unit");
        final TextView bitcoinScale = UI.find(mView, R.id.sendBitcoinScaleText);
        final TextView bitcoinUnitText = UI.find(mView, R.id.sendBitcoinUnitText);
        bitcoinFormat = CurrencyMapper.mapBtcUnitToFormat(btcUnit);
        bitcoinScale.setText(Html.fromHtml(CurrencyMapper.mapBtcUnitToPrefix(btcUnit)));
        if (btcUnit == null || btcUnit.equals("bits")) {
            bitcoinUnitText.setText("bits ");
        } else {
            bitcoinUnitText.setText(Html.fromHtml("&#xf15a; "));
        }

        if (container.getTag(R.id.tag_bitcoin_uri) != null) {
            final Uri uri = (Uri) container.getTag(R.id.tag_bitcoin_uri);
            BitcoinURI bitcoinUri = null;
            try {
                bitcoinUri = new BitcoinURI(uri.toString());
            } catch (final BitcoinURIParseException e) {
                gaActivity.toast(R.string.err_send_invalid_bitcoin_uri);
            }
            if (bitcoinUri != null) {
                processBitcoinURI(bitcoinUri);
            }
            fromIntentURI = true;
            container.setTag(R.id.tag_bitcoin_uri, null);
        }

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                // FIXME: Instead of checking the state here, enable/disable sendButton when state changes
                if (!service.isLoggedIn()) {
                    gaActivity.toast(R.string.err_send_not_connected_will_resume);
                    return;
                }
                final String recipient = UI.getText(recipientEdit);
                final Coin amount;
                Coin nonFinalAmount;
                try {
                    nonFinalAmount = bitcoinFormat.parse(UI.getText(amountEdit));
                } catch (final IllegalArgumentException e) {
                    nonFinalAmount = Coin.ZERO;
                }
                amount = nonFinalAmount;

                if (recipient.isEmpty()) {
                    gaActivity.toast(R.string.err_send_need_recipient);
                    return;
                }

                final boolean validAddress = GaService.isValidAddress(recipient);

                final boolean validAmount =
                        !(amount.compareTo(Coin.ZERO) <= 0) ||
                        maxButton.isChecked();
                String message = null;

                final Map<String, Object> privateData = new HashMap<>();

                final String memo = UI.getText(noteText);
                if (!memo.isEmpty())
                    privateData.put("memo", memo);

                if (curSubaccount != 0)
                    privateData.put("subaccount", curSubaccount);

                if (instantConfirmationCheckbox.isChecked())
                    privateData.put("instant", true);

                ListenableFuture<PreparedTransaction> ptxFn;
                if (payreqData == null) {
                    if (!validAddress && !validAmount) {
                        message = gaActivity.getString(R.string.invalidAmountAndAddress);
                    } else if (!validAddress) {
                        message = gaActivity.getString(R.string.invalidAddress);
                    } else if (!validAmount) {
                        message = gaActivity.getString(R.string.invalidAmount);
                    }
                    if (message == null) {
                        if (maxButton.isChecked()) {
                            // prepareSweepAll again in case some fee estimation
                            // has changed while user was considering the amount,
                            // and to make sure the same algorithm of fee calcualation
                            // is used - 'recipient' fee as opossed to 'sender' fee.
                            // This means the real amount can be different from
                            // the one shown in the edit box, but this way is
                            // safer. If we attempted to send the calculated amount
                            // instead with 'sender' fee algorithm, the transaction
                            // could fail due to differences in calculations.
                            ptxFn = service.prepareSweepAll(curSubaccount, recipient, privateData);
                        } else {
                            ptxFn = service.prepareTx(amount, recipient, privateData);
                        }
                    } else {
                        ptxFn = null;
                    }
                } else {
                    ptxFn = service.preparePayreq(amount, payreqData, privateData);
                }

                if (ptxFn != null) {
                    sendButton.setEnabled(false);
                    CB.after(ptxFn,
                            new CB.Toast<PreparedTransaction>(gaActivity, sendButton) {
                                @Override
                                public void onSuccess(final PreparedTransaction ptx) {
                                    // final Coin fee = Coin.parseCoin("0.0001");        //FIXME: pass real fee
                                    final Coin verifyAmount = maxButton.isChecked() ? null : amount;
                                    CB.after(service.validateTx(ptx, recipient, verifyAmount),
                                            new CB.Toast<Coin>(gaActivity, sendButton) {
                                                @Override
                                                public void onSuccess(final Coin fee) {
                                                    final Map<?, ?> twoFacConfig = service.getTwoFactorConfig();
                                                    // can be non-UI because validation talks to USB if hw wallet is used
                                                    gaActivity.runOnUiThread(new Runnable() {
                                                        public void run() {
                                                            sendButton.setEnabled(true);
                                                            final Coin dialogAmount, dialogFee;
                                                            if (maxButton.isChecked()) {
                                                                // 'fee' in reality is the sent amount in case passed amount=null
                                                                dialogAmount = fee;
                                                                dialogFee = service.getCoinBalance(curSubaccount).subtract(fee);
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

        maxButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton v, boolean isChecked) {
                if (isChecked) {
                    amountEdit.setEnabled(false);
                    amountFiatEdit.setEnabled(false);
                    amountEdit.setText(getString(R.string.send_max_amount));
                } else {
                    amountEdit.setText("");
                    amountEdit.setEnabled(true);
                    amountFiatEdit.setEnabled(true);
                }
            }
        });

        curBalanceObserver = makeBalanceObserver();
        service.addBalanceObserver(curSubaccount, curBalanceObserver);

        if (service.getCoinBalance(curSubaccount) != null) {
            updateBalance();
        }

        scanIcon.setOnClickListener(new View.OnClickListener() {
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


        final FontAwesomeTextView fiatView = UI.find(mView, R.id.sendFiatIcon);
        changeFiatIcon(fiatView, service.getFiatCurrency());

        amountFiatEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {

            }

            @Override
            public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
                convertFiatToBtc();
            }

            @Override
            public void afterTextChanged(final Editable s) {

            }
        });

        amountEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {

            }

            @Override
            public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
                convertBtcToFiat();
            }

            @Override
            public void afterTextChanged(final Editable s) {

            }
        });

        noteIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                if (noteText.getVisibility() == View.VISIBLE) {
                    noteIcon.setText(Html.fromHtml("&#xf040"));
                    noteText.setText("");
                    noteText.setVisibility(View.INVISIBLE);
                } else {
                    noteIcon.setText(Html.fromHtml("&#xf00d"));
                    UI.show(noteText);
                    noteText.requestFocus();
                }
            }
        });

        hideInstantIf2of3();

        return mView;
    }

    @Override
    public void onViewStateRestored(final Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        pausing = false;
    }

    private void hideInstantIf2of3() {
        if (getGAService().findSubaccount("2of3", curSubaccount) != null) {
            UI.hide(instantConfirmationCheckbox);
            instantConfirmationCheckbox.setChecked(false);
        } else
            UI.show(instantConfirmationCheckbox);
    }

    @Override
    protected void onBalanceUpdated() {
        updateBalance();
    }

    private void updateBalance() {
        final String btcUnit = (String) getGAService().getUserConfig("unit");
        final TextView sendSubAccountBalance = UI.find(mView, R.id.sendSubAccountBalance);
        final TextView sendSubAccountBalanceUnit = UI.find(mView, R.id.sendSubAccountBalanceUnit);
        final TextView sendSubAccountBitcoinScale = UI.find(mView, R.id.sendSubAccountBitcoinScale);
        sendSubAccountBitcoinScale.setText(Html.fromHtml(CurrencyMapper.mapBtcUnitToPrefix(btcUnit)));
        if (btcUnit == null || btcUnit.equals("bits")) {
            sendSubAccountBalanceUnit.setText("");
            sendSubAccountBitcoinScale.setText("bits ");
        } else {
            sendSubAccountBalanceUnit.setText(Html.fromHtml("&#xf15a; "));
        }
        final MonetaryFormat format = CurrencyMapper.mapBtcUnitToFormat(btcUnit);
        final String btcBalance = format.noCode().format(
                getGAService().getCoinBalance(curSubaccount)).toString();
        UI.setAmountText(sendSubAccountBalance, btcBalance);

        final int nChars = sendSubAccountBalance.getText().length() + sendSubAccountBitcoinScale.getText().length() + sendSubAccountBalanceUnit.getText().length();
        final int size = Math.min(50 - nChars, 34);
        sendSubAccountBalance.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
        sendSubAccountBalanceUnit.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
        sendSubAccountBalanceUnit.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
        if (getGAService().showBalanceInTitle())
            UI.hide(sendSubAccountBalance, sendSubAccountBalanceUnit, sendSubAccountBitcoinScale);
    }

    private void changeFiatIcon(final FontAwesomeTextView fiatIcon, final String currency) {

        final String converted = CurrencyMapper.map(currency);
        if (converted != null) {
            fiatIcon.setText(Html.fromHtml(converted + " "));
            fiatIcon.setAwesomeTypeface();
            fiatIcon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        } else {
            fiatIcon.setText(currency);
            fiatIcon.setDefaultTypeface();
            fiatIcon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        }
    }

    private void convertBtcToFiat() {
        convertBtcToFiat(getGAService().getFiatRate());
    }

    private void convertBtcToFiat(final float exchangeRate) {
        if (converting || pausing) {
            return;
        }
        converting = true;
        final Fiat exchangeFiat = Fiat.valueOf("???", new BigDecimal(exchangeRate).movePointRight(Fiat.SMALLEST_UNIT_EXPONENT)
                .toBigInteger().longValue());

        try {
            final ExchangeRate rate = new ExchangeRate(exchangeFiat);
            final Coin btcValue = bitcoinFormat.parse(UI.getText(amountEdit));
            Fiat fiatValue = rate.coinToFiat(btcValue);
            // strip extra decimals (over 2 places) because that's what the old JS client does
            fiatValue = fiatValue.subtract(fiatValue.divideAndRemainder((long) Math.pow(10, Fiat.SMALLEST_UNIT_EXPONENT - 2))[1]);
            amountFiatEdit.setText(fiatValue.toPlainString());
        } catch (final ArithmeticException | IllegalArgumentException e) {
            if (UI.getText(amountEdit).equals(getString(R.string.send_max_amount))) {
                amountFiatEdit.setText(getString(R.string.send_max_amount));
            } else {
                amountFiatEdit.setText("");
            }
        }
        converting = false;
    }

    private void convertFiatToBtc() {
        if (converting || pausing) {
            return;
        }
        converting = true;
        final float exchangeRate = getGAService().getFiatRate();
        final Fiat exchangeFiat = Fiat.valueOf("???", new BigDecimal(exchangeRate).movePointRight(Fiat.SMALLEST_UNIT_EXPONENT)
                .toBigInteger().longValue());
        final ExchangeRate rate = new ExchangeRate(exchangeFiat);
        try {
            final Fiat fiatValue = Fiat.parseFiat("???", UI.getText(amountFiatEdit));
            amountEdit.setText(bitcoinFormat.noCode().format(rate.fiatToCoin(fiatValue)));
        } catch (final ArithmeticException | IllegalArgumentException e) {
            amountEdit.setText("");
        }
        converting = false;
    }

    @Override
    public void onPause() {
        super.onPause();
        pausing = true;
    }

    @Override
    public void onStart() {
        super.onStart();
        pausing = false;
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("pausing", pausing);
    }

    public void onDestroyView() {
        super.onDestroyView();
        if (mSummary != null)
            mSummary.dismiss();
        if (mTwoFactor != null)
            mTwoFactor.dismiss();
    }

    @Override
    protected void onSubaccountChanged(final int newSubAccount) {
        final GaService service = getGAService();

        service.deleteBalanceObserver(curSubaccount, curBalanceObserver);
        curSubaccount = newSubAccount;
        hideInstantIf2of3();
        final GaActivity gaActivity = getGaActivity();

        curBalanceObserver = makeBalanceObserver();
        service.addBalanceObserver(curSubaccount, curBalanceObserver);
        CB.after(service.getSubaccountBalance(curSubaccount), new CB.Op<Map<?, ?>>() {
            @Override
            public void onSuccess(final Map<?, ?> balance) {
                final Coin coin = Coin.valueOf(Long.valueOf((String) balance.get("satoshi")));
                final String btcUnit = (String) service.getUserConfig("unit");
                final TextView sendSubAccountBalance = UI.find(mView, R.id.sendSubAccountBalance);
                final MonetaryFormat format = CurrencyMapper.mapBtcUnitToFormat(btcUnit);
                final String btcBalance = format.noCode().format(coin).toString();
                gaActivity.runOnUiThread(new Runnable() {
                    public void run() {
                        UI.setAmountText(sendSubAccountBalance, btcBalance);
                    }
                });
            }
        });
    }
}
