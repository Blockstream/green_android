package com.greenaddress.gdk;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;

import com.blockstream.libgreenaddress.GDK;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.greenaddress.greenapi.data.AssetInfoData;
import com.greenaddress.greenapi.data.BalanceData;
import com.greenaddress.greenapi.data.EstimatesData;
import com.greenaddress.greenapi.data.HWDeviceData;
import com.greenaddress.greenapi.data.JSONData;
import com.greenaddress.greenapi.data.NetworkData;
import com.greenaddress.greenapi.data.PinData;
import com.greenaddress.greenapi.data.ReconnectHint;
import com.greenaddress.greenapi.data.TransactionData;
import com.greenaddress.greenapi.data.TwoFactorConfigData;
import com.greenaddress.greenapi.data.TwoFactorDetailData;
import com.greenaddress.greenapi.model.NotificationHandlerImpl;
import com.greenaddress.greenbits.ui.BuildConfig;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static com.blockstream.libgreenaddress.GDK.GA_ERROR;

public class GDKSession {

    // Fine to have a static objectMapper according to docs if using always same configuration
    private static final ObjectMapper mObjectMapper = new ObjectMapper();
    private static GDKSession instance = new GDKSession();

    private Object mNativeSession;
    private final NotificationHandlerImpl mNotification;

    static {
        mObjectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    protected GDKSession() {
        mNotification = new NotificationHandlerImpl();
        mNativeSession = GDK.create_session();
    }

    /*
     * YOU SHOULD NEVER KEEP A REFERENCE TO THE INSTANCE RETURNED HERE. IT COULD BE DESTROYED
     * AND LEAD TO ITS USAGE AFTER ITS DESTRUCTOR HAS BEEN CALLED
     */
    public static GDKSession get() {
        return instance;
    }
    public static GDKSession getGDKSession() {
        return instance;
    }

    public void connect(final String network, final boolean isDebug) throws Exception {
        final ObjectNode details = mObjectMapper.createObjectNode();
        details.put("name", network);
        details.put("use_tor", false);
        details.put("log_level", isDebug ? "debug" : "none");
        GDK.setNotificationHandler(mNotification);
        GDK.connect(mNativeSession, details);
    }

    public void connectWithProxy(final String network, final String proxyAsString, final boolean useTor, boolean isDebug) throws Exception {
        final ObjectNode details = mObjectMapper.createObjectNode();
        details.put("name", network);
        details.put("proxy", proxyAsString);
        details.put("use_tor", useTor);
        details.put("log_level", isDebug ? "debug" : "none");
        GDK.setNotificationHandler(mNotification);
        GDK.connect(mNativeSession, details);
    }

    public void reconnectNow() throws Exception {
        final ReconnectHint hint = new ReconnectHint();
        hint.setHint("now");
        Log.d("reconnectNow",hint.toString());
        GDK.reconnect_hint(mNativeSession, hint);
    }

    public void reconnectDisable() throws Exception {
        final ReconnectHint hint = new ReconnectHint();
        hint.setHint("disable");
        Log.d("reconnectDisable",hint.toString());
        GDK.reconnect_hint(mNativeSession, hint);
    }

    public void disconnect() throws Exception {
        GDK.disconnect(mNativeSession);
        GDK.setNotificationHandler(null);
        mNotification.reset();
    }

    public void loginWithPin(final String pin, final PinData pinData) throws Exception {
        GDK.login_with_pin(mNativeSession, pin, pinData);
    }

    public void loginWithMnemonic(final String mnemonic, final String password) throws Exception {
        final Object res = GDK.login(mNativeSession, mObjectMapper.createObjectNode(), mnemonic, password);
        final GDKTwoFactorCall call = new GDKTwoFactorCall(res);
        final ObjectNode node =  call.resolve(null, null);
        if (node != null && node.has("error"))
            throw new Exception(node.get("error").asText());
    }

    public String getTorSocks5() throws Exception {
        return GDK.get_tor_socks5(mNativeSession);
    }

    public String getWatchOnlyUsername() throws Exception {
        return GDK.get_watch_only_username(mNativeSession);
    }

    public GDKTwoFactorCall login(final HWDeviceData hwDevice, final String mnemonic, final String mnemonicPassword) throws Exception {
        final ObjectNode hw = hwDevice == null ? mObjectMapper.createObjectNode() : mObjectMapper.valueToTree(hwDevice);
        return new GDKTwoFactorCall(GDK.login(mNativeSession, hw, mnemonic, mnemonicPassword));
    }

    public void loginWatchOnly(final String username, final String password) throws Exception {
        GDK.login_watch_only(mNativeSession, username, password);
    }

    public List<TransactionData> parseTransactions(final ArrayNode txListObject) throws Exception {
        //final ArrayNode txListObject = getTransactionsRaw(subAccount, first, count);
        final List<TransactionData> transactionDataPagedData =
                mObjectMapper.readValue(mObjectMapper.treeAsTokens(txListObject),
                        new TypeReference<List<TransactionData>>() {});
        return transactionDataPagedData;
    }

    public JsonNode findTransactionRaw(final ArrayNode txListObject, final String txhash) throws Exception {
        for (JsonNode node : txListObject) {
            if (node.get("txhash").asText().equals(txhash))
                return node;
        }

        return null;
    }

    public GDKTwoFactorCall getTransactionsRaw(final int subAccount, final int first, final int count) throws Exception {
        final ObjectNode details = mObjectMapper.createObjectNode();
        details.put("subaccount", subAccount);
        details.put("first", first);
        details.put("count", count);
        details.put("num_confs", 0);
        return new GDKTwoFactorCall(GDK.get_transactions(mNativeSession, details));
    }

    public void setWatchOnly(final String username, final String password) throws Exception {
        GDK.set_watch_only(mNativeSession, username, password);
    }

    public GDKTwoFactorCall getSubAccount(final long subAccount) {
        return new GDKTwoFactorCall(GDK.get_subaccount(mNativeSession, subAccount));
    }

    public GDKTwoFactorCall getSubAccounts() {
        return new GDKTwoFactorCall(GDK.get_subaccounts(mNativeSession));
    }

    public GDKTwoFactorCall createSubAccount(final String name, final String type) throws Exception {
        final ObjectNode details = mObjectMapper.createObjectNode();
        details.set("name", new TextNode(name));
        details.set("type", new TextNode(type));
        return new GDKTwoFactorCall(GDK.create_subaccount(mNativeSession, details));
    }

    public TwoFactorConfigData getTwoFactorConfig() throws Exception  {
        final ObjectNode twofactorConfig = (ObjectNode) GDK.get_twofactor_config(mNativeSession);
        final TwoFactorConfigData twoFactorConfigData = mObjectMapper.treeToValue(twofactorConfig, TwoFactorConfigData.class);
        debugEqual(twofactorConfig, twoFactorConfigData);
        return twoFactorConfigData;
    }

    public GDKTwoFactorCall getBalance(final Integer subAccount, final long confirmations) {
        final ObjectNode details = mObjectMapper.createObjectNode();
        details.put("subaccount", subAccount);
        details.put("num_confs", confirmations);
        return new GDKTwoFactorCall( GDK.get_balance(mNativeSession, details));
    }

    public Map<String, Bitmap> getAssetsIcons(final boolean refresh) throws Exception {
        final ObjectNode details = mObjectMapper.createObjectNode();
        details.put("icons", true);
        details.put("assets", false);
        details.put("refresh", refresh);
        final ObjectNode data = (ObjectNode) GDK.refresh_assets(mNativeSession, details);
        final ObjectNode iconsData = (ObjectNode) data.get("icons");
        if (iconsData.has("last_modified"))
            iconsData.remove("last_modified");
        final Map<String, Bitmap> icons = new HashMap<>();
        final Iterator<String> iterator = iconsData.fieldNames();
        while (iterator.hasNext()) {
            final String key = iterator.next();
            final byte[] decodedString = Base64.decode(iconsData.get(key).asText(), Base64.DEFAULT);
            final Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            icons.put(key, decodedByte);
        }
        return icons;
    }

    public Map<String, AssetInfoData> getAssetsInfos(final boolean refresh) throws Exception {
        final ObjectNode details = mObjectMapper.createObjectNode();
        details.put("icons", false);
        details.put("assets", true);
        details.put("refresh", refresh);
        final ObjectNode data = (ObjectNode) GDK.refresh_assets(mNativeSession, details);
        final ObjectNode infosData = (ObjectNode) data.get("assets");
        if (infosData.has("last_modified"))
            infosData.remove("last_modified");
        final Map<String, AssetInfoData> infos = new HashMap<>();
        final Iterator<String> iterator = infosData.fieldNames();
        while (iterator.hasNext()) {
            final String key = iterator.next();
            final AssetInfoData info = mObjectMapper.treeToValue(infosData.get(key), AssetInfoData.class);
            infos.put(key, info);
        }
        return infos;
    }

    public BalanceData convertBalance(final BalanceData balanceData) throws Exception {
        final ObjectNode convertedBalanceData = (ObjectNode) GDK.convert_amount(mNativeSession, balanceData);
        if (balanceData.getAssetInfo() != null)
            convertedBalanceData.set("asset_info", balanceData.getAssetInfo().toObjectNode());
        final BalanceData balanceData1 = BalanceData.from(mObjectMapper, convertedBalanceData);
        debugEqual(convertedBalanceData, balanceData1);
        return balanceData1;
    }

    public BalanceData convertBalance(final long satoshi) throws Exception {
        final ObjectNode convertedBalanceData = (ObjectNode) convertSatoshi(satoshi);
        final BalanceData balanceData = BalanceData.from(mObjectMapper, convertedBalanceData);
        return balanceData;
    }

    public ObjectNode convert(final ObjectNode amount) throws Exception {
        return (ObjectNode) GDK.convert_amount(mNativeSession, amount);
    }

    public ObjectNode convertSatoshi(final long satoshi) throws Exception {
        final ObjectNode amount = mObjectMapper.createObjectNode();
        amount.set("satoshi", new LongNode(satoshi));
        return convert(amount);
    }

    public GDKTwoFactorCall createTransactionRaw(final Activity parent, final JSONData createTransactionData) throws Exception {
        return new GDKTwoFactorCall(GDK.create_transaction(mNativeSession, createTransactionData));
    }

    public GDKTwoFactorCall createTransactionRaw(final Activity parent, final ObjectNode tx) throws Exception {
        return new GDKTwoFactorCall(GDK.create_transaction(mNativeSession, tx));
    }

    public GDKTwoFactorCall createTransactionFromUri(final Activity parent, final String uri, final int subaccount) throws Exception {
        final ObjectNode tx = mObjectMapper.createObjectNode();
        tx.put("subaccount", subaccount);
        final ObjectNode address = mObjectMapper.createObjectNode();
        address.put("address", uri);
        final ArrayNode addressees = mObjectMapper.createArrayNode();
        addressees.add(address);
        tx.set("addressees", addressees);
        return createTransactionRaw(parent, tx);
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
        final ObjectNode feeEstimates = (ObjectNode) GDK.get_fee_estimates(mNativeSession);
        return mObjectMapper.treeToValue(feeEstimates, EstimatesData.class).getFees();
    }

    public GDKTwoFactorCall getUTXO(final long subAccount, final long confirmations) throws Exception {
        final ObjectNode details = mObjectMapper.createObjectNode();
        details.put("subaccount", subAccount);
        details.put("num_confs", confirmations);
        return new GDKTwoFactorCall(GDK.get_unspent_outputs(mNativeSession, details));
    }

    public Map<String, Object> getAvailableCurrencies() throws Exception {
        final ObjectNode availableCurrencies = (ObjectNode) GDK.get_available_currencies(mNativeSession);
        return mObjectMapper.treeToValue(availableCurrencies, Map.class);
    }

    public static List<NetworkData> getNetworks() {
        final List<NetworkData> networksMap = new LinkedList<>();
        final ObjectNode networks = (ObjectNode) GDK.get_networks();
        final ArrayNode nodes = (ArrayNode) networks.get("all_networks");
        final boolean isProduction = !BuildConfig.DEBUG;

        for (final JsonNode node : nodes) {
            final String networkName = node.asText();
            try {
                final NetworkData data = mObjectMapper.treeToValue(networks.get(networkName), NetworkData.class);
                if (!(isProduction && data.getDevelopment())) {
                    networksMap.add(data);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Collections.sort( networksMap);
        return networksMap;
    }

    public static void registerNetwork(final String name, final String networkJson) throws Exception {
        GDK.register_network(name, networkJson);
    }

    public GDKTwoFactorCall getReceiveAddress(final int subAccount) throws Exception {
        final ObjectNode details = mObjectMapper.createObjectNode();
        details.put("subaccount", subAccount);
        return new GDKTwoFactorCall(GDK.get_receive_address(mNativeSession, details));
    }

    public static boolean isEnabled() {
        return GDK.isEnabled();
    }

    public static String generateMnemonic(final String language) {
        return GDK.generate_mnemonic();
    }

    public GDKTwoFactorCall registerUser(final HWDeviceData hwDevice, final String mnemonic) throws Exception {
        final ObjectNode hw = hwDevice == null ? mObjectMapper.createObjectNode() : mObjectMapper.valueToTree(hwDevice);
        return new GDKTwoFactorCall(GDK.register_user(mNativeSession, hw, mnemonic));
    }

    public PinData setPin(final String mnemonic, final String pin, final String device) throws Exception {
        final ObjectNode pinData = (ObjectNode) GDK.set_pin(mNativeSession, mnemonic, pin, device);
        final PinData value = mObjectMapper.treeToValue(pinData, PinData.class);
        debugEqual(pinData, value);
        return value;
    }

    public void destroy() throws Exception {
        GDK.destroy_session(mNativeSession);
        mNativeSession = GDK.create_session();
    }

    public Boolean changeMemo(final String txHashHex, final String memo) throws Exception {
        GDK.set_transaction_memo(mNativeSession, txHashHex, memo, GDK.GA_MEMO_USER);
        return true;
    }

    public String getSystemMessage() throws Exception {
        return GDK.get_system_message(mNativeSession);
    }

    public GDKTwoFactorCall ackSystemMessage(final String message) throws Exception {
        return new GDKTwoFactorCall(GDK.ack_system_message(mNativeSession, message));
    }

    public String getMnemonicPassphrase() {
        // TODO at the moment the encrypted mnemonic is not supported so we are always passing the empty string
        return GDK.get_mnemonic_passphrase(mNativeSession, "");
    }

    public ObjectNode getGDKSettings() throws Exception  {
        return (ObjectNode) GDK.get_settings(mNativeSession);
    }

    protected static void debugEqual(final ObjectNode jsonNode, final JSONData data)  {
        if(!jsonNode.toString().equals(data.toString())) {
            final Class<? extends JSONData> aClass = data.getClass();
            Log.d("DBGEQ", "jsonData type " + aClass);
            Log.d("DBGEQ", "jsonNode " + jsonNode);
            Log.d("DBGEQ", "jsonData " + data);
        }
    }

    public GDKTwoFactorCall changeSettingsTwoFactor(final String method, final TwoFactorDetailData details) throws Exception  {
        final Object twoFactorCall = GDK.change_settings_twofactor(mNativeSession, method, details);
        final GDKTwoFactorCall gdkTwoFactorCall = new GDKTwoFactorCall(twoFactorCall);
        return gdkTwoFactorCall;
    }

    public GDKTwoFactorCall twoFactorReset(final String email, final boolean isDispute) throws Exception  {
        final Object twoFactorCall = GDK.twofactor_reset(mNativeSession, email, isDispute ? GDK.GA_TRUE : GDK.GA_FALSE);
        final GDKTwoFactorCall gdkTwoFactorCall = new GDKTwoFactorCall(twoFactorCall);
        return gdkTwoFactorCall;
    }

    public GDKTwoFactorCall twofactorCancelReset() throws Exception  {
        final Object twoFactorCall = GDK.twofactor_cancel_reset(mNativeSession);
        final GDKTwoFactorCall gdkTwoFactorCall = new GDKTwoFactorCall(twoFactorCall);
        return gdkTwoFactorCall;
    }

    public GDKTwoFactorCall twoFactorChangeLimits(final ObjectNode limitsData) throws Exception  {
        return new GDKTwoFactorCall(GDK.twofactor_change_limits(mNativeSession, limitsData));
    }

    public void sendNlocktimes() throws Exception  {
        GDK.send_nlocktimes(mNativeSession);
    }

    public NotificationHandlerImpl getNotificationModel() {
        return mNotification;
    }

    public GDKTwoFactorCall changeSettings(final ObjectNode setting) throws Exception  {
        return new GDKTwoFactorCall(GDK.change_settings(mNativeSession, setting));
    }

    public static Integer getErrorCode(final String message) {
        try {
            final String stringCode = message.split(" ")[1];
            return Integer.parseInt(stringCode);
        } catch (final Exception e) {
            return GA_ERROR;
        }
    }
}
