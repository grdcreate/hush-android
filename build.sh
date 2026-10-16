#!/bin/bash
# Hush — manual APK build pipeline (no Gradle needed)
set -e
SDK=/home/user/sdk
BT=$SDK/bt/android-14
PLAT=$SDK/plat/android-34/android.jar
SRC=/home/user/hush
BUILD=/home/user/hush-build
OUT=/home/user/release

rm -rf "$BUILD"
mkdir -p "$BUILD/gen" "$BUILD/classes" "$BUILD/dex" "$OUT"

echo ">> aapt2 compile"
$BT/aapt2 compile --dir "$SRC/res" -o "$BUILD/res.zip"

echo ">> aapt2 link"
$BT/aapt2 link -o "$BUILD/base.apk" \
  -I "$PLAT" \
  --manifest "$SRC/AndroidManifest.xml" \
  --java "$BUILD/gen" \
  -A "$SRC/assets" \
  -R "$BUILD/res.zip" \
  --auto-add-overlay

echo ">> javac"
find "$BUILD/gen" "$SRC/src" -name '*.java' > "$BUILD/sources.txt"
javac -nowarn -encoding UTF-8 -Xlint:-options,-path \
  -source 11 -target 11 \
  -classpath "$PLAT" \
  -d "$BUILD/classes" \
  @"$BUILD/sources.txt"

echo ">> d8"
$BT/d8 --release --lib "$PLAT" --min-api 26 --output "$BUILD/dex" \
  $(find "$BUILD/classes" -name '*.class')

echo ">> package dex"
cd "$BUILD/dex"
zip -q "$BUILD/base.apk" classes.dex
cd /home/user

echo ">> zipalign"
$BT/zipalign -f 4 "$BUILD/base.apk" "$BUILD/aligned.apk"

echo ">> keys"
mkdir -p /home/user/keys
if [ ! -f /home/user/keys/key.pk8 ]; then
  openssl req -x509 -newkey rsa:2048 -keyout /home/user/keys/key.pem \
    -out /home/user/keys/cert.pem -days 3650 -nodes \
    -subj "/CN=Hush/OU=Aurora/O=Aurora/L=Guwahati/ST=Assam/C=IN" 2>/dev/null
  openssl pkcs8 -topk8 -nocrypt -outform DER -in /home/user/keys/key.pem -out /home/user/keys/key.pk8
fi

echo ">> sign"
$BT/apksigner sign --key /home/user/keys/key.pk8 --cert /home/user/keys/cert.pem \
  --out "$OUT/Hush-v1.1.0.apk" "$BUILD/aligned.apk"

echo ">> verify"
$BT/apksigner verify --verbose "$OUT/Hush-v1.1.0.apk" | head -4
$BT/aapt2 dump badging "$OUT/Hush-v1.1.0.apk" | head -6
ls -la "$OUT"
