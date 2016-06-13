package com.greenaddress.greenapi;

import android.util.Base64;
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
import com.greenaddress.greenbits.wallets.TrezorHWWallet;
import com.squareup.okhttp.OkHttpClient;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.Utils;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.script.Script;
import org.codehaus.jackson.map.MappingJsonFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.RejectedExecutionException;

import javax.annotation.Nullable;

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
    private String mMnemonics = null;

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
         * @param errorUri   The URI or CURIE of the error that occurred.
         * @param errorDesc  A human readable description of the error.
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
        final StringBuilder b = new StringBuilder();
        b.append(procedure).append("->").append(uri).append(":").append(err);
        Log.d(TAG, b.toString());
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
        final byte[] bytes = mnemonic.getBytes();
        final byte[] hashed = Wally.pbkdf2_hmac_sha512(bytes, GA_PATH.getBytes(), 0, 2048);
        return Wally.hmac_sha512(GA_KEY.getBytes(), hashed);
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

        mHDParent = null;

        if (mConnection != null) {
            mConnection.close();
            mConnection = null;
        }
    }

    public boolean canLogin() {
        return mHDParent != null;
    }

    private final OkHttpClient httpClient = new OkHttpClient();

    public ListenableFuture<LoginData> loginRegister(final String mnemonics, final String deviceId) {

        final SettableFuture<DeterministicKey> rpc = SettableFuture.create();
        final DeterministicKey master = mnemonicToMasterKey(mnemonics);
        final String hexMasterPublicKey = Wally.hex_from_bytes(master.getPubKey());
        final String hexChainCode = Wally.hex_from_bytes(master.getChainCode());
        clientCall(rpc, "login.register", Boolean.class, new CallHandler() {
            public void onResult(final Object result) {
                rpc.set(master);
            }
        }, hexMasterPublicKey, hexChainCode, USER_AGENT);

        final AsyncFunction<DeterministicKey, LoginData> registrationToLogin = new AsyncFunction<DeterministicKey, LoginData>() {
            @Override
            public ListenableFuture<LoginData> apply(final DeterministicKey input) throws Exception {
                return login(new Bip32Wallet(input), deviceId);
            }
        };

        final AsyncFunction<LoginData, LoginData> loginToSetPathPostLogin = new AsyncFunction<LoginData, LoginData>() {
            @Override
            public ListenableFuture<LoginData> apply(final LoginData input) throws Exception {
                mMnemonics = mnemonics;
                return setupPath(mnemonics, input);
            }
        };

        return Futures.transform(Futures.transform(rpc, registrationToLogin, mExecutor),
                                 loginToSetPathPostLogin, mExecutor);
    }


    public ListenableFuture<LoginData> loginRegister(final ISigningWallet signingWallet, final byte[] masterPublicKey, final byte[] masterChaincode, final byte[] pathPublicKey, final byte[] pathChaincode, final String deviceId) {

        final SettableFuture<ISigningWallet> rpc = SettableFuture.create();
        final String hexMasterPublicKey = Wally.hex_from_bytes(masterPublicKey);
        final String hexChainCode = Wally.hex_from_bytes(masterChaincode);

        clientCall(rpc, "login.register", Boolean.class, new CallHandler() {
            public void onResult(final Object result) {
                rpc.set(signingWallet);
            }
        }, hexMasterPublicKey, hexChainCode, String.format("%s HW", USER_AGENT));


        final AsyncFunction<ISigningWallet, LoginData> registrationToLogin = new AsyncFunction<ISigningWallet, LoginData>() {
            @Override
            public ListenableFuture<LoginData> apply(final ISigningWallet input) throws Exception {
                return login(input, deviceId);
            }
        };

        final AsyncFunction<LoginData, LoginData> loginToSetPathPostLogin = new AsyncFunction<LoginData, LoginData>() {
            @Override
            public ListenableFuture<LoginData> apply(final LoginData input) throws Exception {
                return setupPathBTChip(extendedKeyToPath(pathPublicKey, pathChaincode), input);
            }
        };

        return Futures.transform(Futures.transform(rpc, registrationToLogin, mExecutor),
                                 loginToSetPathPostLogin, mExecutor);
    }

    private ListenableFuture<LoginData> setupPathImpl(final byte[] path, final LoginData loginData) {
        final SettableFuture<LoginData> rpc = SettableFuture.create();
        return clientCall(rpc, "login.set_gait_path", Void.class, new CallHandler() {
            public void onResult(final Object result) {
                loginData.setGaitPath(path);
                HDKey.resetCache(loginData.gaitPath);
                rpc.set(loginData);
            }
        }, Wally.hex_from_bytes(path));
    }

    private ListenableFuture<LoginData> setupPath(final String mnemonics, final LoginData loginData) {
        return setupPathImpl(mnemonicToPath(mnemonics), loginData);
    }

    private ListenableFuture<LoginData> setupPathBTChip(final byte[] path, final LoginData loginData) {
        return setupPathImpl(path, loginData);
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

    private void subscribeToWallet() {
        clientSubscribe("txs.wallet_" + mLoginData.receivingId, Map.class, new EventHandler() {
            @Override
            public void onEvent(final String topicUri, final Object event) {

                final Map<?, ?> res = (Map) event;
                final Object subaccounts = res.get("subaccounts");

                final List<Integer> subaccountsList = new ArrayList<>();
                if (subaccounts instanceof Number)
                    subaccountsList.add(((Number) subaccounts).intValue());
                else {
                    final ArrayList values = (ArrayList) subaccounts;
                    if (values != null)
                        for (final Object v: values)
                            subaccountsList.add(v == null ? 0 : (Integer) v);
                }
                mNotificationHandler.onNewTransaction(subaccountsList);
            }
        });
    }

    public ListenableFuture<Void> connect() {
        final SettableFuture<Void> rpc = SettableFuture.create();
        mScheduler.createWorker().schedule(new Action0() {
            @Override
            public void call() {
                final String wsuri = Network.GAIT_WAMP_URL;
                final WampClientBuilder builder = new WampClientBuilder();
                final IWampConnectorProvider connectorProvider = new NettyWampClientConnectorProvider();
                try {
                    builder.withConnectorProvider(connectorProvider)
                            .withProxyAddress(mProxy)
                            .withUri(wsuri)
                            .withRealm("realm1")
                            .withNrReconnects(0);
                } catch (final ApplicationError e) {
                    rpc.setException(e);
                    return;
                }

                // FIXME: add proxy to wamp connection
                // final String wstoruri = String.format("ws://%s/ws/inv", Network.GAIT_ONION);
                // final String ws2toruri = String.format("ws://%s/v2/ws", Network.GAIT_ONION);

                try {
                    mConnection = builder.build();
                } catch (final Exception e) {
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
            public void onSuccess(@Nullable final Void result) {
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

    private DeterministicKey mnemonicToMasterKey(final String mnemonic) {
        final byte[] seed = CryptoHelper.mnemonic_to_seed(mnemonic);
        return HDKey.createMasterKeyFromSeed(seed);
    }

    public ListenableFuture<LoginData> login(final String mnemonics, final String deviceId) {
        mMnemonics = mnemonics;
        final DeterministicKey master = mnemonicToMasterKey(mnemonics);
        return login(new Bip32Wallet(master), deviceId);
    }

    public ListenableFuture<LoginData> login(final String deviceId) {
        return login(mHDParent, deviceId);
    }

    private LoginData loginImpl(final ISigningWallet signingWallet, final String deviceId) throws Exception, LoginFailed {

        final String address = new Address(Network.NETWORK, signingWallet.getIdentifier()).toString();
        final String challengeString;
        if (signingWallet.canSignHashes())
            challengeString = syncCall("login.get_challenge", String.class, address);
        else
            challengeString = syncCall("login.get_trezor_challenge", String.class, address,
                                       !(signingWallet instanceof TrezorHWWallet));

        final String[] challengePath = new String[1];
        final String[] signatures = signingWallet.signChallenge(challengeString, challengePath);

        final Object ret = syncCall("login.authenticate", Object.class, signatures,
                                    true, challengePath[0], deviceId, USER_AGENT);

        if (ret instanceof Boolean) {
            // FIXME: One RPC call should not have multiple return types
            throw new LoginFailed();
        }

        mLoginData = new LoginData((Map) ret);
        subscribeToWallet();  // requires receivingId to be set
        mHDParent = signingWallet;

        if (mLoginData.rbf && getUserConfig("replace_by_fee") == null) {
            // Enable rbf if server supports it and not disabled by user explicitly
            setUserConfig("replace_by_fee", Boolean.TRUE, false);
        }
        return mLoginData;
    }

    public ListenableFuture<LoginData> login(final ISigningWallet signingWallet, final String deviceId) {
        return mExecutor.submit(new Callable<LoginData>() {
            @Override
            public LoginData call() {
                try {
                    return loginImpl(signingWallet, deviceId);
                } catch (Throwable t) {
                    throw Throwables.propagate(t);
                }
            }
        });
    }

    public ListenableFuture<LoginData> login(final PinData data, final String pin, final String deviceId) {
        final SettableFuture<DeterministicKey> rpc = SettableFuture.create();
        clientCall(rpc, "pin.get_password", String.class, new CallHandler() {
            public void onResult(final Object pass) {
                final String[] split = data.encrypted.split(";");

                try {
                    final byte[] passBytes = pass.toString().getBytes();
                    final byte[] hashed;
                    hashed = Wally.pbkdf2_hmac_sha512(passBytes, split[0].getBytes(), 0, 2048);

                    final byte[] truncated = Arrays.copyOf(hashed, 32);

                    final String decrypted = new String(CryptoHelper.decrypt_aes_cbc(
                            Base64.decode(split[1], Base64.NO_WRAP), truncated));

                    final Map<String, String> json = new MappingJsonFactory().getCodec().readValue(
                            decrypted, Map.class);

                    mMnemonics = json.get("mnemonic");
                    rpc.set(HDKey.createMasterKeyFromSeed(Wally.hex_to_bytes(json.get("seed"))));
                } catch (final IOException e) {
                    rpc.setException(e);
                }
            }
        }, pin, data.ident);


        final AsyncFunction<DeterministicKey, LoginData> connectToLogin = new AsyncFunction<DeterministicKey, LoginData>() {
            @Override
            public ListenableFuture<LoginData> apply(final DeterministicKey input) {
                return login(new Bip32Wallet(input), deviceId);
            }
        };

        return Futures.transform(rpc, connectToLogin, mExecutor);
    }

    public Map<?, ?> getMyTransactions(final Integer subaccount) throws Exception {
        return syncCall("txs.get_list_v2", Map.class, null, null, null, null, subaccount);
    }

    public ListenableFuture<Map> getNewAddress(final int subaccount) {
        return simpleCall("vault.fund", Map.class, subaccount, true);
    }

    private ListenableFuture<PinData> getPinData(final String pin, final SetPinData setPinData) {
        final SettableFuture<PinData> rpc = SettableFuture.create();
        return clientCall(rpc, "pin.get_password", String.class, new CallHandler() {
            public void onResult(final Object password) {
                try {
                    final byte[] salt = CryptoHelper.randomBytes(16);
                    final byte[] passBytes = password.toString().getBytes();
                    final byte[] hashed;
                    hashed = Wally.pbkdf2_hmac_sha512(passBytes, Base64.encode(salt, Base64.NO_WRAP), 0, 2048);
                    final byte[] truncated = Arrays.copyOf(hashed, 32);
                    final byte[] aes_cbc = CryptoHelper.encrypt_aes_cbc(setPinData.json, truncated);
                    final String clob = String.format("%s;%s", Base64.encodeToString(salt,
                            Base64.NO_WRAP), Base64.encodeToString(aes_cbc,
                            Base64.NO_WRAP));

                    rpc.set(new PinData(setPinData.ident, clob));

                } catch (final IllegalArgumentException e) {
                    rpc.setException(e);
                }
            }
        }, pin, setPinData.ident);
    }

    private ListenableFuture<SetPinData> setPinLogin(final String mnemonic, final byte[] seed, final String pin, final String device_name) {
        final SettableFuture<SetPinData> rpc = SettableFuture.create();

        mMnemonics = mnemonic;
        final Map<String, String> out = new HashMap<>();
        out.put("mnemonic", mnemonic);
        out.put("seed", Wally.hex_from_bytes(seed));
        out.put("path_seed", Wally.hex_from_bytes(mnemonicToPath(mnemonic)));

        try {
            final byte[] info = serializeJSON(out).toByteArray();
            clientCall(rpc, "pin.set_pin_login", String.class, new CallHandler() {
                public void onResult(final Object ident) {
                    rpc.set(new SetPinData(info, ident.toString()));
                }
            }, pin, device_name);
        } catch (final GAException e) {
            rpc.setException(e);
        }
        return rpc;
    }

    public ListenableFuture<PinData> setPin(final byte[] seed, final String mnemonic, final String pin, final String device_name) {
        return Futures.transform(setPinLogin(mnemonic, seed, pin, device_name), new AsyncFunction<SetPinData, PinData>() {
            @Override
            public ListenableFuture<PinData> apply(final SetPinData pinData) throws Exception {
                return getPinData(pin, pinData);
            }
        }, mExecutor);
    }

    public ListenableFuture<PreparedTransaction> prepareTx(final long satoshis, final String destAddress, final String feesMode, final Map<String, Object> privateData) {
        final SettableFuture<PreparedTransaction.PreparedData> rpc = SettableFuture.create();
        clientCall(rpc, "vault.prepare_tx", Map.class, new CallHandler() {
            public void onResult(final Object prepared) {
                rpc.set(new PreparedTransaction.PreparedData((Map)prepared, privateData, mLoginData.subaccounts, httpClient));
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

        for (final Object tempKey : data.keySet()) {
            dataClone.put(tempKey, data.get(tempKey));
        }
        final Object key = "subaccount";

        if (privateData != null && privateData.containsKey(key)) {
            dataClone.put(key, privateData.get(key));
        }

        clientCall(rpc, "vault.prepare_payreq", Map.class, new CallHandler() {
            public void onResult(final Object prepared) {
                rpc.set(new PreparedTransaction.PreparedData((Map) prepared, privateData, mLoginData.subaccounts, httpClient));
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

    public ListenableFuture<String> sendTransaction(final List<String> signatures, final Object TfaData) {
        return simpleCall("vault.send_tx", null, signatures, TfaData);
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

    private List<String> convertSigs(final List<ECKey.ECDSASignature> sigs) {
        final List<String> result = new LinkedList<>();
        for (final ECKey.ECDSASignature sig : sigs) {
            final TransactionSignature txSig;
            txSig = new TransactionSignature(sig, Transaction.SigHash.ALL, false);
            result.add(Wally.hex_from_bytes(txSig.encodeToBitcoin()));
        }
        return result;
    }

    private List<String> signTransactionHashes(final PreparedTransaction ptx) {
        final Transaction t = ptx.decoded;
        final List<TransactionInput> txInputs = t.getInputs();
        final List<Output> prevOuts = ptx.prev_outputs;
        final List<String> signatures = new ArrayList<>(txInputs.size());
        final SettableFuture<List<String>> rpc = SettableFuture.create();

        final List<ECKey.ECDSASignature> sigs = new LinkedList<>();

        for (int i = 0; i < txInputs.size(); ++i) {
            final Output prevOut = prevOuts.get(i);

            ISigningWallet parent = mHDParent;
            if (prevOut.subaccount != null && prevOut.subaccount != 0)
                parent = parent.derive(ISigningWallet.HARDENED | 3)
                               .derive(ISigningWallet.HARDENED | prevOut.subaccount);

            final ISigningWallet child = parent.derive(prevOut.branch)
                                               .derive(prevOut.pointer);

            final Script script = new Script(Wally.hex_to_bytes(prevOut.script));
            final Sha256Hash hash;
            if (prevOut.scriptType.equals(14)) {
                hash = t.hashForSignatureV2(i, script.getProgram(), Coin.valueOf(prevOut.value), Transaction.SigHash.ALL, false);
            } else {
                hash = t.hashForSignature(i, script.getProgram(), Transaction.SigHash.ALL, false);
            }
            sigs.add(child.signHash(hash.getBytes()));
        }
        return convertSigs(sigs);
    }

    public ListenableFuture<List<String>> signTransaction(final PreparedTransaction ptx) {
        final boolean canSignHashes = mHDParent.canSignHashes();
        return mExecutor.submit(new Callable<List<String>>() {
            @Override
            public List<String> call() {
                if (canSignHashes)
                    return signTransactionHashes(ptx);
                return convertSigs(mHDParent.signTransaction(ptx));
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

    public boolean requiresPrevoutRawTxs() {
        return mHDParent.requiresPrevoutRawTxs();
    }

    public boolean isTrezorHWWallet() {
        return mHDParent instanceof TrezorHWWallet;
    }

    public DeterministicKey getMyPubKey(final Integer subaccount, final Integer pointer) {
        ISigningWallet parent = mHDParent;
        if (subaccount != null && subaccount != 0)
            parent = parent.derive(ISigningWallet.HARDENED | 3)
                           .derive(ISigningWallet.HARDENED | subaccount);

        DeterministicKey k = HDKey.deriveChildKey(parent.getPubKey(), 1);
        return HDKey.deriveChildKey(k, pointer);
    }

    public ListenableFuture<ArrayList> getAllUnspentOutputs(int confs, Integer subaccount) {
        return simpleCall("txs.get_all_unspent_outputs", ArrayList.class, confs, subaccount);
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
