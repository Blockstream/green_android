package com.greenaddress.greenapi.model;

import android.util.Log;
import android.util.SparseArray;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.greenaddress.greenapi.data.SubaccountData;

import java.util.List;
import java.util.Observable;

import static com.greenaddress.gdk.GDKSession.getSession;

public class SubaccountsDataObservable extends Observable {
    private List<SubaccountData> mSubaccountsData;
    private ListeningExecutorService mExecutor;
    private SparseArray<TransactionDataObservable> mTransactionDataObservables;
    private SparseArray<TransactionDataObservable> mUTXODataObservables;
    private SparseArray<ReceiveAddressObservable> mReceiveAddressObservables;
    private SparseArray<BalanceDataObservable> mBalanceDataObservables;
    private ActiveAccountObservable mActiveAccountObservable;
    private BlockchainHeightObservable mBlockchainHeightObservable;

    private AssetsDataObservable mAssetsDataObservable;

    SubaccountsDataObservable(final ListeningExecutorService executor,
                              AssetsDataObservable assetsDataObservable, final Model model) {
        mExecutor = executor;
        mTransactionDataObservables = model.getTransactionDataObservables();
        mBalanceDataObservables = model.getBalanceDataObservables();
        mReceiveAddressObservables = model.getReceiveAddressObservables();
        mActiveAccountObservable = model.getActiveAccountObservable();
        mBlockchainHeightObservable = model.getBlockchainHeightObservable();
        mUTXODataObservables = model.getUTXODataObservables();

        mAssetsDataObservable = assetsDataObservable;

        refresh();
    }

    public void refresh() {
        // this call is syncronous, because other observables depends on this to be initialized
        try {
            final long millis=System.currentTimeMillis();
            final List<SubaccountData> subAccounts = getSession().getSubAccounts();
            for (SubaccountData subAccount : subAccounts) {
                final int pointer = subAccount.getPointer();
                initObservables(pointer);
            }
            Log.d("OBS", "setSubaccountData init took " + (System.currentTimeMillis()-millis) +"ms");
            setSubaccountsData(subAccounts);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initObservables(int pointer) {
        if (mTransactionDataObservables.get(pointer) == null) {
            final TransactionDataObservable transactionDataObservable = new TransactionDataObservable(mExecutor,
                                                                                                      mAssetsDataObservable,
                                                                                                      pointer, false);
            mActiveAccountObservable.addObserver(transactionDataObservable);
            mBlockchainHeightObservable.addObserver(transactionDataObservable);
            mTransactionDataObservables.put(pointer, transactionDataObservable);
        }
        if (mBalanceDataObservables.get(pointer) == null) {
            final BalanceDataObservable balanceDataObservable = new BalanceDataObservable(mExecutor,
                                                                                          mAssetsDataObservable,
                                                                                          pointer);
            mBlockchainHeightObservable.addObserver(balanceDataObservable);
            mBalanceDataObservables.put(pointer, balanceDataObservable);
        }
        if (mReceiveAddressObservables.get(pointer) == null) {
            final ReceiveAddressObservable addressObservable =
                new ReceiveAddressObservable(mExecutor, pointer);
            mReceiveAddressObservables.put(pointer, addressObservable);
            mActiveAccountObservable.addObserver(addressObservable);
        }
        if (mUTXODataObservables.get(pointer) == null) {
            final TransactionDataObservable utxoDataObservable = new TransactionDataObservable(mExecutor,
                                                                                               mAssetsDataObservable,
                                                                                               pointer, true);
            mBlockchainHeightObservable.addObserver(utxoDataObservable);
            mUTXODataObservables.put(pointer, utxoDataObservable);
        }
    }


    public List<SubaccountData> getSubaccountsDataList() {
        return mSubaccountsData;
    }

    public SubaccountData getSubaccountsDataWithPointer(final Integer pointer) {
        for (final SubaccountData subaccountData : mSubaccountsData) {
            if (subaccountData.getPointer().equals(pointer))
                return subaccountData;
        }
        return null;
    }

    private void setSubaccountsData(final List<SubaccountData> subaccountsData) {
        Log.d("OBS", "setSubaccountData(" + subaccountsData +")");
        this.mSubaccountsData = subaccountsData;
        setChanged();
        notifyObservers();
    }

    public void add(final SubaccountData newSubAccount) {
        mSubaccountsData.add(newSubAccount);
        final Integer newPointer = newSubAccount.getPointer();
        initObservables(newPointer);
        mReceiveAddressObservables.get(newPointer).refresh();
        mBalanceDataObservables.get(newPointer).refresh();
        setSubaccountsData(mSubaccountsData);
    }
}
