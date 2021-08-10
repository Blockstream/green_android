package com.greenaddress;

import static com.greenaddress.greenapi.Session.getSession;

import android.content.Context;
import android.preference.PreferenceManager;

import com.greenaddress.greenapi.data.NetworkData;
import com.greenaddress.greenbits.ui.preferences.PrefKeys;

import java.util.List;


// BridgeJava is the place where Java code is moved from all around v3 until the migration to v4 is complete.
// I choose to keep it that way to avoid converting the code to Kotlin adding extra
public class BridgeJava {

    // get Network function
    public static NetworkData getNetworkData(final String network) {
        final List<NetworkData> networks = getSession().getNetworks();
        for (final NetworkData n : networks) {
            if (n.getNetwork().equals(network)) {
                return n;
            }
        }
        return networks.get(0);
    }

    public static void setCurrentNetwork(Context context, final String networkId) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(PrefKeys.NETWORK_ID_ACTIVE, networkId).apply();
    }

    public static String getCurrentNetwork(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(PrefKeys.NETWORK_ID_ACTIVE, "mainnet");
    }
    public static NetworkData getCurrentNetworkData(Context context) {
        return getNetworkData(getCurrentNetwork(context));
    }

}
