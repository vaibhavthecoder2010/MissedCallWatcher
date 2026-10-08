# Missed Call Watcher (Sunitha Tailors)

Android app. Lists missed calls automatically; each row has WhatsApp / SMS / Call buttons.

## Build the APK (no Android Studio needed)
1. Create a free account on github.com, make a new repository.
2. Upload ALL files and folders of this project (including the hidden `.github` folder).
3. Open the "Actions" tab -> "Build APK" -> "Run workflow".
4. After ~5 minutes open the finished run -> download "MissedCallWatcher-apk" -> unzip -> app-debug.apk.
5. Copy app-debug.apk to the phone and install it.

(With Android Studio: open this folder, then Build -> Build APK(s).)

## First-time setup on the phone
1. Open app -> button 1 "Grant notification access" -> switch ON "Missed Call Watcher".
   If the switch is greyed out (Android 13+): Settings -> Apps -> Missed Call Watcher -> three dots (top right) -> "Allow restricted settings", then try again.
2. Button 2 "Allow call log" -> Allow.
3. Battery: Settings -> Battery -> Missed Call Watcher -> allow background / no restrictions (and Auto-start ON if your phone has it).
4. Test: give a missed call from another phone. The number appears in the list.

## Change the message
Edit `MESSAGE` in `app/src/main/java/com/sunithatailors/missedcallwatcher/Store.java`.
