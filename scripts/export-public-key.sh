#!/usr/bin/env bash
set -euo pipefail
KEY="${HOME}/.fortress/device_agent_public.pem"
if [[ ! -f "$KEY" ]]; then
  echo "Public key not found. Start the agent once to generate keys: ./scripts/start-agent.sh"
  exit 1
fi
echo "This machine's public key (registered automatically on login via publicKey in /device-id):"
cat "$KEY"
