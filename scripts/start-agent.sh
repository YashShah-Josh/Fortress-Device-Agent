#!/usr/bin/env bash
set -euo pipefail
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
JAR="${DIR}/target/fortress-device-agent.jar"
if [[ ! -f "$JAR" ]]; then
  echo "Building agent..."
  (cd "$DIR" && mvn -q package -DskipTests)
fi
exec java -jar "$JAR" "$@"
