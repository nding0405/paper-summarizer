#!/bin/sh
# Runs the packaged explorer WAR on a local Apache Tomcat.
#
# Downloads Tomcat into .tools/ on first use (SHA-512 verified), deploys
# target/systems-phd-explorer.war as the root application, and serves it on
# http://localhost:8080 until interrupted. Tomcat listens on the loopback
# interface only; set TOMCAT_ADDRESS=0.0.0.0 to expose it on the network.
set -eu

PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
TOMCAT_VERSION=10.1.59
TOMCAT_DIR="$PROJECT_DIR/.tools/apache-tomcat-$TOMCAT_VERSION"
WAR_FILE="$PROJECT_DIR/target/systems-phd-explorer.war"
TOMCAT_ADDRESS=${TOMCAT_ADDRESS:-127.0.0.1}

. "$PROJECT_DIR/scripts/lib/tools.sh"

ensure_jdk

if [ ! -x "$TOMCAT_DIR/bin/catalina.sh" ]; then
  ARCHIVE="$TOOLS_DIR/apache-tomcat-$TOMCAT_VERSION.tar.gz"
  CHECKSUM="$ARCHIVE.sha512"
  BASE_URL="https://dlcdn.apache.org/tomcat/tomcat-10/v$TOMCAT_VERSION/bin/apache-tomcat-$TOMCAT_VERSION.tar.gz"
  mkdir -p "$TOOLS_DIR"
  echo "Downloading Apache Tomcat $TOMCAT_VERSION..."
  download "$BASE_URL" "$ARCHIVE"
  download "$BASE_URL.sha512" "$CHECKSUM"
  verify_sha512 "$ARCHIVE" "$CHECKSUM"
  tar -xzf "$ARCHIVE" -C "$TOOLS_DIR"
  rm -f "$ARCHIVE" "$CHECKSUM"
  # The stock sample applications are not needed to serve the explorer.
  rm -rf "$TOMCAT_DIR/webapps/docs" "$TOMCAT_DIR/webapps/examples" \
    "$TOMCAT_DIR/webapps/host-manager" "$TOMCAT_DIR/webapps/manager"
fi

if [ ! -f "$WAR_FILE" ]; then
  echo "Missing WAR file: run ./mvnw package first." >&2
  exit 1
fi

# Bind the HTTP connector to the requested address (loopback by default).
sed -i.bak -E \
  "s|<Connector port=\"8080\"( address=\"[^\"]*\")? protocol=\"HTTP/1.1\"|<Connector port=\"8080\" address=\"$TOMCAT_ADDRESS\" protocol=\"HTTP/1.1\"|" \
  "$TOMCAT_DIR/conf/server.xml"
rm -f "$TOMCAT_DIR/conf/server.xml.bak"

rm -rf "$TOMCAT_DIR/webapps/ROOT" "$TOMCAT_DIR/work/Catalina/localhost/ROOT"
rm -f "$TOMCAT_DIR/webapps/ROOT.war"
cp "$WAR_FILE" "$TOMCAT_DIR/webapps/ROOT.war"

echo "Starting Systems PhD Explorer at http://localhost:8080 (press Ctrl+C to stop)"
exec "$TOMCAT_DIR/bin/catalina.sh" run
