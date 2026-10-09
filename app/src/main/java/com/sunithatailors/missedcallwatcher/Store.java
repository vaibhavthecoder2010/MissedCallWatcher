package com.sunithatailors.missedcallwatcher;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.CallLog;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Keeps the list of missed calls and reads the phone's call log. */
public class Store {

    // Message sent to the customer. Edit here if you want different text.
    public static final String MESSAGE =
            "Namaste \uD83D\uDE4F Thanks for calling Sunitha Tailors. We received your call.\n"
            + "\n"
            + "\uD83E\uDDF5 Handed over clothes? Confirm:\n"
            + "\uD83E\uDDF5 \u0C2C\u0C1F\u0C4D\u0C1F\u0C32\u0C41 \u0C07\u0C1A\u0C4D\u0C1A\u0C3E\u0C30\u0C3E? \u0C27\u0C43\u0C35\u0C40\u0C15\u0C30\u0C3F\u0C02\u0C1A\u0C02\u0C21\u0C3F:  https://sunithatailor.netlify.app/confirm.html\n"
            + "\n"
            + "\u2753 Need help?\u2753 \u0C38\u0C39\u0C3E\u0C2F\u0C02 \u0C15\u0C3E\u0C35\u0C3E\u0C32\u0C3E? https://sunithatailor.netlify.app/help";

    /** Message with the customer's number (and saved contact name, if any) in the link, so the confirm page fills them automatically. */
    public static String messageFor(String number, String name) {
        String n = last10(number);
        if (n.isEmpty()) return MESSAGE;
        String q = "confirm.html?phone=" + n;
        String nm = name == null ? "" : name.trim();
        if (nm.length() >= 2 && !nm.matches("[0-9+\\-\\s()]*") && !nm.equalsIgnoreCase("unknown")) {
            try {
                q += "&name=" + java.net.URLEncoder.encode(nm, "UTF-8");
            } catch (Exception ignored) {
            }
        }
        return MESSAGE.replace("confirm.html", q);
    }

    /** Message shown to the customer after the order is booked (opens in WhatsApp for the owner to send). */
    public static String bookedMessage(String name, String number) {
        String n = last10(number);
        String nm = name == null ? "" : name.trim();
        if (nm.length() < 2 || nm.matches("[0-9+\\-\\s()]*") || nm.equalsIgnoreCase("unknown")) nm = "Customer";
        return "*Sunitha Tailors & Alterations*\n\uD83D\uDC56 \uD83E\uDDE5 \uD83D\uDC54 \uD83E\uDD7C \uD83D\uDC57 \uD83E\uDE73 \uD83E\uDD7B \uD83D\uDC55 \uD83C\uDF92\n\uD83D\uDCDE 8247489557\n\nName: " + nm + "\nPhone: " + n + "\n\nYour order has been successfully booked!\n\uD83C\uDF89\n\nOur shop owner will message you shortly with your bill and payment details.\n\nTo check your booking status and for more details, please visit our website:\n\uD83C\uDF10 https://sunithatailor.netlify.app\n\n\u2753 Need help? / \u0C38\u0C39\u0C3E\u0C2F\u0C02 \u0C15\u0C3E\u0C35\u0C3E\u0C32\u0C3E?\n\uD83D\uDC49 https://sunithatailor.netlify.app/?help=1&phone=" + n + "\n\n\uD83D\uDE4F Thank you! \uD83D\uDE4F";
    }

    private static final String PREFS = "mcw";
    private static final String KEY_LIST = "list";
    private static final String KEY_LAST_DATE = "lastCallDate";

    public static synchronized JSONArray load(Context c) {
        try {
            String s = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_LIST, "[]");
            return new JSONArray(s);
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    private static void save(Context c, JSONArray a) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_LIST, a.toString()).apply();
    }

    public static synchronized void clear(Context c) {
        save(c, new JSONArray());
    }

    public static synchronized void markDone(Context c, String id) {
        try {
            JSONArray a = load(c);
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                if (id.equals(o.optString("id"))) {
                    o.put("done", true);
                }
            }
            save(c, a);
        } catch (Exception ignored) {
        }
    }

    public static synchronized void markBooked(Context c, String id) {
        try {
            JSONArray a = load(c);
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                if (id.equals(o.optString("id"))) {
                    o.put("booked", true);
                    o.put("done", true);
                }
            }
            save(c, a);
        } catch (Exception ignored) {
        }
    }

    /** Adds an entry unless the same id is already there. Returns true if added. */
    public static synchronized boolean add(Context c, String id, String number, String name, long time) {
        try {
            JSONArray a = load(c);
            for (int i = 0; i < a.length(); i++) {
                if (id.equals(a.getJSONObject(i).optString("id"))) return false;
            }
            JSONObject o = new JSONObject();
            o.put("id", id);
            o.put("number", number == null ? "" : number);
            o.put("name", name == null ? "" : name);
            o.put("time", time);
            o.put("done", false);
            // newest first, keep at most 200
            JSONArray out = new JSONArray();
            out.put(o);
            for (int i = 0; i < a.length() && out.length() < 200; i++) out.put(a.get(i));
            save(c, out);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static String last10(String s) {
        if (s == null) return "";
        String d = s.replaceAll("\\D", "");
        return d.length() > 10 ? d.substring(d.length() - 10) : d;
    }

    public static boolean hasCallLogPermission(Context c) {
        return c.checkSelfPermission(Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED;
    }

    /** Reads new missed calls from the call log. Returns how many were added. */
    public static synchronized int syncCallLog(Context c) {
        if (!hasCallLogPermission(c)) return 0;
        SharedPreferences sp = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long since = sp.getLong(KEY_LAST_DATE, 0L);
        if (since == 0L) {
            // first run: only look at calls from today
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            since = cal.getTimeInMillis();
        }
        int added = 0;
        long maxDate = since;
        Cursor cur = null;
        try {
            cur = c.getContentResolver().query(
                    CallLog.Calls.CONTENT_URI,
                    new String[]{CallLog.Calls._ID, CallLog.Calls.NUMBER,
                            CallLog.Calls.CACHED_NAME, CallLog.Calls.DATE},
                    CallLog.Calls.TYPE + "=" + CallLog.Calls.MISSED_TYPE
                            + " AND " + CallLog.Calls.DATE + ">?",
                    new String[]{String.valueOf(since)},
                    CallLog.Calls.DATE + " ASC");
            if (cur != null) {
                while (cur.moveToNext()) {
                    long id = cur.getLong(0);
                    String number = cur.getString(1);
                    String name = cur.getString(2);
                    long date = cur.getLong(3);
                    if (add(c, "log" + id, number, name, date)) added++;
                    if (date > maxDate) maxDate = date;
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (cur != null) cur.close();
        }
        if (maxDate > since) {
            sp.edit().putLong(KEY_LAST_DATE, maxDate).apply();
        } else if (sp.getLong(KEY_LAST_DATE, 0L) == 0L) {
            sp.edit().putLong(KEY_LAST_DATE, since).apply();
        }
        return added;
    }

    public static boolean looksLikeMissedCall(String text) {
        if (text == null) return false;
        String t = text.toLowerCase();
        return t.contains("missed call") || t.contains("missed voice call")
                || t.contains("\u0C2E\u0C3F\u0C38\u0C4D\u0C21\u0C4D");
    }

    public static boolean looksLikePhoneApp(String pkg) {
        if (pkg == null) return false;
        String p = pkg.toLowerCase();
        return p.contains("dialer") || p.contains("phone") || p.contains("telecom")
                || p.contains("truecaller") || p.contains("incallui") || p.contains("contacts");
    }

    /** Fallback when call-log permission is not given: read the number from the notification text. */
    public static void addFromNotification(Context c, String title, String text, long time) {
        String all = (title == null ? "" : title) + " " + (text == null ? "" : text);
        Matcher m = Pattern.compile("(\\+?\\d[\\d\\s\\-]{8,}\\d)").matcher(all);
        String number = "";
        if (m.find()) number = m.group(1).replaceAll("[^\\d+]", "");
        String name = number.isEmpty() ? (title == null ? "" : title) : "";
        String id = "n" + (time / 60000L) + (number.isEmpty() ? name : number);
        add(c, id, number, name, time);
    }
}
