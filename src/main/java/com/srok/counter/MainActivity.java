package com.srok.counter;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.PorterDuff;
import android.graphics.Shader;
import android.graphics.SurfaceTexture;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends Activity {

    private static final long WELCOME_MS = 5000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<View> fadeViews = new ArrayList<>();
    private final Random rnd = new Random();

    private TextView since, days, daysLabel, clock, ymd, totals;
    private Button musicBtn;
    private TextureView videoView;
    private MediaPlayer videoPlayer;
    private int vidW, vidH;
    private boolean countingUp = false;
    private boolean musicOn = true;
    private boolean firstShow = true;
    private FrameLayout wordsLayer;
    private boolean wordsRunning = false;

    private static final String[] WORDS = {
            "Lost", "Cry", "Alone", "Empty", "Evil", "Emotional", "Way", "Close", "Die",
            "Pain", "Disappointment", "Stiffness", "Hatred", "Anger", "Humiliation",
            "Despondency", "Oppression", "Decay", "Abomination", "Greed", "Poverty", "Arrogance"
    };

    /** Слова в ASCII-кодах. */
    private static final String[] CODES = {
            "109 111 110 101 121", "112 111 119 101 114", "112 101 97 99 101",
            "102 114 101 101 100 111 109", "104 97 112 112 105 110 101 115 115",
            "69 109 109 97", "79 108 121 97", "108 111 118 101"
    };

    /** Что нового — меняйте этот текст при каждом обновлении. */
    private static final String WHATS_NEW =
            "• Музыка больше не прерывается\n"
            + "  на экране «Поддержать»";


    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            render(true);
            handler.postDelayed(this, 1000 - (System.currentTimeMillis() % 1000));
        }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        musicOn = Music.isOn(this);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        // 1. Video background
        videoView = new TextureView(this);
        videoView.setSurfaceTextureListener(videoListener);
        root.addView(videoView, match());

        // 2. Breathing red light
        View glow = new View(this);
        glow.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.parseColor("#B3C8102E"), Color.parseColor("#664A0610"), Color.parseColor("#33000000")}));
        root.addView(glow, match());
        ObjectAnimator breath = ObjectAnimator.ofFloat(glow, View.ALPHA, 0.35f, 0.85f);
        breath.setDuration(3500);
        breath.setRepeatMode(ValueAnimator.REVERSE);
        breath.setRepeatCount(ValueAnimator.INFINITE);
        breath.start();

        // 3. Words from the dark
        wordsLayer = new FrameLayout(this);
        root.addView(wordsLayer, match());

        // 4. Content
        root.addView(buildContent(), match());

        // 4. WELCOME TO HELL
        root.addView(buildWelcome(), match());

        setContentView(root);
    }

    private View buildContent() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = dp(24);
        box.setPadding(pad, dp(36), pad, pad);

        ImageView diamond = new ImageView(this);
        diamond.setImageResource(R.drawable.diamond);
        diamond.setColorFilter(Color.parseColor("#E8102E"), PorterDuff.Mode.SRC_IN);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(dp(90), dp(90));
        dlp.gravity = Gravity.CENTER_HORIZONTAL;
        diamond.setLayoutParams(dlp);
        ObjectAnimator spin = ObjectAnimator.ofFloat(diamond, View.ROTATION_Y, 0f, 360f);
        spin.setDuration(8000);
        spin.setInterpolator(new LinearInterpolator());
        spin.setRepeatCount(ValueAnimator.INFINITE);
        spin.start();
        ObjectAnimator shimmer = ObjectAnimator.ofFloat(diamond, View.ALPHA, 0.55f, 1f);
        shimmer.setDuration(1600);
        shimmer.setRepeatMode(ValueAnimator.REVERSE);
        shimmer.setRepeatCount(ValueAnimator.INFINITE);
        shimmer.start();

        TextView title = text("Sapphire in CZoo", 24, "#FFFFFF", true);
        TextView sub = text("УЖЕ ПРОШЛО", 14, "#E6FFFFFF", false);
        since = text("", 15, "#D9FFFFFF", false);
        days = text("—", 96, "#FFFFFF", true);
        daysLabel = text("", 20, "#E6FFFFFF", false);
        clock = text("", 48, "#FFFFFF", true);
        TextView clockHint = text("часы : минуты : секунды", 13, "#B3FFFFFF", false);
        ymd = text("", 20, "#FFFFFF", false);
        totals = text("", 15, "#D9FFFFFF", false);

        TextView version = text("версия " + versionName(), 12, "#80FFFFFF", false);

        Button pick = button("Указать дату задержания");
        pick.setOnClickListener(x -> pickDate());
        musicBtn = button("");
        updateMusicBtn();
        musicBtn.setOnClickListener(x -> toggleMusic());
        Button support = button("❤️  Поддержать Сапфира");
        support.setOnClickListener(x -> startActivity(new android.content.Intent(this, SupportActivity.class)));

        View[] items = {diamond, title, sub, since, space(20), days, daysLabel, space(12),
                clock, clockHint, space(20), ymd, space(8), totals, space(28), support, space(10), pick, space(10), musicBtn, space(18), version};
        for (View v : items) {
            box.addView(v);
            fadeViews.add(v);
            v.setAlpha(0f);
        }

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        FrameLayout center = new FrameLayout(this);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        center.addView(box, lp);
        scroll.addView(center, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return scroll;
    }

    private View buildWelcome() {
        final FrameLayout overlay = new FrameLayout(this);
        overlay.setBackgroundColor(Color.parseColor("#E6000000"));
        overlay.setClickable(true);

        final TextView hell = new TextView(this);
        hell.setText("WELCOME\nTO HELL");
        hell.setGravity(Gravity.CENTER);
        hell.setTextSize(TypedValue.COMPLEX_UNIT_SP, 72);
        hell.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD));
        hell.setLetterSpacing(0.12f);
        hell.setLineSpacing(0, 0.9f);
        hell.setShadowLayer(40, 0, 0, Color.parseColor("#FF2A00"));
        hell.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            if (b - t > 0) {
                hell.getPaint().setShader(new LinearGradient(0, 0, 0, b - t,
                        new int[]{Color.parseColor("#FFE066"), Color.parseColor("#FF6A00"),
                                Color.parseColor("#D10000"), Color.parseColor("#5A0000")},
                        new float[]{0f, 0.35f, 0.7f, 1f}, Shader.TileMode.CLAMP));
                hell.invalidate();
            }
        });
        overlay.addView(hell, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));

        // Slam in from huge
        hell.setScaleX(3f);
        hell.setScaleY(3f);
        hell.setAlpha(0f);
        hell.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(800)
                .setInterpolator(new OvershootInterpolator(1.4f)).start();

        // Demonic shake + flicker
        final ValueAnimator shake = ValueAnimator.ofFloat(0f, 1f);
        shake.setDuration(3400);
        shake.setStartDelay(800);
        shake.addUpdateListener(a -> {
            hell.setTranslationX((rnd.nextFloat() - 0.5f) * dp(6));
            hell.setTranslationY((rnd.nextFloat() - 0.5f) * dp(6));
            hell.setAlpha(rnd.nextFloat() < 0.07f ? 0.35f : 1f);
            float s = 1f + 0.04f * (float) Math.sin(a.getAnimatedFraction() * Math.PI * 8);
            hell.setScaleX(s);
            hell.setScaleY(s);
        });
        shake.start();

        // Burn out
        final Runnable hide = () -> {
            shake.cancel();
            hell.animate().scaleX(1.4f).scaleY(1.4f).alpha(0f).setDuration(700)
                    .setInterpolator(new AccelerateInterpolator()).start();
            overlay.animate().alpha(0f).setDuration(800).withEndAction(() -> {
                overlay.setVisibility(View.GONE);
            }).start();
            revealContent();
        };
        handler.postDelayed(hide, WELCOME_MS - 800);
        overlay.setOnClickListener(v -> {
            handler.removeCallbacks(hide);
            hide.run();
        });
        return overlay;
    }

    private boolean revealed = false;

    private void revealContent() {
        if (revealed) return;
        revealed = true;
        for (int i = 0; i < fadeViews.size(); i++) {
            View v = fadeViews.get(i);
            v.setTranslationY(dp(24));
            v.animate().alpha(1f).translationY(0).setStartDelay(i * 60L).setDuration(600)
                    .setInterpolator(new DecelerateInterpolator()).start();
        }
        countUp();
        startWords();
        handler.postDelayed(this::showWhatsNew, 1500);
    }

    // ---------- words from the black hole ----------

    private final Runnable spawnWord = new Runnable() {
        @Override
        public void run() {
            if (!wordsRunning) return;
            spawnWordOnce();
            handler.postDelayed(this, 1400 + rnd.nextInt(1600));
        }
    };

    private void startWords() {
        if (wordsRunning || !revealed) return;
        wordsRunning = true;
        handler.postDelayed(spawnWord, 600);
    }

    private void stopWords() {
        wordsRunning = false;
        handler.removeCallbacks(spawnWord);
    }

    private void spawnWordOnce() {
        final TextView w = new TextView(this);
        boolean code = rnd.nextInt(4) == 0;              // ~1 из 4 — ASCII-код
        if (code) {
            w.setText(CODES[rnd.nextInt(CODES.length)]);
            w.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
            w.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
            w.setLetterSpacing(0.05f);
        } else {
            w.setText(WORDS[rnd.nextInt(WORDS.length)]);
            w.setTextSize(TypedValue.COMPLEX_UNIT_SP, 34);
            w.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD));
            w.setLetterSpacing(0.15f);
        }
        w.setTextColor(rnd.nextInt(3) == 0 ? Color.parseColor("#FF4A4A") : Color.WHITE);
        w.setShadowLayer(24, 0, 0, Color.parseColor("#C8102E"));
        wordsLayer.addView(w, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));

        final double angle = rnd.nextDouble() * Math.PI * 2;
        final float startDist = dp(10 + rnd.nextInt(40));
        final float endDist = dp(160 + rnd.nextInt(220));
        final float cos = (float) Math.cos(angle), sin = (float) Math.sin(angle);
        final float peak = 0.18f + rnd.nextFloat() * 0.14f;
        final float endScale = 2.2f + rnd.nextFloat() * 1.6f;

        ValueAnimator a = ValueAnimator.ofFloat(0f, 1f);
        a.setDuration(4200 + rnd.nextInt(1800));
        a.setInterpolator(new AccelerateInterpolator(1.6f));
        a.addUpdateListener(v -> {
            float f = (float) v.getAnimatedValue();
            float d = startDist + (endDist - startDist) * f;
            w.setTranslationX(cos * d);
            w.setTranslationY(sin * d);
            float sc = 0.12f + (endScale - 0.12f) * f;
            w.setScaleX(sc);
            w.setScaleY(sc);
            float alpha = f < 0.35f ? peak * (f / 0.35f) : peak * (1f - (f - 0.35f) / 0.65f);
            w.setAlpha(Math.max(0f, alpha));
        });
        a.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                wordsLayer.removeView(w);
            }
        });
        a.start();
    }

    // ---------- what's new ----------

    private void showWhatsNew() {
        if (isFinishing()) return;
        long current = versionCode();
        long seen = prefs().getLong("seen_version", 0);
        if (seen >= current) return;
        prefs().edit().putLong("seen_version", current).apply();
        new android.app.AlertDialog.Builder(this)
                .setTitle("🔥 Что нового — версия " + versionName())
                .setMessage(WHATS_NEW)
                .setPositiveButton("Круто", null)
                .show();
    }

    @SuppressWarnings("deprecation")
    private long versionCode() {
        try {
            android.content.pm.PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            return android.os.Build.VERSION.SDK_INT >= 28 ? pi.getLongVersionCode() : pi.versionCode;
        } catch (Exception e) {
            return 0;
        }
    }

    private String versionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "?";
        }
    }

    private void countUp() {
        long start = CounterWidget.getStart(this);
        long target = Math.max(0, System.currentTimeMillis() - start) / CounterWidget.DAY;
        countingUp = true;
        ValueAnimator va = ValueAnimator.ofFloat(0f, target);
        va.setDuration(2200);
        va.setInterpolator(new DecelerateInterpolator(2f));
        va.addUpdateListener(a -> {
            long d = (long) (float) (Float) a.getAnimatedValue();
            days.setText(String.format(Locale.getDefault(), "%,d", d));
            daysLabel.setText(CounterWidget.plural(d, "день", "дня", "дней"));
        });
        va.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                countingUp = false;
                render(false);
            }
        });
        va.start();
    }

    // ---------- video ----------

    private final TextureView.SurfaceTextureListener videoListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture st, int w, int h) {
            startVideo(st);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture st, int w, int h) {
            applyCrop();
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture st) {
            releaseVideo();
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture st) {}
    };

    private void startVideo(SurfaceTexture st) {
        releaseVideo();
        try {
            videoPlayer = new MediaPlayer();
            AssetFileDescriptor afd = getResources().openRawResourceFd(R.raw.bg_video);
            videoPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
            videoPlayer.setSurface(new Surface(st));
            videoPlayer.setLooping(true);
            videoPlayer.setVolume(0f, 0f);
            videoPlayer.setOnVideoSizeChangedListener((mp, w, h) -> {
                vidW = w;
                vidH = h;
                applyCrop();
            });
            videoPlayer.setOnPreparedListener(MediaPlayer::start);
            videoPlayer.prepareAsync();
        } catch (Exception e) {
            releaseVideo();
        }
    }

    private void applyCrop() {
        int vw = videoView.getWidth(), vh = videoView.getHeight();
        if (vw == 0 || vh == 0 || vidW == 0 || vidH == 0) return;
        float scale = Math.max((float) vw / vidW, (float) vh / vidH);
        Matrix m = new Matrix();
        m.setScale(vidW * scale / vw, vidH * scale / vh, vw / 2f, vh / 2f);
        videoView.setTransform(m);
    }

    private void releaseVideo() {
        if (videoPlayer != null) {
            try { videoPlayer.release(); } catch (Exception ignored) {}
            videoPlayer = null;
        }
    }

    // ---------- music ----------

    private void toggleMusic() {
        musicOn = !musicOn;
        Music.setOn(this, musicOn);
        updateMusicBtn();
    }

    private void updateMusicBtn() {
        musicBtn.setText(musicOn ? "🔊  Музыка: вкл" : "🔇  Музыка: выкл");
    }

    private SharedPreferences prefs() {
        return getSharedPreferences("app", MODE_PRIVATE);
    }

    // ---------- lifecycle ----------

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

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(tick);
        try { if (videoPlayer != null) videoPlayer.start(); } catch (Exception ignored) {}
        if (!firstShow && !revealed) revealContent();
        firstShow = false;
        startWords();
        UpdateChecker.check(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(tick);
        stopWords();
        try { if (videoPlayer != null && videoPlayer.isPlaying()) videoPlayer.pause(); } catch (Exception ignored) {}
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        releaseVideo();
    }

    // ---------- counter ----------

    private void render(boolean pulse) {
        long start = CounterWidget.getStart(this);
        long now = System.currentTimeMillis();
        long diff = Math.max(0, now - start) / 1000;
        long d = diff / 86400;
        long h = diff % 86400 / 3600;
        long m = diff % 3600 / 60;
        long s = diff % 60;

        since.setText("с " + new SimpleDateFormat("d MMMM yyyy, HH:mm", new Locale("ru")).format(start));
        if (!countingUp && revealed) {
            days.setText(String.format(Locale.getDefault(), "%,d", d));
            daysLabel.setText(CounterWidget.plural(d, "день", "дня", "дней"));
        }
        clock.setText(String.format(Locale.US, "%02d:%02d:%02d", h, m, s));
        if (pulse && revealed) {
            clock.animate().scaleX(1.08f).scaleY(1.08f).setStartDelay(0).setDuration(120)
                    .withEndAction(() -> clock.animate().scaleX(1f).scaleY(1f).setDuration(300).start())
                    .start();
        }

        int[] p = CounterWidget.ymd(start, now);
        StringBuilder sb = new StringBuilder();
        if (p[0] > 0) sb.append(p[0]).append(' ').append(CounterWidget.plural(p[0], "год", "года", "лет")).append(", ");
        if (p[0] > 0 || p[1] > 0) sb.append(p[1]).append(' ').append(CounterWidget.plural(p[1], "месяц", "месяца", "месяцев")).append(", ");
        sb.append(p[2]).append(' ').append(CounterWidget.plural(p[2], "день", "дня", "дней"));
        long wk = d / 7;
        sb.append("\n").append(wk).append(' ').append(CounterWidget.plural(wk, "неделя", "недели", "недель"));
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
        c.setTimeInMillis(CounterWidget.getStart(this));
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
                render(false);
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show();
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        dd.getDatePicker().setMaxDate(System.currentTimeMillis());
        dd.show();
    }

    // ---------- helpers ----------

    private TextView text(String s, int sp, String color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(Color.parseColor(color));
        t.setGravity(Gravity.CENTER);
        t.setShadowLayer(12, 0, 2, Color.BLACK);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private Button button(String s) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#66000000"));
        bg.setStroke(dp(1), Color.parseColor("#99FF3B3B"));
        bg.setCornerRadius(dp(24));
        b.setBackground(bg);
        b.setPadding(dp(24), dp(10), dp(24), dp(10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER_HORIZONTAL;
        b.setLayoutParams(lp);
        return b;
    }

    private View space(int h) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(h)));
        return v;
    }

    private FrameLayout.LayoutParams match() {
        return new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
