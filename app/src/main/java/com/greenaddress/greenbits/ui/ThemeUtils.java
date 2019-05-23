package com.greenaddress.greenbits.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.util.TypedValue;

import com.greenaddress.greenapi.data.NetworkData;

public class ThemeUtils {
    public static int getThemeFromNetworkId(final NetworkData net, final Context context, final Bundle metadata) {
        String baseTheme = "BitcoinTheme";
        if (net.getLiquid()) {
            baseTheme = "LiquidTheme";
        } else if (!net.getMainnet()) {
            baseTheme = "BitcoinTestnetTheme";
        }

        String finalTheme = applyThemeVariant(baseTheme, metadata);

        return context.getResources().getIdentifier(finalTheme, "style", context.getPackageName());
    }

    private static String applyThemeVariant(String baseTheme, Bundle metadata) {
        // get the "NoActionBar" variant of this theme
        if (metadata != null && metadata.getBoolean("useNoActionBar")) {
            baseTheme += ".NoActionBar";
        }

        return baseTheme;
    }

    public static TypedValue resolveAttribute(final Context context, final int attr) {
        // Resolve the value of an attribute based on the theme used by `context`
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(attr, typedValue, true);

        return typedValue;
    }

    public static @ColorInt int resolveColorAccent(final Context context) {
        return resolveAttribute(context, R.attr.colorAccent).data;
    }
}
