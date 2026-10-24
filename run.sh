#!/usr/bin/env sh
# Builds (when a JDK is available) and launches the simulator.
cd "$(dirname "$0")"
if command -v javac >/dev/null 2>&1; then
    ./build.sh || exit 1
fi
exec java -jar dist/LogicGateSimulator.jar
