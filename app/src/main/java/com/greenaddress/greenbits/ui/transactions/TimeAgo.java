package com.greenaddress.greenbits.ui.transactions;

import android.content.Context;
import android.content.res.Resources;

import com.greenaddress.greenbits.ui.R;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TimeAgo {
    private static final List<Long> TIMES = Arrays.asList(
        TimeUnit.DAYS.toMillis(365),
        TimeUnit.DAYS.toMillis(30),
        TimeUnit.DAYS.toMillis(1),
        TimeUnit.HOURS.toMillis(1),
        TimeUnit.MINUTES.toMillis(1),
        TimeUnit.SECONDS.toMillis(1));

    public static String fromNow(final long date, final Context ctx) {
        final long timeDiff = (new Date()).getTime() - date;
        for (int i = 0; i < TIMES.size(); ++i) {
            final long timeSince = timeDiff / TIMES.get(i);
            if (timeSince > 0) {
                final Resources res = ctx.getResources();
                return ctx.getString(R.string.id_1d_2s_ago, timeSince,
                                     timeSince > 1 ?
                                     res.getStringArray(R.array.timesStringPlurals)[i] :
                                     res.getStringArray(R.array.timesStrings)[i]);
            }
        }
        return ctx.getString(R.string.id_now);
    }
}
