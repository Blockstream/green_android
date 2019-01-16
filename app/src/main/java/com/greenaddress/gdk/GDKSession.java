package com.greenaddress.gdk;

import android.app.Activity;
import android.util.Log;

import com.blockstream.libgreenaddress.GDK;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.greenaddress.greenapi.data.BalanceData;
import com.greenaddress.greenapi.data.CreateTransactionData;
import com.greenaddress.greenapi.data.EstimatesData;
import com.greenaddress.greenapi.data.HWDeviceData;
import com.greenaddress.greenapi.data.JSONData;
import com.greenaddress.greenapi.data.NetworkData;
import com.greenaddress.greenapi.data.PagedData;
import com.greenaddress.greenapi.data.PinData;
import com.greenaddress.greenapi.data.SubaccountData;
import com.greenaddress.greenapi.data.TransactionData;
import com.greenaddress.greenapi.data.TwoFactorConfigData;
import com.greenaddress.greenapi.data.TwoFactorDetailData;
import com.greenaddress.greenapi.model.NotificationHandlerImpl;
import com.greenaddress.greenbits.GaService;
import com.greenaddress.greenbits.ui.BuildConfig;

import org.bitcoinj.core.AddressFormatException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class GDKSession {

    // Fine to have a static objectMapper according to docs if using always same configuration
    private static ObjectMapper mObjectMapper = new ObjectMapper();
    private static GDKSession instance;

    private Object mNativeSession;
    private NotificationHandlerImpl mNotification;

    static {
        mObjectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private GDKSession() {
        GDK.setJSONConverter(new JSONConverterImpl());
        mNotification = new NotificationHandlerImpl();
        GDK.setNotificationHandler(mNotification);
        mNativeSession = GDK.create_session();
    }

    public static GDKSession getInstance() {
        if (instance == null) {
            instance = new GDKSession();
        }
        return instance;
    }

    public void connect(final String network, final boolean isDebug) {
        GDK.connect(mNativeSession, network, isDebug ? GDK.GA_TRUE : GDK.GA_FALSE);
    }

    public void connectWithProxy(final String network, String proxyAsString, boolean useTor, boolean debug) {
        GDK.connect_with_proxy(mNativeSession, network, proxyAsString,
                               useTor ? GDK.GA_TRUE : GDK.GA_FALSE,
                               debug ? GDK.GA_TRUE : GDK.GA_FALSE);
    }

    public void disconnect() {
        GDK.disconnect(mNativeSession);
        GDK.destroy_session(mNativeSession);
        instance=null;
    }

    public void loginWithPin(final String pin, final PinData pinData) {
        GDK.login_with_pin(mNativeSession, pin, pinData);
    }

    public GDKTwoFactorCall login(final Activity parent, final HWDeviceData hwDevice, final String mnemonic, final String mnemonicPassword) {
        final ObjectNode hw = hwDevice == null ? mObjectMapper.createObjectNode() : mObjectMapper.valueToTree(hwDevice);
        return new GDKTwoFactorCall(parent, GDK.login(mNativeSession, hw, mnemonic, mnemonicPassword));
    }

    public void loginWatchOnly(final String username, final String password) {
        GDK.login_watch_only(mNativeSession, username, password);
    }

    public List<TransactionData> getTransactions(final int subAccount, final int pageId) throws IOException {
        return getTransactionsPaged(subAccount,pageId).getList();
    }

    public PagedData<TransactionData> getTransactionsPaged(final int subAccount, final int pageId) throws IOException {
        final ObjectNode txListObject = (ObjectNode) GDK.get_transactions(mNativeSession, subAccount, pageId);
        final PagedData<TransactionData> transactionDataPagedData =
                mObjectMapper.readValue(mObjectMapper.treeAsTokens(txListObject),
                        new TypeReference<PagedData<TransactionData>>() {});
        debugEqual(txListObject,transactionDataPagedData);
        return transactionDataPagedData;
    }

    public JsonNode getTransactionRaw(final int subAccount, final String txhash) {
        final ObjectNode txListObject = (ObjectNode) GDK.get_transactions(mNativeSession, subAccount, 0);
        final ArrayNode array = (ArrayNode) txListObject.get("list");
        for (JsonNode node : array ) {
            if (node.get("txhash").asText().equals(txhash))
                return node;
        }
        return null;
    }

    public void setWatchOnly(final String username, final String password) {
        GDK.set_watch_only(mNativeSession, username, password);
    }

    public List<SubaccountData> getSubAccounts() throws IOException {
        final ArrayNode accounts = (ArrayNode) GDK.get_subaccounts(mNativeSession);
        return mObjectMapper.readValue(mObjectMapper.treeAsTokens(accounts), new TypeReference<List<SubaccountData>>() {});
    }

    public GDKTwoFactorCall createSubAccount(final Activity parent, final String name, final String type) {
        final ObjectNode details = mObjectMapper.createObjectNode();
        details.set("name", new TextNode(name));
        details.set("type", new TextNode(type));
        return new GDKTwoFactorCall(parent, GDK.create_subaccount(mNativeSession, details));
    }

    public TwoFactorConfigData getTwoFactorConfig() throws IOException  {
        final ObjectNode twofactorConfig = (ObjectNode) GDK.get_twofactor_config(mNativeSession);
        TwoFactorConfigData twoFactorConfigData = mObjectMapper.treeToValue(twofactorConfig, TwoFactorConfigData.class);
        debugEqual(twofactorConfig, twoFactorConfigData);
        return twoFactorConfigData;
    }

    public BalanceData getBalance(final Integer subAccount, final long confirmations) throws IOException {
        final ObjectNode balanceData = (ObjectNode) GDK.get_balance(mNativeSession, subAccount.longValue(), confirmations);
        BalanceData balanceData1 = mObjectMapper.treeToValue(balanceData, BalanceData.class);
        debugEqual(balanceData, balanceData1);
        return balanceData1;
    }

    public BalanceData convertBalance(final BalanceData balanceData) throws IOException {
        final ObjectNode convertedBalanceData = (ObjectNode) GDK.convert_amount(mNativeSession, balanceData);
        BalanceData balanceData1 = mObjectMapper.treeToValue(convertedBalanceData, BalanceData.class);
        debugEqual(convertedBalanceData, balanceData1);
        return balanceData1;
    }

    public ObjectNode convert(final ObjectNode amount) {
        return (ObjectNode) GDK.convert_amount(mNativeSession, amount);
    }

    public ObjectNode convertSatoshi(final long satoshi) {
        final ObjectNode amount = mObjectMapper.createObjectNode();
        amount.set("satoshi", new LongNode(satoshi));
        return convert(amount);
    }

    public ObjectNode createTransactionRaw(final JSONData createTransactionData) {
        final ObjectNode data = (ObjectNode) GDK.create_transaction(mNativeSession, createTransactionData);
        return data;
    }

    public ObjectNode createTransactionRaw(final ObjectNode tx) {
        return (ObjectNode) GDK.create_transaction(mNativeSession, tx);
    }

    public ObjectNode createTransactionFromUri(final String uri, final int subaccount) throws AddressFormatException {
        final BalanceData balanceData = new BalanceData();
        balanceData.setAddress(uri);
        final List<BalanceData> balanceDataList = new ArrayList<>();
        balanceDataList.add(balanceData);
        final CreateTransactionData createTransactionData = new CreateTransactionData();
        createTransactionData.setAddressees(balanceDataList);
        createTransactionData.setSubaccount(subaccount);
        final ObjectNode transaction = createTransactionRaw(createTransactionData);
        final String error = transaction.get("error").asText();
        if ("id_invalid_address".equals(error)) {
            throw new AddressFormatException();
        }
        return transaction;
    }

    public CreateTransactionData createTransaction(final JSONData createTransactionData) throws IOException {
        final ObjectNode data = (ObjectNode) GDK.create_transaction(mNativeSession, createTransactionData);
        CreateTransactionData createTransactionData1 = mObjectMapper.treeToValue(data, CreateTransactionData.class);
        debugEqual(data, createTransactionData1);
        return createTransactionData1;
    }

    public GDKTwoFactorCall signTransactionRaw(final Activity parent, final ObjectNode createTransactionData) {
        return new GDKTwoFactorCall(parent, GDK.sign_transaction(mNativeSession, createTransactionData));
    }

    public GDKTwoFactorCall signTransaction(final Activity parent, final JSONData createTransactionData) {
        return new GDKTwoFactorCall(parent, GDK.sign_transaction(mNativeSession, createTransactionData));
    }

    public GDKTwoFactorCall sendTransaction(final Activity parent, final JSONData txDetails) {
        final Object twoFactorCall = GDK.send_transaction(mNativeSession, txDetails);
        final GDKTwoFactorCall gdkTwoFactorCall = new GDKTwoFactorCall(parent, twoFactorCall);
        return gdkTwoFactorCall;
    }

    public GDKTwoFactorCall sendTransactionRaw(final Activity parent, final ObjectNode txDetails) {
        final Object twoFactorCall = GDK.send_transaction(mNativeSession, txDetails);
        final GDKTwoFactorCall gdkTwoFactorCall = new GDKTwoFactorCall(parent, twoFactorCall);
        return gdkTwoFactorCall;
    }

    public String broadcastTransactionRaw(final String txDetails) {
        return GDK.broadcast_transaction(mNativeSession, txDetails);
    }

    public List<Long> getFeeEstimates() throws IOException {
        final ObjectNode feeEstimates = (ObjectNode) GDK.get_fee_estimates(mNativeSession);
        return mObjectMapper.treeToValue(feeEstimates, EstimatesData.class).getFees();
    }

    public List<TransactionData> getUTXO(final long subAccount, final long confirmations) throws IOException {
        final ArrayNode unspentOutputs = (ArrayNode) GDK.get_unspent_outputs(mNativeSession, subAccount, confirmations);
        return mObjectMapper.readValue(mObjectMapper.treeAsTokens(unspentOutputs), new TypeReference<List<TransactionData>>() {});
    }

    public Map<String, Object> getAvailableCurrencies() throws JsonProcessingException {
        final ObjectNode availableCurrencies = (ObjectNode) GDK.get_available_currencies(mNativeSession);
        return mObjectMapper.treeToValue(availableCurrencies, Map.class);
    }

    public static List<NetworkData> getNetworks()  {
        final List<NetworkData> networksMap = new LinkedList<>();
        final ObjectNode networks = (ObjectNode) GDK.get_networks();
        final ArrayNode nodes = (ArrayNode) networks.get("all_networks");
        final boolean isProduction = !BuildConfig.DEBUG;

        for (final JsonNode node : nodes) {
            String networkName = node.asText();
            try {
                final NetworkData data = mObjectMapper.treeToValue(networks.get(networkName), NetworkData.class);
                if (!(isProduction && data.getDevelopment())) {
                    networksMap.add(data);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        return networksMap;
    }

    public static void registerNetwork(final String name, final String networkJson ) {
        GDK.register_network(name, networkJson);
    }

    public String getReceiveAddress(final int subAccount ) {
        return GDK.get_receive_address(mNativeSession, subAccount);
    }

    public static boolean isEnabled() {
        return GDK.isEnabled();
    }

    public static String generateMnemonic(final String language) {
        return GDK.generate_mnemonic();
    }

    public GDKTwoFactorCall registerUser(final Activity parent, final HWDeviceData hwDevice, final String mnemonic) {
        final ObjectNode hw = hwDevice == null ? mObjectMapper.createObjectNode() : mObjectMapper.valueToTree(hwDevice);
        return new GDKTwoFactorCall(parent, GDK.register_user(mNativeSession, hw, mnemonic));
    }

    public PinData setPin(final String mnemonic, final String pin, final String device) throws IOException {
        final ObjectNode pinData = (ObjectNode) GDK.set_pin(mNativeSession, mnemonic, pin, device);
        PinData value = mObjectMapper.treeToValue(pinData, PinData.class);
        debugEqual(pinData, value);
        return value;
    }

    public void destroy() {
        GDK.destroy_session(mNativeSession);
    }

    public Boolean changeMemo(final String txHashHex, final String memo) {
        GDK.set_transaction_memo(mNativeSession, txHashHex, memo, GDK.GA_MEMO_USER);
        return true;
    }

    public String getSystemMessage() {
        return GDK.get_system_message(mNativeSession);
    }

    public GDKTwoFactorCall ackSystemMessage(final Activity parent, final String message) {
        return new GDKTwoFactorCall(parent, GDK.ack_system_message(mNativeSession, message));
    }

    public String getMnemonicPassphrase() {
        // TODO at the moment the encrypted mnemonic is not supported so we are always passing the empty string
        return GDK.get_mnemonic_passphrase(mNativeSession, "");
    }

    public ObjectNode getSettings() {
        return (ObjectNode) GDK.get_settings(mNativeSession);
    }

    public CreateTransactionData getTransactionDetails(final String txHash) throws JsonProcessingException {
        ObjectNode transactionDetails = (ObjectNode) GDK.get_transaction_details(mNativeSession, txHash);
        CreateTransactionData createTransactionData = mObjectMapper.treeToValue(transactionDetails, CreateTransactionData.class);
        debugEqual(transactionDetails,createTransactionData);
        return createTransactionData;
    }

    protected static void debugEqual(ObjectNode jsonNode, JSONData data) {
        if(!jsonNode.toString().equals(data.toString())) {
            Class<? extends JSONData> aClass = data.getClass();
            Log.d("DBGEQ", "jsonData type " + aClass);
            Log.d("DBGEQ", "jsonNode " + jsonNode);
            Log.d("DBGEQ", "jsonData " + data);
        }
    }

    public GDKTwoFactorCall changeSettingsTwoFactor(final Activity parent, final String method, final TwoFactorDetailData details) {
        final Object twoFactorCall = GDK.change_settings_twofactor(mNativeSession, method, details);
        final GDKTwoFactorCall gdkTwoFactorCall = new GDKTwoFactorCall(parent, twoFactorCall);
        return gdkTwoFactorCall;
    }

    public GDKTwoFactorCall twoFactorReset(final Activity parent, final String email, final boolean isDispute) {
        final Object twoFactorCall = GDK.twofactor_reset(mNativeSession, email, isDispute ? GDK.GA_TRUE : GDK.GA_FALSE);
        final GDKTwoFactorCall gdkTwoFactorCall = new GDKTwoFactorCall(parent, twoFactorCall);
        return gdkTwoFactorCall;
    }

    public GDKTwoFactorCall twofactorCancelReset(final Activity parent) {
        final Object twoFactorCall = GDK.twofactor_cancel_reset(mNativeSession);
        final GDKTwoFactorCall gdkTwoFactorCall = new GDKTwoFactorCall(parent, twoFactorCall);
        return gdkTwoFactorCall;
    }

    public GDKTwoFactorCall twoFactorChangeLimits(final Activity parent, final ObjectNode limitsData) {
        return new GDKTwoFactorCall(parent, GDK.twofactor_change_limits(mNativeSession, limitsData));
    }

    public void sendNlocktimes() {
        GDK.send_nlocktimes(mNativeSession);
    }

    public void setNotificationModel(final GaService service) {
        mNotification.setModel(service);
    }

    public GDKTwoFactorCall changeSettings(final Activity parent, ObjectNode setting) {
        return new GDKTwoFactorCall(parent, GDK.change_settings(mNativeSession, setting));
    }
}
