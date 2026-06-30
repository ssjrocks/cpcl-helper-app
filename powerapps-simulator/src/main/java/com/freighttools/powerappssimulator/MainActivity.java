package com.freighttools.powerappssimulator;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final String DEFAULT_CODE = "APLP10056543001";

    private EditText codeInput;
    private EditText copiesInput;
    private TextView previewText;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildLayout();
        updatePreview();
    }

    private void buildLayout() {
        int pageBg = Color.rgb(246, 247, 249);
        int ink = Color.rgb(25, 34, 43);
        int muted = Color.rgb(91, 101, 112);
        int accent = Color.rgb(47, 93, 140);

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

        TextView title = label("Power Apps Simulator", ink, 26, true);
        title.setGravity(Gravity.START);
        page.addView(title, fullWidth());

        TextView subtitle = label("Launches the same freightprint link Power Apps will use", muted, 15, false);
        subtitle.setPadding(0, dp(4), 0, dp(18));
        page.addView(subtitle, fullWidth());

        page.addView(label("Load or freight code", ink, 15, true), fullWidth());
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

        Button previewButton = actionButton("Refresh Link Preview");
        page.addView(previewButton, fullWidthWithBottom(10));

        Button launchButton = actionButton("Launch CPCL Helper");
        launchButton.setTextColor(Color.WHITE);
        launchButton.setBackgroundColor(accent);
        page.addView(launchButton, fullWidthWithBottom(22));

        previewText = label("", muted, 14, false);
        previewText.setPadding(dp(12), dp(12), dp(12), dp(12));
        previewText.setBackgroundColor(Color.WHITE);
        page.addView(previewText, fullWidthWithBottom(12));

        statusText = label("Ready.", muted, 14, false);
        statusText.setPadding(dp(12), dp(12), dp(12), dp(12));
        statusText.setBackgroundColor(Color.WHITE);
        page.addView(statusText, fullWidth());

        previewButton.setOnClickListener(v -> updatePreview());
        launchButton.setOnClickListener(v -> launchBridge());

        setContentView(scrollView);
    }

    private void launchBridge() {
        Uri uri = buildFreightPrintUri();
        updatePreview(uri);

        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.addCategory(Intent.CATEGORY_BROWSABLE);

        try {
            startActivity(intent);
            statusText.setText("Launched CPCL Helper with " + uri);
        } catch (ActivityNotFoundException ex) {
            statusText.setText("CPCL Helper is not installed or cannot handle freightprint://print links.");
        }
    }

    private Uri buildFreightPrintUri() {
        return new Uri.Builder()
                .scheme("freightprint")
                .authority("print")
                .appendQueryParameter("code", cleanCode())
                .appendQueryParameter("copies", String.valueOf(parseCopies()))
                .build();
    }

    private String cleanCode() {
        String code = codeInput.getText().toString().trim();
        if (code.length() == 0) {
            return DEFAULT_CODE;
        }
        return code;
    }

    private int parseCopies() {
        try {
            int copies = Integer.parseInt(copiesInput.getText().toString().trim());
            if (copies < 1) {
                return 1;
            }
            return Math.min(copies, 20);
        } catch (Exception ex) {
            return 1;
        }
    }

    private void updatePreview() {
        updatePreview(buildFreightPrintUri());
    }

    private void updatePreview(Uri uri) {
        previewText.setText("Deep link:\n" + uri.toString());
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

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
