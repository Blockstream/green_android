package com.greenaddress.greenbits.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.greenaddress.greenapi.data.BalanceData;
import com.greenaddress.greenapi.data.SubaccountData;
import com.greenaddress.greenapi.data.TransactionData;
import com.greenaddress.greenapi.model.ActiveAccountObservable;
import com.greenaddress.greenapi.model.BalanceDataObservable;
import com.greenaddress.greenapi.model.ReceiveAddressObservable;
import com.greenaddress.greenapi.model.TransactionDataObservable;
import com.greenaddress.greenbits.GaService;
import com.greenaddress.greenbits.ui.components.AccountView;
import com.greenaddress.greenbits.ui.components.OnGdkListener;

import org.bitcoinj.core.Sha256Hash;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observer;

import static android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION;
import static com.greenaddress.greenbits.ui.TabbedMainActivity.REQUEST_SELECT_ASSET;

public class MainFragment extends SubaccountFragment implements View.OnClickListener, OnGdkListener {
    private static final String TAG = MainFragment.class.getSimpleName();
    private AccountView mAccountView;
    private final List<TransactionItem> mTxItems = new ArrayList<>();
    private Map<Sha256Hash, List<Sha256Hash>> replacedTxs;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private boolean justClicked = false;
    private ListTransactionsAdapter mTransactionsAdapter;
    private LinearLayoutManager mLayoutManager = new LinearLayoutManager(getActivity());

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        Log.d(TAG, "onCreateView -> " + TAG);
        if (isZombieNoView())
            return null;

        final GaService service = getGAService();

        mView = inflater.inflate(R.layout.fragment_main, container, false);

        // Setup recycler & adapter
        final RecyclerView txView = UI.find(mView, R.id.mainTransactionList);
        txView.setHasFixedSize(true);
        txView.addItemDecoration(new DividerItem(getActivity()));
        txView.setLayoutManager(mLayoutManager);
        float offsetPx = getResources().getDimension(R.dimen.adapter_bar);
        final BottomOffsetDecoration bottomOffsetDecoration = new BottomOffsetDecoration((int) offsetPx);
        txView.addItemDecoration(bottomOffsetDecoration);
        mTransactionsAdapter = new ListTransactionsAdapter(getGaActivity(), service, mTxItems);
        txView.setAdapter(mTransactionsAdapter);
        txView.addOnScrollListener(recyclerViewOnScrollListener);
        // FIXME, more efficient to use swap
        // txView.swapAdapter(lta, false);
        mSwipeRefreshLayout = UI.find(mView, R.id.mainTransactionListSwipe);
        mSwipeRefreshLayout.setColorSchemeColors(ContextCompat.getColor(getContext(), R.color.accent));
        mSwipeRefreshLayout.setOnRefreshListener(() -> {
            Log.d(TAG, "onRefresh -> " + TAG);
            final int subaccount = service.getModel().getCurrentSubaccount();
            service.getModel().getTransactionDataObservable(subaccount).refresh();
        });

        // Setup account card view
        mAccountView = UI.find(mView, R.id.accountView);
        mAccountView.setIcon(getResources().getDrawable(getGAService().getNetwork().getIcon()));
        mAccountView.listMode(true);
        mAccountView.setBackgroundForNetwork(service.getNetwork());
        mAccountView.setOnClickListener(this);
        if (service.getModel().isTwoFAReset())
            mAccountView.hideActions();
        else
            mAccountView.showActions(service.isWatchOnly());

        final TextView assetsSelection = UI.find(mView, R.id.assetsSelection);
        assetsSelection.setOnClickListener(v -> startActivityForResult(new Intent(getGaActivity(),
                                                                                  AssetsSelectActivity.class),
                                                                       REQUEST_SELECT_ASSET));
        try {
            final int size = service.getSession().getBalance(service.getModel().getCurrentSubaccount(),0).size();
            if (size > 1) {
                assetsSelection.setText(size + " assets in this wallet");
            } else {
                assetsSelection.setVisibility(View.GONE);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return mView;
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!isDisconnected()) {
            justClicked = false;
            mTxItems.clear();
            mTransactionsAdapter.notifyDataSetChanged();
            UI.find(mView, R.id.loadingList).setVisibility(View.VISIBLE);
            onUpdateBalance(mBalanceDataObservable);
            onUpdateReceiveAddress(mReceiveAddressObservable);
            onUpdateTransactions(mTransactionDataObservable);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    // Called when a new transaction is seen
    @Override
    public void onNewTx(final Observer observer) {
        if (!isPageSelected()) {
            Log.d(TAG, "New transaction while page hidden");
            setIsDirty(true);
            return;
        }
        //reloadTransactions(false);
    }

    // Called when a new verified transaction is seen
    @Override
    public void onVerifiedTx(final Observer observer) {

        final GaService service = getGAService();
        final boolean isSPVEnabled = service.isSPVEnabled();

        for (final TransactionItem txItem : mTxItems)
            txItem.spvVerified = !isSPVEnabled || service.isSPVVerified(txItem.txHash);

        final RecyclerView txView = UI.find(mView, R.id.mainTransactionList);
        txView.getAdapter().notifyDataSetChanged();
    }

    private void showTxView(final boolean doShowTxList) {
        UI.find(mView, R.id.loadingList).setVisibility(View.GONE);
        UI.showIf(doShowTxList, UI.find(mView, R.id.mainTransactionList));
        UI.showIf(!doShowTxList, UI.find(mView, R.id.emptyListText));
    }

    private void reloadTransactions(final List<TransactionData> txList, final boolean showWaitDialog) {
        final GaService service = getGAService();
        final int subaccount = service.getModel().getCurrentSubaccount();
        final RecyclerView txView;

        if (isZombie())
            return;

        // Mark ourselves as clean before fetching. This means that while the callback
        // is running, we may be marked dirty again if a new block arrives, which
        // is required to avoid missing updates while the RPC is in flight.
        setIsDirty(false);

        txView = UI.find(mView, R.id.mainTransactionList);

        if (replacedTxs == null)
            replacedTxs = new HashMap<>();

        try {
            final int currentBlock = service.getModel().getBlockchainHeightObservable().getHeight();

            if (mSwipeRefreshLayout != null)
                mSwipeRefreshLayout.setRefreshing(false);

            showTxView(!txList.isEmpty());

            final Sha256Hash oldTop = !mTxItems.isEmpty() ? mTxItems.get(0).txHash : null;
            mTxItems.clear();
            replacedTxs.clear();

            for (final TransactionData tx : txList) {
                try {
                    mTxItems.add(new TransactionItem(service, tx, currentBlock, subaccount));
                    /*
                       //TODO gdk handling of replaced
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
                     */
                } catch (final ParseException e) {
                    e.printStackTrace();
                }
            }

            for (final TransactionItem txItem : mTxItems) {
                if (replacedTxs.containsKey(txItem.txHash))
                    txItem.replacedHashes.addAll(replacedTxs.get(txItem.txHash));
            }

            txView.getAdapter().notifyDataSetChanged();

            final Sha256Hash newTop = !mTxItems.isEmpty() ? mTxItems.get(0).txHash : null;
            if (oldTop != null && newTop != null && !oldTop.equals(newTop)) {
                // A new tx has arrived; scroll to the top to show it
                txView.smoothScrollToPosition(0);
            }

        } catch (final Exception e) {
            e.printStackTrace();
            if (mSwipeRefreshLayout != null)
                mSwipeRefreshLayout.setRefreshing(false);
        }
    }

    @Override
    public void setUserVisibleHint(final boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            hideKeyboard();
        }
    }

    @Override
    public void onClick(final View view) {
        if (justClicked) {
            justClicked = false;
            return;
        }
        if (view.getId() == R.id.receiveButton) {
            justClicked = true;
            final Intent intent = new Intent(getActivity(), ReceiveActivity.class);
            getActivity().startActivity(intent);
        } else if (view.getId() == R.id.sendButton) {
            justClicked = true;
            final Intent intent = new Intent(getActivity(), ScanActivity.class);
            getActivity().startActivity(intent);
        } else if (view.getId() == R.id.selectSubaccount) {
            final Intent intent = new Intent(getActivity(), SubaccountSelectActivity.class);
            intent.setFlags(FLAG_ACTIVITY_NO_ANIMATION);
            getActivity().startActivity(intent);
        }
    }

    @Override
    public void onUpdateBalance(final BalanceDataObservable observable) {
        Log.d(TAG, "Updating balance");
        final BalanceData balanceData = observable.getBalanceData();
        if (isZombie() || balanceData == null)
            return;

        getGaActivity().runOnUiThread(() -> {
            final GaService service = getGAService();
            final int subaccount = service.getModel().getCurrentSubaccount();
            final SubaccountData subaccountData = service.getSubaccountData(subaccount);
            mAccountView.setTitle(subaccountData.getNameWithDefault(getString(R.string.id_main_account)));
            mAccountView.setIcon(getResources().getDrawable(getGAService().getNetwork().getIcon()));
            mAccountView.setBalance(service, balanceData);
            if (service.isElements()) {
                // No fiat values in elements multiasset
                UI.hide(UI.find(mView, R.id.mainLocalBalanceText));
            }
        });
    }

    @Override
    public void onUpdateReceiveAddress(final ReceiveAddressObservable observable) {}

    @Override
    public void onUpdateTransactions(final TransactionDataObservable observable) {
        isLoading=false;
        List<TransactionData> txList = observable.getTransactionDataList();
        if (isZombie() || txList == null || !observable.isExecutedOnce())
            return;
        Log.d(TAG, "Updating transactions");
        isLastPage=observable.isLastPage();
        getGaActivity().runOnUiThread(() -> reloadTransactions(txList, false));
    }

    @Override
    public void onUpdateActiveSubaccount(final ActiveAccountObservable observable) {}

    private boolean isLoading = false;
    private boolean isLastPage = false;

    private void loadMoreItems() {
        Log.d(TAG, "loadMoreItems");
        isLoading=true;
        final GaService service = getGAService();
        final int subaccount = service.getModel().getCurrentSubaccount();
        service.getModel().getTransactionDataObservable(subaccount).refresh(false);
    }

    private RecyclerView.OnScrollListener recyclerViewOnScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(final RecyclerView recyclerView, final int newState) {
            super.onScrollStateChanged(recyclerView, newState);
        }

        @Override
        public void onScrolled(final RecyclerView recyclerView, final int dx, final int dy) {
            super.onScrolled(recyclerView, dx, dy);
            int visibleItemCount = mLayoutManager.getChildCount();
            int totalItemCount = mLayoutManager.getItemCount();
            int firstVisibleItemPosition = mLayoutManager.findFirstVisibleItemPosition();

            /*Log.d(TAG, "visible: " + visibleItemCount +
                    " total:  " +totalItemCount +
                    " first: " +firstVisibleItemPosition +
                    " isLoading: " + isLoading +
                    " isLastPage: " + isLastPage);*/
            if (!isLoading && !isLastPage) {
                if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                    && firstVisibleItemPosition >= 0) {
                    loadMoreItems();
                }
            }
        }
    };
}
