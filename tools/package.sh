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
# source = everything git tracks (no build output, no run folder, no reference photo)
git ls-files -z | xargs -0 -I{} cp --parents {} "$STAGE/source/"
mkdir -p dist
rm -f dist/SebastianHiliHorror.zip
(cd "$(dirname "$STAGE")" && zip -qr -X "$OLDPWD/dist/SebastianHiliHorror.zip" SebastianHiliHorror)
ls -la dist/SebastianHiliHorror.zip
