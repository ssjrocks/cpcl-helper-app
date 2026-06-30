package com.freighttools.cpclhelper;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

final class CpclPrinter {
    static final String PREFS_NAME = "cpcl_helper";
    static final String PREF_PRINTER_MAC = "printer_mac";
    static final String PREF_PRINTER_NAME = "printer_name";
    static final String DEFAULT_CODE = "APLP10056543001";

    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private CpclPrinter() {
    }

    static boolean hasBluetoothPermission(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S
                || context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
    }

    static String getSavedPrinterMac(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(PREF_PRINTER_MAC, null);
    }

    static String getSavedPrinterName(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(PREF_PRINTER_NAME, "Bluetooth printer");
    }

    static String buildCpcl(String code, int copies) {
        return "! 0 200 200 320 " + copies + "\r\n"
                + "PW 576\r\n"
                + "CENTER\r\n"
                + "BARCODE QR 200 30 M 2 U 11\r\n"
                + "MA," + code + "\r\n"
                + "ENDQR\r\n"
                + "TEXT 4 0 24 270 " + code + "\r\n"
                + "FORM\r\n"
                + "PRINT\r\n";
    }

    static String sanitizeForCpcl(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length() && builder.length() < 120; i++) {
            char c = value.charAt(i);
            if (c >= 32 && c <= 126) {
                builder.append(c);
            } else if (c == '\r' || c == '\n' || c == '\t') {
                builder.append(' ');
            }
        }
        return builder.toString().trim();
    }

    static int parseCopies(String value) {
        try {
            int copies = Integer.parseInt(value);
            if (copies < 1) {
                return 1;
            }
            return Math.min(copies, 20);
        } catch (Exception ex) {
            return 1;
        }
    }

    static void sendCpcl(String mac, String cpcl) throws IOException {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            throw new IOException("Bluetooth is not available on this device.");
        }
        if (!adapter.isEnabled()) {
            throw new IOException("Bluetooth is turned off.");
        }

        BluetoothDevice device;
        try {
            device = adapter.getRemoteDevice(mac);
        } catch (SecurityException ex) {
            throw new IOException("Bluetooth permission was denied while opening the saved printer.");
        } catch (IllegalArgumentException ex) {
            throw new IOException("Saved printer address is invalid.");
        }

        BluetoothSocket socket = null;
        try {
            socket = device.createRfcommSocketToServiceRecord(SPP_UUID);
            writeCpclToSocket(socket, cpcl);
        } catch (IOException firstFailure) {
            closeQuietly(socket);
            socket = null;
            try {
                socket = device.createInsecureRfcommSocketToServiceRecord(SPP_UUID);
                writeCpclToSocket(socket, cpcl);
            } catch (SecurityException ex) {
                throw new IOException("Bluetooth permission was denied while opening the fallback printer socket.");
            } catch (IOException secondFailure) {
                throw new IOException("Could not connect to printer: " + secondFailure.getMessage());
            }
        } catch (SecurityException ex) {
            throw new IOException("Bluetooth permission was denied while opening the printer socket.");
        } finally {
            closeQuietly(socket);
        }
    }

    private static void writeCpclToSocket(BluetoothSocket socket, String cpcl) throws IOException {
        socket.connect();
        socket.getOutputStream().write(cpcl.getBytes(StandardCharsets.US_ASCII));
        socket.getOutputStream().flush();
    }

    private static void closeQuietly(BluetoothSocket socket) {
        if (socket == null) {
            return;
        }
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }
}
