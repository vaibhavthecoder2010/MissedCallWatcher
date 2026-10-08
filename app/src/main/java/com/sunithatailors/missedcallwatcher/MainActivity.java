package com.sunithatailors.missedcallwatcher;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int BROWN = Color.parseColor("#B2651A");
    private LinearLayout listBox;
    private TextView statusView;

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private Button makeButton(String label, int color) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setBackgroundColor(color);
        b.setTextSize(13);
        return b;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(20), dp(16), dp(30));
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("Missed Call Watcher");
        title.setTextSize(26);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(Color.parseColor("#555555"));
        root.addView(title);

        TextView info = new TextView(this);
        info.setText("Lists missed calls on this phone automatically. "
                + "Tap Send message to reply to the customer.");
        info.setTextSize(14);
        info.setPadding(0, dp(6), 0, dp(10));
        root.addView(info);

        statusView = new TextView(this);
        statusView.setTextSize(13);
        statusView.setPadding(0, 0, 0, dp(10));
        root.addView(statusView);

        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        Button grantNotif = makeButton("1. Grant notification access", BROWN);
        grantNotif.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
            }
        });
        LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        lp1.setMargins(0, 0, dp(6), 0);
        row1.addView(grantNotif, lp1);

        Button grantLog = makeButton("2. Allow call log", BROWN);
        grantLog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestPermissions(new String[]{Manifest.permission.READ_CALL_LOG}, 1);
            }
        });
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        row1.addView(grantLog, lp2);
        root.addView(row1);

        Button clear = makeButton("Clear list", Color.parseColor("#777777"));
        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Store.clear(MainActivity.this);
                refresh();
            }
        });
        LinearLayout.LayoutParams lp3 = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp3.setMargins(0, dp(8), 0, dp(14));
        root.addView(clear, lp3);

        listBox = new LinearLayout(this);
        listBox.setOrientation(LinearLayout.VERTICAL);
        root.addView(listBox);

        setContentView(scroll);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Store.syncCallLog(this);
        refresh();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Store.syncCallLog(this);
        refresh();
    }

    private boolean notificationAccessOn() {
        String s = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        return s != null && s.contains(getPackageName());
    }

    private void refresh() {
        boolean n = notificationAccessOn();
        boolean l = Store.hasCallLogPermission(this);
        statusView.setText("Notification access: " + (n ? "ON \u2705" : "OFF \u274C")
                + "\nCall log access: " + (l ? "ON \u2705" : "OFF (numbers of saved contacts may show only as names)"));

        listBox.removeAllViews();
        JSONArray a = Store.load(this);
        if (a.length() == 0) {
            TextView empty = new TextView(this);
            empty.setText("No missed calls yet.");
            empty.setPadding(0, dp(20), 0, 0);
            listBox.addView(empty);
            return;
        }
        SimpleDateFormat fmt = new SimpleDateFormat("dd MMM, hh:mm a", Locale.ENGLISH);
        for (int i = 0; i < a.length(); i++) {
            try {
                final JSONObject o = a.getJSONObject(i);
                final String id = o.optString("id");
                final String number = Store.last10(o.optString("number"));
                String name = o.optString("name");
                boolean done = o.optBoolean("done", false);

                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setPadding(0, dp(12), 0, dp(12));

                TextView head = new TextView(this);
                String shown = number.isEmpty() ? (name.isEmpty() ? "Unknown number" : name) : number;
                head.setText((done ? "\u2705 " : "\uD83D\uDCDE ") + shown);
                head.setTextSize(20);
                head.setTypeface(Typeface.DEFAULT_BOLD);
                head.setTextColor(done ? Color.parseColor("#999999") : Color.parseColor("#B03A2E"));
                card.addView(head);

                TextView sub = new TextView(this);
                String subText = fmt.format(new Date(o.optLong("time")));
                if (!number.isEmpty() && !name.isEmpty()) subText = name + " \u00B7 " + subText;
                sub.setText(subText + (done ? " \u00B7 done" : ""));
                sub.setTextColor(Color.parseColor("#777777"));
                sub.setPadding(0, 0, 0, dp(6));
                card.addView(sub);

                LinearLayout btns = new LinearLayout(this);
                btns.setOrientation(LinearLayout.HORIZONTAL);

                Button wa = makeButton("\uD83D\uDCAC WhatsApp", Color.parseColor("#1F8A4C"));
                wa.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (number.length() < 10) { noNumber(); return; }
                        openUri(Intent.ACTION_VIEW, "https://api.whatsapp.com/send?phone=91" + number
                                + "&text=" + Uri.encode(Store.MESSAGE), null);
                    }
                });
                Button sms = makeButton("\uD83D\uDCE9 SMS", Color.parseColor("#1D4E5E"));
                sms.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (number.length() < 10) { noNumber(); return; }
                        openUri(Intent.ACTION_SENDTO, "smsto:" + number, Store.MESSAGE);
                    }
                });
                Button call = makeButton("\uD83D\uDCDE Call", Color.parseColor("#8B6512"));
                call.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (number.length() < 10) { noNumber(); return; }
                        openUri(Intent.ACTION_DIAL, "tel:" + number, null);
                    }
                });
                Button doneBtn = makeButton("\u2713", Color.parseColor("#777777"));
                doneBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Store.markDone(MainActivity.this, id);
                        refresh();
                    }
                });

                LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                bp.setMargins(0, 0, dp(4), 0);
                btns.addView(wa, bp);
                btns.addView(sms, bp);
                btns.addView(call, bp);
                LinearLayout.LayoutParams dp2 = new LinearLayout.LayoutParams(dp(48), LinearLayout.LayoutParams.WRAP_CONTENT);
                btns.addView(doneBtn, dp2);
                card.addView(btns);

                View line = new View(this);
                line.setBackgroundColor(Color.parseColor("#DDDDDD"));
                listBox.addView(card);
                listBox.addView(line, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1));
            } catch (Exception ignored) {
            }
        }
    }

    private void noNumber() {
        Toast.makeText(this, "No phone number for this entry. Allow call log access (button 2).",
                Toast.LENGTH_LONG).show();
    }

    private void openUri(String action, String uri, String smsBody) {
        try {
            Intent i = new Intent(action, Uri.parse(uri));
            if (smsBody != null) i.putExtra("sms_body", smsBody);
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, "Could not open the app", Toast.LENGTH_SHORT).show();
        }
    }
}
