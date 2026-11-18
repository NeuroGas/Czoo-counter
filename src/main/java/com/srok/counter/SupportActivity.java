package com.srok.counter;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputFilter;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** "Поддержать" screen: send a message via Google Form, show the wall from the published sheet. */
public class SupportActivity extends Activity {

    private static final String FORM_POST =
            "https://docs.google.com/forms/d/e/1FAIpQLSdHZqE50PTBOQUsKfzK-EeNXrki3KpoZs7LwPjM6uZnEYgGOQ/formResponse";
    private static final String FIELD_NAME = "entry.1489676908";
    private static final String FIELD_TEXT = "entry.1533154854";
    /** Published CSV of the responses sheet ("Опубликовать в интернете" → CSV). */
    private static final String WALL_CSV = "https://docs.google.com/spreadsheets/d/1JepR1MxWIhHWaiara80SKYeYqfMSmYR8dJ2XMpFpTRA/gviz/tq?tqx=out:csv";

    private static final long COOLDOWN_MS = 60_000;

    private final Handler main = new Handler(Looper.getMainLooper());
    private EditText name, text;
    private Button send;
    private LinearLayout wall;
    private TextView wallStatus;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        box.setPadding(pad, dp(28), pad, dp(28));

        TextView title = label("❤️ Поддержать Сапфира", 26, true, "#FFFFFF");
        title.setGravity(Gravity.CENTER);
        TextView sub = label("Напишите своё имя и пару тёплых слов. Их увидят все, кто откроет приложение.",
                15, false, "#D9FFFFFF");
        sub.setGravity(Gravity.CENTER);

        name = input("Ваше имя", false, 40);
        text = input("Ваше пожелание", true, 500);

        send = new Button(this);
        send.setText("Отправить ❤️");
        send.setAllCaps(false);
        send.setTextColor(Color.WHITE);
        send.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        send.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable sbg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{Color.parseColor("#E8102E"), Color.parseColor("#8A0A1C")});
        sbg.setCornerRadius(dp(26));
        send.setBackground(sbg);
        send.setOnClickListener(v -> submit());

        TextView wallTitle = label("Стена поддержки", 20, true, "#FFFFFF");
        wallStatus = label("Загрузка…", 14, false, "#B3FFFFFF");
        wall = new LinearLayout(this);
        wall.setOrientation(LinearLayout.VERTICAL);

        box.addView(title);
        box.addView(gap(8));
        box.addView(sub);
        box.addView(gap(20));
        box.addView(name);
        box.addView(gap(10));
        box.addView(text);
        box.addView(gap(14));
        box.addView(send, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));
        box.addView(gap(30));
        box.addView(wallTitle);
        box.addView(gap(6));
        box.addView(wallStatus);
        box.addView(gap(6));
        box.addView(wall);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.parseColor("#7A0A1A"), Color.parseColor("#2A0408"), Color.parseColor("#050505")}));
        LinearLayout holder = new LinearLayout(this);
        holder.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                Math.min(dp(640), getResources().getDisplayMetrics().widthPixels), ViewGroup.LayoutParams.WRAP_CONTENT);
        holder.addView(box, blp);
        scroll.addView(holder);
        setContentView(scroll);

        loadWall();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Music.screenStarted(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Music.screenStopped();
    }

    // ---------- send ----------

    private void submit() {
        final String n = name.getText().toString().trim();
        final String t = text.getText().toString().trim();
        if (n.isEmpty()) { name.setError("Напишите имя"); return; }
        if (t.isEmpty()) { text.setError("Напишите пожелание"); return; }
        long last = prefs().getLong("last_send", 0);
        if (System.currentTimeMillis() - last < COOLDOWN_MS) {
            toast("Подождите минутку перед следующим сообщением");
            return;
        }
        send.setEnabled(false);
        send.setText("Отправляю…");
        new Thread(() -> {
            boolean ok = false;
            try {
                String body = FIELD_NAME + "=" + URLEncoder.encode(n, "UTF-8")
                        + "&" + FIELD_TEXT + "=" + URLEncoder.encode(t, "UTF-8");
                HttpURLConnection c = (HttpURLConnection) new URL(FORM_POST).openConnection();
                c.setConnectTimeout(10000);
                c.setReadTimeout(10000);
                c.setRequestMethod("POST");
                c.setDoOutput(true);
                c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                try (OutputStream os = c.getOutputStream()) {
                    os.write(body.getBytes("UTF-8"));
                }
                int code = c.getResponseCode();
                ok = code >= 200 && code < 400;
            } catch (Exception ignored) {}
            final boolean sent = ok;
            main.post(() -> {
                send.setEnabled(true);
                send.setText("Отправить ❤️");
                if (sent) {
                    prefs().edit().putLong("last_send", System.currentTimeMillis()).apply();
                    text.setText("");
                    toast("Спасибо! Ваше пожелание отправлено ❤️");
                    addCard(n, t, "только что", 0);
                    wallStatus.setText("На стене оно появится у всех в течение нескольких минут.");
                } else {
                    toast("Не получилось отправить. Проверьте интернет.");
                }
            });
        }).start();
    }

    // ---------- wall ----------

    private void loadWall() {
        if (!WALL_CSV.startsWith("http")) {
            wallStatus.setText("Скоро здесь появятся пожелания.");
            return;
        }
        new Thread(() -> {
            List<String[]> rows = null;
            try {
                HttpURLConnection c = (HttpURLConnection) new URL(WALL_CSV + (WALL_CSV.contains("?") ? "&" : "?")
                        + "t=" + System.currentTimeMillis() / 60000).openConnection();
                c.setInstanceFollowRedirects(true);
                c.setConnectTimeout(10000);
                c.setReadTimeout(10000);
                StringBuilder sb = new StringBuilder();
                try (BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), "UTF-8"))) {
                    char[] buf = new char[4096];
                    int k;
                    while ((k = r.read(buf)) > 0) sb.append(buf, 0, k);
                }
                rows = parseCsv(sb.toString());
            } catch (Exception ignored) {}
            final List<String[]> result = rows;
            main.post(() -> showWall(result));
        }).start();
    }

    private void showWall(List<String[]> rows) {
        if (rows == null) {
            wallStatus.setText("Не удалось загрузить стену. Проверьте интернет.");
            return;
        }
        List<String[]> data = new ArrayList<>();
        for (int i = 1; i < rows.size(); i++) {           // skip header
            String[] r = rows.get(i);
            if (r.length >= 3 && !r[1].trim().isEmpty() && !r[2].trim().isEmpty()) data.add(r);
        }
        Collections.reverse(data);                         // newest first
        if (data.isEmpty()) {
            wallStatus.setText("Пока пусто — будьте первым ❤️");
            return;
        }
        wallStatus.setText("Пожеланий: " + data.size());
        int i = 0;
        for (String[] r : data) {
            if (i >= 150) break;
            String date = r[0].length() >= 10 ? r[0].substring(0, 10) : r[0];
            addCard(r[1].trim(), r[2].trim(), date, -1);
            i++;
        }
    }

    private void addCard(String who, String msg, String when, int index) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(12), dp(16), dp(12));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#59000000"));
        bg.setStroke(dp(1), Color.parseColor("#66FF3B3B"));
        bg.setCornerRadius(dp(16));
        card.setBackground(bg);

        TextView n = label(who, 16, true, "#FFFFFF");
        TextView m = label(msg, 15, false, "#F2FFFFFF");
        TextView d = label(when, 12, false, "#99FFFFFF");
        card.addView(n);
        card.addView(gap(4));
        card.addView(m);
        card.addView(gap(6));
        card.addView(d);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(10);
        if (index >= 0) wall.addView(card, index, lp); else wall.addView(card, lp);
        card.setAlpha(0f);
        card.animate().alpha(1f).setDuration(400).start();
    }

    /** Minimal CSV parser: handles quotes, escaped quotes and newlines inside fields. */
    static List<String[]> parseCsv(String s) {
        List<String[]> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder f = new StringBuilder();
        boolean q = false;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (q) {
                if (ch == '"') {
                    if (i + 1 < s.length() && s.charAt(i + 1) == '"') { f.append('"'); i++; }
                    else q = false;
                } else f.append(ch);
            } else if (ch == '"') q = true;
            else if (ch == ',') { row.add(f.toString()); f.setLength(0); }
            else if (ch == '\n') { row.add(f.toString()); f.setLength(0); rows.add(row.toArray(new String[0])); row = new ArrayList<>(); }
            else if (ch != '\r') f.append(ch);
        }
        if (f.length() > 0 || !row.isEmpty()) { row.add(f.toString()); rows.add(row.toArray(new String[0])); }
        return rows;
    }

    // ---------- helpers ----------

    private EditText input(String hint, boolean multi, int max) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setTextColor(Color.WHITE);
        e.setHintTextColor(Color.parseColor("#80FFFFFF"));
        e.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        e.setFilters(new InputFilter[]{new InputFilter.LengthFilter(max)});
        e.setPadding(dp(16), dp(12), dp(16), dp(12));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#66000000"));
        bg.setStroke(dp(1), Color.parseColor("#80FF3B3B"));
        bg.setCornerRadius(dp(14));
        e.setBackground(bg);
        if (multi) {
            e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                    | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            e.setMinLines(3);
            e.setGravity(Gravity.TOP | Gravity.START);
        } else {
            e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
            e.setSingleLine(true);
        }
        return e;
    }

    private TextView label(String s, int sp, boolean bold, String color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(Color.parseColor(color));
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private View gap(int h) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(h)));
        return v;
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }

    private SharedPreferences prefs() {
        return getSharedPreferences("app", MODE_PRIVATE);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
