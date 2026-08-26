# Testing on a physical phone

This project has no emulator/CI test harness set up — testing means building the
debug APK and driving it on a real device over `adb`. This doc captures the
workflow used to test on a Pixel 9 Pro over USB.

## 1. One-time setup

**Locate the Android SDK / adb.** `local.properties` points at the SDK:

```bash
cat local.properties   # sdk.dir=/opt/homebrew/share/android-commandlinetools
```

`adb` lives under `<sdk.dir>/platform-tools/adb`. Put it on `PATH` for the session:

```bash
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
export PATH="$ANDROID_HOME/platform-tools:$PATH"
```

**Connect the device.** Plug in over USB with USB debugging enabled (Settings →
Developer options), then confirm:

```bash
adb devices -l
# List of devices attached
# 53171FDAP001AW   device usb:... product:caiman model:Pixel_9_Pro ...
```

If it shows `unauthorized`, accept the RSA fingerprint prompt on the phone.

## 2. Build and install

```bash
cd "/Users/satvikm/Documents/claude projects/minimalist launcher"
./gradlew :app:installDebug
```

This builds and pushes the debug APK to the connected device in one step (no
need for a separate `assembleDebug` + manual `adb install`). Package id is
`com.satvikm.quiet`.

Re-running `installDebug` after a code change **preserves app data** (Room DB,
DataStore) — it's a normal APK update, not a fresh install.

## 3. Launch and grant permissions

```bash
adb shell am start -n com.satvikm.quiet/.MainActivity
```

Quiet is a launcher, so it replaces the home screen — walk through onboarding
once to set it as default and grant the optional permissions (Usage access,
Accessibility, Notification access, and on Android 13+, POST_NOTIFICATIONS for
the focus-recap feature). These are OS-level grants tied to the package, not
app data, so they normally survive app updates.

**Important:** they do *not* survive `pm clear`. If you need to reset app state
mid-session, prefer reinstalling (`installDebug` again, or uninstall +
reinstall) over `pm clear` — clearing data on this device also revoked the
accessibility service, the notification listener, and the POST_NOTIFICATIONS
grant, all of which then had to be re-granted before testing could continue.
If you do end up clearing data, re-grant by hand through the onboarding flow
or system Settings — writing directly to `settings put secure
enabled_accessibility_services` did not reliably take effect on this device;
toggling it through the real Settings UI did.

## 4. Driving the UI over adb

There's no Espresso/UI test suite here, so interaction is manual, via
screenshots + taps:

```bash
# Screenshot to a local file
adb exec-out screencap -p > /tmp/screen.png
# then Read the file to see it

# Tap / swipe (coordinates are physical pixels, from `adb shell wm size`)
adb shell input tap <x> <y>
adb shell input swipe <x1> <y1> <x2> <y2> <duration_ms>
adb shell input text "some text"        # types into a focused field
adb shell input keyevent KEYCODE_BACK   # KEYCODE_HOME, KEYCODE_ENTER, etc.
```

**Don't eyeball tap coordinates from a screenshot — dump the UI tree instead.**
Screenshots get downscaled for display, and misjudging pixel positions wastes
a lot of round-trips. Get exact element bounds first:

```bash
adb shell uiautomator dump /sdcard/window_dump.xml
adb pull /sdcard/window_dump.xml /tmp/dump.xml
grep -o 'text="Focus now"[^>]*bounds="[^"]*"' /tmp/dump.xml
# bounds="[387,1935][573,2043]" -> tap the center: (480, 1989)
```

or dump every labeled node at once:

```bash
python3 -c "
import re
data = open('/tmp/dump.xml').read()
for m in re.finditer(r'<node[^>]*text=\"([^\"]*)\"[^>]*bounds=\"([^\"]*)\"', data):
    if m.group(1).strip():
        print(m.group(1), m.group(2))
"
```

**Launching another app to test friction/blocking:**

```bash
adb shell monkey -p com.android.chrome -c android.intent.category.LAUNCHER 1
```

Quiet's accessibility service intercepts this the same way it would a real
tap in the drawer, so it's the right way to test the friction screen.

**Checking logs / crashes:**

```bash
adb logcat -c                                    # clear buffer first
# ... do the thing ...
adb logcat -d | grep -i "satvikm\|AndroidRuntime\|FATAL"
```

**Checking notifications** (e.g. the focus-recap notification):

```bash
adb shell cmd statusbar expand-notifications
adb exec-out screencap -p > /tmp/shade.png
adb shell input keyevent KEYCODE_BACK   # collapse again
```

## 5. Gotchas hit in practice

- **Screensaver/Dream interrupts idle gaps.** If the device sits idle between
  commands (e.g. while you're reading a screenshot), a Dream/ambient screen
  can take over. A plain `adb shell input tap` dismisses it — no need to
  wake/unlock the device yourself unless it's actually locked.
- **`pm clear` is destructive beyond app data** — see the permissions note
  above. Prefer it only when you genuinely want a from-scratch install state,
  and expect to re-grant OS permissions afterward.
- **Multi-user profile noise:** `dumpsys` commands may throw
  `SecurityException: Shell does not have permission to access user 11` if a
  secondary profile exists on the device — harmless, just target user 0
  implicitly (the default) rather than passing `--user`.
- **A commitment-locked focus session cannot be shortcut.** If you start one
  with "Can't end early" on, the only way out before the timer is up is to
  wait it out or clear app data — that's the feature working as designed, not
  a bug to work around mid-test.
