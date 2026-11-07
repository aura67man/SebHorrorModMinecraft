#!/usr/bin/env bash
# Takes in-game screenshots of zinc ore in stone/deepslate next to vanilla ores (run/screenshots/).
set -uo pipefail
cd "$(dirname "$0")/.."
export DISPLAY=:99
Xvfb :99 -screen 0 1280x720x24 -nolisten tcp >/dev/null 2>&1 &
XVFB_PID=$!
sleep 2
mkdir -p run && touch run/options.txt
sed -i 's/^ao:.*/ao:true/' run/options.txt; grep -q '^ao:' run/options.txt || echo 'ao:true' >> run/options.txt
rm -rf run/screenshots run/demo_markers.txt
SEB_DEMO=ores ./gradlew runClient --console=plain > run/ores_client.log 2>&1
kill "$XVFB_PID" 2>/dev/null
ls run/screenshots
