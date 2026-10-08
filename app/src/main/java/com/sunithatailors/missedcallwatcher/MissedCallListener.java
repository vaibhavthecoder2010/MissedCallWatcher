package com.sunithatailors.missedcallwatcher;

import android.app.Notification;
import android.os.Handler;
import android.os.Looper;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

/** Wakes up when a "missed call" notification appears and records the number. */
public class MissedCallListener extends NotificationListenerService {

    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public void onListenerConnected() {
        Store.syncCallLog(getApplicationContext());
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        try {
            if (sbn == null) return;
            String pkg = sbn.getPackageName();
            if (pkg == null || pkg.equals(getPackageName())) return;

            Notification n = sbn.getNotification();
            if (n == null || n.extras == null) return;
            CharSequence t = n.extras.getCharSequence(Notification.EXTRA_TITLE);
            CharSequence x = n.extras.getCharSequence(Notification.EXTRA_TEXT);
            CharSequence b = n.extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
            String title = t == null ? "" : t.toString();
            String text = x == null ? "" : x.toString();
            String big = b == null ? "" : b.toString();
            String all = title + " " + text + " " + big;

            if (!Store.looksLikePhoneApp(pkg) || !Store.looksLikeMissedCall(all)) return;

            final android.content.Context ctx = getApplicationContext();
            if (Store.hasCallLogPermission(ctx)) {
                // The call log can lag a moment behind the notification, so read twice.
                Store.syncCallLog(ctx);
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Store.syncCallLog(ctx);
                    }
                }, 3000);
            } else {
                Store.addFromNotification(ctx, title, text + " " + big, sbn.getPostTime());
            }
        } catch (Exception ignored) {
        }
    }
}
