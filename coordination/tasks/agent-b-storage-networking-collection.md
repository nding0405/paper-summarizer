# Agent B — FAST, NSDI, and SIGCOMM

## Paste-ready instruction

You are Agent B, responsible for official program collection and relevance
classification for FAST, NSDI, and SIGCOMM.

Work on 2025 first. Do not start 2023, 2024, or 2026 until the coordinator
explicitly opens the bulk phase.

Read `AGENTS.md`, `docs/scope.md`, `docs/inclusion-policy.md`,
`docs/source-policy.md`, `docs/taxonomy.md`, `docs/data-layout.md`, and
`docs/paper-field-map.md`.

## Exclusive write scope

You may edit only `venue-year.json`, `metadata.json`, and `relevance.json`
under:

```text
data/shards/fast/<assigned-year>/
data/shards/nsdi/<assigned-year>/
data/shards/sigcomm/<assigned-year>/
```

You may also create only your own claim file. Do not edit specialist stage
files after `init-paper` creates their placeholders.

## Procedure

1. Claim exact venue/year shards.
2. Initialize each venue-year, then verify official conference, program,
   proceedings, and date sources.
3. Enumerate the full main technical-paper program and document all excluded
   non-main categories.
4. Initialize one bundle per canonical conference paper.
5. Fill exact identity, ordered authors, publication-time affiliations, DOI,
   official links, artifact/code availability, and provenance.
6. Classify every considered main-track paper with a rationale and multi-label
   taxonomy.
7. Check title variants and preprint/journal duplicates.
8. Reconcile all venue-year counts with staged records.

## Classification emphasis

For FAST, capture file systems, storage devices, caching, persistent memory,
reliability, and storage measurement without forcing one topic.

For NSDI and SIGCOMM, distinguish networking mechanisms and networked systems
from pure analysis that lacks a meaningful systems contribution. Useful
measurement work may be core or adjacent when its artifact or systems insight
is substantial.

## Validation

```sh
./mvnw -q compile exec:java -Dexec.args="validate --root data/shards/<venue>/<year>"
```

## Handoff

Provide official source coverage, exact counts, created paper IDs, classification
breakdown, unresolved records, suspected duplicates, missing artifacts/links,
and validator output.
