package com.greenaddress.greenbits.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.google.common.base.Function;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.greenaddress.greenapi.PreparedTransaction;
import com.greenaddress.greenbits.ConnectivityObservable;
import com.greenaddress.greenbits.GaService;
import com.greenaddress.greenbits.GreenAddressApplication;

import org.bitcoinj.core.Coin;
import org.bitcoinj.uri.BitcoinURI;
import org.bitcoinj.uri.BitcoinURIParseException;
import org.bitcoinj.utils.ExchangeRate;
import org.bitcoinj.utils.Fiat;
import org.bitcoinj.utils.MonetaryFormat;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import javax.annotation.Nullable;

import de.schildbach.wallet.ui.ScanActivity;

public class SendFragment extends GAFragment {

    private Dialog mSummary;
    private Dialog mTwoFactor;
    private EditText amountEdit;
    private EditText amountFiatEdit;
    private EditText recipientEdit;
    private EditText noteText;
    private CheckBox instantConfirmationCheckbox;
    private TextView noteIcon;
    private Button sendButton;
    private TextView scanIcon;
    private Map<?, ?> payreqData = null;
    private boolean fromIntentURI = false;


    private LinearLayout fiatGroup;
    private PopupMenu fiatPopup;
    private boolean converting = false;
    private int selected_group = 1;  // incremented on each selection change, not sure if there's
    private MonetaryFormat bitcoinFormat;
    // any better way to do it
    private View rootView;
    private int curSubaccount;
    private Observer curBalanceObserver;

    public void showTransactionSummary(final String method, final Coin fee, final Coin amount, final String recipient, final PreparedTransaction prepared) {
        Log.i("SendActivity.showTransactionSummary", "params " + method + " " + fee + " " + amount + " " + recipient);
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
        if (prefix.isEmpty()) {
            amountUnit.setText("bits ");
            feeUnit.setText("bits ");
        } else {
            amountUnit.setText(Html.fromHtml("&#xf15a; "));
            feeUnit.setText(Html.fromHtml("&#xf15a; "));
        }
        amountText.setText(bitcoinFormat.noCode().format(amount));
        feeText.setText(bitcoinFormat.noCode().format(fee));

        if (payreqData == null) {
            recipientText.setText(recipient.substring(0, 12) + "\n" + recipient.substring(12, 24) + "\n" + recipient.substring(24));
        } else {
            recipientText.setText(recipient);
        }

        final Map<String, String> twoFacData;

        if (method == null) {
            twoFAText.setVisibility(View.GONE);
            newTx2FACodeText.setVisibility(View.GONE);
            twoFacData = null;
        } else {
            twoFAText.setText("2FA " + method + " code");
            twoFacData = new HashMap<>();
            twoFacData.put("method", method);
            if (!method.equals("gauth")) {
                ((GreenAddressApplication) getActivity().getApplication()).gaService.requestTwoFacCode(method, "send_tx");
            }
        }

        mSummary = new MaterialDialog.Builder(getActivity())
                .title(R.string.newTxTitle)
                .customView(inflatedLayout)
                .positiveText(R.string.send)
                .negativeText(R.string.cancel)
                .positiveColorRes(R.color.accent)
                .negativeColorRes(R.color.accent)
                .titleColorRes(R.color.white)
                .contentColorRes(android.R.color.white)
                .theme(Theme.DARK)
                .callback(new MaterialDialog.Callback() {
                    @Override
                    public void onPositive(final MaterialDialog dialog) {
                        if (twoFacData != null) {
                            twoFacData.put("code", newTx2FACodeText.getText().toString());
                        }
                        final ListenableFuture<String> sendFuture = ((GreenAddressApplication) getActivity().getApplication()).gaService.signAndSendTransaction(prepared, twoFacData);
                        Futures.addCallback(sendFuture, new FutureCallback<String>() {
                            @Override
                            public void onSuccess(@Nullable final String result) {
                                if (fromIntentURI) {
                                    getActivity().finish();
                                    return;
                                }
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        // FIXME: Add notification with "Transaction sent"?
                                        amountEdit.setText("");
                                        recipientEdit.setText("");
                                        noteIcon.setText(Html.fromHtml("&#xf040"));
                                        noteText.setText("");
                                        noteText.setVisibility(View.INVISIBLE);
                                    }
                                });
                            }

                            @Override
                            public void onFailure(final Throwable t) {
                                t.printStackTrace();
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(getActivity(), t.getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                });
                            }
                        }, ((GreenAddressApplication) getActivity().getApplication()).gaService.es);
                    }

                    @Override
                    public void onNegative(final MaterialDialog dialog) {
                        Log.i("SendActivity.showTransactionSummary", "SHOWN ON CLOSE!");

                    }
                })
                .build();

        mSummary.show();
    }

    public void show2FAChoices(final Coin fee, final Coin amount, final String recipient, final PreparedTransaction prepared) {
        Log.i("SendActivity.show2FAChoices", "params " + fee + " " + amount + " " + recipient);
        String[] enabledTwoFacNames = new String[]{};
        final List<String> enabledTwoFacNamesSystem = ((GreenAddressApplication) getActivity().getApplication()).gaService.getEnabledTwoFacNames(true);
        mTwoFactor = new MaterialDialog.Builder(getActivity())
                .title(R.string.twoFactorChoicesTitle)
                .items(((GreenAddressApplication) getActivity().getApplication()).gaService.getEnabledTwoFacNames(false).toArray(enabledTwoFacNames))
                .itemsCallbackSingleChoice(0, new MaterialDialog.ListCallback() {
                    @Override
                    public void onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                        showTransactionSummary(enabledTwoFacNamesSystem.get(which), fee, amount, recipient, prepared);
                    }
                })
                .positiveText(R.string.choose)
                .negativeText(R.string.cancel)
                .positiveColorRes(R.color.accent)
                .negativeColorRes(R.color.accent)
                .titleColorRes(R.color.white)
                .contentColorRes(android.R.color.white)
                .theme(Theme.DARK)
                .build();
        mTwoFactor.show();
    }

    public void processBitcoinURI(final BitcoinURI URI) {
        if (URI.getPaymentRequestUrl() != null) {
            rootView.findViewById(R.id.sendBip70ProgressBar).setVisibility(View.VISIBLE);
            recipientEdit.setEnabled(false);
            sendButton.setEnabled(false);
            noteIcon.setVisibility(View.GONE);
            Futures.addCallback(((GreenAddressApplication) getActivity().getApplication()).gaService.processBip70URL(URI.getPaymentRequestUrl()),
                    new FutureCallback<Map<?, ?>>() {
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
                                    if (!amountStr.equals("")) {
                                        amountEdit.setText(amountStr);
                                        convertBtcToFiat();
                                        amountEdit.setEnabled(false);
                                        amountFiatEdit.setEnabled(false);
                                    }
                                    rootView.findViewById(R.id.sendBip70ProgressBar).setVisibility(View.GONE);
                                }
                            });
                        }

                        @Override
                        public void onFailure(final Throwable t) {
                            Toast.makeText(getActivity(), t.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        } else {
            recipientEdit.setText(URI.getAddress().toString());
            if (URI.getAmount() != null) {
                amountEdit.setText(bitcoinFormat.noCode().format(URI.getAmount()));
                convertBtcToFiat();
                amountEdit.setEnabled(false);
                amountFiatEdit.setEnabled(false);
            }
        }
    }

    @Override
    public View onGACreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_send, container, false);

        curSubaccount = getActivity().getSharedPreferences("send", Context.MODE_PRIVATE).getInt("curSubaccount", 0);

        sendButton = (Button) rootView.findViewById(R.id.sendSendButton);
        noteText = (EditText) rootView.findViewById(R.id.sendToNoteText);
        noteIcon = (TextView) rootView.findViewById(R.id.sendToNoteIcon);
        instantConfirmationCheckbox = (CheckBox) rootView.findViewById(R.id.instantConfirmationCheckBox);

        amountEdit = (EditText) rootView.findViewById(R.id.sendAmountEditText);
        amountFiatEdit = (EditText) rootView.findViewById(R.id.sendAmountFiatEditText);
        recipientEdit = (EditText) rootView.findViewById(R.id.sendToEditText);
        scanIcon = (TextView) rootView.findViewById(R.id.sendScanIcon);

        final String btcUnit = (String) ((GreenAddressApplication) getActivity().getApplication()).gaService.getAppearanceValue("unit");
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
                Toast.makeText(getActivity(), "Failed parsing Bitcoin URI", Toast.LENGTH_LONG).show();
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
                if (!((GreenAddressApplication) getActivity().getApplication()).getConnectionObservable().getState().equals(ConnectivityObservable.State.LOGGEDIN)) {
                    Toast.makeText(getActivity(), "Not connected, connection will resume automatically", Toast.LENGTH_LONG).show();
                    return;
                }
                final String recipient = recipientEdit.getText().toString();
                final Coin amount;
                Coin nonFinalAmount;
                try {
                    nonFinalAmount = bitcoinFormat.parse(amountEdit.getText().toString());
                } catch (final IllegalArgumentException e) {
                    nonFinalAmount = Coin.ZERO;
                }
                amount = nonFinalAmount;

                if (recipient.isEmpty()) {
                    Toast.makeText(getActivity(), "You need to provide a recipient", Toast.LENGTH_LONG).show();
                    return;
                }

                final GaService gaService = ((GreenAddressApplication) getActivity().getApplication()).gaService;
                final boolean validAddress = gaService.isValidAddress(recipient);

                final boolean validAmount = !(amount.compareTo(Coin.ZERO) <= 0);
                String message = null;

                Map<String, Object> privData = null;


                if (!noteText.getText().toString().isEmpty()) {
                    privData = new HashMap<>();
                    privData.put("memo", noteText.getText().toString());
                }

                if (curSubaccount != 0) {
                    if (privData == null) privData = new HashMap<>();
                    privData.put("subaccount", curSubaccount);
                }

                if (instantConfirmationCheckbox.isChecked()) {
                    if (privData == null) privData = new HashMap<>();
                    privData.put("instant", true);
                }

                ListenableFuture<PreparedTransaction> prepared;
                if (payreqData == null) {
                    if (!validAddress && !validAmount) {
                        message = getActivity().getString(R.string.invalidAmountAndAddress) ;
                    } else if (!validAddress) {
                        message = getActivity().getString(R.string.invalidAddress) ;
                    } else if (!validAmount) {
                        message = getActivity().getString(R.string.invalidAmount) ;
                    }
                    if (message == null) {
                        prepared = ((GreenAddressApplication) getActivity().getApplication()).gaService.prepareTx(amount, recipient, privData);
                    } else {
                        prepared = null;
                    }
                } else {
                    prepared = ((GreenAddressApplication) getActivity().getApplication()).gaService.preparePayreq(amount, payreqData, privData);
                }

                if (prepared != null) {
                    sendButton.setEnabled(false);
                    Futures.addCallback(prepared,
                            new FutureCallback<PreparedTransaction>() {
                                @Override
                                public void onSuccess(@Nullable final PreparedTransaction result) {
                                    // final Coin fee = Coin.parseCoin("0.0001");        //FIXME: pass real fee
                                    Futures.addCallback(((GreenAddressApplication) getActivity().getApplication()).gaService.validateTxAndCalculateFee(result, recipient, amount),
                                            new FutureCallback<Coin>() {
                                                @Override
                                                public void onSuccess(@Nullable final Coin fee) {
                                                    final Map<?, ?> twoFacConfig = ((GreenAddressApplication) getActivity().getApplication()).gaService.getTwoFacConfig();
                                                    // can be non-UI because validation talks to USB if hw wallet is used
                                                    getActivity().runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            sendButton.setEnabled(true);
                                                            if (result.requires_2factor.booleanValue() && twoFacConfig != null && ((Boolean) twoFacConfig.get("any")).booleanValue()) {
                                                                final List<String> enabledTwoFac =
                                                                        ((GreenAddressApplication) getActivity().getApplication()).gaService.getEnabledTwoFacNames(true);
                                                                if (enabledTwoFac.size() > 1) {
                                                                    show2FAChoices(fee, amount, recipient, result);
                                                                } else {
                                                                    showTransactionSummary(enabledTwoFac.get(0), fee, amount, recipient, result);
                                                                }
                                                            } else {
                                                                showTransactionSummary(null, fee, amount, recipient, result);
                                                            }
                                                        }
                                                    });
                                                }

                                                @Override
                                                public void onFailure(Throwable t) {
                                                    sendButton.setEnabled(true);
                                                    t.printStackTrace();
                                                    Toast.makeText(getActivity(), t.getMessage(), Toast.LENGTH_LONG).show();
                                                }
                                            });
                                }

                                @Override
                                public void onFailure(final Throwable t) {
                                    sendButton.setEnabled(true);
                                    Toast.makeText(getActivity(), t.getMessage(), Toast.LENGTH_LONG).show();
                                }
                            });
                }

                if (message != null) {
                    Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
                }
            }
        });

        curBalanceObserver = makeBalanceObserver();
        ((GreenAddressApplication) getActivity().getApplication()).gaService.getBalanceObservables().get(new Long(curSubaccount)).addObserver(curBalanceObserver);

        if (((GreenAddressApplication) getActivity().getApplication()).gaService.getBalanceCoin(curSubaccount) != null) {
            updateBalance(getActivity());
        }

        final Animation iconPressed = AnimationUtils.loadAnimation(getActivity().getApplicationContext(), R.anim.icon_pressed);
        scanIcon.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(final View view) {
                                            scanIcon.startAnimation(iconPressed);
                                            final Intent qrcodeScanner = new Intent(getActivity(), ScanActivity.class);
                                            getActivity().startActivityForResult(qrcodeScanner, TabbedMainActivity.REQUEST_SEND_QR_SCAN);
                                        }
                                    }
        );

        final LinearLayout bitcoinGroup = (LinearLayout) rootView.findViewById(R.id.sendBitcoinGroup);
        bitcoinGroup.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(final View view) {
                        if (!((GreenAddressApplication) getActivity().getApplication()).getConnectionObservable().getState().equals(ConnectivityObservable.State.LOGGEDIN)) {
                            Toast.makeText(getActivity(), "Not connected, connection will resume automatically", Toast.LENGTH_LONG).show();
                            return;
                        }
                        final PopupMenu popup = new PopupMenu(getActivity(), view);
                        popup.setOnMenuItemClickListener(
                                new PopupMenu.OnMenuItemClickListener() {

                                    @Override
                                    public boolean onMenuItemClick(final MenuItem item) {
                                        MonetaryFormat newFormat = bitcoinFormat;
                                        final GaService gaService = ((GreenAddressApplication) getActivity().getApplication()).gaService;
                                        switch (item.getItemId()) {
                                            case R.id.bitcoinScaleUnit:
                                                gaService.setAppearanceValue("unit", "BTC", true);
                                                bitcoinScale.setText("");
                                                bitcoinUnitText.setText(Html.fromHtml("&#xf15a;"));
                                                newFormat = CurrencyMapper.mapBtcUnitToFormat("BTC");
                                                break;
                                            case R.id.bitcoinScaleMilli:
                                                gaService.setAppearanceValue("unit", "mBTC", true);
                                                bitcoinScale.setText("m");
                                                bitcoinUnitText.setText(Html.fromHtml("&#xf15a;"));
                                                newFormat = CurrencyMapper.mapBtcUnitToFormat("mBTC");
                                                break;
                                            case R.id.bitcoinScaleMicro:
                                                gaService.setAppearanceValue("unit", Html.fromHtml("&micro;").toString() + "BTC", true);
                                                bitcoinScale.setText(Html.fromHtml("&micro;"));
                                                bitcoinUnitText.setText(Html.fromHtml("&#xf15a;"));
                                                newFormat = CurrencyMapper.mapBtcUnitToFormat(Html.fromHtml("&micro;").toString() + "BTC");
                                                break;
                                            case R.id.bitcoinScaleBits:
                                                gaService.setAppearanceValue("unit", "bits", true);
                                                bitcoinScale.setText("");
                                                bitcoinUnitText.setText("bits");
                                                newFormat = CurrencyMapper.mapBtcUnitToFormat("bits");
                                        }
                                        // update the values in main fragment
                                        gaService.fireBalanceChanged(0);
                                        for (Object subaccount : gaService.getSubaccounts()) {
                                            Map<String, ?> subaccountMap = (Map) subaccount;
                                            gaService.fireBalanceChanged(((Number) subaccountMap.get("pointer")).longValue());
                                        }

                                        updateBalance(getActivity());
                                        try {
                                            Coin oldValue = bitcoinFormat.parse(amountEdit.getText().toString());
                                            bitcoinFormat = newFormat;
                                            amountEdit.setText(newFormat.noCode().format(oldValue));
                                        } catch (IllegalArgumentException e) {
                                            bitcoinFormat = newFormat;
                                            amountEdit.setText("");
                                        }
                                        return false;
                                    }
                                }
                        );

                        popup.inflate(R.menu.bitcoin_scale);
                        popup.show();
                    }
                }
        );


        fiatGroup = (LinearLayout) rootView.findViewById(R.id.sendFiatGroup);

        changeFiatIcon((FontAwesomeTextView) rootView.findViewById(R.id.sendFiatIcon),
                ((GreenAddressApplication) getActivity().getApplication()).gaService.getFiatCurrency());

        fiatPopup = new PopupMenu(getActivity(), fiatGroup);
        final ArrayList<List<String>> currencyExchangePairs = new ArrayList<>();

        Futures.addCallback(
                ((GreenAddressApplication) getActivity().getApplication()).gaService.getCurrencyExchangePairs(),
                new FutureCallback<List<List<String>>>() {
                    @Override
                    public void onSuccess(@Nullable final List<List<String>> result) {
                        final Activity activity = getActivity();
                        if (activity != null) {
                            int order = 0;
                            for (final List<String> currency_exchange : result) {
                                currencyExchangePairs.add(currency_exchange);
                                final boolean current = currency_exchange.get(0).equals(((GreenAddressApplication) activity.getApplication()).gaService.getFiatCurrency())
                                        && currency_exchange.get(1).equals(((GreenAddressApplication) activity.getApplication()).gaService.getFiatExchange());
                                final int group = current ? selected_group : Menu.NONE;
                                fiatPopup.getMenu().add(group, order, order, formatFiatListItem(currency_exchange.get(0), currency_exchange.get(1)));
                                order += 1;
                            }
                            fiatPopup.getMenu().setGroupEnabled(selected_group, false);
                        }
                    }

                    @Override
                    public void onFailure(final Throwable t) {
                        t.printStackTrace();
                    }
                }, ((GreenAddressApplication) getActivity().getApplication()).gaService.es);

        fiatGroup.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(final View view) {
                        if (!((GreenAddressApplication) getActivity().getApplication()).getConnectionObservable().getState().equals(ConnectivityObservable.State.LOGGEDIN)) {
                            Toast.makeText(getActivity(), "Not connected, connection will resume automatically", Toast.LENGTH_LONG).show();
                            return;
                        }
                        fiatPopup.setOnMenuItemClickListener(
                                new PopupMenu.OnMenuItemClickListener() {

                                    @Override
                                    public boolean onMenuItemClick(final MenuItem item) {

                                        final List<String> currency_exchange = currencyExchangePairs.get(item.getItemId());

                                        changePricingSource(item.getItemId(), currency_exchange.get(0), currency_exchange.get(1));
                                        return false;
                                    }
                                }
                        );
                        fiatPopup.show();
                    }
                }
        );
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

        final GaService gaService = ((GreenAddressApplication) getActivity().getApplication()).gaService;
        hideInstantIf2of3();
        ((GreenAddressApplication) getActivity().getApplication()).configureSubaccountsFooter(
                curSubaccount,
                getActivity(),
                (TextView) rootView.findViewById(R.id.sendAccountName),
                (LinearLayout) rootView.findViewById(R.id.sendFooter),
                (LinearLayout) rootView.findViewById(R.id.footerClickableArea),
                new Function<Integer, Void>() {
                    @Nullable
                    @Override
                    public Void apply(@Nullable Integer input) {
                        ((GreenAddressApplication) getActivity().getApplication()).gaService.getBalanceObservables().get(new Long(curSubaccount)).deleteObserver(curBalanceObserver);
                        curSubaccount = input.intValue();
                        hideInstantIf2of3();
                        final SharedPreferences.Editor editor = getActivity().getSharedPreferences("send", Context.MODE_PRIVATE).edit();
                        editor.putInt("curSubaccount", curSubaccount);
                        editor.apply();
                        curBalanceObserver = makeBalanceObserver();
                        ((GreenAddressApplication) getActivity().getApplication()).gaService.getBalanceObservables().get(new Long(curSubaccount)).addObserver(curBalanceObserver);
                        Futures.addCallback(gaService.getSubaccountBalance(curSubaccount), new FutureCallback<Map<?, ?>>() {
                            @Override
                            public void onSuccess(@Nullable Map<?, ?> result) {
                                Coin coin = Coin.valueOf(Long.valueOf((String) result.get("satoshi")).longValue());
                                final String btcUnit = (String) gaService.getAppearanceValue("unit");
                                final TextView sendSubAccountBalance = (TextView) rootView.findViewById(R.id.sendSubAccountBalance);
                                MonetaryFormat format = CurrencyMapper.mapBtcUnitToFormat(btcUnit);
                                final String btcBalance = format.noCode().format(coin).toString();
                                final DecimalFormat formatter = new DecimalFormat("#,###.########");
                                try {
                                    sendSubAccountBalance.setText(formatter.format(formatter.parse(btcBalance)));
                                } catch (final ParseException e) {
                                    sendSubAccountBalance.setText(btcBalance);
                                }
                            }

                            @Override
                            public void onFailure(Throwable t) {

                            }
                        });
                        return null;
                    }
                },
                rootView.findViewById(R.id.sendNoTwoFacFooter)
        );

        return rootView;
    }

    private void hideInstantIf2of3() {
        final GaService gaService = ((GreenAddressApplication) getActivity().getApplication()).gaService;
        instantConfirmationCheckbox.setVisibility(View.VISIBLE);
        for (Object subaccount_ : gaService.getSubaccounts()) {
            Map<String, ?> subaccountMap = (Map) subaccount_;
            if (subaccountMap.get("type").equals("2of3") && subaccountMap.get("pointer").equals(curSubaccount)) {
                instantConfirmationCheckbox.setVisibility(View.GONE);
                instantConfirmationCheckbox.setChecked(false);
            }
        }
    }

    private Observer makeBalanceObserver() {
        return new Observer() {
            @Override
            public void update(final Observable observable, final Object o) {
                final Activity activity = getActivity();
                if (activity != null) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateBalance(activity);
                        }
                    });
                }
            }
        };
    }

    private void updateBalance(final Activity activity) {
        final String btcUnit = (String) ((GreenAddressApplication) activity.getApplication()).gaService.getAppearanceValue("unit");
        final TextView sendSubAccountBalance = (TextView) rootView.findViewById(R.id.sendSubAccountBalance);
        final TextView sendSubAccountBalanceUnit = (TextView) rootView.findViewById(R.id.sendSubAccountBalanceUnit);
        final TextView sendSubAccountBitcoinScale = (TextView) rootView.findViewById(R.id.sendSubAccountBitcoinScale);
        sendSubAccountBitcoinScale.setText(Html.fromHtml(CurrencyMapper.mapBtcUnitToPrefix(btcUnit)));
        if (btcUnit == null || btcUnit.equals("bits")) {
            sendSubAccountBalanceUnit.setText("bits ");
        } else{
            sendSubAccountBalanceUnit.setText(Html.fromHtml("&#xf15a; "));
        }
        MonetaryFormat format = CurrencyMapper.mapBtcUnitToFormat(btcUnit);
        final String btcBalance = format.noCode().format(
                ((GreenAddressApplication) activity.getApplication()).gaService.getBalanceCoin(curSubaccount)).toString();
        final DecimalFormat formatter = new DecimalFormat("#,###.########");

        try {
            sendSubAccountBalance.setText(formatter.format(formatter.parse(btcBalance)));
        } catch (final ParseException e) {
            sendSubAccountBalance.setText(btcBalance);
        }
    }

    private void changePricingSource(final int order, final String currency, final String exchange) {
        fiatGroup.setEnabled(false);
        final Animation rotateAnim = AnimationUtils.loadAnimation(getActivity(), R.anim.rotation);
        fiatGroup.startAnimation(rotateAnim);
        final ListenableFuture<Map<?, ?>> balanceFuture = Futures.transform(((GreenAddressApplication) getActivity().getApplication()).gaService.setPricingSource(currency, exchange),
                new AsyncFunction<Boolean, Map<?, ?>>() {
                    @Override
                    public ListenableFuture<Map<?, ?>> apply(final Boolean input) throws Exception {
                        return ((GreenAddressApplication) getActivity().getApplication()).gaService.updateBalance(curSubaccount);
                    }
                }, ((GreenAddressApplication) getActivity().getApplication()).gaService.es);
        final ListenableFuture future = Futures.transform(balanceFuture, new Function<Map<?, ?>, Object>() {
            @Nullable
            @Override
            public Object apply(@Nullable final Map<?, ?> result) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        convertBtcToFiat(Float.valueOf((String) result.get("fiat_exchange")).floatValue());
                        changeFiatIcon((FontAwesomeTextView) rootView.findViewById(R.id.sendFiatIcon), currency);
                        fiatPopup.getMenu().setGroupEnabled(selected_group++, true);
                        fiatPopup.getMenu().removeItem(order);
                        fiatPopup.getMenu().add(selected_group, order, order, formatFiatListItem(currency, exchange));
                        fiatPopup.getMenu().setGroupEnabled(selected_group, false);
                    }
                });
                return null;
            }
        }, ((GreenAddressApplication) getActivity().getApplication()).gaService.es);
        Futures.addCallback(future, new FutureCallback() {
            @Override
            public void onSuccess(@Nullable final Object result) {
                final Activity activity = getActivity();
                if (activity != null) {

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            fiatGroup.setEnabled(true);
                            fiatGroup.clearAnimation();
                        }
                    });
                }

            }

            @Override
            public void onFailure(final Throwable t) {
                final Activity activity = getActivity();
                if (activity != null) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            fiatGroup.setEnabled(true);
                            fiatGroup.clearAnimation();
                        }
                    });
                }
            }
        }, ((GreenAddressApplication) getActivity().getApplication()).gaService.es);
    }

    private Spanned formatFiatListItem(final String currency, final String exchange) {
        final String converted = CurrencyMapper.map(currency);
        final Spanned other = new SpannedString(currency + " (" + exchange + ")");
        if (converted != null) {
            final Spanned unit = Html.fromHtml(converted + " ");

            final SpannableStringBuilder sb = new SpannableStringBuilder(TextUtils.concat(unit, other));

            sb.setSpan(new CustomTypefaceSpan("", Typeface.createFromAsset(getActivity().getAssets(), "fonts/fontawesome-webfont.ttf")), 0, unit.length(),
                    Spanned.SPAN_EXCLUSIVE_INCLUSIVE);

            return sb;
        }
        return other;
    }

    private void changeFiatIcon(final FontAwesomeTextView fiatIcon, final String currency) {

        String converted = CurrencyMapper.map(currency);
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
        convertBtcToFiat(((GreenAddressApplication) getActivity().getApplication()).gaService.getFiatRate());
    }

    private void convertBtcToFiat(final float exchangeRate) {
        if (converting) {
            return;
        }
        converting = true;
        final Fiat exchangeFiat = Fiat.valueOf("???", new BigDecimal(exchangeRate).movePointRight(Fiat.SMALLEST_UNIT_EXPONENT)
                .toBigInteger().longValue());
        final ExchangeRate rate = new ExchangeRate(exchangeFiat);

        try {
            final Coin btcValue = bitcoinFormat.parse(amountEdit.getText().toString());
            Fiat fiatValue = rate.coinToFiat(btcValue);
            // strip extra decimals (over 2 places) because that's what the old JS client does
            fiatValue = fiatValue.subtract(fiatValue.divideAndRemainder((long) Math.pow(10, Fiat.SMALLEST_UNIT_EXPONENT - 2))[1]);
            amountFiatEdit.setText(fiatValue.toPlainString());
        } catch (final ArithmeticException | IllegalArgumentException e) {
            amountFiatEdit.setText("");
        }
        converting = false;
    }

    private void convertFiatToBtc() {
        if (converting) {
            return;
        }
        converting = true;
        final float exchangeRate = ((GreenAddressApplication) getActivity().getApplication()).gaService.getFiatRate();
        final Fiat exchangeFiat = Fiat.valueOf("???", new BigDecimal(exchangeRate).movePointRight(Fiat.SMALLEST_UNIT_EXPONENT)
                .toBigInteger().longValue());
        final ExchangeRate rate = new ExchangeRate(exchangeFiat);
        try {
            final Fiat fiatValue = Fiat.parseFiat("???", amountFiatEdit.getText().toString());
            amountEdit.setText(bitcoinFormat.noCode().format(rate.fiatToCoin(fiatValue)));
        } catch (final ArithmeticException | IllegalArgumentException e) {
            amountEdit.setText("");
        }
        converting = false;
    }

    public void onDestroyView() {
        super.onDestroyView();
        if (mSummary != null) {
            mSummary.dismiss();
        }
        if (mTwoFactor != null) {
            mTwoFactor.dismiss();
        }
    }
}
