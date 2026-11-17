package com.srok.counter;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.widget.RemoteViews;

import java.util.Calendar;

public class CounterWidget extends AppWidgetProvider {

    private static final String ACTION_TICK = "com.srok.counter.DAY_TICK";

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        updateAll(context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String a = intent.getAction();
        if (ACTION_TICK.equals(a)
                || Intent.ACTION_TIME_CHANGED.equals(a)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(a)) {
            updateAll(context);
        }
    }

    @Override
    public void onDisabled(Context context) {
        AlarmManager am = context.getSystemService(AlarmManager.class);
        if (am != null) am.cancel(tickIntent(context));
    }

    static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, CounterWidget.class));
        if (ids == null || ids.length == 0) return;

        RemoteViews v = new RemoteViews(context.getPackageName(), R.layout.widget);

        Intent open = new Intent(context, MainActivity.class);
        PendingIntent openPi = PendingIntent.getActivity(context, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        v.setOnClickPendingIntent(R.id.root, openPi);

        long start = CounterWidget.getStart(context);
        long now = System.currentTimeMillis();

        if (start < 0) {
            v.setTextViewText(R.id.days, "—");
            v.setTextViewText(R.id.days_label, "");
            v.setChronometer(R.id.clock, SystemClock.elapsedRealtime(), null, false);
            v.setTextViewText(R.id.hint, "Нажмите, чтобы указать дату");
        } else {
            long diff = Math.max(0, now - start);
            long days = diff / CounterWidget.DAY;
            long rest = diff % CounterWidget.DAY;
            v.setTextViewText(R.id.days, String.valueOf(days));
            v.setTextViewText(R.id.days_label, CounterWidget.plural(days, "день", "дня", "дней"));
            v.setChronometer(R.id.clock, SystemClock.elapsedRealtime() - rest, null, true);
            v.setTextViewText(R.id.hint, "часы : минуты : секунды");
            scheduleNextDay(context, start + (days + 1) * CounterWidget.DAY);
        }

        manager.updateAppWidget(ids, v);
    }

    private static void scheduleNextDay(Context context, long at) {
        AlarmManager am = context.getSystemService(AlarmManager.class);
        if (am == null) return;
        am.setWindow(AlarmManager.RTC, at, 60_000L, tickIntent(context));
    }

    private static PendingIntent tickIntent(Context context) {
        Intent i = new Intent(context, CounterWidget.class).setAction(ACTION_TICK);
        return PendingIntent.getBroadcast(context, 1, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    // ---- helpers ----

    static final long DAY = 24L * 60 * 60 * 1000;
    private static final String PREFS = "counter";
    private static final String KEY_START = "start";

    static long getStart(Context c) {
        return prefs(c).getLong(KEY_START, -1);
    }

    static void setStart(Context c, long millis) {
        prefs(c).edit().putLong(KEY_START, millis).apply();
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static String plural(long n, String one, String few, String many) {
        long n100 = Math.abs(n) % 100;
        long n10 = n100 % 10;
        if (n100 > 10 && n100 < 20) return many;
        if (n10 > 1 && n10 < 5) return few;
        if (n10 == 1) return one;
        return many;
    }

    /** Years, months, days between start and now. */
    static int[] ymd(long start, long now) {
        Calendar a = Calendar.getInstance();
        a.setTimeInMillis(start);
        Calendar b = Calendar.getInstance();
        b.setTimeInMillis(now);
        int y = b.get(Calendar.YEAR) - a.get(Calendar.YEAR);
        int m = b.get(Calendar.MONTH) - a.get(Calendar.MONTH);
        int d = b.get(Calendar.DAY_OF_MONTH) - a.get(Calendar.DAY_OF_MONTH);
        int secA = a.get(Calendar.HOUR_OF_DAY) * 3600 + a.get(Calendar.MINUTE) * 60 + a.get(Calendar.SECOND);
        int secB = b.get(Calendar.HOUR_OF_DAY) * 3600 + b.get(Calendar.MINUTE) * 60 + b.get(Calendar.SECOND);
        if (secB < secA) d--;
        if (d < 0) {
            m--;
            Calendar prev = (Calendar) b.clone();
            prev.add(Calendar.MONTH, -1);
            d += prev.getActualMaximum(Calendar.DAY_OF_MONTH);
        }
        if (m < 0) {
            y--;
            m += 12;
        }
        return new int[]{y, m, d};
    }
}
