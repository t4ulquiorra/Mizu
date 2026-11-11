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
adb push /tmp/mizu_test_media/t_mp4.mp4 /sdcard/Movies/
adb push /tmp/mizu_test_media/t_mp4.srt /sdcard/Movies/
adb push /tmp/mizu_test_media/t_mkv.mkv /sdcard/Movies/
adb push /tmp/mizu_test_media/t_avi.avi /sdcard/Movies/
adb push /tmp/mizu_test_media/t_audio.mp3 /sdcard/Music/
adb shell chmod -R 777 /sdcard/Movies /sdcard/Music

echo "=== Scanning test media into MediaStore ==="
adb shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file:///sdcard/Movies/t_mp4.mp4
adb shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file:///sdcard/Movies/t_mkv.mkv
adb shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file:///sdcard/Movies/t_avi.avi
adb shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file:///sdcard/Music/t_audio.mp3

echo "=== Building and installing debug APK ==="
./gradlew assembleDebug --stacktrace
adb install -r app/build/outputs/apk/debug/app-universal-debug.apk || adb install -r app/build/outputs/apk/debug/app-x86_64-debug.apk || adb install -r app/build/outputs/apk/debug/*.apk

echo "=== Pre-granting media permissions ==="
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
