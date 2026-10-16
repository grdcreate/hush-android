package com.aurora.hush;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.util.Locale;

public class MapPickActivity extends Activity {

    private WebView wv;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_map);
        wv = findViewById(R.id.web);
        wv.getSettings().setJavaScriptEnabled(true);
        wv.getSettings().setDomStorageEnabled(true);
        wv.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, android.webkit.WebResourceRequest request,
                                        android.webkit.WebResourceError error) {
                if (request.isForMainFrame()) {
                    Toast.makeText(MapPickActivity.this,
                            "Map needs an internet connection to load once", Toast.LENGTH_LONG).show();
                }
            }
        });
        wv.addJavascriptInterface(new Bridge(), "AndroidBridge");
        wv.loadUrl("file:///android_asset/map.html");
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private Location lastKnown() {
        try {
            LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            Location best = null;
            for (String p : lm.getAllProviders()) {
                Location l = lm.getLastKnownLocation(p);
                if (l == null) continue;
                if (best == null || l.getTime() > best.getTime()) best = l;
            }
            return best;
        } catch (SecurityException e) {
            return null;
        }
    }

    private class Bridge {
        @JavascriptInterface
        public String getInitial() {
            Location l = lastKnown();
            double la = l != null ? l.getLatitude() : 26.1445d;
            double ln = l != null ? l.getLongitude() : 91.7362d;
            int zoom = l != null ? 15 : 12;
            return String.format(Locale.US, "{\"lat\":%f,\"lng\":%f,\"zoom\":%d}", la, ln, zoom);
        }

        @JavascriptInterface
        public void confirm(final double lat, final double lng) {
            runOnUiThread(() -> {
                Intent data = new Intent();
                data.putExtra("lat", lat);
                data.putExtra("lng", lng);
                setResult(RESULT_OK, data);
                finish();
            });
        }

        @JavascriptInterface
        public void toast(final String msg) {
            runOnUiThread(() -> Toast.makeText(MapPickActivity.this, msg, Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    public void onBackPressed() {
        finish();
    }
}
