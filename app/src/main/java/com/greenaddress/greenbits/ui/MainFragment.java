package com.greenaddress.greenbits.ui;

import android.app.Activity;
import android.content.DialogInterface;
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
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.utils.MonetaryFormat;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observer;

public class MainFragment extends SubaccountFragment {
    private MaterialDialog mUnconfirmedDialog = null;
    private View mView;
    private List<TransactionItem> mTxItems;
    private Map<Sha256Hash, List<Sha256Hash> > replacedTxs;
    private Observer curBalanceObserver;
    private int curSubaccount;
    private final Observer mVerifiedTxObserver = makeUiObserver(new Runnable() { public void run() { onVerifiedTx(); } });
    private final Observer mNewTxObserver = makeUiObserver(new Runnable() { public void run() { onNewTx(); } });

    private void updateBalance() {
        final GaService service = getGAService();
        final Monetary monetary = service.getCoinBalance(curSubaccount);
        if (monetary == null)
            return;

        if (service.getLoginData() == null)
            return;

        final String btcUnit = (String) service.getUserConfig("unit");
        final MonetaryFormat bitcoinFormat = CurrencyMapper.mapBtcUnitToFormat(btcUnit).noCode();
        final TextView balanceBitcoinIcon = UI.find(mView, R.id.mainBalanceBitcoinIcon);
        final TextView bitcoinScale = UI.find(mView, R.id.mainBitcoinScaleText);
        bitcoinScale.setText(Html.fromHtml(CurrencyMapper.mapBtcUnitToPrefix(btcUnit)));
        if (btcUnit == null || btcUnit.equals("bits")) {
            balanceBitcoinIcon.setText("");
            bitcoinScale.setText("bits ");
        } else {
            balanceBitcoinIcon.setText(Html.fromHtml("&#xf15a; "));
        }

        final String btcBalance = bitcoinFormat.format(monetary).toString();
        final String btcVerifiedBalance;
        final Coin verifiedBalance = service.getSPVVerifiedBalance(curSubaccount);
        if (verifiedBalance != null)
            btcVerifiedBalance = bitcoinFormat.format(verifiedBalance).toString();
        else
            btcVerifiedBalance = bitcoinFormat.format(Coin.valueOf(0)).toString();

        final String fiatBalance =
                MonetaryFormat.FIAT.minDecimals(2).noCode().format(
                        service.getFiatBalance(curSubaccount))
                        .toString();
        final String fiatCurrency = service.getFiatCurrency();
        final String converted = CurrencyMapper.map(fiatCurrency);

        // Hide balance question mark if we know our balance is verified
        // (or we are in watch only mode and so have no SPV to verify it with)
        final TextView balanceQuestionMark = UI.find(mView, R.id.mainBalanceQuestionMark);
        final boolean verified = btcBalance.equals(btcVerifiedBalance) ||
                                 !service.isSPVEnabled();
        UI.hideIf(verified, balanceQuestionMark);

        final TextView balanceText = UI.find(mView, R.id.mainBalanceText);
        final TextView balanceFiatText = UI.find(mView, R.id.mainLocalBalanceText);
        final FontAwesomeTextView balanceFiatIcon = UI.find(mView, R.id.mainLocalBalanceIcon);
        UI.setAmountText(balanceText, btcBalance);

        final int nChars = balanceText.getText().length() + balanceQuestionMark.getText().length() + bitcoinScale.getText().length() + balanceBitcoinIcon.getText().length();
        final int size = Math.min(50 - nChars, 34);
        balanceText.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
        bitcoinScale.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
        balanceBitcoinIcon.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);

        UI.setAmountText(balanceFiatText, fiatBalance);

        if (converted != null) {
            balanceFiatIcon.setText(Html.fromHtml(converted + " "));
            balanceFiatIcon.setAwesomeTypeface();
            balanceFiatIcon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        } else {
            balanceFiatIcon.setText(fiatCurrency);
            balanceFiatIcon.setDefaultTypeface();
            balanceFiatIcon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        }
        if (service.showBalanceInTitle())
            UI.hide(bitcoinScale, balanceText, balanceBitcoinIcon);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        final GaService service = getGAService();

        registerReceiver();

        mView = inflater.inflate(R.layout.fragment_main, container, false);
        final RecyclerView txView = UI.find(mView, R.id.mainTransactionList);
        txView.setHasFixedSize(true);
        txView.addItemDecoration(new DividerItem(getActivity()));

        final LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        txView.setLayoutManager(layoutManager);

        curSubaccount = service.getCurrentSubAccount();

        final TextView firstP = UI.find(mView, R.id.mainFirstParagraphText);
        final TextView secondP = UI.find(mView, R.id.mainSecondParagraphText);
        final TextView thirdP = UI.find(mView, R.id.mainThirdParagraphText);

        firstP.setMovementMethod(LinkMovementMethod.getInstance());
        secondP.setMovementMethod(LinkMovementMethod.getInstance());
        thirdP.setMovementMethod(LinkMovementMethod.getInstance());

        final TextView balanceText = UI.find(mView, R.id.mainBalanceText);
        final TextView balanceQuestionMark = UI.find(mView, R.id.mainBalanceQuestionMark);
        final View.OnClickListener unconfirmedClickListener = new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                if (mUnconfirmedDialog == null && balanceQuestionMark.getVisibility() == View.VISIBLE) {
                    // Question mark is visible and dialog not shown, so show it
                    mUnconfirmedDialog = UI.popup(getActivity(), R.string.unconfirmedBalanceTitle, 0)
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

        if (service.getCoinBalance(curSubaccount) != null) {
            updateBalance();
            reloadTransactions(false);
        }
        return mView;
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

        final GaService service = getGAService();
        for (final TransactionItem txItem : mTxItems)
            txItem.spvVerified = service.isSPVVerified(txItem.txHash);

        final RecyclerView txView = UI.find(mView, R.id.mainTransactionList);
        txView.getAdapter().notifyDataSetChanged();
    }

    private void showTxView(boolean doShow) {
        UI.showIf(doShow, (View) UI.find(mView, R.id.mainTransactionList));
        UI.hideIf(doShow, (View) UI.find(mView, R.id.mainEmptyTransText));
    }

    private void reloadTransactions(boolean newAdapter) {
        final Activity activity = getActivity();
        final GaService service = getGAService();
        final RecyclerView txView = UI.find(mView, R.id.mainTransactionList);

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
                    public void run() {

                        showTxView(txList.size() > 0);

                        final Sha256Hash oldTop = mTxItems.size() > 0 ? mTxItems.get(0).txHash : null;
                        mTxItems.clear();
                        replacedTxs.clear();

                        for (Object tx : txList) {
                            try {
                                Map<String, Object> txJSON = (Map) tx;
                                ArrayList<String> replacedList = (ArrayList) txJSON.get("replaced_by");

                                if (replacedList == null) {
                                    mTxItems.add(new TransactionItem(service, txJSON, currentBlock));
                                    continue;
                                }

                                for (String replacedBy : replacedList) {
                                    final Sha256Hash replacedHash = Sha256Hash.wrap(replacedBy);
                                    if (!replacedTxs.containsKey(replacedHash))
                                        replacedTxs.put(replacedHash, new ArrayList<Sha256Hash>());
                                    final Sha256Hash newTxHash = Sha256Hash.wrap((String) txJSON.get("txhash"));
                                    replacedTxs.get(replacedHash).add(newTxHash);
                                }
                            } catch (final ParseException e) {
                                e.printStackTrace();
                            }
                        }

                        for (TransactionItem txItem : mTxItems) {
                            if (replacedTxs.containsKey(txItem.txHash))
                                for (Sha256Hash replaced : replacedTxs.get(txItem.txHash))
                                    txItem.replacedHashes.add(replaced);
                        }

                        txView.getAdapter().notifyDataSetChanged();

                        final Sha256Hash newTop = mTxItems.size() > 0 ? mTxItems.get(0).txHash : null;
                        if (oldTop != null && newTop != null && !oldTop.equals(newTop)) {
                            // A new tx has arrived; scroll to the top to show it
                            txView.smoothScrollToPosition(0);
                        }
                    }
                });

            }

            @Override
            public void onFailure(final Throwable t) {
                t.printStackTrace();
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        showTxView(false);
                    }
                });
            }
        }, service.getExecutor());
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
