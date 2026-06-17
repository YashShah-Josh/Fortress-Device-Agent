#!/usr/bin/env bash
# Build native installer with jpackage (run on target OS).
set -euo pipefail
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
VERSION="${1:-1.0.0}"
cd "$DIR"
mvn -q package -DskipTests
JAR="$DIR/target/fortress-device-agent.jar"
INPUT="$DIR/target/jpackage-input"
rm -rf "$INPUT"
mkdir -p "$INPUT"
cp "$JAR" "$INPUT/"

OS="$(uname -s)"
case "$OS" in
  Darwin)
    jpackage \
      --name FortressDeviceAgent \
      --input "$INPUT" \
      --main-jar fortress-device-agent.jar \
      --main-class com.fortress.deviceagent.Main \
      --type dmg \
      --app-version "$VERSION" \
      --vendor Fortress \
      --dest "$DIR/target/dist"
    ;;
  Linux)
    chmod +x "$DIR/installer/jpackage/linux/postinst" "$DIR/installer/jpackage/linux/prerm"
    jpackage \
      --name fortress-device-agent \
      --input "$INPUT" \
      --main-jar fortress-device-agent.jar \
      --main-class com.fortress.deviceagent.Main \
      --type deb \
      --app-version "$VERSION" \
      --vendor Fortress \
      --dest "$DIR/target/dist" \
      --linux-shortcut \
      --resource-dir "$DIR/installer/jpackage/linux"
    ;;
  *)
    echo "On Windows run scripts/build-installer.bat"
    exit 1
    ;;
esac
echo "Installer written to $DIR/target/dist"
