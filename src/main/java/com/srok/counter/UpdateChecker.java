package com.srok.counter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/** Checks GitHub Releases for a newer build and asks the user to update. */
final class UpdateChecker {

    private static final String LATEST =
            "https://api.github.com/repos/NeuroGas/Czoo-counter/releases/latest";

    private static AlertDialog shown;

    private UpdateChecker() {}

    static void check(final Activity activity) {
        if (shown != null && shown.isShowing()) return;
        final long current = currentVersion(activity);
        new Thread(() -> {
            try {
                HttpURLConnection c = (HttpURLConnection) new URL(LATEST).openConnection();
                c.setConnectTimeout(8000);
                c.setReadTimeout(8000);
                c.setRequestProperty("Accept", "application/vnd.github+json");
                if (c.getResponseCode() != 200) return;
                StringBuilder sb = new StringBuilder();
                try (BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), "UTF-8"))) {
                    String line;
                    while ((line = r.readLine()) != null) sb.append(line);
                }
                JSONObject json = new JSONObject(sb.toString());
                String tag = json.optString("tag_name", "");          // "build-42"
                long latest = Long.parseLong(tag.replaceAll("\\D", ""));
                JSONArray assets = json.optJSONArray("assets");
                String url = json.optString("html_url");
                if (assets != null) {
                    for (int i = 0; i < assets.length(); i++) {
                        String u = assets.getJSONObject(i).optString("browser_download_url");
                        if (u.endsWith(".apk")) { url = u; break; }
                    }
                }
                if (latest > current) {
                    final String download = url;
                    new Handler(Looper.getMainLooper()).post(() -> ask(activity, download));
                }
            } catch (Exception ignored) {
                // no internet or bad answer: just skip the check
            }
        }).start();
    }

    private static void ask(Activity activity, String url) {
        if (activity.isFinishing() || activity.isDestroyed()) return;
        if (shown != null && shown.isShowing()) return;
        shown = new AlertDialog.Builder(activity)
                .setTitle("🔥 Вышло обновление")
                .setMessage("Есть новая версия Jail Time. Нажмите «Обновить», скачайте файл и установите его поверх — все данные сохранятся.")
                .setCancelable(false)
                .setPositiveButton("Обновить", (d, w) -> {
                    shown = null;
                    try {
                        activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                    } catch (Exception ignored) {}
                })
                .show();
    }

    @SuppressWarnings("deprecation")
    private static long currentVersion(Activity a) {
        try {
            PackageInfo pi = a.getPackageManager().getPackageInfo(a.getPackageName(), 0);
            return Build.VERSION.SDK_INT >= 28 ? pi.getLongVersionCode() : pi.versionCode;
        } catch (Exception e) {
            return Long.MAX_VALUE;
        }
    }
}
