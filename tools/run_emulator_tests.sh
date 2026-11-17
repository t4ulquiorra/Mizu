#!/usr/bin/env bash
set -e

mkdir -p ui-screenshots

echo "=== Waiting for device to finish booting ==="
adb wait-for-device
while [ "$(adb shell getprop sys.boot_completed | tr -d '\r')" != "1" ]; do
  sleep 2
done

echo "=== Enabling adb root and waiting for storage readiness ==="
adb root || true
adb wait-for-device
while [ "$(adb shell getprop sys.boot_completed | tr -d '\r')" != "1" ]; do
  sleep 2
done

for i in $(seq 1 30); do
  if adb shell "mkdir -p /sdcard/Movies /sdcard/Music && touch /sdcard/Movies/.test && rm -f /sdcard/Movies/.test"; then
    echo "Storage is ready!"
    break
  fi
  echo "Waiting for /sdcard storage..."
  sleep 2
done

echo "=== Pushing test media to emulator /sdcard/ ==="
if [ -d /tmp/mizu_test_media ]; then
  adb push /tmp/mizu_test_media/t_mp4.mp4 /sdcard/Movies/ || true
  adb push /tmp/mizu_test_media/t_mp4.srt /sdcard/Movies/ || true
  adb push /tmp/mizu_test_media/t_mkv.mkv /sdcard/Movies/ || true
  adb push /tmp/mizu_test_media/t_avi.avi /sdcard/Movies/ || true
  adb push /tmp/mizu_test_media/t_audio.mp3 /sdcard/Music/ || true
  adb shell chmod -R 777 /sdcard/Movies /sdcard/Music || true

  echo "=== Scanning test media into MediaStore ==="
  adb shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file:///sdcard/Movies/t_mp4.mp4 || true
  adb shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file:///sdcard/Movies/t_mkv.mkv || true
  adb shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file:///sdcard/Movies/t_avi.avi || true
  adb shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file:///sdcard/Music/t_audio.mp3 || true
fi

echo "=== Testing Upgrade from v0.1.0 to Newly Built Release APK ==="
mkdir -p /tmp/prev_release
if gh release download v0.1.0 -p "app-x86_64-release.apk" -R t4ulquiorra/Mizu --dir /tmp/prev_release; then
  echo "Downloaded previous release v0.1.0"
  adb install /tmp/prev_release/app-x86_64-release.apk
  echo "Installed v0.1.0 successfully. Now upgrading to new release..."
  if [ -f app/build/outputs/apk/release/app-x86_64-release.apk ]; then
    adb install -r app/build/outputs/apk/release/app-x86_64-release.apk
  elif [ -f app/build/outputs/apk/release/app-universal-release.apk ]; then
    adb install -r app/build/outputs/apk/release/app-universal-release.apk
  fi
  echo "Upgrade succeeded!"
  adb uninstall com.mizu.player || true
else
  echo "Previous release download skipped or not found."
fi

echo "=== Building and installing debug APK ==="
./gradlew assembleDebug --stacktrace
adb install -r app/build/outputs/apk/debug/app-universal-debug.apk || adb install -r app/build/outputs/apk/debug/app-x86_64-debug.apk || adb install -r app/build/outputs/apk/debug/*.apk

echo "=== Pre-granting media permissions for instrumented test suite ==="
adb shell pm grant com.mizu.player.debug android.permission.READ_MEDIA_VIDEO || true
adb shell pm grant com.mizu.player.debug android.permission.READ_MEDIA_AUDIO || true
adb shell pm grant com.mizu.player.debug android.permission.READ_MEDIA_VISUAL_USER_SELECTED || true

echo "=== Running Connected Android Tests ==="
TEST_STATUS=0
./gradlew connectedDebugAndroidTest --stacktrace || TEST_STATUS=$?

echo "=== Capturing UI screenshot and logcat from emulator ==="
adb exec-out screencap -p > ui-screenshots/emulator_post_test.png || true
adb logcat -d > ui-screenshots/emulator_logcat.txt || true

if [ $TEST_STATUS -ne 0 ]; then
  echo "Instrumented tests failed with code $TEST_STATUS"
  exit $TEST_STATUS
fi

echo "=== Emulator test execution finished successfully ==="
