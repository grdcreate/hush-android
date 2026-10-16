package com.aurora.hush;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class AddZoneActivity extends Activity {

    private static final int REQ_MAP = 42;

    private EditText etName;
    private SeekBar sbRadius;
    private TextView txtRadius, txtLoc;
    private View btnCurrent, btnMap, btnSave;

    private double lat, lng;
    private boolean hasLoc = false;

    private LocationManager lm;
    private AlertDialog locating;
    private final Handler h = new Handler();
    private LocationListener single;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_add_zone);

        etName = findViewById(R.id.etName);
        sbRadius = findViewById(R.id.sbRadius);
        txtRadius = findViewById(R.id.txtRadius);
        txtLoc = findViewById(R.id.txtLoc);
        btnCurrent = findViewById(R.id.btnCurrent);
        btnMap = findViewById(R.id.btnMap);
        btnSave = findViewById(R.id.btnSave);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        sbRadius.setMax(60);
        sbRadius.setProgress(4);
        updateRadius();
        sbRadius.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                updateRadius();
            }

            @Override
            public void onStartTrackingTouch(SeekBar s) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar s) {
            }
        });

        btnCurrent.setOnClickListener(v -> locate());
        btnMap.setOnClickListener(v -> startActivityForResult(new Intent(this, MapPickActivity.class), REQ_MAP));
        btnSave.setOnClickListener(v -> save());
    }

    private int radius() {
        return 50 + sbRadius.getProgress() * 25;
    }

    private void updateRadius() {
        txtRadius.setText(Geo.fmt(radius()));
    }

    private void setLoc(double la, double lo) {
        lat = la;
        lng = lo;
        hasLoc = true;
        txtLoc.setText(String.format(Locale.US, "%.5f, %.5f", la, lo));
        txtLoc.setTextColor(0xFF2EE6A8);
    }

    private void locate() {
        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Location best = null;
        try {
            for (String p : lm.getAllProviders()) {
                Location l = lm.getLastKnownLocation(p);
                if (l == null) continue;
                if (best == null || l.getTime() > best.getTime()) best = l;
            }
        } catch (SecurityException ignore) {
        }
        if (best != null && System.currentTimeMillis() - best.getTime() < 10 * 60 * 1000) {
            setLoc(best.getLatitude(), best.getLongitude());
            return;
        }
        startLiveLocate();
    }

    private void startLiveLocate() {
        AlertDialog.Builder bld = new AlertDialog.Builder(this);
        bld.setTitle(R.string.locating);
        bld.setMessage("Waiting for a GPS / network fix…");
        bld.setNegativeButton("Cancel", (d, w) -> stopLiveLocate());
        locating = bld.show();

        single = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (location != null) {
                    setLoc(location.getLatitude(), location.getLongitude());
                    stopLiveLocate();
                }
            }

            @Override
            public void onStatusChanged(String p, int s, Bundle x) {
            }

            @Override
            public void onProviderEnabled(String p) {
            }

            @Override
            public void onProviderDisabled(String p) {
            }
        };
        try {
            if (lm.getAllProviders().contains(LocationManager.GPS_PROVIDER))
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, single);
            if (lm.getAllProviders().contains(LocationManager.NETWORK_PROVIDER))
                lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, single);
        } catch (SecurityException e) {
            stopLiveLocate();
            Toast.makeText(this, "Location permission missing", Toast.LENGTH_SHORT).show();
            return;
        }
        h.postDelayed(() -> {
            if (locating != null) {
                stopLiveLocate();
                Toast.makeText(this, R.string.no_fix, Toast.LENGTH_LONG).show();
            }
        }, 20000);
    }

    private void stopLiveLocate() {
        if (locating != null) {
            locating.dismiss();
            locating = null;
        }
        if (lm != null && single != null) {
            try {
                lm.removeUpdates(single);
            } catch (Exception ignore) {
            }
        }
        single = null;
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == REQ_MAP && res == RESULT_OK && data != null) {
            setLoc(data.getDoubleExtra("lat", 0), data.getDoubleExtra("lng", 0));
        }
    }

    private void save() {
        String name = etName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, R.string.need_name, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!hasLoc) {
            Toast.makeText(this, R.string.need_loc, Toast.LENGTH_SHORT).show();
            return;
        }
        int idx = ZoneStore.list(this).size() % Zone.PALETTE.length;
        Zone z = new Zone(System.currentTimeMillis(), name, lat, lng, radius(), idx);
        ZoneStore.add(this, z);
        Prefs.setMaster(this, true);
        if (SilentService.hasLocationPermission(this)) {
            try {
                Intent i = new Intent(this, SilentService.class);
                if (Build.VERSION.SDK_INT >= 26) startForegroundService(i);
                else startService(i);
            } catch (Exception ignore) {
            }
        }
        Toast.makeText(this, R.string.zone_saved, Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    protected void onDestroy() {
        stopLiveLocate();
        h.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
