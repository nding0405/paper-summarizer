# Agent workflow

## 1. Receive a bounded assignment

An assignment should name:

- agent ID;
- role;
- exact venue/year shards;
- exact stage files;
- allowed entity directories; and
- expected validation profile.

Avoid assigning two agents the same stage file. Parallel work is safe when
either the shard or stage differs.

## 2. Claim without touching shared state

Copy `coordination/claims/example.json` to:

```text
coordination/claims/<agent-id>.json
```

Only that agent edits that file. Claims are advisory and reviewable in Git;
they are not a distributed lock.

## 3. Initialize records

The venue collector creates the paper bundle with `init-paper`, then fills only
`metadata.json` and the shard's `venue-year.json`. Other role files begin with
explicit `NOT_STARTED` workflow state.

When an independent specialist receives an existing paper ID, they edit only
their stage:

- relevance classifier: `relevance.json`;
- summary writer: `summary.json`;
- group resolver: `groups.json` and assigned entity files;
- award verifier: `awards.json`; or
- reviewer: `review.json`.

## 4. Preserve uncertainty

Use nulls, `UNRESOLVED`, `NEEDS_HUMAN_REVIEW`, and issue records as designed.
Do not guess missing metadata, PI relationships, recruiting status, awards, or
limitations.

## 5. Validate locally

During collection:

```sh
./mvnw -q compile exec:java -Dexec.args="validate --root data/shards/<venue>/<year>"
```

Before release:

```sh
./mvnw -q compile exec:java -Dexec.args="validate --root data/shards"
```

## 6. Hand off, then merge centrally

The handoff report lists:

- files changed;
- official source URLs;
- paper counts considered/included/excluded;
- unresolved fields;
- review items;
- validator result; and
- collector version.

A designated merger validates all shards and generates the site export. If duplicate IDs, titles, people, or institutions are detected,
the merger creates review issues rather than mutating agent records silently.

## Suggested role allocation

| Role | Default scope |
| --- | --- |
| Agent A | OSDI, SOSP, EuroSys, ATC metadata |
| Agent B | FAST, NSDI, SIGCOMM metadata |
| Agent C | ASPLOS metadata and architecture classification |
| Agent D | CCS, NDSS, IEEE S&P, USENIX Security metadata/relevance |
| Agent E | summaries |
| Agent F | faculty, labs, institutions, geography |
| Agent G | awards |
| Agent H | independent factual review |
| Agent I | frontend and client-side search |

Branches are still useful, but the file boundaries are the primary conflict
control. Never allow multiple agents to write the generated export.
