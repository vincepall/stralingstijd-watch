package com.rt.stralingstijdwatch;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.support.wearable.complications.ProviderUpdateRequester;
import androidx.wear.tiles.TileService;

public class AlarmHelper {
    private static Ringtone ringtone;
    private static PowerManager.WakeLock alarmWakeLock;
    private static volatile boolean isRinging = false;
    private static final Handler vibHandler = new Handler(Looper.getMainLooper());
    private static Runnable vibLoopRunnable;

    public static synchronized void startAlarm(Context context) {
        if (isRinging) return;
        isRinging = true;
        final Context appCtx = context.getApplicationContext();

        // 1. Wake lock to keep watch CPU awake while alarm is ringing
        try {
            if (alarmWakeLock == null) {
                PowerManager pm = (PowerManager) appCtx.getSystemService(Context.POWER_SERVICE);
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

        // 2. Repeating pulse vibration via Handler (stops immediately on cancel without OS waveform lock-in!)
        startVibrationLoop(appCtx);

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
                ringtone = RingtoneManager.getRingtone(appCtx, alertUri);
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

    private static void startVibrationLoop(final Context appCtx) {
        if (vibLoopRunnable != null) {
            vibHandler.removeCallbacks(vibLoopRunnable);
        }
        vibLoopRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isRinging) return;
                try {
                    Vibrator vibrator = getVibrator(appCtx);
                    if (vibrator != null && vibrator.hasVibrator()) {
                        long[] pattern = {0, 400, 150, 400};
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            AudioAttributes audioAttr = new AudioAttributes.Builder()
                                    .setUsage(AudioAttributes.USAGE_ALARM)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                    .build();
                            // Repeat index -1 = single cycle, re-triggered by Handler while isRinging is true
                            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1), audioAttr);
                        } else {
                            vibrator.vibrate(pattern, -1);
                        }
                    }
                } catch (Exception ignored) {}

                if (isRinging) {
                    vibHandler.postDelayed(this, 1350);
                }
            }
        };
        vibHandler.post(vibLoopRunnable);
    }

    public static synchronized void stopAlarm(Context context) {
        isRinging = false;
        final Context appCtx = context.getApplicationContext();

        // 0. Remove scheduled vibration pulses immediately
        try {
            if (vibLoopRunnable != null) {
                vibHandler.removeCallbacks(vibLoopRunnable);
            }
            vibHandler.removeCallbacksAndMessages(null);
        } catch (Exception ignored) {}

        // 1. Stop audio sound
        try {
            if (ringtone != null) {
                if (ringtone.isPlaying()) {
                    ringtone.stop();
                }
                ringtone = null;
            }
        } catch (Exception ignored) {}

        // 2. Stop vibration across both Application and Activity contexts + send tiny 1ms cancel override
        cancelVibratorInstance(getVibrator(context));
        cancelVibratorInstance(getVibrator(appCtx));
        try {
            Vibrator legacyVib = (Vibrator) appCtx.getSystemService(Context.VIBRATOR_SERVICE);
            cancelVibratorInstance(legacyVib);
        } catch (Exception ignored) {}

        // 3. Release alarm wake lock
        try {
            if (alarmWakeLock != null && alarmWakeLock.isHeld()) {
                alarmWakeLock.release();
            }
        } catch (Exception ignored) {}

        // 4. Request complication and tile update
        requestExternalUpdates(appCtx);
    }

    private static void cancelVibratorInstance(Vibrator vibrator) {
        if (vibrator == null) return;
        try {
            vibrator.cancel();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Override any lingering hardware waveform buffer with a 1ms silent one-shot, then cancel
                vibrator.vibrate(VibrationEffect.createOneShot(1, 1));
            }
            vibrator.cancel();
        } catch (Exception ignored) {}
    }

    public static void requestExternalUpdates(Context context) {
        if (context == null) return;
        final Context appCtx = context.getApplicationContext();

        // 1. Update Wear OS Tile (Widget)
        try {
            TileService.getUpdater(appCtx).requestUpdate(StralingstijdTileService.class);
        } catch (Exception ignored) {}

        // 2. Update Wear OS Watchface Complication (Android 12+ / targetSdk 34 compatible with FLAG_IMMUTABLE)
        try {
            ComponentName providerComponent = new ComponentName(appCtx, TimerComplicationService.class);
            Intent intent = new Intent(ProviderUpdateRequester.ACTION_REQUEST_UPDATE_ALL);
            intent.setPackage("com.google.android.wearable.app");
            intent.putExtra(ProviderUpdateRequester.EXTRA_PROVIDER_COMPONENT, providerComponent);

            int piFlags = 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                piFlags |= PendingIntent.FLAG_IMMUTABLE;
            }
            PendingIntent verifyPi = PendingIntent.getActivity(appCtx, 0, new Intent(""), piFlags);
            intent.putExtra(ProviderUpdateRequester.EXTRA_PENDING_INTENT, verifyPi);
            appCtx.sendBroadcast(intent);
        } catch (Exception ignored) {}
    }

    public static synchronized boolean isAlarmRinging() {
        return isRinging;
    }

    private static Vibrator getVibrator(Context context) {
        if (context == null) return null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vm = (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            if (vm != null) return vm.getDefaultVibrator();
        }
        return (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }
}
