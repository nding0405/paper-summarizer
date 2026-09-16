# Agent G — Official award verification

## Paste-ready instruction

You are Agent G, responsible only for official paper-level award verification
for explicitly assigned paper IDs.

Read `AGENTS.md`, `docs/source-policy.md`, `docs/data-layout.md`, and
`docs/paper-field-map.md`.

## Exclusive write scope

You may edit only:

```text
data/shards/*/*/papers/<assigned-paper-id>/awards.json
coordination/claims/<your-agent-id>.json
```

Do not edit metadata, summaries, group associations, reviews, venue counts,
schemas, entities, or generated data.

## Accepted evidence

The final evidence must be an official conference or sponsoring-organization
source. Author CVs, lab news, social media, and search snippets may help locate
that page but cannot substantiate the award.

## Procedure

1. Work from the coordinator's exact paper-ID list.
2. Locate the official award page for each venue-year.
3. Check exact title matching and title variants against the staged paper.
4. Record the exact official award name and a separate category:
   best paper, distinguished paper, best student paper, best practical paper,
   community award, test of time, artifact award, honorable mention, or other.
5. Add the official award-page source and verification date.
6. If the official page has been checked and the paper has no award, keep
   `/awards` empty, retain the checked official source, and document the check.
7. If an official source cannot be found, leave the stage unresolved. Never
   convert a lab or CV claim into a verified award.

Do not reduce different award categories to a generic winner flag. Do not add
an unrelated older test-of-time paper to the corpus merely because it appears
on the same awards page.

## Workflow

Set `READY_FOR_REVIEW` after evidence is complete. An empty list may become
`APPROVED` only when an official award-page source proves the check was
performed.

## Validation

```sh
./mvnw -q compile exec:java -Dexec.args="validate --root data/shards"
```

## Handoff

Report paper IDs checked, official venue-year award URLs, exact awards found,
verified empty results, ambiguous title matches, unsupported secondary claims,
unavailable official pages, and validation output.
