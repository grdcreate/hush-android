package com.aurora.hush;

/** Lightweight in-memory mirror of the service state, for the UI. */
public class StateHub {
    public static String nearestName = null;
    public static double nearestDist = -1;
    public static int nearestRadius = 0;
    public static int nearestColor = 0xFF2EE6A8;
    public static float proximity = 0f;
    public static boolean serviceRunning = false;
}
