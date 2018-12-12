package com.greenaddress.greenapi.model;

import android.util.SparseArray;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.greenaddress.gdk.GDKSession;
import com.greenaddress.greenapi.data.SettingsData;
import com.greenaddress.greenapi.data.TwoFactorConfigData;


public class Model {
    private SubaccountDataObservable mSubaccountDataObservable;
    private TwoFactorConfigDataObservable mTwoFactorConfigDataObservable;
    private EventDataObservable mEventDataObservable;
    private SparseArray<TransactionDataObservable> mTransactionDataObservables = new SparseArray<>();
    private SparseArray<TransactionDataObservable> mUTXODataObservables = new SparseArray<>();
    private SparseArray<ReceiveAddressObservable> mReceiveAddressObservables = new SparseArray<>();
    private SparseArray<BalanceDataObservable> mBalanceDataObservables = new SparseArray<>();
    private ActiveAccountObservable mActiveAccountObservable = new ActiveAccountObservable();
    private SettingsObservable mSettingsObservable = new SettingsObservable();
    private BlockchainHeightObservable blockchainHeightObservable = new BlockchainHeightObservable();
    private FeeObservable mFeeObservable;
    private AvailableCurrenciesObservable mAvailableCurrenciesObservable;
    private Boolean mTwoFAReset = false;

    private Model() {}

    public Model(final GDKSession session, final ListeningExecutorService executor) {
        mSubaccountDataObservable = new SubaccountDataObservable(session,
                                                                 executor, this);
        mEventDataObservable = new EventDataObservable(session);
        mTwoFactorConfigDataObservable = new TwoFactorConfigDataObservable(session, executor, mEventDataObservable);
        mFeeObservable = new FeeObservable(session, executor);
        mAvailableCurrenciesObservable = new AvailableCurrenciesObservable(session, executor);
    }

    public SubaccountDataObservable getSubaccountDataObservable() {
        return mSubaccountDataObservable;
    }

    public TwoFactorConfigDataObservable getTwoFactorConfigDataObservable() {
        return mTwoFactorConfigDataObservable;
    }

    public TransactionDataObservable getTransactionDataObservable(final Integer pointer) {
        return mTransactionDataObservables.get(pointer);
    }

    public TransactionDataObservable getUTXODataObservable(final Integer pointer) {
        return mUTXODataObservables.get(pointer);
    }

    public ReceiveAddressObservable getReceiveAddressObservable(final Integer pointer) {
        return mReceiveAddressObservables.get(pointer);
    }

    public BalanceDataObservable getBalanceDataObservable(final Integer pointer) {
        return mBalanceDataObservables.get(pointer);
    }

    public ActiveAccountObservable getActiveAccountObservable() {
        return mActiveAccountObservable;
    }

    public EventDataObservable getEventDataObservable() {
        return mEventDataObservable;
    }

    public BlockchainHeightObservable getBlockchainHeightObservable() {
        return blockchainHeightObservable;
    }

    public FeeObservable getFeeObservable() {
        return mFeeObservable;
    }

    public boolean isTwoFAReset() {
        return mTwoFAReset;
    }

    public void setTwoFAReset(boolean m2FAReset) {
        this.mTwoFAReset = m2FAReset;
    }

    public SparseArray<TransactionDataObservable> getTransactionDataObservables() {
        return mTransactionDataObservables;
    }

    public SparseArray<TransactionDataObservable> getUTXODataObservables() {
        return mUTXODataObservables;
    }

    public SparseArray<ReceiveAddressObservable> getReceiveAddressObservables() {
        return mReceiveAddressObservables;
    }

    public SparseArray<BalanceDataObservable> getBalanceDataObservables() {
        return mBalanceDataObservables;
    }

    public void fireBalances() {
        for (int i = 0, nsize = mBalanceDataObservables.size(); i < nsize; i++) {
            mBalanceDataObservables.valueAt(i).fire();
        }
    }

    public AvailableCurrenciesObservable getAvailableCurrenciesObservable() {
        return mAvailableCurrenciesObservable;
    }

    public TwoFactorConfigData getTwoFactorConfig() {
        return getTwoFactorConfigDataObservable().getTwoFactorConfigData();
    }

    public int getCurrentBlock() {
        return getBlockchainHeightObservable().getHeight();
    }

    public SettingsObservable getSettingsObservable() {
        return mSettingsObservable;
    }

    public SettingsData getSettings() {
        return mSettingsObservable.getSettings();
    }
}
