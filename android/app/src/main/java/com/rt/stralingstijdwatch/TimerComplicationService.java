package com.rt.stralingstijdwatch;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.ComplicationManager;
import android.support.wearable.complications.ComplicationProviderService;
import android.support.wearable.complications.ComplicationText;
import java.util.Locale;

public class TimerComplicationService extends ComplicationProviderService {

    @Override
    public void onComplicationUpdate(int complicationId, int dataType, ComplicationManager complicationManager) {
        SharedPreferences sp = getSharedPreferences("stralingstijd_prefs", Context.MODE_PRIVATE);
        long lastSeconds = sp.getLong("last_timer_seconds", 120);
        if (lastSeconds <= 0) lastSeconds = 120;

        long m = lastSeconds / 60;
        long s = lastSeconds % 60;
        String shortTimeStr;
        if (m >= 60) {
            long h = m / 60;
            m = m % 60;
            shortTimeStr = String.format(Locale.US, "%d:%02d:%02d", h, m, s);
        } else {
            shortTimeStr = String.format(Locale.US, "%02d:%02d", m, s);
        }

        // Tap action: directly start the timer in MainActivity
        Intent startIntent = new Intent(this, MainActivity.class);
        startIntent.putExtra("action", "start_timer");
        startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent tapPendingIntent = PendingIntent.getActivity(this, 2050 + complicationId, startIntent, flags);

        Icon icon = Icon.createWithResource(this, R.drawable.ic_complication_timer);
        Icon appIcon = Icon.createWithResource(this, R.mipmap.ic_launcher);

        ComplicationData data = null;

        switch (dataType) {
            case ComplicationData.TYPE_SHORT_TEXT:
                data = new ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                        .setShortText(ComplicationText.plainText(shortTimeStr))
                        .setIcon(icon)
                        .setTapAction(tapPendingIntent)
                        .setContentDescription(ComplicationText.plainText("Start Stralingstijd Timer " + shortTimeStr))
                        .build();
                break;

            case ComplicationData.TYPE_LONG_TEXT:
                data = new ComplicationData.Builder(ComplicationData.TYPE_LONG_TEXT)
                        .setLongText(ComplicationText.plainText(shortTimeStr))
                        .setIcon(icon)
                        .setTapAction(tapPendingIntent)
                        .setContentDescription(ComplicationText.plainText("Start Stralingstijd Timer " + shortTimeStr))
                        .build();
                break;

            case ComplicationData.TYPE_RANGED_VALUE:
                data = new ComplicationData.Builder(ComplicationData.TYPE_RANGED_VALUE)
                        .setValue(lastSeconds)
                        .setMinValue(0f)
                        .setMaxValue(Math.max(lastSeconds, 600f))
                        .setShortText(ComplicationText.plainText(shortTimeStr))
                        .setIcon(icon)
                        .setTapAction(tapPendingIntent)
                        .setContentDescription(ComplicationText.plainText("Start Stralingstijd Timer " + shortTimeStr))
                        .build();
                break;

            case ComplicationData.TYPE_ICON:
                data = new ComplicationData.Builder(ComplicationData.TYPE_ICON)
                        .setIcon(icon)
                        .setTapAction(tapPendingIntent)
                        .setContentDescription(ComplicationText.plainText("Start Stralingstijd Timer"))
                        .build();
                break;

            case ComplicationData.TYPE_SMALL_IMAGE:
                data = new ComplicationData.Builder(ComplicationData.TYPE_SMALL_IMAGE)
                        .setSmallImage(appIcon)
                        .setImageStyle(ComplicationData.IMAGE_STYLE_ICON)
                        .setTapAction(tapPendingIntent)
                        .setContentDescription(ComplicationText.plainText("Start Stralingstijd Timer"))
                        .build();
                break;
        }

        if (data != null) {
            complicationManager.updateComplicationData(complicationId, data);
        } else {
            complicationManager.noUpdateRequired(complicationId);
        }
    }
}
