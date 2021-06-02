package com.greenaddress.greenapi;

import android.app.Activity;

import com.blockstream.gdk.data.Network;
import com.blockstream.gdk.data.TwoFactorReset;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.greenaddress.Bridge;
import com.greenaddress.gdk.GDKSession;
import com.greenaddress.gdk.GDKTwoFactorCall;
import com.greenaddress.greenapi.data.EventData;
import com.greenaddress.greenapi.data.NetworkData;
import com.greenaddress.greenapi.data.SettingsData;
import com.greenaddress.greenapi.data.SubaccountData;
import com.greenaddress.greenapi.data.TransactionData;
import com.greenaddress.greenbits.ui.GaActivity;
import com.greenaddress.greenbits.wallets.HardwareCodeResolver;
import com.greenaddress.jade.HttpRequestHandler;
import com.greenaddress.jade.HttpRequestProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Session extends GDKSession implements HttpRequestProvider {
    private static final ObjectMapper mObjectMapper = new ObjectMapper();
    private static Session instance = new Session();

    private Registry mRegistry;
    private String mWatchOnlyUsername;
    private SettingsData mSettings;
    private TwoFactorReset mTwoFAReset = null;
    private String mNetwork;

    private Session() {
        super();
    }

    public static Session getSession() {
        return instance;
    }

    public void bridgeSession(Object session, String network, String watchOnlyUsername){
        mNativeSession = session;
        mNetwork = network;
        mWatchOnlyUsername = watchOnlyUsername;

        // Reset
        mSettings = null;
        mTwoFAReset = null;
        mSubAccount = 0;

        getNotificationModel().reset();
    }

    public Registry getRegistry() {
        return Registry.getInstance();
    }

    public boolean isWatchOnly() {
        return null != mWatchOnlyUsername;
    }

    public HWWallet getHWWallet() {
        return Bridge.INSTANCE.getHWWallet();
    }

    public TwoFactorReset getTwoFAReset() {
        return this.mTwoFAReset;
    }

    public void setTwoFAReset(final TwoFactorReset eventData) {
        this.mTwoFAReset = eventData;
    }

    public boolean isTwoFAReset() {
        return mTwoFAReset != null;
    }

    public void setNetwork(final String network) {
        mNetwork = network;
    }

    public void disconnect() throws Exception {
        super.disconnect();

        mWatchOnlyUsername = null;
        mSettings = null;
        mTwoFAReset = null;
    }

    public SettingsData refreshSettings() {
        try {
            final ObjectNode settings = getGDKSettings();
            final SettingsData settingsData = mObjectMapper.convertValue(settings, SettingsData.class);
            mSettings = settingsData;
            return settingsData;
        } catch (final Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public SettingsData getSettings() {
        if (mSettings != null)
            return mSettings;
        return refreshSettings();
    }

    public SubaccountData getSubAccount(final GaActivity activity, final long subaccount) throws Exception {
        final GDKTwoFactorCall call = getSubAccount(subaccount);
        final ObjectNode account = call.resolve(null, new HardwareCodeResolver(activity, getHWWallet()));
        final SubaccountData subAccount = mObjectMapper.readValue(account.toString(), SubaccountData.class);
        return subAccount;
    }

    public Map<String, Long> getBalance(final GaActivity activity, final Integer subaccount) throws Exception {
        final GDKTwoFactorCall call = getBalance(subaccount, 0);
        final ObjectNode balanceData = call.resolve(null, new HardwareCodeResolver(activity, getHWWallet()));
        final Map<String, Long> map = new HashMap<>();
        final Iterator<String> iterator = balanceData.fieldNames();
        while (iterator.hasNext()) {
            final String key = iterator.next();
            map.put(key, balanceData.get(key).asLong(0));
        }
        return map;
    }

    public List<TransactionData> getTransactions(final GaActivity activity, final int subaccount, final int first, final int size) throws Exception {
        final GDKTwoFactorCall call =
                getTransactionsRaw(subaccount, first, size);
        final ObjectNode txListObject = call.resolve(null, new HardwareCodeResolver(activity, getHWWallet()));
        final List<TransactionData> transactions =
                parseTransactions((ArrayNode) txListObject.get("transactions"));
        return transactions;
    }

    public List<SubaccountData> getSubAccounts(final GaActivity activity) throws Exception {
        final GDKTwoFactorCall call = getSubAccounts();
        final ObjectNode accounts = call.resolve(null, new HardwareCodeResolver(activity, getHWWallet()));
        final List<SubaccountData> subAccounts =
                mObjectMapper.readValue(mObjectMapper.treeAsTokens(accounts.get("subaccounts")),
                        new TypeReference<List<SubaccountData>>() {});
        return subAccounts;
    }

    public List<Long> getFees() {
        if (!getNotificationModel().getFees().isEmpty())
            return getNotificationModel().getFees();
        try {
            return getFeeEstimates();
        } catch (final Exception e) {
            return new ArrayList<Long>(0);
        }
    }

    public NetworkData getNetworkData() {
        final List<NetworkData> networks = getNetworks();
        for (final NetworkData n : networks) {
            if (n.getNetwork().equals(mNetwork)) {
                return n;
            }
        }
        return null;
    }

    public GDKTwoFactorCall createTransactionFromUri(final Activity parent, final String uri, final int subaccount) throws Exception {
        NetworkData network = getNetworkData();
        String assetId = null;
        if(network.getLiquid()){
            assetId = network.getPolicyAsset();
        }
        return createTransactionFromUri(parent, uri, assetId,subaccount);
    }

    public void setSettings(final SettingsData settings) {
        mSettings = settings;
    }

    @Override
    public HttpRequestHandler getHttpRequest() {
        return this;
    }
}
