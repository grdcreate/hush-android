package com.aurora.hush;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsActivity extends Activity {

    private Switch swBoot, swRestore;
    private TextView chipSilent, chipVibrate;
    private TextView stLoc, stDnd, stNotif;
    private boolean applying = false;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_settings);

        swBoot = findViewById(R.id.swBoot);
        swRestore = findViewById(R.id.swRestore);
        chipSilent = findViewById(R.id.chipSilent);
        chipVibrate = findViewById(R.id.chipVibrate);
        stLoc = findViewById(R.id.stLoc);
        stDnd = findViewById(R.id.stDnd);
        stNotif = findViewById(R.id.stNotif);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.rowLoc).setOnClickListener(v -> {
            try {
                startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        android.net.Uri.parse("package:" + getPackageName())));
            } catch (Exception ignore) {
            }
        });
        findViewById(R.id.rowDnd).setOnClickListener(v -> {
            try {
                startActivity(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS));
            } catch (Exception ignore) {
            }
        });
        findViewById(R.id.rowNotif).setOnClickListener(v -> {
            try {
                startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName()));
            } catch (Exception ignore) {
            }
        });

        applying = true;
        swBoot.setChecked(Prefs.bootStart(this));
        swRestore.setChecked(Prefs.restoreOnExit(this));
        applying = false;

        swBoot.setOnCheckedChangeListener((btn, on) -> {
            if (!applying) Prefs.setBootStart(this, on);
        });
        swRestore.setOnCheckedChangeListener((btn, on) -> {
            if (!applying) Prefs.setRestoreOnExit(this, on);
        });

        chipSilent.setOnClickListener(v -> {
            Prefs.setVibrateMode(this, false);
            styleChips();
        });
        chipVibrate.setOnClickListener(v -> {
            Prefs.setVibrateMode(this, true);
            styleChips();
        });
        styleChips();

        findViewById(R.id.btnRestoreNow).setOnClickListener(v -> {
            Intent i = new Intent(this, SilentService.class).setAction(SilentService.ACTION_RESTORE);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(i);
            else startService(i);
            Toast.makeText(this, "Sound restored", Toast.LENGTH_SHORT).show();
        });
        findViewById(R.id.btnStop).setOnClickListener(v -> {
            Prefs.setMaster(this, false);
            try {
                stopService(new Intent(this, SilentService.class));
            } catch (Exception ignore) {
            }
            Toast.makeText(this, "Protection stopped", Toast.LENGTH_SHORT).show();
        });
    }

    private void styleChips() {
        boolean vib = Prefs.vibrateMode(this);
        chipStyle(chipSilent, !vib);
        chipStyle(chipVibrate, vib);
    }

    private void chipStyle(TextView tv, boolean active) {
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dip(14));
        if (active) {
            g.setColor(0xFFFF6A3D);
            tv.setTextColor(0xFF14151A);
        } else {
            g.setColor(0xFF1D2026);
            tv.setTextColor(0xFF8A8F98);
        }
        tv.setBackground(g);
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean loc = checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        android.app.NotificationManager nm = getSystemService(android.app.NotificationManager.class);
        boolean dnd = nm != null && nm.isNotificationPolicyAccessGranted();
        boolean notif = Build.VERSION.SDK_INT < 33 || checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        stLoc.setText(loc ? R.string.granted : R.string.missing);
        stLoc.setTextColor(loc ? 0xFF2EE6A8 : 0xFFFF6A3D);
        stDnd.setText(dnd ? R.string.granted : R.string.missing);
        stDnd.setTextColor(dnd ? 0xFF2EE6A8 : 0xFFFF6A3D);
        stNotif.setText(notif ? R.string.granted : R.string.missing);
        stNotif.setTextColor(notif ? 0xFF2EE6A8 : 0xFFFF6A3D);
    }

    private float dip(float v) {
        return v * getResources().getDisplayMetrics().density;
    }
}
