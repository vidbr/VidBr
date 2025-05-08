package com.video.vidbr.util;

import android.content.Context;

import com.google.firebase.Timestamp;
import com.video.vidbr.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class TimeUtils {

    private static final long SECOND = TimeUnit.SECONDS.toMillis(1);
    private static final long MINUTE = TimeUnit.MINUTES.toMillis(1);
    private static final long HOUR = TimeUnit.HOURS.toMillis(1);
    private static final long DAY = TimeUnit.DAYS.toMillis(1);
    private static final long WEEK = 7 * DAY;
    private static final long MONTH = 30 * DAY; // Aproximação para um mês

    /**
     * Returns a human-readable representation of the time ago from the given timestamp.
     *
     * @param timestamp The timestamp to compare with the current time.
     * @return A string representing the time ago.
     */
    public static String getTimeAgo(Timestamp timestamp, Context context) {
        long currentTime = System.currentTimeMillis();
        long timeDifference = currentTime - timestamp.toDate().getTime();

        if (timeDifference < SECOND) {
            return context.getString(R.string.just_now);
        } else if (timeDifference < MINUTE) {
            long seconds = TimeUnit.MILLISECONDS.toSeconds(timeDifference);
            return String.format(context.getString(R.string.seconds_ago), seconds);
        } else if (timeDifference < HOUR) {
            long minutes = TimeUnit.MILLISECONDS.toMinutes(timeDifference);
            return String.format(context.getString(R.string.minutes_ago), minutes);
        } else if (timeDifference < DAY) {
            long hours = TimeUnit.MILLISECONDS.toHours(timeDifference);
            return String.format(context.getString(R.string.hours_ago), hours);
        } else if (timeDifference < WEEK) {
            long days = TimeUnit.MILLISECONDS.toDays(timeDifference);
            return String.format(context.getString(R.string.days_ago), days);
        } else if (timeDifference < MONTH) {
            long weeks = timeDifference / WEEK;
            return String.format(context.getString(R.string.weeks_ago), weeks);
        } else {
            long months = timeDifference / MONTH;
            return String.format(context.getString(R.string.months_ago), months);
        }
    }
}
