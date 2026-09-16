# Java/JSP frontend rewrite handoff

## Final goal

Replace the existing static, JavaScript-heavy interface with a clean, concise,
server-rendered Java/JSP application hosted on Apache Tomcat 10.1.

The finished main page should have:

- one compact search and filter bar at the top;
- conferences grouped by CSRankings Systems category and year in a left sidebar;
- a paginated paper list on the right;
- only essential paper information: title, authors, venue/year, an optional
  one-line takeaway, at most two topics, and available source links;
- simple neutral styling focused on scanability rather than decorative polish;
- responsive and keyboard-accessible behavior without requiring client-side
  JavaScript; and
- a single local command, preferably `make serve`, that generates the paper
  export, builds a WAR, starts Tomcat, and serves the application at
  `http://localhost:8080`.

The current corpus contains 6,863 paper bundles. The default paper list should
hide `EXCLUDED` records but allow users to select them explicitly with the
relevance filter.

## Product decisions already made

The conference navigation uses the current CSRankings Systems-area names:

- **Operating systems:** OSDI, SOSP, EuroSys, USENIX ATC, FAST
- **Computer networks:** SIGCOMM, NSDI
- **Computer security:** ACM CCS, IEEE S&P, USENIX Security, plus the
  repository's additional NDSS venue
- **Computer architecture:** ASPLOS

These mappings were checked against the official CSRankings website and source
repository on 2026-09-15. This product must not turn paper counts or relevance
scores into faculty, group, conference, or advisor rankings.

The server-side filters currently planned and partly implemented are:

- text search across title, authors, takeaway, topics, methods, layers,
  institutions, locations, faculty, and groups;
- category;
- conference;
- year;
- relevance class; and
- code/artifact availability.

Search terms are ANDed. Quoted phrases are treated as a single term. Results
sort by year descending, then venue and title, and use pages of 40 records.

## Status: COMPLETE (2026-09-15)

Every step of the original continuation list was carried out and verified.
The remaining sections record what exists now, how it was validated, and what
a future maintainer should know.

## Files edited or added

### Build and runtime

- `.gitignore` — ignores `target/` (Maven output) and `.tools/` (downloaded
  JDK, Maven, and Tomcat).
- `pom.xml` — Java 17 WAR project: Jakarta Servlet 6 (provided), JSTL 3
  (API + GlassFish implementation, with container-provided EL/JSP/Servlet
  APIs excluded), Jackson 2.21.2, JUnit 5.14.3. Packages
  `site/data/papers.json`, `site/data/fixture-papers.json`,
  `site/data/venue-years.json`, and `config/taxonomy.v1.json` into
  `WEB-INF/data/`.
- `mvnw` — Maven launcher. Uses installed `mvn`/JDK when present; otherwise
  downloads Maven 3.9.16 (SHA-512 from the Apache CDN) and, when no `javac`
  exists, Eclipse Temurin JDK 17.0.20.1+1 (pinned SHA-256) into `.tools/`.
- `scripts/lib/tools.sh` — shared download, checksum, and `ensure_jdk`
  helpers sourced by `mvnw` and `scripts/run-tomcat.sh`.
- `scripts/run-tomcat.sh` — downloads Tomcat 10.1.59 (SHA-512), removes the
  stock sample webapps, binds the HTTP connector to `127.0.0.1` (override with
  `TOMCAT_ADDRESS`), deploys the WAR as `ROOT.war`, and runs Tomcat.
- `Makefile` — `check` now includes `java-test`; `war` packages the WAR after
  `site-data`; `serve` runs `war` then `scripts/run-tomcat.sh`.
- `README.md` — Java 17/Tomcat quick start, `make serve`, data-source
  override, and WAR deployment instructions.
- `site/README.md` — explains that the UI moved to `src/main/webapp/` and
  that `site/data/` is build input only.
- Removed: `site/index.html`, `site/app.js`, `site/styles.css`,
  `tests/frontend/app.test.js`, `tests/frontend/static.test.js`.

### Java application (`src/main/java/org/systemsphd/explorer/`)

- `model/Paper.java` — immutable paper model. Only absolute `http`/`https`
  links survive `Paper.from`; long author lists keep the first six and the
  last author; at most two topic labels are precomputed.
- `model/RepositorySnapshot.java` — loaded papers plus fixture flag.
- `catalog/ConferenceCatalog.java`, `ConferenceCategory.java`,
  `ConferenceDefinition.java` — the four CSRankings Systems categories and
  twelve conferences.
- `catalog/TopicLabels.java` — track labels from `config/taxonomy.v1.json`,
  acronym-aware slug humanization for subtopics.
- `catalog/VenueCoverage.java`, `VenueYearCoverage.java` — venue-year
  collection status from the generated `venue-years.json`.
- `data/PaperRepository.java` — loads the configured file
  (`systems.phd.papers` property or `SYSTEMS_PHD_PAPERS` variable), else the
  packaged export, else the packaged fixture; validates id/title/venue.
- `web/FilterState.java` — validated GET parameters; builds sidebar, page,
  remove-one-filter, and clear URLs on the request's context path.
- `web/ExplorerService.java` — search (ANDed terms, quoted phrases), filters,
  default `EXCLUDED` hiding, sort (year desc, venue, title), 40-per-page
  pagination, sidebar counts (precomputed once), coverage lookup.
- `web/ExplorerServlet.java` — mapped to the context root (`""`) so
  `/assets/*` is served by Tomcat's default servlet; loads data at startup,
  sets CSP/Referrer-Policy/nosniff headers, forwards to the JSP.
- `web/ExplorerPage.java`, `ActiveFilter.java`, `NavigationCategory.java`,
  `NavigationVenue.java`, `NavigationYear.java` — JSP view models.

### Web resources (`src/main/webapp/`)

- `WEB-INF/views/index.jsp` — the single page: filter form, sidebar, paper
  list, active-filter chips, fixture banner, coverage notice, empty state,
  pagination. Every data-derived value goes through `<c:out>`.
- `WEB-INF/views/error.jsp`, `WEB-INF/web.xml` — plain 404/500 pages.
- `assets/app.css` — system fonts, neutral palette with a dark scheme,
  250 px sticky sidebar that stacks above the list below 800 px, visible
  focus rings, no gradients or scripts.

### Tests (`src/test/java/org/systemsphd/explorer/`)

56 JUnit 5 tests: `data/PaperRepositoryTest`, `model/PaperTest`,
`catalog/TopicLabelsTest`, `catalog/VenueCoverageTest`,
`web/FilterStateTest`, `web/ExplorerServiceTest`, with helpers under
`support/` that build synthetic exports and manifests in memory.

## Product notes

- Relevance tags are shown on a row only when the class is not
  `CORE_SYSTEMS`, keeping default rows to the essentials.
- Sidebar counts exclude `EXCLUDED` papers and do not change with the
  current search; the paper list does.
- The sidebar lists every venue-year present in the coverage manifest, so a
  2026 program that is `UPCOMING` or `UNAVAILABLE` appears with that mark
  instead of disappearing, and selecting it explains the status rather than
  reporting zero papers. This covers the core of directive
  `coordinator-agent-i-venue-year-coverage-ui-2026-07-30`; the directive
  itself is coordinator-owned and still needs to be closed or re-scoped.
- The generated corpus has 6,863 papers (1,795 `EXCLUDED`, 5,068 visible by
  default), 71 takeaways, and http(s)-only links; the fixture has 8 papers.

## Verification record (2026-09-15)

- `make check`: 12 Python tests OK; `tests/fixtures/shards` release profile
  1 bundle / 7 files, 0 errors, 0 warnings; `data/shards` draft profile
  6,863 bundles / 41,320 files, 0 errors, 0 warnings; `./mvnw -B -q test`
  passed (56 tests, 0 failures).
- `make serve`: `site-data` → `./mvnw package` → Tomcat 10.1.59 started in
  638 ms on `127.0.0.1:8080`; log line "Loaded 6863 papers from generated
  paper export and coverage for 48 venue-years".
- HTTP smoke tests (all server-rendered, zero `<script>` tags): `/` →
  "Showing 1–40 of 5,068 papers"; `?q=network&category=computer-networks&relevance=ALL&page=3`
  → "Showing 81–120 of 687 papers" with correct prev/next links;
  `?q=kernel&venue=osdi&year=2025&relevance=ALL&availability=code` → five
  active-filter chips whose remove links drop exactly one parameter;
  `?venue=nonsense&year=abc&relevance=WHAT&page=-9` → unfiltered page;
  `?page=9999` → last page; `?venue=ccs&year=2026` → "Program not published
  yet" with no zero count; `/assets/app.css` → 200 `text/css`;
  `/does-not-exist` → 404 error page.
- Headless Firefox 155 screenshots at 1280 px (light and dark) and 390 px
  confirmed the layout, sticky sidebar, chip-style mobile navigation, and
  absence of horizontal scrolling.
- No shard, SQLite database, export, schema, taxonomy, or entity file was
  edited. `site/data/papers.json` and `site/data/venue-years.json` were
  regenerated with `make site-data` only.

## Follow-up on 2026-09-15 (same day)

After the first completion the product owner asked for, and received:

- expandable entries (abstract, LLM summary with workflow status, review
  verdict) on every list and a dedicated `paper.jsp?id=…` page;
- search over abstract and summary text, ranked title > abstract > other,
  with live suggestions (`suggest.json` + `assets/app.js`, optional script);
- a search icon that reveals the box and a Filters pop-up panel holding the
  selects and multi-select keyword facets (topics, layers, methods; any/all);
- `proceedings.jsp` — one conference-year grouped by track/session in
  program order with a session table of contents;
- pagination jumps (±5/±10) and a go-to-page box;
- white theme only, no product title, `/index.jsp` in the address bar.

Contract change: `src/systems_phd_explorer/export.py` now emits `abstract`
(null until collected — no metadata file has one), `track_or_session`,
`summary`, and `review` per paper. Abstract collection is a separate data task
that needs a metadata-schema addition.

Verification: `make check` passed (13 Python tests; fixtures 0/0; 6,863
bundles / 41,320 files 0 errors 0 warnings; 73 JUnit tests). All routes
probed over HTTP; headless Firefox screenshots of the three pages.

## Java-only migration (2026-09-16)

At the owner's request the Python tooling was removed and the project is
Java-only. `org.systemsphd.explorer.tools.Main` provides `validate`,
`export`, `init-paper`, `init-venue`, and `collect-abstracts`, run through
Maven's exec plugin (`make validate`, `make export`, `make abstracts`,
`make tool ARGS=...`). The stage-file templates live in
`src/main/resources/templates/`. `make check` = validate + JUnit (90 tests).
Dropped: the SQLite build and migrations, release-profile gating, the
monitor, and the pilot HTML collectors. The Java export was verified
byte-identical to the last Python export, and the JSON writer reproduces
the repository's formatting so data rewrites do not churn diffs.

## Important repository rules

Before continuing, read `AGENTS.md`, `docs/data-layout.md`,
`docs/paper-field-map.md`, `docs/agent-workflow.md`, and
`coordination/tasks/agent-i-frontend.md`. The user explicitly authorized this
task to expand beyond the original static-frontend path boundary.

Do not hand-edit `site/data/papers.json`; generate it with `make site-data`.
Do not edit paper shards or merger-generated database/export artifacts as part
of this frontend rewrite.
