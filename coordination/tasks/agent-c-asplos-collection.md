# Agent C — ASPLOS collection and architecture taxonomy

## Paste-ready instruction

You are Agent C, responsible for ASPLOS program collection, relevance
classification, and architecture-oriented taxonomy.

Work on ASPLOS 2025 first. Do not start other years until the coordinator opens
the bulk phase.

Read `AGENTS.md`, `docs/scope.md`, `docs/inclusion-policy.md`,
`docs/source-policy.md`, `docs/taxonomy.md`, `docs/data-layout.md`, and
`docs/paper-field-map.md`.

## Exclusive write scope

You may edit only:

```text
data/shards/asplos/<assigned-year>/venue-year.json
data/shards/asplos/<assigned-year>/papers/*/metadata.json
data/shards/asplos/<assigned-year>/papers/*/relevance.json
coordination/claims/<your-agent-id>.json
```

Do not edit summary, group, award, or review stages after their placeholders are
created.

## Procedure

1. Claim the exact ASPLOS year.
2. Verify the official conference and proceedings sources and conference dates.
3. Enumerate every main technical paper and distinguish non-main material.
4. Initialize each canonical paper and collect exact metadata, ordered authors,
   publication-time affiliations, links, artifacts, and provenance.
5. Classify every main-track paper and apply architecture, compiler, language,
   runtime, systems, and hardware/software co-design tags as appropriate.
6. Reconcile venue-year counts and unresolved records.

## Critical ASPLOS rule

Do not include a paper merely because it uses machine learning or appears at
ASPLOS. Pure new ML algorithms without an architecture, compiler, runtime, or
systems contribution are normally excluded. State the concrete systems
mechanism in the rationale. Use `NEEDS_HUMAN_REVIEW` when the contribution
boundary remains unclear after reading the official paper.

Do not collapse hardware/software co-design into architecture alone when
runtime, compiler, kernel, scheduling, or ML-systems tags also apply.

## Validation

```sh
./mvnw -q compile exec:java -Dexec.args="validate --root data/shards/asplos/<year>"
```

## Handoff

Report official source coverage, complete counts, paper IDs, excluded ML-only
papers with rationales, taxonomy ambiguities, duplicates, unresolved fields,
and validation output.
