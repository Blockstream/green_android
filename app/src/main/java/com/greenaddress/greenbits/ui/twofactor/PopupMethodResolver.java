package com.greenaddress.greenbits.ui.twofactor;

import android.app.Activity;
import android.util.Log;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.common.util.concurrent.SettableFuture;
import com.greenaddress.gdk.MethodResolver;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.UI;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Deprecated
public class PopupMethodResolver implements MethodResolver {
    private Activity activity;
    private MaterialDialog dialog;

    public PopupMethodResolver(final Activity activity) {
        this.activity = activity;
    }

    @Override
    public SettableFuture<String> method(List<String> methods) {
        final SettableFuture<String> future = SettableFuture.create();
        if (methods.size() == 1) {
            future.set(methods.get(0));
        } else {

            List methodChoices = new ArrayList(methods);

            int i = 0;
            for (Iterator<String> it = methodChoices.iterator(); it.hasNext(); i++) {
                String method = it.next();
                if("email".equals(method)){
                    method = activity.getString(R.string.id_email);
                } else if("sms".equals(method)){
                    method = activity.getString(R.string.id_sms);
                } else if("gauth".equals(method)){
                    method = activity.getString(R.string.id_authenticator_app);
                }else if("phone".equals(method)){
                    method = activity.getString(R.string.id_phone_call);
                }

                methodChoices.set(i, method);
            }

            final MaterialDialog.Builder builder = UI.popup(activity, R.string.id_choose_method_to_authorize_the)
                                                   .cancelable(false)
                                                   .items(methodChoices)
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
                dialog = builder.show();
            });
        }
        return future;
    }

    @Override
    public void dismiss() {
        if (dialog != null) {
            activity.runOnUiThread(() -> {
                dialog.dismiss();
            });
        }
    }
}
