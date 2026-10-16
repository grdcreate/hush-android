package com.aurora.hush;

import android.content.Context;
import android.content.SharedPreferences;

public class Prefs {
    private static final String P = "hush_prefs";

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(P, Context.MODE_PRIVATE);
    }

    public static boolean master(Context c) {
        return sp(c).getBoolean("master", true);
    }

    public static void setMaster(Context c, boolean v) {
        sp(c).edit().putBoolean("master", v).apply();
    }

    public static boolean vibrateMode(Context c) {
        return sp(c).getBoolean("vibrate", false);
    }

    public static void setVibrateMode(Context c, boolean v) {
        sp(c).edit().putBoolean("vibrate", v).apply();
    }

    public static boolean restoreOnExit(Context c) {
        return sp(c).getBoolean("restore", true);
    }

    public static void setRestoreOnExit(Context c, boolean v) {
        sp(c).edit().putBoolean("restore", v).apply();
    }

    public static boolean bootStart(Context c) {
        return sp(c).getBoolean("boot", true);
    }

    public static void setBootStart(Context c, boolean v) {
        sp(c).edit().putBoolean("boot", v).apply();
    }
}
