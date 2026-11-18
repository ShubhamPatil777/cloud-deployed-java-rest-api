#!/bin/sh
set -eu
exec java ${JAVA_OPTS:--XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/urandom} -jar /app/app.jar
