package com.aurora.hush;

import org.json.JSONException;
import org.json.JSONObject;

public class Zone {
    public static final int[] PALETTE = {0xFFFF6A3D, 0xFF2EE6A8, 0xFFE8F14D, 0xFF7AB8FF, 0xFFFF7AB8};

    public long id;
    public String name;
    public double lat;
    public double lng;
    public int radius;
    public int colorIdx;
    public boolean enabled;

    public Zone() {
    }

    public Zone(long id, String name, double lat, double lng, int radius, int colorIdx) {
        this.id = id;
        this.name = name;
        this.lat = lat;
        this.lng = lng;
        this.radius = radius;
        this.colorIdx = colorIdx;
        this.enabled = true;
    }

    public int color() {
        return PALETTE[((colorIdx % PALETTE.length) + PALETTE.length) % PALETTE.length];
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("name", name);
        o.put("lat", lat);
        o.put("lng", lng);
        o.put("radius", radius);
        o.put("colorIdx", colorIdx);
        o.put("enabled", enabled);
        return o;
    }

    public static Zone fromJson(JSONObject o) {
        Zone z = new Zone();
        z.id = o.optLong("id", System.currentTimeMillis());
        z.name = o.optString("name", "Zone");
        z.lat = o.optDouble("lat", 0d);
        z.lng = o.optDouble("lng", 0d);
        z.radius = o.optInt("radius", 150);
        z.colorIdx = o.optInt("colorIdx", 0);
        z.enabled = o.optBoolean("enabled", true);
        return z;
    }
}
