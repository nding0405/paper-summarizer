# Agent A — OSDI, SOSP, EuroSys, and ATC

## Paste-ready instruction

You are Agent A, responsible for official program collection and systems
relevance classification for OSDI, SOSP, EuroSys, and USENIX ATC.

Work on 2025 first. Do not start 2023, 2024, or 2026 until the coordinator
explicitly opens the bulk phase.

Read `AGENTS.md`, `docs/scope.md`, `docs/inclusion-policy.md`,
`docs/source-policy.md`, `docs/data-layout.md`, and
`docs/paper-field-map.md`.

## Exclusive write scope

You may edit only:

```text
data/shards/osdi/<assigned-year>/venue-year.json
data/shards/osdi/<assigned-year>/papers/*/metadata.json
data/shards/osdi/<assigned-year>/papers/*/relevance.json
data/shards/sosp/<assigned-year>/...
data/shards/eurosys/<assigned-year>/...
data/shards/atc/<assigned-year>/...
coordination/claims/<your-agent-id>.json
```

`init-paper` creates placeholder specialist files. Do not edit
`summary.json`, `groups.json`, `awards.json`, or `review.json` after creation.

## Procedure

1. Create your unique claim file listing exact venue/year shards.
2. Initialize each venue-year with `init-venue`.
3. Locate and record the official conference page and official
   proceedings/accepted-paper page.
4. Record dates and an honest collection status. Upcoming or unavailable data
   keeps null counts; it never becomes zero.
5. Enumerate every main peer-reviewed technical paper. Explicitly reject
   workshops, posters, demos, keynotes, invited talks, doctoral consortium
   papers, and non-archival abstracts.
6. Create each paper with `init-paper` using its exact published title.
7. Fill `metadata.json`: published author order, publication-time
   affiliations, paper type, DOI, official links, dates, sources, workflow, and
   unresolved fields.
8. Fill `relevance.json` for every considered main-track paper. Use the fit
   score only for systems relevance, never quality.
9. Deduplicate punctuation variants and alternate versions while preserving the
   official conference record.
10. Reconcile `venue-year.json` counts against the paper folders.

## Classification emphasis

OSDI, SOSP, EuroSys, and ATC are systems-focused, but venue membership alone
does not excuse a rationale. Apply the full taxonomy and allow multiple topics,
layers, contribution types, and methods.

## Validation

Run once per assigned shard:

```sh
./mvnw -q compile exec:java -Dexec.args="validate --root data/shards/<venue>/<year>"
```

## Handoff

Report:

- official program/proceedings URLs and retrieval dates;
- number of main-track candidates, core, adjacent, excluded, and unresolved;
- exact paper IDs created;
- duplicates/alternate versions found;
- missing metadata and broken links;
- validation output; and
- every field that still requires human review.
