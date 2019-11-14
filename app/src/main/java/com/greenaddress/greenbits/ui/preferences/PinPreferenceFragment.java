package com.greenaddress.greenbits.ui.preferences;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.greenaddress.greenbits.AuthenticationHandler;
import com.greenaddress.greenbits.KeyStoreAES;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.UI;
import com.greenaddress.greenbits.ui.onboarding.PinSaveActivity;

import java.util.Observable;
import java.util.Observer;

import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import static android.app.Activity.RESULT_OK;
import static com.greenaddress.gdk.GDKSession.getSession;

public class PinPreferenceFragment extends GAPreferenceFragment implements Observer {
    private static final String TAG = GeneralPreferenceFragment.class.getSimpleName();

    private static final int ACTIVITY_REQUEST_PINSAVE = 100;
    private static final int ACTIVITY_REQUEST_NATIVE = 101;

    private SwitchPreference mPinPref;
    private SwitchPreference mNativePref;

    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        addPreferencesFromResource(R.xml.preference_pin);
        setHasOptionsMenu(true);

        if (getGAApp().getModel() == null) {
            logout();
            return;
        }

        mPinPref = find(PrefKeys.PIN_AUTH);
        mNativePref = find(PrefKeys.NATIVE_AUTH);

        mPinPref.setOnPreferenceChangeListener(this::onPinChanged);
        mNativePref.setOnPreferenceChangeListener(this::onNativeChanged);
        reload();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                KeyStoreAES.createKey(true, getNetwork().getNetwork());
            } catch (final Exception e) {
                mNativePref.setEnabled(false);
                mNativePref.setSummary(R.string.id_a_screen_lock_must_be_enabled);
            }
        }
    }

    private void reload() {
        final Context context = getContext();
        mPinPref.setChecked(AuthenticationHandler.getPinAuth(context) != null);
        mNativePref.setChecked(AuthenticationHandler.getNativeAuth(context) != null);
        mNativePref.setEnabled(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M);
    }

    @Override
    public void update(final Observable observable, final Object o) {}

    private boolean onPinChanged(final Preference preference, final Object newValue) {
        if ((Boolean) newValue)
            return onPinEnabled();
        else
            return onPinDisabled();
    }

    private boolean onPinEnabled() {
        if (getGAApp().warnIfOffline(getActivity()))
            return false;
        final Intent savePin = PinSaveActivity.createIntent(getActivity(), getSession().getMnemonicPassphrase());
        startActivityForResult(savePin, ACTIVITY_REQUEST_PINSAVE);
        return true;
    }

    private boolean onPinDisabled() {
        final Context context = getContext();
        final SharedPreferences pin = AuthenticationHandler.getPinAuth(context);
        if (pin == null) {
            UI.toast(getActivity(), R.string.id_please_disable_biometric, Toast.LENGTH_LONG);
            return false;
        }
        UI.popup(getActivity(), R.string.id_warning)
        .content(R.string.id_deleting_your_pin_will_remove)
        .cancelable(false)
        .onNegative((dlg, which) -> mPinPref.setChecked(true))
        .onPositive((dlg, which) -> {
            AuthenticationHandler.clean(context, pin);
        }).show();
        return true;
    }

    private boolean onNativeChanged(final Preference preference, final Object newValue) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return false;
        if ((Boolean) newValue)
            return onNativeEnabled();
        else
            return onNativeDisabled();
    }


    private boolean onNativeEnabled() {
        if (getGAApp().warnIfOffline(getActivity()))
            return false;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return false;
        if (AuthenticationHandler.getPinAuth(getContext()) == null) {
            UI.toast(getActivity(), R.string.id_please_enable_pin, Toast.LENGTH_LONG);
            return false;
        }

        try {
            final String mnemonic = getSession().getMnemonicPassphrase();
            final String network = getNetwork().getNetwork();
            final SharedPreferences preferences = AuthenticationHandler.getNewAuth(getContext());
            final String pin = KeyStoreAES.tryEncrypt(network, preferences);
            final ListenableFuture<Void> future = getGAApp().getExecutor().submit(() -> {
                getConnectionManager().setPin(mnemonic, pin, preferences);
                return null;
            });
            Futures.addCallback(future, new FutureCallback<Void>() {
                @Override
                public void onSuccess(final Void result) {}

                @Override
                public void onFailure(final Throwable t) {
                    UI.popup(getActivity(), R.string.id_warning).content(t.getMessage()).show();
                }
            }, getGAApp().getExecutor());
        } catch (final KeyStoreAES.RequiresAuthenticationScreen e) {
            try {
                KeyStoreAES.showAuthenticationScreen(getActivity(), "");
            } catch (final RuntimeException exception) {
                UI.popup(getActivity(), R.string.id_warning).content(e.getMessage()).show();
            }
            return false;
        } catch (final KeyStoreAES.KeyInvalidated e) {
            UI.popup(getActivity(), R.string.id_warning).content(R.string.id_problem_with_key_1s,
                                                                 e.getMessage()).show();
            return false;
        } catch (final RuntimeException e) {
            UI.popup(getActivity(), R.string.id_warning).content(e.getMessage()).show();
        }
        return true;
    }

    private boolean onNativeDisabled() {
        final Context context = getContext();
        final SharedPreferences pin = AuthenticationHandler.getNativeAuth(context);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return false;
        if (pin == null)
            return false;
        UI.popup(getActivity(), R.string.id_warning)
        .content(R.string.id_deleting_your_pin_will_remove)
        .cancelable(false)
        .onNegative((dlg, which) -> mNativePref.setChecked(true))
        .onPositive((dlg, which) -> {
            AuthenticationHandler.clean(context, pin);
        }).show();
        return true;
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        reload();
        if (requestCode == KeyStoreAES.ACTIVITY_REQUEST_CODE) {
            // Challenge completed, proceed with using cipher
            if (resultCode == RESULT_OK)
                mNativePref.setChecked(onNativeEnabled());
        }
    }
}