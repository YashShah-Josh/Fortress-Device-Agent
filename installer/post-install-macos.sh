#!/usr/bin/env bash
set -euo pipefail
PLIST_DST="${HOME}/Library/LaunchAgents/com.fortress.device-agent.plist"
APP_EXE="/Applications/FortressDeviceAgent.app/Contents/MacOS/FortressDeviceAgent"
LABEL="com.fortress.device-agent"

if [[ ! -x "$APP_EXE" ]]; then
  echo "Fortress Device Agent not found at $APP_EXE" >&2
  echo "Install the app to /Applications first." >&2
  exit 1
fi

mkdir -p "${HOME}/Library/LaunchAgents"
cat > "$PLIST_DST" << EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>Label</key>
    <string>${LABEL}</string>
    <key>ProgramArguments</key>
    <array>
        <string>${APP_EXE}</string>
    </array>
    <key>RunAtLoad</key>
    <true/>
    <key>KeepAlive</key>
    <true/>
    <key>StandardOutPath</key>
    <string>/tmp/fortress-device-agent.log</string>
    <key>StandardErrorPath</key>
    <string>/tmp/fortress-device-agent.err</string>
</dict>
</plist>
EOF

GUI_DOMAIN="gui/$(id -u)"
launchctl bootout "${GUI_DOMAIN}/${LABEL}" 2>/dev/null || true
launchctl bootstrap "$GUI_DOMAIN" "$PLIST_DST"
echo "Fortress Device Agent registered for auto-start."
