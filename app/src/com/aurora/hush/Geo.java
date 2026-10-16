package com.aurora.hush;

import java.util.Locale;

public class Geo {
    public static double dist(double la1, double lo1, double la2, double lo2) {
        double r = 6371000d;
        double dLa = Math.toRadians(la2 - la1);
        double dLo = Math.toRadians(lo2 - lo1);
        double a = Math.sin(dLa / 2) * Math.sin(dLa / 2)
                + Math.cos(Math.toRadians(la1)) * Math.cos(Math.toRadians(la2))
                * Math.sin(dLo / 2) * Math.sin(dLo / 2);
        return 2 * r * Math.asin(Math.min(1d, Math.sqrt(a)));
    }

    public static String fmt(double m) {
        if (m < 0) return "—";
        if (m < 1000) return Math.round(m) + " m";
        return String.format(Locale.US, "%.2f km", m / 1000d);
    }
}
