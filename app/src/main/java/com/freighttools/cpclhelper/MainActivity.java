package com.freighttools.cpclhelper;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends Activity {
    private static final String PREFS_NAME = "cpcl_helper";
    private static final String PREF_PRINTER_MAC = "printer_mac";
    private static final String PREF_PRINTER_NAME = "printer_name";
    private static final String DEFAULT_CODE = "APLP10056543001";
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final int REQUEST_BLUETOOTH_CONNECT = 1001;

    private final List<DeviceChoice> devices = new ArrayList<>();

    private SharedPreferences prefs;
    private TextView selectedPrinterText;
    private TextView statusText;
    private EditText codeInput;
    private EditText copiesInput;
    private Spinner printerSpinner;
    private Button savePrinterButton;
    private Button testPrintButton;
    private Button refreshButton;
    private String selectedMac;
    private String selectedName;
    private String pendingPrintCode;
    private int pendingPrintCopies;
    private String pendingPrintSource;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        selectedMac = prefs.getString(PREF_PRINTER_MAC, null);
        selectedName = prefs.getString(PREF_PRINTER_NAME, null);

        buildLayout();
        updateSelectedPrinterLabel();
        refreshPrinters();
        ensureBluetoothPermission();
        handlePrintIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handlePrintIntent(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH_CONNECT) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                refreshPrinters();
                if (pendingPrintCode != null) {
                    String code = pendingPrintCode;
                    int copies = pendingPrintCopies;
                    String source = pendingPrintSource;
                    pendingPrintCode = null;
                    pendingPrintCopies = 0;
                    pendingPrintSource = null;
                    printLabel(code, copies, source);
                } else {
                    setStatus("Bluetooth permission granted.");
                }
            } else {
                setStatus("Bluetooth permission is required to list paired printers and print.");
            }
        }
    }

    private void buildLayout() {
        int pageBg = Color.rgb(247, 248, 245);
        int ink = Color.rgb(28, 39, 38);
        int muted = Color.rgb(88, 99, 96);
        int accent = Color.rgb(33, 107, 95);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(pageBg);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20), dp(22), dp(20), dp(24));
        scrollView.addView(page, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        TextView title = new TextView(this);
        title.setText("CPCL Helper");
        title.setTextColor(ink);
        title.setTextSize(28);
        title.setGravity(Gravity.START);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        page.addView(title, fullWidth());

        TextView subtitle = new TextView(this);
        subtitle.setText("Bluetooth bridge for Power Apps label printing");
        subtitle.setTextColor(muted);
        subtitle.setTextSize(15);
        subtitle.setPadding(0, dp(4), 0, dp(18));
        page.addView(subtitle, fullWidth());

        selectedPrinterText = label("No printer selected", ink, 17, true);
        selectedPrinterText.setPadding(0, 0, 0, dp(10));
        page.addView(selectedPrinterText, fullWidth());

        printerSpinner = new Spinner(this);
        page.addView(printerSpinner, fullWidthWithBottom(12));

        LinearLayout printerActions = row();
        refreshButton = actionButton("Refresh");
        savePrinterButton = actionButton("Save printer");
        printerActions.addView(refreshButton, weighted());
        printerActions.addView(savePrinterButton, weightedWithStartMargin(10));
        page.addView(printerActions, fullWidthWithBottom(22));

        page.addView(label("Label code", ink, 15, true), fullWidth());
        codeInput = new EditText(this);
        codeInput.setSingleLine(true);
        codeInput.setText(DEFAULT_CODE);
        codeInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        page.addView(codeInput, fullWidthWithBottom(12));

        page.addView(label("Copies", ink, 15, true), fullWidth());
        copiesInput = new EditText(this);
        copiesInput.setSingleLine(true);
        copiesInput.setText("1");
        copiesInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        page.addView(copiesInput, fullWidthWithBottom(18));

        testPrintButton = actionButton("Test Print");
        testPrintButton.setTextColor(Color.WHITE);
        testPrintButton.setBackgroundColor(accent);
        page.addView(testPrintButton, fullWidthWithBottom(22));

        statusText = label("Ready.", muted, 14, false);
        statusText.setPadding(dp(12), dp(12), dp(12), dp(12));
        statusText.setBackgroundColor(Color.WHITE);
        page.addView(statusText, fullWidth());

        refreshButton.setOnClickListener(v -> refreshPrinters());
        savePrinterButton.setOnClickListener(v -> saveSelectedPrinter());
        testPrintButton.setOnClickListener(v -> {
            String code = codeInput.getText().toString();
            int copies = parseCopies(copiesInput.getText().toString());
            printLabel(code, copies, "manual test");
        });

        printerSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < devices.size()) {
                    DeviceChoice choice = devices.get(position);
                    selectedMac = choice.mac;
                    selectedName = choice.name;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        setContentView(scrollView);
    }

    private void refreshPrinters() {
        if (!hasBluetoothPermission()) {
            setStatus("Bluetooth permission is needed before paired printers can be shown.");
            return;
        }

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            setStatus("This device does not have Bluetooth.");
            return;
        }
        if (!adapter.isEnabled()) {
            setStatus("Bluetooth is turned off. Turn it on in Android settings.");
            return;
        }

        devices.clear();
        try {
            Set<BluetoothDevice> bondedDevices = adapter.getBondedDevices();
            for (BluetoothDevice device : bondedDevices) {
                String name = safeDeviceName(device);
                String mac = device.getAddress();
                devices.add(new DeviceChoice(name, mac));
            }
        } catch (SecurityException ex) {
            setStatus("Android blocked Bluetooth access. Grant Bluetooth permission and retry.");
            return;
        }

        Collections.sort(devices, new Comparator<DeviceChoice>() {
            @Override
            public int compare(DeviceChoice first, DeviceChoice second) {
                return first.name.compareToIgnoreCase(second.name);
            }
        });

        List<String> labels = new ArrayList<>();
        int selectedIndex = 0;
        for (int i = 0; i < devices.size(); i++) {
            DeviceChoice choice = devices.get(i);
            labels.add(choice.name + "\n" + choice.mac);
            if (choice.mac.equals(prefs.getString(PREF_PRINTER_MAC, ""))) {
                selectedIndex = i;
            }
        }

        ArrayAdapter<String> adapterView = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                labels
        );
        adapterView.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        printerSpinner.setAdapter(adapterView);

        if (!devices.isEmpty()) {
            printerSpinner.setSelection(selectedIndex);
            setStatus("Found " + devices.size() + " paired Bluetooth device(s).");
        } else {
            setStatus("No paired Bluetooth devices found. Pair the printer in Android settings first.");
        }
    }

    private void saveSelectedPrinter() {
        if (selectedMac == null) {
            setStatus("Choose a paired printer first.");
            return;
        }
        prefs.edit()
                .putString(PREF_PRINTER_MAC, selectedMac)
                .putString(PREF_PRINTER_NAME, selectedName)
                .apply();
        updateSelectedPrinterLabel();
        setStatus("Saved printer: " + selectedName + " (" + selectedMac + ")");
    }

    private void updateSelectedPrinterLabel() {
        String savedName = prefs.getString(PREF_PRINTER_NAME, null);
        String savedMac = prefs.getString(PREF_PRINTER_MAC, null);
        if (savedMac == null) {
            selectedPrinterText.setText("No printer selected");
        } else {
            selectedPrinterText.setText("Saved printer: " + savedName + " (" + savedMac + ")");
        }
    }

    private void handlePrintIntent(Intent intent) {
        if (intent == null || intent.getData() == null) {
            return;
        }

        Uri uri = intent.getData();
        if (!"freightprint".equals(uri.getScheme()) || !"print".equals(uri.getHost())) {
            return;
        }

        String code = firstNonBlank(
                uri.getQueryParameter("code"),
                uri.getQueryParameter("label"),
                uri.getQueryParameter("text")
        );
        if (code == null) {
            setStatus("Print link opened, but it did not include code=...");
            return;
        }

        int copies = parseCopies(uri.getQueryParameter("copies"));
        codeInput.setText(code);
        copiesInput.setText(String.valueOf(copies));
        printLabel(code, copies, "Power Apps link");
    }

    private void printLabel(String rawCode, int copies, String source) {
        String savedMac = prefs.getString(PREF_PRINTER_MAC, null);
        String savedName = prefs.getString(PREF_PRINTER_NAME, "Bluetooth printer");
        if (savedMac == null) {
            setStatus("No saved printer. Select the paired printer, save it, then retry.");
            return;
        }
        if (!hasBluetoothPermission()) {
            pendingPrintCode = rawCode;
            pendingPrintCopies = copies;
            pendingPrintSource = source;
            ensureBluetoothPermission();
            setStatus("Bluetooth permission is required before printing.");
            return;
        }

        String code = sanitizeForCpcl(rawCode);
        if (code.length() == 0) {
            setStatus("Label code is empty.");
            return;
        }

        String cpcl = buildCpcl(code, copies);
        setStatus("Printing " + code + " to " + savedName + " from " + source + "...");

        new Thread(() -> {
            try {
                sendCpcl(savedMac, cpcl);
                runOnUiThread(() -> setStatus("Printed " + code + " to " + savedName + "."));
            } catch (Exception ex) {
                runOnUiThread(() -> setStatus("Print failed: " + ex.getMessage()));
            }
        }).start();
    }

    private void sendCpcl(String mac, String cpcl) throws IOException {
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

    private void writeCpclToSocket(BluetoothSocket socket, String cpcl) throws IOException {
        socket.connect();
        socket.getOutputStream().write(cpcl.getBytes(StandardCharsets.US_ASCII));
        socket.getOutputStream().flush();
    }

    private void closeQuietly(BluetoothSocket socket) {
        if (socket == null) {
            return;
        }
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }

    private String buildCpcl(String code, int copies) {
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

    private String sanitizeForCpcl(String value) {
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

    private int parseCopies(String value) {
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

    private void ensureBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                    REQUEST_BLUETOOTH_CONNECT
            );
        }
    }

    private boolean hasBluetoothPermission() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S
                || checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
    }

    private String safeDeviceName(BluetoothDevice device) {
        try {
            String name = device.getName();
            if (name != null && name.trim().length() > 0) {
                return name.trim();
            }
        } catch (SecurityException ignored) {
        }
        return "Bluetooth printer";
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && value.trim().length() > 0) {
                return value.trim();
            }
        }
        return null;
    }

    private void setStatus(String message) {
        statusText.setText(message);
    }

    private TextView label(String text, int color, int sp, boolean bold) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(color);
        view.setTextSize(sp);
        if (bold) {
            view.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        }
        return view;
    }

    private Button actionButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setMinHeight(dp(48));
        return button;
    }

    private LinearLayout row() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        return row;
    }

    private LinearLayout.LayoutParams fullWidth() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }

    private LinearLayout.LayoutParams fullWidthWithBottom(int bottomDp) {
        LinearLayout.LayoutParams params = fullWidth();
        params.setMargins(0, 0, 0, dp(bottomDp));
        return params;
    }

    private LinearLayout.LayoutParams weighted() {
        return new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
        );
    }

    private LinearLayout.LayoutParams weightedWithStartMargin(int startDp) {
        LinearLayout.LayoutParams params = weighted();
        params.setMargins(dp(startDp), 0, 0, 0);
        return params;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private static class DeviceChoice {
        final String name;
        final String mac;

        DeviceChoice(String name, String mac) {
            this.name = name;
            this.mac = mac;
        }
    }
}
