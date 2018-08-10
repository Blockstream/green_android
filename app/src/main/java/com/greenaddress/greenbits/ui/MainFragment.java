package com.greenaddress.greenbits.ui;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.greenaddress.greenapi.JSONMap;
import com.greenaddress.greenbits.GaService;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Sha256Hash;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Observer;

public class MainFragment extends SubaccountFragment {
    private static final String TAG = MainFragment.class.getSimpleName();

    private MaterialDialog mUnconfirmedDialog;
    private List<TransactionItem> mTxItems;
    private Map<Sha256Hash, List<Sha256Hash> > replacedTxs;
    private int mSubaccount;
    private Observer mVerifiedTxObserver;
    private Observer mNewTxObserver;
    private final Runnable mDialogCB = new Runnable() { public void run() { mUnconfirmedDialog = null; } };
    private SwipeRefreshLayout mSwipeRefreshLayout;

    private Boolean mIsExchanger = false;

    private void updateBalance() {
        Log.d(TAG, "Updating balance");
        if (isZombie())
            return;

        final GaService service = getGAService();
        final Coin balance = service.getCoinBalance(mSubaccount);
        if (service.getLoginData() == null || balance == null)
            return;

        final FontAwesomeTextView balanceUnit = UI.find(mView, R.id.mainBalanceUnit);
        final TextView balanceText = UI.find(mView, R.id.mainBalanceText);
        UI.setCoinText(service, balanceUnit, balanceText, balance);

        final Coin verifiedBalance = service.getSPVVerifiedBalance(mSubaccount);

        // Hide balance question mark if we know our balance is verified
        // (or we are in watch only mode and so have no SPV to verify it with)
        final boolean verified = balance.equals(verifiedBalance) || !service.isSPVEnabled();
        final TextView balanceQuestionMark = UI.find(mView, R.id.mainBalanceQuestionMark);
        UI.hideIf(verified, balanceQuestionMark);

        final TextView balanceFiatText = UI.find(mView, R.id.mainLocalBalanceText);
        final FontAwesomeTextView balanceFiatIcon = UI.find(mView, R.id.mainLocalBalanceIcon);

        final int nChars = balanceText.getText().length() + balanceQuestionMark.getText().length() + balanceUnit.getText().length();
        final int size = Math.min(50 - nChars, 34);
        balanceText.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
        balanceUnit.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);

        UI.setAmountText(balanceFiatText, service.getFiatBalance(mSubaccount));

        if (!service.isElements())
            AmountFields.changeFiatIcon(balanceFiatIcon, service.getFiatCurrency());
        else {
            balanceUnit.setText(String.format("%s ", service.getAssetSymbol()));
            balanceText.setText(service.getAssetFormat().format(balance));

            if (!mIsExchanger) {
                // No fiat values in elements multiasset
                UI.hide(UI.find(mView, R.id.mainLocalBalance));
                // Currently no SPV either
                UI.hide(balanceQuestionMark);
            }
        }

        if (service.showBalanceInTitle())
            UI.hide(balanceText, balanceUnit);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {

        Log.d(TAG, "onCreateView -> " + TAG);
        if (isZombieNoView())
            return null;

        final GaService service = getGAService();
        popupWaitDialog(R.string.loading_transactions);

        if (savedInstanceState != null)
            mIsExchanger = savedInstanceState.getBoolean("isExchanger", false);

        if (mIsExchanger)
            mView = inflater.inflate(R.layout.fragment_exchanger_txs, container, false);
        else
            mView = inflater.inflate(R.layout.fragment_main, container, false);
        final RecyclerView txView = UI.find(mView, R.id.mainTransactionList);
        txView.setHasFixedSize(true);
        txView.addItemDecoration(new DividerItem(getActivity()));

        final LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        txView.setLayoutManager(layoutManager);

        mSubaccount = service.getCurrentSubAccount();

        if (!mIsExchanger) {
            final TextView firstP = UI.find(mView, R.id.mainFirstParagraphText);
            final TextView secondP = UI.find(mView, R.id.mainSecondParagraphText);
            final TextView thirdP = UI.find(mView, R.id.mainThirdParagraphText);

            if (service.isElements())
                UI.hide(firstP); // Don't show a Bitcoin message for elements
            else
                firstP.setMovementMethod(LinkMovementMethod.getInstance());
            secondP.setMovementMethod(LinkMovementMethod.getInstance());
            thirdP.setMovementMethod(LinkMovementMethod.getInstance());
        }

        final TextView balanceText = UI.find(mView, R.id.mainBalanceText);
        final TextView balanceQuestionMark = UI.find(mView, R.id.mainBalanceQuestionMark);
        final View.OnClickListener unconfirmedClickListener = new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                if (mUnconfirmedDialog == null && balanceQuestionMark.getVisibility() == View.VISIBLE) {
                    // Question mark is visible and dialog not shown, so show it
                    mUnconfirmedDialog = UI.popup(getActivity(), R.string.unconfirmedBalanceTitle, 0)
                                           .content(R.string.unconfirmedBalanceText).build();
                    UI.setDialogCloseHandler(mUnconfirmedDialog, mDialogCB);
                    mUnconfirmedDialog.show();
                }
            }
        };
        balanceText.setOnClickListener(unconfirmedClickListener);
        balanceQuestionMark.setOnClickListener(unconfirmedClickListener);

        makeBalanceObserver(mSubaccount);
        if (isPageSelected() && service.getCoinBalance(mSubaccount) != null) {
            updateBalance();
            reloadTransactions(true);
        }

        if (!mIsExchanger) {
            mSwipeRefreshLayout = UI.find(mView, R.id.mainTransactionListSwipe);
            mSwipeRefreshLayout.setColorSchemeColors(ContextCompat.getColor(getContext(), R.color.accent));
            mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    Log.d(TAG, "onRefresh -> " + TAG);
                    // user action to force reload balance and tx list
                    onBalanceUpdated();
                }
            });
        }

        registerReceiver();
        return mView;
    }

    @Override
    protected void onBalanceUpdated() {
        Log.d(TAG, "onBalanceUpdated -> " + TAG);
        updateBalance();
        reloadTransactions(false);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause -> " + TAG);
        detachObservers();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume -> " + TAG);
        if (getGAService() != null)
            attachObservers();
        setIsDirty(true);
    }

    @Override
    public void attachObservers() {
        if (mVerifiedTxObserver == null) {
            mNewTxObserver = makeUiObserver(new Runnable() { public void run() { onNewTx(); } });
            getGAService().addNewTxObserver(mNewTxObserver);
        }
        if (mVerifiedTxObserver == null) {
            mVerifiedTxObserver = makeUiObserver(new Runnable() { public void run() { onVerifiedTx(); } });
            getGAService().addVerifiedTxObserver(mVerifiedTxObserver);
        }
        super.attachObservers();
    }

    @Override
    public void detachObservers() {
        super.detachObservers();
        if (mVerifiedTxObserver != null) {
            getGAService().deleteNewTxObserver(mNewTxObserver);
            mNewTxObserver = null;
        }
        if (mVerifiedTxObserver != null) {
            getGAService().deleteVerifiedTxObserver(mVerifiedTxObserver);
            mVerifiedTxObserver = null;
        }
    }

    // Called when a new transaction is seen
    private void onNewTx() {
        if (!isPageSelected()) {
            Log.d(TAG, "New transaction while page hidden");
            setIsDirty(true);
            return;
        }
        reloadTransactions(false);
    }

    // Called when a new verified transaction is seen
    private void onVerifiedTx() {
        if (mTxItems == null)
          return;

        final GaService service = getGAService();
        final boolean isSPVEnabled = service.isSPVEnabled();

        for (final TransactionItem txItem : mTxItems)
            txItem.spvVerified = isSPVEnabled ? service.isSPVVerified(txItem.txHash) : true;

        final RecyclerView txView = UI.find(mView, R.id.mainTransactionList);
        txView.getAdapter().notifyDataSetChanged();
    }

    private void showTxView(final boolean doShow) {
        UI.showIf(doShow, UI.find(mView, R.id.mainTransactionList));
        if (!mIsExchanger)
            UI.hideIf(doShow, UI.find(mView, R.id.mainEmptyTransText));
    }

    private void reloadTransactions(final boolean showWaitDialog) {
        final Activity activity = getActivity();
        final GaService service = getGAService();
        final RecyclerView txView;

        if (isZombie())
            return;

        // Mark ourselves as clean before fetching. This means that while the callback
        // is running, we may be marked dirty again if a new block arrives, which
        // is required to avoid missing updates while the RPC is in flight.
        setIsDirty(false);

        txView = UI.find(mView, R.id.mainTransactionList);

        if (mTxItems == null || mTxItems.isEmpty() || showWaitDialog) {
            // Show a wait dialog only when initially loading transactions
            popupWaitDialog(R.string.loading_transactions);
        }

        if (mTxItems == null) {
            mTxItems = new ArrayList<>();
            txView.setAdapter(new ListTransactionsAdapter(activity, service, mTxItems, mIsExchanger));
            // FIXME, more efficient to use swap
            // txView.swapAdapter(lta, false);
        }

        if (replacedTxs == null)
            replacedTxs = new HashMap<>();

        Futures.addCallback(service.getMyTransactions(mSubaccount),
            new FutureCallback<Map<String, Object>>() {
            @Override
            public void onSuccess(final Map<String, Object> result) {
                final List txList = (List) result.get("list");
                final int currentBlock = ((Integer) result.get("cur_block"));

                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        if (mSwipeRefreshLayout != null)
                            mSwipeRefreshLayout.setRefreshing(false);

                        if (!isPageSelected()) {
                            Log.d(TAG, "Callback after hiding, ignoring");
                            // Mark ourselves as dirty so we reload when next shown
                            setIsDirty(true);
                            return;
                        }

                        showTxView(!txList.isEmpty());

                        final Sha256Hash oldTop = !mTxItems.isEmpty() ? mTxItems.get(0).txHash : null;
                        mTxItems.clear();
                        replacedTxs.clear();

                        for (final Object tx : txList) {
                            try {
                                final JSONMap txJSON = (JSONMap) tx;
                                final ArrayList<String> replacedList = txJSON.get("replaced_by");

                                if (replacedList == null) {
                                    mTxItems.add(new TransactionItem(service, txJSON, currentBlock));
                                    continue;
                                }

                                for (final String replacedBy : replacedList) {
                                    final Sha256Hash replacedHash = Sha256Hash.wrap(replacedBy);
                                    if (!replacedTxs.containsKey(replacedHash))
                                        replacedTxs.put(replacedHash, new ArrayList<Sha256Hash>());
                                    replacedTxs.get(replacedHash).add(txJSON.getHash("txhash"));
                                }
                            } catch (final ParseException e) {
                                e.printStackTrace();
                            }
                        }

                        final Iterator<TransactionItem> iterator = mTxItems.iterator();
                        while (iterator.hasNext()) {
                            final TransactionItem txItem = iterator.next();
                            final boolean isExchangerAddress = service.cfg().getBoolean("exchanger_address_" + txItem.receivedOn, false);
                            if (isExchangerAddress && txItem.memo == null) {
                                txItem.memo = Exchanger.TAG_EXCHANGER_TX_MEMO;
                                CB.after(service.changeMemo(txItem.txHash.toString(), Exchanger.TAG_EXCHANGER_TX_MEMO, null),
                                         new CB.Toast<Boolean>(activity));
                            } else if (mIsExchanger && (txItem.memo == null || !txItem.memo.contains(Exchanger.TAG_EXCHANGER_TX_MEMO))) {
                                // FIXME should be better to filter list with api query
                                iterator.remove();
                            }
                            if (replacedTxs.containsKey(txItem.txHash))
                                txItem.replacedHashes.addAll(replacedTxs.get(txItem.txHash));
                        }

                        txView.getAdapter().notifyDataSetChanged();

                        final Sha256Hash newTop = !mTxItems.isEmpty() ? mTxItems.get(0).txHash : null;
                        if (oldTop != null && newTop != null && !oldTop.equals(newTop)) {
                            // A new tx has arrived; scroll to the top to show it
                            txView.smoothScrollToPosition(0);
                        }
                        hideWaitDialog();
                    }
                });

            }

            @Override
            public void onFailure(final Throwable t) {
                t.printStackTrace();
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        hideWaitDialog();
                        if (mSwipeRefreshLayout != null)
                            mSwipeRefreshLayout.setRefreshing(false);
                    }
                });
            }
        }, service.getExecutor());
    }

    @Override
    protected void onSubaccountChanged(final int newSubAccount) {
        mSubaccount = newSubAccount;
        makeBalanceObserver(mSubaccount);
        if (!isPageSelected()) {
            Log.d(TAG, "Subaccount changed while page hidden");
            setIsDirty(true);
            return;
        }
        reloadTransactions(true);
        updateBalance();
    }

    @Override
    public void setUserVisibleHint(final boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            hideKeyboard();
        }
    }

    public void setPageSelected(final boolean isSelected) {
        final boolean needReload = isSelected && !isPageSelected() && isDirty();
        super.setPageSelected(isSelected);
        if (needReload) {
            Log.d(TAG, "Dirty, reloading");
            reloadTransactions(true);
            updateBalance();
            if (!isZombie())
                setIsDirty(false);
        }
    }

    public void setIsExchanger(final boolean isExchanger) {
        mIsExchanger = isExchanger;
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("isExchanger", mIsExchanger);
    }
}
