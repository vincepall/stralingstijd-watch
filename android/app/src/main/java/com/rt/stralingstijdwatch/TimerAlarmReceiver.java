package com.rt.stralingstijdwatch;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;

public class TimerAlarmReceiver extends BroadcastReceiver {
    public static final String ACTION_TIMER_ALARM = "com.rt.stralingstijdwatch.ACTION_TIMER_ALARM";
    public static final String ACTION_STOP_ALARM = "com.rt.stralingstijdwatch.ACTION_STOP_ALARM";
    public static final String CHANNEL_ALARM_ID = "stralingstijd_alarm_channel";
    public static final int NOTIFICATION_ID = 2001;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();

        if (ACTION_STOP_ALARM.equals(action)) {
            // Stop alarm action
            AlarmHelper.stopAlarm(context);
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.cancel(NOTIFICATION_ID);
            }
            Intent stopIntent = new Intent(context, MainActivity.class);
            stopIntent.setAction(ACTION_STOP_ALARM);
            stopIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            context.startActivity(stopIntent);
            return;
        }

        // Direct start loud audio alarm and repeating vibration!
        AlarmHelper.startAlarm(context);

        // Acquire wake lock to wake the watch CPU and screen
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            PowerManager.WakeLock wl = pm.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    "stralingstijd:alarm_broadcast"
            );
            wl.acquire(15000); // 15 seconds wake lock
        }

        // Create FullScreenIntent to open MainActivity
        Intent activityIntent = new Intent(context, MainActivity.class);
        activityIntent.setAction(ACTION_TIMER_ALARM);
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(context, 1002, activityIntent, flags);

        // Action to stop alarm directly
        Intent stopIntent = new Intent(context, TimerAlarmReceiver.class);
        stopIntent.setAction(ACTION_STOP_ALARM);
        PendingIntent stopPendingIntent = PendingIntent.getBroadcast(context, 1003, stopIntent, flags);

        // Show High Priority Alarm Notification with FullScreenIntent
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ALARM_ID,
                        "Stralingstijd Alarm",
                        NotificationManager.IMPORTANCE_HIGH
                );
                channel.setDescription("Alarm wanneer stralingstijd is verstreken");
                channel.enableVibration(true);
                channel.setBypassDnd(true);
                nm.createNotificationChannel(channel);
            }

            Notification.Builder builder = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    ? new Notification.Builder(context, CHANNEL_ALARM_ID)
                    : new Notification.Builder(context);

            builder.setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("⚠ STRALINGSTIJD VERSTREKEN! ⚠")
                    .setContentText("Opname voltooid - bron intrekken!")
                    .setContentIntent(fullScreenPendingIntent)
                    .setFullScreenIntent(fullScreenPendingIntent, true)
                    .setAutoCancel(true)
                    .setCategory(Notification.CATEGORY_ALARM)
                    .setPriority(Notification.PRIORITY_MAX);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
                Notification.Action stopAction = new Notification.Action.Builder(
                        R.drawable.btn_timer_stop, "STOP ALARM", stopPendingIntent).build();
                builder.addAction(stopAction);
            }

            nm.notify(NOTIFICATION_ID, builder.build());
        }

        // Direct launch of MainActivity
        try {
            context.startActivity(activityIntent);
        } catch (Exception ignored) {}
    }
}
