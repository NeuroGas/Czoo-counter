package com.srok.counter;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends Activity {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView since, days, daysLabel, clock, ymd, totals;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            render();
            handler.postDelayed(this, 1000 - (System.currentTimeMillis() % 1000));
        }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = dp(24);
        box.setPadding(pad, dp(48), pad, pad);
        box.setBackgroundColor(Color.parseColor("#0F1115"));

        TextView title = text("УЖЕ ПРОШЛО", 15, "#8B93A3", false);
        since = text("", 15, "#8B93A3", false);
        days = text("—", 96, "#EEF0F4", true);
        daysLabel = text("", 20, "#8B93A3", false);
        clock = text("", 48, "#6EA8FE", true);
        TextView clockHint = text("часы : минуты : секунды", 13, "#8B93A3", false);
        ymd = text("", 20, "#EEF0F4", false);
        totals = text("", 15, "#8B93A3", false);

        Button pick = new Button(this);
        pick.setText("Указать дату задержания");
        pick.setAllCaps(false);
        pick.setOnClickListener(x -> pickDate());

        box.addView(title);
        box.addView(since);
        box.addView(space(24));
        box.addView(days);
        box.addView(daysLabel);
        box.addView(space(16));
        box.addView(clock);
        box.addView(clockHint);
        box.addView(space(24));
        box.addView(ymd);
        box.addView(space(8));
        box.addView(totals);
        box.addView(space(32));
        box.addView(pick);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.parseColor("#0F1115"));
        scroll.addView(box);
        setContentView(scroll);

        if (CounterWidget.getStart(this) < 0) pickDate();
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(tick);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(tick);
    }

    private void render() {
        long start = CounterWidget.getStart(this);
        if (start < 0) {
            since.setText("Дата ещё не указана");
            return;
        }
        long now = System.currentTimeMillis();
        long diff = Math.max(0, now - start) / 1000;
        long d = diff / 86400;
        long h = diff % 86400 / 3600;
        long m = diff % 3600 / 60;
        long s = diff % 60;

        since.setText("с " + new SimpleDateFormat("d MMMM yyyy, HH:mm", new Locale("ru")).format(start));
        days.setText(String.format(Locale.getDefault(), "%,d", d));
        daysLabel.setText(CounterWidget.plural(d, "день", "дня", "дней"));
        clock.setText(String.format(Locale.US, "%02d:%02d:%02d", h, m, s));

        int[] p = CounterWidget.ymd(start, now);
        StringBuilder sb = new StringBuilder();
        if (p[0] > 0) sb.append(p[0]).append(' ').append(CounterWidget.plural(p[0], "год", "года", "лет")).append(", ");
        if (p[1] > 0) sb.append(p[1]).append(' ').append(CounterWidget.plural(p[1], "месяц", "месяца", "месяцев")).append(", ");
        sb.append(p[2]).append(' ').append(CounterWidget.plural(p[2], "день", "дня", "дней"));
        ymd.setText(sb.toString());

        long th = diff / 3600, tm = diff / 60;
        totals.setText(String.format(Locale.getDefault(),
                "Всего: %,d %s · %,d %s\n%,d %s",
                th, CounterWidget.plural(th, "час", "часа", "часов"),
                tm, CounterWidget.plural(tm, "минута", "минуты", "минут"),
                diff, CounterWidget.plural(diff, "секунда", "секунды", "секунд")));
    }

    private void pickDate() {
        final Calendar c = Calendar.getInstance();
        long start = CounterWidget.getStart(this);
        if (start >= 0) c.setTimeInMillis(start);
        DatePickerDialog dd = new DatePickerDialog(this, (view, y, mo, day) -> {
            c.set(Calendar.YEAR, y);
            c.set(Calendar.MONTH, mo);
            c.set(Calendar.DAY_OF_MONTH, day);
            new TimePickerDialog(this, (tv, hh, mm) -> {
                c.set(Calendar.HOUR_OF_DAY, hh);
                c.set(Calendar.MINUTE, mm);
                c.set(Calendar.SECOND, 0);
                c.set(Calendar.MILLISECOND, 0);
                CounterWidget.setStart(this, c.getTimeInMillis());
                CounterWidget.updateAll(this);
                render();
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show();
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        dd.getDatePicker().setMaxDate(System.currentTimeMillis());
        dd.show();
    }

    private TextView text(String s, int sp, String color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(Color.parseColor(color));
        t.setGravity(Gravity.CENTER);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private android.view.View space(int h) {
        android.view.View v = new android.view.View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(h)));
        return v;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
