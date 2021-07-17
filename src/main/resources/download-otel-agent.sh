#!/bin/sh
basename="$(dirname $0)"
projectDir="${basename}/../../.."

cd "$projectDir" || exit 1
mkdir -p build/classes/java/main
cd build/classes/java/main || exit 1

agentname=opentelemetry-javaagent-all.jar

if [ ! -f "$agentname" ]; then
  curl --location \
    --remote-name \
    --remote-header-name \
    --compressed \
    "https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/$agentname"
fi
