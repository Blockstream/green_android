package com.greenaddress.greenbits.ui.twofactor;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.text.InputType;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.common.util.concurrent.SettableFuture;
import com.greenaddress.gdk.CodeResolver;
import com.greenaddress.greenapi.data.HWDeviceRequiredData;
import com.greenaddress.greenbits.ui.GaActivity;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.UI;

public class PopupCodeResolver implements CodeResolver {
    private Activity activity;

    public PopupCodeResolver(final Activity activity) {
        this.activity=activity;
    }

    @Override
    public SettableFuture<String> hardwareRequest(final HWDeviceRequiredData requiredData) {
        return null;
    }

    @Override
    public SettableFuture<String> code(final String method) {
        final SettableFuture<String> future = SettableFuture.create();

        final MaterialDialog.Builder builder =
            UI.popup(activity, activity.getString(R.string.id_please_provide_your_1s_code,
                                                  method))
            .inputType(InputType.TYPE_CLASS_NUMBER)
            .icon(getIconFor(method))
            .cancelable(false)
            .input("", "", (dialog, input) -> {
            Log.d("RSV", "PopupCodeResolver OK callback");
            future.set(input.toString());
        })
            .onNegative((dialog, which) -> {
            Log.d("RSV", "PopupCodeResolver CANCEL callback");
            future.set(null);
        });

        activity.runOnUiThread(() -> {
            Log.d("RSV", "PopupCodeResolver dialog show");
            builder.show();
        });

        return future;
    }

    private Drawable getIconFor(final String method) {
        switch (method) {
        case "email": return ContextCompat.getDrawable(activity, R.drawable.ic_2fa_email);
        case "sms": return ContextCompat.getDrawable(activity, R.drawable.ic_2fa_sms);
        case "gauth": return ContextCompat.getDrawable(activity, R.drawable.ic_2fa_google);
        case "phone": return ContextCompat.getDrawable(activity, R.drawable.ic_2fa_call);
        default: return null;
        }
    }
}
