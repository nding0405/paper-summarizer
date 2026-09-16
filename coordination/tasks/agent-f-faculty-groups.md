# Agent F — Faculty, groups, institutions, and geography

## Paste-ready instruction

You are Agent F, responsible for resolving faculty, research groups,
institutions, publication/current affiliation distinctions, and institutional
geography for explicitly assigned paper IDs.

Read `AGENTS.md`, `docs/source-policy.md`, `docs/data-layout.md`,
`docs/paper-field-map.md`, and the entity schemas in `schemas/v1/`.

## Exclusive write scope

You may edit only:

```text
data/shards/*/*/papers/<assigned-paper-id>/groups.json
data/entities/authors/<author-id>.json
data/entities/faculty/<faculty-id>.json
data/entities/research-groups/<group-id>.json
data/entities/institutions/<institution-id>.json
coordination/claims/<your-agent-id>.json
```

List assigned paper IDs in your claim. You are the sole entity-directory owner;
other agents must report candidate entity corrections to you.

## Attribution rules

- Never infer a PI from last-author or first-author order.
- Associate a paper with a faculty member or group only with official evidence:
  lab/faculty publication page, official student/advisor page, institution
  page, or the paper/project page naming the group.
- A paper may belong to multiple groups.
- Preserve publication-time affiliation in `metadata.json`; do not edit it.
- Store current faculty affiliation separately in the faculty entity.
- Faculty moves never rewrite historical data.
- When evidence is insufficient, use `/unresolved_authors` and do not create a
  guessed association.

## Procedure

1. Search existing entity files before creating IDs or aliases.
2. Create one file per author, institution, faculty member, and group using the
   matching versioned schema.
3. Normalize institution names and official institutional locations only.
4. Resolve each assigned paper into zero or more evidence-backed associations.
5. Store evidence source IDs and a concrete rationale per association.
6. For industrial authors, record organization, research division/group when
   official, and location; do not manufacture an academic PI.
7. Record recruiting information only when a current official source contains
   an explicit statement. Preserve exact wording, source, and verification
   date. Absence of a statement means null, not “not recruiting.”
8. Flag possible duplicate people, labs, or institutions instead of silently
   merging uncertain identities.

Use only the broad geographic regions defined by the project. Never geocode
home addresses.

## Workflow

An approved unresolved result is acceptable when it clearly names unresolved
authors and explains missing evidence. Empty associations without a completed
search are `NOT_STARTED`, not a verified negative.

## Validation

Entity references require full-root validation:

```sh
./mvnw -q compile exec:java -Dexec.args="validate --root data/shards"
```

## Handoff

Report paper IDs resolved, entity files created/reused, evidence URLs, historical
versus current affiliation decisions, recruiting statements, duplicate
candidates, unresolved authors/groups, and validation output.
