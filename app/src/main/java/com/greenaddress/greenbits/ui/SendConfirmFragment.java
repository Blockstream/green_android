package com.greenaddress.greenbits.ui;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.greenaddress.gdk.GDKSession;
import com.greenaddress.gdk.GDKTwoFactorCall;
import com.greenaddress.greenapi.ConnectionManager;
import com.greenaddress.greenapi.data.BalanceData;
import com.greenaddress.greenapi.data.CreateTransactionData;
import com.greenaddress.greenapi.data.HWDeviceData;
import com.greenaddress.greenapi.data.SubaccountData;
import com.greenaddress.greenapi.model.EventDataObservable;
import com.greenaddress.greenbits.GaService;
import com.greenaddress.greenbits.ui.components.SwipeButton;

import java.io.IOException;
import java.util.ArrayList;

public class SendConfirmFragment extends GAFragment {
    private static final String TAG = SendConfirmFragment.class.getSimpleName();

    private CreateTransactionData mTxData;
    private HWDeviceData mHwData;
    private View mView;

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {

        Log.d(TAG, "onCreateView -> " + TAG);
        //if (isZombieNoView())
        //    return null;

        final GaService service = getGAService();
        final GaActivity activity = getGaActivity();
        final GDKSession session = service.getSession();

        // Get arguments from bundle
        final Bundle b = this.getArguments();
        if (b == null)
            return mView;
        final String tx_json = b.getString("transaction");
        final String hww_json = b.getString("hww");

        final ObjectMapper mObjectMapper = new ObjectMapper();
        mObjectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            mTxData = mObjectMapper.readValue(tx_json, CreateTransactionData.class);
            if (hww_json != null)
                mHwData = mObjectMapper.readValue(hww_json, HWDeviceData.class);
        } catch (IOException e) {
            e.printStackTrace();
            UI.toast(activity, "error", Toast.LENGTH_LONG);
            activity.finishOnUiThread();
            return mView;
        }

        // Bin Ui views
        mView = inflater.inflate(R.layout.fragment_send_confirm, container, false);
        final TextView noteText = UI.find(mView, R.id.noteText);
        final TextView addressText = UI.find(mView, R.id.addressText);
        final TextView subaccountText = UI.find(mView, R.id.subaccountText);
        final SwipeButton swipeButton = UI.find(mView, R.id.swipeButton);

        // Setup views fields
        final BalanceData currentRecipient = mTxData.getAddressees().get(0);
        final boolean isSweeping = mTxData.getIsSweep();
        final Integer subaccount = mTxData.getChangeSubaccount();
        if (isSweeping) {
            subaccountText.setText("Paper Wallet");
        } else {
            final int subAccount = subaccount;
            final SubaccountData subaccountData =
                service.getModel().getSubaccountDataObservable().getSubaccountDataWithPointer(subAccount);
            subaccountText.setText(subaccountData.getName());
        }
        addressText.setText(currentRecipient.getAddress());

        // Set currency & amount
        final long amount = mTxData.getSatoshi();
        final long fee = mTxData.getFee();
        final ObjectNode amountNode = session.convertSatoshi(amount);
        final ObjectNode feeNode = session.convertSatoshi(fee);
        final TextView sendAmount = UI.find(mView, R.id.sendAmount);
        sendAmount.setText(String.format("%s / %s",
                                         service.getValueString(amountNode,false,true),
                                         service.getValueString(amountNode,true,true)));
        final TextView sendFee = UI.find(mView, R.id.sendFee);
        sendFee.setText(String.format("%s / %s",
                                      service.getValueString(feeNode,false,true),
                                      service.getValueString(feeNode,true,true)));

        if (mHwData != null && mHwData.getDevice().isSupportsArbitraryScripts() && mTxData.getChangeAddress() != null) {
            UI.show(UI.find(mView, R.id.changeLayout));
            final TextView view = UI.find(mView, R.id.changeAddressText);
            view.setText(mTxData.getChangeAddress().getAddress());
        }

        swipeButton.setOnActiveListener(new SwipeButton.OnActiveListener() {
            @Override
            public void onActive() {
                getGaActivity().startLoading();
                swipeButton.setEnabled(false);
                final String memo = noteText.getText().toString();
                service.getExecutor().execute(() ->
                {
                    try {
                        ObjectNode tx = (ObjectNode) (new ObjectMapper()).readTree(tx_json);
                        tx.set("memo", new TextNode(memo));
                        // sign transaction
                        final ConnectionManager cm = service.getConnectionManager();
                        final GDKTwoFactorCall signCall = session.signTransactionRaw(getActivity(), tx);
                        tx = signCall.resolve(null, cm.getHWResolver());

                        // send transaction
                        final boolean isSweep = tx.get("is_sweep").asBoolean();
                        if (isSweep) {
                            session.broadcastTransactionRaw(tx.get("transaction").asText());
                        } else {
                            final GDKTwoFactorCall sendCall = session.sendTransactionRaw(getActivity(), tx);
                            sendCall.resolve(new PopupMethodResolver(activity), new PopupCodeResolver(activity));
                            service.getModel().getTwoFactorConfigDataObservable().refresh();
                        }
                        if (tx.has("previous_transaction")) {
                            //emptying list to avoid showing replaced txs
                            service.getModel().getTransactionDataObservable(subaccount)
                            .setTransactionDataList(new ArrayList<>());
                            final String hash = tx.get("previous_transaction").get("txhash").asText();
                            service.getModel().getEventDataObservable().removeTx(hash);
                        }
                        UI.toast(activity, R.string.id_transaction_sent, Toast.LENGTH_LONG);

                        activity.setResult(Activity.RESULT_OK);
                        activity.finishOnUiThread();
                    } catch (final Exception e) {
                        final Resources res = getResources();
                        final String msg = UI.i18n(res, e.getMessage());
                        e.printStackTrace();
                        UI.toast(getGaActivity(), msg, Toast.LENGTH_LONG);
                        if (msg.equals(res.getString(R.string.id_transaction_already_confirmed))) {
                            activity.setResult(Activity.RESULT_OK);
                            activity.finishOnUiThread();
                            return;
                        }
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                swipeButton.setEnabled(true);
                                swipeButton.moveButtonBack();
                                getGaActivity().stopLoading();
                            }
                        });
                    }
                });
            }
        });

        return mView;
    }
}
