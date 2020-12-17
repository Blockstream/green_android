package com.greenaddress.greenbits.ui.transactions;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.greenaddress.greenapi.data.NetworkData;
import com.greenaddress.greenapi.data.TransactionData;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.UI;
import com.greenaddress.greenbits.ui.preferences.PrefKeys;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static android.content.Context.MODE_PRIVATE;

public class TransactionSharingFrament extends BottomSheetDialogFragment {

    private TransactionData mTxData;
    private NetworkData mNetworkData;

    public static TransactionSharingFrament createTransactionSharingFrament(final NetworkData network, final TransactionData tx) {
        final TransactionSharingFrament fragment = new TransactionSharingFrament();
        final Bundle bundle = new Bundle();
        bundle.putSerializable("TRANSACTION", tx);
        bundle.putSerializable("NETWORK", network);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable final Bundle savedInstanceState) {
        final Bundle bundle = getArguments();
        if (bundle != null) {
            mTxData = (TransactionData) bundle.getSerializable("TRANSACTION");
            mNetworkData = (NetworkData) bundle.getSerializable("NETWORK");
        }
        return inflater.inflate(R.layout.bottom_sheet_sharing_transaction, container, false);
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        UI.find(view, R.id.explorer_view).setOnClickListener(this::onClickConfidentialView);
        UI.find(view, R.id.sharing_view).setOnClickListener(this::onClickSharingView);
        UI.find(view, R.id.confidential_view).setOnClickListener(this::onClickConfidentialView);
        UI.find(view, R.id.unconfidential_view).setOnClickListener(this::onClickUnconfidentialView);
        UI.find(view, R.id.share_blinding_view).setOnClickListener(this::onClickShareBlindingView);
        UI.hideIf(mNetworkData.getLiquid(), UI.find(view, R.id.explorer_view));
        UI.hideIf(mNetworkData.getLiquid(), UI.find(view, R.id.sharing_view));
        UI.showIf(mNetworkData.getLiquid(), UI.find(view, R.id.confidential_view));
        UI.hide(UI.find(view, R.id.unconfidential_view)); // hide showing unconfidential tx to esplora
        UI.showIf(mNetworkData.getLiquid(), UI.find(view, R.id.share_blinding_view));
    }

    private void onClickConfidentialView(final View view) {
        final Uri uri = Uri.parse(TextUtils.concat(mNetworkData.getTxExplorerUrl(),  mTxData.getTxhash()).toString());
        openInBrowser(uri);
    }

    private void onClickUnconfidentialView(final View view) {
        final Uri uri = Uri.parse(TextUtils.concat(mNetworkData.getTxExplorerUrl(),  mTxData.getTxhash(), getUnblindedString()).toString());
        openInBrowser(uri);
    }

    private void onClickSharingView(final View view) {
        final Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, mTxData.getTxhash());
        sendIntent.setType("text/plain");
        final Intent shareIntent = Intent.createChooser(sendIntent, null);
        startActivity(shareIntent);
    }

    private void onClickShareBlindingView(final View view) {
        final String text = mTxData.getUnblindedData().toString();
        final Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, text);
        sendIntent.setType("text/plain");
        final Intent shareIntent = Intent.createChooser(sendIntent, null);
        startActivity(shareIntent);
    }

    private String getUnblindedString() {
        final String s = mTxData.getUnblindedString();
        return s.isEmpty() ? s : "#blinded=" + s;
    }

    private void openInBrowser(final Uri uri) {
        final String domain = uri.getHost();
        final String stripped = domain.startsWith("www.") ? domain.substring(4) : domain;
        final SharedPreferences cfg = getContext().getSharedPreferences(mNetworkData.getNetwork(), MODE_PRIVATE);
        final boolean dontAskAgain = cfg.getBoolean(PrefKeys.DONT_ASK_AGAIN_TO_OPEN_URL, false);
        if (dontAskAgain) {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } else {
            new MaterialDialog.Builder(getContext())
                    .checkBoxPromptRes(R.string.id_dont_ask_me_again, false,
                            (buttonView,
                             isChecked) -> cfg.edit().putBoolean(PrefKeys.DONT_ASK_AGAIN_TO_OPEN_URL,
                                    isChecked).apply())
                    .content(getString(R.string.id_are_you_sure_you_want_to_view, stripped))
                    .backgroundColor(getResources().getColor(R.color.buttonJungleGreen))
                    .positiveText(android.R.string.ok)
                    .negativeText(android.R.string.cancel)
                    .cancelable(false)
                    .onNegative((dialog, which) -> cfg.edit().putBoolean(PrefKeys.DONT_ASK_AGAIN_TO_OPEN_URL,
                            false).apply())
                    .onPositive((dialog, which) -> startActivity(new Intent(Intent.ACTION_VIEW, uri)))
                    .build().show();
        }
    }
}
