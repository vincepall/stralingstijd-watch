package com.rt.stralingstijdwatch;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Locale;

/**
 * Volledig scherm keypad (numpad) voor het direct aanpassen van de belichtingstijd (MM:SS)
 * vanaf het Widget (Tile) scherm of het hoofdscherm.
 */
public class TimeInputActivity extends Activity {

    private static final int COLOR_BG = 0xFF000000;
    private static final int COLOR_ACCENT = 0xFFF5DF4D;
    private static final int COLOR_TEXT_WHITE = 0xFFFFFFFF;
    private static final int COLOR_TEXT_MUTED = 0xFFAAAAAA;
    private static final int COLOR_CANCEL_RED = 0xFFFF5252;

    private long currentSeconds = 120;
    private StringBuilder inputBuffer = new StringBuilder();
    private TextView numpadDisplay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences sp = getSharedPreferences("stralingstijd_prefs", Context.MODE_PRIVATE);
        currentSeconds = sp.getLong("last_timer_seconds", 120);
        if (currentSeconds <= 0) currentSeconds = 120;

        setContentView(buildNumpadView());
        updateNumpadDisplay();
    }

    private LinearLayout buildNumpadView() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(COLOR_BG);
        root.setPadding(dp(8), dp(10), dp(8), dp(6));
        root.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        long curM = currentSeconds / 60;
        long curS = currentSeconds % 60;
        TextView numpadTitle = new TextView(this);
        numpadTitle.setText(String.format(Locale.US, "TIJD (Huidig: %02d:%02d)", curM, curS));
        numpadTitle.setTextSize(11);
        numpadTitle.setTextColor(COLOR_ACCENT);
        numpadTitle.setTypeface(Typeface.DEFAULT_BOLD);
        numpadTitle.setGravity(Gravity.CENTER);
        root.addView(numpadTitle);

        LinearLayout displayRow = new LinearLayout(this);
        displayRow.setOrientation(LinearLayout.HORIZONTAL);
        displayRow.setGravity(Gravity.CENTER);
        displayRow.setLayoutParams(new LinearLayout.LayoutParams(dp(156), ViewGroup.LayoutParams.WRAP_CONTENT));

        numpadDisplay = new TextView(this);
        numpadDisplay.setText("00:00");
        numpadDisplay.setTextSize(22);
        numpadDisplay.setTextColor(COLOR_TEXT_WHITE);
        numpadDisplay.setTypeface(Typeface.DEFAULT_BOLD);
        numpadDisplay.setGravity(Gravity.CENTER);
        numpadDisplay.setIncludeFontPadding(false);
        numpadDisplay.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        displayRow.addView(numpadDisplay);

        TextView btnBk = new TextView(this);
        btnBk.setText("⌫");
        btnBk.setTextSize(16);
        btnBk.setTextColor(COLOR_TEXT_WHITE);
        btnBk.setTypeface(Typeface.DEFAULT_BOLD);
        btnBk.setGravity(Gravity.CENTER);
        btnBk.setIncludeFontPadding(false);
        btnBk.setBackgroundResource(R.drawable.btn_key_normal);
        btnBk.setClickable(true);
        btnBk.setFocusable(true);
        btnBk.setLayoutParams(new LinearLayout.LayoutParams(dp(38), dp(26)));
        btnBk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleNumpadKey("⌫");
            }
        });
        displayRow.addView(btnBk);
        root.addView(displayRow);

        addSpace(root, dp(3));

        LinearLayout keyGrid = new LinearLayout(this);
        keyGrid.setOrientation(LinearLayout.VERTICAL);
        keyGrid.setGravity(Gravity.CENTER);
        keyGrid.setLayoutParams(new LinearLayout.LayoutParams(dp(156), ViewGroup.LayoutParams.WRAP_CONTENT));

        String[][] digitRows = {
                {"1", "2", "3"},
                {"4", "5", "6"},
                {"7", "8", "9"}
        };

        for (final String[] rowKeys : digitRows) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(35)));

            for (final String key : rowKeys) {
                TextView b = createKey(key, 20, COLOR_TEXT_WHITE, R.drawable.btn_key_normal, 1.0f, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        handleNumpadKey(key);
                    }
                });
                row.addView(b);
            }
            keyGrid.addView(row);
        }

        LinearLayout row4 = new LinearLayout(this);
        row4.setOrientation(LinearLayout.HORIZONTAL);
        row4.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(35)));

        TextView btnCancel = createKey("TERUG", 12, COLOR_CANCEL_RED, R.drawable.btn_key_cancel, 1.15f, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        TextView btn0 = createKey("0", 20, COLOR_TEXT_WHITE, R.drawable.btn_key_normal, 1.0f, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleNumpadKey("0");
            }
        });

        TextView btnOk = createKey("OK", 14, 0xFF000000, R.drawable.btn_key_confirm, 1.15f, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                commitNumpad();
            }
        });

        row4.addView(btnCancel);
        row4.addView(btn0);
        row4.addView(btnOk);
        keyGrid.addView(row4);

        root.addView(keyGrid);
        return root;
    }

    private void handleNumpadKey(String key) {
        if ("⌫".equals(key)) {
            if (inputBuffer.length() > 0) {
                inputBuffer.deleteCharAt(inputBuffer.length() - 1);
            }
        } else {
            if (inputBuffer.length() < 5) {
                if (inputBuffer.length() == 0 && "0".equals(key)) {
                    // negeer voorloopnul
                } else {
                    inputBuffer.append(key);
                }
            }
        }
        updateNumpadDisplay();
    }

    private void updateNumpadDisplay() {
        if (inputBuffer.length() == 0) {
            numpadDisplay.setText("00:00");
            numpadDisplay.setTextColor(COLOR_TEXT_MUTED);
            return;
        }
        numpadDisplay.setTextColor(COLOR_TEXT_WHITE);
        long val = Long.parseLong(inputBuffer.toString());
        long mins = val / 100;
        long secs = val % 100;
        numpadDisplay.setText(String.format(Locale.US, "%02d:%02d", mins, secs));
    }

    private void commitNumpad() {
        if (inputBuffer.length() > 0) {
            try {
                long val = Long.parseLong(inputBuffer.toString());
                long mins = val / 100;
                long secs = val % 100;
                long total = mins * 60 + secs;
                if (total > 0) {
                    currentSeconds = total;
                    SharedPreferences sp = getSharedPreferences("stralingstijd_prefs", Context.MODE_PRIVATE);
                    sp.edit()
                            .putLong("last_timer_seconds", currentSeconds)
                            .putBoolean("manual_time_override", true)
                            .commit();
                    AlarmHelper.requestExternalUpdates(this);
                }
            } catch (Exception ignored) {}
        }
        finish();
    }

    private TextView createKey(String text, int textSizeSp, int textColor, int bgRes, float weight, View.OnClickListener l) {
        TextView b = new TextView(this);
        b.setText(text);
        b.setTextSize(textSizeSp);
        b.setTextColor(textColor);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setGravity(Gravity.CENTER);
        b.setPadding(0, 0, 0, 0);
        b.setIncludeFontPadding(false);
        b.setBackgroundResource(bgRes);
        b.setClickable(true);
        b.setFocusable(true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, weight);
        lp.setMargins(dp(2), dp(1), dp(2), dp(1));
        b.setLayoutParams(lp);
        b.setOnClickListener(l);
        return b;
    }

    private void addSpace(LinearLayout parent, int heightPx) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, heightPx));
        parent.addView(v);
    }

    private int dp(float val) {
        return Math.round(val * getResources().getDisplayMetrics().density);
    }
}
