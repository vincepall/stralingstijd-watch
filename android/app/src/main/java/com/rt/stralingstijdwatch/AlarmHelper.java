package com.rt.stralingstijdwatch;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;

public class AlarmHelper {
    private static Ringtone ringtone;
    private static PowerManager.WakeLock alarmWakeLock;
    private static boolean isRinging = false;

    public static synchronized void startAlarm(Context context) {
        if (isRinging) return;
        isRinging = true;

        // 1. Wake lock to keep watch CPU awake while alarm is ringing
        try {
            if (alarmWakeLock == null) {
                PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                if (pm != null) {
                    alarmWakeLock = pm.newWakeLock(
                            PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                            "stralingstijd:alarm_helper"
                    );
                    alarmWakeLock.setReferenceCounted(false);
                }
            }
            if (alarmWakeLock != null && !alarmWakeLock.isHeld()) {
                alarmWakeLock.acquire(60000); // 60 seconds max
            }
        } catch (Exception ignored) {}

        // 2. Continuous repeating alarm vibration
        try {
            Vibrator vibrator = getVibrator(context);
            if (vibrator != null && vibrator.hasVibrator()) {
                long[] pattern = {0, 500, 150, 500, 150, 800, 300};
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    AudioAttributes audioAttr = new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build();
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0), audioAttr);
                } else {
                    vibrator.vibrate(pattern, 0);
                }
            }
        } catch (Exception ignored) {}

        // 3. Audio alarm sound
        try {
            Uri alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alertUri == null) {
                alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }
            if (alertUri == null) {
                alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            }
            if (alertUri != null) {
                ringtone = RingtoneManager.getRingtone(context.getApplicationContext(), alertUri);
                if (ringtone != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        ringtone.setAudioAttributes(new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build());
                    }
                    ringtone.play();
                }
            }
        } catch (Exception ignored) {}
    }

    public static synchronized void stopAlarm(Context context) {
        isRinging = false;

        // 1. Stop audio sound
        try {
            if (ringtone != null) {
                if (ringtone.isPlaying()) {
                    ringtone.stop();
                }
                ringtone = null;
            }
        } catch (Exception ignored) {}

        // 2. Stop vibration
        try {
            Vibrator vibrator = getVibrator(context);
            if (vibrator != null) {
                vibrator.cancel();
            }
        } catch (Exception ignored) {}

        // 3. Release alarm wake lock
        try {
            if (alarmWakeLock != null && alarmWakeLock.isHeld()) {
                alarmWakeLock.release();
            }
        } catch (Exception ignored) {}
    }

    public static synchronized boolean isAlarmRinging() {
        return isRinging;
    }

    private static Vibrator getVibrator(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vm = (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            if (vm != null) return vm.getDefaultVibrator();
        }
        return (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }
}
