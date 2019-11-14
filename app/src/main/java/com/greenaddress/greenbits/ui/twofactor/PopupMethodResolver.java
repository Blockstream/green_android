package com.greenaddress.greenbits.ui.twofactor;

import android.app.Activity;
import android.util.Log;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.common.util.concurrent.SettableFuture;
import com.greenaddress.gdk.MethodResolver;
import com.greenaddress.greenbits.ui.GaActivity;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.UI;

import java.util.List;

public class PopupMethodResolver implements MethodResolver {
    private Activity activity;

    public PopupMethodResolver(final Activity activity) {
        this.activity = activity;
    }

    @Override
    public SettableFuture<String> method(List<String> methods) {
        final SettableFuture<String> future = SettableFuture.create();
        if (methods.size() == 1) {
            future.set(methods.get(0));
        } else {
            final MaterialDialog.Builder builder = UI.popup(activity, R.string.id_choose_method_to_authorize_the)
                                                   .cancelable(false)
                                                   .items(methods)
                                                   .itemsCallbackSingleChoice(0, (dialog, v, which, text) -> {
                Log.d("RSV", "PopupMethodResolver CHOOSE callback");
                future.set(methods.get(which));
                return true;
            })
                                                   .onNegative((dialog, which) -> {
                Log.d("RSV", "PopupMethodResolver CANCEL callback");
                future.set(null);
            });
            activity.runOnUiThread(() -> {
                Log.d("RSV", "PopupMethodResolver dialog show");
                builder.show();
            });
        }
        return future;
    }
}
