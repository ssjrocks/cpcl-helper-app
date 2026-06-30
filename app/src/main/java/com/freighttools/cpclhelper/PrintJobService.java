package com.freighttools.cpclhelper;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.widget.Toast;

public class PrintJobService extends Service {
    static final String EXTRA_CODE = "code";
    static final String EXTRA_COPIES = "copies";
    static final String EXTRA_SOURCE = "source";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String rawCode = intent == null ? null : intent.getStringExtra(EXTRA_CODE);
        int copies = intent == null ? 1 : intent.getIntExtra(EXTRA_COPIES, 1);
        String source = intent == null ? "print link" : intent.getStringExtra(EXTRA_SOURCE);

        new Thread(() -> {
            try {
                print(rawCode, copies, source);
            } finally {
                stopSelf(startId);
            }
        }).start();

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void print(String rawCode, int copies, String source) {
        String savedMac = CpclPrinter.getSavedPrinterMac(this);
        String savedName = CpclPrinter.getSavedPrinterName(this);
        if (savedMac == null) {
            showToast("CPCL Helper: no saved printer.");
            return;
        }
        if (!CpclPrinter.hasBluetoothPermission(this)) {
            showToast("CPCL Helper: Bluetooth permission is required.");
            return;
        }

        String code = CpclPrinter.sanitizeForCpcl(rawCode);
        if (code.length() == 0) {
            showToast("CPCL Helper: label code is empty.");
            return;
        }

        try {
            CpclPrinter.sendCpcl(savedMac, CpclPrinter.buildCpcl(code, copies));
            showToast("CPCL Helper: printed " + code + " to " + savedName + ".");
        } catch (Exception ex) {
            showToast("CPCL Helper: print failed: " + ex.getMessage());
        }
    }

    private void showToast(String message) {
        android.os.Handler handler = new android.os.Handler(getMainLooper());
        handler.post(() -> Toast.makeText(this, message, Toast.LENGTH_LONG).show());
    }
}
