#!/bin/sh
set -eu

snapshot() {
  {
    find src -type f -printf '%T@ %p\n'
    stat -c '%Y %n' pom.xml
  } 2>/dev/null | sort
}

# Start from a complete classpath; the clean build also removes partial class files left by
# an interrupted or concurrent Maven compile.
mvn -q -Dmaven.test.skip=true clean compile
mkdir -p target/classes
touch target/classes/.reloadtrigger

(
  previous="$(snapshot)"
  while :; do
    current="$(snapshot)"
    if [ "$current" != "$previous" ]; then
      # Let editor save bursts settle before compiling, then trigger exactly one restart only
      # after javac has finished writing every class file.
      sleep 1
      current="$(snapshot)"
      if [ "$current" != "$previous" ] && mvn -q -Dmaven.test.skip=true compile; then
        touch target/classes/.reloadtrigger
        printf '%s\n' '[dev-watch] backend compiled; DevTools will restart the application.'
      elif [ "$current" != "$previous" ]; then
        printf '%s\n' '[dev-watch] compilation failed; waiting for the next source change.' >&2
      fi
      previous="$current"
    fi
    sleep 1
  done
) &
watcher_pid=$!

mvn -q -Dmaven.test.skip=true spring-boot:run &
application_pid=$!
trap 'kill "$watcher_pid" 2>/dev/null || true' EXIT INT TERM
wait "$application_pid"
