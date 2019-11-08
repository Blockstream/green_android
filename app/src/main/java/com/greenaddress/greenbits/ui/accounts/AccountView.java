package com.greenaddress.greenbits.ui.accounts;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.greenaddress.greenapi.data.BalanceData;
import com.greenaddress.greenbits.GaService;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.UI;

public class AccountView extends CardView {

    private View mView;
    private Button mSendButton, mReceiveButton;
    private ImageButton mSelectButton;
    private LinearLayout mBodyLayout, mActionLayout, mSubaccount, mAddSubaccount;
    private TextView mTitleText, mBalanceText, mBalanceUnitText, mBalanceFiatText;

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
        mBalanceUnitText = UI.find(view, R.id.mainBalanceUnitText);
        mBalanceFiatText = UI.find(view, R.id.mainLocalBalanceText);
        mSelectButton = UI.find(view, R.id.selectSubaccount);
        mSubaccount= UI.find(view, R.id.subaccount);
        mAddSubaccount = UI.find(view, R.id.addSubaccount);

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

    public void listMode(boolean mode) {
        mSelectButton.setImageDrawable(mode
                                       ? getResources().getDrawable(R.drawable.ic_stack_wallets)
                                       : getResources().getDrawable(R.drawable.ic_arrow_forward_24dp)
                                       );
    }

    public void setTitle(final String text) {
        mTitleText.setText(text);
    }

    public void setBalance(final GaService service, final long satoshi) {
        final String valueBitcoin = service.getValueString(satoshi, false, false);
        final String valueFiat = service.getValueString(satoshi, true, true);
        mBalanceText.setVisibility(VISIBLE);
        mBalanceText.setText(valueBitcoin);
        mBalanceUnitText.setVisibility(VISIBLE);
        mBalanceUnitText.setText(" " + service.getBitcoinOrLiquidUnit());
        mBalanceFiatText.setText("≈  " + valueFiat);
    }

    // Set on click listener
    @Override
    public void setOnClickListener(final OnClickListener onClickListener) {
        mSendButton.setOnClickListener(onClickListener);
        mReceiveButton.setOnClickListener(onClickListener);
        mBodyLayout.setOnClickListener(onClickListener);
        mBalanceText.setOnClickListener(onClickListener);
        mSelectButton.setOnClickListener(onClickListener);
        mAddSubaccount.setOnClickListener(onClickListener);
    }

    public void showAdd(final boolean value) {
        mSubaccount.setVisibility(value ? GONE : VISIBLE);
        mAddSubaccount.setVisibility(value ? VISIBLE : GONE);
    }
}
