package com.rt.stralingstijdwatch;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.wear.tiles.ActionBuilders;
import androidx.wear.tiles.ColorBuilders;
import androidx.wear.tiles.DimensionBuilders;
import androidx.wear.tiles.LayoutElementBuilders;
import androidx.wear.tiles.ModifiersBuilders;
import androidx.wear.tiles.RequestBuilders;
import androidx.wear.tiles.ResourceBuilders;
import androidx.wear.tiles.TileBuilders;
import androidx.wear.tiles.TileService;
import androidx.wear.tiles.TimelineBuilders;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Locale;

public class StralingstijdTileService extends TileService {

    private static final String RESOURCES_VERSION = "2";

    @Override
    protected ListenableFuture<TileBuilders.Tile> onTileRequest(RequestBuilders.TileRequest requestParams) {
        SharedPreferences sp = getSharedPreferences("stralingstijd_prefs", Context.MODE_PRIVATE);
        long lastSeconds = sp.getLong("last_timer_seconds", 120);
        String lastFilm = sp.getString("last_timer_film", "D4");
        String lastSource = sp.getString("last_timer_source", "Ir-192");
        float lastThickness = sp.getFloat("last_timer_thickness", 15.0f);

        if (lastSeconds <= 0) {
            lastSeconds = 120;
        }

        long m = lastSeconds / 60;
        long s = lastSeconds % 60;
        String formattedTime;
        if (m >= 60) {
            long h = m / 60;
            m = m % 60;
            formattedTime = String.format(Locale.US, "%d:%02d:%02d", h, m, s);
        } else {
            formattedTime = String.format(Locale.US, "%02d:%02d", m, s);
        }

        String subLabel = String.format(Locale.US, "%s • %s • %.1fmm", lastFilm, lastSource, lastThickness);

        // Action to open Stralingstijd app normally (Calculator)
        ActionBuilders.LaunchAction openAppAction = new ActionBuilders.LaunchAction.Builder()
                .setAndroidActivity(new ActionBuilders.AndroidActivity.Builder()
                        .setPackageName("com.rt.stralingstijdwatch")
                        .setClassName("com.rt.stralingstijdwatch.MainActivity")
                        .build())
                .build();

        ModifiersBuilders.Clickable openAppClickable = new ModifiersBuilders.Clickable.Builder()
                .setId("open_stralingstijd_app")
                .setOnClick(openAppAction)
                .build();

        // Action to start the last used timer immediately
        ActionBuilders.LaunchAction startTimerAction = new ActionBuilders.LaunchAction.Builder()
                .setAndroidActivity(new ActionBuilders.AndroidActivity.Builder()
                        .setPackageName("com.rt.stralingstijdwatch")
                        .setClassName("com.rt.stralingstijdwatch.MainActivity")
                        .addKeyToExtraMapping("action", new ActionBuilders.AndroidStringExtra.Builder().setValue("start_timer").build())
                        .build())
                .build();

        ModifiersBuilders.Clickable startTimerClickable = new ModifiersBuilders.Clickable.Builder()
                .setId("start_last_timer")
                .setOnClick(startTimerAction)
                .build();

        // 1. Header Title
        LayoutElementBuilders.Text titleText = new LayoutElementBuilders.Text.Builder()
                .setText("STRALINGSTIJD")
                .setFontStyle(new LayoutElementBuilders.FontStyle.Builder()
                        .setSize(DimensionBuilders.sp(10))
                        .setColor(ColorBuilders.argb(0xFFF5DF4D)) // Accent Yellow
                        .setWeight(700)
                        .build())
                .build();

        // 2. Large Time Display (Last used time)
        LayoutElementBuilders.Text timeDisplay = new LayoutElementBuilders.Text.Builder()
                .setText(formattedTime)
                .setFontStyle(new LayoutElementBuilders.FontStyle.Builder()
                        .setSize(DimensionBuilders.sp(32))
                        .setColor(ColorBuilders.argb(0xFFFFFFFF))
                        .setWeight(700)
                        .build())
                .build();

        // 3. Subtitle (Film & Source details)
        LayoutElementBuilders.Text subText = new LayoutElementBuilders.Text.Builder()
                .setText(subLabel)
                .setFontStyle(new LayoutElementBuilders.FontStyle.Builder()
                        .setSize(DimensionBuilders.sp(10))
                        .setColor(ColorBuilders.argb(0xFFAAAAAA)) // Muted Grey
                        .build())
                .build();

        // 4. Action Button (Pill shaped with Start Timer)
        LayoutElementBuilders.Text btnText = new LayoutElementBuilders.Text.Builder()
                .setText("▶ START TIMER")
                .setFontStyle(new LayoutElementBuilders.FontStyle.Builder()
                        .setSize(DimensionBuilders.sp(12))
                        .setColor(ColorBuilders.argb(0xFF000000))
                        .setWeight(700)
                        .build())
                .build();

        LayoutElementBuilders.Box actionButton = new LayoutElementBuilders.Box.Builder()
                .setWidth(DimensionBuilders.dp(148))
                .setHeight(DimensionBuilders.dp(38))
                .setModifiers(new ModifiersBuilders.Modifiers.Builder()
                        .setBackground(new ModifiersBuilders.Background.Builder()
                                .setColor(ColorBuilders.argb(0xFFF5DF4D))
                                .setCorner(new ModifiersBuilders.Corner.Builder()
                                        .setRadius(DimensionBuilders.dp(19))
                                        .build())
                                .build())
                        .setClickable(startTimerClickable)
                        .build())
                .addContent(btnText)
                .build();

        // 5. Bottom Hint / Link to calculator
        LayoutElementBuilders.Text hintText = new LayoutElementBuilders.Text.Builder()
                .setText("⚙ REKENMACHINE")
                .setFontStyle(new LayoutElementBuilders.FontStyle.Builder()
                        .setSize(DimensionBuilders.sp(10))
                        .setColor(ColorBuilders.argb(0xFF777777))
                        .setWeight(700)
                        .build())
                .build();

        // Layout Column
        LayoutElementBuilders.Column column = new LayoutElementBuilders.Column.Builder()
                .setWidth(DimensionBuilders.expand())
                .setHeight(DimensionBuilders.expand())
                .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                .addContent(new LayoutElementBuilders.Spacer.Builder().setHeight(DimensionBuilders.dp(10)).build())
                .addContent(titleText)
                .addContent(new LayoutElementBuilders.Spacer.Builder().setHeight(DimensionBuilders.dp(2)).build())
                .addContent(timeDisplay)
                .addContent(new LayoutElementBuilders.Spacer.Builder().setHeight(DimensionBuilders.dp(2)).build())
                .addContent(subText)
                .addContent(new LayoutElementBuilders.Spacer.Builder().setHeight(DimensionBuilders.dp(10)).build())
                .addContent(actionButton)
                .addContent(new LayoutElementBuilders.Spacer.Builder().setHeight(DimensionBuilders.dp(8)).build())
                .addContent(hintText)
                .build();

        // Root Box - Clickable everywhere to open app if button not directly tapped
        LayoutElementBuilders.Box rootBox = new LayoutElementBuilders.Box.Builder()
                .setWidth(DimensionBuilders.expand())
                .setHeight(DimensionBuilders.expand())
                .setModifiers(new ModifiersBuilders.Modifiers.Builder()
                        .setClickable(openAppClickable)
                        .setBackground(new ModifiersBuilders.Background.Builder()
                                .setColor(ColorBuilders.argb(0xFF000000))
                                .build())
                        .build())
                .addContent(column)
                .build();

        TimelineBuilders.TimelineEntry timelineEntry = new TimelineBuilders.TimelineEntry.Builder()
                .setLayout(new LayoutElementBuilders.Layout.Builder()
                        .setRoot(rootBox)
                        .build())
                .build();

        TimelineBuilders.Timeline timeline = new TimelineBuilders.Timeline.Builder()
                .addTimelineEntry(timelineEntry)
                .build();

        TileBuilders.Tile tile = new TileBuilders.Tile.Builder()
                .setResourcesVersion(RESOURCES_VERSION)
                .setTimeline(timeline)
                .build();

        return new ImmediateFuture<>(tile);
    }

    @Override
    protected ListenableFuture<ResourceBuilders.Resources> onResourcesRequest(RequestBuilders.ResourcesRequest requestParams) {
        return new ImmediateFuture<>(new ResourceBuilders.Resources.Builder()
                .setVersion(RESOURCES_VERSION)
                .build());
    }
}
