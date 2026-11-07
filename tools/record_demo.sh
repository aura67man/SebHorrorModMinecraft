#!/usr/bin/env bash
# Records the scripted demo video on a headless Linux box (or any X11 desktop).
#   needs: Xvfb, ffmpeg (libx264), pulseaudio (optional, for sound), JDK 17
#   usage: tools/record_demo.sh [output.mp4]
#          SEB_DEMO=boss CRF=26 tools/record_demo.sh demo/boss_fight.mp4   (the boss fight video)
set -uo pipefail
cd "$(dirname "$0")/.."
OUT="${1:-demo/SebastianHiliHorror_demo.mp4}"
mkdir -p "$(dirname "$OUT")" run/config
RAW="$(mktemp -d)/raw.mkv"

if [ -z "${DISPLAY:-}" ] || [ "${USE_XVFB:-1}" = "1" ]; then
  export DISPLAY=:99
  Xvfb :99 -screen 0 1280x720x24 -nolisten tcp >/dev/null 2>&1 &
  XVFB_PID=$!
  sleep 2
fi

AUDIO_ARGS=()
if command -v pulseaudio >/dev/null; then
  pulseaudio --start --exit-idle-time=-1 >/dev/null 2>&1 || true
  pactl load-module module-null-sink sink_name=rec >/dev/null 2>&1 || true
  pactl set-default-sink rec >/dev/null 2>&1 || true
  AUDIO_ARGS=(-f pulse -thread_queue_size 1024 -i rec.monitor)
fi

# Demo tuning: no surprise arrivals, fast "look away" relocations, short beg cooldown.
cat > run/config/sebastianhilihorror-common.toml <<'TOML'
[features]
	naturalSpawning = false
	hints = true
[ranges]
	watchMinDistance = 9.0
	watchMaxDistance = 15.0
[timers]
	begTimeoutSeconds = 6
	begCooldownSeconds = 3
	searchIntervalMinMinutes = 600
	searchIntervalMaxMinutes = 600
	answerTimeoutSeconds = 30
	rummageIntervalMinSeconds = 3600
	rummageIntervalMaxSeconds = 3600
	relocateIntervalMinSeconds = 3
	relocateIntervalMaxSeconds = 4
	rageWindupSeconds = 3
	chargeBlinkSeconds = 6
TOML
rm -f run/demo_markers.txt

REC_START=$(date +%s%3N)
ffmpeg -hide_banner -loglevel error -y -f x11grab -framerate 30 -video_size 1280x720 -draw_mouse 0 -thread_queue_size 1024 -i "$DISPLAY" \
  "${AUDIO_ARGS[@]}" -c:v libx264 -preset ultrafast -crf 18 -pix_fmt yuv420p -c:a aac -b:a 160k "$RAW" &
FFMPEG_PID=$!

SEB_DEMO="${SEB_DEMO:-true}" ./gradlew runClient --console=plain > run/demo_client.log 2>&1
kill -INT "$FFMPEG_PID" 2>/dev/null; wait "$FFMPEG_PID" 2>/dev/null
[ -n "${XVFB_PID:-}" ] && kill "$XVFB_PID" 2>/dev/null

if [ ! -f run/demo_markers.txt ]; then
  echo "The demo never started (see run/demo_client.log). Raw capture: $RAW"; exit 1
fi
START=$(awk '/^start/{print $2}' run/demo_markers.txt | tail -1)
END=$(awk '/^end/{print $2}' run/demo_markers.txt | tail -1)
[ -z "$END" ] && END=$(date +%s%3N)
SS=$(python3 -c "print(max(0,($START-$REC_START)/1000-0.3))")
DUR=$(python3 -c "print(($END-$START)/1000+0.8)")
ffmpeg -hide_banner -loglevel error -y -ss "$SS" -i "$RAW" -t "$DUR" -c:v libx264 -preset slow -crf "${CRF:-22}" -pix_fmt yuv420p \
  -movflags +faststart -c:a aac -b:a 160k "$OUT"
echo "Wrote $OUT ($(python3 -c "print(round($DUR))")s)"
