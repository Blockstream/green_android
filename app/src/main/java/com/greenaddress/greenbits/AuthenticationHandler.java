package com.greenaddress.greenbits;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.text.TextUtils;

import com.greenaddress.greenapi.data.PinData;
import com.greenaddress.greenbits.ui.preferences.PrefKeys;

import androidx.preference.PreferenceManager;

import static android.content.Context.MODE_PRIVATE;

public class AuthenticationHandler {

    public static SharedPreferences getNativeAuth(final Context context) {
        final SharedPreferences pin = cfgPin(context);
        final SharedPreferences secPin = cfgSecPin(context);
        if (isValid(pin) && isNative(pin))
            return pin;
        else if (isValid(secPin) && isNative(secPin))
            return secPin;
        return null;
    }

    public static SharedPreferences getPinAuth(final Context context) {
        final SharedPreferences pin = cfgPin(context);
        final SharedPreferences secPin = cfgSecPin(context);
        if (isValid(pin) && !isNative(pin))
            return pin;
        else if (isValid(secPin) && !isNative(secPin))
            return secPin;
        return null;
    }

    public static SharedPreferences getNewAuth(final Context context) {
        if (!isValid(cfgPin(context)))
            return cfgPin(context);
        else
            return cfgSecPin(context);
    }

    public static boolean hasPin(final Context context) {
        return getPinAuth(context) != null || getNativeAuth(context) != null;
    }

    private static boolean isValid(final SharedPreferences pin) {
        return pin != null && pin.getString("ident", null) != null;
    }

    private static boolean isNative(final SharedPreferences pin) {
        final String nativePin = pin != null ? pin.getString("native", null) : null;
        return nativePin != null && !TextUtils.isEmpty(nativePin);
    }

    public static String network(final Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(PrefKeys.NETWORK_ID_ACTIVE, "mainnet");
    }

    private static SharedPreferences cfgPin(final Context context) {
        return context.getSharedPreferences(network(context) + "_pin", MODE_PRIVATE);
    }

    private static SharedPreferences cfgSecPin(final Context context) {
        return context.getSharedPreferences(network(context) + "_pin_sec", MODE_PRIVATE);
    }

    public static void setPin(final PinData pinData, final Boolean isSixDigit, final SharedPreferences preferences) {
        preferences.edit().putString("ident", pinData.getPinIdentifier())
                    .putString("encrypted", pinData.getEncryptedGB())
                    .putInt("counter", 0)
                    .putBoolean("is_six_digit", isSixDigit)
                    .apply();
    }

    public static void clean(final Context context, final SharedPreferences pin) {
        // The user has set a non-native PIN.
        // In case they already had a native PIN they are overriding,
        // blank the native value so future logins don't detect it.
        // FIXME: Requiring M or higher is required because otherwise this crashes @ android < 21
        // and native is not available before M anyway
        // java.lang.VerifyError: com/greenaddress/greenbits/KeyStoreAES
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            KeyStoreAES.wipePIN(pin, network(context));
        pin.edit().clear().commit();
    }
}
