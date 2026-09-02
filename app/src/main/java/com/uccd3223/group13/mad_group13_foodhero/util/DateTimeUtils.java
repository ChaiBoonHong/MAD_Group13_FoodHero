package com.uccd3223.group13.mad_group13_foodhero.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateTimeUtils {
    public static String formatPickupWindow(String start, String end) {
        if (start == null && end == null) return "Pickup Today";
        return String.format(Locale.US, "Pickup: %s - %s", start != null ? start : "", end != null ? end : "");
    }

    public static String getCountdownText(String pickupEnd) {
        if (pickupEnd == null || !pickupEnd.contains(":")) {
            return "Ends soon";
        }
        try {
            String[] parts = pickupEnd.split(":");
            int endHour = Integer.parseInt(parts[0]);
            int endMin = Integer.parseInt(parts[1]);

            Calendar now = Calendar.getInstance();
            int curHour = now.get(Calendar.HOUR_OF_DAY);
            int curMin = now.get(Calendar.MINUTE);

            int diffMinutes = (endHour * 60 + endMin) - (curHour * 60 + curMin);
            if (diffMinutes <= 0) {
                return "Expired";
            } else if (diffMinutes < 60) {
                return diffMinutes + "m left";
            } else {
                int hours = diffMinutes / 60;
                int mins = diffMinutes % 60;
                return hours + "h " + mins + "m left";
            }
        } catch (Exception e) {
            return "Ends today";
        }
    }

    public static String formatFriendlyTime(long timestampMillis) {
        long diff = System.currentTimeMillis() - timestampMillis;
        if (diff < 60000) return "Just now";
        if (diff < 3600000) return (diff / 60000) + " mins ago";
        if (diff < 86400000) return (diff / 3600000) + " hours ago";
        return new SimpleDateFormat("dd MMM", Locale.US).format(new Date(timestampMillis));
    }
}
