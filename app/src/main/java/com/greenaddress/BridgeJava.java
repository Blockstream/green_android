package com.greenaddress;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.blockstream.libgreenaddress.GDK;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.greenaddress.gdk.JSONConverterImpl;
import com.greenaddress.greenapi.data.NetworkData;
import com.greenaddress.greenbits.ui.FailHardActivity;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.preferences.PrefKeys;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static android.content.Context.MODE_PRIVATE;
import static com.greenaddress.greenapi.Session.getSession;


// BridgeJava is the place where Java code is moved from all around v3 until the migration to v4 is complete.
// I choose to keep it that way to avoid converting the code to Kotlin adding extra
public class BridgeJava {
    public static void failHard(Context context, final String title, final String message) {
        final Intent fail = new Intent(context, FailHardActivity.class);
        fail.putExtra("errorTitle", title);
        final String supportMessage = "Please contact info@greenaddress.it for support.";
        fail.putExtra("errorContent", String.format("%s. %s", message, supportMessage));
        fail.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(fail);
    }

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
        final boolean res = PreferenceManager.getDefaultSharedPreferences(context).edit().putString(PrefKeys.NETWORK_ID_ACTIVE, networkId).commit();
        if (res == false) {
            failHard(context, context.getString(R.string.id_error), context.getString(R.string.id_operation_failure));
        }
    }

    public static String getCurrentNetwork(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(PrefKeys.NETWORK_ID_ACTIVE, "mainnet");
    }
    public static NetworkData getCurrentNetworkData(Context context) {
        return getNetworkData(getCurrentNetwork(context));
    }

}
