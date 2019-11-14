package com.greenaddress.greenapi.model;

import android.util.Log;
import android.util.SparseArray;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.greenaddress.greenapi.data.AssetInfoData;
import com.greenaddress.greenapi.data.BalanceData;
import com.greenaddress.greenapi.data.NetworkData;
import com.greenaddress.greenapi.data.SettingsData;
import com.greenaddress.greenapi.data.SubaccountData;
import com.greenaddress.greenapi.data.TwoFactorConfigData;
import com.greenaddress.greenbits.ui.UI;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static com.greenaddress.gdk.GDKSession.getSession;


public class Model {
    private SubaccountDataObservable mSubaccountDataObservable;
    private TwoFactorConfigDataObservable mTwoFactorConfigDataObservable;
    private EventDataObservable mEventDataObservable;
    private AssetsDataObservable mAssetsObservable;
    private SparseArray<TransactionDataObservable> mTransactionDataObservables = new SparseArray<>();
    private SparseArray<TransactionDataObservable> mUTXODataObservables = new SparseArray<>();
    private SparseArray<ReceiveAddressObservable> mReceiveAddressObservables = new SparseArray<>();
    private SparseArray<BalanceDataObservable> mBalanceDataObservables = new SparseArray<>();
    private ActiveAccountObservable mActiveAccountObservable = new ActiveAccountObservable();
    private SettingsObservable mSettingsObservable = new SettingsObservable();
    private BlockchainHeightObservable mBlockchainHeightObservable = new BlockchainHeightObservable();
    private ToastObservable mToastObservable = new ToastObservable();
    private ConnectionMessageObservable mConnMsgObservable = new ConnectionMessageObservable();

    private FeeObservable mFeeObservable;
    private AvailableCurrenciesObservable mAvailableCurrenciesObservable;
    private Boolean mTwoFAReset = false;
    private NetworkData mNetworkData;

    private Model() {}

    public Model(final ListeningExecutorService executor, final NetworkData networkData) {
        mAssetsObservable = new AssetsDataObservable(executor);
        mSubaccountDataObservable = new SubaccountDataObservable(executor, mAssetsObservable, this);
        mEventDataObservable = new EventDataObservable();
        mTwoFactorConfigDataObservable = new TwoFactorConfigDataObservable(executor, mEventDataObservable);
        mFeeObservable = new FeeObservable(executor);
        mAvailableCurrenciesObservable = new AvailableCurrenciesObservable(executor);
        mNetworkData = networkData;
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
        return mBlockchainHeightObservable;
    }

    public ToastObservable getToastObservable() {
        return mToastObservable;
    }

    public ConnectionMessageObservable getConnMsgObservable() {
        return mConnMsgObservable;
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

    public int getCurrentSubaccount() {
        return getActiveAccountObservable().getActiveAccount();
    }

    public Map<String, Long> getCurrentAccountBalanceData() {
        return getBalanceDataObservable(getCurrentSubaccount()).getBalanceData();
    }

    public AssetsDataObservable getAssetsObservable() {
        return mAssetsObservable;
    }


    public String getFiatCurrency() {
        return getSettings().getPricing().getCurrency();
    }

    public String getBitcoinOrLiquidUnit() {
        final int index = Math.max(UI.UNIT_KEYS_LIST.indexOf(getUnitKey()), 0);
        if (mNetworkData.getLiquid()) {
            return UI.LIQUID_UNITS[index];
        } else {
            return UI.UNITS[index];
        }
    }

    public String getUnitKey() {
        final String unit = getSettings().getUnit();
        return toUnitKey(unit);
    }

    public static String toUnitKey(final String unit) {
        if (!Arrays.asList(UI.UNITS).contains(unit))
            return UI.UNITS[0].toLowerCase(Locale.US);
        return unit.equals("\u00B5BTC") ? "ubtc" : unit.toLowerCase(Locale.US);
    }

    public String getValueString(final long amount, final boolean asFiat, boolean withUnit) {
        try {
            return getValueString(getSession().convertSatoshi(amount), asFiat, withUnit);
        } catch (final RuntimeException | IOException e) {
            Log.e("", "Conversion error: " + e.getLocalizedMessage());
            return "";
        }
    }
    public String getValueString(final ObjectNode amount, final boolean asFiat, boolean withUnit) {
        if (asFiat)
            return amount.get("fiat").asText() + (withUnit ? (" " + getFiatCurrency()) : "");
        return amount.get(getUnitKey()).asText() + (withUnit ? (" " + getBitcoinOrLiquidUnit()) : "");
    }

    public String getValueString(final long amount, final String asset, final AssetInfoData assetInfo,
                                 boolean withUnit) {
        try {
            final AssetInfoData assetInfoData = assetInfo != null ? assetInfo : new AssetInfoData(asset);
            final ObjectNode details = new ObjectMapper().createObjectNode();
            details.put("satoshi", amount);
            details.set("asset_info", assetInfoData.toObjectNode());
            final ObjectNode converted = getSession().convert(details);
            return converted.get(asset).asText() + (withUnit ? " " + assetInfoData.getTicker() : "");
        } catch (final RuntimeException | IOException e) {
            Log.e("", "Conversion error: " + e.getLocalizedMessage());
            return "";
        }
    }

    public SubaccountData getSubaccountData(final int subAccount) {
        return getSubaccountDataObservable().getSubaccountDataWithPointer(subAccount);
    }

    public String getAddress(final int subAccount) {
        return getReceiveAddressObservable(subAccount).getReceiveAddress();
    }

    public BalanceData getBalanceData(final int subAccount) {
        try {
            final long satoshi = getBalanceDataObservable(subAccount).getBtcBalanceData();
            return getSession().convertBalance(satoshi);
        } catch (final Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getReceivingId() {
        return getSubaccountData(0).getReceivingId();
    }
}
