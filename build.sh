#!/usr/bin/env sh
# Compiles the project and packages dist/LogicGateSimulator.jar (needs a JDK 11+).
set -e
cd "$(dirname "$0")"
rm -rf out
mkdir -p out dist
javac -encoding UTF-8 -sourcepath src -d out src/logicsim/LogicGateSimulator.java
jar --create --file dist/LogicGateSimulator.jar --main-class logicsim.LogicGateSimulator -C out .
echo "Built dist/LogicGateSimulator.jar"
