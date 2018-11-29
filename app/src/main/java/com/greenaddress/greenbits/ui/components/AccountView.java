package com.greenaddress.greenbits.ui.components;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.greenaddress.greenapi.data.BalanceData;
import com.greenaddress.greenbits.GaService;
import com.greenaddress.greenbits.ui.FontAwesomeTextView;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.UI;

public class AccountView extends CardView {

    private View mView;
    private ImageView mReceiveQrImageView;
    private Button mSendButton, mReceiveButton;
    private RelativeLayout mBodyLayout;
    private LinearLayout mActionLayout, mActionReceiveLayout, mActionSendLayout, mActionDividerLayout;
    private TextView mReceiveAddressText, mTitleText, mBalanceText, mBalanceFiatText;
    private FontAwesomeTextView mBalanceUnit;

    public AccountView(final Context context) {
        super(context);
        setupInflate(context);
        setupViews(mView);
    }

    public AccountView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        setupInflate(context);
        setupViews(mView);
    }

    public void setView(final View view) {
        mView = view;
        setupViews(mView);
    }

    private void setupInflate(final Context context) {
        final LayoutInflater inflater = (LayoutInflater) context
                                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.list_element_account, this, true);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            getBackground().setAlpha(0);
        else
            setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));

        mView = getRootView();
    }

    private void setupViews(final View view) {
        mSendButton = UI.find(view, R.id.sendButton);
        mReceiveButton = UI.find(view, R.id.receiveButton);
        mBodyLayout = UI.find(view, R.id.body);
        mActionLayout = UI.find(view, R.id.actionLayout);
        mReceiveAddressText = UI.find(view, R.id.receiveAddressText);
        mTitleText = UI.find(view, R.id.name);
        mBalanceText = UI.find(view, R.id.mainBalanceText);
        mReceiveQrImageView = UI.find(view, R.id.receiveQrImageView);
        mBalanceUnit = UI.find(view, R.id.mainBalanceUnit);
        mBalanceFiatText = UI.find(view, R.id.mainLocalBalanceText);
        mActionReceiveLayout = UI.find(view, R.id.actionReceiveLayout);
        mActionSendLayout = UI.find(view, R.id.actionSendLayout);
        mActionDividerLayout = UI.find(view, R.id.actionDividerLayout);
    }

    // Show actions
    public void hideActions() {
        mActionLayout.setVisibility(GONE);
    }

    public void showActions() {
        mActionReceiveLayout.setVisibility(VISIBLE);
        mActionSendLayout.setVisibility(VISIBLE);
        mActionDividerLayout.setVisibility(VISIBLE);
        mActionLayout.setVisibility(VISIBLE);
    }

    public void showActionOnlyReceive() {
        mActionReceiveLayout.setVisibility(VISIBLE);
        mActionSendLayout.setVisibility(GONE);
        mActionDividerLayout.setVisibility(GONE);
        mActionLayout.setVisibility(VISIBLE);
    }


    // Get/Set properties
    public void setReceiveAddress(final String address) {
        mReceiveAddressText.setText(address);
    }

    public String getReceiveAddress() {
        if (mReceiveAddressText == null)
            return null;
        return mReceiveAddressText.getText().toString();
    }

    public void setTitle(final String text) {
        mTitleText.setText(text);
    }

    public void setBalance(final GaService service, final BalanceData balance) {
        final ObjectNode balanceData = balance.toObjectNode();
        final String valueBitcoin = service.getValueString(balanceData, false, true);
        final String valueFiat = service.getValueString(balanceData, true, true);
        mBalanceText.setVisibility(VISIBLE);
        mBalanceText.setText(valueBitcoin);
        mBalanceFiatText.setText(valueFiat);

        /*if (service.isElements()) {
            mBalanceUnit.setText(String.format("%s ", service.getAssetSymbol()));
            mBalanceText.setText(service.getAssetFormat().format(balance));
           }*/
    }

    public void setReceiveQrImageView(final Context context, final String address) {
        mReceiveQrImageView.setVisibility(VISIBLE);
        mReceiveQrImageView.setImageDrawable(UI.getQrBitmapDrawable(context, address));
    }

    public void setReceiveQrImageView(final Bitmap qrImageView) {
        mReceiveQrImageView.setVisibility(VISIBLE);
        mReceiveQrImageView.setImageBitmap(qrImageView);
    }

    public void setReceiveQrImageView(final BitmapDrawable qrDrawable) {
        mReceiveQrImageView.setVisibility(VISIBLE);
        mReceiveQrImageView.setImageDrawable(qrDrawable);
    }

    public BitmapDrawable getQrBitmapDrawable(final Context context) {
        return UI.getQrBitmapDrawable(context, mReceiveAddressText.getText().toString());
    }

    // Set on click listener
    @Override
    public void setOnClickListener(final OnClickListener onClickListener) {
        mSendButton.setOnClickListener(onClickListener);
        mReceiveButton.setOnClickListener(onClickListener);
        mBodyLayout.setOnClickListener(onClickListener);
        mBalanceText.setOnClickListener(onClickListener);
    }

}
