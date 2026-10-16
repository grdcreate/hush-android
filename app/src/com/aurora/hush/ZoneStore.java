package com.aurora.hush;

import android.content.Context;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

public class ZoneStore {
    private static final String PREFS = "hush_zones";
    private static final String KEY = "zones_json";

    public static List<Zone> list(Context c) {
        List<Zone> out = new ArrayList<>();
        String s = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null);
        if (s != null) {
            try {
                JSONArray a = new JSONArray(s);
                for (int i = 0; i < a.length(); i++) out.add(Zone.fromJson(a.getJSONObject(i)));
            } catch (Exception ignore) {
            }
        }
        return out;
    }

    public static void save(Context c, List<Zone> zones) {
        JSONArray a = new JSONArray();
        for (Zone z : zones) {
            try {
                a.put(z.toJson());
            } catch (Exception ignore) {
            }
        }
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, a.toString()).apply();
    }

    public static void add(Context c, Zone z) {
        List<Zone> l = list(c);
        l.add(z);
        save(c, l);
    }

    public static void remove(Context c, long id) {
        List<Zone> l = list(c);
        for (int i = l.size() - 1; i >= 0; i--) {
            if (l.get(i).id == id) l.remove(i);
        }
        save(c, l);
    }

    public static void setEnabled(Context c, long id, boolean enabled) {
        List<Zone> l = list(c);
        for (Zone z : l) {
            if (z.id == id) z.enabled = enabled;
        }
        save(c, l);
    }
}
