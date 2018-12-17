package com.greenaddress.greenbits.ui.components;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.greenaddress.greenapi.data.BalanceData;
import com.greenaddress.greenbits.GaService;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.UI;

public class AccountView extends CardView {

    private View mView;
    private Button mSendButton, mReceiveButton;
    private ImageButton mBackButton, mNetworkImage;
    private LinearLayout mBodyLayout, mActionLayout, mActionReceiveLayout, mActionSendLayout, mActionDividerLayout;
    private TextView mTitleText, mBalanceText, mBalanceFiatText;

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
        mTitleText = UI.find(view, R.id.name);
        mBalanceText = UI.find(view, R.id.mainBalanceText);
        mBalanceFiatText = UI.find(view, R.id.mainLocalBalanceText);
        mActionReceiveLayout = UI.find(view, R.id.actionReceiveLayout);
        mActionSendLayout = UI.find(view, R.id.actionSendLayout);
        mBackButton = UI.find(view, R.id.backButton);
        mNetworkImage = UI.find(view, R.id.networkImage);
    }

    // Show actions
    public void hideActions() {
        mActionLayout.setVisibility(GONE);

    }

    public void showActions(final boolean isWatchOnly) {
        if (isWatchOnly) {
            mSendButton.setText(R.string.id_sweep);
            final Drawable sweepDraw = getResources().getDrawable(R.drawable.ic_sweep);
            mSendButton.setCompoundDrawablesWithIntrinsicBounds(sweepDraw, null, null, null);
        }
    }

    public void showBack(boolean show) {
        mBackButton.setVisibility(show ? VISIBLE : GONE);
        mNetworkImage.setVisibility(show ? VISIBLE : GONE);
    }

    public void setIcon(final Drawable resource) {
        mNetworkImage.setImageDrawable(resource);
    }

    public void setTitle(final String text) {
        if (TextUtils.isEmpty(text))
            mTitleText.setText(R.string.id_main);
        else
            mTitleText.setText(text);
    }

    public void setBalance(final GaService service, final BalanceData balance) {
        final ObjectNode balanceData = balance.toObjectNode();
        final String valueBitcoin = service.getValueString(balanceData, false, true);
        final String valueFiat = service.getValueString(balanceData, true, true);
        mBalanceText.setVisibility(VISIBLE);
        mBalanceText.setText(valueBitcoin);
        mBalanceFiatText.setText("≈  " + valueFiat);

        /*if (service.isElements()) {
            mBalanceUnit.setText(String.format("%s ", service.getAssetSymbol()));
            mBalanceText.setText(service.getAssetFormat().format(balance));
           }*/
    }

    // Set on click listener
    @Override
    public void setOnClickListener(final OnClickListener onClickListener) {
        mSendButton.setOnClickListener(onClickListener);
        mReceiveButton.setOnClickListener(onClickListener);
        mBodyLayout.setOnClickListener(onClickListener);
        mBalanceText.setOnClickListener(onClickListener);
        mBackButton.setOnClickListener(onClickListener);
    }

}
