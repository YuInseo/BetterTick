#!/usr/bin/env bash
set +e
adb shell settings put secure show_ime_with_hard_keyboard 1
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.bettertick.ui.screens.tasks.components.EditorImeRegressionTest --build-cache --console=plain
ui_result=$?
mkdir -p app/build/reports/ui-regression
adb pull /data/local/tmp/editor-ui/. app/build/reports/ui-regression/ || true
python3 scripts/emit-ui-snapshots.py
exit "$ui_result"
