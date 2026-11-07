#!/usr/bin/env bash
# Synthesizes the mod's original sound effects with ffmpeg (mono OGG Vorbis).
set -euo pipefail
OUT="src/main/resources/assets/sebastianhilihorror/sounds"
mkdir -p "$OUT"
ff() { ffmpeg -hide_banner -loglevel error -y "$@"; }
# Two low thumps: lub-dub.
ff -f lavfi -i "aevalsrc='0.9*sin(2*PI*55*t)*exp(-18*t)+0.7*sin(2*PI*48*(t-0.28))*exp(-20*(t-0.28))*gte(t,0.28)':s=44100:d=0.9" \
   -ac 1 -c:a libvorbis -q:a 4 "$OUT/heartbeat.ogg"
# Breathy whisper: band-passed noise with a syllable-like tremolo.
ff -f lavfi -i "anoisesrc=d=1.8:c=pink:a=0.6" -af "bandpass=f=2600:w=1800,tremolo=f=5.5:d=0.85,afade=t=in:d=0.25,afade=t=out:st=1.3:d=0.5,volume=2.2" \
   -ac 1 -c:a libvorbis -q:a 4 "$OUT/whisper.ogg"
# Rage scream: detuned saws sweeping down + noise, heavily overdriven.
ff -f lavfi -i "aevalsrc='0.5*sin(2*PI*(420-140*t)*t+3*sin(2*PI*7*t))+0.4*sin(2*PI*(433-150*t)*t)+0.25*(random(0)-0.5)':s=44100:d=2.2" \
   -af "acrusher=bits=6:mix=0.6,highpass=f=180,lowpass=f=5000,afade=t=in:d=0.05,afade=t=out:st=1.5:d=0.7,volume=1.6" \
   -ac 1 -c:a libvorbis -q:a 4 "$OUT/rage_scream.ogg"
# Penny clink: inharmonic metallic partials with a fast decay.
ff -f lavfi -i "aevalsrc='(0.5*sin(2*PI*3150*t)+0.35*sin(2*PI*4720*t)+0.25*sin(2*PI*6930*t)+0.2*sin(2*PI*2210*t))*exp(-9*t)':s=44100:d=0.7" \
   -ac 1 -c:a libvorbis -q:a 4 "$OUT/penny_clink.ogg"
# Sting: low drone swell with a dissonant top note.
ff -f lavfi -i "aevalsrc='(0.5*sin(2*PI*41*t)+0.3*sin(2*PI*82.5*t)+0.18*sin(2*PI*1243*t)*sin(2*PI*0.7*t))*min(1,t*1.5)*exp(-0.6*t)':s=44100:d=3" \
   -af "afade=t=out:st=2.2:d=0.8" -ac 1 -c:a libvorbis -q:a 4 "$OUT/sting.ogg"
ls -la "$OUT"
