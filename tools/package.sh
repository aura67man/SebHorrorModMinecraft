#!/usr/bin/env bash
# Builds dist/SebastianHiliHorror.zip: jar + skin + demo video + README + full source.
set -euo pipefail
cd "$(dirname "$0")/.."
VIDEO="${1:-demo/SebastianHiliHorror_demo.mp4}"
./gradlew build --console=plain -q
STAGE="$(mktemp -d)/SebastianHiliHorror"
mkdir -p "$STAGE/source"
cp build/libs/SebastianHiliHorror-1.20.1-1.0.0.jar "$STAGE/"
cp SebastianHili_skin.png README.md "$STAGE/"
[ -f "$VIDEO" ] && cp "$VIDEO" "$STAGE/SebastianHiliHorror_demo.mp4"
[ -f demo/SebastianHili_BossFight.mp4 ] && cp demo/SebastianHili_BossFight.mp4 "$STAGE/"
# source = everything git tracks (no build output, no run folder, no reference photo)
git ls-files -z | xargs -0 -I{} cp --parents {} "$STAGE/source/"
# the licensed song is deliberately not in git, but this personal zip includes it so the source builds as-is
MUSIC=src/main/resources/assets/sebastianhilihorror/sounds/music
[ -d "$MUSIC" ] && mkdir -p "$STAGE/source/$MUSIC" && cp "$MUSIC"/*.ogg "$STAGE/source/$MUSIC/"
mkdir -p dist
rm -f dist/SebastianHiliHorror.zip
(cd "$(dirname "$STAGE")" && zip -qr -X "$OLDPWD/dist/SebastianHiliHorror.zip" SebastianHiliHorror)
ls -la dist/SebastianHiliHorror.zip
