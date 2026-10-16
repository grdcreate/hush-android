package com.aurora.hush;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import java.util.List;

public class SilentService extends Service implements LocationListener {

    public static final String ACTION_STATE = "com.aurora.hush.STATE";
    public static final String ACTION_RESTORE = "com.aurora.hush.RESTORE";

    private static final int NOTIF_ID = 1001;
    private static final String CHANNEL = "hush_service";
    private static final int[] STREAMS = {
            AudioManager.STREAM_RING,
            AudioManager.STREAM_NOTIFICATION,
            AudioManager.STREAM_MUSIC,
            AudioManager.STREAM_SYSTEM,
            AudioManager.STREAM_DTMF
    };

    public static boolean silentActive = false;
    public static String activeZoneName = null;

    private LocationManager lm;
    private AudioManager am;
    private NotificationManager nm;
    private Handler h;
    private int savedRinger = -1;
    private int[] savedVolumes = null;
    private boolean updatesOn = false;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            try {
                fallbackEvaluate();
            } catch (Exception ignore) {
            }
            h.postDelayed(this, 20000);
        }
    };

    public static boolean hasLocationPermission(Context c) {
        return c.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || c.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            h = new Handler(Looper.getMainLooper());
            StateHub.serviceRunning = true;
        } catch (Exception e) {
            lm = null;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Android 14 throws SecurityException if a location-type foreground service
        // starts without location permission: bail out quietly instead of crashing.
        if (!hasLocationPermission(this)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        try {
            startForeground(NOTIF_ID, buildNotif());
        } catch (Exception e) {
            stopSelf();
            return START_NOT_STICKY;
        }
        if (intent != null && ACTION_RESTORE.equals(intent.getAction())) {
            release();
            broadcast();
            updateNotif();
            return START_STICKY;
        }
        ensureUpdates();
        h.removeCallbacks(tick);
        h.post(tick);
        fallbackEvaluate();
        return START_STICKY;
    }

    private void ensureUpdates() {
        if (updatesOn || lm == null) return;
        try {
            if (lm.getAllProviders().contains(LocationManager.GPS_PROVIDER)) {
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 15000, 10, this, Looper.getMainLooper());
            }
            if (lm.getAllProviders().contains(LocationManager.NETWORK_PROVIDER)) {
                lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 15000, 10, this, Looper.getMainLooper());
            }
            if (lm.getAllProviders().contains(LocationManager.PASSIVE_PROVIDER)) {
                lm.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 15000, 10, this, Looper.getMainLooper());
            }
            updatesOn = true;
        } catch (Exception ignore) {
        }
    }

    private void fallbackEvaluate() {
        if (lm == null) return;
        Location best = null;
        try {
            for (String p : lm.getAllProviders()) {
                Location l = lm.getLastKnownLocation(p);
                if (l == null) continue;
                if (best == null || l.getTime() > best.getTime()) best = l;
            }
        } catch (Exception ignore) {
        }
        if (best != null && System.currentTimeMillis() - best.getTime() < 5 * 60 * 1000) {
            evaluate(best.getLatitude(), best.getLongitude());
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        try {
            if (location != null) evaluate(location.getLatitude(), location.getLongitude());
        } catch (Exception ignore) {
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
        fallbackEvaluate();
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    private void evaluate(double lat, double lng) {
        List<Zone> zones = ZoneStore.list(this);
        Zone inside = null;
        Zone nearest = null;
        double bestInside = Double.MAX_VALUE;
        double bestNear = Double.MAX_VALUE;

        for (Zone z : zones) {
            if (!z.enabled) continue;
            double d = Geo.dist(lat, lng, z.lat, z.lng);
            if (d <= z.radius && d < bestInside) {
                bestInside = d;
                inside = z;
            }
            if (d < bestNear) {
                bestNear = d;
                nearest = z;
            }
        }

        if (nearest != null) {
            StateHub.nearestName = nearest.name;
            StateHub.nearestDist = bestNear;
            StateHub.nearestRadius = nearest.radius;
            StateHub.nearestColor = nearest.color();
            float span = (float) (nearest.radius * 4);
            float p = 1f - (float) (bestNear / Math.max(span, 1f));
            StateHub.proximity = Math.max(0f, Math.min(1f, p));
            if (inside != null) StateHub.proximity = 1f;
        } else {
            StateHub.nearestName = null;
            StateHub.nearestDist = -1;
            StateHub.proximity = 0f;
        }

        if (inside != null && !silentActive) {
            engage(inside);
        } else if (inside == null && silentActive) {
            release();
        }
        broadcast();
        updateNotif();
    }

    private void engage(Zone z) {
        silentActive = true;
        activeZoneName = z.name;
        try {
            savedRinger = am.getRingerMode();
            savedVolumes = new int[STREAMS.length];
            for (int i = 0; i < STREAMS.length; i++) savedVolumes[i] = am.getStreamVolume(STREAMS[i]);
            try {
                am.setRingerMode(Prefs.vibrateMode(this)
                        ? AudioManager.RINGER_MODE_VIBRATE
                        : AudioManager.RINGER_MODE_SILENT);
            } catch (Exception ignore) {
                // DND access not granted: volumes are still muted below.
            }
            for (int s : STREAMS) {
                try {
                    am.setStreamVolume(s, 0, 0);
                } catch (Exception ignore) {
                }
            }
        } catch (Exception ignore) {
        }
    }

    private void release() {
        if (!silentActive && savedRinger == -1) return;
        silentActive = false;
        activeZoneName = null;
        try {
            if (savedRinger != -1) {
                try {
                    am.setRingerMode(savedRinger);
                } catch (Exception ignore) {
                }
            }
            if (savedVolumes != null) {
                for (int i = 0; i < STREAMS.length && i < savedVolumes.length; i++) {
                    try {
                        am.setStreamVolume(STREAMS[i], savedVolumes[i], 0);
                    } catch (Exception ignore) {
                    }
                }
            }
        } catch (Exception ignore) {
        }
        savedRinger = -1;
        savedVolumes = null;
    }

    private void broadcast() {
        try {
            sendBroadcast(new Intent(ACTION_STATE).setPackage(getPackageName()));
        } catch (Exception ignore) {
        }
    }

    private Notification buildNotif() {
        if (nm != null && nm.getNotificationChannel(CHANNEL) == null) {
            NotificationChannel ch = new NotificationChannel(CHANNEL, "Protection", NotificationManager.IMPORTANCE_LOW);
            ch.setShowBadge(false);
            ch.setSound(null, null);
            nm.createNotificationChannel(ch);
        }
        String text;
        if (silentActive) {
            text = getString(R.string.svc_silent, activeZoneName == null ? "" : activeZoneName);
        } else {
            text = getString(R.string.svc_armed, ZoneStore.list(this).size());
        }
        PendingIntent pi = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this, CHANNEL)
                .setSmallIcon(R.drawable.ic_moon)
                .setContentTitle("Hush")
                .setContentText(text)
                .setContentIntent(pi)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
    }

    private void updateNotif() {
        try {
            nm.notify(NOTIF_ID, buildNotif());
        } catch (Exception ignore) {
        }
    }

    @Override
    public void onDestroy() {
        try {
            h.removeCallbacks(tick);
            if (lm != null && updatesOn) lm.removeUpdates(this);
        } catch (Exception ignore) {
        }
        release();
        StateHub.serviceRunning = false;
        broadcast();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
