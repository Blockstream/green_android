package com.greenaddress.greenbits.ui.transactions;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.greenaddress.greenapi.data.BalanceData;
import com.greenaddress.greenapi.data.SubaccountData;
import com.greenaddress.greenapi.data.TransactionData;
import com.greenaddress.greenapi.model.ActiveAccountObservable;
import com.greenaddress.greenapi.model.BalanceDataObservable;
import com.greenaddress.greenapi.model.ReceiveAddressObservable;
import com.greenaddress.greenapi.model.TransactionDataObservable;
import com.greenaddress.greenbits.GaService;
import com.greenaddress.greenbits.GreenAddressApplication;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.SubaccountFragment;
import com.greenaddress.greenbits.ui.UI;
import com.greenaddress.greenbits.ui.accounts.AccountView;
import com.greenaddress.greenbits.ui.accounts.SubaccountSelectActivity;
import com.greenaddress.greenbits.ui.accounts.SwitchNetworkFragment;
import com.greenaddress.greenbits.ui.assets.AssetsSelectActivity;
import com.greenaddress.greenbits.ui.components.BottomOffsetDecoration;
import com.greenaddress.greenbits.ui.components.DividerItem;
import com.greenaddress.greenbits.ui.components.OnGdkListener;
import com.greenaddress.greenbits.ui.preferences.PrefKeys;
import com.greenaddress.greenbits.ui.receive.ReceiveActivity;
import com.greenaddress.greenbits.ui.send.ScanActivity;

import org.bitcoinj.core.Sha256Hash;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observer;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

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
    private TextView mAssetsSelection;
    private TextView mSwitchNetwork;

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        Log.d(TAG, "onCreateView -> " + TAG);
        if (isZombieNoView())
            return null;

        mView = inflater.inflate(R.layout.fragment_main, container, false);

        // Setup recycler & adapter
        final RecyclerView txView = UI.find(mView, R.id.mainTransactionList);
        txView.setHasFixedSize(true);
        txView.addItemDecoration(new DividerItem(getActivity()));
        txView.setLayoutManager(mLayoutManager);
        float offsetPx = getResources().getDimension(R.dimen.adapter_bar);
        final BottomOffsetDecoration bottomOffsetDecoration = new BottomOffsetDecoration((int) offsetPx);
        txView.addItemDecoration(bottomOffsetDecoration);
        final GreenAddressApplication app = (GreenAddressApplication) getActivity().getApplication();
        mTransactionsAdapter = new ListTransactionsAdapter(getGaActivity(), app.getService(),
                                                           getNetwork(),  mTxItems, getModel());
        txView.setAdapter(mTransactionsAdapter);
        txView.addOnScrollListener(recyclerViewOnScrollListener);
        // FIXME, more efficient to use swap
        // txView.swapAdapter(lta, false);
        mSwipeRefreshLayout = UI.find(mView, R.id.mainTransactionListSwipe);
        mSwipeRefreshLayout.setColorSchemeColors(ContextCompat.getColor(getContext(), R.color.accent));
        mSwipeRefreshLayout.setOnRefreshListener(() -> {
            Log.d(TAG, "onRefresh -> " + TAG);
            final int subaccount = getModel().getCurrentSubaccount();
            getModel().getTransactionDataObservable(subaccount).refresh();
            updateAssetSelection();
        });

        // Setup account card view
        mAccountView = UI.find(mView, R.id.accountView);
        mAccountView.listMode(true);
        mAccountView.setOnClickListener(this);
        if (getModel().isTwoFAReset())
            mAccountView.hideActions();
        else
            mAccountView.showActions(getConnectionManager().isWatchOnly());

        mAssetsSelection = UI.find(mView, R.id.assetsSelection);
        mAssetsSelection.setOnClickListener(v -> startActivityForResult(new Intent(getGaActivity(),
                                                                                   AssetsSelectActivity.class),
                                                                        REQUEST_SELECT_ASSET));
        mSwitchNetwork = UI.find(mView, R.id.switchNetwork);
        mSwitchNetwork.setOnClickListener(v -> showDialog());

        mSwitchNetwork.setText(getNetwork().getName());
        mSwitchNetwork.setTextColor(getResources().getColor(R.color.white));

        final Drawable arrow = getContext().getResources().getDrawable(R.drawable.ic_expand_more_24dp);
        mSwitchNetwork.setCompoundDrawablesWithIntrinsicBounds(null, null, arrow, null);

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
            updateAssetSelection();
        }
    }

    private void updateAssetSelection() {
        try {
            Log.d(TAG, "updateAssetSelection");
            if (getNetwork().getLiquid()) {
                final int size = getModel().getCurrentAccountBalanceData().size();
                mAssetsSelection.setText(size == 1 ?
                                         getString(R.string.id_d_asset_in_this_account, size) :
                                         getString(R.string.id_d_assets_in_this_account, size));
            } else {
                mAssetsSelection.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            e.printStackTrace();
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
        final GreenAddressApplication app = (GreenAddressApplication) getActivity().getApplication();
        final GaService service = app.getService();
        final boolean isSPVEnabled = service.isSPVEnabled();

        for (final TransactionItem txItem : mTxItems)
            txItem.spvVerified = !isSPVEnabled || service.isSPVVerified(txItem.txHash);

        final RecyclerView txView = UI.find(mView, R.id.mainTransactionList);
        txView.getAdapter().notifyDataSetChanged();
    }

    private void showDialog() {

        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.addToBackStack(null);

        // Create and show the dialog.
        DialogFragment newFragment = SwitchNetworkFragment.newInstance();
        newFragment.show(ft, "dialog");
    }

    private void showTxView(final boolean doShowTxList) {
        UI.find(mView, R.id.loadingList).setVisibility(View.GONE);
        UI.showIf(doShowTxList, UI.find(mView, R.id.mainTransactionList));
        UI.showIf(!doShowTxList, UI.find(mView, R.id.emptyListText));
    }

    private void reloadTransactions(final List<TransactionData> txList, final boolean showWaitDialog) {
        final int subaccount = getModel().getCurrentSubaccount();
        final RecyclerView txView;

        if (isZombie())
            return;

        // Mark ourselves as clean before fetching. This means that while the callback
        // is running, we may be marked dirty again if a new block arrives, which
        // is required to avoid missing updates while the RPC is in flight.
        setIsDirty(false);

        txView = UI.find(mView, R.id.mainTransactionList);
        final GreenAddressApplication app = (GreenAddressApplication) getActivity().getApplication();

        if (replacedTxs == null)
            replacedTxs = new HashMap<>();

        try {
            final int currentBlock = getModel().getBlockchainHeightObservable().getHeight();

            if (mSwipeRefreshLayout != null)
                mSwipeRefreshLayout.setRefreshing(false);

            showTxView(!txList.isEmpty());

            final Sha256Hash oldTop = !mTxItems.isEmpty() ? mTxItems.get(0).txHash : null;
            mTxItems.clear();
            replacedTxs.clear();

            for (final TransactionData tx : txList) {
                try {
                    mTxItems.add(new TransactionItem(app.getService(), tx, currentBlock, subaccount, getNetwork(),
                                                     getModel()));
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

            if (getNetwork().getLiquid() && getModel().getCurrentAccountBalanceData().get("btc") == 0L) {
                UI.popup(getGaActivity(), R.string.id_warning, R.string.id_receive, R.string.id_cancel)
                .content(R.string.id_insufficient_lbtc_to_send_a)
                .onPositive((dialog, which) -> {
                    final Intent intent = new Intent(getActivity(), ReceiveActivity.class);
                    getActivity().startActivity(intent);
                })
                .build().show();
            } else {
                final Intent intent = new Intent(getActivity(), ScanActivity.class);
                intent.putExtra(PrefKeys.SWEEP, getConnectionManager().isWatchOnly());
                getActivity().startActivity(intent);
            }
        } else if (view.getId() == R.id.selectSubaccount) {
            final Intent intent = new Intent(getActivity(), SubaccountSelectActivity.class);
            intent.setFlags(FLAG_ACTIVITY_NO_ANIMATION);
            getActivity().startActivity(intent);
        }
    }

    @Override
    public void onUpdateBalance(final BalanceDataObservable observable) {
        Log.d(TAG, "Updating balance");
        final Long satoshi = observable.getBtcBalanceData();
        if (isZombie() || satoshi == null)
            return;

        getGaActivity().runOnUiThread(() -> {
            final int subaccount = getModel().getCurrentSubaccount();
            final SubaccountData subaccountData = getModel().getSubaccountData(subaccount);
            mAccountView.setTitle(subaccountData.getNameWithDefault(getString(R.string.id_main_account)));
            mAccountView.setBalance(getModel(), satoshi);
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
        updateAssetSelection();
    }

    @Override
    public void onUpdateActiveSubaccount(final ActiveAccountObservable observable) {}

    private boolean isLoading = false;
    private boolean isLastPage = false;

    private void loadMoreItems() {
        Log.d(TAG, "loadMoreItems");
        isLoading=true;
        final int subaccount = getModel().getCurrentSubaccount();
        getModel().getTransactionDataObservable(subaccount).refresh(false);
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
