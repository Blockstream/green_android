package com.greenaddress.greenbits.ui;


import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.greenaddress.greenapi.Network;
import com.greenaddress.greenbits.QrBitmap;

import org.bitcoinj.core.Address;
import org.bitcoinj.uri.BitcoinURI;

import nordpol.android.OnDiscoveredTagListener;
import nordpol.android.TagDispatcher;


public class ReceiveFragment extends SubaccountFragment implements OnDiscoveredTagListener {
    private static final String TAG = ReceiveFragment.class.getSimpleName();

    private View mView;
    private FutureCallback<QrBitmap> mNewAddressCallback = null;
    private QrBitmap mQrCodeBitmap = null;
    private int mSubAccount;
    private boolean mPausing = false;
    private boolean mSettingQrCode = false;
    private Dialog mQrCodeDialog;
    private TagDispatcher mTagDispatcher;
    TextView mAddressText;
    ImageView mAddressImage;
    TextView mCopyIcon;

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean("pausing", mPausing);
        if (mQrCodeBitmap != null)
            outState.putParcelable("address", mQrCodeBitmap);
    }

    @Override
    public void setUserVisibleHint(final boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        final View v = getView(); // FIXME: This should use mView

        if (!mPausing && v != null) {
            // get a new address every time the tab is displayed
            if (isVisibleToUser) {
                hideKeyboard();
                // get a new address:
                if (mQrCodeBitmap == null && !mSettingQrCode)
                    getNewAddress(v);
            } else { // !isVisibleToUser
                // hide to avoid showing old address when swiping
                mQrCodeBitmap = null;
                mAddressText.setText("");
                mAddressImage.setImageBitmap(null);
            }
        }
        if (isVisibleToUser)
            mPausing = false;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume -> " + TAG);
        if (mNewAddressCallback != null && mQrCodeBitmap == null && !mSettingQrCode)
            getNewAddress(null);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause -> " + TAG);
        if (getUserVisibleHint())
            mPausing = true;

        mTagDispatcher.disableExclusiveNfc();
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView -> " + TAG);
        popupWaitDialog(R.string.generating_address);

        final GaActivity gaActivity = getGaActivity();

        if (savedInstanceState != null) {
            mPausing = savedInstanceState.getBoolean("pausing");
            mQrCodeBitmap = savedInstanceState.getParcelable("address");
        }

        mTagDispatcher = TagDispatcher.get(getActivity(), this);
        mTagDispatcher.enableExclusiveNfc();

        mSubAccount = getGAService().getCurrentSubAccount();

        mView = inflater.inflate(R.layout.fragment_receive, container, false);
        mAddressText = UI.find(mView, R.id.receiveAddressText);
        mAddressImage = UI.find(mView, R.id.receiveQrImageView);
        mCopyIcon = UI.find(mView, R.id.receiveCopyIcon);
        UI.disable(mCopyIcon);

        mCopyIcon.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(final View v) {
                        // Gets a handle to the clipboard service.
                        final ClipboardManager clipboard = (ClipboardManager)
                                getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                        final ClipData clip = ClipData.newPlainText("data", UI.getText(mAddressText).replace("\n", ""));
                        clipboard.setPrimaryClip(clip);

                        final String text = gaActivity.getString(R.string.toastOnCopyAddress) + " " + gaActivity.getString(R.string.warnOnPaste);
                        gaActivity.toast(text);
                    }
                }
        );
        final View qrView = getActivity().getLayoutInflater().inflate(R.layout.dialog_qrcode, null, false);

        final ImageView qrcodeInDialog = UI.find(qrView, R.id.qrInDialogImageView);
        mNewAddressCallback = new FutureCallback<QrBitmap>() {
            @Override
            public void onSuccess(final QrBitmap result) {
                mQrCodeBitmap = result;

                final Activity activity = getActivity();
                if (activity == null)
                    return;

                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        final BitmapDrawable bd = new BitmapDrawable(getResources(), result.getQRCode());

                        hideWaitDialog();
                        UI.enable(mCopyIcon);
                        bd.setFilterBitmap(false);
                        mAddressImage.setImageDrawable(bd);

                        final String qrData = result.getData();
                        mAddressText.setText(String.format("%s\n%s\n%s", qrData.substring(0, 12), qrData.substring(12, 24), qrData.substring(24)));
                        mSettingQrCode = false;

                        mAddressImage.setOnClickListener(new View.OnClickListener() {
                            public void onClick(final View v) {
                                if (mQrCodeDialog == null) {
                                    final DisplayMetrics displaymetrics = new DisplayMetrics();
                                    activity.getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
                                    final int height = displaymetrics.heightPixels;
                                    final int width = displaymetrics.widthPixels;
                                    Log.d(TAG, height + "x" + width);
                                    final int min = (int) (Math.min(height, width) * 0.8);
                                    final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(min, min);
                                    qrcodeInDialog.setLayoutParams(layoutParams);

                                    mQrCodeDialog = new Dialog(activity);
                                    mQrCodeDialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
                                    mQrCodeDialog.setContentView(qrView);
                                }
                                mQrCodeDialog.show();
                                final BitmapDrawable bd = new BitmapDrawable(getResources(), result.getQRCode());
                                bd.setFilterBitmap(false);
                                qrcodeInDialog.setImageDrawable(bd);
                            }
                        });
                    }
                });
            }

            @Override
            public void onFailure(final Throwable t) {
                t.printStackTrace();
                if (getActivity() == null)
                    return;

                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        hideWaitDialog();
                        UI.enable(mCopyIcon);
                    }
                });
            }
        };

        if (mQrCodeBitmap != null)
            mNewAddressCallback.onSuccess(mQrCodeBitmap);

        final TextView newAddressIcon = UI.find(mView, R.id.receiveNewAddressIcon);
        newAddressIcon.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(final View v) {
                        if (!mSettingQrCode) {
                            // FIXME: Instead of checking the state here, enable/disable sendButton when state changes
                            if (!getGAApp().mService.isLoggedIn()) {
                                gaActivity.toast(R.string.err_send_not_connected_will_resume);
                                return;
                            }
                            getNewAddress(mView);
                        }
                    }
                }
        );

        registerReceiver();
        return mView;
    }

    private void getNewAddress(final View v) {
        mSettingQrCode = true;

        popupWaitDialog(R.string.generating_address);
        if (v != null)
            startNewAddressAnimation(v);

        Futures.addCallback(getGAService().getNewAddressBitmap(mSubAccount),
                            mNewAddressCallback, getGAService().getExecutor());
     }

    private void startNewAddressAnimation(final View v) {
        if (getActivity() == null)
            return;

        UI.disable(mCopyIcon);
        mAddressText.setText("");
        mAddressImage.setImageBitmap(null);
    }

    @Override
    public void tagDiscovered(final Tag t) {
        Log.d("NFC", "Tag discovered " + t);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate -> " + TAG);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.receive, menu);
    }

    @Override
    protected void onSubaccountChanged(final int newSubAccount) {
        mSubAccount = newSubAccount;
        if (mView != null)
            startNewAddressAnimation(mView);

        if (!mSettingQrCode)
            getNewAddress(null);
    }

    private String getAddressUri() {
        final Address address = Address.fromBase58(Network.NETWORK, mQrCodeBitmap.getData());
        return BitcoinURI.convertToBitcoinURI(address, null, null, null);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {

        if (item.getItemId() == R.id.action_share) {
            if (mQrCodeBitmap != null && !mQrCodeBitmap.getData().isEmpty()) {
                // SHARE intent
                final Intent intent = new Intent();
                intent.setAction(Intent.ACTION_SEND);
                intent.putExtra(Intent.EXTRA_TEXT, getAddressUri());
                intent.setType("text/plain");
                startActivity(intent);
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
