package com.blockstream.green.settings;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.blockstream.green.Preferences;
import com.greenaddress.greenbits.ui.preferences.PrefKeys;

import java.util.Map;
import java.util.Set;

public class MigratorJava {

    // migrate preferences from previous GreenBits app with single network
    static void migratePreferencesFromV2(Context context) {
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        final boolean migrated = sharedPreferences.getBoolean(Preferences.MIGRATED_V2_V3,false);
        if (!migrated) {
            // SPV_SYNCRONIZATION is now off by default unless a user had set the trusted peers,
            // in that case it stay how it was

            final SharedPreferences preferences = context.getSharedPreferences(getCurrentNetwork(context), MODE_PRIVATE);
            final boolean isEnabled = preferences.getBoolean(PrefKeys.SPV_ENABLED, false);
            final boolean haveTrustedPeers = !"".equals(preferences.getString(PrefKeys.TRUSTED_ADDRESS, "").trim());
            if (haveTrustedPeers && isEnabled) {
                preferences.edit().putBoolean(PrefKeys.SPV_ENABLED, true).apply();
            }
            // mainnet PIN migration
            copyPreferences(context.getSharedPreferences("pin", MODE_PRIVATE), context.getSharedPreferences("mainnet_pin", MODE_PRIVATE));
            sharedPreferences.edit().putBoolean(Preferences.MIGRATED_V2_V3, true).apply();
        }
    }

    private static void copyPreferences(final SharedPreferences source, final SharedPreferences destination) {
        if (source.getAll().isEmpty())
            return;
        final SharedPreferences.Editor destinationEditor = destination.edit();
        for (final Map.Entry<String, ?> entry : source.getAll().entrySet())
            writePreference(entry.getKey(), entry.getValue(), destinationEditor);
        destinationEditor.apply();
    }

    private static SharedPreferences.Editor writePreference(final String key, final Object value, final SharedPreferences.Editor preferences) {
        if (value instanceof Boolean)
            return preferences.putBoolean(key, (Boolean) value);
        else if (value instanceof String)
            return preferences.putString(key, (String) value);
        else if (value instanceof Long)
            return preferences.putLong(key, (Long) value);
        else if (value instanceof Integer)
            return preferences.putInt(key, (Integer) value);
        else if (value instanceof Float)
            return preferences.putFloat(key, (Float) value);
        else if (value instanceof Set)
            return preferences.putStringSet(key, (Set<String>) value);
        else
            throw new RuntimeException("Unknown preference type");
    }

    public static String getCurrentNetwork(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(PrefKeys.NETWORK_ID_ACTIVE, "mainnet");
    }

}
