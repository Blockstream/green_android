package com.greenaddress.greenapi.model;

import android.util.Log;

import com.blockstream.gdk.data.TwoFactorReset;
import com.blockstream.libgreenaddress.GDK;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.greenaddress.Bridge;
import com.greenaddress.greenapi.data.EstimatesData;
import com.greenaddress.greenapi.data.EventData;
import com.greenaddress.greenapi.data.SettingsData;
import com.greenaddress.greenbits.ui.R;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

import static com.greenaddress.greenapi.Session.getSession;


public class NotificationHandlerImpl implements GDK.NotificationHandler {

    private static final ObjectMapper mObjectMapper = new ObjectMapper();
    private Integer mBlockHeight = 0;
    private JsonNode mNetworkNode;
    private final List<Long> mFees = new ArrayList<>();
    private final List<EventData> mEventDataList = new ArrayList<>();

    private final PublishSubject<JsonNode> mTransactionPublish = PublishSubject.create();
    private final PublishSubject<Integer> mBlockPublish = PublishSubject.create();
    private final PublishSubject<JsonNode> mNetworkPublish = PublishSubject.create();
    private final PublishSubject<JsonNode> mTorPublish = PublishSubject.create();
    private final PublishSubject<List<EventData>> mEventsPublish = PublishSubject.create();
    private final PublishSubject<SettingsData> mSettingsPublish = PublishSubject.create();

    public NotificationHandlerImpl() {
        mObjectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public Observable<JsonNode> getTransactionObservable() {
        return mTransactionPublish.hide();
    }

    public Observable<JsonNode> getTorObservable() {
        return mTorPublish.hide();
    }

    public Observable<Integer> getBlockObservable() {
        return mBlockPublish.hide();
    }

    public Observable<JsonNode> getNetworkObservable() {
        return mNetworkPublish.hide();
    }

    public Observable<List<EventData>> getEventsObservable() {
        return mEventsPublish.hide();
    }

    public Observable<SettingsData> getSettingsObservable() {
        return mSettingsPublish.hide();
    }

    public Integer getBlockHeight() {
        return mBlockHeight;
    }

    public List<Long> getFees() {
        return mFees;
    }

    public List<EventData> getEvents() {
        return mEventDataList;
    }

    public JsonNode getNetworkNode() {
        return mNetworkNode;
    }

    public void reset() {
        mBlockHeight = 0;
        mNetworkNode = null;
        mFees.clear();
        mEventDataList.clear();
    }

    @Override
    public synchronized void onNewNotification(final Object session, final Object jsonObject) {
        process(Bridge.INSTANCE.toJackson(jsonObject));
    }

    private void process(final ObjectNode jsonObject) {
        Log.d("OBSNTF", "notification " + jsonObject);
        if (jsonObject == null)
            return;
        final ObjectNode objectNode = jsonObject;
        try {
            switch (objectNode.get("event").asText()) {
            case "tor":
                final JsonNode torJson = objectNode.get("tor");
                mTorPublish.onNext(torJson);
                break;
            case "network": {
                //{"event":"network","network":{"connected":false,"elapsed":1091312175736,"limit":true,"waiting":0}}
                final JsonNode networkNode = objectNode.get("network");
                mNetworkNode = networkNode;
                mNetworkPublish.onNext(networkNode);
                break;
            }
            case "block": {
                //{"block":{"block_hash":"0000000000003c640a577923dd385428edcfa570ee3bb46d435efca1efbb71a5","block_height":1435025},"event":"block"}
                final JsonNode blockHeight = objectNode.get("block").get("block_height");
                Log.d("OBSNTF", "blockHeight " + blockHeight);
                mBlockHeight = blockHeight.asInt(0);
                mBlockPublish.onNext(mBlockHeight);
                break;
            }
            case "fees": {
                //{"fees":[1000,86602,86602,23249,23249,23249,23249,23249,23249,23249,23249,23249,23249,23249,23249,23249,23249,23249,23249,23249,23249,23249,23249,23249,23249]}
                final EstimatesData estimatesData = mObjectMapper.treeToValue(objectNode, EstimatesData.class);
                mFees.clear();
                mFees.addAll(estimatesData.getFees());
                Log.d("OBSNTF", "estimatesData " + estimatesData);
                break;
            }
            case "transaction": {
                //{"event":"transaction","transaction":{"satoshi":7895722,"subaccounts":[0],"txhash":"eab1e3aaa357a78f83c7a7d009fe8d2c8acbe9e1c5071398694bbeed7f812f2f","type":"incoming"}}
                final JsonNode transaction = objectNode.get("transaction");
                final ArrayNode arrayNode = (ArrayNode) transaction.get("subaccounts");
                final List<Integer> subaccounts = mObjectMapper.readValue(
                    arrayNode.toString(), new TypeReference<List<Integer>>(){});

                boolean eventPushed = false;
                for (final JsonNode jsonNode : arrayNode) {
                    final int subaccount = jsonNode.asInt();
                    Log.d("OBSNTF", "subaccount involved " + subaccount);
                    mTransactionPublish.onNext(transaction);

                    Integer description = null;
                    if (subaccounts.size() > 1) {
                        description = R.string.id_new_transaction_involving;
                    } else if (transaction.get("type") != null) {
                        if ("incoming".equals(transaction.get("type").asText())) {
                            description = R.string.id_new_incoming_transaction_in;
                        } else {
                            description = R.string.id_new_outgoing_transaction_from;
                        }
                    }

                    if (!eventPushed && description != null) {
                        mEventDataList.add(new EventData(
                                               R.string.id_new_transaction, description, transaction));
                        mEventsPublish.onNext(mEventDataList);
                        eventPushed = true;
                    }
                }
                break;
            }
            case "settings": {
                //{"event":"settings","settings":{"altimeout":5,"notifications":{"email_incoming":true,"email_outgoing":true},"pricing":{"currency":"MYR","exchange":"LUNO"},"required_num_blocks":24,"sound":false,"unit":"bits"}}
                final SettingsData settings =
                    mObjectMapper.convertValue(objectNode.get("settings"), SettingsData.class);
                getSession().setSettings(settings);
                Log.d("OBSNTF", "SettingsData " + settings);
                mSettingsPublish.onNext(settings);
                break;
            }
            case "subaccount": {
                //{"event":"subaccount","subaccount":{"bits":"701144.66","btc":"0.70114466","fiat":"0.7712591260000000622741556099981585311432","fiat_currency":"EUR","fiat_rate":"1.10000000000000008881784197001252","has_transactions":true,"mbtc":"701.14466","name":"","pointer":0,"receiving_id":"GA3MQKVp6pP7royXDuZcw55F2TXTgg","recovery_chain_code":"","recovery_pub_key":"","satoshi":70114466,"type":"2of2","ubtc":"701144.66"}}
                // server-side subaccount setting is ignored because we use the locally-saved one
                break;
            }
            case "twofactor_reset": {
                //{"event":"twofactor_reset","twofactor_reset":{"days_remaining":90,"is_active":true,"is_disputed":false}}
                final JsonNode resetData = objectNode.get("twofactor_reset");

                if (resetData.get("is_active").asBoolean()) {
                    final TwoFactorReset reset;
                    if (resetData.get("is_disputed").asBoolean()) {
                        reset = new TwoFactorReset(true, -1, true);
                    } else{
                        final Integer days = resetData.get("days_remaining").asInt();
                        reset = new TwoFactorReset(true, days, false);
                    }

                    getSession().setTwoFAReset(reset);
                }
                break;
            }
            }
        } catch (final Exception e) {
            Log.e("NOTIF", e.getMessage());
        }
    }
}
