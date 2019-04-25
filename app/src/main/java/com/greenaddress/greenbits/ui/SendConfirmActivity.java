package com.greenaddress.greenbits.ui;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.greenaddress.gdk.GDKTwoFactorCall;
import com.greenaddress.greenapi.ConnectionManager;
import com.greenaddress.greenapi.data.BalanceData;
import com.greenaddress.greenapi.data.CreateTransactionData;
import com.greenaddress.greenapi.data.HWDeviceData;
import com.greenaddress.greenapi.data.SubaccountData;
import com.greenaddress.greenbits.ui.components.CharInputFilter;
import com.greenaddress.greenbits.ui.components.SwipeButton;

import java.io.IOException;
import java.util.ArrayList;

public class SendConfirmActivity extends LoggedActivity implements SwipeButton.OnActiveListener {
    private static final String TAG = SendConfirmActivity.class.getSimpleName();

    private HWDeviceData mHwData;
    private String mTxJson;
    private SwipeButton mSwipeButton;

    @Override
    protected void onCreateWithService(final Bundle savedInstanceState) {
        if (mService == null || mService.getModel() == null) {
            toFirst();
            return;
        }
        final CreateTransactionData mTxData;
        try {
            mTxJson = getIntent().getStringExtra("transaction");
            final String hwwJson = getIntent().getStringExtra("hww");
            final ObjectMapper mObjectMapper = new ObjectMapper();
            mObjectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            mTxData = mObjectMapper.readValue(mTxJson, CreateTransactionData.class);
            if (hwwJson != null)
                mHwData = mObjectMapper.readValue(hwwJson, HWDeviceData.class);
        } catch (final Exception e) {
            e.printStackTrace();
            UI.toast(this, e.getLocalizedMessage(), Toast.LENGTH_LONG);
            setResult(Activity.RESULT_CANCELED);
            finishOnUiThread();
            return;
        }

        // Bin Ui views
        setContentView(R.layout.activity_send_confirm);
        UI.preventScreenshots(this);
        setTitleBackTransparent();
        final TextView noteTextTitle = UI.find(this, R.id.sendMemoTitle);
        final TextView noteText = UI.find(this, R.id.noteText);
        final TextView addressText = UI.find(this, R.id.addressText);
        final TextView subaccountText = UI.find(this, R.id.subaccountText);
        mSwipeButton = UI.find(this, R.id.swipeButton);

        // Setup views fields
        final BalanceData currentRecipient = mTxData.getAddressees().get(0);
        final boolean isSweeping = mTxData.getIsSweep();
        final Integer subaccount = mTxData.getChangeSubaccount();
        UI.hideIf(isSweeping, noteTextTitle);
        UI.hideIf(isSweeping, noteText);
        if (isSweeping)
            subaccountText.setText(R.string.id_sweep_from_paper_wallet);
        else {
            final int subAccount = subaccount;
            final SubaccountData subaccountData =
                mService.getModel().getSubaccountDataObservable().getSubaccountDataWithPointer(subAccount);
            subaccountText.setText(subaccountData.getNameWithDefault(getString(R.string.id_main_account)));
        }
        addressText.setText(currentRecipient.getAddress());
        noteText.setText(mTxData.getMemo());
        CharInputFilter.setIfNecessary(noteText);

        // Set currency & amount
        final long amount = mTxData.getSatoshi();
        final long fee = mTxData.getFee();
        final TextView sendAmount = UI.find(this, R.id.sendAmount);
        final TextView sendFee = UI.find(this, R.id.sendFee);
        sendAmount.setText(getFormatAmount(amount));
        sendFee.setText(getFormatAmount(fee));

        if (mHwData != null && mTxData.getChangeAddress() != null && mTxData.getChangeAmount() > 0) {
            UI.show(UI.find(this, R.id.changeLayout));
            final TextView view = UI.find(this, R.id.changeAddressText);
            view.setText(mTxData.getChangeAddress().getAddress());
        }

        mSwipeButton.setOnActiveListener(this);
    }

    private String getFormatAmount(final long amount) {
        try {
            final ObjectNode feeNode = mService.getSession().convertSatoshi(amount);
            return String.format("%s / %s",
                                 mService.getValueString(feeNode, false, true),
                                 mService.getValueString(feeNode, true, true));
        } catch (final RuntimeException | IOException e) {
            Log.e(TAG, "Conversion error: " + e.getLocalizedMessage());
            return "";
        }
    }

    @Override
    public void onActive() {
        startLoading();
        mSwipeButton.setEnabled(false);
        final TextView noteText = UI.find(this, R.id.noteText);
        final String memo = noteText.getText().toString();
        final GaActivity activity = SendConfirmActivity.this;
        mService.getExecutor().execute(() -> {
            try {
                ObjectNode tx = (ObjectNode) new ObjectMapper().readTree(mTxJson);
                tx.set("memo", new TextNode(memo));
                // sign transaction
                final ConnectionManager cm = mService.getConnectionManager();
                final GDKTwoFactorCall signCall = mService.getSession().signTransactionRaw(activity, tx);
                tx = signCall.resolve(null, cm.getHWResolver());

                // send transaction
                final boolean isSweep = tx.get("is_sweep").asBoolean();
                if (isSweep) {
                    mService.getSession().broadcastTransactionRaw(tx.get("transaction").asText());
                } else {
                    final GDKTwoFactorCall sendCall = mService.getSession().sendTransactionRaw(activity, tx);
                    sendCall.resolve(new PopupMethodResolver(activity), new PopupCodeResolver(activity));
                    mService.getModel().getTwoFactorConfigDataObservable().refresh();
                }
                if (tx.has("previous_transaction")) {
                    //emptying list to avoid showing replaced txs
                    mService.getModel().getTransactionDataObservable(tx.get("change_subaccount").asInt())
                    .setTransactionDataList(new ArrayList<>());
                    final String hash = tx.get("previous_transaction").get("txhash").asText();
                    mService.getModel().getEventDataObservable().removeTx(hash);
                }
                UI.toast(activity, R.string.id_transaction_sent, Toast.LENGTH_LONG);

                activity.setResult(Activity.RESULT_OK);
                activity.finishOnUiThread();
            } catch (final Exception e) {
                final Resources res = getResources();
                final String msg = UI.i18n(res, e.getMessage());
                e.printStackTrace();
                UI.toast(activity, msg, Toast.LENGTH_LONG);
                if (msg.equals(res.getString(R.string.id_transaction_already_confirmed))) {
                    activity.setResult(Activity.RESULT_OK);
                    activity.finishOnUiThread();
                    return;
                }
                activity.runOnUiThread(() -> {
                    mSwipeButton.setEnabled(true);
                    mSwipeButton.moveButtonBack();
                    activity.stopLoading();
                });
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            onBackPressed();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
}
