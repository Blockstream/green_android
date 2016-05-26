package com.greenaddress.greenbits.ui;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.greenaddress.greenbits.GaService;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Monetary;
import org.bitcoinj.utils.MonetaryFormat;
import org.codehaus.jackson.map.MappingJsonFactory;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

public class MainFragment extends SubaccountFragment implements Observer {
    @Nullable
    private MaterialDialog mUnconfirmedDialog = null;
    private View rootView;
    private List<TransactionItem> currentList;
    private Map<String, List<String> > replacedTxs;
    @Nullable
    private Observer curBalanceObserver;
    private int curSubaccount;
    @Nullable
    private Observer txVerifiedObservable;

    private void updateBalance() {
        final GaService service = getGAService();
        final Monetary monetary = service.getBalanceCoin(curSubaccount);
        if (monetary == null)
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
        if (!getGAService().isSPVEnabled() || btcBalance.equals(btcBalanceVerified))
            balanceQuestionMark.setVisibility(View.GONE);
        else
            balanceQuestionMark.setVisibility(View.VISIBLE);

        final TextView balanceFiatText = (TextView) rootView.findViewById(R.id.mainLocalBalanceText);
        final FontAwesomeTextView balanceFiatIcon = (FontAwesomeTextView) rootView.findViewById(R.id.mainLocalBalanceIcon);
        final DecimalFormat formatter = new DecimalFormat("#,###.########");
        try {
            balanceText.setText(formatter.format(Double.valueOf(btcBalance)));
        } catch (@NonNull final NumberFormatException e) {
            balanceText.setText(btcBalance);
        }

        final int nChars = balanceText.getText().length() + balanceQuestionMark.getText().length() + bitcoinScale.getText().length() + balanceBitcoinIcon.getText().length();
        final int size = Math.min(50 - nChars, 34);
        balanceText.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
        bitcoinScale.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
        balanceBitcoinIcon.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);

        try {
            balanceFiatText.setText(formatter.format(Double.valueOf(fiatBalance)));
        } catch (@NonNull final NumberFormatException e) {
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
    public View onCreateView(@NonNull final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        registerReceiver();

        rootView = inflater.inflate(R.layout.fragment_main, container, false);
        final RecyclerView recyclerView = (RecyclerView) rootView.findViewById(R.id.mainTransactionList);
        recyclerView.setHasFixedSize(true);
        recyclerView.addItemDecoration(new DividerItem(getActivity()));

        final LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);

        curSubaccount = getGAService().getCurrentSubAccount();

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
                    mUnconfirmedDialog = GaActivity.Popup(getActivity(), getString(R.string.unconfirmedBalanceTitle), 0)
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
        getGAService().getBalanceObservables().get(curSubaccount).addObserver(curBalanceObserver);

        if (getGAService().getBalanceCoin(curSubaccount) != null)
            updateBalance();

        reloadTransactions(getActivity());
        return rootView;
    }

    @Nullable
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
                            reloadTransactions(activity, true);  // newAdapter for unit change
                        }
                    });
                }
            }
        };
    }

    @Override
    public void onPause() {
        super.onPause();
        getGAService().getNewTxVerifiedObservable().deleteObserver(txVerifiedObservable);
        getGAService().getNewTransactionsObservable().deleteObserver(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        getGAService().getNewTransactionsObservable().addObserver(this);
        getGAService().getNewTxVerifiedObservable().addObserver(makeTxVerifiedObservable());
    }

    @Nullable
    private Observer makeTxVerifiedObservable() {
        txVerifiedObservable = new Observer() {
            @Override
            public void update(final Observable observable, final Object data) {
                if (currentList == null) return;
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        final SharedPreferences prefs = getGAService().cfgIn("verified_utxo_");
                        for (final TransactionItem tx : currentList)
                            tx.spvVerified = prefs.getBoolean(tx.txhash, false);

                        final RecyclerView recycleView = (RecyclerView) rootView.findViewById(R.id.mainTransactionList);
                        recycleView.getAdapter().notifyDataSetChanged();
                    }
                });
            }
        };
        return txVerifiedObservable;
    }

    private void reloadTransactions(@NonNull final Activity activity) {
        reloadTransactions(activity, false);
    }

    private void reloadTransactions(@NonNull final Activity activity, boolean newAdapter) {
        final GaService service = getGAService();
        final RecyclerView recyclerView = (RecyclerView) rootView.findViewById(R.id.mainTransactionList);
        final LinearLayout mainEmptyTransText = (LinearLayout) rootView.findViewById(R.id.mainEmptyTransText);

        if (currentList == null || newAdapter) {
            currentList = new ArrayList<>();
            recyclerView.setAdapter(new ListTransactionsAdapter(activity, service, currentList));
            // FIXME, more efficient to use swap
            // recyclerView.swapAdapter(lta, false);

        }

        if (replacedTxs == null || newAdapter)
            replacedTxs = new HashMap<>();

        final ListenableFuture<Map<?, ?>> txFuture = service.getMyTransactions(curSubaccount);

        Futures.addCallback(txFuture, new FutureCallback<Map<?, ?>>() {
            @Override
            public void onSuccess(@Nullable final Map<?, ?> result) {
                final List resultList = (List) result.get("list");
                final int curBlock = ((Integer) result.get("cur_block"));
                service.setCurBlock(curBlock);

                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // "Make sure the content of your adapter is not modified from a background
                        //  thread, but only from the UI thread. Make sure your adapter calls
                        //  notifyDataSetChanged() when its content changes."

                        final GaService service = getGAService();
                        if (service.isSPVEnabled()) {
                            service.spv.setUpSPV();
                            service.spv.startSpvSync();
                        }
                        if (resultList != null && resultList.size() > 0) {
                            recyclerView.setVisibility(View.VISIBLE);
                            mainEmptyTransText.setVisibility(View.GONE);
                        } else {
                            recyclerView.setVisibility(View.GONE);
                            mainEmptyTransText.setVisibility(View.VISIBLE);
                        }

                        final String oldFirstTxHash = currentList.size() > 0? currentList.get(0).txhash : null;

                        currentList.clear();
                        replacedTxs.clear();
                        for (int i = 0; i < resultList.size(); ++i) {
                            try {
                                Map<String, Object> txMap = (Map<String, Object>) resultList.get(i);
                                if (txMap.get("replaced_by") != null) {
                                    ArrayList replaced_by_arr = (ArrayList) txMap.get("replaced_by");
                                    for (int j = 0; j < replaced_by_arr.size(); ++j) {
                                        String replaced_by = (String) replaced_by_arr.get(j);
                                        if (!replacedTxs.containsKey(replaced_by)) {
                                            replacedTxs.put(replaced_by, new ArrayList<String>());
                                        }
                                        replacedTxs.get(replaced_by).add((String) txMap.get("txhash"));
                                    }
                                } else {
                                    currentList.add(new TransactionItem(service, txMap, curBlock));
                                }
                            } catch (@NonNull final ParseException e) {
                                e.printStackTrace();
                            }
                        }

                        for (int i = 0; i < currentList.size(); ++i) {
                            String txhash = currentList.get(i).txhash;
                            if (replacedTxs.containsKey(txhash)) {
                                for (int j = 0; j < replacedTxs.get(txhash).size(); ++j) {
                                    currentList.get(i).replacedHashes.add(
                                            replacedTxs.get(txhash).get(j)
                                    );
                                }
                            }
                        }

                        String newFirstTxHash = null;
                        if (currentList.size() > 0) {
                            newFirstTxHash = currentList.get(0).txhash;
                        }

                        recyclerView.getAdapter().notifyDataSetChanged();

                        // scroll to top when new tx comes in
                        if (oldFirstTxHash != null && newFirstTxHash != null &&
                                !oldFirstTxHash.equals(newFirstTxHash)) {
                            recyclerView.smoothScrollToPosition(0);
                        }

                    }
                });

            }

            @Override
            public void onFailure(@NonNull final Throwable t) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        recyclerView.setVisibility(View.GONE);
                        mainEmptyTransText.setVisibility(View.VISIBLE);
                    }
                });
                t.printStackTrace();

            }
        }, service.es);
    }

    @Override
    public void update(final Observable observable, final Object data) {
        getGAService().spv.startSpvSync();
        reloadTransactions(getActivity());
    }

    @Override
    protected void onSubaccountChanged(final int input) {
        getGAService().getBalanceObservables().get(curSubaccount).deleteObserver(curBalanceObserver);
        curSubaccount = input;
        curBalanceObserver = makeBalanceObserver();
        getGAService().getBalanceObservables().get(curSubaccount).addObserver(curBalanceObserver);
        reloadTransactions(getActivity());
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
