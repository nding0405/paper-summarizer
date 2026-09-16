.PHONY: check test validate export abstracts war serve tool

MVN ?= ./mvnw
# Runs one of the data tools (see `make tool ARGS=help`). ./mvnw bootstraps a
# JDK and Maven into .tools/ when they are not installed.
TOOL = $(MVN) -q -B compile exec:java -Dexec.args

check: validate test

test:
	$(MVN) -B -q test

# Validates every shard, venue-year, and entity file against the JSON schemas
# plus the cross-file rules the merge depends on.
validate:
	$(TOOL)="validate --root data/shards"

# Writes site/data/papers.json and site/data/venue-years.json from the shards.
export:
	$(TOOL)="export --root data/shards --output site/data"

# Fetches published abstracts into data/shards/**/metadata.json. Resumable;
# papers that already have an abstract are skipped. Set MAILTO for the APIs'
# polite pools and ABSTRACT_ARGS for extra options (e.g. "--venue ndss").
abstracts:
	$(TOOL)="collect-abstracts --root data/shards $(if $(MAILTO),--mailto $(MAILTO),) $(ABSTRACT_ARGS)"

# Any tool command, e.g. make tool ARGS='init-paper --venue osdi --year 2025 --title "Exact Title" --owner agent-a'
tool:
	$(TOOL)="$(ARGS)"

# Packages target/systems-phd-explorer.war with a freshly generated export.
war: export
	$(MVN) -B -q package

# Generates the export, builds the WAR, and serves it at http://localhost:8080.
serve: war
	scripts/run-tomcat.sh
