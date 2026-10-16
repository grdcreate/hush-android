import android.content.Context;
import android.content.SharedPreferences;

import com.aurora.hush.Geo;
import com.aurora.hush.Prefs;
import com.aurora.hush.Zone;
import com.aurora.hush.ZoneStore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestLogic {

    static int passed = 0, failed = 0;

    static void check(String name, boolean cond) {
        if (cond) {
            passed++;
            System.out.println("  PASS  " + name);
        } else {
            failed++;
            System.out.println("  FAIL  " + name);
        }
    }

    /** In-memory Android prefs stub. */
    static class MemPrefs implements SharedPreferences {
        final Map<String, Object> map = new HashMap<>();

        public String getString(String k, String d) {
            Object v = map.get(k);
            return v instanceof String ? (String) v : d;
        }

        public boolean getBoolean(String k, boolean d) {
            Object v = map.get(k);
            return v instanceof Boolean ? (Boolean) v : d;
        }

        public Editor edit() {
            return new Editor() {
                public Editor putString(String k, String v) {
                    map.put(k, v);
                    return this;
                }

                public Editor putBoolean(String k, boolean v) {
                    map.put(k, v);
                    return this;
                }

                public void apply() {
                }

                public boolean commit() {
                    return true;
                }
            };
        }
    }

    static class MemContext extends Context {
        final Map<String, MemPrefs> store = new HashMap<>();

        public SharedPreferences getSharedPreferences(String name, int mode) {
            MemPrefs p = store.get(name);
            if (p == null) {
                p = new MemPrefs();
                store.put(name, p);
            }
            return p;
        }
    }

    public static void main(String[] args) {
        System.out.println("== Geo tests ==");
        double d0 = Geo.dist(26.1445, 91.7362, 26.1445, 91.7362);
        check("same point distance = 0", Math.abs(d0) < 0.001);
        double d1 = Geo.dist(26.1445, 91.7362, 26.1445, 91.7462);
        check("~1 km east (" + Math.round(d1) + " m)", d1 > 950 && d1 < 1050);
        double d2 = Geo.dist(26.1445, 91.7362, 26.1535, 91.7362);
        check("~1 km north (" + Math.round(d2) + " m)", d2 > 950 && d2 < 1050);
        double d3 = Geo.dist(-90, 0, 90, 0);
        check("pole-to-pole ~20015 km", d3 > 19900000 && d3 < 20100000);
        check("fmt 999 m", Geo.fmt(999).equals("999 m"));
        check("fmt 1.50 km", Geo.fmt(1500).equals("1.50 km"));
        check("fmt negative", Geo.fmt(-5).equals("—"));

        System.out.println("== Zone JSON round-trip ==");
        Zone z = new Zone(123L, "Home", 26.1445, 91.7362, 250, 2);
        Zone back = Zone.fromJson(z.toJson());
        check("id", back.id == 123L);
        check("name", back.name.equals("Home"));
        check("lat/lng", Math.abs(back.lat - 26.1445) < 1e-9 && Math.abs(back.lng - 91.7362) < 1e-9);
        check("radius", back.radius == 250);
        check("colorIdx", back.colorIdx == 2);
        check("enabled default true", back.enabled);
        check("palette color", z.color() == Zone.PALETTE[2]);

        System.out.println("== ZoneStore CRUD ==");
        MemContext ctx = new MemContext();
        check("empty list", ZoneStore.list(ctx).isEmpty());
        ZoneStore.add(ctx, new Zone(1L, "Home", 26.1, 91.7, 150, 0));
        ZoneStore.add(ctx, new Zone(2L, "Office", 26.2, 91.8, 300, 1));
        List<Zone> l = ZoneStore.list(ctx);
        check("two zones", l.size() == 2);
        ZoneStore.setEnabled(ctx, 2L, false);
        check("disable office", !ZoneStore.list(ctx).get(1).enabled);
        ZoneStore.remove(ctx, 1L);
        l = ZoneStore.list(ctx);
        check("one left & it is Office", l.size() == 1 && l.get(0).name.equals("Office"));
        // corrupt-json resilience
        ctx.getSharedPreferences("hush_zones", 0).edit().putString("zones_json", "{broken").apply();
        check("corrupt json -> empty list, no crash", ZoneStore.list(ctx).isEmpty());

        System.out.println("== Prefs defaults & toggles ==");
        MemContext c2 = new MemContext();
        check("master default true", Prefs.master(c2));
        check("restore default true", Prefs.restoreOnExit(c2));
        check("boot default true", Prefs.bootStart(c2));
        check("vibrate default false", !Prefs.vibrateMode(c2));
        Prefs.setMaster(c2, false);
        Prefs.setVibrateMode(c2, true);
        Prefs.setRestoreOnExit(c2, false);
        Prefs.setBootStart(c2, false);
        check("master toggled", !Prefs.master(c2));
        check("vibrate toggled", Prefs.vibrateMode(c2));
        check("restore toggled", !Prefs.restoreOnExit(c2));
        check("boot toggled", !Prefs.bootStart(c2));

        System.out.println();
        System.out.println(passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }
}
