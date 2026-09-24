#!/usr/bin/env bash
# Run exactly the APKs produced by the build job, avoiding a second compilation.
set -euo pipefail
mkdir -p device-reports
adb install -r apks/app/app-debug.apk
adb install -r apks/test/app-debug-androidTest.apk
adb logcat -c
set +e
adb shell am instrument -w com.streamvault.test/androidx.test.runner.AndroidJUnitRunner > device-reports/instrumentation.txt 2>&1
runner_result=$?
cat device-reports/instrumentation.txt
adb logcat -d > device-reports/logcat.txt
set -e
python3 - "$runner_result" <<'PY'
import os, re, sys
from pathlib import Path
text = Path('device-reports/instrumentation.txt').read_text()
match = re.search(r'OK \((\d+) tests?\)', text)
passed = match is not None and int(match[1]) > 0 and int(sys.argv[1]) == 0
if re.search(r'INSTRUMENTATION_STATUS_CODE: -(1|2)\b|FAILURES!!!|INSTRUMENTATION_FAILED|shortMsg=', text):
    passed = False
if passed:
    message = f'Instrumented tests: {match[1]} passed on Android 15 / API 35'
    print('::notice::' + message)
else:
    message = 'Instrumented tests failed; see device-reports/instrumentation.txt and logcat.txt'
    print('::error::' + message)
    # Include actual assertions/stack traces in check annotations for remote diagnostics.
    for part in re.findall(r'INSTRUMENTATION_STATUS: stack=(.*?)(?=INSTRUMENTATION_STATUS:|INSTRUMENTATION_STATUS_CODE:|\Z)', text, re.S)[:10]:
        print('::error::' + part[:3500].replace('%', '%25').replace('\n', '%0A').replace('\r', '%0D'))
if 'GITHUB_STEP_SUMMARY' in os.environ:
    with open(os.environ['GITHUB_STEP_SUMMARY'], 'a') as out:
        out.write('## Android device tests\n' + message + '\n')
sys.exit(0 if passed else 1)
PY
