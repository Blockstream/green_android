package com.greenaddress.greenapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.greenaddress.Bridge;
import com.greenaddress.gdk.GDKSession;
import com.greenaddress.gdk.GDKTwoFactorCall;
import com.greenaddress.greenapi.data.NetworkData;
import com.greenaddress.greenapi.data.SettingsData;
import com.greenaddress.greenapi.data.SubaccountData;
import com.greenaddress.greenbits.ui.GaActivity;
import com.greenaddress.greenbits.wallets.HardwareCodeResolver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

public class Session extends GDKSession {
    private static final ObjectMapper mObjectMapper = new ObjectMapper();
    private static Session instance = new Session();

    private SettingsData mSettings;
    private String mNetwork;
    private ObjectNode pendingTransaction = null;

    private Session() {
        super();
    }

    public static Session getSession() {
        return instance;
    }

    public void bridgeSession(Object session, String network){
        mNativeSession = session;
        mNetwork = network;

        // Reset
        mSettings = null;

        getNotificationModel().reset();
    }

    public Registry getRegistry() {
        return Registry.getInstance();
    }

    public boolean isWatchOnly() {
        return false;
    }

    public HWWallet getHWWallet() {
        return Bridge.INSTANCE.getHWWallet();
    }

    public void setNetwork(final String network) {
        mNetwork = network;
    }

    public void disconnect() throws Exception {
        super.disconnect();

        mSettings = null;
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
        final ObjectNode account = call.resolve( new HardwareCodeResolver(activity, getHWWallet()), null);
        final SubaccountData subAccount = mObjectMapper.readValue(account.toString(), SubaccountData.class);

        // GDK 0.44 removed balance info from SubAccount json. v4 should request balance only when needed
        subAccount.setSatoshi(getBalance(activity, (int) subaccount));

        return subAccount;
    }

    public Map<String, Long> getBalance(final GaActivity activity, final Integer subaccount) throws Exception {
        final GDKTwoFactorCall call = getBalance(subaccount, 0);
        final ObjectNode balanceData = call.resolve(new HardwareCodeResolver(activity, getHWWallet()), null);
        final Map<String, Long> map = new HashMap<>();
        final Iterator<String> iterator = balanceData.fieldNames();
        while (iterator.hasNext()) {
            final String key = iterator.next();
            map.put(key, balanceData.get(key).asLong(0));
        }
        return map;
    }

    public List<Long> getFees() {
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

    public void setSettings(final SettingsData settings) {
        mSettings = settings;
    }

    public void setPendingTransaction(@Nullable ObjectNode pendingTransaction){
        this.pendingTransaction = pendingTransaction;
    }

    @Nullable
    public ObjectNode getPendingTransaction(){
        return this.pendingTransaction;
    }
}
