package com.greenaddress.greenbits.ui;

import android.content.Context;

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

    private static String[] getTimesString(final Context context) {
        if (timesString == null) {
            timesString = context.getResources().getStringArray(R.array.timesStrings);
        }
        return timesString;
    }

    private static String[] getTimesStringPlurals(final Context context) {
        if (timesStringPlurals == null) {
            timesStringPlurals = context.getResources().getStringArray(R.array.timesStringPlurals);
        }
        return timesStringPlurals;
    }

    private static String toDuration(final long duration, Context context) {


        final StringBuilder res = new StringBuilder();
        for (int i = 0; i < times.size(); ++i) {
            final long current = times.get(i);
            final long temp = duration / current;
            if (temp > 0) {
                res.append(temp)
                        .append(" ")
                        .append(temp > 1 ? getTimesStringPlurals(context)[i] : getTimesString(context)[i])
                        .append(" ")
                        .append(context.getString(R.string.ago));
                break;
            }
        }
        return (res.length() == 0) ? "NOW" : res.toString();
    }

    public static String fromNow(final long date, final Context context) {
        return toDuration((new Date()).getTime() - date, context);
    }
}
