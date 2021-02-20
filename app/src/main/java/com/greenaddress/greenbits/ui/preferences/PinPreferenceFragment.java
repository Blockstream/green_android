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
import com.greenaddress.greenapi.data.PinData;
import com.greenaddress.greenbits.AuthenticationHandler;
import com.greenaddress.greenbits.KeyStoreAES;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.UI;
import com.greenaddress.greenbits.ui.onboarding.PinSaveActivity;

import androidx.preference.Preference;
import androidx.preference.SwitchPreference;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static android.app.Activity.RESULT_OK;


public class PinPreferenceFragment extends GAPreferenceFragment {
    private static final String TAG = GeneralPreferenceFragment.class.getSimpleName();

    private static final int ACTIVITY_REQUEST_PINSAVE = 100;
    private static final int ACTIVITY_REQUEST_NATIVE = 101;

    private SwitchPreference mPinPref;
    private SwitchPreference mNativePref;
    private Disposable saveDisposable;

    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        addPreferencesFromResource(R.xml.preference_pin);
        setHasOptionsMenu(true);

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

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (saveDisposable != null)
            saveDisposable.dispose();
    }

    private void reload() {
        final Context context = getContext();
        mPinPref.setChecked(AuthenticationHandler.getPinAuth(context) != null);
        mNativePref.setChecked(AuthenticationHandler.getNativeAuth(context) != null);
        mNativePref.setEnabled(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M);
    }

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
        final SharedPreferences pinNative = AuthenticationHandler.getNativeAuth(context);
        if (pinNative != null) {
            UI.toast(getActivity(), R.string.id_please_disable_biometric, Toast.LENGTH_LONG);
            return false;
        }
        UI.popup(getActivity(), R.string.id_warning)
        .content(R.string.id_deleting_your_pin_will_remove)
        .cancelable(false)
        .onNegative((dlg, which) -> mPinPref.setChecked(true))
        .onPositive((dlg, which) -> {
            final SharedPreferences pin = AuthenticationHandler.getPinAuth(context);
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
        if (AuthenticationHandler.getPinAuth(getContext()) == null) {
            UI.toast(getActivity(), R.string.id_please_enable_pin, Toast.LENGTH_LONG);
            return false;
        }

        try {
            final String network = getNetwork().getNetwork();
            final SharedPreferences preferences = AuthenticationHandler.getNewAuth(getContext());
            final String pin = KeyStoreAES.tryEncrypt(network, preferences);
            saveDisposable = Observable.just(getSession())
                             .observeOn(Schedulers.computation())
                             .map((session) -> {
                final String mnemonic = getSession().getMnemonicPassphrase();
                return session.setPin(mnemonic, pin, "default");
            })
                             .observeOn(AndroidSchedulers.mainThread())
                             .subscribe((pinData) -> {
                AuthenticationHandler.setPin(pinData, pin.length() == 6, preferences);
                getSession().setPinJustSaved(true);
            }, (e) -> {
                UI.popup(getActivity(), R.string.id_warning).content(e.getMessage()).show();
            });
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
        if (pin == null)
            return false;
        AuthenticationHandler.clean(context, pin);
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