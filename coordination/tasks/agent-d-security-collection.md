# Agent D — CCS, NDSS, IEEE S&P, and USENIX Security

## Paste-ready instruction

You are Agent D, responsible for official program collection and strict
systems-relevance screening for ACM CCS, NDSS, IEEE S&P, and USENIX Security.

Work on 2025 first. Do not start 2023, 2024, or 2026 until the coordinator
explicitly opens the bulk phase.

Read `AGENTS.md`, `docs/scope.md`, `docs/inclusion-policy.md`,
`docs/source-policy.md`, `docs/taxonomy.md`, `docs/data-layout.md`, and
`docs/paper-field-map.md`.

## Exclusive write scope

You may edit only `venue-year.json`, `metadata.json`, and `relevance.json`
under:

```text
data/shards/ccs/<assigned-year>/
data/shards/ndss/<assigned-year>/
data/shards/sp/<assigned-year>/
data/shards/usenix-security/<assigned-year>/
```

You may create only your own claim file. Do not edit summary, group, award, or
review stage content.

## Procedure

1. Claim exact venue/year shards.
2. Verify official programs, proceedings, dates, and paper-type boundaries.
3. Consider every main technical paper, including papers ultimately excluded.
4. Create a minimal auditable bundle for excluded main-track papers:
   `metadata.json` plus a complete `relevance.json`. Excluded records do not
   require a story summary.
5. Collect full metadata for included and adjacent papers.
6. Screen relevance using the paper's actual artifact and contribution, not
   security-venue membership.
7. Reconcile all venue-year counts, including excluded and needs-review papers.

## Security screening rules

Usually include systems security, kernel or hardware security, isolation,
trusted/confidential computing, memory safety, fuzzing, vulnerability
discovery, program analysis with a systems artifact, authentication
infrastructure, and network security mechanisms.

Usually exclude pure cryptographic primitives/protocols, purely theoretical
security, social-science or perception studies without a systems contribution,
policy-only work, misinformation/content moderation, pure ML attacks or
defenses without a systems artifact, and biometric classification alone.

When uncertain, use `NEEDS_HUMAN_REVIEW`; never inflate the corpus by default.

## Validation

```sh
./mvnw -q compile exec:java -Dexec.args="validate --root data/shards/<venue>/<year>"
```

## Handoff

Provide official source coverage, all main-track counts, inclusion/exclusion
breakdown, exact rationales for boundary cases, created paper IDs, suspected
duplicates, unresolved fields, and validator output.
