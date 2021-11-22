package com.greenaddress.greenbits.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.res.Resources;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.greenaddress.greenapi.model.Conversion;

import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.util.Arrays;
import java.util.List;

public abstract class UI {
    private static final String TAG = UI.class.getSimpleName();

    public static String PACKAGE_NAME = "com.greenaddress.greenbits_android_wallet";

    static final int INVALID_RESOURCE_ID = 0;
    public static final String[] UNITS = {"BTC", "mBTC", "\u00B5BTC", "bits", "sats"};
    public static final String[] LIQUID_UNITS = {"L-BTC", "L-mBTC", "L-\u00B5BTC", "L-bits", "L-sats"};

    private static Function<String, String> toUnitKeyFunc = new Function<String, String>() {
        @NullableDecl
        @Override
        public String apply(@NullableDecl String input) {
            return input != null ? Conversion.toUnitKey(input) : null;
        }
    };
    public static final List<String> UNIT_KEYS_LIST = Lists.transform(Arrays.asList(UNITS), UI.toUnitKeyFunc);

    public static AlertDialog dismiss(final Activity a, final Dialog d) {
        if (d != null)
            if (a != null)
                a.runOnUiThread(new Runnable() { public void run() { d.dismiss(); } });
            else
                d.dismiss();
        return null;

    }

    public static MaterialAlertDialogBuilder popup(final Activity a, final String title, final int pos, final int neg) {
        final MaterialAlertDialogBuilder b;
        b = new MaterialAlertDialogBuilder(a, R.style.ThemeOverlay_Green_MaterialAlertDialog)
            .setTitle(title);
        if (pos != INVALID_RESOURCE_ID)
            b.setPositiveButton(pos, null);
        if (neg != INVALID_RESOURCE_ID)
            return b.setNegativeButton(neg, null);
        return b;
    }

    public static MaterialAlertDialogBuilder popup(final Activity a, final int title, final int pos, final int neg) {
        return popup(a, a.getString(title), pos, neg);
    }

    public static MaterialAlertDialogBuilder popup(final Activity a, final int title, final int pos) {
        return popup(a, title, pos, INVALID_RESOURCE_ID);
    }

    public static void toast(final Activity activity, final String msg, final Button reenable, final int len) {
        activity.runOnUiThread(() -> {
            if (reenable != null)
                reenable.setEnabled(true);
            Log.d(TAG, "Toast: " + msg);
            final Resources res = activity.getResources();
            final String translated = i18n(res, msg);
            final Toast t = Toast.makeText(activity, translated, len);
            try{
                final View v = t.getView();
                if(v != null) {
                    v.setBackgroundColor(0xaf000000);
                    ((TextView) v.findViewById(android.R.id.message)).setTextColor(ThemeUtils.resolveColorAccent(activity));
                }
            }catch (Exception e){
                e.printStackTrace();
            }

            t.show();
        });
    }

    public static void toast(final Activity activity, final int id, final Button reenable) {
        toast(activity, activity.getString(id), reenable);
    }

    public static void toast(final Activity activity, final String msg, final Button reenable) {
        toast(activity, msg, reenable, Toast.LENGTH_LONG);
    }

    public static void toast(final Activity activity, final Throwable t, final Button reenable) {
        t.printStackTrace();
        toast(activity, t.getMessage(), reenable, Toast.LENGTH_LONG);
    }

    public static void toast(final Activity activity, final int id, final int len) {
        toast(activity, activity.getString(id), null, len);
    }

    public static void toast(final Activity activity, final String s, final int len) {
        toast(activity, s, null, len);
    }

    // Show/Hide controls
    static void showIf(final boolean condition, final View v, final int hiddenViewState) {
        if (v != null)
            v.setVisibility(condition ? View.VISIBLE : hiddenViewState);
    }

    public static void showIf(final boolean condition, final View v) {
        showIf(condition, v, View.GONE);
    }

    public static void show(final View v) { showIf(true, v); }

    public static void hideIf(final boolean condition, final View v) {
        showIf(!condition, v);
    }

    public static void hide(final View v) { showIf(false, v); }

    // Enable/Disable controls
    public static void enableIf(final boolean condition, final View v) {
        v.setEnabled(condition);
    }

    public static void enable(final View v) { enableIf(true, v); }

    public static void disable(final View v) { enableIf(false, v); }

    public static void setText(final Activity activity, final int id, final int msgId) {
        final TextView t = find(activity, id);
        t.setText(msgId);
    }

    public static void clear(final TextView v) {
        v.setText(R.string.empty);
    }

    public static < T extends View > T find(final Activity activity, final int id) {
        return (T) activity.findViewById(id);
    }

    public static < T extends View > T find(final View v, final int id) {
        return (T) v.findViewById(id);
    }

    public static < T extends View > T find(final Dialog dialog, final int id) {
        return (T) dialog.findViewById(id);
    }

    // Return the translated string represented by the identifier given
    public static String i18n(final Resources res, final String textOrIdentifier) {
        if (TextUtils.isEmpty(textOrIdentifier))
            return "";
        if (!textOrIdentifier.startsWith("id_"))
            return textOrIdentifier; // Not a string id
        try {
            int resId = res.getIdentifier(textOrIdentifier, "string", PACKAGE_NAME);
            return res.getString(resId);
        } catch (final Exception e) {
            return textOrIdentifier; // Unknown id
        }
    }

}
