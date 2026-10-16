package com.aurora.hush;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            if (!Prefs.bootStart(context) || !Prefs.master(context)) return;
            if (!SilentService.hasLocationPermission(context)) return;
            Intent i = new Intent(context, SilentService.class);
            if (Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(i);
            } else {
                context.startService(i);
            }
        } catch (Exception ignore) {
            // Never crash the boot process.
        }
    }
}
