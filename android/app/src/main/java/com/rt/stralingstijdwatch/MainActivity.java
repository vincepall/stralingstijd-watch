package com.rt.stralingstijdwatch;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import androidx.wear.tiles.TileService;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    // Theme Colors
    private static final int COLOR_BG = 0xFF000000;
    private static final int COLOR_ACCENT = 0xFFF5DF4D;
    private static final int COLOR_TEXT_WHITE = 0xFFFFFFFF;
    private static final int COLOR_TEXT_MUTED = 0xFFAAAAAA;
    private static final int COLOR_TEXT_DIM = 0xFF666666;
    private static final int COLOR_CANCEL_RED = 0xFFFF5252;
    private static final int COLOR_SUCCESS_GREEN = 0xFF00E676;
    private static final int COLOR_CARD_BG = 0xFF181818;

    // Calculation Constants (From stralingstijd.html)
    // Ir192: k = 0.13, hvl = 13.68, ff = 17.10
    // Se75:  k = 0.054, hvl = 10.33, ff = 13.26
    private static final double IR192_K = 0.13;
    private static final double IR192_HVL = 13.68;
    private static final double IR192_FF = 17.10;

    private static final double SE75_K = 0.054;
    private static final double SE75_HVL = 10.33;
    private static final double SE75_FF = 13.26;

    // State
    private List<SourceDb.SourceItem> sourceList;
    private SourceDb.SourceItem currentSource;
    private double currentCi = 56.5;
    private double thickness = 20.0; // mm
    private int ffd = 60;            // cm
    private String selectedFilm = "D4"; // "D4", "D5", "D7"
    private double materialFactor = 1.0;
    private MaterialDb.Material selectedMaterial = null;

    // Root Containers
    private FrameLayout rootFrame;
    private ScrollView mainScrollView;
    private LinearLayout mainContentLayout;

    // Top Running Timer Mini-Banner
    private LinearLayout bannerActiveTimer;
    private TextView txtBannerTimerTime;

    // Source Card Views
    private TextView txtSourceName;
    private TextView txtSourceCi;
    private TextView txtSourceInfo;
    private LinearLayout rowCi;

    // Thickness & FFD Views
    private TextView txtThicknessVal;
    private TextView txtFfdVal;

    // Film Selector Buttons (Only D4, D5, D7)
    private TextView[] btnFilms = new TextView[3];
    private static final String[] FILM_TYPES = {"D4", "D5", "D7"};

    // Result Card Views
    private TextView txtResultTime;
    private TextView txtResultSub;
    private TextView btnStartTimerMain;

    // Source Picker Overlay Views
    private LinearLayout sourcePickerOverlay;
    private ScrollView sourcePickerScrollView;
    private LinearLayout sourcePickerListLayout;
    private TextView txtPickerSyncStatus;

    // Material Card Views
    private TextView txtMatFactorBox;
    private TextView txtMatSubLabel;
    private LinearLayout rowMat;

    // Material Picker Overlay Views
    private LinearLayout materialPickerOverlay;
    private ScrollView materialPickerScrollView;
    private LinearLayout materialPickerListLayout;
    private TextView txtMatPickerSourceBadge;

    // =========================================================================
    // TIMER OVERLAY STATE & VIEWS
    // =========================================================================
    private FrameLayout timerOverlay;
    private LinearLayout timerOverlayContent;
    private LinearLayout timerControlsTop;
    private LinearLayout timerControlsBottom;
    private TextView timerAodBadge;
    private TextView timerAodHint;
    private TextView timerHeader;
    private TextView timerSubInfo;
    private TextView timerDisplay;
    private TextView timerProgressText;
    private TextView btnTimerPause;
    private TextView btnTimerStop;
    private TextView btnTimerMinimize;

    // AOD & Battery Saver Timer State
    private boolean isTimerAodMode = true;
    private boolean isScreenSleeping = false;
    private boolean isUserInteracting = false;
    private long timerStartTime = 0;
    private PowerManager.WakeLock cpuWakeLock;
    private View sleepCoverView;
    private Handler aodHideHandler = new Handler(Looper.getMainLooper());
    private Runnable aodHideRunnable;

    // Alarm UI Views
    private LinearLayout layoutAlarmBanner;
    private TextView btnStopAlarm;

    private boolean isTimerRunning = false;
    private boolean isTimerPaused = false;
    private boolean isAlarmRinging = false;
    private long timerTotalSeconds = 0;
    private long timerTargetEndTime = 0;
    private long timerRemainingSeconds = 0;
    private String timerFilmName = "D4";

    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;

    // =========================================================================
    // NUMPAD OVERLAY STATE & VIEWS
    // =========================================================================
    private LinearLayout numpadOverlay;
    private TextView numpadTitle;
    private TextView numpadDisplay;
    private LinearLayout numpadRow4Integer;
    private LinearLayout numpadRow4Decimal;
    private String numpadBuffer = "";
    private int numpadTarget = 0;
    private boolean numpadIsDecimal = false;

    private static final int TARGET_THICKNESS = 1;
    private static final int TARGET_FFD = 2;
    private static final int TARGET_CI = 3;
    private static final int TARGET_FACTOR = 4;

    private Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }

        // Init Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vm = (VibratorManager) getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            if (vm != null) vibrator = vm.getDefaultVibrator();
        } else {
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        }

        // Load Sources from CSV / default list
        sourceList = SourceDb.getSources(this);
        // Default to first CSV source or first source available
        if (sourceList.size() > 2) {
            currentSource = sourceList.get(2); // First saved source
        } else {
            currentSource = sourceList.get(0); // Manual Ir-192
        }

        // Load user preferences
        SharedPreferences sp = getSharedPreferences("stralingstijd_prefs", Context.MODE_PRIVATE);
        if (sp.contains("last_timer_source_name")) {
            String savedName = sp.getString("last_timer_source_name", "");
            for (SourceDb.SourceItem item : sourceList) {
                if (item.name.equalsIgnoreCase(savedName)) {
                    currentSource = item;
                    break;
                }
            }
        }
        currentCi = currentSource.calculateCurrentActivity();

        if (sp.contains("last_timer_thickness")) {
            thickness = sp.getFloat("last_timer_thickness", (float) thickness);
            ffd = sp.getInt("last_timer_ffd", ffd);
            selectedFilm = sp.getString("last_timer_film", selectedFilm);
        }
        if (currentSource.isManual && sp.contains("last_timer_ci")) {
            currentCi = sp.getFloat("last_timer_ci", (float) currentCi);
        }
        if (sp.contains("last_timer_factor")) {
            materialFactor = sp.getFloat("last_timer_factor", (float) materialFactor);
        }

        rootFrame = new FrameLayout(this);
        rootFrame.setBackgroundColor(COLOR_BG);

        // 1. Main Scrollable List
        mainScrollView = new ScrollView(this);
        mainScrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        mainScrollView.setVerticalScrollBarEnabled(false);

        mainContentLayout = buildMainContent();
        mainScrollView.addView(mainContentLayout);
        rootFrame.addView(mainScrollView);

        // 2. Fullscreen Timer Overlay
        timerOverlay = buildTimerOverlay();
        timerOverlay.setVisibility(View.GONE);
        rootFrame.addView(timerOverlay);

        // 3. Fullscreen Numpad Overlay (156dp keygrid)
        numpadOverlay = buildFullscreenNumpad();
        numpadOverlay.setVisibility(View.GONE);
        rootFrame.addView(numpadOverlay);

        // 4. Fullscreen Source Picker Overlay
        sourcePickerOverlay = buildSourcePickerOverlay();
        sourcePickerOverlay.setVisibility(View.GONE);
        rootFrame.addView(sourcePickerOverlay);

        // 5. Fullscreen Material Picker Overlay
        materialPickerOverlay = buildMaterialPickerOverlay();
        materialPickerOverlay.setVisibility(View.GONE);
        rootFrame.addView(materialPickerOverlay);

        setContentView(rootFrame);

        setupTimerRunnable();
        setupAodRunnable();
        updateAllUI();
        syncSourcesWithGithub(false);

        if (!sp.contains("last_timer_seconds")) {
            double initialSec = calculateResultSecondsForFilm(selectedFilm);
            saveLastUsedTimer(initialSec > 0 ? Math.round(initialSec) : 120, selectedFilm);
        }
        handleIncomingIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncomingIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isTimerRunning && !isTimerPaused && !isAlarmRinging) {
            long now = SystemClock.elapsedRealtime();
            long remainingMs = timerTargetEndTime - now;
            long elapsedMs = now - timerStartTime;
            if (elapsedMs < 5000 || remainingMs <= 30000) {
                wakeScreen();
            } else {
                wakeScreen();
                isUserInteracting = true;
                scheduleUserInactivityTimeout();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveCurrentCalculationToPrefs();
    }

    private void haptic(long ms) {
        try {
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(ms);
                }
            }
        } catch (Exception ignored) {}
    }

    private void startAlarmVibration() {
        try {
            if (vibrator != null && vibrator.hasVibrator()) {
                long[] pattern = {0, 400, 150, 400, 150, 600, 300};
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0)); // repeat
                } else {
                    vibrator.vibrate(pattern, 0);
                }
            }
        } catch (Exception ignored) {}
    }

    private void stopVibration() {
        try {
            if (vibrator != null) vibrator.cancel();
        } catch (Exception ignored) {}
    }

    private int dp(float dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    // =========================================================================
    // 1. MAIN CONTENT BUILDER
    // =========================================================================
    private LinearLayout buildMainContent() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        // Top and bottom safe padding for circular bezel
        root.setPadding(dp(12), dp(26), dp(12), dp(60));
        root.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // 0. Active Timer Mini Banner (Shown when timer is running in background)
        bannerActiveTimer = buildActiveTimerBanner();
        bannerActiveTimer.setVisibility(View.GONE);
        root.addView(bannerActiveTimer);
        addSpace(root, dp(6));

        // 1. BRON & ACTIVITEIT CARD
        root.addView(buildSourceCard());
        addSpace(root, dp(8));

        // 2. DOORSTRAALDE DIKTE (mm)
        root.addView(buildThicknessCard());
        addSpace(root, dp(8));

        // 3. FFD AFSTAND (cm)
        root.addView(buildFfdCard());
        addSpace(root, dp(8));

        // 4. FILMTYPE (D4 / D5 / D7)
        root.addView(buildFilmCard());
        addSpace(root, dp(8));

        // 5. MATERIAALFACTOR CARD
        root.addView(buildMaterialCard());
        addSpace(root, dp(10));

        // 6. STRALLINGSTIJD (RESULT CARD & START TIMER)
        root.addView(buildResultCard());

        return root;
    }

    // =========================================================================
    // ACTIVE TIMER MINI BANNER (AT TOP OF CALCULATOR)
    // =========================================================================
    private LinearLayout buildActiveTimerBanner() {
        LinearLayout banner = new LinearLayout(this);
        banner.setOrientation(LinearLayout.HORIZONTAL);
        banner.setGravity(Gravity.CENTER);
        banner.setBackgroundResource(R.drawable.badge_timer_active);
        banner.setPadding(dp(10), dp(6), dp(10), dp(6));
        banner.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        txtBannerTimerTime = new TextView(this);
        txtBannerTimerTime.setText("⏱ TIMER BEZIG: 00:00");
        txtBannerTimerTime.setTextSize(12);
        txtBannerTimerTime.setTextColor(COLOR_ACCENT);
        txtBannerTimerTime.setTypeface(Typeface.DEFAULT_BOLD);
        txtBannerTimerTime.setGravity(Gravity.CENTER);
        banner.addView(txtBannerTimerTime);

        banner.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                haptic(20);
                openTimerOverlay();
            }
        });
        return banner;
    }

    // =========================================================================
    // BRON & ACTIVITEIT CARD
    // =========================================================================
    private View buildSourceCard() {
        LinearLayout card = createBaseCard("BRON & ACTIVITEIT");

        // Row 1: Source Selector Row
        LinearLayout rowSource = new LinearLayout(this);
        rowSource.setOrientation(LinearLayout.HORIZONTAL);
        rowSource.setGravity(Gravity.CENTER_VERTICAL);
        rowSource.setBackgroundResource(R.drawable.card_touch_val);
        rowSource.setPadding(dp(10), dp(8), dp(10), dp(8));
        rowSource.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(44)));

        TextView lblSrc = new TextView(this);
        lblSrc.setText("BRON:");
        lblSrc.setTextSize(12);
        lblSrc.setTextColor(COLOR_TEXT_MUTED);
        lblSrc.setTypeface(Typeface.DEFAULT_BOLD);
        rowSource.addView(lblSrc);

        txtSourceName = new TextView(this);
        txtSourceName.setText("Laden...");
        txtSourceName.setTextSize(13);
        txtSourceName.setSingleLine(true);
        txtSourceName.setTextColor(COLOR_ACCENT);
        txtSourceName.setTypeface(Typeface.DEFAULT_BOLD);
        txtSourceName.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        txtSourceName.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        rowSource.addView(txtSourceName);

        rowSource.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                haptic(20);
                showSourcePicker();
            }
        });
        card.addView(rowSource);

        addSpace(card, dp(6));

        // Row 2: Activity Ci Touch Row
        txtSourceCi = new TextView(this);
        rowCi = createFullWidthValueRow("ACTIVITEIT:", txtSourceCi, COLOR_TEXT_WHITE, new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (currentSource != null && currentSource.isManual) {
                    openNumpad("ACTIVITEIT (Ci)", currentCi, TARGET_CI, true);
                }
            }
        });
        card.addView(rowCi);

        addSpace(card, dp(4));

        txtSourceInfo = new TextView(this);
        txtSourceInfo.setTextSize(10);
        txtSourceInfo.setTextColor(COLOR_TEXT_DIM);
        txtSourceInfo.setGravity(Gravity.CENTER);
        card.addView(txtSourceInfo);

        return card;
    }

    private void showSourcePicker() {
        haptic(20);
        if (txtPickerSyncStatus != null) {
            txtPickerSyncStatus.setText(SourceDb.getLastSyncStatus(this));
            txtPickerSyncStatus.setTextColor(COLOR_TEXT_MUTED);
        }
        populateSourcePickerList();
        sourcePickerOverlay.setVisibility(View.VISIBLE);
        syncSourcesWithGithub(false);
    }

    private LinearLayout buildSourcePickerOverlay() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(COLOR_BG);
        root.setPadding(dp(10), dp(18), dp(10), dp(16));
        root.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        TextView title = new TextView(this);
        title.setText("KIES EEN BRON");
        title.setTextSize(13);
        title.setTextColor(COLOR_ACCENT);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        txtPickerSyncStatus = new TextView(this);
        txtPickerSyncStatus.setText(SourceDb.getLastSyncStatus(this));
        txtPickerSyncStatus.setTextSize(9);
        txtPickerSyncStatus.setTextColor(COLOR_TEXT_MUTED);
        txtPickerSyncStatus.setGravity(Gravity.CENTER);
        root.addView(txtPickerSyncStatus);

        addSpace(root, dp(4));

        sourcePickerScrollView = new ScrollView(this);
        sourcePickerScrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        sourcePickerScrollView.setVerticalScrollBarEnabled(false);
        sourcePickerScrollView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

        sourcePickerListLayout = new LinearLayout(this);
        sourcePickerListLayout.setOrientation(LinearLayout.VERTICAL);
        sourcePickerListLayout.setGravity(Gravity.CENTER_HORIZONTAL);

        populateSourcePickerList();

        sourcePickerScrollView.addView(sourcePickerListLayout);
        root.addView(sourcePickerScrollView);

        addSpace(root, dp(4));

        // Bottom Action Buttons: [ 🔄 SYNC ]  [ TERUG ]
        LinearLayout rowActions = new LinearLayout(this);
        rowActions.setOrientation(LinearLayout.HORIZONTAL);
        rowActions.setGravity(Gravity.CENTER);
        rowActions.setLayoutParams(new LinearLayout.LayoutParams(dp(170), dp(28)));

        TextView btnSync = new TextView(this);
        btnSync.setText("🔄 SYNC");
        btnSync.setTextSize(11);
        btnSync.setTextColor(COLOR_ACCENT);
        btnSync.setTypeface(Typeface.DEFAULT_BOLD);
        btnSync.setGravity(Gravity.CENTER);
        btnSync.setBackgroundResource(R.drawable.btn_timer_control);
        btnSync.setClickable(true);
        btnSync.setFocusable(true);
        LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.0f);
        sLp.setMargins(dp(2), 0, dp(2), 0);
        btnSync.setLayoutParams(sLp);
        btnSync.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                haptic(20);
                syncSourcesWithGithub(true);
            }
        });
        rowActions.addView(btnSync);

        TextView btnClose = new TextView(this);
        btnClose.setText("TERUG");
        btnClose.setTextSize(11);
        btnClose.setTextColor(COLOR_CANCEL_RED);
        btnClose.setTypeface(Typeface.DEFAULT_BOLD);
        btnClose.setGravity(Gravity.CENTER);
        btnClose.setBackgroundResource(R.drawable.btn_timer_stop);
        btnClose.setClickable(true);
        btnClose.setFocusable(true);
        LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.0f);
        cLp.setMargins(dp(2), 0, dp(2), 0);
        btnClose.setLayoutParams(cLp);
        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                haptic(15);
                sourcePickerOverlay.setVisibility(View.GONE);
            }
        });
        rowActions.addView(btnClose);

        root.addView(rowActions);

        return root;
    }

    private void populateSourcePickerList() {
        if (sourcePickerListLayout == null) return;
        sourcePickerListLayout.removeAllViews();

        for (int i = 0; i < sourceList.size(); i++) {
            final SourceDb.SourceItem item = sourceList.get(i);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setBackgroundResource(R.drawable.card_touch_val);
            row.setPadding(dp(12), dp(7), dp(12), dp(7));
            LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rLp.bottomMargin = dp(5);
            row.setLayoutParams(rLp);

            TextView tvName = new TextView(this);
            tvName.setText(item.isManual ? item.name : (item.name + " (" + item.type + ")"));
            tvName.setTextSize(13);
            tvName.setTextColor(COLOR_TEXT_WHITE);
            tvName.setTypeface(Typeface.DEFAULT_BOLD);
            row.addView(tvName);

            TextView tvSub = new TextView(this);
            if (item.isManual) {
                tvSub.setText("Handmatige invoer");
            } else {
                double cur = item.calculateCurrentActivity();
                tvSub.setText(String.format(Locale.US, "Huidig: %.1f Ci", cur));
            }
            tvSub.setTextSize(10);
            tvSub.setTextColor(COLOR_ACCENT);
            row.addView(tvSub);

            row.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    haptic(25);
                    currentSource = item;
                    currentCi = item.calculateCurrentActivity();
                    if (selectedMaterial != null) {
                        materialFactor = selectedMaterial.getFactor(currentSource.type);
                    }
                    sourcePickerOverlay.setVisibility(View.GONE);
                    updateAllUI();
                }
            });

            sourcePickerListLayout.addView(row);
        }
    }

    private void syncSourcesWithGithub(final boolean manualTrigger) {
        if (txtPickerSyncStatus != null) {
            txtPickerSyncStatus.setText("🔄 Synchroniseren met GitHub...");
            txtPickerSyncStatus.setTextColor(COLOR_ACCENT);
        }
        SourceDb.syncWithGithub(this, new SourceDb.SyncCallback() {
            @Override
            public void onSyncFinished(boolean success, List<SourceDb.SourceItem> updatedList, String statusMessage) {
                if (isDestroyed() || isFinishing()) return;
                if (manualTrigger) haptic(success ? 25 : 15);
                sourceList = updatedList;

                if (currentSource != null && !currentSource.isManual) {
                    for (SourceDb.SourceItem item : sourceList) {
                        if (item.name.equalsIgnoreCase(currentSource.name)) {
                            currentSource = item;
                            currentCi = item.calculateCurrentActivity();
                            break;
                        }
                    }
                }

                if (txtPickerSyncStatus != null) {
                    txtPickerSyncStatus.setText(statusMessage);
                    txtPickerSyncStatus.setTextColor(success ? COLOR_SUCCESS_GREEN : COLOR_TEXT_MUTED);
                }

                populateSourcePickerList();
                updateAllUI();
            }
        });
    }

    // =========================================================================
    // DOORSTRAALDE DIKTE CARD
    // =========================================================================
    private View buildThicknessCard() {
        LinearLayout card = createBaseCard("DOORSTRAALDE DIKTE (mm)");

        txtThicknessVal = new TextView(this);
        LinearLayout row = createFullWidthValueRow("DIKTE:", txtThicknessVal, COLOR_ACCENT, new View.OnClickListener() {
            @Override public void onClick(View v) {
                openNumpad("DIKTE (mm)", thickness, TARGET_THICKNESS, true);
            }
        });
        card.addView(row);

        return card;
    }

    // =========================================================================
    // FFD AFSTAND CARD
    // =========================================================================
    private View buildFfdCard() {
        LinearLayout card = createBaseCard("FFD AFSTAND (cm)");

        txtFfdVal = new TextView(this);
        LinearLayout row = createFullWidthValueRow("FFD:", txtFfdVal, COLOR_TEXT_WHITE, new View.OnClickListener() {
            @Override public void onClick(View v) {
                openNumpad("FFD (cm)", ffd, TARGET_FFD, false);
            }
        });
        card.addView(row);

        return card;
    }

    // =========================================================================
    // MATERIAALFACTOR CARD & PICKER
    // =========================================================================
    private View buildMaterialCard() {
        LinearLayout card = createBaseCard("MATERIAALFACTOR");

        txtMatFactorBox = new TextView(this);
        rowMat = createFullWidthValueRow("FACTOR:", txtMatFactorBox, COLOR_ACCENT, new View.OnClickListener() {
            @Override public void onClick(View v) {
                haptic(20);
                openNumpad("MATERIAALFACTOR", materialFactor, TARGET_FACTOR, true);
            }
        });
        card.addView(rowMat);

        addSpace(card, dp(4));

        txtMatSubLabel = new TextView(this);
        txtMatSubLabel.setText("Geen factor (1.00x)");
        txtMatSubLabel.setTextSize(11);
        txtMatSubLabel.setTextColor(COLOR_TEXT_MUTED);
        txtMatSubLabel.setGravity(Gravity.CENTER);
        card.addView(txtMatSubLabel);

        addSpace(card, dp(4));

        TextView btnPickDb = new TextView(this);
        btnPickDb.setText("Kies legering uit database...");
        btnPickDb.setTextSize(11);
        btnPickDb.setTextColor(COLOR_TEXT_WHITE);
        btnPickDb.setTypeface(Typeface.DEFAULT_BOLD);
        btnPickDb.setGravity(Gravity.CENTER);
        btnPickDb.setBackgroundResource(R.drawable.btn_key_normal);
        btnPickDb.setPadding(0, 0, 0, 0);
        btnPickDb.setIncludeFontPadding(false);
        btnPickDb.setClickable(true);
        btnPickDb.setFocusable(true);
        btnPickDb.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(34)));
        btnPickDb.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                haptic(20);
                showMaterialPicker();
            }
        });
        card.addView(btnPickDb);

        return card;
    }

    private void showMaterialPicker() {
        haptic(20);
        if (txtMatPickerSourceBadge != null) {
            txtMatPickerSourceBadge.setText("Factor voor: " + (currentSource != null ? currentSource.type : "Ir192"));
        }
        populateMaterialPickerList();
        materialPickerOverlay.setVisibility(View.VISIBLE);
        if (materialPickerScrollView != null) {
            materialPickerScrollView.scrollTo(0, 0);
        }
    }

    private LinearLayout buildMaterialPickerOverlay() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(COLOR_BG);
        root.setPadding(dp(10), dp(18), dp(10), dp(16));
        root.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        TextView title = new TextView(this);
        title.setText("KIES MATERIAAL");
        title.setTextSize(13);
        title.setTextColor(COLOR_ACCENT);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        txtMatPickerSourceBadge = new TextView(this);
        txtMatPickerSourceBadge.setText("Factor voor: " + (currentSource != null ? currentSource.type : "Ir192"));
        txtMatPickerSourceBadge.setTextSize(9);
        txtMatPickerSourceBadge.setTextColor(COLOR_TEXT_MUTED);
        txtMatPickerSourceBadge.setGravity(Gravity.CENTER);
        root.addView(txtMatPickerSourceBadge);

        addSpace(root, dp(4));

        materialPickerScrollView = new ScrollView(this);
        materialPickerScrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        materialPickerScrollView.setVerticalScrollBarEnabled(false);
        materialPickerScrollView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

        materialPickerListLayout = new LinearLayout(this);
        materialPickerListLayout.setOrientation(LinearLayout.VERTICAL);
        materialPickerListLayout.setGravity(Gravity.CENTER_HORIZONTAL);

        populateMaterialPickerList();

        materialPickerScrollView.addView(materialPickerListLayout);
        root.addView(materialPickerScrollView);

        addSpace(root, dp(4));

        // Bottom Action Button: [ SLUITEN ]
        TextView btnClose = new TextView(this);
        btnClose.setText("SLUITEN");
        btnClose.setTextSize(11);
        btnClose.setTextColor(COLOR_CANCEL_RED);
        btnClose.setTypeface(Typeface.DEFAULT_BOLD);
        btnClose.setGravity(Gravity.CENTER);
        btnClose.setBackgroundResource(R.drawable.btn_timer_stop);
        btnClose.setClickable(true);
        btnClose.setFocusable(true);
        btnClose.setLayoutParams(new LinearLayout.LayoutParams(dp(120), dp(28)));
        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                haptic(15);
                materialPickerOverlay.setVisibility(View.GONE);
            }
        });
        root.addView(btnClose);

        return root;
    }

    private void populateMaterialPickerList() {
        if (materialPickerListLayout == null) return;
        materialPickerListLayout.removeAllViews();

        final String srcType = (currentSource != null && currentSource.type != null) ? currentSource.type : "Ir192";

        // Item 0: Reset / Standaard (1.00x)
        LinearLayout resetRow = new LinearLayout(this);
        resetRow.setOrientation(LinearLayout.VERTICAL);
        resetRow.setBackgroundResource(selectedMaterial == null && Math.abs(materialFactor - 1.0) < 0.001 ? R.drawable.result_card_bg : R.drawable.card_touch_val);
        resetRow.setPadding(dp(12), dp(7), dp(12), dp(7));
        LinearLayout.LayoutParams resetLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        resetLp.bottomMargin = dp(5);
        resetRow.setLayoutParams(resetLp);

        TextView tvResetName = new TextView(this);
        tvResetName.setText("GEEN FACTOR (Standaard)");
        tvResetName.setTextSize(12);
        tvResetName.setTextColor(COLOR_ACCENT);
        tvResetName.setTypeface(Typeface.DEFAULT_BOLD);
        resetRow.addView(tvResetName);

        TextView tvResetSub = new TextView(this);
        tvResetSub.setText("Factor 1.00x • Koolstofstaal");
        tvResetSub.setTextSize(10);
        tvResetSub.setTextColor(COLOR_TEXT_WHITE);
        resetRow.addView(tvResetSub);

        resetRow.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                haptic(25);
                selectedMaterial = null;
                materialFactor = 1.0;
                materialPickerOverlay.setVisibility(View.GONE);
                updateAllUI();
            }
        });
        materialPickerListLayout.addView(resetRow);

        // List all 89 materials
        for (int i = 0; i < MaterialDb.ALL.length; i++) {
            final MaterialDb.Material mat = MaterialDb.ALL[i];
            final double factor = mat.getFactor(srcType);
            final boolean isCurrent = (selectedMaterial != null && selectedMaterial.name.equals(mat.name));

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setBackgroundResource(isCurrent ? R.drawable.result_card_bg : R.drawable.card_touch_val);
            row.setPadding(dp(12), dp(7), dp(12), dp(7));
            LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rLp.bottomMargin = dp(5);
            row.setLayoutParams(rLp);

            TextView tvName = new TextView(this);
            tvName.setText(mat.name);
            tvName.setTextSize(12);
            tvName.setTextColor(isCurrent ? COLOR_ACCENT : COLOR_TEXT_WHITE);
            tvName.setTypeface(Typeface.DEFAULT_BOLD);
            row.addView(tvName);

            TextView tvSub = new TextView(this);
            tvSub.setText(String.format(Locale.US, "%s • Factor: %.2fx", mat.group, factor));
            tvSub.setTextSize(10);
            tvSub.setTextColor(isCurrent ? COLOR_TEXT_WHITE : COLOR_ACCENT);
            row.addView(tvSub);

            row.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    haptic(25);
                    selectedMaterial = mat;
                    materialFactor = factor;
                    materialPickerOverlay.setVisibility(View.GONE);
                    updateAllUI();
                }
            });

            materialPickerListLayout.addView(row);
        }
    }

    // =========================================================================
    // FILM SELECTOR CARD
    // =========================================================================
    private View buildFilmCard() {
        LinearLayout card = createBaseCard("FILMTYPE");

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setBaselineAligned(false);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(34)));

        for (int i = 0; i < FILM_TYPES.length; i++) {
            final String filmName = FILM_TYPES[i];
            final int filmIdx = i;
            TextView b = createSegmentBtn(filmName, new View.OnClickListener() {
                @Override public void onClick(View v) {
                    haptic(15);
                    selectedFilm = filmName;
                    updateAllUI();
                }
            });
            btnFilms[filmIdx] = b;
            row.addView(b);
        }
        card.addView(row);

        return card;
    }

    private View buildResultCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setBackgroundResource(R.drawable.result_card_bg);
        card.setPadding(dp(12), dp(12), dp(12), dp(14));
        card.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView tag = new TextView(this);
        tag.setText("STRALINGSTIJD");
        tag.setTextSize(11);
        tag.setTextColor(COLOR_ACCENT);
        tag.setTypeface(Typeface.DEFAULT_BOLD);
        tag.setGravity(Gravity.CENTER);
        card.addView(tag);

        // Single Film Time Display
        txtResultTime = new TextView(this);
        txtResultTime.setText("0m 00s");
        txtResultTime.setTextSize(34);
        txtResultTime.setTextColor(COLOR_TEXT_WHITE);
        txtResultTime.setTypeface(Typeface.DEFAULT_BOLD);
        txtResultTime.setGravity(Gravity.CENTER);
        card.addView(txtResultTime);

        // Subtitle info
        txtResultSub = new TextView(this);
        txtResultSub.setTextSize(10);
        txtResultSub.setTextColor(COLOR_TEXT_MUTED);
        txtResultSub.setGravity(Gravity.CENTER);
        card.addView(txtResultSub);

        addSpace(card, dp(8));

        // Big Prominent START TIMER Action Button
        btnStartTimerMain = new TextView(this);
        btnStartTimerMain.setText("▶ START TIMER");
        btnStartTimerMain.setTextSize(14);
        btnStartTimerMain.setTextColor(0xFF000000);
        btnStartTimerMain.setTypeface(Typeface.DEFAULT_BOLD);
        btnStartTimerMain.setGravity(Gravity.CENTER);
        btnStartTimerMain.setBackgroundResource(R.drawable.btn_timer_start);
        btnStartTimerMain.setPadding(0, 0, 0, 0);
        btnStartTimerMain.setIncludeFontPadding(false);
        btnStartTimerMain.setClickable(true);
        btnStartTimerMain.setFocusable(true);
        btnStartTimerMain.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(40)));
        btnStartTimerMain.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                haptic(25);
                startCountdownForFilm(selectedFilm);
            }
        });
        card.addView(btnStartTimerMain);

        return card;
    }

    // =========================================================================
    // FULLSCREEN COUNTDOWN TIMER OVERLAY
    // =========================================================================
    private FrameLayout buildTimerOverlay() {
        FrameLayout overlay = new FrameLayout(this);
        overlay.setBackgroundColor(COLOR_BG);
        overlay.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        timerOverlayContent = new LinearLayout(this);
        timerOverlayContent.setOrientation(LinearLayout.VERTICAL);
        timerOverlayContent.setGravity(Gravity.CENTER);
        timerOverlayContent.setPadding(dp(10), dp(8), dp(10), dp(8));
        timerOverlayContent.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // 1. Alarm Banner (Flashing when time up)
        layoutAlarmBanner = new LinearLayout(this);
        layoutAlarmBanner.setOrientation(LinearLayout.VERTICAL);
        layoutAlarmBanner.setGravity(Gravity.CENTER);
        layoutAlarmBanner.setBackgroundColor(0xFFCC0000);
        layoutAlarmBanner.setPadding(dp(6), dp(4), dp(6), dp(4));
        layoutAlarmBanner.setVisibility(View.GONE);

        TextView txtAlarm = new TextView(this);
        txtAlarm.setText("⚠ TIJD VERSTREKEN! ⚠");
        txtAlarm.setTextSize(11);
        txtAlarm.setTextColor(0xFFFFFFFF);
        txtAlarm.setTypeface(Typeface.DEFAULT_BOLD);
        txtAlarm.setGravity(Gravity.CENTER);
        layoutAlarmBanner.addView(txtAlarm);

        btnStopAlarm = new TextView(this);
        btnStopAlarm.setText("STOP ALARM");
        btnStopAlarm.setTextSize(12);
        btnStopAlarm.setTextColor(0xFF000000);
        btnStopAlarm.setTypeface(Typeface.DEFAULT_BOLD);
        btnStopAlarm.setGravity(Gravity.CENTER);
        btnStopAlarm.setBackgroundResource(R.drawable.btn_timer_start);
        btnStopAlarm.setPadding(dp(12), dp(4), dp(12), dp(4));
        LinearLayout.LayoutParams saLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        saLp.topMargin = dp(3);
        btnStopAlarm.setLayoutParams(saLp);
        btnStopAlarm.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                haptic(30);
                stopAlarm();
            }
        });
        layoutAlarmBanner.addView(btnStopAlarm);
        timerOverlayContent.addView(layoutAlarmBanner);

        // 2. Controls Top (Header & SubInfo - hidden in AOD mode)
        timerControlsTop = new LinearLayout(this);
        timerControlsTop.setOrientation(LinearLayout.VERTICAL);
        timerControlsTop.setGravity(Gravity.CENTER_HORIZONTAL);
        timerControlsTop.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        timerHeader = new TextView(this);
        timerHeader.setText("TIMER: D4");
        timerHeader.setTextSize(11);
        timerHeader.setTextColor(COLOR_ACCENT);
        timerHeader.setTypeface(Typeface.DEFAULT_BOLD);
        timerHeader.setGravity(Gravity.CENTER);
        timerControlsTop.addView(timerHeader);

        timerSubInfo = new TextView(this);
        timerSubInfo.setText("20mm • FFD 60cm");
        timerSubInfo.setTextSize(9);
        timerSubInfo.setTextColor(COLOR_TEXT_MUTED);
        timerSubInfo.setGravity(Gravity.CENTER);
        timerControlsTop.addView(timerSubInfo);

        timerOverlayContent.addView(timerControlsTop);

        // 3. Center Section: AOD Subtitle + Big Digits + Tap Hint
        timerAodBadge = new TextView(this);
        timerAodBadge.setText("⏱ D4 • 20mm");
        timerAodBadge.setTextSize(10);
        timerAodBadge.setTextColor(0xFF888888);
        timerAodBadge.setGravity(Gravity.CENTER);
        timerOverlayContent.addView(timerAodBadge);

        timerDisplay = new TextView(this);
        timerDisplay.setText("00:00");
        timerDisplay.setTextSize(46);
        timerDisplay.setTextColor(0xFFDCDCDC);
        timerDisplay.setTypeface(Typeface.DEFAULT_BOLD);
        timerDisplay.setGravity(Gravity.CENTER);
        timerDisplay.setIncludeFontPadding(false);
        timerOverlayContent.addView(timerDisplay);

        timerAodHint = new TextView(this);
        timerAodHint.setText("Tik voor opties");
        timerAodHint.setTextSize(9);
        timerAodHint.setTextColor(0xFF555555);
        timerAodHint.setGravity(Gravity.CENTER);
        timerOverlayContent.addView(timerAodHint);

        // 4. Controls Bottom (Progress + Buttons - hidden in AOD mode)
        timerControlsBottom = new LinearLayout(this);
        timerControlsBottom.setOrientation(LinearLayout.VERTICAL);
        timerControlsBottom.setGravity(Gravity.CENTER_HORIZONTAL);
        timerControlsBottom.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        timerProgressText = new TextView(this);
        timerProgressText.setText("Totaal: 0m 00s");
        timerProgressText.setTextSize(10);
        timerProgressText.setTextColor(COLOR_TEXT_DIM);
        timerProgressText.setGravity(Gravity.CENTER);
        timerControlsBottom.addView(timerProgressText);

        addSpace(timerControlsBottom, dp(8));

        btnTimerPause = new TextView(this);
        btnTimerPause.setText("PAUZE");
        btnTimerPause.setTextSize(14);
        btnTimerPause.setTextColor(0xFF000000);
        btnTimerPause.setTypeface(Typeface.DEFAULT_BOLD);
        btnTimerPause.setGravity(Gravity.CENTER);
        btnTimerPause.setBackgroundResource(R.drawable.btn_timer_start);
        btnTimerPause.setPadding(0, 0, 0, 0);
        btnTimerPause.setIncludeFontPadding(false);
        btnTimerPause.setLayoutParams(new LinearLayout.LayoutParams(dp(156), dp(34)));
        btnTimerPause.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                haptic(25);
                toggleTimerPause();
            }
        });
        timerControlsBottom.addView(btnTimerPause);

        addSpace(timerControlsBottom, dp(6));

        LinearLayout rowBottom = new LinearLayout(this);
        rowBottom.setOrientation(LinearLayout.HORIZONTAL);
        rowBottom.setGravity(Gravity.CENTER);
        rowBottom.setLayoutParams(new LinearLayout.LayoutParams(dp(156), dp(30)));

        btnTimerStop = createTimerActionBtn("STOP", 11, COLOR_CANCEL_RED, R.drawable.btn_timer_stop, new View.OnClickListener() {
            @Override public void onClick(View v) {
                haptic(25);
                stopTimer();
            }
        });

        btnTimerMinimize = createTimerActionBtn("MIN", 11, COLOR_TEXT_MUTED, R.drawable.btn_timer_control, new View.OnClickListener() {
            @Override public void onClick(View v) {
                haptic(15);
                minimizeTimer();
            }
        });

        rowBottom.addView(btnTimerStop);
        rowBottom.addView(btnTimerMinimize);
        timerControlsBottom.addView(rowBottom);

        timerOverlayContent.addView(timerControlsBottom);

        // Tap screen to wake or toggle between AOD minimalist view and interactive buttons
        View.OnClickListener toggleClick = new View.OnClickListener() {
            @Override public void onClick(View v) {
                onTimerOverlayClicked();
            }
        };
        overlay.setOnClickListener(toggleClick);
        timerOverlayContent.setOnClickListener(toggleClick);
        timerDisplay.setOnClickListener(toggleClick);
        timerAodBadge.setOnClickListener(toggleClick);
        timerAodHint.setOnClickListener(toggleClick);

        overlay.addView(timerOverlayContent);

        sleepCoverView = new View(this);
        sleepCoverView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        sleepCoverView.setBackgroundColor(COLOR_BG);
        sleepCoverView.setClickable(true);
        sleepCoverView.setFocusable(true);
        sleepCoverView.setVisibility(View.GONE);
        sleepCoverView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    onTimerOverlayClicked();
                    return true;
                }
                return false;
            }
        });
        overlay.addView(sleepCoverView);

        return overlay;
    }

    private TextView createTimerActionBtn(String text, int textSizeSp, int textColor, int bgRes, View.OnClickListener onClick) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(textSizeSp);
        tv.setTextColor(textColor);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setGravity(Gravity.CENTER);
        tv.setBackgroundResource(bgRes);
        tv.setClickable(true);
        tv.setFocusable(true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.0f);
        lp.setMargins(dp(2), 0, dp(2), 0);
        tv.setLayoutParams(lp);
        tv.setOnClickListener(onClick);
        return tv;
    }

    // =========================================================================
    // AOD & BATTERY SAVER TIMER ENGINE
    // =========================================================================
    private void putScreenToSleep() {
        if (!isTimerRunning || isTimerPaused || isAlarmRinging) return;
        isScreenSleeping = true;
        try {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } catch (Exception ignored) {}

        if (sleepCoverView != null) {
            sleepCoverView.setVisibility(View.VISIBLE);
            sleepCoverView.bringToFront();
        }
        if (timerOverlayContent != null) {
            timerOverlayContent.setVisibility(View.INVISIBLE);
        }
    }

    private void wakeScreen() {
        isScreenSleeping = false;
        try {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                PowerManager.WakeLock wl = pm.newWakeLock(
                        PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE,
                        "stralingstijd:wake_timer"
                );
                wl.acquire(1000);
                wl.release();
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setTurnScreenOn(true);
                setShowWhenLocked(true);
            }
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } catch (Exception ignored) {}

        if (sleepCoverView != null) {
            sleepCoverView.setVisibility(View.GONE);
        }
        if (timerOverlayContent != null) {
            timerOverlayContent.setVisibility(View.VISIBLE);
        }
    }

    private void acquireCpuWakeLock() {
        try {
            if (cpuWakeLock == null) {
                PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                if (pm != null) {
                    cpuWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "stralingstijd:cpu_wake");
                    cpuWakeLock.setReferenceCounted(false);
                }
            }
            if (cpuWakeLock != null && !cpuWakeLock.isHeld()) {
                cpuWakeLock.acquire((timerTotalSeconds + 60) * 1000L);
            }
        } catch (Exception ignored) {}
    }

    private void releaseCpuWakeLock() {
        try {
            if (cpuWakeLock != null && cpuWakeLock.isHeld()) {
                cpuWakeLock.release();
            }
        } catch (Exception ignored) {}
    }

    private void scheduleUserInactivityTimeout() {
        if (aodHideHandler != null && aodHideRunnable != null) {
            aodHideHandler.removeCallbacks(aodHideRunnable);
            aodHideHandler.postDelayed(aodHideRunnable, 5000);
        }
    }

    private void onTimerOverlayClicked() {
        if (isAlarmRinging) return;
        haptic(15);
        if (isScreenSleeping) {
            wakeScreen();
            isUserInteracting = true;
            exitTimerAodMode();
            scheduleUserInactivityTimeout();
        } else if (isTimerAodMode) {
            isUserInteracting = true;
            exitTimerAodMode();
            scheduleUserInactivityTimeout();
        } else {
            isUserInteracting = false;
            long now = SystemClock.elapsedRealtime();
            long remainingMs = timerTargetEndTime - now;
            long elapsedMs = now - timerStartTime;
            if (elapsedMs >= 5000 && remainingMs > 30000) {
                putScreenToSleep();
            } else {
                enterTimerAodMode();
            }
        }
    }

    private void setupAodRunnable() {
        aodHideRunnable = new Runnable() {
            @Override public void run() {
                isUserInteracting = false;
                if (isTimerRunning && !isTimerPaused && !isAlarmRinging) {
                    long now = SystemClock.elapsedRealtime();
                    long remainingMs = timerTargetEndTime - now;
                    long elapsedMs = now - timerStartTime;
                    if (elapsedMs >= 5000 && remainingMs > 30000) {
                        putScreenToSleep();
                    } else {
                        enterTimerAodMode();
                    }
                }
            }
        };
    }

    private void setDimmedAodBrightness(boolean dim) {
        try {
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
            getWindow().setAttributes(lp);
        } catch (Exception ignored) {}
    }

    private void enterTimerAodMode() {
        if (!isTimerRunning || isTimerPaused || isAlarmRinging) return;
        isTimerAodMode = true;
        setDimmedAodBrightness(true);

        if (timerControlsTop != null) timerControlsTop.setVisibility(View.GONE);
        if (timerControlsBottom != null) timerControlsBottom.setVisibility(View.GONE);
        if (timerAodBadge != null) {
            timerAodBadge.setVisibility(View.VISIBLE);
            timerAodBadge.setTextColor(0xFF555555);
        }
        if (timerAodHint != null) {
            timerAodHint.setVisibility(View.VISIBLE);
            timerAodHint.setTextColor(0xFF333333);
        }

        if (timerDisplay != null) {
            timerDisplay.setTextSize(46);
            timerDisplay.setTextColor(0xFF888888);
        }
        if (aodHideHandler != null && aodHideRunnable != null) {
            aodHideHandler.removeCallbacks(aodHideRunnable);
        }
    }

    private void exitTimerAodMode() {
        isTimerAodMode = false;
        setDimmedAodBrightness(false);

        if (timerControlsTop != null) timerControlsTop.setVisibility(View.VISIBLE);
        if (timerControlsBottom != null) timerControlsBottom.setVisibility(View.VISIBLE);
        if (timerAodBadge != null) timerAodBadge.setVisibility(View.GONE);
        if (timerAodHint != null) timerAodHint.setVisibility(View.GONE);

        if (timerDisplay != null) {
            timerDisplay.setTextSize(36);
            if (isAlarmRinging) {
                timerDisplay.setTextColor(COLOR_CANCEL_RED);
            } else if (isTimerPaused) {
                timerDisplay.setTextColor(COLOR_ACCENT);
            } else {
                timerDisplay.setTextColor(COLOR_TEXT_WHITE);
            }
        }

        // Auto-hide back to AOD or Sleep after 5 seconds of inactivity
        scheduleUserInactivityTimeout();
    }

    private void toggleTimerAodMode() {
        if (isAlarmRinging) return;
        haptic(15);
        if (isTimerAodMode) {
            exitTimerAodMode();
        } else {
            enterTimerAodMode();
        }
    }

    // =========================================================================
    // TIMER ENGINE & RUNNABLE (DRIFT-FREE VIA SYSTEMCLOCK)
    // =========================================================================
    private void setupTimerRunnable() {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isTimerRunning || isTimerPaused) return;

                long now = SystemClock.elapsedRealtime();
                long remainingMs = timerTargetEndTime - now;

                if (remainingMs <= 0) {
                    timerRemainingSeconds = 0;
                    isTimerRunning = false;
                    onTimerFinished();
                } else {
                    timerRemainingSeconds = (remainingMs + 999) / 1000;
                    long elapsedMs = now - timerStartTime;

                    // Always-on display schedule:
                    // 1. First 5 seconds (elapsedMs < 5000): Screen ON
                    // 2. In between (elapsedMs >= 5000 && remainingMs > 30000): Screen completely OFF (battery saver)
                    // 3. Last 30 seconds (remainingMs <= 30000): Screen ON
                    if (elapsedMs < 5000) {
                        if (isScreenSleeping) {
                            wakeScreen();
                        }
                    } else if (remainingMs > 30000) {
                        if (!isScreenSleeping && !isUserInteracting) {
                            putScreenToSleep();
                        }
                    } else {
                        // Last 30 seconds!
                        if (isScreenSleeping) {
                            wakeScreen();
                        }
                    }

                    updateTimerUI();
                    updateBannerTimer();
                    timerHandler.postDelayed(this, 500); // Check twice a second for precision
                }
            }
        };
    }

    private void startCountdownForFilm(String film) {
        double sec = calculateResultSecondsForFilm(film);
        if (sec <= 0) return;
        startCountdown(Math.round(sec), film);
    }

    private void startCountdown(long seconds, String film) {
        if (seconds <= 0) return;
        timerFilmName = film;
        timerTotalSeconds = seconds;
        timerRemainingSeconds = timerTotalSeconds;
        timerStartTime = SystemClock.elapsedRealtime();
        timerTargetEndTime = timerStartTime + (timerTotalSeconds * 1000);
        isTimerRunning = true;
        isTimerPaused = false;
        isAlarmRinging = false;
        isScreenSleeping = false;
        isUserInteracting = false;

        saveLastUsedTimer(timerTotalSeconds, timerFilmName);
        acquireCpuWakeLock();
        wakeScreen();
        enterTimerAodMode();

        layoutAlarmBanner.setVisibility(View.GONE);
        btnTimerPause.setText("PAUZE");
        btnTimerPause.setBackgroundResource(R.drawable.btn_timer_start);
        btnTimerPause.setTextColor(0xFF000000);

        timerHandler.removeCallbacks(timerRunnable);
        timerHandler.post(timerRunnable);

        openTimerOverlay();
        updateAllUI();
    }

    private void saveLastUsedTimer(long seconds, String film) {
        try {
            SharedPreferences sp = getSharedPreferences("stralingstijd_prefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor ed = sp.edit();
            ed.putLong("last_timer_seconds", seconds);
            ed.putString("last_timer_film", film);
            ed.putString("last_timer_source", currentSource != null ? currentSource.type : "Ir-192");
            ed.putString("last_timer_source_name", currentSource != null ? currentSource.name : "");
            ed.putFloat("last_timer_thickness", (float) thickness);
            ed.putInt("last_timer_ffd", ffd);
            ed.putFloat("last_timer_ci", (float) currentCi);
            ed.putFloat("last_timer_factor", (float) materialFactor);
            ed.apply();

            TileService.getUpdater(this).requestUpdate(StralingstijdTileService.class);
        } catch (Exception ignored) {}
    }

    private void saveCurrentCalculationToPrefs() {
        if (isTimerRunning) return;
        double sec = calculateResultSecondsForFilm(selectedFilm);
        long timerSec = sec > 0 ? Math.round(sec) : 120;
        saveLastUsedTimer(timerSec, selectedFilm);
    }

    private void handleIncomingIntent(Intent intent) {
        if (intent != null && "start_timer".equals(intent.getStringExtra("action"))) {
            intent.removeExtra("action");
            if (isTimerRunning) {
                openTimerOverlay();
            } else {
                long customSec = intent.getLongExtra("seconds", 0);
                SharedPreferences sp = getSharedPreferences("stralingstijd_prefs", Context.MODE_PRIVATE);
                long lastSec = customSec > 0 ? customSec : sp.getLong("last_timer_seconds", 0);
                String lastFilm = sp.getString("last_timer_film", selectedFilm != null ? selectedFilm : "D4");
                if (lastSec <= 0) {
                    double sec = calculateResultSecondsForFilm(lastFilm);
                    lastSec = sec > 0 ? Math.round(sec) : 120;
                }
                startCountdown(lastSec, lastFilm);
            }
        }
    }

    private void toggleTimerPause() {
        if (!isTimerRunning && timerRemainingSeconds <= 0) return;

        if (isTimerPaused) {
            // Resume
            isTimerPaused = false;
            timerStartTime = SystemClock.elapsedRealtime(); // Reset start window so 5s on screen is granted
            isUserInteracting = false;
            timerTargetEndTime = timerStartTime + (timerRemainingSeconds * 1000);
            btnTimerPause.setText("PAUZE");
            btnTimerPause.setBackgroundResource(R.drawable.btn_timer_start);
            btnTimerPause.setTextColor(0xFF000000);
            acquireCpuWakeLock();
            wakeScreen();
            enterTimerAodMode();
            timerHandler.post(timerRunnable);
        } else {
            // Pause
            isTimerPaused = true;
            long now = SystemClock.elapsedRealtime();
            timerRemainingSeconds = Math.max(0, (timerTargetEndTime - now + 999) / 1000);
            btnTimerPause.setText("HERVAT");
            btnTimerPause.setBackgroundResource(R.drawable.btn_key_confirm);
            btnTimerPause.setTextColor(0xFF000000);
            timerHandler.removeCallbacks(timerRunnable);
            // Keep controls visible & screen awake when paused
            wakeScreen();
            exitTimerAodMode();
            if (aodHideHandler != null && aodHideRunnable != null) {
                aodHideHandler.removeCallbacks(aodHideRunnable);
            }
        }
        updateTimerUI();
        updateBannerTimer();
    }


    private void stopTimer() {
        isTimerRunning = false;
        isTimerPaused = false;
        timerRemainingSeconds = 0;
        timerHandler.removeCallbacks(timerRunnable);
        if (aodHideHandler != null && aodHideRunnable != null) {
            aodHideHandler.removeCallbacks(aodHideRunnable);
        }
        releaseCpuWakeLock();
        wakeScreen();
        exitTimerAodMode();
        setDimmedAodBrightness(false);
        stopAlarm();
        timerOverlay.setVisibility(View.GONE);
        updateAllUI();
    }

    private void minimizeTimer() {
        if (aodHideHandler != null && aodHideRunnable != null) {
            aodHideHandler.removeCallbacks(aodHideRunnable);
        }
        setDimmedAodBrightness(false);
        timerOverlay.setVisibility(View.GONE);
        updateBannerTimer();
        if (mainScrollView != null) {
            mainScrollView.smoothScrollTo(0, 0);
        }
    }

    private void openTimerOverlay() {
        timerOverlay.setVisibility(View.VISIBLE);
        if (isTimerRunning && !isTimerPaused && !isAlarmRinging) {
            wakeScreen();
            enterTimerAodMode();
        } else {
            wakeScreen();
            exitTimerAodMode();
        }
        updateTimerUI();
    }

    private void onTimerFinished() {
        isAlarmRinging = true;
        releaseCpuWakeLock();
        if (aodHideHandler != null && aodHideRunnable != null) {
            aodHideHandler.removeCallbacks(aodHideRunnable);
        }
        wakeScreen();
        exitTimerAodMode();
        setDimmedAodBrightness(false);

        updateTimerUI();
        updateBannerTimer();

        // Make sure overlay is visible so user sees it
        timerOverlay.setVisibility(View.VISIBLE);
        layoutAlarmBanner.setVisibility(View.VISIBLE);

        startAlarmVibration();
    }

    private void stopAlarm() {
        isAlarmRinging = false;
        stopVibration();
        releaseCpuWakeLock();
        wakeScreen();
        if (layoutAlarmBanner != null) layoutAlarmBanner.setVisibility(View.GONE);

        // Automatisch resetten naar de oorspronkelijke ingestelde tijd
        isTimerRunning = false;
        isTimerPaused = false;
        timerRemainingSeconds = timerTotalSeconds;
        btnTimerPause.setText("PAUZE");
        btnTimerPause.setBackgroundResource(R.drawable.btn_timer_start);
        btnTimerPause.setTextColor(0xFF000000);

        exitTimerAodMode();
        setDimmedAodBrightness(false);

        updateTimerUI();
        updateBannerTimer();
    }

    private void updateTimerUI() {
        timerHeader.setText("TIMER: " + timerFilmName + " (" + currentSource.type + ")");
        String timerMatInfo = Math.abs(materialFactor - 1.0) >= 0.005 ? String.format(Locale.US, " • Fac: %.2f", materialFactor) : "";
        timerSubInfo.setText(String.format(Locale.US, "%.1fmm • FFD %dcm • %.1f Ci%s", thickness, ffd, currentCi, timerMatInfo));

        long m = timerRemainingSeconds / 60;
        long s = timerRemainingSeconds % 60;
        if (m >= 60) {
            long h = m / 60;
            m = m % 60;
            timerDisplay.setText(String.format(Locale.US, "%d:%02d:%02d", h, m, s));
        } else {
            timerDisplay.setText(String.format(Locale.US, "%02d:%02d", m, s));
        }

        if (timerAodBadge != null) {
            String matStr = (selectedMaterial != null) ? (" • " + selectedMaterial.name) : (Math.abs(materialFactor - 1.0) >= 0.005 ? String.format(Locale.US, " • %.2fx", materialFactor) : "");
            timerAodBadge.setText(String.format(Locale.US, "⏱ %s (%s)%s", timerFilmName, currentSource.type, matStr));
        }

        if (isAlarmRinging) {
            timerDisplay.setTextColor(COLOR_CANCEL_RED);
        } else if (isTimerPaused) {
            timerDisplay.setTextColor(COLOR_ACCENT);
        } else if (isTimerAodMode) {
            timerDisplay.setTextColor(0xFF888888);
        } else {
            timerDisplay.setTextColor(COLOR_TEXT_WHITE);
        }

        long totM = timerTotalSeconds / 60;
        long totS = timerTotalSeconds % 60;
        int pct = (timerTotalSeconds > 0) ? (int) (100 - (timerRemainingSeconds * 100 / timerTotalSeconds)) : 100;
        timerProgressText.setText(String.format(Locale.US, "Tot: %dm %02ds • Voortgang: %d%%", totM, totS, Math.min(100, Math.max(0, pct))));
    }

    private void updateBannerTimer() {
        if (isTimerRunning || isTimerPaused || isAlarmRinging) {
            bannerActiveTimer.setVisibility(View.VISIBLE);
            long m = timerRemainingSeconds / 60;
            long s = timerRemainingSeconds % 60;
            String status = isAlarmRinging ? "⚠ ALARM" : (isTimerPaused ? "PAUZE" : "BEZIG");
            txtBannerTimerTime.setText(String.format(Locale.US, "⏱ %s: %02d:%02d", status, m, s));
        } else {
            bannerActiveTimer.setVisibility(View.GONE);
        }
    }

    // =========================================================================
    // FULLSCREEN NUMPAD OVERLAY (SCALED FOR WEAR OS CIRCULAR SCREEN)
    // =========================================================================
    private LinearLayout buildFullscreenNumpad() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(COLOR_BG);
        root.setPadding(dp(8), dp(10), dp(8), dp(6));
        root.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // 1. Header Title
        numpadTitle = new TextView(this);
        numpadTitle.setText("INVOER");
        numpadTitle.setTextSize(11);
        numpadTitle.setTextColor(COLOR_ACCENT);
        numpadTitle.setTypeface(Typeface.DEFAULT_BOLD);
        numpadTitle.setGravity(Gravity.CENTER);
        root.addView(numpadTitle);

        // 2. Display Row: [ Value ] and [ Backspace ⌫ ]
        LinearLayout displayRow = new LinearLayout(this);
        displayRow.setOrientation(LinearLayout.HORIZONTAL);
        displayRow.setGravity(Gravity.CENTER);
        displayRow.setLayoutParams(new LinearLayout.LayoutParams(dp(156), ViewGroup.LayoutParams.WRAP_CONTENT));

        numpadDisplay = new TextView(this);
        numpadDisplay.setText("_");
        numpadDisplay.setTextSize(22);
        numpadDisplay.setTextColor(COLOR_TEXT_WHITE);
        numpadDisplay.setTypeface(Typeface.DEFAULT_BOLD);
        numpadDisplay.setGravity(Gravity.CENTER);
        numpadDisplay.setPadding(0, 0, 0, 0);
        numpadDisplay.setIncludeFontPadding(false);
        numpadDisplay.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        displayRow.addView(numpadDisplay);

        TextView btnBk = new TextView(this);
        btnBk.setText("⌫");
        btnBk.setTextSize(16);
        btnBk.setTextColor(COLOR_TEXT_WHITE);
        btnBk.setTypeface(Typeface.DEFAULT_BOLD);
        btnBk.setGravity(Gravity.CENTER);
        btnBk.setPadding(0, 0, 0, 0);
        btnBk.setIncludeFontPadding(false);
        btnBk.setBackgroundResource(R.drawable.btn_key_normal);
        btnBk.setClickable(true);
        btnBk.setFocusable(true);
        btnBk.setLayoutParams(new LinearLayout.LayoutParams(dp(38), dp(26)));
        btnBk.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                haptic(15);
                handleNumpadKey("⌫");
            }
        });
        displayRow.addView(btnBk);
        root.addView(displayRow);

        addSpace(root, dp(3));

        // 3. Keypad Container: Exactly 156dp wide to guarantee circular fit & zero overlap
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
                TextView b = createNumpadKey(key, 20, COLOR_TEXT_WHITE, R.drawable.btn_key_normal, 1.0f, new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        handleNumpadKey(key);
                    }
                });
                row.addView(b);
            }
            keyGrid.addView(row);
        }

        // Row 4 for Integers: [ TERUG ]   [ 0 ]   [ OK ]
        numpadRow4Integer = new LinearLayout(this);
        numpadRow4Integer.setOrientation(LinearLayout.HORIZONTAL);
        numpadRow4Integer.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(35)));

        TextView btnIntCancel = createNumpadKey("TERUG", 12, COLOR_CANCEL_RED, R.drawable.btn_key_cancel, 1.15f, new View.OnClickListener() {
            @Override public void onClick(View v) {
                numpadOverlay.setVisibility(View.GONE);
            }
        });

        TextView btnInt0 = createNumpadKey("0", 20, COLOR_TEXT_WHITE, R.drawable.btn_key_normal, 1.0f, new View.OnClickListener() {
            @Override public void onClick(View v) {
                handleNumpadKey("0");
            }
        });

        TextView btnIntConfirm = createNumpadKey("OK", 14, 0xFF000000, R.drawable.btn_key_confirm, 1.15f, new View.OnClickListener() {
            @Override public void onClick(View v) {
                handleNumpadKey("✓");
            }
        });

        numpadRow4Integer.addView(btnIntCancel);
        numpadRow4Integer.addView(btnInt0);
        numpadRow4Integer.addView(btnIntConfirm);
        keyGrid.addView(numpadRow4Integer);

        // Row 4 for Decimals: [ ✕ ]  [ 0 ]  [ . ]  [ ✓ ]
        numpadRow4Decimal = new LinearLayout(this);
        numpadRow4Decimal.setOrientation(LinearLayout.HORIZONTAL);
        numpadRow4Decimal.setVisibility(View.GONE);
        numpadRow4Decimal.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(35)));

        TextView btnDecCancel = createNumpadKey("✕", 16, COLOR_CANCEL_RED, R.drawable.btn_key_cancel, 1.0f, new View.OnClickListener() {
            @Override public void onClick(View v) {
                numpadOverlay.setVisibility(View.GONE);
            }
        });

        TextView btnDec0 = createNumpadKey("0", 20, COLOR_TEXT_WHITE, R.drawable.btn_key_normal, 1.0f, new View.OnClickListener() {
            @Override public void onClick(View v) {
                handleNumpadKey("0");
            }
        });

        TextView btnDecDot = createNumpadKey(".", 22, COLOR_TEXT_WHITE, R.drawable.btn_key_normal, 1.0f, new View.OnClickListener() {
            @Override public void onClick(View v) {
                handleNumpadKey(".");
            }
        });

        TextView btnDecConfirm = createNumpadKey("✓", 18, 0xFF000000, R.drawable.btn_key_confirm, 1.0f, new View.OnClickListener() {
            @Override public void onClick(View v) {
                handleNumpadKey("✓");
            }
        });

        numpadRow4Decimal.addView(btnDecCancel);
        numpadRow4Decimal.addView(btnDec0);
        numpadRow4Decimal.addView(btnDecDot);
        numpadRow4Decimal.addView(btnDecConfirm);
        keyGrid.addView(numpadRow4Decimal);

        root.addView(keyGrid);
        return root;
    }

    private TextView createNumpadKey(final String text, int textSizeSp, int textColor, int bgRes, float weight, final View.OnClickListener onClick) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(textSizeSp);
        tv.setTextColor(textColor);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(0, 0, 0, 0);
        tv.setIncludeFontPadding(false);
        tv.setBackgroundResource(bgRes);
        tv.setClickable(true);
        tv.setFocusable(true);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, weight);
        lp.setMargins(dp(2), dp(1), dp(2), dp(1));
        tv.setLayoutParams(lp);

        tv.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                haptic(15);
                onClick.onClick(v);
            }
        });
        return tv;
    }

    private void openNumpad(String title, double currentVal, int target, boolean isDecimal) {
        if (target == TARGET_CI && currentSource != null && !currentSource.isManual) {
            return;
        }
        haptic(20);
        numpadTarget = target;
        numpadIsDecimal = isDecimal;
        String curStr = isDecimal ? String.format(Locale.US, "%.1f", currentVal) : String.valueOf((int) Math.round(currentVal));
        numpadTitle.setText(title + " (Huidig: " + curStr + ")");
        numpadBuffer = "";
        numpadDisplay.setText("_");

        numpadRow4Integer.setVisibility(isDecimal ? View.GONE : View.VISIBLE);
        numpadRow4Decimal.setVisibility(isDecimal ? View.VISIBLE : View.GONE);

        numpadOverlay.setVisibility(View.VISIBLE);
    }

    private void handleNumpadKey(String key) {
        if (key.equals("✓")) {
            applyNumpadValue();
            numpadOverlay.setVisibility(View.GONE);
            updateAllUI();
            return;
        }
        if (key.equals("⌫")) {
            if (numpadBuffer.length() > 0) {
                numpadBuffer = numpadBuffer.substring(0, numpadBuffer.length() - 1);
            }
        } else if (key.equals(".")) {
            if (!numpadBuffer.contains(".") && numpadBuffer.length() < 7) {
                numpadBuffer = numpadBuffer.isEmpty() ? "0." : numpadBuffer + ".";
            }
        } else {
            if (numpadBuffer.length() < 7) {
                numpadBuffer += key;
            }
        }
        numpadDisplay.setText(numpadBuffer.isEmpty() ? "_" : numpadBuffer);
    }

    private void applyNumpadValue() {
        if (numpadBuffer.isEmpty()) return;
        try {
            double v = Double.parseDouble(numpadBuffer);
            switch (numpadTarget) {
                case TARGET_THICKNESS:
                    thickness = Math.max(0.1, v);
                    break;
                case TARGET_FFD:
                    ffd = Math.max(1, (int) Math.round(v));
                    break;
                case TARGET_CI:
                    currentCi = Math.max(0.1, v);
                    break;
                case TARGET_FACTOR:
                    materialFactor = Math.max(0.01, v);
                    selectedMaterial = null;
                    break;
            }
        } catch (Exception ignored) {}
    }

    // =========================================================================
    // CALCULATION & UI UPDATE
    // =========================================================================
    private void updateAllUI() {
        // 1. Source Details & Ci Row enabled/disabled state
        if (currentSource.isManual) {
            txtSourceName.setText(currentSource.type + " (Handm.)");
            txtSourceInfo.setText("Handmatige invoer (" + currentSource.type + ")");
            if (rowCi != null) {
                rowCi.setClickable(true);
                rowCi.setBackgroundResource(R.drawable.card_touch_val);
            }
            txtSourceCi.setTextColor(COLOR_TEXT_WHITE);
        } else {
            txtSourceName.setText(currentSource.name + " (" + currentSource.type + ")");
            txtSourceInfo.setText("Bron " + currentSource.name + " (vast o.b.v. verval)");
            if (rowCi != null) {
                rowCi.setClickable(false);
                rowCi.setBackgroundResource(R.drawable.card_bg);
            }
            txtSourceCi.setTextColor(COLOR_TEXT_DIM);
        }
        txtSourceCi.setText(String.format(Locale.US, "%.1f Ci", currentCi));

        // 2. Thickness & FFD
        txtThicknessVal.setText(String.format(Locale.US, "%.1f mm", thickness));
        txtFfdVal.setText(String.format(Locale.US, "%d cm", ffd));

        // 2b. Material Factor UI
        if (selectedMaterial != null) {
            materialFactor = selectedMaterial.getFactor(currentSource.type);
        }
        if (txtMatFactorBox != null) {
            txtMatFactorBox.setText(String.format(Locale.US, "%.2fx", materialFactor));
        }
        if (txtMatSubLabel != null) {
            if (selectedMaterial != null) {
                txtMatSubLabel.setText(selectedMaterial.name + " (" + currentSource.type + ")");
            } else if (Math.abs(materialFactor - 1.0) < 0.001) {
                txtMatSubLabel.setText("Geen factor (1.00x)");
            } else {
                txtMatSubLabel.setText(String.format(Locale.US, "Handmatig: %.2fx", materialFactor));
            }
        }

        // 3. Film Selector Buttons
        for (int i = 0; i < FILM_TYPES.length; i++) {
            boolean active = FILM_TYPES[i].equals(selectedFilm);
            setSegmentStyle(btnFilms[i], active);
        }

        // 4. Result Card Calculation (Single selected film)
        double sec = calculateResultSecondsForFilm(selectedFilm);
        txtResultTime.setText(formatTime(sec));
        String matInfo = Math.abs(materialFactor - 1.0) >= 0.005 ? String.format(Locale.US, " • Fac: %.2f", materialFactor) : "";
        txtResultSub.setText(String.format(Locale.US, "%s Film • %s • %.1fmm • %dcm • %.1f Ci%s",
                selectedFilm, currentSource.type, thickness, ffd, currentCi, matInfo));

        updateBannerTimer();
        saveCurrentCalculationToPrefs();
    }

    private double calculateResultSecondsForFilm(String film) {
        if (currentCi <= 0 || ffd <= 0 || thickness < 0) return 0;

        boolean isSe75 = "Se75".equalsIgnoreCase(currentSource.type);
        double k   = isSe75 ? SE75_K : IR192_K;
        double hvl = isSe75 ? SE75_HVL : IR192_HVL;
        double ff  = isSe75 ? SE75_FF : IR192_FF;

        double ffd_m = ffd / 100.0;
        // Formula from stralingstijd.html:
        // base_exposure = (consts.ff * (ffd_m * ffd_m)) / (ci * consts.k); // in minutes
        // attenuation = Math.pow(2, (thick / consts.hvl));
        // time_d7 = base_exposure * attenuation;
        // if (iso === 'Se75') time_d7 *= 1.05;
        double baseExposureMin = (ff * (ffd_m * ffd_m)) / (currentCi * k);
        double attenuation = Math.pow(2.0, thickness / hvl);
        double timeD7Min = baseExposureMin * attenuation;
        if (isSe75) timeD7Min *= 1.05;

        // Materiaalfactor toepassen
        timeD7Min *= materialFactor;

        double timeD7Sec = timeD7Min * 60.0;
        double timeD4Sec = timeD7Sec * 2.44;
        double timeD5Sec = timeD4Sec / 1.6666;

        if ("D4".equalsIgnoreCase(film)) return timeD4Sec;
        if ("D5".equalsIgnoreCase(film)) return timeD5Sec;
        if ("D7".equalsIgnoreCase(film)) return timeD7Sec;
        return timeD4Sec; // fallback
    }

    private String formatTime(double totalSec) {
        if (totalSec <= 0) return "0m 00s";
        long rounded = Math.round(totalSec);
        long m = rounded / 60;
        long s = rounded % 60;
        if (m >= 60) {
            long h = m / 60;
            m = m % 60;
            return String.format(Locale.US, "%dh %02dm %02ds", h, m, s);
        }
        return String.format(Locale.US, "%dm %02ds", m, s);
    }

    // =========================================================================
    // ROTARY CROWN & BACK NAVIGATION
    // =========================================================================
    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_SCROLL) {
            float delta = -event.getAxisValue(MotionEvent.AXIS_SCROLL);
            if (delta != 0) {
                if (timerOverlay != null && timerOverlay.getVisibility() == View.VISIBLE) {
                    if (isScreenSleeping) {
                        wakeScreen();
                        isUserInteracting = true;
                        exitTimerAodMode();
                        scheduleUserInactivityTimeout();
                        haptic(10);
                        return true;
                    }
                    if (isTimerAodMode) {
                        isUserInteracting = true;
                        exitTimerAodMode();
                        scheduleUserInactivityTimeout();
                        haptic(10);
                        return true;
                    }
                }
                if (materialPickerOverlay != null && materialPickerOverlay.getVisibility() == View.VISIBLE) {
                    if (materialPickerScrollView != null) materialPickerScrollView.scrollBy(0, (int) (delta * dp(45)));
                    haptic(5);
                    return true;
                }
                if (sourcePickerOverlay != null && sourcePickerOverlay.getVisibility() == View.VISIBLE) {
                    if (sourcePickerScrollView != null) sourcePickerScrollView.scrollBy(0, (int) (delta * dp(45)));
                    haptic(5);
                    return true;
                }
                if (numpadOverlay.getVisibility() != View.VISIBLE && timerOverlay.getVisibility() != View.VISIBLE) {
                    mainScrollView.scrollBy(0, (int) (delta * dp(45)));
                    haptic(5);
                    return true;
                }
            }
        }
        return super.onGenericMotionEvent(event);
    }

    @Override
    public void onBackPressed() {
        if (numpadOverlay != null && numpadOverlay.getVisibility() == View.VISIBLE) {
            numpadOverlay.setVisibility(View.GONE);
            return;
        }
        if (timerOverlay != null && timerOverlay.getVisibility() == View.VISIBLE) {
            if (isScreenSleeping) {
                wakeScreen();
                isUserInteracting = true;
                exitTimerAodMode();
                scheduleUserInactivityTimeout();
                return;
            }
            if (isTimerAodMode) {
                exitTimerAodMode();
                return;
            }
            minimizeTimer();
            return;
        }
        if (materialPickerOverlay != null && materialPickerOverlay.getVisibility() == View.VISIBLE) {
            materialPickerOverlay.setVisibility(View.GONE);
            return;
        }
        if (sourcePickerOverlay != null && sourcePickerOverlay.getVisibility() == View.VISIBLE) {
            sourcePickerOverlay.setVisibility(View.GONE);
            return;
        }
        if (isAlarmRinging) {
            stopAlarm();
            return;
        }
        if (timerOverlay != null && timerOverlay.getVisibility() == View.VISIBLE) {
            minimizeTimer();
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopVibration();
        releaseCpuWakeLock();
        if (timerHandler != null && timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
    }

    // =========================================================================
    // VIEW HELPERS
    // =========================================================================
    private LinearLayout createBaseCard(String title) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.card_bg);
        card.setPadding(dp(12), dp(10), dp(12), dp(10));
        card.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView t = new TextView(this);
        t.setText(title);
        t.setTextSize(10);
        t.setTextColor(COLOR_TEXT_DIM);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        card.addView(t);
        addSpace(card, dp(6));

        return card;
    }

    private LinearLayout createFullWidthValueRow(String label, TextView valueView, int valueColor, View.OnClickListener onClick) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundResource(R.drawable.card_touch_val);
        row.setPadding(dp(12), dp(8), dp(12), dp(8));
        row.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(44)));

        TextView lbl = new TextView(this);
        lbl.setText(label);
        lbl.setTextSize(12);
        lbl.setTextColor(COLOR_TEXT_MUTED);
        lbl.setTypeface(Typeface.DEFAULT_BOLD);
        lbl.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        valueView.setTextSize(20);
        valueView.setTextColor(valueColor);
        valueView.setTypeface(Typeface.DEFAULT_BOLD);
        valueView.setGravity(Gravity.END);

        row.addView(lbl);
        row.addView(valueView);

        final View.OnClickListener click = onClick;
        row.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                haptic(20);
                click.onClick(v);
            }
        });
        return row;
    }

    private TextView createSegmentBtn(String text, View.OnClickListener onClick) {
        TextView b = new TextView(this);
        b.setText(text);
        b.setTextSize(11);
        b.setSingleLine(true);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setGravity(Gravity.CENTER);
        b.setPadding(0, 0, 0, 0);
        b.setIncludeFontPadding(false);
        b.setClickable(true);
        b.setFocusable(true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.0f);
        lp.setMargins(dp(2), 0, dp(2), 0);
        b.setLayoutParams(lp);
        b.setOnClickListener(onClick);
        return b;
    }

    private void setSegmentStyle(TextView b, boolean active) {
        if (active) {
            b.setBackgroundResource(R.drawable.btn_pill_active);
            b.setTextColor(0xFF111111);
        } else {
            b.setBackgroundResource(R.drawable.btn_pill_inactive);
            b.setTextColor(COLOR_TEXT_WHITE);
        }
    }

    private void addSpace(ViewGroup parent, int sizePx) {
        View space = new View(this);
        space.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, sizePx));
        parent.addView(space);
    }
}
