package com.greenaddress.greenapi;

import android.text.TextUtils;
import android.util.Log;

import com.blockstream.libwally.Wally;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.SettableFuture;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.GeneratedMessage;
import com.greenaddress.greenbits.GaService;
import com.greenaddress.greenbits.spv.Socks5SocketFactory;
import com.greenaddress.greenbits.ui.BuildConfig;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.bitcoin.protocols.payments.Protos;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.protocols.payments.PaymentProtocol;
import org.bitcoinj.protocols.payments.PaymentProtocolException;
import org.bitcoinj.protocols.payments.PaymentSession;
import org.codehaus.jackson.map.MappingJsonFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.SocketAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.RejectedExecutionException;

import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import rx.Scheduler;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import ws.wamp.jawampa.ApplicationError;
import ws.wamp.jawampa.CallFlags;
import ws.wamp.jawampa.PubSubData;
import ws.wamp.jawampa.Reply;
import ws.wamp.jawampa.WampClient;
import ws.wamp.jawampa.WampClientBuilder;
import ws.wamp.jawampa.connection.IWampConnectorProvider;
import ws.wamp.jawampa.transport.netty.NettyWampClientConnectorProvider;
import ws.wamp.jawampa.transport.netty.NettyWampConnectionConfig;

public class WalletClient {

    private static final String TAG = WalletClient.class.getSimpleName();
    // v2: API version 2, sw: Opt in/out segwit
    private static final String FEATURES = "v2,sw";
    private static final String USER_AGENT = String.format("[%s]%s;%s;%s;%s",
            FEATURES, BuildConfig.VERSION_CODE, BuildConfig.BUILD_TYPE,
            android.os.Build.VERSION.SDK_INT, System.getProperty("os.arch"));

    private final Scheduler mScheduler = Schedulers.newThread();
    private final ListeningExecutorService mExecutor;
    private final INotificationHandler mNotificationHandler;
    private SocketAddress mProxyAddress;
    private final OkHttpClient mHttpClient = new OkHttpClient();
    private final OkHttpClient mHttpClientBIP70 = new OkHttpClient();
    private boolean mTorEnabled;
    private WampClient mConnection;
    private LoginData mLoginData;
    private JSONMap mFeeEstimates;
    private ISigningWallet mHDParent;
    private String mWatchOnlyUsername;
    private String mWatchOnlyPassword;
    private String mMnemonic;

    private String h(final byte[] data) { return Wally.hex_from_bytes(data); }

    public WalletClient(final INotificationHandler notificationHandler,
                        final ListeningExecutorService es) {
        mNotificationHandler = notificationHandler;
        mExecutor = es;
    }

    /**
     * Call handler.
     */
    private interface CallHandler {

        /**
         * Fired on successful completion of call.
         *
         * @param result     The RPC result transformed into the type that was specified in call.
         */
        void onResult(Object result);
    }

    // FIXME: Remove this and replace with a catch from its only caller
    private interface ErrorHandler {
        void onError(final GAException e);
    }

    private <V> CallHandler simpleHandler(final SettableFuture<V> f) {
        return new CallHandler() {
            public void onResult(final Object result) {
                f.set((V) result);
            }
        };
    }

    private <V> CallHandler stringHandler(final SettableFuture<V> f) {
        return new CallHandler() {
            public void onResult(final Object result) {
                f.set((V)result.toString());
            }
        };
    }

    /**
     * Handler for PubSub events.
     */
    public interface EventHandler {

        /**
         * Fired when an event for the PubSub subscription is received.
         *
         * @param topicUri   The URI or CURIE of the topic the event was published to.
         * @param event      The event, transformed into the type that was specified when subscribing.
         */
        void onEvent(String topicUri, Object event);
    }

    private static void logCallDetails(final String procedure, final String result, final Object... args) {
        final ArrayList<Object> expanded = new ArrayList<>();
        for (final Object o : args) {
            if (o instanceof Object[])
                expanded.add(String.format("[%s]", TextUtils.join(", ", (Object[]) o)));
            else
                expanded.add(o);
        }
        Log.v(TAG, String.format("%s(%s)\n\t -> %s", procedure, TextUtils.join(", ", expanded), result));
    }

    private void onCallError(final SettableFuture rpc, final String procedure,
                             final ErrorHandler errHandler, final GAException e,
                             final Object... args) {
        final String logDetails = e.mUri + ':' + e.mMessage;
        if (BuildConfig.DEBUG)
            logCallDetails(procedure, logDetails, args);
        else
            Log.d(TAG, procedure + "->" + logDetails);
        if (errHandler != null)
            errHandler.onError(e); // May change the exception details in e
        rpc.setException(e);
    }

    private SettableFuture clientCall(final SettableFuture rpc,
                                      final String procedure, final Class result,
                                      final CallHandler handler, final ErrorHandler errHandler,
                                      final Object... args) {
        final ObjectMapper mapper = new ObjectMapper();
        final ArrayNode argsNode = mapper.valueToTree(Arrays.asList(args));

        final Action1<Reply> replyHandler = new Action1<Reply>() {
            @Override
            public void call(final Reply reply) {
                final JsonNode node = reply.arguments().get(0);
                if (BuildConfig.DEBUG)
                    logCallDetails(procedure, node.toString(), args);
                handler.onResult(mapper.convertValue(node, result));
            }
        };

        final Action1<Throwable> errorHandler = new Action1<Throwable>() {
            @Override
            public void call(final Throwable t) {
                GAException e = null;
                if (t instanceof ApplicationError) {
                    final ArrayNode a = ((ApplicationError) t).arguments();
                    if (a != null && a.size() >= 2)
                        e = new GAException(a.get(0).asText(), a.get(1).asText());
                }
                if (e == null)
                    e = new GAException(t.toString());
                onCallError(rpc, procedure, errHandler, e, args);
            }
        };

        try {
            if (mConnection != null) {
                final EnumSet<CallFlags> flags = EnumSet.of(CallFlags.DiscloseMe);
                final String callName = "com.greenaddress." + procedure;
                mConnection.call(callName, flags, argsNode, null)
                           .observeOn(mScheduler)
                           .subscribe(replyHandler, errorHandler);
                return rpc;
            }
        } catch (final RejectedExecutionException e) {
            // Fall through
        }
        onCallError(rpc, procedure, errHandler, new GAException("not connected"), args);
        return rpc;
    }

    private SettableFuture clientCall(final SettableFuture rpc,
                                      final String procedure, final Class result,
                                      final CallHandler handler, final Object... args) {
        return clientCall(rpc, procedure, result, handler, null, args);
    }

    private <V> ListenableFuture<V> simpleCall(final String procedure, final Class result, final Object... args) {
        final SettableFuture<V> rpc = SettableFuture.create();
        final CallHandler handler = result == null ? stringHandler(rpc) : simpleHandler(rpc);
        final Class resultClass = result == null ? String.class : result;
        return clientCall(rpc, procedure, resultClass, handler, args);
    }

    private <T> T syncCall(final String procedure, final Class result,
                           final Object... args) throws Exception {

        if (mConnection == null)
            throw new GAException("not connected");

        final ObjectMapper mapper = new ObjectMapper();
        final ArrayNode argsNode = mapper.valueToTree(Arrays.asList(args));

        try {
            final EnumSet<CallFlags> flags = EnumSet.of(CallFlags.DiscloseMe);
            final String callName = "com.greenaddress." + procedure;
            final Reply reply;
            reply = mConnection.call(callName, flags, argsNode, null)
                               .observeOn(mScheduler).toBlocking().single();
            final JsonNode node = reply.arguments().get(0);
            if (BuildConfig.DEBUG)
                logCallDetails(procedure, node.toString(), args);
            return (T) mapper.convertValue(node, result);
        } catch (final RejectedExecutionException e) {
            if (BuildConfig.DEBUG)
                logCallDetails(procedure, e.getMessage(), args);
            throw new GAException("rejected");
        } catch (final Exception e) {
            Log.d(TAG, "Sync RPC exception: (" + procedure + ")->" + e.toString());
            String uri = GAException.INTERNAL;
            String error = e.toString();
            if (e instanceof ApplicationError) {
                final ArrayNode a = ((ApplicationError) e).arguments();
                if (a != null && a.size() >= 2) {
                    uri = a.get(0).asText();
                    error = a.get(1).asText();
                }
            }
            if (BuildConfig.DEBUG)
                logCallDetails(procedure, error, args);
            throw new GAException(uri, error);
        }
    }

    private void clientSubscribe(final String s, final Class mapClass, final EventHandler eventHandler) {
        final String topic = "com.greenaddress." + s;
        mConnection.makeSubscription(topic)
                   .observeOn(mScheduler)
                   .subscribe(new Action1<PubSubData>() {
            @Override
            public void call(final PubSubData pubSubData) {
                final ObjectMapper mapper = new ObjectMapper();

                eventHandler.onEvent(topic, mapper.convertValue(
                        pubSubData.arguments().get(0),
                        mapClass
                ));
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(final Throwable t) {
                Log.w(TAG, t);
                Log.i(TAG, "Subscribe failed (" + topic + "): " + t.toString());
            }
        });
    }

    public void setProxy(final String host, final String port) {
        if (TextUtils.isEmpty(host) || TextUtils.isEmpty(port)) {
            mProxyAddress = null;
            mHttpClient.setSocketFactory(null);
            mHttpClientBIP70.setSocketFactory(null);
            return;
        }
        try {
            mProxyAddress = new InetSocketAddress(host, Integer.parseInt(port));
            mHttpClient.setSocketFactory(new Socks5SocketFactory(host, port));
            mHttpClientBIP70.setSocketFactory(new Socks5SocketFactory(host, port));
        } catch (final UnknownHostException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void setTorEnabled(final boolean torEnabled) {
        mTorEnabled = torEnabled;
    }

    public LoginData getLoginData() {
        return mLoginData;
    }

    public JSONMap getFeeEstimates() {
        return mFeeEstimates;
    }


    public String getMnemonic() {
        return mMnemonic;
    }

    public void disconnect() {
        // FIXME: Server should handle logout without having to disconnect
        mLoginData = null;
        mMnemonic = null;
        mWatchOnlyUsername = null;
        mHDParent = null;

        if (mConnection != null) {
            mConnection.close();
            mConnection = null;
        }
    }

    public void registerUser(final ISigningWallet signingWallet,
                             final String mnemonic,
                             final String userAgent,
                             final byte[] pubkey, final byte[] chaincode,
                             final String deviceId) throws Exception {
        final String agent;
        if (userAgent == null)
            agent = USER_AGENT;
        else
            agent = String.format("%s %s", USER_AGENT, userAgent);

        mMnemonic = mnemonic;

        final byte[] path = signingWallet.getGAPath();


        // We don't check the return value of login.register (never returns false)
        syncCall("login.register", Boolean.class, h(pubkey), h(chaincode), agent, h(path));
        loginImpl(signingWallet, deviceId);
        HDKey.resetCache(mLoginData.mGaitPath);
    }

    public ListenableFuture<Map<String, Object>> getSubaccountBalance(final int subAccount) {
        return simpleCall("txs.get_balance", Map.class, subAccount);
    }

    // FIXME: Get rid of this
    public ListenableFuture<Map<?, ?>> getTwoFactorConfig() {
        return simpleCall("twofactor.get_config", Map.class);
    }

    public Map<?, ?> getTwoFactorConfigSync() throws Exception {
        return syncCall("twofactor.get_config", Map.class);
    }

    public void sendNLocktime() throws Exception {
        syncCall("txs.send_nlocktime", Boolean.class);
    }

    public ListenableFuture<Map<?, ?>> getAvailableCurrencies() {
        return simpleCall("login.available_currencies", Map.class);
    }

    public void setSpendingLimits(final JSONMap limitsData,
                                  final Map<String, String> twoFacData) throws Exception {
        syncCall("login.change_settings", Boolean.class, "tx_limits", limitsData.mData, twoFacData);
    }

    private void onAuthenticationComplete(final Map<String, Object> loginData, final ISigningWallet wallet, final String username, final String password) {
        mLoginData = new LoginData(loginData);
        mFeeEstimates = null;
        if (loginData.containsKey("fee_estimates"))
            mFeeEstimates = new JSONMap((Map) loginData.get("fee_estimates"));

        mHDParent = wallet;
        mWatchOnlyUsername = username;
        mWatchOnlyPassword = password;
        if (mHDParent != null)
            HDClientKey.resetCache(mLoginData.mSubAccounts, mHDParent);

        final boolean rbf = mLoginData.get("rbf");
        if (rbf && getUserConfig("replace_by_fee") == null) {
            // Enable rbf if server supports it and not disabled by user explicitly
            // FIXME: The server should do this surely?
            final Object t = Boolean.TRUE;
            setUserConfig(ImmutableMap.of("replace_by_fee", t), false);
        }

        clientSubscribe("txs.wallet_" + mLoginData.get("receiving_id"), Map.class, new EventHandler() {
            @Override
            public void onEvent(final String topicUri, final Object event) {

                final Map<?, ?> res = (Map) event;
                final Object subAccounts = res.get("subaccounts");

                final int affectedSubAccounts[];
                if (subAccounts instanceof Number) {
                    affectedSubAccounts = new int[1];
                    affectedSubAccounts[0] = ((Number) subAccounts).intValue();
                } else {
                    final ArrayList values = (ArrayList) subAccounts;
                    if (values == null)
                        affectedSubAccounts = new int[0];
                    else {
                        affectedSubAccounts = new int[values.size()];
                        for (int i = 0; i < values.size(); ++i) {
                            final int v = (values.get(i) == null ? 0 : (Integer) values.get(i));
                            affectedSubAccounts[i] = v;
                        }
                    }
                }
                mNotificationHandler.onNewTransaction(affectedSubAccounts);
            }
        });
    }

    private NettyWampConnectionConfig getNettyConfig() throws SSLException {
        final int TWO_MB = 2 * 1024 * 1024; // Max message size in bytes

        final NettyWampConnectionConfig.Builder configBuilder;
        configBuilder = new NettyWampConnectionConfig.Builder()
                                                     .withMaxFramePayloadLength(TWO_MB);

        if (Network.GAIT_WAMP_CERT_PINS != null && !isTorEnabled()) {
            final TrustManagerFactory tmf;
            tmf = new FingerprintTrustManagerFactorySHA256(Network.GAIT_WAMP_CERT_PINS);
            final SslContext ctx = SslContextBuilder.forClient().trustManager(tmf).build();
            configBuilder.withSslContext(ctx);
        }

        return configBuilder.build();
    }

    private String getUri() {
        if (isTorEnabled())
            return String.format("ws://%s/v2/ws/", Network.GAIT_ONION);
        return Network.GAIT_WAMP_URL;
    }

    private boolean isTorEnabled() {
        return mTorEnabled && mProxyAddress != null;
    }

    public ListenableFuture<Void> connect() {
        final SettableFuture<Void> rpc = SettableFuture.create();
        mScheduler.createWorker().schedule(new Action0() {
            @Override
            public void call() {
                final String wsuri = getUri();
                Log.i(TAG, "Proxy is configured " + mProxyAddress);
                Log.i(TAG, "Connecting to " + wsuri);

                final WampClientBuilder builder = new WampClientBuilder();
                final IWampConnectorProvider connectorProvider = new NettyWampClientConnectorProvider();
                try {
                    builder.withConnectorProvider(connectorProvider)
                            .withProxyAddress(mProxyAddress)
                            .withUri(wsuri)
                            .withRealm("realm1")
                            .withNrReconnects(0)
                            .withConnectionConfiguration(getNettyConfig());
                } catch (final ApplicationError | SSLException e) {
                    e.printStackTrace();
                    rpc.setException(e);
                    return;
                }

                try {
                    mConnection = builder.build();
                } catch (final Exception e) {
                    e.printStackTrace();
                    rpc.setException(new GAException(e.toString()));
                    return;
                }

                mConnection.statusChanged()
                    .observeOn(mScheduler)
                    .subscribe(new Action1<WampClient.State>() {

                        boolean initialDisconnectedStateSeen;
                        boolean connected;

                        @Override
                        public void call(final WampClient.State newStatus) {
                            if (newStatus instanceof WampClient.ConnectedState) {
                                // Client got connected to the remote router
                                // and the session was established
                                connected = true;
                                rpc.set(null);
                                return;
                            }

                            if (newStatus instanceof WampClient.DisconnectedState)
                                if (!initialDisconnectedStateSeen)
                                    // First state set is always 'disconnected'
                                    initialDisconnectedStateSeen = true;
                                else
                                    if (connected)
                                        // Client got disconnected from the remote router
                                        mNotificationHandler.onConnectionClosed(0);
                                    else {
                                        // or the last possible connect attempt failed
                                        final Throwable t = ((WampClient.DisconnectedState) newStatus).disconnectReason();
                                        if (t != null)
                                            rpc.setException(t);
                                        else
                                            rpc.setException(new GAException("Disconnected"));
                                    }
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(final Throwable t) {
                            t.printStackTrace();
                            Log.d(TAG, t.toString());
                        }
                    });
                try {
                    mConnection.open();
                } catch (final IllegalStateException e) {
                    // already disconnected
                    e.printStackTrace();
                }
            }
        });

        Futures.addCallback(rpc, new FutureCallback<Void>() {
            @Override
            public void onSuccess(final Void result) {
                clientSubscribe("blocks", Map.class, new EventHandler() {
                    @Override
                    public void onEvent(final String topicUri, final Object event) {
                        Log.i(TAG, "BLOCKS IS " + event.toString());
                        mNotificationHandler.onNewBlock(Integer.parseInt(((Map) event).get("count").toString()));
                    }
                });
                clientSubscribe("fee_estimates", Map.class, new EventHandler() {
                    @Override
                    public void onEvent(final String topicUri, final Object newFeeEstimates) {
                        Log.i(TAG, "FEE_ESTIMATES IS " + newFeeEstimates.toString());
                        mFeeEstimates = new JSONMap((Map) newFeeEstimates);
                    }
                });
            }

            @Override
            public void onFailure(final Throwable t) {
                t.printStackTrace();
            }
        }, mExecutor);

        return rpc;
    }

    private LoginData watchOnlyLoginImpl(final String username, final String password) throws Exception {
        final Map<String, Object> loginData;
        loginData = syncCall("login.watch_only_v2",  Map.class, "custom",
                             ImmutableMap.of("username", username, "password", password),
                             USER_AGENT);
        onAuthenticationComplete(loginData, null, username, password);  // requires receivingId to be set
        return mLoginData;
    }

    public void disableWatchOnly() throws Exception {
        syncCall("addressbook.disable_sync",  Void.class, "custom");
        mWatchOnlyPassword = null;
        mWatchOnlyUsername = null;
    }

    public boolean isWatchOnly() {
        return !TextUtils.isEmpty(mWatchOnlyUsername) && !TextUtils.isEmpty(mWatchOnlyPassword);
    }

    public void registerWatchOnly(final String username, final String password) throws Exception {

        syncCall("addressbook.sync_custom", Boolean.class, username , password);
        mWatchOnlyUsername = username;
    }

    public String getWatchOnlyUsername() throws Exception {
        if (mWatchOnlyUsername == null) {
            final Map<?, ?> sync_status = syncCall("addressbook.get_sync_status", Map.class);
            mWatchOnlyUsername = (String) sync_status.get("username");
        }
        return mWatchOnlyUsername;
    }

    public String getWatchOnlyPassword() {
        return mWatchOnlyPassword;
    }

    private LoginData loginImpl(final ISigningWallet signingWallet, final String deviceId) throws Exception {

        // FIXME: Unify this RPC call, this is ugly
        final Object[] args = signingWallet.getChallengeArguments();
        final String challengeString;
        if (args.length == 2)
            challengeString = syncCall((String) args[0], String.class, args[1]);
        else
            challengeString = syncCall((String) args[0], String.class, args[1], args[2]);

        final String[] challengePath = new String[1];
        final String[] signatures = signingWallet.signChallenge(challengeString, challengePath);
        final Object ret = syncCall("login.authenticate", Object.class, signatures,
                                    true, challengePath[0], deviceId, USER_AGENT);

        if (ret instanceof Boolean) {
            // FIXME: One RPC call should not have multiple return types
            throw new LoginFailed();
        }

        onAuthenticationComplete((Map <String, Object>) ret, signingWallet, null, null);  // requires receivingId to be set
        return mLoginData;
    }

    public ListenableFuture<LoginData> watchOnlylogin(final String username, final String password) {
        return mExecutor.submit(new Callable<LoginData>() {
            @Override
            public LoginData call() {
                try {
                    return watchOnlyLoginImpl(username, password);
                } catch (final Throwable t) {
                    throw Throwables.propagate(t);
                }
            }
        });
    }

    public ListenableFuture<LoginData> login(final ISigningWallet signingWallet, final String deviceId, final String mnemonic) {
        if (mnemonic != null)
            mMnemonic = mnemonic;
        return mExecutor.submit(new Callable<LoginData>() {
            @Override
            public LoginData call() {
                try {
                    return loginImpl(signingWallet, deviceId);
                } catch (final Throwable t) {
                    throw Throwables.propagate(t);
                }
            }
        });
    }

    public byte[] getPinPassword(final String pinIdentifier, final String pin) throws Exception {
        final String password = syncCall("pin.get_password", String.class, pin, pinIdentifier);
        return password.getBytes();
    }

    public JSONMap getNewAddress(final int subAccount, final String addrType) {
        try {
            final JSONMap m = new JSONMap((Map<String, Object>) syncCall("vault.fund", Map.class, subAccount, true, addrType));
            return m.mData == null ? null : m;
        } catch (final Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Map<String, Object> getMyTransactions(final String searchQuery, final int subAccount) throws Exception {
        return syncCall("txs.get_list_v2", Map.class, null, searchQuery, null, null, subAccount);
    }

    public PinData setPin(final String mnemonic, final String pin, final String deviceName) throws Exception {
        mMnemonic = mnemonic;

        // FIXME: set_pin_login could return the password as well, saving a
        // round-trip vs calling getPinPassword() below.
        final String pinIdentifier = syncCall("pin.set_pin_login", String.class, pin, deviceName);
        final byte[] password = getPinPassword(pinIdentifier, pin);
        return PinData.fromMnemonic(pinIdentifier, mnemonic, password);
    }

    public ListenableFuture<Map<?, ?>> prepareSweepSocial(final byte[] pubKey, final boolean useElectrum) {
        final Integer[] pubKeyObjs = new Integer[pubKey.length];
        for (int i = 0; i < pubKey.length; ++i)
            pubKeyObjs[i] = pubKey[i] & 0xff;
        return simpleCall("vault.prepare_sweep_social", Map.class,
                          new ArrayList<>(Arrays.asList(pubKeyObjs)), useElectrum);

    }

    public ListenableFuture<String> sendTransaction(final List<byte[]> signatures, final Object twoFacData) {
        final List<String> sigs = new ArrayList<>();
        for (final byte[] s : signatures)
            sigs.add(h(s));
        return simpleCall("vault.send_tx", null, sigs, twoFacData);
    }

    public ListenableFuture<Map<String, Object>>
    sendRawTransaction(final Transaction tx, final Map<String, Object> twoFacData,
                       final JSONMap privateData, final boolean returnTx) {
        final String txHex = h(tx.bitcoinSerialize());
        return simpleCall("vault.send_raw_tx", Map.class, txHex, twoFacData,
                          privateData == null ? null : privateData.mData, returnTx);
    }

    public ListenableFuture<List<byte[]>> signTransaction(final ISigningWallet signingWallet, final PreparedTransaction ptx) {
        return mExecutor.submit(new Callable<List<byte[]>>() {
            @Override
            public List<byte[]> call() {
                return signingWallet.signTransaction(ptx);
            }
        });
    }

    public Object getUserConfig(final String key) {
        return mLoginData.mUserConfig.get(key);
    }

    // Returns True if the user hasn't elected to use segwit yet
    public boolean isSegwitUnconfirmed() {
        return getUserConfig("use_segwit") == null;
    }

    // Returns True iff the user has elected to use segwit
    public boolean isSegwitEnabled() {
        return !isSegwitUnconfirmed() && (Boolean) getUserConfig("use_segwit");
    }

    private static <T> ByteArrayOutputStream serializeJSON(final T src) throws GAException {
        final ByteArrayOutputStream b = new ByteArrayOutputStream();
        try {
            new MappingJsonFactory().getCodec().writeValue(b, src);
        } catch (final IOException e) {
            throw new GAException(e.getMessage());
        }
        return b;
    }

    private static void updateMap(final Map<String, Object> dest, final Map<String, Object> src,
                                  final Set<String> keys) {
        for (final String k : keys)
            dest.put(k, src.get(k));
    }

    public ListenableFuture<Boolean> setUserConfig(final Map<String, Object> values, final boolean updateImmediately) {
        // Create updated JSON config for the RPC call
        final Map<String, Object> clonedConfig = new HashMap<>(mLoginData.mUserConfig);
        updateMap(clonedConfig, values, values.keySet());

        final String newJSON;
        try {
            newJSON = serializeJSON(clonedConfig).toString();
        } catch (final GAException e) {
            return Futures.immediateFailedFuture(e);
        }

        final Map<String, Object> oldValues = new HashMap<>();
        if (updateImmediately) {
            // Save old values and update current config
            updateMap(oldValues, mLoginData.mUserConfig, values.keySet());
            updateMap(mLoginData.mUserConfig, values, values.keySet());
        }

        final SettableFuture<Boolean> rpc = SettableFuture.create();
        final CallHandler handler = new CallHandler() {
            public void onResult(final Object result) {
                // Update local config if it wasn't updated previously
                if (!updateImmediately)
                    updateMap(mLoginData.mUserConfig, values, values.keySet());
                rpc.set(true);
            }
        };
        final ErrorHandler errHandler = new ErrorHandler() {
            public void onError(final GAException e) {
                Log.d(TAG, "updateAppearance failed: " + e.mMessage);
                // Restore local config if it was updated previously
                if (updateImmediately)
                    updateMap(mLoginData.mUserConfig, oldValues, oldValues.keySet());
            }
        };
        return clientCall(rpc, "login.set_appearance", Map.class, handler, errHandler, newJSON);
    }

    public ListenableFuture<Object> requestTwoFacCode(final String type, final String action, final Object data) {
        return simpleCall("twofactor.request_" + type, Object.class, action, data);
    }

    public ISigningWallet getSigningWallet() {
        return mHDParent;
    }

    public ListenableFuture<List<JSONMap>> getAllUnspentOutputs(final int confs, final Integer subAccount) {
         final ListenableFuture<ArrayList> rpc;
         rpc = simpleCall("txs.get_all_unspent_outputs", ArrayList.class,
                          confs, subAccount, "any");
         return Futures.transform(rpc, new Function<ArrayList, List<JSONMap>>() {
            @Override
            public List<JSONMap> apply(final ArrayList utxos) {
                return JSONMap.fromList(utxos);
            }
        });
    }

    private ListenableFuture<Transaction> transactionCall(final String procedure, final Object... args) {
        final SettableFuture<Transaction> rpc = SettableFuture.create();
        final CallHandler handler = new CallHandler() {
            public void onResult(final Object tx) {
                rpc.set(GaService.buildTransaction((String) tx));
            }
        };
        return clientCall(rpc, procedure, String.class, handler, args);
    }

    public ListenableFuture<Transaction> getRawUnspentOutput(final Sha256Hash txHash) {
        return transactionCall("txs.get_raw_unspent_output", txHash.toString());
    }

    public String getRawOutputHex(final Sha256Hash txHash) throws Exception {
        return syncCall("txs.get_raw_output", String.class, txHash.toString());
    }

    public ListenableFuture<Boolean> changeMemo(final String txHashHex, final String memo, final String memoType) {
        return simpleCall("txs.change_memo", Boolean.class, txHashHex, memo, memoType);
    }

    public ListenableFuture<Boolean> setPricingSource(final String currency, final String exchange) {
        return simpleCall("login.set_pricing_source", Boolean.class, currency, exchange);
    }

    public ListenableFuture<Boolean> initEnableTwoFac(final String type, final String details, final Map<?, ?> twoFacData) {
        return simpleCall("twofactor.init_enable_" + type, Boolean.class, details, twoFacData);
    }

    public ListenableFuture<Boolean> enableTwoFactor(final String type, final String code, final Object twoFacData) {
        if (twoFacData == null)
            return simpleCall("twofactor.enable_" + type, Boolean.class, code);
        return simpleCall("twofactor.enable_" + type, Boolean.class, code, twoFacData);
    }

    public Boolean disableTwoFactor(final String type, final Map<String, String> twoFacData) throws Exception {
        return syncCall("twofactor.disable_" + type, Boolean.class, twoFacData);
    }

    public JSONMap getSpendingLimits() throws Exception {
        return new JSONMap((Map<String, Object>) syncCall("login.get_spending_limits", Map.class));
    }

    public ListenableFuture<PaymentSession> fetchPaymentRequest(final String url) {
        final SettableFuture<PaymentSession> rpc = SettableFuture.create();
        final Request request = new Request.Builder().url(url)
                .addHeader("Accept", PaymentProtocol.MIMETYPE_PAYMENTREQUEST).build();

        mHttpClientBIP70.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(final Request request, final IOException e) {
                Log.d(TAG,"fetchPaymentRequest.onFailure: " +  e.toString());
                rpc.set(null);
            }

            @Override
            public void onResponse(final Response response) throws IOException {
                try {
                    final Protos.PaymentRequest paymentRequest = Protos.PaymentRequest.parseFrom(response.body().bytes());
                    rpc.set(new PaymentSession(paymentRequest, true));
                } catch (final PaymentProtocolException | InvalidProtocolBufferException e) {
                    Log.d(TAG, "fetchPaymentRequest.onResponse: " + e.toString());
                    rpc.set(null);
                }
            }
        });
        return rpc;
    }

    public static byte[] serializeProtobuf(final GeneratedMessage msg) {
        final byte[] byteArray = new byte[msg.getSerializedSize()];
        final CodedOutputStream codedOutputStream = CodedOutputStream.newInstance(byteArray);
        try {
            msg.writeTo(codedOutputStream);
        } catch (final IOException e) {
            Log.e(TAG, "failed to serialize message: " + e.toString());
            e.printStackTrace();
            return null;
        }
        return byteArray;
    }

    public ListenableFuture<PaymentProtocol.Ack> sendPayment(final PaymentSession paymentSession,
                                                             final List<Transaction> txns,
                                                             final Address refundAddr,
                                                             final String memo)
        throws IOException, PaymentProtocolException.InvalidNetwork,
            PaymentProtocolException.Expired, PaymentProtocolException.InvalidPaymentURL {

        Protos.Payment payment = paymentSession.getPayment(txns, refundAddr, memo);
        if (payment == null)
            return null;
        if (paymentSession.isExpired())
            throw new PaymentProtocolException.Expired("PaymentRequest is expired");
        URL url;
        try {
            url = new URL(paymentSession.getPaymentUrl());
        } catch (final MalformedURLException e) {
            throw new PaymentProtocolException.InvalidPaymentURL(e);
        }

        final byte[] paymentBytes;
        final SettableFuture<PaymentProtocol.Ack> rpc = SettableFuture.create();

        if ((paymentBytes = serializeProtobuf(payment)) == null) {
            rpc.set(null);
            return rpc;
        }

        final MediaType mediaType = MediaType.parse(PaymentProtocol.MIMETYPE_PAYMENT);
        final RequestBody body = RequestBody.create(mediaType, paymentBytes);

        final Request request = new Request.Builder()
                .url(url)
                .addHeader("Content-Type", PaymentProtocol.MIMETYPE_PAYMENT)
                .addHeader("Accept", PaymentProtocol.MIMETYPE_PAYMENTACK)
                .addHeader("Content-Length", Integer.toString(payment.getSerializedSize()))
                .post(body)
                .build();

        mHttpClientBIP70.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                rpc.set(null);
            }

            @Override
            public void onResponse(final Response response) {
                try {
                    Protos.PaymentACK paymentAck = Protos.PaymentACK.parseFrom(response.body().bytes());
                    rpc.set(PaymentProtocol.parsePaymentAck(paymentAck));
                } catch (final Exception e) {
                    rpc.set(null);
                }
            }
        });

        return rpc;
    }
}
