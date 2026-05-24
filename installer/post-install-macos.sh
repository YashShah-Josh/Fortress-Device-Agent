#!/usr/bin/env bash
set -euo pipefail
PLIST_SRC="$(cd "$(dirname "$0")" && pwd)/macos/com.fortress.device-agent.plist"
PLIST_DST="${HOME}/Library/LaunchAgents/com.fortress.device-agent.plist"
mkdir -p "${HOME}/Library/LaunchAgents"
cp "$PLIST_SRC" "$PLIST_DST"
launchctl unload "$PLIST_DST" 2>/dev/null || true
launchctl load "$PLIST_DST"
echo "Fortress Device Agent registered for auto-start."
