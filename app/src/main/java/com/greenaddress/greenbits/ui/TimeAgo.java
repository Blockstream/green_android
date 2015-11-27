package com.greenaddress.greenbits.ui;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

class TimeAgo {
    @NonNull
    private static final List<Long> TIMES = Arrays.asList(
            TimeUnit.DAYS.toMillis(365),
            TimeUnit.DAYS.toMillis(30),
            TimeUnit.DAYS.toMillis(1),
            TimeUnit.HOURS.toMillis(1),
            TimeUnit.MINUTES.toMillis(1),
            TimeUnit.SECONDS.toMillis(1));

    @NonNull
    public static String fromNow(final long date, @NonNull final Context ctx) {
        final long timeDiff = (new Date()).getTime() - date;
        for (int i = 0; i < TIMES.size(); ++i) {
            final long timeSince = timeDiff / TIMES.get(i);
            if (timeSince > 0) {
                final Resources res = ctx.getResources();
                return String
                        .format("%s %s %s",
                                timeSince,
                                timeSince > 1 ?
                                        res.getStringArray(R.array.timesStringPlurals)[i] :
                                        res.getStringArray(R.array.timesStrings)[i],
                                ctx.getString(R.string.ago));
            }
        }
        return "NOW";
    }
}