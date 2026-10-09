package com.sunithatailors.missedcallwatcher;

import android.os.Handler;
import android.os.Looper;
import android.util.Base64;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** Writes one missed-call booking to Firestore `orders` using the REST API (no Firebase SDK needed). */
public class Booker {

    // >>> FILL THESE TWO from the firebaseConfig in your index.html <<<
    private static final String PROJECT_ID = "sunitha-tailors-43886";
    private static final String API_KEY = "AIzaSyCkMC0QbDLHzHs9lnXBrz0YX4Z2FSa6ipc";

    public interface Callback {
        void onResult(boolean ok, String error);
    }

    private static JSONObject str(String v) throws Exception {
        return new JSONObject().put("stringValue", v == null ? "" : v);
    }

    public static void book(String phone, String name, long callTime, Callback cb) {
        book(phone, name, callTime, null, cb);
    }

    private static String voiceDataUrl(File f) throws Exception {
        FileInputStream in = new FileInputStream(f);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        in.close();
        return "data:audio/mp4;base64," + Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP);
    }

    public static void book(final String phone, final String name, final long callTime, final File voice, final Callback cb) {
        final Handler ui = new Handler(Looper.getMainLooper());
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean ok = false;
                String err = "";
                HttpURLConnection con = null;
                try {
                    SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH);
                    iso.setTimeZone(TimeZone.getTimeZone("UTC"));

                    JSONObject f = new JSONObject();
                    f.put("orderType", str("missedCall"));
                    f.put("status", str("new"));
                    f.put("customerPhone", str(phone));
                    f.put("customerName", str(name));
                    f.put("source", str("watcher-app"));
                    f.put("callTime", new JSONObject().put("timestampValue", iso.format(new Date(callTime))));
                    f.put("createdAt", new JSONObject().put("timestampValue", iso.format(new Date())));
                    if (voice != null && voice.exists() && voice.length() > 0) {
                        f.put("voiceNote", str(voiceDataUrl(voice)));
                    }
                    JSONObject body = new JSONObject().put("fields", f);

                    URL url = new URL("https://firestore.googleapis.com/v1/projects/" + PROJECT_ID
                            + "/databases/(default)/documents/orders?key=" + API_KEY);
                    con = (HttpURLConnection) url.openConnection();
                    con.setRequestMethod("POST");
                    con.setConnectTimeout(15000);
                    con.setReadTimeout(15000);
                    con.setDoOutput(true);
                    con.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                    OutputStream os = con.getOutputStream();
                    os.write(body.toString().getBytes("UTF-8"));
                    os.close();

                    int code = con.getResponseCode();
                    ok = code >= 200 && code < 300;
                    if (!ok) err = "HTTP " + code;
                } catch (Exception e) {
                    err = e.getClass().getSimpleName();
                } finally {
                    if (con != null) con.disconnect();
                }
                final boolean fOk = ok;
                final String fErr = err;
                ui.post(new Runnable() {
                    @Override
                    public void run() {
                        cb.onResult(fOk, fErr);
                    }
                });
            }
        }).start();
    }
}
