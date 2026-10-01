package com.rt.stralingstijdwatch;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SourceDb {

    private static final String PREF_NAME = "source_db_prefs";
    private static final String KEY_LAST_SYNC = "last_sync_timestamp";
    private static final String CACHE_FILE_NAME = "bronnen_cached.csv";

    private static final String GITHUB_RAW_URL = "https://raw.githubusercontent.com/vincepall/rt/main/bronnen.csv";
    private static final String GITHUB_PAGES_URL = "https://vincepall.github.io/rt/bronnen.csv";

    public interface SyncCallback {
        void onSyncFinished(boolean success, List<SourceItem> sources, String statusMessage);
    }

    public static class SourceItem {
        public final String name;
        public final String type; // "Ir192" or "Se75"
        public final double initialCi;
        public final String dateStr; // "dd-MM-yyyy"
        public final double focalSize;
        public final boolean isManual;

        public SourceItem(String name, String type, double initialCi, String dateStr, double focalSize, boolean isManual) {
            this.name = name;
            this.type = type;
            this.initialCi = initialCi;
            this.dateStr = dateStr;
            this.focalSize = focalSize;
            this.isManual = isManual;
        }

        public double calculateCurrentActivity() {
            if (isManual || dateStr == null || dateStr.isEmpty()) {
                return initialCi;
            }
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.US);
                Date sourceDate = sdf.parse(dateStr);
                if (sourceDate == null) return initialCi;
                long diffMs = System.currentTimeMillis() - sourceDate.getTime();
                double days = (double) diffMs / (1000.0 * 60.0 * 60.0 * 24.0);
                double halfLife = "Se75".equalsIgnoreCase(type) ? 120.0 : 74.0;
                double cur = initialCi / Math.pow(2.0, days / halfLife);
                return Math.max(0.1, cur);
            } catch (Exception e) {
                return initialCi;
            }
        }

        public String getDisplayName() {
            if (isManual) {
                return name;
            }
            double curCi = calculateCurrentActivity();
            return String.format(Locale.US, "%s (%s) [%.1f Ci]", name, type, curCi);
        }
    }

    public static List<SourceItem> getSources(Context context) {
        return loadSourcesFromLocal(context);
    }

    public static List<SourceItem> loadSourcesFromLocal(Context context) {
        List<SourceItem> list = new ArrayList<>();
        // 1. Manual options always on top
        list.add(new SourceItem("Ir-192 (Handmatig)", "Ir192", 50.0, "", 0, true));
        list.add(new SourceItem("Se-75 (Handmatig)", "Se75", 80.0, "", 0, true));

        // 2. Try loading from cached file in internal storage
        boolean loaded = false;
        File cacheFile = (context != null) ? new File(context.getFilesDir(), CACHE_FILE_NAME) : null;
        if (cacheFile != null && cacheFile.exists() && cacheFile.length() > 20) {
            try {
                FileInputStream fis = new FileInputStream(cacheFile);
                loaded = parseCsvStream(fis, list);
                fis.close();
            } catch (Exception ignored) {}
        }

        // 3. If not cached, load from assets/bronnen.csv
        if (!loaded && context != null && context.getAssets() != null) {
            try {
                InputStream is = context.getAssets().open("bronnen.csv");
                loaded = parseCsvStream(is, list);
                is.close();
            } catch (Exception ignored) {}
        }

        // 4. Hardcoded fallback if everything else fails
        if (!loaded) {
            list.add(new SourceItem("35042P", "Ir192", 56.5, "31-07-2026", 2.358, false));
            list.add(new SourceItem("RIL294", "Se75", 80.68, "31-03-2026", 3.0, false));
            list.add(new SourceItem("97969M", "Se75", 84.50, "21-04-2026", 3.524, false));
            list.add(new SourceItem("RR3508", "Se75", 94.73, "24-02-2026", 3.0, false));
            list.add(new SourceItem("RR3820", "Se75", 84.28, "29-05-2026", 3.0, false));
        }

        return list;
    }

    private static boolean parseCsvStream(InputStream is, List<SourceItem> outList) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            boolean first = true;
            boolean foundAny = false;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                if (first) {
                    first = false;
                    if (line.toLowerCase().contains("bron")) continue; // header
                }
                String[] cols = line.split(",");
                if (cols.length >= 4) {
                    String name = cols[0].trim();
                    String type = cols[1].trim();
                    double initCi = Double.parseDouble(cols[2].trim());
                    String date = cols[3].trim();
                    double focal = cols.length > 4 ? Double.parseDouble(cols[4].trim()) : 0.0;
                    outList.add(new SourceItem(name, type, initCi, date, focal, false));
                    foundAny = true;
                }
            }
            return foundAny;
        } catch (Exception e) {
            return false;
        }
    }

    public static String getLastSyncStatus(Context context) {
        if (context == null) return "";
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String last = sp.getString(KEY_LAST_SYNC, "");
        if (last.isEmpty()) {
            return "Nog niet gesynchroniseerd";
        }
        return "GitHub: " + last;
    }

    public static void syncWithGithub(final Context context, final SyncCallback callback) {
        new SyncThread(context, callback).start();
    }

    private static class SyncResultRunnable implements Runnable {
        private final SyncCallback callback;
        private final boolean success;
        private final List<SourceItem> resultList;
        private final String statusMsg;

        SyncResultRunnable(SyncCallback callback, boolean success, List<SourceItem> resultList, String statusMsg) {
            this.callback = callback;
            this.success = success;
            this.resultList = resultList;
            this.statusMsg = statusMsg;
        }

        @Override
        public void run() {
            if (callback != null) {
                callback.onSyncFinished(success, resultList, statusMsg);
            }
        }
    }

    private static class SyncThread extends Thread {
        private final Context context;
        private final SyncCallback callback;

        SyncThread(Context context, SyncCallback callback) {
            this.context = context;
            this.callback = callback;
        }

        @Override
        public void run() {
            String csvData = downloadUrl(GITHUB_RAW_URL + "?t=" + System.currentTimeMillis());
            if (csvData == null || csvData.length() < 20) {
                // Fallback to GitHub Pages
                csvData = downloadUrl(GITHUB_PAGES_URL + "?t=" + System.currentTimeMillis());
            }

            boolean ok = false;
            String msg = "";

            if (csvData != null && csvData.length() >= 20 && csvData.contains(",")) {
                // Save to cache file
                try {
                    File cacheFile = new File(context.getFilesDir(), CACHE_FILE_NAME);
                    FileOutputStream fos = new FileOutputStream(cacheFile);
                    fos.write(csvData.getBytes("UTF-8"));
                    fos.close();

                    SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.US);
                    String nowTime = timeFormat.format(new Date());
                    SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                    sp.edit().putString(KEY_LAST_SYNC, "Vandaag " + nowTime).apply();

                    ok = true;
                    msg = "✓ Bijgewerkt (" + nowTime + ")";
                } catch (Exception e) {
                    ok = false;
                    msg = "Fout bij opslaan";
                }
            } else {
                ok = false;
                msg = "Geen verbinding met GitHub";
            }

            final boolean success = ok;
            final String statusMsg = msg;
            final List<SourceItem> resultList = loadSourcesFromLocal(context);

            new Handler(Looper.getMainLooper()).post(new SyncResultRunnable(callback, success, resultList, statusMsg));
        }
    }

    private static String downloadUrl(String urlStr) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(6000);
            conn.setUseCaches(false);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 WearOS-Stralingstijd");
            conn.connect();

            int code = conn.getResponseCode();
            if (code == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                br.close();
                return sb.toString();
            }
        } catch (Exception ignored) {
        } finally {
            if (conn != null) {
                try { conn.disconnect(); } catch (Exception ignored) {}
            }
        }
        return null;
    }
}
