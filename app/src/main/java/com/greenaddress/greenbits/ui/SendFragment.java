package com.greenaddress.greenbits.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
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
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Switch;
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

    private static final String TAG = "SendFragment";
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


    private LinearLayout fiatGroup;
    private PopupMenu fiatPopup;
    private boolean converting = false;
    private int selected_group = 1;  // incremented on each selection change, not sure if there's
    private MonetaryFormat bitcoinFormat;
    // any better way to do it
    private View rootView;
    private int curSubaccount;
    private Observer curBalanceObserver;
    private boolean pausing;

    public void showTransactionSummary(final String method, final Coin fee, final Coin amount, final String recipient, final PreparedTransaction prepared) {
        Log.i(TAG, "showTransactionSummary( params " + method + " " + fee + " " + amount + " " + recipient + ")");
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
                getGAService().requestTwoFacCode(method, "send_tx");
            }
        }

        mSummary = new MaterialDialog.Builder(getActivity())
                .title(R.string.newTxTitle)
                .customView(inflatedLayout, true)
                .positiveText(R.string.send)
                .negativeText(R.string.cancel)
                .positiveColorRes(R.color.accent)
                .negativeColorRes(R.color.accent)
                .titleColorRes(R.color.white)
                .contentColorRes(android.R.color.white)
                .theme(Theme.DARK)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(final MaterialDialog dialog) {
                        if (twoFacData != null) {
                            twoFacData.put("code", newTx2FACodeText.getText().toString());
                        }
                        final ListenableFuture<String> sendFuture = getGAService().signAndSendTransaction(prepared, twoFacData);
                        Futures.addCallback(sendFuture, new FutureCallback<String>() {
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
                                        ViewPager mViewPager = (ViewPager) getActivity().findViewById(R.id.pager);
                                        mViewPager.setCurrentItem(1);
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
                        }, getGAService().es);
                    }

                    @Override
                    public void onNegative(final MaterialDialog dialog) {
                        Log.i(TAG, "SHOWN ON CLOSE!");

                    }
                })
                .build();

        mSummary.show();
    }

    public void show2FAChoices(final Coin fee, final Coin amount, final String recipient, final PreparedTransaction prepared) {
        Log.i(TAG, "params " + fee + " " + amount + " " + recipient);
        String[] enabledTwoFacNames = new String[]{};
        final List<String> enabledTwoFacNamesSystem = getGAService().getEnabledTwoFacNames(true);
        mTwoFactor = new MaterialDialog.Builder(getActivity())
                .title(R.string.twoFactorChoicesTitle)
                .items(getGAService().getEnabledTwoFacNames(false).toArray(enabledTwoFacNames))
                .itemsCallbackSingleChoice(0, new MaterialDialog.ListCallbackSingleChoice() {
                    @Override
                    public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                        showTransactionSummary(enabledTwoFacNamesSystem.get(which), fee, amount, recipient, prepared);
                        return true;
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
            Futures.addCallback(getGAService().processBip70URL(URI.getPaymentRequestUrl()),
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

                        @Override
                        public void onFailure(final Throwable t) {
                            Toast.makeText(getActivity(), t.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        } else {
            recipientEdit.setText(URI.getAddress().toString());
            if (URI.getAmount() != null) {
                final ListenableFuture<Map<?, ?>> future = getGAService().getClient().getBalance(curSubaccount);
                Futures.addCallback(future, new FutureCallback<Map<?, ?>>() {
                    @Override
                    public void onSuccess(@Nullable final Map<?, ?> result) {
                        getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Float fiatRate = Float.valueOf((String) result.get("fiat_exchange")).floatValue();
                                    amountEdit.setText(bitcoinFormat.noCode().format(URI.getAmount()));
                                    convertBtcToFiat(fiatRate);
                                    amountEdit.setEnabled(false);
                                    amountFiatEdit.setEnabled(false);
                                }
                            });
                        }
                    @Override
                    public void onFailure(final Throwable t) {

                    }
                }, getGAService().es);
            }
        }
    }

    @Override
    public View onGACreateView(final LayoutInflater inflater, final ViewGroup container,
                               final Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            pausing = savedInstanceState.getBoolean("pausing");
        }

        rootView = inflater.inflate(R.layout.fragment_send, container, false);

        curSubaccount = getGAApp().getSharedPreferences("send", Context.MODE_PRIVATE).getInt("curSubaccount", 0);

        sendButton = (Button) rootView.findViewById(R.id.sendSendButton);
        maxButton = (Switch) rootView.findViewById(R.id.sendMaxButton);
        noteText = (EditText) rootView.findViewById(R.id.sendToNoteText);
        noteIcon = (TextView) rootView.findViewById(R.id.sendToNoteIcon);
        instantConfirmationCheckbox = (CheckBox) rootView.findViewById(R.id.instantConfirmationCheckBox);

        if (Build.VERSION.SDK_INT < (new Build.VERSION_CODES()).LOLLIPOP) {
            // pre-Material Design the label was already a part of the switch
            rootView.findViewById(R.id.sendMaxLabel).setVisibility(View.GONE);
        }

        amountEdit = (EditText) rootView.findViewById(R.id.sendAmountEditText);
        amountFiatEdit = (EditText) rootView.findViewById(R.id.sendAmountFiatEditText);
        recipientEdit = (EditText) rootView.findViewById(R.id.sendToEditText);
        scanIcon = (TextView) rootView.findViewById(R.id.sendScanIcon);

        final String btcUnit = (String) getGAService().getAppearanceValue("unit");
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
                if (!getGAApp().getConnectionObservable().getState().equals(ConnectivityObservable.State.LOGGEDIN)) {
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

                final GaService gaService = getGAService();
                final boolean validAddress = gaService.isValidAddress(recipient);

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
                            prepared = getGAService().prepareSweepAll(curSubaccount, recipient, privData);
                        } else {
                            prepared = getGAService().prepareTx(amount, recipient, privData);
                        }
                    } else {
                        prepared = null;
                    }
                } else {
                    prepared = getGAService().preparePayreq(amount, payreqData, privData);
                }

                if (prepared != null) {
                    sendButton.setEnabled(false);
                    Futures.addCallback(prepared,
                            new FutureCallback<PreparedTransaction>() {
                                @Override
                                public void onSuccess(@Nullable final PreparedTransaction result) {
                                    // final Coin fee = Coin.parseCoin("0.0001");        //FIXME: pass real fee
                                    Futures.addCallback(getGAService().validateTxAndCalculateFeeOrAmount(
                                                    result, recipient, maxButton.isChecked() ? null : amount),
                                            new FutureCallback<Coin>() {
                                                @Override
                                                public void onSuccess(@Nullable final Coin fee) {
                                                    final Map<?, ?> twoFacConfig = getGAService().getTwoFacConfig();
                                                    // can be non-UI because validation talks to USB if hw wallet is used
                                                    getActivity().runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            sendButton.setEnabled(true);
                                                            final Coin dialogAmount, dialogFee;
                                                            if (maxButton.isChecked()) {
                                                                // 'fee' in reality is the sent amount in case passed amount=null
                                                                dialogAmount = fee;
                                                                dialogFee = getGAService().getBalanceCoin(curSubaccount).subtract(fee);
                                                            } else {
                                                                dialogAmount = amount;
                                                                dialogFee = fee;
                                                            }
                                                            if (result.requires_2factor.booleanValue() && twoFacConfig != null && ((Boolean) twoFacConfig.get("any")).booleanValue()) {
                                                                final List<String> enabledTwoFac =
                                                                        getGAService().getEnabledTwoFacNames(true);
                                                                if (enabledTwoFac.size() > 1) {
                                                                    show2FAChoices(dialogFee, dialogAmount, recipient, result);
                                                                } else {
                                                                    showTransactionSummary(enabledTwoFac.get(0), dialogFee, dialogAmount, recipient, result);
                                                                }
                                                            } else {
                                                                showTransactionSummary(null, dialogFee, dialogAmount, recipient, result);
                                                            }
                                                        }
                                                    });
                                                }

                                                @Override
                                                public void onFailure(final Throwable t) {
                                                    final Activity activity = getActivity();
                                                    if (activity != null) {
                                                        activity.runOnUiThread(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                sendButton.setEnabled(true);
                                                                t.printStackTrace();
                                                                Toast.makeText(activity, t.getMessage(), Toast.LENGTH_LONG).show();                                                            }
                                                        });
                                                    }
                                                }
                                            });
                                }

                                @Override
                                public void onFailure(final Throwable t) {
                                    final Activity activity = getActivity();
                                    if (activity != null) {
                                        activity.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                sendButton.setEnabled(true);
                                                Toast.makeText(activity, t.getMessage(), Toast.LENGTH_LONG).show();                                           }
                                        });
                                    }
                                }
                            });
                }

                if (message != null) {
                    Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
                }
            }
        });

        maxButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton v, boolean isChecked) {
                if (isChecked) {
                    amountEdit.setEnabled(false);
                    amountFiatEdit.setEnabled(false);
                    amountEdit.setText("MAX");
                } else {
                    amountEdit.setText("");
                    amountEdit.setEnabled(true);
                    amountFiatEdit.setEnabled(true);
                }
            }
        });

        curBalanceObserver = makeBalanceObserver();
        getGAService().getBalanceObservables().get(new Long(curSubaccount)).addObserver(curBalanceObserver);

        if (getGAService().getBalanceCoin(curSubaccount) != null) {
            updateBalance();
        }

        final Animation iconPressed = AnimationUtils.loadAnimation(getActivity().getApplicationContext(), R.anim.icon_pressed);
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

                                                scanIcon.startAnimation(iconPressed);
                                                final Intent qrcodeScanner = new Intent(getActivity(), ScanActivity.class);
                                                getActivity().startActivityForResult(qrcodeScanner, TabbedMainActivity.REQUEST_SEND_QR_SCAN);
                                            }
                                        }
                                    }
        );

        final LinearLayout bitcoinGroup = (LinearLayout) rootView.findViewById(R.id.sendBitcoinGroup);
        bitcoinGroup.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(final View view) {
                        if (!getGAApp().getConnectionObservable().getState().equals(ConnectivityObservable.State.LOGGEDIN)) {
                            Toast.makeText(getActivity(), "Not connected, connection will resume automatically", Toast.LENGTH_LONG).show();
                            return;
                        }
                        final PopupMenu popup = new PopupMenu(getActivity(), view);
                        popup.setOnMenuItemClickListener(
                                new PopupMenu.OnMenuItemClickListener() {

                                    @Override
                                    public boolean onMenuItemClick(final MenuItem item) {
                                        MonetaryFormat newFormat = bitcoinFormat;
                                        final GaService gaService = getGAService();
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

                                        updateBalance();
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
                getGAService().getFiatCurrency());

        fiatPopup = new PopupMenu(getActivity(), fiatGroup);
        final ArrayList<List<String>> currencyExchangePairs = new ArrayList<>();

        Futures.addCallback(
                getGAService().getCurrencyExchangePairs(),
                new FutureCallback<List<List<String>>>() {
                    @Override
                    public void onSuccess(@Nullable final List<List<String>> result) {
                        final Activity activity = getActivity();
                        if (activity != null) {
                            int order = 0;
                            for (final List<String> currency_exchange : result) {
                                currencyExchangePairs.add(currency_exchange);
                                final boolean current = currency_exchange.get(0).equals(getGAService().getFiatCurrency())
                                        && currency_exchange.get(1).equals(getGAService().getFiatExchange());
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
                }, getGAService().es);

        fiatGroup.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(final View view) {
                        if (!getGAApp().getConnectionObservable().getState().equals(ConnectivityObservable.State.LOGGEDIN)) {
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

        final GaService gaService = getGAService();
        hideInstantIf2of3();
        getGAApp().configureSubaccountsFooter(
                curSubaccount,
                getActivity(),
                (TextView) rootView.findViewById(R.id.sendAccountName),
                (LinearLayout) rootView.findViewById(R.id.sendFooter),
                (LinearLayout) rootView.findViewById(R.id.footerClickableArea),
                new Function<Integer, Void>() {
                    @Nullable
                    @Override
                    public Void apply(@Nullable Integer input) {
                        getGAService().getBalanceObservables().get(new Long(curSubaccount)).deleteObserver(curBalanceObserver);
                        curSubaccount = input.intValue();
                        hideInstantIf2of3();
                        final SharedPreferences.Editor editor = getGAApp().getSharedPreferences("send", Context.MODE_PRIVATE).edit();
                        editor.putInt("curSubaccount", curSubaccount);
                        editor.apply();
                        curBalanceObserver = makeBalanceObserver();
                        getGAService().getBalanceObservables().get(new Long(curSubaccount)).addObserver(curBalanceObserver);
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

    @Override
    public void onViewStateRestored(@android.support.annotation.Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        pausing = false;
    }

    private void hideInstantIf2of3() {
        final GaService gaService = getGAService();
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
                            updateBalance();
                        }
                    });
                }
            }
        };
    }

    private void updateBalance() {
        final String btcUnit = (String) getGAService().getAppearanceValue("unit");
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
        MonetaryFormat format = CurrencyMapper.mapBtcUnitToFormat(btcUnit);
        final String btcBalance = format.noCode().format(
                getGAService().getBalanceCoin(curSubaccount)).toString();
        final DecimalFormat formatter = new DecimalFormat("#,###.########");

        try {
            sendSubAccountBalance.setText(formatter.format(formatter.parse(btcBalance)));
        } catch (final ParseException e) {
            sendSubAccountBalance.setText(btcBalance);
        }

        final int nChars = sendSubAccountBalance.getText().length() + sendSubAccountBitcoinScale.getText().length() + sendSubAccountBalanceUnit.getText().length();
        final int size = Math.min(50 - nChars, 34);
        sendSubAccountBalance.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
        sendSubAccountBalanceUnit.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
        sendSubAccountBalanceUnit.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
    }

    private void changePricingSource(final int order, final String currency, final String exchange) {
        fiatGroup.setEnabled(false);
        final Animation rotateAnim = AnimationUtils.loadAnimation(getActivity(), R.anim.rotation);
        fiatGroup.startAnimation(rotateAnim);
        final ListenableFuture<Map<?, ?>> balanceFuture = Futures.transform(getGAService().setPricingSource(currency, exchange),
                new AsyncFunction<Boolean, Map<?, ?>>() {
                    @Override
                    public ListenableFuture<Map<?, ?>> apply(final Boolean input) throws Exception {
                        return getGAService().updateBalance(curSubaccount);
                    }
                }, getGAService().es);
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
        }, getGAService().es);
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
        }, getGAService().es);
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
        final ExchangeRate rate = new ExchangeRate(exchangeFiat);

        try {
            final Coin btcValue = bitcoinFormat.parse(amountEdit.getText().toString());
            Fiat fiatValue = rate.coinToFiat(btcValue);
            // strip extra decimals (over 2 places) because that's what the old JS client does
            fiatValue = fiatValue.subtract(fiatValue.divideAndRemainder((long) Math.pow(10, Fiat.SMALLEST_UNIT_EXPONENT - 2))[1]);
            amountFiatEdit.setText(fiatValue.toPlainString());
        } catch (final ArithmeticException | IllegalArgumentException e) {
            if (amountEdit.getText().toString().equals("MAX")) {
                amountFiatEdit.setText("MAX");
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
        if (mSummary != null) {
            mSummary.dismiss();
        }
        if (mTwoFactor != null) {
            mTwoFactor.dismiss();
        }
    }
}
