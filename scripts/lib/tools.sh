#!/bin/sh
# Shared helpers for the local build-tool bootstrap scripts.
#
# Sourced by ./mvnw and scripts/run-tomcat.sh. The caller must set PROJECT_DIR
# before sourcing this file. Downloaded tools live under .tools/, which is
# ignored by Git.

TOOLS_DIR="$PROJECT_DIR/.tools"

JDK_RELEASE="17.0.20.1+1"
JDK_DIR="$TOOLS_DIR/jdk-$JDK_RELEASE"
JDK_ARCHIVE_NAME="OpenJDK17U-jdk_x64_linux_hotspot_17.0.20.1_1.tar.gz"
JDK_URL="https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.20.1%2B1/$JDK_ARCHIVE_NAME"
JDK_SHA256="3808d1d15e3ec6bd5b84057fb5d84c33d8a1536a258146bcea2e603fc726e08e"

download() {
  source_url=$1
  destination=$2
  if command -v curl >/dev/null 2>&1; then
    curl --fail --location --silent --show-error "$source_url" --output "$destination"
  elif command -v wget >/dev/null 2>&1; then
    wget --quiet "$source_url" --output-document="$destination"
  else
    echo "Cannot download $source_url: neither curl nor wget is available." >&2
    exit 1
  fi
}

file_sha256() {
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$1" | awk '{print $1}'
  else
    shasum -a 256 "$1" | awk '{print $1}'
  fi
}

file_sha512() {
  if command -v sha512sum >/dev/null 2>&1; then
    sha512sum "$1" | awk '{print $1}'
  else
    shasum -a 512 "$1" | awk '{print $1}'
  fi
}

# verify_sha256 <archive> <expected-hex-digest>
verify_sha256() {
  if [ "$(file_sha256 "$1")" != "$2" ]; then
    echo "SHA-256 verification failed for $1" >&2
    exit 1
  fi
}

# verify_sha512 <archive> <checksum-file>
verify_sha512() {
  expected=$(awk '{print $1}' "$2")
  if [ "$(file_sha512 "$1")" != "$expected" ]; then
    echo "SHA-512 verification failed for $1" >&2
    exit 1
  fi
}

# Exports JAVA_HOME pointing at a JDK (not just a JRE). Prefers an explicit
# JAVA_HOME, then a javac on PATH, then an Eclipse Temurin JDK downloaded into
# .tools/. Maven needs javac; Tomcat only needs a JRE, but sharing one runtime
# keeps the build and the server consistent.
ensure_jdk() {
  if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/javac" ]; then
    return
  fi
  if command -v javac >/dev/null 2>&1; then
    javac_path=$(readlink -f "$(command -v javac)")
    JAVA_HOME=$(dirname "$(dirname "$javac_path")")
    export JAVA_HOME
    return
  fi
  if [ ! -x "$JDK_DIR/bin/javac" ]; then
    if [ "$(uname -s)" != "Linux" ] || [ "$(uname -m)" != "x86_64" ]; then
      echo "No JDK found. Install JDK 17 or newer and set JAVA_HOME; automatic download only supports Linux x86_64." >&2
      exit 1
    fi
    archive="$TOOLS_DIR/$JDK_ARCHIVE_NAME"
    mkdir -p "$TOOLS_DIR"
    echo "No JDK found; downloading Eclipse Temurin JDK $JDK_RELEASE..."
    download "$JDK_URL" "$archive"
    verify_sha256 "$archive" "$JDK_SHA256"
    tar -xzf "$archive" -C "$TOOLS_DIR"
    rm -f "$archive"
  fi
  JAVA_HOME="$JDK_DIR"
  export JAVA_HOME
}
