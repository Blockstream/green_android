package com.greenaddress.greenbits.ui;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.greenaddress.greenbits.GaService;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Monetary;
import org.bitcoinj.utils.MonetaryFormat;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observer;

public class MainFragment extends SubaccountFragment {
    private MaterialDialog mUnconfirmedDialog = null;
    private View rootView;
    private List<TransactionItem> mTxItems;
    private Map<String, List<String> > replacedTxs;
    private Observer curBalanceObserver;
    private int curSubaccount;
    private final Observer mVerifiedTxObserver = makeUiObserver(new Runnable() { public void run() { onVerifiedTx(); } });
    private final Observer mNewTxObserver = makeUiObserver(new Runnable() { public void run() { onNewTx(); } });

    private void updateBalance() {
        final GaService service = getGAService();
        final Monetary monetary = service.getBalanceCoin(curSubaccount);
        if (monetary == null)
            return;

        if (service.getLoginData() == null)
            return;

        final String btcUnit = (String) service.getUserConfig("unit");
        final MonetaryFormat bitcoinFormat = CurrencyMapper.mapBtcUnitToFormat(btcUnit);
        final TextView balanceBitcoinIcon = (TextView) rootView.findViewById(R.id.mainBalanceBitcoinIcon);
        final TextView bitcoinScale = (TextView) rootView.findViewById(R.id.mainBitcoinScaleText);
        bitcoinScale.setText(Html.fromHtml(CurrencyMapper.mapBtcUnitToPrefix(btcUnit)));
        if (btcUnit == null || btcUnit.equals("bits")) {
            balanceBitcoinIcon.setText("");
            bitcoinScale.setText("bits ");
        } else {
            balanceBitcoinIcon.setText(Html.fromHtml("&#xf15a; "));
        }

        final String btcBalance = bitcoinFormat.noCode().format(
                monetary).toString();
        final String btcBalanceVerified;
        if (service.spv.verifiedBalancesCoin.get(curSubaccount) != null) {
            btcBalanceVerified = bitcoinFormat.noCode().format(
                    service.spv.verifiedBalancesCoin.get(curSubaccount)).toString();
        } else {
            btcBalanceVerified = bitcoinFormat.noCode().format(Coin.valueOf(0)).toString();
        }
        final String fiatBalance =
                MonetaryFormat.FIAT.minDecimals(2).noCode().format(
                        service.getBalanceFiat(curSubaccount))
                        .toString();
        final String fiatCurrency = service.getFiatCurrency();
        final String converted = CurrencyMapper.map(fiatCurrency);

        final TextView balanceText = (TextView) rootView.findViewById(R.id.mainBalanceText);
        final TextView balanceQuestionMark = (TextView) rootView.findViewById(R.id.mainBalanceQuestionMark);
        if (!service.isSPVEnabled() || btcBalance.equals(btcBalanceVerified))
            balanceQuestionMark.setVisibility(View.GONE);
        else
            balanceQuestionMark.setVisibility(View.VISIBLE);

        final TextView balanceFiatText = (TextView) rootView.findViewById(R.id.mainLocalBalanceText);
        final FontAwesomeTextView balanceFiatIcon = (FontAwesomeTextView) rootView.findViewById(R.id.mainLocalBalanceIcon);
        final DecimalFormat formatter = new DecimalFormat("#,###.########");
        try {
            balanceText.setText(formatter.format(Double.valueOf(btcBalance)));
        } catch (final NumberFormatException e) {
            balanceText.setText(btcBalance);
        }

        final int nChars = balanceText.getText().length() + balanceQuestionMark.getText().length() + bitcoinScale.getText().length() + balanceBitcoinIcon.getText().length();
        final int size = Math.min(50 - nChars, 34);
        balanceText.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
        bitcoinScale.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
        balanceBitcoinIcon.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);

        try {
            balanceFiatText.setText(formatter.format(Double.valueOf(fiatBalance)));
        } catch (final NumberFormatException e) {
            balanceFiatText.setText(fiatBalance);
        }

        if (converted != null) {
            balanceFiatIcon.setText(Html.fromHtml(converted + " "));
            balanceFiatIcon.setAwesomeTypeface();
            balanceFiatIcon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        } else {
            balanceFiatIcon.setText(fiatCurrency);
            balanceFiatIcon.setDefaultTypeface();
            balanceFiatIcon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        final GaService service = getGAService();

        registerReceiver();

        rootView = inflater.inflate(R.layout.fragment_main, container, false);
        final RecyclerView txView = (RecyclerView) rootView.findViewById(R.id.mainTransactionList);
        txView.setHasFixedSize(true);
        txView.addItemDecoration(new DividerItem(getActivity()));

        final LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        txView.setLayoutManager(layoutManager);

        curSubaccount = service.getCurrentSubAccount();

        final TextView firstP = (TextView) rootView.findViewById(R.id.mainFirstParagraphText);
        final TextView secondP = (TextView) rootView.findViewById(R.id.mainSecondParagraphText);
        final TextView thirdP = (TextView) rootView.findViewById(R.id.mainThirdParagraphText);

        firstP.setMovementMethod(LinkMovementMethod.getInstance());
        secondP.setMovementMethod(LinkMovementMethod.getInstance());
        thirdP.setMovementMethod(LinkMovementMethod.getInstance());

        final TextView balanceText = (TextView) rootView.findViewById(R.id.mainBalanceText);
        final TextView balanceQuestionMark = (TextView) rootView.findViewById(R.id.mainBalanceQuestionMark);
        final View.OnClickListener unconfirmedClickListener = new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                if (mUnconfirmedDialog == null && balanceQuestionMark.getVisibility() == View.VISIBLE) {
                    // Question mark is visible and dialog not shown, so show it
                    mUnconfirmedDialog = GaActivity.popup(getActivity(), R.string.unconfirmedBalanceTitle, 0)
                            .content(R.string.unconfirmedBalanceText)
                            .cancelListener(new DialogInterface.OnCancelListener() {
                                @Override
                                public void onCancel(final DialogInterface dialog) {
                                    mUnconfirmedDialog = null;
                                }
                            }).build();
                    mUnconfirmedDialog.show();
                }
            }
        };
        balanceText.setOnClickListener(unconfirmedClickListener);
        balanceQuestionMark.setOnClickListener(unconfirmedClickListener);

        curBalanceObserver = makeBalanceObserver();
        service.addBalanceObserver(curSubaccount, curBalanceObserver);

        if (service.getBalanceCoin(curSubaccount) != null) {
            updateBalance();
            reloadTransactions(false);
        }
        return rootView;
    }

    @Override
    protected void onBalanceUpdated() {
        updateBalance();
        reloadTransactions(true); // newAdapter for unit change
    }

    @Override
    public void onPause() {
        super.onPause();
        final GaService service = getGAService();
        service.deleteVerifiedTxObserver(mVerifiedTxObserver);
        service.deleteNewTxObserver(mNewTxObserver);
    }

    @Override
    public void onResume() {
        super.onResume();
        final GaService service = getGAService();
        service.addNewTxObserver(mNewTxObserver);
        service.addVerifiedTxObserver(mVerifiedTxObserver);
    }

    // Called when a new transaction is seen
    private void onNewTx() {
        reloadTransactions(false);
    }

    // Called when a new verified transaction is seen
    private void onVerifiedTx() {
        if (mTxItems == null)
          return;

        final SharedPreferences prefs = getGAService().cfgIn("verified_utxo_");
        for (final TransactionItem txItem : mTxItems)
            txItem.spvVerified = prefs.getBoolean(txItem.txhash, false);

        final RecyclerView txView = (RecyclerView) rootView.findViewById(R.id.mainTransactionList);
        txView.getAdapter().notifyDataSetChanged();
    }

    private void setVisibility(final int id, final int vis) {
        rootView.findViewById(id).setVisibility(vis);
    }

    private void showTxView(boolean doShow) {
        setVisibility(R.id.mainTransactionList, doShow ? View.VISIBLE : View.GONE);
        setVisibility(R.id.mainEmptyTransText, doShow ? View.GONE : View.VISIBLE);
    }

    private void reloadTransactions(boolean newAdapter) {
        final Activity activity = getActivity();
        final GaService service = getGAService();
        final RecyclerView txView = (RecyclerView) rootView.findViewById(R.id.mainTransactionList);

        if (mTxItems == null || newAdapter) {
            mTxItems = new ArrayList<>();
            txView.setAdapter(new ListTransactionsAdapter(activity, service, mTxItems));
            // FIXME, more efficient to use swap
            // txView.swapAdapter(lta, false);
        }

        if (replacedTxs == null || newAdapter)
            replacedTxs = new HashMap<>();

        Futures.addCallback(service.getMyTransactions(curSubaccount),
            new FutureCallback<Map<?, ?>>() {
            @Override
            public void onSuccess(final Map<?, ?> result) {
                final List txList = (List) result.get("list");
                final int currentBlock = ((Integer) result.get("cur_block"));

                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        showTxView(txList.size() > 0);

                        final String oldTop = mTxItems.size() > 0 ? mTxItems.get(0).txhash : null;
                        mTxItems.clear();
                        replacedTxs.clear();

                        for (Object tx : txList) {
                            try {
                                Map<String, Object> txJSON = (Map<String, Object>) tx;
                                ArrayList<String> replacedList = (ArrayList<String>) txJSON.get("replaced_by");

                                if (replacedList == null) {
                                    mTxItems.add(new TransactionItem(service, txJSON, currentBlock));
                                    continue;
                                }

                                for (String replacedBy : replacedList) {
                                    if (!replacedTxs.containsKey(replacedBy))
                                        replacedTxs.put(replacedBy, new ArrayList<String>());
                                    replacedTxs.get(replacedBy).add((String) txJSON.get("txhash"));
                                }
                            } catch (final ParseException e) {
                                e.printStackTrace();
                            }
                        }

                        for (TransactionItem txItem : mTxItems) {
                            if (!replacedTxs.containsKey(txItem.txhash))
                                continue;
                            for (String replaced : replacedTxs.get(txItem.txhash))
                                txItem.replacedHashes.add(replaced);
                        }

                        txView.getAdapter().notifyDataSetChanged();

                        final String newTop = mTxItems.size() > 0 ? mTxItems.get(0).txhash : null;
                        if (oldTop != null && newTop != null && !oldTop.equals(newTop)) {
                            // A new tx has arrived; scroll to the top to show it
                            txView.smoothScrollToPosition(0);
                        }
                    }
                });

            }

            @Override
            public void onFailure(final Throwable t) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showTxView(false);
                    }
                });
                t.printStackTrace();

            }
        }, service.es);
    }

    @Override
    protected void onSubaccountChanged(final int input) {
        final GaService service = getGAService();

        service.deleteBalanceObserver(curSubaccount, curBalanceObserver);
        curSubaccount = input;
        curBalanceObserver = makeBalanceObserver();
        service.addBalanceObserver(curSubaccount, curBalanceObserver);
        reloadTransactions(false);
        updateBalance();
    }

    @Override
    public void setUserVisibleHint(final boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            hideKeyboard();
        }
    }
}
