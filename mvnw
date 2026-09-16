#!/bin/sh
set -eu

PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
MAVEN_VERSION=3.9.16
MAVEN_DIR="$PROJECT_DIR/.tools/apache-maven-$MAVEN_VERSION"

. "$PROJECT_DIR/scripts/lib/tools.sh"

ensure_jdk

if command -v mvn >/dev/null 2>&1; then
  exec mvn "$@"
fi

if [ ! -x "$MAVEN_DIR/bin/mvn" ]; then
  ARCHIVE="$TOOLS_DIR/apache-maven-$MAVEN_VERSION-bin.tar.gz"
  CHECKSUM="$ARCHIVE.sha512"
  BASE_URL="https://dlcdn.apache.org/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz"
  mkdir -p "$TOOLS_DIR"
  echo "Downloading Apache Maven $MAVEN_VERSION..."
  download "$BASE_URL" "$ARCHIVE"
  download "$BASE_URL.sha512" "$CHECKSUM"
  verify_sha512 "$ARCHIVE" "$CHECKSUM"
  tar -xzf "$ARCHIVE" -C "$TOOLS_DIR"
  rm -f "$ARCHIVE" "$CHECKSUM"
fi

exec "$MAVEN_DIR/bin/mvn" "$@"
