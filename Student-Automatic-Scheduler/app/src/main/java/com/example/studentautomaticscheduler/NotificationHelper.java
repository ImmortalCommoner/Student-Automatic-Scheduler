package com.example.studentautomaticscheduler;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationHelper {

    public static void scheduleClassReminders(Context context) {
        DatabaseHelper db = new DatabaseHelper(context);
        List<ScheduleItem> schedules = db.getAllSchedules();
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        for (ScheduleItem item : schedules) {
            scheduleNotification(context, alarmManager, item);
        }
    }

    private static void scheduleNotification(Context context, AlarmManager alarmManager, ScheduleItem item) {
        // time format: "11:00AM - 01:00PM"
        String startTimeStr = item.time.split("-")[0].trim();
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mma", Locale.US);

        try {
            Date startTime = sdf.parse(startTimeStr);
            Calendar classCalendar = Calendar.getInstance();
            Calendar now = Calendar.getInstance();

            Calendar timeCal = Calendar.getInstance();
            timeCal.setTime(startTime);

            classCalendar.set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY));
            classCalendar.set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE));
            classCalendar.set(Calendar.SECOND, 0);

            // Set the day of the week
            int dayOfWeek = getDayOfWeek(item.day);
            classCalendar.set(Calendar.DAY_OF_WEEK, dayOfWeek);

            // Schedule 10 minutes before class
            classCalendar.add(Calendar.MINUTE, -10);

            // If the time has already passed this week, schedule for next week
            if (classCalendar.before(now)) {
                classCalendar.add(Calendar.WEEK_OF_YEAR, 1);
            }

            Intent intent = new Intent(context, NotificationReceiver.class);
            intent.putExtra("subject", item.subject);
            intent.putExtra("room", item.room);
            intent.putExtra("time", item.time);

            // Unique ID for each notification based on hash of item properties
            int notificationId = (item.subject + item.day + item.time).hashCode();

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    notificationId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            if (alarmManager != null) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        classCalendar.getTimeInMillis(),
                        pendingIntent
                );
            }

        } catch (ParseException e) {
            Log.e("NotificationHelper", "Error parsing time: " + item.time, e);
        }
    }

    private static int getDayOfWeek(String shortDay) {
        switch (shortDay) {
            case "Mon": return Calendar.MONDAY;
            case "Tue": return Calendar.TUESDAY;
            case "Wed": return Calendar.WEDNESDAY;
            case "Thu": return Calendar.THURSDAY;
            case "Fri": return Calendar.FRIDAY;
            case "Sat": return Calendar.SATURDAY;
            case "Sun": return Calendar.SUNDAY;
            default: return Calendar.MONDAY;
        }
    }
}
