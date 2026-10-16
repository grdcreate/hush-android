package com.aurora.hush;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private final Handler clockH = new Handler();
    private boolean applyingSwitch = false;
    private String lastZoneSig = "";

    private TextView txtClock, txtDate, txtArm, txtNearest, txtDist, txtCount, txtBattery;
    private RingView ringMain;
    private LinearLayout zoneContainer;
    private View cardPerm;
    private Switch swMaster;

    private final Runnable clockR = new Runnable() {
        @Override
        public void run() {
            try {
                tickClock();
            } catch (Exception ignore) {
            }
            clockH.postDelayed(this, 1000);
        }
    };

    private final BroadcastReceiver stateR = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshProtection();
            refreshZones();
        }
    };

    private final BroadcastReceiver battR = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int lvl = intent.getIntExtra("level", -1);
            if (lvl >= 0) txtBattery.setText(lvl + "%");
        }
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);

        txtClock = findViewById(R.id.txtClock);
        txtDate = findViewById(R.id.txtDate);
        txtArm = findViewById(R.id.txtArm);
        txtNearest = findViewById(R.id.txtNearest);
        txtDist = findViewById(R.id.txtDist);
        txtCount = findViewById(R.id.txtCount);
        txtBattery = findViewById(R.id.txtBattery);
        ringMain = findViewById(R.id.ringMain);
        zoneContainer = findViewById(R.id.zoneContainer);
        cardPerm = findViewById(R.id.cardPerm);
        swMaster = findViewById(R.id.swMaster);

        ringMain.setIcon(getDrawable(R.drawable.ic_shield));

        findViewById(R.id.btnSettings).setOnClickListener(v -> {
            try {
                startActivity(new Intent(this, SettingsActivity.class));
            } catch (Exception ignore) {
            }
        });
        findViewById(R.id.btnAdd).setOnClickListener(v -> {
            try {
                startActivity(new Intent(this, AddZoneActivity.class));
            } catch (Exception ignore) {
            }
        });
        cardPerm.setOnClickListener(v -> openDndSettings());

        swMaster.setOnCheckedChangeListener((btn, on) -> {
            if (applyingSwitch) return;
            Prefs.setMaster(this, on);
            if (on) {
                startSvc();
            } else {
                try {
                    stopService(new Intent(this, SilentService.class));
                } catch (Exception ignore) {
                }
            }
            refreshProtection();
        });

        tickClock();
    }

    private void tickClock() {
        Date now = new Date();
        txtClock.setText(new SimpleDateFormat("HH:mm:ss", Locale.US).format(now));
        txtDate.setText(new SimpleDateFormat("EEE dd MMM", Locale.US).format(now).toUpperCase(Locale.US));
    }

    private void openDndSettings() {
        try {
            startActivity(new Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS));
        } catch (Exception e) {
            try {
                startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
            } catch (Exception ignore) {
            }
        }
    }

    void startSvc() {
        // Android 14 crashes location-type foreground services started without
        // location permission, so never start it until permission is granted.
        if (!SilentService.hasLocationPermission(this)) return;
        if (!Prefs.master(this)) return;
        try {
            Intent i = new Intent(this, SilentService.class);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(i);
            else startService(i);
        } catch (Exception ignore) {
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // targetSdk 34 requires an export flag for non-system broadcasts.
        IntentFilter f = new IntentFilter(SilentService.ACTION_STATE);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(stateR, f, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(stateR, f);
        }
        registerReceiver(battR, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        clockH.removeCallbacks(clockR);
        clockH.post(clockR);
        ensurePermissions();
        startSvc();
        applyingSwitch = true;
        swMaster.setChecked(Prefs.master(this));
        applyingSwitch = false;
        refreshProtection();
        refreshZones();
        updatePermCard();
    }

    @Override
    protected void onPause() {
        super.onPause();
        clockH.removeCallbacks(clockR);
        try {
            unregisterReceiver(stateR);
        } catch (Exception ignore) {
        }
        try {
            unregisterReceiver(battR);
        } catch (Exception ignore) {
        }
    }

    private void ensurePermissions() {
        try {
            if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                }, 11);
                return;
            }
            if (Build.VERSION.SDK_INT >= 29 &&
                    checkSelfPermission(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.ACCESS_BACKGROUND_LOCATION}, 12);
            }
            if (Build.VERSION.SDK_INT >= 33 &&
                    checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 13);
            }
        } catch (Exception ignore) {
        }
    }

    private void updatePermCard() {
        try {
            android.app.NotificationManager nm = getSystemService(android.app.NotificationManager.class);
            boolean dnd = nm != null && nm.isNotificationPolicyAccessGranted();
            cardPerm.setVisibility(dnd ? View.GONE : View.VISIBLE);
        } catch (Exception ignore) {
        }
    }

    private void refreshProtection() {
        boolean silent = SilentService.silentActive;
        if (silent) {
            txtArm.setText(getString(R.string.silenced_at, SilentService.activeZoneName));
            txtArm.setTextColor(0xFFFF6A3D);
        } else if (Prefs.master(this)) {
            txtArm.setText(R.string.protection_armed);
            txtArm.setTextColor(0xFF2EE6A8);
        } else {
            txtArm.setText(R.string.protection_off);
            txtArm.setTextColor(0xFF8A8F98);
        }
        applyingSwitch = true;
        swMaster.setChecked(Prefs.master(this));
        applyingSwitch = false;

        if (StateHub.nearestName == null) {
            txtNearest.setText(R.string.no_zones);
            txtDist.setText(R.string.add_zone_hint);
            ringMain.setProgress(0f);
            ringMain.setColor(0xFF2EE6A8);
        } else {
            txtNearest.setText(StateHub.nearestName);
            txtDist.setText(StateHub.nearestDist < 0 ? "—"
                    : Geo.fmt(StateHub.nearestDist) + " away · radius " + Geo.fmt(StateHub.nearestRadius));
            ringMain.setProgress(SilentService.silentActive ? 1f : StateHub.proximity);
            ringMain.setColor(StateHub.nearestColor);
        }
    }

    private void refreshZones() {
        List<Zone> zones = ZoneStore.list(this);
        StringBuilder sig = new StringBuilder();
        for (Zone z : zones) {
            boolean inside = SilentService.silentActive && z.name != null
                    && z.name.equals(SilentService.activeZoneName);
            sig.append(z.id).append(z.enabled ? 1 : 0).append(inside ? 1 : 0);
        }
        sig.append('|').append(SilentService.silentActive ? 1 : 0)
                .append('|').append(Math.round(StateHub.proximity * 20));
        String s = sig.toString();
        if (s.equals(lastZoneSig) && zoneContainer.getChildCount() > 0) return;
        lastZoneSig = s;

        zoneContainer.removeAllViews();
        txtCount.setText(getString(R.string.silent_zones) + "  ·  " + zones.size());
        for (final Zone z : zones) {
            View row = getLayoutInflater().inflate(R.layout.item_zone, zoneContainer, false);
            RingView ring = row.findViewById(R.id.ring);
            TextView name = row.findViewById(R.id.txtName);
            TextView sub = row.findViewById(R.id.txtSub);
            TextView pill = row.findViewById(R.id.pill);
            ImageButton del = row.findViewById(R.id.btnDel);

            ring.setColor(z.color());
            boolean inside = SilentService.silentActive && z.name != null
                    && z.name.equals(SilentService.activeZoneName);
            ring.setProgress(inside ? 1f : (StateHub.nearestName != null && z.name.equals(StateHub.nearestName) ? StateHub.proximity : 0f));
            Drawable pin = getDrawable(R.drawable.ic_pin).mutate();
            pin.setTint(0xFFECEDEF);
            ring.setIcon(pin);

            name.setText(z.name);
            sub.setText(String.format(Locale.US, "%.5f, %.5f · radius %s", z.lat, z.lng, Geo.fmt(z.radius)));

            pill.setText(inside ? "SILENCED" : (z.enabled ? "ARMED" : "OFF"));
            int pillColor = inside ? 0xFFFF6A3D : (z.enabled ? 0xFF2EE6A8 : 0xFF8A8F98);
            GradientDrawable pg = new GradientDrawable();
            pg.setCornerRadius(dip(100));
            pg.setColor(withAlpha(pillColor, 0x26));
            pill.setBackground(pg);
            pill.setTextColor(pillColor);

            row.setOnClickListener(v -> {
                ZoneStore.setEnabled(this, z.id, !z.enabled);
                lastZoneSig = "";
                refreshZones();
            });
            del.setOnClickListener(v -> {
                ZoneStore.remove(this, z.id);
                lastZoneSig = "";
                refreshZones();
                refreshProtection();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = (int) dip(10);
            zoneContainer.addView(row, lp);
        }
        if (zones.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No zones yet — tap + to add your first silent zone.");
            empty.setTextColor(0xFF8A8F98);
            empty.setTextSize(13);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, (int) dip(28), 0, (int) dip(28));
            zoneContainer.addView(empty);
        }
    }

    private float dip(float v) {
        return v * getResources().getDisplayMetrics().density;
    }

    private static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (alpha << 24);
    }
}
