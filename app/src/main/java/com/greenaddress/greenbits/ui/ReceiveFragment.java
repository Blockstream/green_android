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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Html;
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
import com.google.common.util.concurrent.ListenableFuture;
import com.greenaddress.greenapi.Network;
import com.greenaddress.greenbits.ConnectivityObservable;
import com.greenaddress.greenbits.QrBitmap;

import org.bitcoinj.core.Address;
import org.bitcoinj.uri.BitcoinURI;

import nordpol.android.OnDiscoveredTagListener;
import nordpol.android.TagDispatcher;


public class ReceiveFragment extends SubaccountFragment implements OnDiscoveredTagListener {
    @NonNull private static final String TAG = ReceiveFragment.class.getSimpleName();

    @Nullable
    private FutureCallback<QrBitmap> onAddress = null;
    @Nullable
    private QrBitmap address = null;
    private int curSubaccount;
    private boolean pausing = false;
    private Dialog qrDialog;
    private TagDispatcher tagDispatcher;

    @Override
    public void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean("pausing", pausing);
        if (address != null) {
            outState.putParcelable("address", address);
        }
    }

    @Override
    public void setUserVisibleHint(final boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        final View rootView = getView();

        if (!pausing && rootView != null) {
            // get a new address every time the tab is displayed
            if (isVisibleToUser) {
                hideKeyboard();
                // get a new address:
                if (address == null && !setting_qrcode) {
                    setting_qrcode = true;
                    final ListenableFuture<QrBitmap> ft = getGAService().getNewAddress(curSubaccount);
                    Futures.addCallback(ft, onAddress, getGAService().es);
                    startNewAddressAnimation(rootView);
                }
            } else { // !isVisibleToUser
                // hide to avoid showing old address when swiping
                final TextView receiveAddress = (TextView) rootView.findViewById(R.id.receiveAddressText);
                final ImageView imageView = (ImageView) rootView.findViewById(R.id.receiveQrImageView);
                address = null;
                receiveAddress.setText("");
                imageView.setImageBitmap(null);
            }
        }
        if (isVisibleToUser) {
            pausing = false;
        }
    }

    boolean setting_qrcode = false;

    @Override
    public void onResume() {
        Log.i(TAG, "onResume");
        super.onResume();
        if (onAddress != null && address == null && !setting_qrcode) {
            setting_qrcode = true;
            final ListenableFuture<QrBitmap> ft = getGAService().getNewAddress(curSubaccount);
            Futures.addCallback(ft, onAddress, getGAService().es);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (getUserVisibleHint()) {
            pausing = true;
        }
        
        tagDispatcher.disableExclusiveNfc();
    }

    private View rootView;

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, final ViewGroup container,
                             @Nullable final Bundle savedInstanceState) {
        final GaActivity gaActivity = getGaActivity();

        registerReceiver();

        if (savedInstanceState != null) {
            pausing = savedInstanceState.getBoolean("pausing");
            address = savedInstanceState.getParcelable("address");
        }
        
        tagDispatcher = TagDispatcher.get(getActivity(), this);
        tagDispatcher.enableExclusiveNfc();

        curSubaccount = getGAService().cfg("main").getInt("curSubaccount", 0);

        rootView = inflater.inflate(R.layout.fragment_receive, container, false);
        final TextView receiveAddress = (TextView) rootView.findViewById(R.id.receiveAddressText);
        final TextView copyIcon = (TextView) rootView.findViewById(R.id.receiveCopyIcon);
        final TextView copyText = (TextView) rootView.findViewById(R.id.receiveCopyText);
        copyIcon.setVisibility(View.GONE);
        copyText.setVisibility(View.GONE);

        final TextView newAddressIcon = (TextView) rootView.findViewById(R.id.receiveNewAddressIcon);
        final ImageView imageView = (ImageView) rootView.findViewById(R.id.receiveQrImageView);
        copyIcon.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(final View view) {
                        // Gets a handle to the clipboard service.
                        final ClipboardManager clipboard = (ClipboardManager)
                                getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                        final ClipData clip = ClipData.newPlainText("data", receiveAddress.getText().toString().replace("\n", ""));
                        clipboard.setPrimaryClip(clip);

                        final String text = gaActivity.getString(R.string.toastOnCopyAddress) + " " + gaActivity.getString(R.string.warnOnPaste);
                        gaActivity.toast(text);
                    }
                }
        );
        final View inflatedLayout = getActivity().getLayoutInflater().inflate(R.layout.dialog_qrcode, null, false);

        final ImageView qrcodeInDialog = (ImageView) inflatedLayout.findViewById(R.id.qrInDialogImageView);
        onAddress = new FutureCallback<QrBitmap>() {

            private void onUiThread(@Nullable final QrBitmap result) {
                address = result;

                final Activity activity = getActivity();
                if (activity != null) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {


                            final Activity activity = getActivity();
                            if (activity == null) {
                                return;
                            }

                            copyIcon.setVisibility(View.VISIBLE);
                            copyText.setVisibility(View.VISIBLE);
                            stopNewAddressAnimation(rootView);
                            final BitmapDrawable bd = new BitmapDrawable(getResources(), result.qrcode);
                            bd.setFilterBitmap(false);
                            imageView.setImageDrawable(bd);

                            receiveAddress.setText(String.format("%s\n%s\n%s", result.data.substring(0, 12), result.data.substring(12, 24), result.data.substring(24)));
                            setting_qrcode = false;


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
                                            final BitmapDrawable bd = new BitmapDrawable(getResources(), result.qrcode);
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
            public void onSuccess(@Nullable final QrBitmap result) {
                onUiThread(result);
            }

            @Override
            public void onFailure(@NonNull final Throwable t) {
                t.printStackTrace();
                final Activity activity = getActivity();
                if (activity != null) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
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
                        if (!setting_qrcode) {
                            // FIXME: Instead of checking the state here, enable/disable sendButton when state changes
                            if (!getGAApp().getConnectionObservable().getState().equals(ConnectivityObservable.State.LOGGEDIN)) {
                                gaActivity.toast(R.string.err_send_not_connected_will_resume);
                                return;
                            }
                            setting_qrcode = true;

                            startNewAddressAnimation(rootView);

                            final ListenableFuture<QrBitmap> ft = getGAService().getNewAddress(curSubaccount);
                            Futures.addCallback(ft, onAddress, getGAService().es);
                        }
                    }
                }
        );

        return rootView;
    }

    private void stopNewAddressAnimation(@NonNull final View rootView) {
        final FontAwesomeTextView newAddressIcon = (FontAwesomeTextView) rootView.findViewById(R.id.receiveNewAddressIcon);
        newAddressIcon.clearAnimation();
        newAddressIcon.setText(Html.fromHtml("&#xf067;"));
        final TextView copyIcon = (TextView) rootView.findViewById(R.id.receiveCopyIcon);
        final TextView copyText = (TextView) rootView.findViewById(R.id.receiveCopyText);
        copyIcon.setVisibility(View.VISIBLE);
        copyText.setVisibility(View.VISIBLE);
    }

    private void startNewAddressAnimation(@NonNull final View rootView) {
        if (getActivity() == null) return;

        final FontAwesomeTextView newAddressIcon = (FontAwesomeTextView) rootView.findViewById(R.id.receiveNewAddressIcon);
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
        imageView.setImageBitmap(null);
    }

    @Override
    public void tagDiscovered(final Tag t) {
    	Log.d("NFC", "Tag discovered " + t);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu (final Menu menu, final MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.receive, menu);
    }

    @Override
    protected void onSubaccountChanged(final int input) {
        curSubaccount = input;
        if (rootView != null) {
            startNewAddressAnimation(rootView);
        }
        if (!setting_qrcode) {
            setting_qrcode = true;

            Futures.addCallback(
                    getGAService().getNewAddress(curSubaccount),
                    onAddress, getGAService().es);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        final int id = item.getItemId();
        if (id == R.id.action_share) {
            if (address != null && !address.data.isEmpty()) {
                //SHARE intent
                final Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, BitcoinURI.convertToBitcoinURI(Address.fromBase58(Network.NETWORK, address.data), null, null, null));
                sendIntent.setType("text/plain");
                startActivity(sendIntent);
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
