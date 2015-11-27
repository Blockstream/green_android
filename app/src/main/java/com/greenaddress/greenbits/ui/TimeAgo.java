package com.greenaddress.greenbits.ui;

import android.content.Context;
import android.support.annotation.NonNull;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

class TimeAgo {
    private static final List<Long> times = Arrays.asList(
            TimeUnit.DAYS.toMillis(365),
            TimeUnit.DAYS.toMillis(30),
            TimeUnit.DAYS.toMillis(1),
            TimeUnit.HOURS.toMillis(1),
            TimeUnit.MINUTES.toMillis(1),
            TimeUnit.SECONDS.toMillis(1));
    private static String[] timesString;

    private static String[] timesStringPlurals;

    private static String[] getTimesString(@NonNull final Context context) {
        if (timesString == null) {
            timesString = context.getResources().getStringArray(R.array.timesStrings);
        }
        return timesString;
    }

    private static String[] getTimesStringPlurals(@NonNull final Context context) {
        if (timesStringPlurals == null) {
            timesStringPlurals = context.getResources().getStringArray(R.array.timesStringPlurals);
        }
        return timesStringPlurals;
    }

    public static String fromNow(final long date, @NonNull final Context context) {
        final long timeDiff = (new Date()).getTime() - date;
        for (int i = 0; i < times.size(); ++i) {
            final long timeSince = timeDiff / times.get(i);
            if (timeSince > 0) {
                return String.format("%s %s %s", timeSince, timeSince > 1
                                ? getTimesStringPlurals(context)[i] : getTimesString(context)[i],
                        context.getString(R.string.ago));
            }
        }
        return "NOW";
    }
}
