#!/usr/bin/env bash
set +e
adb shell settings put secure show_ime_with_hard_keyboard 1
# The keyboard app on a fresh emulator can still be starting when the tests
# begin; then no keyboard ever appears. Wait for a default IME and select it.
ime=""
for _ in $(seq 1 45); do
  ime=$(adb shell settings get secure default_input_method | tr -d '\r')
  if [ -n "$ime" ] && [ "$ime" != "null" ] && adb shell ime list -s | tr -d '\r' | grep -qx "$ime"; then break; fi
  sleep 2
done
echo "default IME: ${ime:-none}"
adb shell ime list -s
if [ -n "$ime" ] && [ "$ime" != "null" ]; then
  adb shell ime enable "$ime"
  adb shell ime set "$ime"
fi
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.bettertick.ui.screens.tasks.components.EditorImeRegressionTest --build-cache --console=plain
ui_result=$?
mkdir -p app/build/reports/ui-regression
adb pull /data/local/tmp/editor-ui/. app/build/reports/ui-regression/ || true
python3 scripts/emit-ui-snapshots.py
exit "$ui_result"
