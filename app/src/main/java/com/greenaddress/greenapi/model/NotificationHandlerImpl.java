package com.greenaddress.greenapi.model;

import android.util.Log;
import android.util.SparseArray;

import com.blockstream.libgreenaddress.GDK;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.greenaddress.greenapi.data.EstimatesData;
import com.greenaddress.greenapi.data.EventData;
import com.greenaddress.greenapi.data.SettingsData;
import com.greenaddress.greenapi.data.TransactionData;
import com.greenaddress.greenbits.GaService;
import com.greenaddress.greenbits.ui.R;

import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;

public class NotificationHandlerImpl implements GDK.NotificationHandler {
    private Model mModel;
    private GaService mService;
    private Queue<Object> mTemp = new LinkedList<>();
    private static final ObjectMapper mObjectMapper = new ObjectMapper();

    public NotificationHandlerImpl() {
        mObjectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public synchronized void setModel(final GaService service) {
        this.mService = service;
        this.mModel = service.getModel();
        for (Object element : mTemp) {
            process(element);
        }
        mTemp.clear();
    }

    @Override
    public synchronized void onNewNofification(final Object session, final Object jsonObject) {
        if (mModel == null) {
            mTemp.add(jsonObject);
        } else {
            process(jsonObject);
        }
    }
//{"reset_2fa_active":true,"reset_2fa_days_remaining":90,"reset_2fa_disputed":false}
    private void process(final Object jsonObject) {
        Log.d("OBSNTF", "notification " + jsonObject);
        if (jsonObject == null)
            return;
        ObjectNode objectNode = (ObjectNode) jsonObject;
        try {
            switch (objectNode.get("event").asText()) {
            case "block": {
                //{"block":{"block_hash":"0000000000003c640a577923dd385428edcfa570ee3bb46d435efca1efbb71a5","block_height":1435025},"event":"block"}
                final JsonNode blockHeight = objectNode.get("block").get("block_height");
                Log.d("OBSNTF", "blockHeight " + blockHeight);
                mModel.getBlockchainHeightObservable().setHeight(blockHeight.asInt());
                break;
            }
            case "fees": {
                //{"fees":[1000,86602,86602,23249,23249,23249,23249,23249,23249,23249,23249,23249,23249,23249,23249,23249,23249,23249,23249,23249,23249,23249,23249,23249,23249]}
                final EstimatesData estimatesData = mObjectMapper.treeToValue(objectNode, EstimatesData.class);
                mModel.getFeeObservable().setFees(estimatesData.getFees());
                Log.d("OBSNTF", "estimatesData " + estimatesData);
                break;
            }
            case "transaction": {
                //{"event":"transaction","transaction":{"satoshi":7895722,"subaccounts":[0],"txhash":"eab1e3aaa357a78f83c7a7d009fe8d2c8acbe9e1c5071398694bbeed7f812f2f","type":"incoming"}}
                final JsonNode transaction = objectNode.get("transaction");
                final ArrayNode arrayNode = (ArrayNode) transaction.get("subaccounts");

                for (JsonNode jsonNode : arrayNode) {
                    final int subaccount = jsonNode.asInt();
                    Log.d("OBSNTF", "subaccount involved " + subaccount);
                    mModel.getBalanceDataObservable(subaccount).refresh();
                    mModel.getReceiveAddressObservable(subaccount).refresh();
                    mModel.getTransactionDataObservable(subaccount).refresh();
                    final TransactionData transactionData = mObjectMapper.convertValue(transaction,
                                                                                       TransactionData.class);
                    transactionData.setSubaccount(subaccount);
                    Log.d("OBSNTF", "transactionData " + transactionData);
                    mModel.getEventDataObservable().pushEvent(new EventData(R.string.id_new_transaction,
                                                                            R.string.id_new_s_transaction_of_s_in,
                                                                            transactionData));
                }
                break;
            }
            case "settings": {
                //{"event":"settings","settings":{"altimeout":5,"notifications":{"email_incoming":true,"email_outgoing":true},"pricing":{"currency":"MYR","exchange":"LUNO"},"required_num_blocks":24,"sound":false,"unit":"bits"}}
                final SettingsData settings =
                    mObjectMapper.convertValue(objectNode.get("settings"), SettingsData.class);
                Log.d("OBSNTF", "SettingsData " + settings);
                mModel.getSettingsObservable().setSettings(settings);
                break;
            }
            case "subaccount": {
                //{"event":"subaccount","subaccount":{"bits":"701144.66","btc":"0.70114466","fiat":"0.7712591260000000622741556099981585311432","fiat_currency":"EUR","fiat_rate":"1.10000000000000008881784197001252","has_transactions":true,"mbtc":"701.14466","name":"","pointer":0,"receiving_id":"GA3MQKVp6pP7royXDuZcw55F2TXTgg","recovery_chain_code":"","recovery_pub_key":"","satoshi":70114466,"type":"2of2","ubtc":"701144.66"}}
                final Integer account = objectNode.get("subaccount").get("pointer").asInt();
                mModel.getActiveAccountObservable().setActiveAccount(account);
                Log.d("OBSNTF", "activeAccount " + account);
                break;
            }
            case "twofactor_reset": {
                //{"event":"twofactor_reset","twofactor_reset":{"days_remaining":90,"is_active":true,"is_disputed":false}}
                final JsonNode twofactorReset = objectNode.get("twofactor_reset");
                final boolean isActive = twofactorReset.get("is_active").asBoolean();
                if (isActive) {
                    final String daysRemaining;
                    if (twofactorReset.get("is_disputed").asBoolean()) {
                        daysRemaining = "disputed";     // FIXME: id_disputed
                    } else{
                        daysRemaining = String.valueOf(twofactorReset.get("days_remaining").asInt());
                    }
                    mModel.setTwoFAReset(true);
                    mModel.getEventDataObservable().pushEvent(new EventData(R.string.id_twofactor_authentication,
                                                                            R.string.id_days_remaining_s,
                                                                            daysRemaining));
                }
                break;
            }
            }
        } catch (final Exception e) {
            Log.e("NOTIF", e.getMessage());
        }

    }
}
