package com.greenaddress.greenbits.ui.accounts;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.greenaddress.greenapi.Session;
import com.greenaddress.greenapi.model.Conversion;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.UI;

public class AccountView extends CardView {

    private View mView;
    private Button mSendButton, mReceiveButton;
    private ImageButton mSelectButton;
    private LinearLayout mBodyLayout, mActionLayout, mSubaccount, mAddSubaccount;
    private TextView mTitleText, mBalanceText, mBalanceUnitText, mBalanceFiatText;
    private Session mSession;

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
        inflater.inflate(R.layout.list_element_wallet, this, true);

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

        setButtonDrawable(mSendButton, getResources().getDrawable(R.drawable.ic_send));
        setButtonDrawable(mReceiveButton, getResources().getDrawable(R.drawable.ic_receive));
    }

    // Show actions
    public void hideActions() {
        mActionLayout.setVisibility(GONE);
    }

    private void setButtonDrawable(final Button button, final Drawable drawable) {
        // Set programmatically tint color for Android Api < 24
        final TypedValue typedValue = new TypedValue();
        getContext().getTheme().resolveAttribute(R.attr.drawableSendReceiveIcColor, typedValue, true);
        final int color = typedValue.data;
        DrawableCompat.setTint(drawable, color);
        DrawableCompat.setTintMode(drawable, PorterDuff.Mode.SRC_ATOP);
        button.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
    }

    public void showActions(final boolean isWatchOnly) {
        if (isWatchOnly) {
            mSendButton.setText(R.string.id_sweep);
            setButtonDrawable(mSendButton, getResources().getDrawable(R.drawable.ic_sweep));
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

    public void setBalance(final long satoshi) {
        mBalanceText.setVisibility(VISIBLE);
        mBalanceUnitText.setVisibility(VISIBLE);
        try {
            final String valueBitcoin = Conversion.getBtc(mSession, satoshi, false);
            final String valueFiat = Conversion.getFiat(mSession, satoshi, true);
            mBalanceText.setText(valueBitcoin);
            mBalanceUnitText.setText(" " + Conversion.getBitcoinOrLiquidUnit(mSession));
            mBalanceFiatText.setText("≈  " + valueFiat);
        } catch (final Exception e) {
            Log.e("", "Conversion error: " + e.getLocalizedMessage());
        }
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

    public void setSession(Session session) {
        mSession = session;
    }
}
