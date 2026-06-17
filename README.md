# Fortress Device Agent

Local HTTP service that exposes a signed hardware serial for Fortress login and device binding.

## End users (installers)

Download the installer for your OS from the release artifacts:

| OS | File |
|----|------|
| Windows | `FortressDeviceAgent-*-windows-x64.msi` |
| macOS | `FortressDeviceAgent-*-macos-aarch64.dmg` |
| Linux | `FortressDeviceAgent-*-linux-amd64.deb` |

1. Run the installer (double-click).
2. The agent starts automatically and listens on `http://127.0.0.1:4567/device-id`.
3. Log in to Fortress in the browser on the same machine.

Auto-start after reboot/login:

| OS | Mechanism |
|----|-----------|
| Windows | Registry `Run` key (set by MSI installer) |
| macOS | LaunchAgent (registered on first agent launch, or via script below) |
| Linux | systemd service (enabled by `.deb` post-install) |

**macOS post-install (if auto-start did not register):**
```bash
chmod +x installer/post-install-macos.sh
./installer/post-install-macos.sh
```

Or launch the app once from `/Applications` — the agent registers itself for future logins.

## Developers

Requirements: JDK 17+, Maven 3.8+

```bash
./scripts/start-agent.sh
curl http://127.0.0.1:4567/device-id
```

Each agent registers its own public key with the backend on first login (`publicKey` in `/device-id` response). No shared backend env key is required.

Build native installer (run on target OS):

```bash
./scripts/build-installer.sh 1.0.0
```

## API

`GET http://127.0.0.1:4567/device-id`

```json
{
  "deviceSerial": "C02XXXXXX",
  "timestamp": 1716200000123,
  "signature": "<base64>"
}
```

Keys are stored in `~/.fortress/`.
