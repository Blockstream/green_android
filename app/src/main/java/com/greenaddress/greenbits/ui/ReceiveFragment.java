package com.greenaddress.greenbits.ui;


import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.text.Html;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Function;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.greenaddress.greenbits.ConnectivityObservable;
import com.greenaddress.greenbits.QrBitmap;

import javax.annotation.Nullable;


public class ReceiveFragment extends GAFragment {
    private static final String TAG = "ReceiveFragment";
    FutureCallback<QrBitmap> onAddress = null;
    QrBitmap address = null;
    private int curSubaccount;
    private boolean pausing = false;
    private Dialog qrDialog;

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean("pausing", pausing);
        if (address != null) {
            outState.putParcelable("address", address);
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        final View rootView = getView();

        if (!pausing && rootView != null) {
            // get a new address every time the tab is displayed
            if (isVisibleToUser) {
                // get a new address:
                final ListenableFuture<QrBitmap> ft = getGAService().getNewAddress(curSubaccount);
                Futures.addCallback(ft, onAddress, getGAService().es);
                startNewAddressAnimation(rootView);
            } else { // !isVisibleToUser
                // hide to avoid showing old address when swiping
                final TextView receiveAddress = (TextView) rootView.findViewById(R.id.receiveAddressText);
                final ImageView imageView = (ImageView) rootView.findViewById(R.id.receiveQrImageView);

                receiveAddress.setText("");
                imageView.setImageBitmap(null);
            }
        }
        if (isVisibleToUser) {
            pausing = false;
        }
    }

    @Override
    public void onPause() {
        super.onDestroyView();

        if (getUserVisibleHint()) {
            pausing = true;
        }
    }

    @Override
    public View onGACreateView(final LayoutInflater inflater, final ViewGroup container,
                               final Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            pausing = savedInstanceState.getBoolean("pausing");
            address = savedInstanceState.getParcelable("address");
        }

        curSubaccount = getGAApp().getSharedPreferences("receive", Context.MODE_PRIVATE).getInt("curSubaccount", 0);

        final View rootView = inflater.inflate(R.layout.fragment_receive, container, false);
        final TextView receiveAddress = (TextView) rootView.findViewById(R.id.receiveAddressText);
        final TextView copyIcon = (TextView) rootView.findViewById(R.id.receiveCopyIcon);
        final TextView copyText = (TextView) rootView.findViewById(R.id.receiveCopyText);
        copyIcon.setVisibility(View.GONE);
        copyText.setVisibility(View.GONE);

        final TextView newAddressIcon = (TextView) rootView.findViewById(R.id.receiveNewAddressIcon);
        final ImageView imageView = (ImageView) rootView.findViewById(R.id.receiveQrImageView);
        final Animation iconPressed = AnimationUtils.loadAnimation(getActivity(), R.anim.icon_pressed);
        copyIcon.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(final View view) {
                        copyIcon.startAnimation(iconPressed);
                        // Gets a handle to the clipboard service.
                        final ClipboardManager clipboard = (ClipboardManager)
                                getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                        final ClipData clip = ClipData.newPlainText("data", receiveAddress.getText().toString().replace("\n", ""));
                        clipboard.setPrimaryClip(clip);

                        final CharSequence text = getActivity().getString(R.string.toastOnCopyAddress) + " " + getActivity().getString(R.string.warnOnPaste);

                        Toast.makeText(getActivity(), text, Toast.LENGTH_LONG).show();

                    }
                }
        );
        final View inflatedLayout = getActivity().getLayoutInflater().inflate(R.layout.dialog_qrcode, null, false);

        final ImageView qrcodeInDialog = (ImageView) inflatedLayout.findViewById(R.id.qrInDialogImageView);
        onAddress = new FutureCallback<QrBitmap>() {
            @Override
            public void onSuccess(@Nullable final QrBitmap result) {
                address = result;

                final Activity activity = getActivity();
                if (activity != null) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            copyIcon.setVisibility(View.VISIBLE);
                            copyText.setVisibility(View.VISIBLE);
                            stopNewAddressAnimation(rootView);
                            BitmapDrawable bd = new BitmapDrawable(getResources(), result.qrcode);
                            bd.setFilterBitmap(false);
                            imageView.setImageDrawable(bd);

                            receiveAddress.setText(result.data.substring(0, 12) + "\n" + result.data.substring(12, 24) + "\n" + result.data.substring(24));


                            imageView.setOnClickListener(new View.OnClickListener() {
                                public void onClick(final View view) {

                                    getActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (qrDialog == null) {
                                                final DisplayMetrics displaymetrics = new DisplayMetrics();
                                                getActivity().getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
                                                final int height = displaymetrics.heightPixels;
                                                final int width = displaymetrics.widthPixels;
                                                Log.i(TAG, height + "x" + width);
                                                final int min = (int) (Math.min(height, width) * 0.8);
                                                final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(min, min);
                                                qrcodeInDialog.setLayoutParams(layoutParams);

                                                qrDialog = new Dialog(getActivity());
                                                qrDialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
                                                qrDialog.setContentView(inflatedLayout);
                                            }
                                            qrDialog.show();
                                            BitmapDrawable bd = new BitmapDrawable(getResources(), result.qrcode);
                                            bd.setFilterBitmap(false);
                                            qrcodeInDialog.setImageDrawable(bd);
                                        }
                                    });
                                }
                            });
                        }
                    });
                }
            }

            @Override
            public void onFailure(final Throwable t) {
                t.printStackTrace();
                final Activity activity = getActivity();
                if (activity != null) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(activity, "Can't get a new address", Toast.LENGTH_LONG).show();
                            stopNewAddressAnimation(rootView);
                            copyIcon.setVisibility(View.VISIBLE);
                            copyText.setVisibility(View.VISIBLE);
                        }
                    });
                }
            }
        };

        if (address != null) {
            onAddress.onSuccess(address);
        }

        newAddressIcon.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(final View view) {
                        if (!getGAApp().getConnectionObservable().getState().equals(ConnectivityObservable.State.LOGGEDIN)) {
                            Toast.makeText(getActivity(), "Not connected, connection will resume automatically", Toast.LENGTH_LONG).show();
                            return;
                        }
                        startNewAddressAnimation(rootView);

                        final ListenableFuture<QrBitmap> ft = getGAService().getNewAddress(curSubaccount);
                        Futures.addCallback(ft, onAddress, getGAService().es);
                    }
                }
        );

        getGAApp().configureSubaccountsFooter(
                curSubaccount,
                getActivity(),
                (TextView) rootView.findViewById(R.id.sendAccountName),
                (LinearLayout) rootView.findViewById(R.id.receiveFooter),
                (LinearLayout) rootView.findViewById(R.id.footerClickableArea),
                new Function<Integer, Void>() {
                    @Nullable
                    @Override
                    public Void apply(@Nullable Integer input) {
                        curSubaccount = input;
                        final SharedPreferences.Editor editor = getGAApp().getSharedPreferences("receive", Context.MODE_PRIVATE).edit();
                        editor.putInt("curSubaccount", curSubaccount);
                        editor.apply();
                        startNewAddressAnimation(rootView);
                        Futures.addCallback(
                                getGAService().getLatestOrNewAddress(curSubaccount),
                                onAddress, getGAService().es);
                        return null;
                    }
                },
                rootView.findViewById(R.id.receiveNoTwoFacFooter)
        );


        return rootView;
    }

    private void stopNewAddressAnimation(final View rootView) {
        final FontAwesomeTextView newAddressIcon = (FontAwesomeTextView) rootView.findViewById(R.id.receiveNewAddressIcon);
        newAddressIcon.clearAnimation();
        newAddressIcon.setText(Html.fromHtml("&#xf067;"));
        final TextView copyIcon = (TextView) rootView.findViewById(R.id.receiveCopyIcon);
        final TextView copyText = (TextView) rootView.findViewById(R.id.receiveCopyText);
        copyIcon.setVisibility(View.VISIBLE);
        copyText.setVisibility(View.VISIBLE);
    }

    private void startNewAddressAnimation(final View rootView) {
        if (getActivity() == null) return;

        final FontAwesomeTextView newAddressIcon = (FontAwesomeTextView) rootView.findViewById(R.id.receiveNewAddressIcon);
        final Animation rotateAnim = AnimationUtils.loadAnimation(getActivity(), R.anim.rotation);
        newAddressIcon.startAnimation(rotateAnim);
        newAddressIcon.setText(Html.fromHtml("&#xf021;"));
        newAddressIcon.setAwesomeTypeface();
        newAddressIcon.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 34);

        final TextView receiveAddress = (TextView) rootView.findViewById(R.id.receiveAddressText);
        final TextView copyIcon = (TextView) rootView.findViewById(R.id.receiveCopyIcon);
        final TextView copyText = (TextView) rootView.findViewById(R.id.receiveCopyText);
        final ImageView imageView = (ImageView) rootView.findViewById(R.id.receiveQrImageView);
        copyIcon.setVisibility(View.GONE);
        copyText.setVisibility(View.GONE);
        receiveAddress.setText("");
        receiveAddress.setText("");
        imageView.setImageBitmap(null);
    }
}
