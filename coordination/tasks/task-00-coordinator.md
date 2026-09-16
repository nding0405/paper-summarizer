# Task 00 — Coordinator and merger

## Paste-ready instruction

You are the merge coordinator for Systems PhD Explorer. You own phase gates,
shared contracts, full-corpus validation, deterministic merges, generated
artifacts, and routing issues back to the agent that owns the affected stage.
You do not perform source research merely to make an agent's incomplete record
pass.

Read `Tasks.md`, `AGENTS.md`, all files in `docs/`, and
`coordination/tasks/README.md` before acting.

## Exclusive ownership

You alone may change:

- shared schemas, migrations, taxonomy, venue configuration, and policies;
- pipeline and validation code under `src/`;
- the read-only task definitions;
- generated venue manifests, SQLite databases, exports, reports, and
  `site/data/papers.json`; and
- phase status and merge decisions.

Preserve agent-authored stage files unless an agent explicitly hands ownership
back to you. Route corrections to the responsible agent.

## Work order

1. Confirm every active agent has a unique claim and non-overlapping paths.
2. Open Wave 1 for 2025 only.
3. After Agents A–D hand off, run draft validation and compare every declared
   venue count with staged records.
4. Audit collector exclusions and official source coverage before assigning
   exact pilot paper IDs to Agents E–H.
5. Let E, F, G, and I work in parallel. Do not give two agents the same stage
   file.
6. Give Agent H a frozen snapshot only after the other stage owners finish.
7. Route Agent H's issues to owners; Agent H must not repair their files.
8. Run release validation on the pilot. Open Wave 4 only after policy/schema
   issues are resolved.
9. Merge expanded shards in stable path order.
10. Build SQLite and exports atomically; never hand-edit them.

## Commands

```sh
make check
```

```sh
# (build-manifest was retired with the Python tooling; the coverage manifest is produced by `make export`)
```

```sh
./mvnw -q compile exec:java -Dexec.args="validate --root data/shards"
```

```sh
make build-db
make export
```

## Merge rejection conditions

Reject a handoff when:

- it edits files outside the claim;
- a venue-year lacks an honest status;
- a main-track paper was not considered;
- a relevance decision lacks rationale;
- a PI link or award lacks official evidence;
- a summary has unsupported numerical claims;
- stage owners reviewed their own work;
- duplicate paper or entity IDs are unresolved; or
- generated files were edited manually.

## Completion

A phase closes only when tests and the relevant validation profile pass,
coverage statistics are generated, unresolved items remain visible, and the
handoff states exactly what is and is not complete.

## Handoff

Publish the phase name, merged agent claims, exact source coverage, venue-year
counts, validation and test results, SQLite integrity result, generated artifact
paths, unresolved review items, and the next wave that is authorized. Explicitly
state whether completeness is established or still unverified.
