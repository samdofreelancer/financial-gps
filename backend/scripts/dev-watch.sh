#!/bin/sh
set -eu

snapshot() {
  {
    find src -type f -printf '%T@ %p\n'
    stat -c '%Y %n' pom.xml
  } 2>/dev/null | sort
}

mvn -q -DskipTests compile

(
  previous="$(snapshot)"
  while :; do
    current="$(snapshot)"
    if [ "$current" != "$previous" ]; then
      if mvn -q -DskipTests compile; then
        printf '%s\n' '[dev-watch] backend compiled; DevTools will restart the application.'
      else
        printf '%s\n' '[dev-watch] compilation failed; waiting for the next source change.' >&2
      fi
      previous="$current"
    fi
    sleep 1
done
) &
watcher_pid=$!

mvn spring-boot:run &
application_pid=$!
trap 'kill "$watcher_pid" 2>/dev/null || true' EXIT INT TERM
wait "$application_pid"
