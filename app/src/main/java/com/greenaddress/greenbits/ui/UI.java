package com.greenaddress.greenbits.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.greenaddress.greenbits.GaService;

import java.util.List;

public abstract class UI {
    public static final int INVALID_RESOURCE_ID = 0;

    public static TextView.OnEditorActionListener getListenerRunOnEnter(final Runnable r) {
        return new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(final TextView v, final int actionId, final KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                        actionId == EditorInfo.IME_ACTION_DONE ||
                        (event != null && event.getAction() == KeyEvent.ACTION_DOWN) &&
                                event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    if (event == null || !event.isShiftPressed()) {
                        // the user is done typing.
                        r.run();
                        return true; // consume.
                    }
                }
                return false; // pass on to other listeners.
            }
        };
    }

    public static MaterialDialog.Builder popup(final Activity a, final String title, final int pos, final int neg) {
        final MaterialDialog.Builder b;
        b = new MaterialDialog.Builder(a)
                .title(title)
                .titleColorRes(R.color.white)
                .positiveColorRes(R.color.accent)
                .negativeColorRes(R.color.accent)
                .contentColorRes(R.color.white)
                .theme(Theme.DARK);
        if (pos != INVALID_RESOURCE_ID)
            b.positiveText(pos);
        if (neg != INVALID_RESOURCE_ID)
            return b.negativeText(neg);
        return b;
    }

    public static MaterialDialog.Builder popup(final Activity a, final int title, final int pos, final int neg) {
        return popup(a, a.getString(title), pos, neg);
    }

    public static MaterialDialog.Builder popup(final Activity a, final String title, final int pos) {
        return popup(a, title, pos, INVALID_RESOURCE_ID);
    }

    public static MaterialDialog.Builder popup(final Activity a, final int title, final int pos) {
        return popup(a, title, pos, INVALID_RESOURCE_ID);
    }

    public static MaterialDialog.Builder popup(final Activity a, final String title) {
        return popup(a, title, android.R.string.ok, android.R.string.cancel);
    }

    public static MaterialDialog.Builder popup(final Activity a, final int title) {
        return popup(a, title, android.R.string.ok, android.R.string.cancel);
    }

    public static MaterialDialog popupTwoFactorChoice(final Activity a, final GaService service,
                                                      final boolean skip, final CB.Runnable1T<String> callback) {
        final List<String> names = service.getEnabledTwoFacNames(false);

        if (skip || names.size() <= 1) {
            // Caller elected to skip, or no choices are available: don't prompt
            a.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    callback.run(names.isEmpty() ? null : service.getEnabledTwoFacNames(true).get(0));
                }
            });
            return null;
        }

        // Return a pop up dialog to let the user choose.
        String[] namesArray = new String[names.size()];
        namesArray = names.toArray(namesArray);
        return popup(a, R.string.twoFactorChoicesTitle, R.string.choose, R.string.cancel)
                .items(namesArray)
                .itemsCallbackSingleChoice(0, new MaterialDialog.ListCallbackSingleChoice() {
                    @Override
                    public boolean onSelection(MaterialDialog dialog, View v, int which, CharSequence text) {
                        final List<String> systemNames = service.getEnabledTwoFacNames(true);
                        callback.run(systemNames.get(which));
                        return true;
                    }
                }).build();
    }

    public static void toast(final Activity activity, final int id, final int len) {
        activity.runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(activity, id, len).show();
            }
        });
    }

    public static void toast(final Activity activity, final String s, final int len) {
        activity.runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(activity, s, len).show();
            }
        });
    }

    public static void toast(final Activity activity, final Throwable t, final Button reenable) {
        activity.runOnUiThread(new Runnable() {
            public void run() {
                if (reenable != null)
                    reenable.setEnabled(true);
                t.printStackTrace();
                Toast.makeText(activity, t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    public static View mapClick(final Activity activity, final int id, final View.OnClickListener fn) {
        final View v = activity.findViewById(id);
        v.setOnClickListener(fn);
        return v;
    }

    public static View mapClick(final Activity activity, final int id, final Intent activityIntent) {
        return mapClick(activity, id, new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                activity.startActivity(activityIntent);
            }
        });
    }

    public static void showIf(final boolean condition, final View... views) {
        for (View v: views)
            v.setVisibility(condition ? View.VISIBLE : View.GONE);
    }

    public static void show(final View... views) { showIf(true, views); }

    public static void hideIf(final boolean condition, final View... views) {
        showIf(!condition, views);
    }

    public static void hide(final View... views) { showIf(false, views); }

    public static String getText(TextView text) {
        return text.getText().toString();
    }

    public static <T> T find(final Activity activity, int id) {
        return (T) activity.findViewById(id);
    }

    public static <T> T find(final View v, int id) {
        return (T) v.findViewById(id);
    }

    public static void showDialog(final Dialog dialog) {
        // (FIXME not sure if there's any smaller subset of these 3 calls below which works too)
        dialog.getWindow().clearFlags(LayoutParams.FLAG_NOT_FOCUSABLE |
                                   LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        dialog.getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dialog.show();
    }
}
