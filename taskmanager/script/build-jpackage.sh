#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."
mvn -pl desktop-app -am -DskipTests package jpackage:jpackage

