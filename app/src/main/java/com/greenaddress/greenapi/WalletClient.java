package com.greenaddress.greenapi;

import android.text.TextUtils;
import android.util.Log;

import com.blockstream.libwally.Wally;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.SettableFuture;
import com.greenaddress.greenbits.ui.BuildConfig;
import com.squareup.okhttp.OkHttpClient;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.codehaus.jackson.map.MappingJsonFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.RejectedExecutionException;

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

public class WalletClient {

    private static final String TAG = WalletClient.class.getSimpleName();
    private static final String GA_KEY = "GreenAddress.it HD wallet path";
    private static final String GA_PATH = "greenaddress_path";
    private static final String USER_AGENT = String.format("%s;%s;%s;%s",
            BuildConfig.VERSION_CODE, BuildConfig.BUILD_TYPE,
            android.os.Build.VERSION.SDK_INT, System.getProperty("os.arch"));

    private final Scheduler mScheduler = Schedulers.newThread();
    private final ListeningExecutorService mExecutor;
    private final INotificationHandler mNotificationHandler;
    private WampClient mConnection;
    private SocketAddress mProxy = null;
    private LoginData mLoginData;
    private ISigningWallet mHDParent;
    private String mWatchOnlyUsername = null;
    private String mWatchOnlyPassword = null;
    private String mMnemonics = null;
    private final OkHttpClient httpClient = new OkHttpClient();

    /**
     * Call handler.
     */
    public interface CallHandler {

        /**
         * Fired on successful completion of call.
         *
         * @param result     The RPC result transformed into the type that was specified in call.
         */
        void onResult(Object result);
    }

    public interface ErrorHandler {
        /**
         * Fired on call failure.
         *
         * @param uri   The URI or CURIE of the error that occurred.
         * @param err  A human readable description of the error.
         */
        void onError(String uri, String err);
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

    private void onCallError(final SettableFuture rpc, final String procedure,
                             final ErrorHandler errHandler,
                             final String uri, final String err) {
        Log.d(TAG, procedure + "->" + uri + ":" + err);
        if (errHandler != null)
            errHandler.onError(uri, err);
        else
            rpc.setException(new GAException(err));
    }

    private SettableFuture clientCall(final SettableFuture rpc,
                                      final String procedure, final Class result,
                                      final CallHandler handler, final ErrorHandler errHandler,
                                      Object... args) {
        final ObjectMapper mapper = new ObjectMapper();
        final ArrayNode argsNode = mapper.valueToTree(Arrays.asList(args));

        final Action1<Reply> replyHandler = new Action1<Reply>() {
            @Override
            public void call(final Reply reply) {
                final JsonNode node = reply.arguments().get(0);
                handler.onResult(mapper.convertValue(node, result));
            }
        };

        final Action1<Throwable> errorHandler = new Action1<Throwable>() {
            @Override
            public void call(final Throwable err) {

                if (err instanceof ApplicationError) {
                    final ArrayNode a = ((ApplicationError) err).arguments();
                    if (a != null && a.size() >= 2) {
                        onCallError(rpc, procedure, errHandler, a.get(0).asText(), a.get(1).asText());
                        return;
                    }
                }
                onCallError(rpc, procedure, errHandler, err.toString(), err.toString());
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
        onCallError(rpc, procedure, errHandler, "not connected", "not connected");
        return rpc;
    }

    private SettableFuture clientCall(final SettableFuture rpc,
                                      final String procedure, final Class result,
                                      final CallHandler handler, Object... args) {
        return clientCall(rpc, procedure, result, handler, null, args);
    }

    private <V> ListenableFuture<V> simpleCall(final String procedure, final Class result, Object... args) {
        final SettableFuture<V> rpc = SettableFuture.create();
        final CallHandler handler = result == null ? stringHandler(rpc) : simpleHandler(rpc);
        final Class resultClass = result == null ? String.class : result;
        return clientCall(rpc, procedure, resultClass, handler, args);
    }

    private <T> T syncCall(final String procedure, final Class result,
                           Object... args) throws Exception {

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
            return (T)mapper.convertValue(node, result);
        } catch (final RejectedExecutionException e) {
            throw new GAException("rejected");
        }
        catch (final Exception e) {
            Log.d(TAG, "Sync RPC exception: (" + procedure + ")->" + e.toString());
            if (e instanceof ApplicationError) {
                final ArrayNode a = ((ApplicationError) e).arguments();
                if (a != null && a.size() >= 2) {
                    // Throw the actual error message and ignore the URI
                    throw new GAException(a.get(1).asText());
                }
            }
            throw new GAException(e.toString());
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
            public void call(final Throwable throwable) {
                Log.w(TAG, throwable);
                Log.i(TAG, "Subscribe failed (" + topic + "): " + throwable.toString());
            }
        });
    }

    public WalletClient(final INotificationHandler notificationHandler, final ListeningExecutorService es) {
        mNotificationHandler = notificationHandler;
        mExecutor = es;
    }

    public void setProxy(final String host, final String port) {
        if (host != null && !host.equals("") && port != null && !port.equals("")) {
            mProxy = new InetSocketAddress(host, Integer.parseInt(port));
            httpClient.setProxy(new Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved(host, Integer.parseInt(port))));
        } else {
            mProxy = null;
            httpClient.setProxy(null);
        }
    }

    public LoginData getLoginData() {
        return mLoginData;
    }

    private static byte[] mnemonicToPath(final String mnemonic) {
        final byte[] hash = CryptoHelper.pbkdf2_hmac_sha512(mnemonic.getBytes(), GA_PATH.getBytes());
        return Wally.hmac_sha512(GA_KEY.getBytes(), hash);
    }

    private static byte[] extendedKeyToPath(final byte[] publicKey, final byte[] chainCode) {
        final byte[] data = new byte[publicKey.length + chainCode.length];
        System.arraycopy(chainCode, 0, data, 0, chainCode.length);
        System.arraycopy(publicKey, 0, data, chainCode.length, publicKey.length);
        return Wally.hmac_sha512(GA_KEY.getBytes(), data);
    }

    public String getMnemonics() {
        return mMnemonics;
    }

    public void disconnect() {
        // FIXME: Server should handle logout without having to disconnect
        mLoginData = null;
        mMnemonics = null;
        mWatchOnlyUsername = null;

        mHDParent = null;

        if (mConnection != null) {
            mConnection.close();
            mConnection = null;
        }
    }

    public ListenableFuture<LoginData> loginRegister(final ISigningWallet signingWallet,
                                                     final byte[] masterPublicKey, final byte[] masterChaincode,
                                                     final String mnemonics, final String deviceId) {
        mMnemonics = mnemonics;
        return loginRegisterImpl(signingWallet, masterPublicKey, masterChaincode,
                                 mnemonicToPath(mnemonics), USER_AGENT, deviceId);
    }

    public ListenableFuture<LoginData> loginRegister(final ISigningWallet signingWallet,
                                                     final byte[] masterPublicKey, final byte[] masterChaincode,
                                                     final byte[] pathPublicKey, final byte[] pathChaincode,
                                                     final String deviceId) {
        return loginRegisterImpl(signingWallet, masterPublicKey, masterChaincode,
                                 extendedKeyToPath(pathPublicKey, pathChaincode),
                                 String.format("%s HW", USER_AGENT), deviceId);
    }

    public ListenableFuture<LoginData> loginRegisterImpl(final ISigningWallet signingWallet,
                                                         final byte[] masterPublicKey, final byte[] masterChaincode,
                                                         final byte[] path,final String agent,  final String deviceId) {

        final SettableFuture<ISigningWallet> rpc = SettableFuture.create();
        clientCall(rpc, "login.register", Boolean.class, new CallHandler() {
            public void onResult(final Object result) {
                rpc.set(signingWallet);
            }
        }, Wally.hex_from_bytes(masterPublicKey), Wally.hex_from_bytes(masterChaincode), agent);

        final AsyncFunction<ISigningWallet, LoginData> fn = new AsyncFunction<ISigningWallet, LoginData>() {
            @Override
            public ListenableFuture<LoginData> apply(final ISigningWallet signingWallet) throws Exception {
                return login(signingWallet, deviceId);
            }
        };

        final Function<LoginData, LoginData> postFn = new Function<LoginData, LoginData>() {
            @Override
            public LoginData apply(LoginData loginData) {
                try {
                    syncCall("login.set_gait_path", Void.class, Wally.hex_from_bytes(path));
                } catch (final Exception e) {
                    throw new RuntimeException(e);
                }
                loginData.setGaUserPath(path);
                HDKey.resetCache(loginData.gaUserPath);
                return loginData;
            }
        };

        return Futures.transform(Futures.transform(rpc, fn, mExecutor), postFn, mExecutor);
    }

    public ListenableFuture<Map<?, ?>> getSubaccountBalance(final int subAccount) {
        return simpleCall("txs.get_balance", Map.class, subAccount);
    }

    public ListenableFuture<Map<?, ?>> getTwoFacConfig() {
        return simpleCall("twofactor.get_config", Map.class);
    }

    public ListenableFuture<Map<?, ?>> getAvailableCurrencies() {
        return simpleCall("login.available_currencies", Map.class);
    }

    private void onAuthenticationComplete(final Map<?,?> loginData, final ISigningWallet wallet, final String username, final String password) throws IOException {
        mLoginData = new LoginData(loginData);
        mHDParent = wallet;
        mWatchOnlyUsername = username;
        mWatchOnlyPassword = password;

        if (mLoginData.rbf && getUserConfig("replace_by_fee") == null) {
            // Enable rbf if server supports it and not disabled by user explicitly
            setUserConfig("replace_by_fee", Boolean.TRUE, false);
        }

        clientSubscribe("txs.wallet_" + mLoginData.receivingId, Map.class, new EventHandler() {
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

    public ListenableFuture<Void> connect() {
        final SettableFuture<Void> rpc = SettableFuture.create();
        mScheduler.createWorker().schedule(new Action0() {
            @Override
            public void call() {
                final String wsuri = Network.GAIT_WAMP_URL;
                Log.i(TAG, "Connecting to " + wsuri);
                final WampClientBuilder builder = new WampClientBuilder();
                final IWampConnectorProvider connectorProvider = new NettyWampClientConnectorProvider();
                try {
                    builder.withConnectorProvider(connectorProvider)
                            .withProxyAddress(mProxy)
                            .withUri(wsuri)
                            .withRealm("realm1")
                            .withNrReconnects(0);
                } catch (final ApplicationError e) {
                    e.printStackTrace();
                    rpc.setException(e);
                    return;
                }

                // FIXME: add proxy to wamp connection
                // final String wstoruri = String.format("ws://%s/ws/inv", Network.GAIT_ONION);
                // final String ws2toruri = String.format("ws://%s/v2/ws", Network.GAIT_ONION);

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

                        boolean initialDisconnectedStateSeen = false;
                        boolean connected = false;

                        @Override
                        public void call(final WampClient.State newStatus) {
                            if (newStatus instanceof WampClient.ConnectedState) {
                                // Client got connected to the remote router
                                // and the session was established
                                connected = true;
                                rpc.set(null);
                            } else if (newStatus instanceof WampClient.DisconnectedState) {
                                if (!initialDisconnectedStateSeen) {
                                    // First state set is always 'disconnected'
                                    initialDisconnectedStateSeen = true;
                                } else {
                                    if (connected) {
                                        // Client got disconnected from the remote router
                                        mNotificationHandler.onConnectionClosed(0);
                                    } else {
                                        // or the last possible connect attempt failed
                                        rpc.setException(new GAException("Disconnected"));
                                    }
                                }
                            }
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(final Throwable throwable) {
                            Log.d(TAG, throwable.toString());
                        }
                    });
                try {
                    mConnection.open();
                } catch (final IllegalStateException e) {
                    // already disconnected
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
                    public void onEvent(final String topicUri, final Object event) {
                        Log.i(TAG, "FEE_ESTIMATES IS " + event.toString());
                        if (mLoginData != null) {
                            mLoginData.feeEstimates = (Map) event;
                        }
                    }
                });
            }

            @Override
            public void onFailure(final Throwable t) {

            }
        }, mExecutor);


        return rpc;
    }

    public ListenableFuture<LoginData> login(final ISigningWallet signingWallet, final String mnemonics, final String deviceId) {
        mMnemonics = mnemonics;
        return login(signingWallet, deviceId);
    }

    private LoginData watchOnlyLoginImpl(final String username, final String password) throws Exception {
        final Map<String, String> credentials = new HashMap<>(2);
        credentials.put("username", username);
        credentials.put("password", password);
        final Object ret = syncCall("login.watch_only",  Object.class, "custom", credentials, false);
        final Map<?, ?> json;
        json = new MappingJsonFactory().getCodec().readValue((String)ret, Map.class);
        onAuthenticationComplete(json, null, username, password);  // requires receivingId to be set
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

    public boolean registerWatchOnly(final String username, final String password) throws Exception {

        final boolean res = syncCall("addressbook.sync_custom", Boolean.class, username , password);
        if (res)
            mWatchOnlyUsername = username;
        return res;
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

    private LoginData loginImpl(final ISigningWallet signingWallet, final String deviceId) throws Exception, LoginFailed {

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

        onAuthenticationComplete((Map <?,?>) ret, signingWallet, null, null);  // requires receivingId to be set
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

    public ListenableFuture<LoginData> login(final ISigningWallet signingWallet, final String deviceId) {
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

    public Map<?, ?> getMyTransactions(final int subAccount) throws Exception {
        return syncCall("txs.get_list_v2", Map.class, null, null, null, null, subAccount);
    }

    public ListenableFuture<Map> getNewAddress(final int subAccount) {
        return simpleCall("vault.fund", Map.class, subAccount, true);
    }

    public PinData setPin(final String mnemonic, final String pin, final String deviceName) throws Exception {
        mMnemonics = mnemonic;

        // FIXME: set_pin_login could return the password as well, saving a
        // round-trip vs calling getPinPassword() below.
        final String pinIdentifier = syncCall("pin.set_pin_login", String.class, pin, deviceName);
        final byte[] password = getPinPassword(pinIdentifier, pin);
        return PinData.fromMnemonic(pinIdentifier, mnemonic, password);
    }

    public ListenableFuture<PreparedTransaction> prepareTx(final long satoshis, final String destAddress, final String feesMode, final Map<String, Object> privateData) {
        final SettableFuture<PreparedTransaction.PreparedData> rpc = SettableFuture.create();
        clientCall(rpc, "vault.prepare_tx", Map.class, new CallHandler() {
            public void onResult(final Object prepared) {
                rpc.set(new PreparedTransaction.PreparedData((Map)prepared, privateData, mLoginData.subAccounts, httpClient));
            }
        }, satoshis, destAddress, feesMode, privateData);

        return processPreparedTx(rpc);
    }

    private ListenableFuture<PreparedTransaction> processPreparedTx(final ListenableFuture<PreparedTransaction.PreparedData> rpc) {
        return Futures.transform(rpc, new Function<PreparedTransaction.PreparedData, PreparedTransaction>() {
            @Override
            public PreparedTransaction apply(final PreparedTransaction.PreparedData ptxData) {
                return new PreparedTransaction(ptxData);
            }
        }, mExecutor);
    }

    public ListenableFuture<Map<?, ?>> processBip70URL(final String url) {
        return simpleCall("vault.process_bip0070_url", Map.class, url);
    }

    public ListenableFuture<PreparedTransaction> preparePayreq(final Coin amount, Map<?, ?> data, final Map<String, Object> privateData) {

        final SettableFuture<PreparedTransaction.PreparedData> rpc = SettableFuture.create();


        final Map dataClone = new HashMap<>();
        for (final Object k : data.keySet())
            dataClone.put(k, data.get(k));

        if (privateData != null && privateData.containsKey("subaccount"))
            dataClone.put("subaccount", privateData.get("subaccount"));

        clientCall(rpc, "vault.prepare_payreq", Map.class, new CallHandler() {
            public void onResult(final Object prepared) {
                rpc.set(new PreparedTransaction.PreparedData((Map) prepared, privateData, mLoginData.subAccounts, httpClient));
            }
        }, amount.longValue(), dataClone, privateData);

        return processPreparedTx(rpc);
    }

    public ListenableFuture<Map<?, ?>> prepareSweepSocial(final byte[] pubKey, final boolean useElectrum) {
        final Integer[] pubKeyObjs = new Integer[pubKey.length];
        for (int i = 0; i < pubKey.length; ++i)
            pubKeyObjs[i] = pubKey[i] & 0xff;
        return simpleCall("vault.prepare_sweep_social", Map.class,
                          new ArrayList<>(Arrays.asList(pubKeyObjs)), useElectrum);

    }

    public ListenableFuture<String> sendTransaction(final List<byte[]> signatures, final Object TfaData) {
        final List<String> args = new ArrayList<>();
        for (final byte[] s : signatures)
            args.add(Wally.hex_from_bytes(s));
        return simpleCall("vault.send_tx", null, args, TfaData);
    }

    public ListenableFuture<Map<String, Object>> sendRawTransaction(Transaction tx, Map<String, Object> twoFacData, final boolean returnErrorUri) {
        final SettableFuture<Map<String, Object>> rpc = SettableFuture.create();
        final ErrorHandler errHandler = new ErrorHandler() {
            public void onError(final String uri, final String err) {
                rpc.setException(new GAException(returnErrorUri ? uri : err));
            }
        };
        final String txStr =  Wally.hex_from_bytes(tx.bitcoinSerialize());
        return clientCall(rpc, "vault.send_raw_tx", Map.class, simpleHandler(rpc), errHandler, txStr, twoFacData);
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
        return mLoginData.userConfig.get(key);
    }

    private <T> ByteArrayOutputStream serializeJSON(T src) throws GAException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        try {
            new MappingJsonFactory().getCodec().writeValue(b, src);
        } catch (final IOException e) {
            throw new GAException(e.getMessage());
        }
        return b;
    }

    /**
     * @param updateImmediately whether to not wait for server to reply before updating
     *                          the value in local settings dict (set false to wait)
     */
    public ListenableFuture<Boolean> setUserConfig(final String key, final Object value, final boolean updateImmediately) {
        final Object oldValue = getUserConfig(key);
        if (updateImmediately)
            mLoginData.userConfig.put(key, value);

        final Map<String, Object> clonedConfig = new HashMap<>(mLoginData.userConfig);
        clonedConfig.put(key, value);
        final String newJSON;
        try {
            newJSON = serializeJSON(clonedConfig).toString();
        } catch (final GAException e) {
            if (updateImmediately)
                mLoginData.userConfig.put(key, oldValue); // Restore
            return Futures.immediateFailedFuture(e);
        }

        final SettableFuture<Boolean> rpc = SettableFuture.create();
        final CallHandler handler = new CallHandler() {
            public void onResult(final Object result) {
                if (!updateImmediately)
                    mLoginData.userConfig.put(key, value);
                rpc.set(true);
            }
        };
        final ErrorHandler errHandler = new ErrorHandler() {
            public void onError(final String uri, final String err) {
                Log.d(TAG, "updateAppearance failed: " + err);
                if (updateImmediately)
                    mLoginData.userConfig.put(key, oldValue); // Restore
                rpc.setException(new GAException(err));
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

    public ListenableFuture<ArrayList> getAllUnspentOutputs(int confs, Integer subAccount) {
        return simpleCall("txs.get_all_unspent_outputs", ArrayList.class, confs, subAccount);
    }

    private ListenableFuture<Transaction> transactionCall(final String procedure, Object... args) {
        final SettableFuture<Transaction> rpc = SettableFuture.create();
        final CallHandler handler = new CallHandler() {
            public void onResult(final Object tx) {
                rpc.set(new Transaction(Network.NETWORK, Wally.hex_to_bytes((String) tx)));
            }
        };
        return clientCall(rpc, procedure, String.class, handler, args);
    }

    public ListenableFuture<Transaction> getRawUnspentOutput(final Sha256Hash txHash) {
        return transactionCall("txs.get_raw_unspent_output", txHash.toString());
    }

    public ListenableFuture<Transaction> getRawOutput(final Sha256Hash txHash) {
        return transactionCall("txs.get_raw_output", txHash.toString());
    }

    public ListenableFuture<Boolean> changeMemo(final String txhash, final String memo) {
        return simpleCall("txs.change_memo", Boolean.class, txhash, memo);
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

    public ListenableFuture<Boolean> disableTwoFac(final String type, final Map<String, String> twoFacData) {
        return simpleCall("twofactor.disable_" + type, Boolean.class, twoFacData);
    }
}
