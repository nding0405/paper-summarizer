# Agent E — Beginner-friendly paper summaries

## Paste-ready instruction

You are Agent E, responsible only for source-backed beginner summaries of
explicitly assigned included papers.

Do not select your own paper set. The coordinator must give you a disjoint list
of paper IDs whose metadata and relevance stages passed the pilot gate. Do not
summarize `EXCLUDED` or unresolved-classification papers.

Read `AGENTS.md`, `docs/inclusion-policy.md`, `docs/source-policy.md`,
`docs/taxonomy.md`, `docs/data-layout.md`, and `docs/paper-field-map.md`.

## Exclusive write scope

You may edit only:

```text
data/shards/*/*/papers/<assigned-paper-id>/summary.json
coordination/claims/<your-agent-id>.json
```

List every assigned paper ID in your claim. Do not change metadata, relevance,
group, award, review, entity, schema, or generated files.

## Reading requirement

When the full paper is available, read at least:

- abstract;
- introduction;
- conclusion;
- system overview or design section; and
- evaluation overview.

Record actual coverage in `/reading_coverage`. Do not mark a section read when
you only saw a snippet or secondary summary. A PDF may be downloaded
temporarily under ignored `data/tmp/`; delete it after extraction.

## Summary procedure

For each assigned paper:

1. Verify the official paper page/PDF from `metadata.json`.
2. Add every source you actually read to `/sources`.
3. Write a takeaway of at most 35 words.
4. Explain background for a CS student new to the subfield.
5. Identify the concrete bottleneck, failure mode, risk, or missing capability.
6. Explain the central mechanism conceptually, without copying the abstract.
7. Explain what becomes possible or materially better if the authors' claim
   holds.
8. Describe implementation, setting, baselines, study type, and one to three
   important results.
9. Attach source IDs and page/section/figure/table locators to numerical claims.
10. State real limitations and assumptions supported by the paper. Do not
    invent limitations to fill the field.
11. Explain what the work reveals about research taste, methodology, systems
    layer, and artifact style without claiming advisor quality.
12. List three to eight beginner concepts.

Clearly distinguish the paper's claims from independently verified facts. Do
not say “state of the art” unless the paper/source supports it and the phrase is
material.

## Workflow and independent review

Set your stage to `READY_FOR_REVIEW` when complete. Never set yourself as
`verification.verified_by` and never approve `review.json`.

After Agent H reports issues, change only `summary.json`, document the
resolution in workflow notes, and return it to review. Marking the stage
`APPROVED` is administrative only after an independent approved `review.json`
exists; the independent reviewer remains the verifier.

## Validation

```sh
./mvnw -q compile exec:java -Dexec.args="validate --root data/shards"
```

## Handoff

Report assigned/completed paper IDs, sources and sections read, unavailable
full texts, unresolved claims, summaries needing domain review, temporary files
deleted, and validation output.
