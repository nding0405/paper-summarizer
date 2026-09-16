# Systems PhD Explorer

Systems PhD Explorer is a static-first research discovery project for students
preparing systems PhD applications. It organizes recent papers, beginner
summaries, research groups, potential supervisors, institutions, and evidence
provenance without turning paper counts into prestige rankings.

Paper data is staged as conflict-resistant JSON fragments, validated, and
exported to a compact index that the web application serves.

## Quick start

Requirements: Java 17 or newer. `./mvnw` downloads Maven (and a JDK when only
a JRE is installed) into `.tools/` on first use; `scripts/run-tomcat.sh`
downloads Tomcat 10.1 the same way. Every download is checksum-verified.

Run the checks (schema and cross-file validation of every data file, then
the JUnit tests):

```sh
make check
```

Generate the paper export, build the web application, and serve it:

```sh
make serve
```

Then open `http://localhost:8080`.

### Data tools

Every tool is a subcommand of one Java entry point, run through Maven:

```sh
make validate                       # validate data/shards and data/entities
make export                         # write site/data/papers.json and venue-years.json
make abstracts MAILTO=you@example   # fetch missing abstracts (resumable)
make tool ARGS="help"               # list every command and option
```

Create a paper bundle in an agent-owned venue/year shard:

```sh
./mvnw -q compile exec:java -Dexec.args="init-paper --venue osdi --year 2025 --title \"Exact published title\" --owner agent-a"
```

Initialize an upcoming or unavailable venue-year even when it has no paper
records yet:

```sh
./mvnw -q compile exec:java -Dexec.args="init-venue --venue sosp --year 2026 --owner agent-a"
```

Validate staged data:

```sh
./mvnw -q compile exec:java -Dexec.args="validate --root data/shards"
```

## Web interface

The interface is a server-rendered Java Servlet/JSP application for Apache
Tomcat 10.1. Every page is a plain GET request with a shareable URL; the only
script is an optional enhancement for live search suggestions.

Pages:

- `index.jsp` — search and filter. A search icon opens the search box; typing
  shows the best-ranked papers (title matches first, then abstract, then
  everything else) and arrow keys plus Enter open a paper's page. The
  **Filters** pop-up holds conference, year, relevance, code/artifact, and
  multi-select keyword facets (topics, system layers, methods) with "match
  any"/"match all". The sidebar groups conferences by CSRankings Systems area
  with per-year counts and coverage marks. Results come 40 per page with
  previous/next, ±5/±10 jumps, and a go-to-page box.
- `proceedings.jsp?venue=asplos&year=2025` — one conference-year laid out
  like its program: a session table of contents, then every paper (including
  excluded ones) under its track or session, for reading straight through.
- `paper.jsp?id=…` — one paper: authors, session, links, abstract, the
  LLM-written summary with its workflow status, and the independent review
  verdict with any open issues.

Every list entry expands in place to the same abstract, summary, and review
sections. Abstracts are shown and searched as soon as the metadata carries
them; the corpus does not include them yet.

Search terms are ANDed and match title, authors, abstract, takeaway, summary
text, topics, methods, system layers, institutions, locations, faculty, and
groups. Wrap a phrase in double quotes to match it exactly. `EXCLUDED` papers
are hidden from search unless the relevance filter selects them or "All
classes"; the proceedings view always lists them.

Paper counts are navigation aids only; nothing in the interface ranks faculty,
groups, or conferences.

### Building and running

`make serve` runs three steps that can also be run separately:

```sh
make export           # validate shards and write site/data/papers.json + venue-years.json
./mvnw package        # compile, run JUnit tests, build target/systems-phd-explorer.war
scripts/run-tomcat.sh # download Tomcat 10.1 on first use and serve the WAR on port 8080
```

`./mvnw` uses an installed `mvn` and JDK when available. Otherwise it downloads
Apache Maven 3.9 and, if no `javac` is found, Eclipse Temurin JDK 17 into
`.tools/` (Linux x86_64 only for the JDK; on other platforms install a JDK
and set `JAVA_HOME`). `.tools/` and `target/` are ignored by Git.

`scripts/run-tomcat.sh` binds Tomcat to `127.0.0.1` only. Set
`TOMCAT_ADDRESS=0.0.0.0` to accept connections from other machines.

Run only the JUnit tests:

```sh
make test
```

### Data sources

The WAR packages `site/data/papers.json` when it exists, otherwise the
committed synthetic `site/data/fixture-papers.json`; a banner marks fixture
data. To point a running server at another export, start Tomcat with the
`systems.phd.papers` system property or the `SYSTEMS_PHD_PAPERS` environment
variable set to the file path, for example:

```sh
CATALINA_OPTS="-Dsystems.phd.papers=/path/to/papers.json" scripts/run-tomcat.sh
```

`config/taxonomy.v1.json` (topic labels) and the generated
`site/data/venue-years.json` (coverage status) are packaged alongside the
export.

### Deploying to an existing Tomcat

Copy `target/systems-phd-explorer.war` into the `webapps/` directory of any
Tomcat 10.1 installation running on Java 17 or newer. The application works
at the root context or under a named context path.

## Where paper information goes

Every paper gets its own directory:

```text
data/shards/<venue>/<year>/papers/<paper-id>/
├── metadata.json       # venue collector
├── relevance.json      # relevance classifier
├── summary.json        # summary writer
├── groups.json         # faculty/lab resolver
├── awards.json         # award verifier
└── review.json         # independent reviewer
```

For example:

```text
data/shards/osdi/2025/papers/osdi-2025-example-title-a1b2c3d4/
```

The files are deliberately separated by responsibility so agents working on
the same paper do not edit the same file. See
[`docs/data-layout.md`](docs/data-layout.md) and
[`docs/paper-field-map.md`](docs/paper-field-map.md) for exact field placement,
then read [`docs/agent-workflow.md`](docs/agent-workflow.md) before delegating
work.

## Repository map

```text
config/                 Stable venue and taxonomy configuration
coordination/           Per-agent claim files
data/shards/            Agent-authored paper fragments, by venue and year
data/entities/          One-file-per-entity resolved people/groups/institutions
data/review_queue/      Generated human-review items
docs/                   Scope and policy documents
schemas/v1/             Versioned JSON Schemas
scripts/                Tomcat launcher and shared tool-bootstrap helpers
site/data/              Generated paper export and synthetic fixture (build input)
src/main/               Java/JSP web interface (Servlet, JSP view, stylesheet)
src/main/java/.../tools/   Data tools: validate, export, init-paper, collect-abstracts
src/test/java/          JUnit tests for the web interface
tests/fixtures/         Release-profile fixture shard used by the JUnit tests
```

`site/data/papers.json`, `site/data/venue-years.json`, logs, caches, and
downloaded PDFs are generated artifacts and are not edited or committed by
collection agents.

## Delegating work

Use the paste-ready briefs in
[`coordination/tasks/README.md`](coordination/tasks/README.md). They define a
coordinator plus Agents A–I, execution waves, exclusive paths, validation
commands, and handoff criteria. Give each agent one brief and require a unique
claim file before it edits data.

## Current status

The structure, policies, schemas, 2023–2026 venue/year manifest, merge tooling,
and server-rendered web interface are present. The corpus is intentionally empty. No venue
coverage, award, group association, or paper summary should be treated as
verified until its source-backed stage file passes review.
