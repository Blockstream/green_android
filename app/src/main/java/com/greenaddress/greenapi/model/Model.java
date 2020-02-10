package com.greenaddress.greenapi.model;

import android.util.SparseArray;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.greenaddress.gdk.CodeResolver;
import com.greenaddress.greenapi.ConnectionManager;
import com.greenaddress.greenapi.data.AssetInfoData;
import com.greenaddress.greenapi.data.BalanceData;
import com.greenaddress.greenapi.data.NetworkData;
import com.greenaddress.greenapi.data.SettingsData;
import com.greenaddress.greenapi.data.SubaccountData;
import com.greenaddress.greenapi.data.TwoFactorConfigData;
import com.greenaddress.greenbits.ui.UI;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

import static com.greenaddress.gdk.GDKSession.getSession;


public class Model {
    private SubaccountsDataObservable mSubaccountsDataObservable;
    private TwoFactorConfigDataObservable mTwoFactorConfigDataObservable;
    private EventDataObservable mEventDataObservable;
    private AssetsDataObservable mAssetsObservable;
    private SparseArray<TransactionDataObservable> mTransactionDataObservables = new SparseArray<>();
    private SparseArray<TransactionDataObservable> mUTXODataObservables = new SparseArray<>();
    private SparseArray<ReceiveAddressObservable> mReceiveAddressObservables = new SparseArray<>();
    private SparseArray<BalanceDataObservable> mBalanceDataObservables = new SparseArray<>();
    private ActiveAccountObservable mActiveAccountObservable = new ActiveAccountObservable();
    private SettingsObservable mSettingsObservable;
    private BlockchainHeightObservable mBlockchainHeightObservable = new BlockchainHeightObservable();
    private ToastObservable mToastObservable = new ToastObservable();
    private ConnectionMessageObservable mConnMsgObservable = new ConnectionMessageObservable();

    private FeeObservable mFeeObservable;
    private AvailableCurrenciesObservable mAvailableCurrenciesObservable;
    private Boolean mTwoFAReset = false;
    private NetworkData mNetworkData;

    private Model() {}

    public Model(final ListeningExecutorService executor, final CodeResolver codeResolver,
                 final NetworkData networkData) {
        mAssetsObservable = new AssetsDataObservable(executor);
        mSubaccountsDataObservable =
            new SubaccountsDataObservable(executor, mAssetsObservable, this, codeResolver);
        mEventDataObservable = new EventDataObservable();
        mTwoFactorConfigDataObservable = new TwoFactorConfigDataObservable(executor, mEventDataObservable);
        mFeeObservable = new FeeObservable(executor);
        mAvailableCurrenciesObservable = new AvailableCurrenciesObservable(executor);
        mSettingsObservable= new SettingsObservable(executor);
        mNetworkData = networkData;
    }

    public SubaccountsDataObservable getSubaccountsDataObservable() {
        return mSubaccountsDataObservable;
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

    public String getFiat(final long satoshi, final boolean withUnit) throws Exception {
        return getFiat(getSession().convertBalance(satoshi), withUnit);
    }

    public String getBtc(final long satoshi, final boolean withUnit) throws Exception {
        return getBtc(getSession().convertBalance(satoshi), withUnit);
    }

    public String getAsset(final long satoshi,  final String asset, final AssetInfoData assetInfo,
                           final boolean withUnit) throws Exception {
        final AssetInfoData assetInfoData = assetInfo != null ? assetInfo : new AssetInfoData(asset);
        final BalanceData balance = new BalanceData();
        balance.setSatoshi(satoshi);
        balance.setAssetInfo(assetInfoData);
        final BalanceData converted = getSession().convertBalance(balance);
        return getAsset(converted, withUnit);
    }

    public String getFiat(final BalanceData balanceData, final boolean withUnit) {
        try {
            final Double number = Double.parseDouble(balanceData.getFiat());
            return getNumberFormat(2).format(number) + (withUnit ? " " + getFiatCurrency() : "");
        } catch (final Exception e) {
            return "N.A." + (withUnit ? " " + getFiatCurrency() : "");
        }
    }

    public String getBtc(final BalanceData balanceData, final boolean withUnit) {
        final String converted = balanceData.toObjectNode().get(getUnitKey()).asText();
        final Double number = Double.parseDouble(converted);
        return getNumberFormat().format(number) + (withUnit ? " " + getBitcoinOrLiquidUnit() : "");
    }

    public NumberFormat getNumberFormat() {
        switch (getUnitKey()) {
        case "btc":
            return getNumberFormat(8);
        case "mbtc":
            return getNumberFormat(5);
        case "ubtc":
        case "bits":
            return getNumberFormat(2);
        default:
            return getNumberFormat(0);
        }
    }

    public static NumberFormat getNumberFormat(final int decimals) {
        return getNumberFormat(decimals, Locale.getDefault());
    }

    public static NumberFormat getNumberFormat(final int decimals, final Locale locale) {
        final NumberFormat instance = NumberFormat.getInstance(locale);
        instance.setMinimumFractionDigits(decimals);
        instance.setMaximumFractionDigits(decimals);
        return instance;
    }

    public String getAsset(final BalanceData balanceData, final boolean withUnit) {
        final AssetInfoData info = balanceData.getAssetInfo();
        final Double number = Double.parseDouble(balanceData.getAssetValue());
        final String ticker = info.getTicker() != null ? info.getTicker() : "";
        return getNumberFormat(info.getPrecision()).format(number) + (withUnit ? " " + ticker : "");
    }

    public SubaccountData getSubaccountsData(final int subAccount) {
        return getSubaccountsDataObservable().getSubaccountsDataWithPointer(subAccount);
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

    public String getReceivingId(final int subAccount) {
        return getSubaccountsData(subAccount).getReceivingId();
    }
}
