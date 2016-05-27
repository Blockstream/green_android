package com.greenaddress.greenbits.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.text.Editable;
import android.text.Html;
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
import android.widget.Switch;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.common.util.concurrent.FutureCallback;
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
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import de.schildbach.wallet.ui.ScanActivity;

public class SendFragment extends SubaccountFragment {

    @NonNull private static final String TAG = SendFragment.class.getSimpleName();
    private static final int REQUEST_SEND_QR_SCAN = 0;
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
    @Nullable
    private Map<?, ?> payreqData = null;
    private boolean fromIntentURI = false;


    private boolean converting = false;
    private MonetaryFormat bitcoinFormat;
    // any better way to do it
    private View rootView;
    private int curSubaccount;
    @Nullable
    private Observer curBalanceObserver;
    private boolean pausing;

    private void showTransactionSummary(@Nullable final String method, final Coin fee, final Coin amount, @NonNull final String recipient, @NonNull final PreparedTransaction prepared) {
        Log.i(TAG, "showTransactionSummary( params " + method + " " + fee + " " + amount + " " + recipient + ")");
        final GaActivity gaActivity = getGaActivity();

        final View inflatedLayout = getActivity().getLayoutInflater().inflate(R.layout.dialog_new_transaction, null, false);

        final TextView amountText = (TextView) inflatedLayout.findViewById(R.id.newTxAmountText);
        final TextView amountScale = (TextView) inflatedLayout.findViewById(R.id.newTxAmountScaleText);
        final TextView amountUnit = (TextView) inflatedLayout.findViewById(R.id.newTxAmountUnitText);
        final TextView feeText = (TextView) inflatedLayout.findViewById(R.id.newTxFeeText);
        final TextView feeScale = (TextView) inflatedLayout.findViewById(R.id.newTxFeeScale);
        final TextView feeUnit = (TextView) inflatedLayout.findViewById(R.id.newTxFeeUnit);

        final TextView recipientText = (TextView) inflatedLayout.findViewById(R.id.newTxRecipientText);
        final TextView twoFAText = (TextView) inflatedLayout.findViewById(R.id.newTx2FATypeText);
        final EditText newTx2FACodeText = (EditText) inflatedLayout.findViewById(R.id.newTx2FACodeText);
        final String prefix = CurrencyMapper.mapBtcFormatToPrefix(bitcoinFormat);

        amountScale.setText(Html.fromHtml(prefix));
        feeScale.setText(Html.fromHtml(prefix));
        if (prefix == null || prefix.isEmpty()) {
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
            twoFAText.setVisibility(View.GONE);
            newTx2FACodeText.setVisibility(View.GONE);
            twoFacData = null;
        } else {
            twoFAText.setText(String.format("2FA %s code", method));
            twoFacData = new HashMap<>();
            twoFacData.put("method", method);
            if (!method.equals("gauth")) {
                getGAService().requestTwoFacCode(method, "send_tx", null);
            }
        }

        mSummary = GaActivity.Popup(getActivity(), getString(R.string.newTxTitle), R.string.send, R.string.cancel)
                .customView(inflatedLayout, true)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(final @NonNull MaterialDialog dialog, final @NonNull DialogAction which) {
                        if (twoFacData != null) {
                            twoFacData.put("code", newTx2FACodeText.getText().toString());
                        }
                        final ListenableFuture<String> sendFuture = getGAService().signAndSendTransaction(prepared, twoFacData);
                        Futures.addCallback(sendFuture, new CB.Toast<String>(gaActivity) {
                            @Override
                            public void onSuccess(@Nullable final String result) {
                                if (fromIntentURI) {
                                    // FIXME If coming back from the Trusted UI, there can be a race condition
                                    if (getActivity() != null) {
                                        getActivity().finish();
                                    }
                                    return;
                                }
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        // FIXME: Add notification with "Transaction sent"?
                                        amountEdit.setText("");
                                        recipientEdit.setText("");
                                        maxButton.setChecked(false);

                                        noteIcon.setText(Html.fromHtml("&#xf040"));
                                        noteText.setText("");
                                        noteText.setVisibility(View.INVISIBLE);

                                        final ViewPager mViewPager = (ViewPager) getActivity().findViewById(R.id.container);
                                        mViewPager.setCurrentItem(1);
                                    }
                                });
                            }
                        }, getGAService().es);
                    }
                }).build();

        mSummary.show();
    }

    private void processBitcoinURI(@NonNull final BitcoinURI URI) {
        final GaService service = getGAService();
        final GaActivity gaActivity = getGaActivity();

        if (URI.getPaymentRequestUrl() != null) {
            rootView.findViewById(R.id.sendBip70ProgressBar).setVisibility(View.VISIBLE);
            recipientEdit.setEnabled(false);
            sendButton.setEnabled(false);
            noteIcon.setVisibility(View.GONE);
            Futures.addCallback(service.processBip70URL(URI.getPaymentRequestUrl()),
                    new CB.Toast<Map<?, ?>>(gaActivity) {
                        @Override
                        public void onSuccess(@Nullable final Map<?, ?> result) {
                            payreqData = result;

                            final String name;
                            if (result.get("merchant_cn") != null) {
                                name = (String) result.get("merchant_cn");
                            } else {
                                name = (String) result.get("request_url");
                            }


                            long amount = 0;
                            for (final Map<?, ?> out : (ArrayList<Map<?, ?>>) result.get("outputs")) {
                                amount += ((Number) out.get("amount")).longValue();
                            }
                            final CharSequence amountStr;
                            if (amount > 0) {
                                amountStr = bitcoinFormat.noCode().format(Coin.valueOf(amount));
                            } else {
                                amountStr = "";
                            }
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    recipientEdit.setText(name);
                                    sendButton.setEnabled(true);
                                    if (!amountStr.toString().isEmpty()) {
                                        amountEdit.setText(amountStr);
                                        convertBtcToFiat();
                                        amountEdit.setEnabled(false);
                                        amountFiatEdit.setEnabled(false);
                                    }
                                    rootView.findViewById(R.id.sendBip70ProgressBar).setVisibility(View.GONE);
                                }
                            });
                        }
                    });
        } else {
            recipientEdit.setText(URI.getAddress().toString());
            if (URI.getAmount() != null) {
                Futures.addCallback(service.getSubaccountBalance(curSubaccount), new CB.NoOp<Map<?, ?>>() {
                    @Override
                    public void onSuccess(@Nullable final Map<?, ?> result) {
                        getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    final Float fiatRate = Float.valueOf((String) result.get("fiat_exchange"));
                                    amountEdit.setText(bitcoinFormat.noCode().format(URI.getAmount()));
                                    convertBtcToFiat(fiatRate);
                                    amountEdit.setEnabled(false);
                                    amountFiatEdit.setEnabled(false);
                                }
                        });
                    }
                }, service.es);
            }
        }
    }

    @Override
    public View onCreateView(@Nullable final LayoutInflater inflater, @Nullable final ViewGroup container,
                             @Nullable final Bundle savedInstanceState) {
        final GaService service = getGAService();

        registerReceiver();
        final GaActivity gaActivity = getGaActivity();

        if (savedInstanceState != null)
            pausing = savedInstanceState.getBoolean("pausing");

        rootView = inflater.inflate(R.layout.fragment_send, container, false);

        curSubaccount = service.getCurrentSubAccount();

        sendButton = (Button) rootView.findViewById(R.id.sendSendButton);
        maxButton = (Switch) rootView.findViewById(R.id.sendMaxButton);
        noteText = (EditText) rootView.findViewById(R.id.sendToNoteText);
        noteIcon = (TextView) rootView.findViewById(R.id.sendToNoteIcon);
        instantConfirmationCheckbox = (CheckBox) rootView.findViewById(R.id.instantConfirmationCheckBox);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            // pre-Material Design the label was already a part of the switch
            rootView.findViewById(R.id.sendMaxLabel).setVisibility(View.GONE);
        }

        amountEdit = (EditText) rootView.findViewById(R.id.sendAmountEditText);
        amountFiatEdit = (EditText) rootView.findViewById(R.id.sendAmountFiatEditText);
        recipientEdit = (EditText) rootView.findViewById(R.id.sendToEditText);
        scanIcon = (TextView) rootView.findViewById(R.id.sendScanIcon);

        final String btcUnit = (String) service.getUserConfig("unit");
        final TextView bitcoinScale = (TextView) rootView.findViewById(R.id.sendBitcoinScaleText);
        final TextView bitcoinUnitText = (TextView) rootView.findViewById(R.id.sendBitcoinUnitText);
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
            } catch (BitcoinURIParseException e) {
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
            public void onClick(final View view) {
                // FIXME: Instead of checking the state here, enable/disable sendButton when state changes
                if (!service.isLoggedIn()) {
                    gaActivity.toast(R.string.err_send_not_connected_will_resume);
                    return;
                }
                final String recipient = recipientEdit.getText().toString();
                final Coin amount;
                Coin nonFinalAmount;
                try {
                    nonFinalAmount = bitcoinFormat.parse(amountEdit.getText().toString());
                } catch (@NonNull final IllegalArgumentException e) {
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

                final Map<String, Object> privData = new HashMap<>();


                if (!noteText.getText().toString().isEmpty()) {
                    privData.put("memo", noteText.getText().toString());
                }

                if (curSubaccount != 0) {
                    privData.put("subaccount", curSubaccount);
                }

                if (instantConfirmationCheckbox.isChecked()) {
                    privData.put("instant", true);
                }

                ListenableFuture<PreparedTransaction> prepared;
                if (payreqData == null) {
                    if (!validAddress && !validAmount) {
                        message = getActivity().getString(R.string.invalidAmountAndAddress);
                    } else if (!validAddress) {
                        message = getActivity().getString(R.string.invalidAddress);
                    } else if (!validAmount) {
                        message = getActivity().getString(R.string.invalidAmount);
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
                            prepared = service.prepareSweepAll(curSubaccount, recipient, privData);
                        } else {
                            prepared = service.prepareTx(amount, recipient, privData);
                        }
                    } else {
                        prepared = null;
                    }
                } else {
                    prepared = service.preparePayreq(amount, payreqData, privData);
                }

                if (prepared != null) {
                    sendButton.setEnabled(false);
                    CB.after(prepared,
                            new CB.Toast<PreparedTransaction>(gaActivity, sendButton) {
                                @Override
                                public void onSuccess(@Nullable final PreparedTransaction result) {
                                    // final Coin fee = Coin.parseCoin("0.0001");        //FIXME: pass real fee
                                    CB.after(service.spv.validateTxAndCalculateFeeOrAmount(result, recipient, maxButton.isChecked() ? null : amount),
                                            new CB.Toast<Coin>(gaActivity, sendButton) {
                                                @Override
                                                public void onSuccess(@Nullable final Coin fee) {
                                                    final Map<?, ?> twoFacConfig = service.getTwoFacConfig();
                                                    // can be non-UI because validation talks to USB if hw wallet is used
                                                    getActivity().runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            sendButton.setEnabled(true);
                                                            final Coin dialogAmount, dialogFee;
                                                            if (maxButton.isChecked()) {
                                                                // 'fee' in reality is the sent amount in case passed amount=null
                                                                dialogAmount = fee;
                                                                dialogFee = service.getBalanceCoin(curSubaccount).subtract(fee);
                                                            } else {
                                                                dialogAmount = amount;
                                                                dialogFee = fee;
                                                            }
                                                            final boolean skipChoice = !result.requires_2factor ||
                                                                                        twoFacConfig == null || !((Boolean) twoFacConfig.get("any"));
                                                            mTwoFactor = GaActivity.popupTwoFactorChoice(gaActivity, service, skipChoice,
                                                                                                         new CB.Runnable1T<String>() {
                                                                @Override
                                                                public void run(final String method) {
                                                                    showTransactionSummary(method, dialogFee, dialogAmount, recipient, result);
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

        if (service.getBalanceCoin(curSubaccount) != null) {
            updateBalance();
        }

        scanIcon.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(final View view) {
                                            //New Marshmallow permissions paradigm
                                            final String[] perms = {"android.permission.CAMERA"};
                                            if (Build.VERSION.SDK_INT>Build.VERSION_CODES.LOLLIPOP_MR1 &&
                                                    getActivity().checkSelfPermission(perms[0]) != PackageManager.PERMISSION_GRANTED) {
                                                final int permsRequestCode = 100;
                                                getActivity().requestPermissions(perms, permsRequestCode);
                                            } else {

                                                final Intent qrcodeScanner = new Intent(getActivity(), ScanActivity.class);
                                                getActivity().startActivityForResult(qrcodeScanner, REQUEST_SEND_QR_SCAN);
                                            }
                                        }
                                    }
        );


        changeFiatIcon((FontAwesomeTextView) rootView.findViewById(R.id.sendFiatIcon),
                service.getFiatCurrency());

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
                    noteText.setVisibility(View.VISIBLE);
                    noteText.requestFocus();
                }
            }
        });

        hideInstantIf2of3();

        return rootView;
    }

    @Override
    public void onViewStateRestored(final @Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        pausing = false;
    }

    private void hideInstantIf2of3() {
        final GaService service = getGAService();
        instantConfirmationCheckbox.setVisibility(View.VISIBLE);
        for (Object subaccount_ : service.getSubaccounts()) {
            Map<String, ?> subaccountMap = (Map) subaccount_;
            if (subaccountMap.get("type").equals("2of3") && subaccountMap.get("pointer").equals(curSubaccount)) {
                instantConfirmationCheckbox.setVisibility(View.GONE);
                instantConfirmationCheckbox.setChecked(false);
            }
        }
    }

    @Override
    protected void onBalanceUpdated(final Activity activity) {
        updateBalance();
    }

    private void updateBalance() {
        final String btcUnit = (String) getGAService().getUserConfig("unit");
        final TextView sendSubAccountBalance = (TextView) rootView.findViewById(R.id.sendSubAccountBalance);
        final TextView sendSubAccountBalanceUnit = (TextView) rootView.findViewById(R.id.sendSubAccountBalanceUnit);
        final TextView sendSubAccountBitcoinScale = (TextView) rootView.findViewById(R.id.sendSubAccountBitcoinScale);
        sendSubAccountBitcoinScale.setText(Html.fromHtml(CurrencyMapper.mapBtcUnitToPrefix(btcUnit)));
        if (btcUnit == null || btcUnit.equals("bits")) {
            sendSubAccountBalanceUnit.setText("");
            sendSubAccountBitcoinScale.setText("bits ");
        } else {
            sendSubAccountBalanceUnit.setText(Html.fromHtml("&#xf15a; "));
        }
        final MonetaryFormat format = CurrencyMapper.mapBtcUnitToFormat(btcUnit);
        final String btcBalance = format.noCode().format(
                getGAService().getBalanceCoin(curSubaccount)).toString();
        final DecimalFormat formatter = new DecimalFormat("#,###.########");

        try {
            sendSubAccountBalance.setText(formatter.format(Double.valueOf(btcBalance)));
        } catch (@NonNull final NumberFormatException e) {
            sendSubAccountBalance.setText(btcBalance);
        }

        final int nChars = sendSubAccountBalance.getText().length() + sendSubAccountBitcoinScale.getText().length() + sendSubAccountBalanceUnit.getText().length();
        final int size = Math.min(50 - nChars, 34);
        sendSubAccountBalance.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
        sendSubAccountBalanceUnit.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
        sendSubAccountBalanceUnit.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
    }

    private void changeFiatIcon(@NonNull final FontAwesomeTextView fiatIcon, final String currency) {

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
            final Coin btcValue = bitcoinFormat.parse(amountEdit.getText().toString());
            Fiat fiatValue = rate.coinToFiat(btcValue);
            // strip extra decimals (over 2 places) because that's what the old JS client does
            fiatValue = fiatValue.subtract(fiatValue.divideAndRemainder((long) Math.pow(10, Fiat.SMALLEST_UNIT_EXPONENT - 2))[1]);
            amountFiatEdit.setText(fiatValue.toPlainString());
        } catch (@NonNull final ArithmeticException | IllegalArgumentException e) {
            if (amountEdit.getText().toString().equals(getString(R.string.send_max_amount))) {
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
            final Fiat fiatValue = Fiat.parseFiat("???", amountFiatEdit.getText().toString());
            amountEdit.setText(bitcoinFormat.noCode().format(rate.fiatToCoin(fiatValue)));
        } catch (@NonNull final ArithmeticException | IllegalArgumentException e) {
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
    public void onSaveInstanceState(@NonNull final Bundle outState) {
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
        CB.after(service.getSubaccountBalance(curSubaccount), new CB.NoOp<Map<?, ?>>() {
            @Override
            public void onSuccess(final @Nullable Map<?, ?> balance) {
                final Coin coin = Coin.valueOf(Long.valueOf((String) balance.get("satoshi")));
                final String btcUnit = (String) service.getUserConfig("unit");
                final TextView sendSubAccountBalance = (TextView) rootView.findViewById(R.id.sendSubAccountBalance);
                final MonetaryFormat format = CurrencyMapper.mapBtcUnitToFormat(btcUnit);
                final String btcBalance = format.noCode().format(coin).toString();
                final DecimalFormat formatter = new DecimalFormat("#,###.########");
                gaActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            sendSubAccountBalance.setText(formatter.format(Double.valueOf(btcBalance)));
                        } catch (@NonNull final NumberFormatException e) {
                            sendSubAccountBalance.setText(btcBalance);
                        }
                    }
                });
            }
        });
    }
}
