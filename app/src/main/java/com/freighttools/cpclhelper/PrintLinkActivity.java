package com.freighttools.cpclhelper;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

public class PrintLinkActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handlePrintLink(getIntent());
        finish();
        overridePendingTransition(0, 0);
    }

    private void handlePrintLink(Intent intent) {
        Uri uri = intent == null ? null : intent.getData();
        if (uri == null || !"freightprint".equals(uri.getScheme()) || !"print".equals(uri.getHost())) {
            return;
        }

        String code = firstNonBlank(
                uri.getQueryParameter("code"),
                uri.getQueryParameter("label"),
                uri.getQueryParameter("text")
        );
        if (code == null) {
            Toast.makeText(this, "CPCL Helper: print link did not include code=...", Toast.LENGTH_LONG).show();
            return;
        }

        Intent serviceIntent = new Intent(this, PrintJobService.class);
        serviceIntent.putExtra(PrintJobService.EXTRA_CODE, code);
        serviceIntent.putExtra(PrintJobService.EXTRA_COPIES, CpclPrinter.parseCopies(uri.getQueryParameter("copies")));
        serviceIntent.putExtra(PrintJobService.EXTRA_SOURCE, "Power Apps link");
        startService(serviceIntent);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && value.trim().length() > 0) {
                return value.trim();
            }
        }
        return null;
    }
}
