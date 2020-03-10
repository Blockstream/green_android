package com.greenaddress.greenapi.model;

import android.util.Log;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.ListeningExecutorService;

import com.greenaddress.gdk.CodeResolver;
import com.greenaddress.gdk.GDKTwoFactorCall;
import com.greenaddress.greenapi.ConnectionManager;
import com.greenaddress.greenapi.data.TransactionData;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import static com.greenaddress.gdk.GDKSession.getSession;

public class TransactionDataObservable extends Observable implements Observer {
    private static final ObjectMapper mObjectMapper = new ObjectMapper();

    private List<TransactionData> mTransactionDataList = new ArrayList<>();
    private ListeningExecutorService mExecutor;
    private CodeResolver mCodeResolver;
    private Integer mSubaccount;
    private boolean mUTXOOnly;
    private Integer mPageLoaded = 0;
    private Integer mLastPage = Integer.MAX_VALUE;
    private boolean mExecutedOnce = false;

    private static final int TX_PER_PAGE = 15;

    public TransactionDataObservable(final ListeningExecutorService executor,
                                     final CodeResolver codeResolver,
                                     final AssetsDataObservable assetsDataObservable,
                                     final Integer subaccount,
                                     final boolean UTXOOnly) {
        mExecutor = executor;
        mCodeResolver = codeResolver;
        mSubaccount = subaccount;
        mUTXOOnly = UTXOOnly;
        // this is not initialized by default but by visiting the account detail page

        assetsDataObservable.addObserver((observable, o) -> this.refresh(true));
    }

    public void refresh() {
        refresh(true);
    }

    public void refresh(final boolean reset) {
        if (reset) {
            mPageLoaded = 0;
            mLastPage = Integer.MAX_VALUE;
            mTransactionDataList.clear();
        }
        mExecutor.submit(this::refreshSync);
    }

    public synchronized void refreshSync() {
        try {
            List<TransactionData> transactions;
            if (mUTXOOnly) {
                final GDKTwoFactorCall call = getSession().getUTXO(mSubaccount, 0);
                JsonNode unspentOutputs = call.resolve(null, mCodeResolver).get("unspent_outputs");

                transactions = new ArrayList<>();
                if (unspentOutputs.has("btc")) {
                    // at the moment the returned json is different if calling get_unspent_outputs or
                    // get_transactions, since get_transactions is already updated for supporting assets and
                    // we don't need satoshi amount, this is an hack to not fail json parsing
                    ArrayNode arrayNode = (ArrayNode) unspentOutputs.get("btc");
                    for (int i = 0; i < arrayNode.size(); i++) {
                        ((ObjectNode) arrayNode.get(i)).remove("satoshi");
                    }
                    transactions = mObjectMapper.readValue(mObjectMapper.treeAsTokens(arrayNode),
                                                           new TypeReference<List<TransactionData>>() {});
                }
            } else {
                final GDKTwoFactorCall call = getSession().getTransactionsRaw(mSubaccount,
                                                                              mPageLoaded * TX_PER_PAGE, TX_PER_PAGE);
                ObjectNode txListObject = call.resolve(null, mCodeResolver);
                transactions = getSession().parseTransactions((ArrayNode) txListObject.get("transactions"));
                if (transactions.size() < TX_PER_PAGE)
                    mLastPage = mPageLoaded;
                mPageLoaded++;
                Log.d("OBS", "page loaded " + mPageLoaded + " " + transactions.size() + " Loaded txs");
            }
            mExecutedOnce = true;
            setTransactionDataList(transactions);
        } catch (Exception e) {
            Log.e("OBS", e.getMessage());
            e.printStackTrace();
        }
    }

    public List<TransactionData> getTransactionDataList() {
        return mTransactionDataList;
    }

    public Integer getSubaccount() {
        return mSubaccount;
    }

    public void setTransactionDataList(final List<TransactionData> transactionData) {
        Log.d("OBS",
              "pageLoaded:" + mPageLoaded + " set" + (mUTXOOnly ? "UTXO" : "Transaction") + "DataList(" +  mSubaccount + ", " + transactionData +
              ")");
        this.mTransactionDataList.addAll(transactionData);
        fire();
    }

    @Override
    public void update(final Observable observable, final Object o) {
        if (observable instanceof ActiveAccountObservable) {
            if ( ((ActiveAccountObservable) observable).getActiveAccount().equals(mSubaccount))
                if (!isExecutedOnce())
                    refresh();

        } else if (observable instanceof BlockchainHeightObservable) {
            if (isExecutedOnce()) {
                refresh();
            }
        }
    }

    public void fire() {
        setChanged();
        notifyObservers();
    }

    public boolean isLastPage() {
        return (mPageLoaded >= mLastPage);
    }

    public boolean isExecutedOnce() {
        return mExecutedOnce;
    }
}
