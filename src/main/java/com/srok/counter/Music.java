package com.srok.counter;

import android.content.Context;
import android.media.MediaPlayer;

/**
 * Background music shared by all screens of the app.
 * Keeps playing while moving between screens and pauses only when the app goes to the background.
 */
final class Music {

    private static MediaPlayer player;
    private static int visibleScreens = 0;

    private Music() {}

    static boolean isOn(Context c) {
        return c.getSharedPreferences("app", Context.MODE_PRIVATE).getBoolean("music_on", true);
    }

    static void setOn(Context c, boolean on) {
        c.getSharedPreferences("app", Context.MODE_PRIVATE).edit().putBoolean("music_on", on).apply();
        if (on) play(c); else pause();
    }

    /** Call from every screen's onStart(). */
    static void screenStarted(Context c) {
        visibleScreens++;
        play(c);
    }

    /** Call from every screen's onStop(). */
    static void screenStopped() {
        visibleScreens = Math.max(0, visibleScreens - 1);
        if (visibleScreens == 0) pause();
    }

    private static void play(Context c) {
        if (!isOn(c)) return;
        try {
            if (player == null) {
                player = MediaPlayer.create(c.getApplicationContext(), R.raw.music);
                if (player == null) return;
                player.setLooping(true);
            }
            if (!player.isPlaying()) player.start();
        } catch (Exception ignored) {}
    }

    private static void pause() {
        try {
            if (player != null && player.isPlaying()) player.pause();
        } catch (Exception ignored) {}
    }
}
