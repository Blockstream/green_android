package com.greenaddress.gdk;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import com.blockstream.gdk.GreenWallet;
import com.blockstream.gdk.data.Network;
import com.blockstream.gdk.params.DeviceParams;
import com.blockstream.gdk.data.Device;
import com.blockstream.libgreenaddress.GDK;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.greenaddress.Bridge;
import com.greenaddress.greenapi.data.AssetInfoData;
import com.greenaddress.greenapi.data.BalanceData;
import com.greenaddress.greenapi.data.EstimatesData;
import com.greenaddress.greenapi.data.JSONData;
import com.greenaddress.greenapi.data.NetworkData;
import com.greenaddress.greenapi.data.TransactionData;
import com.greenaddress.greenapi.data.TwoFactorConfigData;
import com.greenaddress.greenapi.data.TwoFactorDetailData;
import com.greenaddress.greenapi.model.NotificationHandlerImpl;
import com.greenaddress.greenbits.ui.BuildConfig;
import com.greenaddress.jade.HttpRequestHandler;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import static com.blockstream.libgreenaddress.GDK.GA_ERROR;
import static com.blockstream.libgreenaddress.GDK.GA_RECONNECT;

public class GDKSession{

    // Fine to have a static objectMapper according to docs if using always same configuration
    private static final ObjectMapper mObjectMapper = new ObjectMapper();

    protected GreenWallet mGreenWallet;
    protected Object mNativeSession;
    private final NotificationHandlerImpl mNotification;

    static {
        mObjectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    protected GDKSession() {
        mNotification = new NotificationHandlerImpl();
    }

    public void initFromV4(GreenWallet greenWallet){
        mGreenWallet = greenWallet;
    }

    @Nullable
    public Object getNativeSession(){
        return mNativeSession;
    }

    public void disconnect() throws Exception {
        mNotification.reset();
    }

    public GDKTwoFactorCall login(final Device device) throws Exception {
        return new GDKTwoFactorCall(GDK.login_user(mNativeSession, new DeviceParams(device), null));
    }

    public void setWatchOnly(final String username, final String password) throws Exception {
        GDK.set_watch_only(mNativeSession, username, password);
    }

    public GDKTwoFactorCall getSubAccount(final long subAccount) {
        return new GDKTwoFactorCall(GDK.get_subaccount(mNativeSession, subAccount));
    }

    public GDKTwoFactorCall getBalance(final Integer subAccount, final long confirmations) {
        final ObjectNode details = mObjectMapper.createObjectNode();
        details.put("subaccount", subAccount);
        details.put("num_confs", confirmations);
        return new GDKTwoFactorCall( GDK.get_balance(mNativeSession, details));
    }

    public BalanceData convertBalance(final BalanceData balanceData) throws Exception {
        final ObjectNode convertedBalanceData = Bridge.INSTANCE.toJackson(GDK.convert_amount(mNativeSession, balanceData));
        if (balanceData.getAssetInfo() != null)
            convertedBalanceData.set("asset_info", balanceData.getAssetInfo().toObjectNode());
        final BalanceData balanceData1 = BalanceData.from(mObjectMapper, convertedBalanceData);
        debugEqual(convertedBalanceData, balanceData1);
        return balanceData1;
    }

    public BalanceData convertBalance(final long satoshi) throws Exception {
        final ObjectNode convertedBalanceData = convertSatoshi(satoshi);
        final BalanceData balanceData = BalanceData.from(mObjectMapper, convertedBalanceData);
        return balanceData;
    }

    public ObjectNode convert(final ObjectNode amount) throws Exception {
        return Bridge.INSTANCE.toJackson(GDK.convert_amount(mNativeSession, amount));
    }

    public ObjectNode convertSatoshi(final long satoshi) throws Exception {
        final ObjectNode amount = mObjectMapper.createObjectNode();
        amount.set("satoshi", new LongNode(satoshi));
        return convert(amount);
    }

    public GDKTwoFactorCall createTransactionRaw(final ObjectNode tx) throws Exception {
        return new GDKTwoFactorCall(GDK.create_transaction(mNativeSession, tx));
    }

    public GDKTwoFactorCall signTransactionRaw(final ObjectNode createTransactionData) throws Exception {
        return new GDKTwoFactorCall(GDK.sign_transaction(mNativeSession, createTransactionData));
    }

    public GDKTwoFactorCall sendTransactionRaw(final Activity parent, final ObjectNode txDetails) throws Exception {
        final Object twoFactorCall = GDK.send_transaction(mNativeSession, txDetails);
        final GDKTwoFactorCall gdkTwoFactorCall = new GDKTwoFactorCall(twoFactorCall);
        return gdkTwoFactorCall;
    }

    public String broadcastTransactionRaw(final String txDetails) throws Exception {
        return GDK.broadcast_transaction(mNativeSession, txDetails);
    }

    public List<Long> getFeeEstimates() throws Exception {
        final ObjectNode feeEstimates = Bridge.INSTANCE.toJackson(GDK.get_fee_estimates(mNativeSession));
        return mObjectMapper.treeToValue(feeEstimates, EstimatesData.class).getFees();
    }

    private List<NetworkData> cachedNetworks = null;
    public List<NetworkData> getNetworks() {
        if(cachedNetworks == null) {
            ArrayList<NetworkData> list = new ArrayList<>();
            Map<String, Network> networks = mGreenWallet.getNetworks().getNetworks();

            for (Map.Entry<String, Network> networkEntry : networks.entrySet()) {
                try {
                    list.add(mObjectMapper.treeToValue(mObjectMapper.readTree(networkEntry.getValue().toString()), NetworkData.class));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            cachedNetworks = list;
        }
        return cachedNetworks;
    }

    @Nullable
    public ObjectNode getGDKSettings() throws Exception  {
        if(mNativeSession == null) return null;
        return Bridge.INSTANCE.toJackson(GDK.get_settings(mNativeSession));
    }

    protected static void debugEqual(final ObjectNode jsonNode, final JSONData data)  {
        if(!jsonNode.toString().equals(data.toString())) {
            final Class<? extends JSONData> aClass = data.getClass();
            Log.d("DBGEQ", "jsonData type " + aClass);
            Log.d("DBGEQ", "jsonNode " + jsonNode);
            Log.d("DBGEQ", "jsonData " + data);
        }
    }

    public NotificationHandlerImpl getNotificationModel() {
        return mNotification;
    }

    public GDKTwoFactorCall changeSettings(final ObjectNode setting) throws Exception  {
        return new GDKTwoFactorCall(GDK.change_settings(mNativeSession, setting));
    }
}
