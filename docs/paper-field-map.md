# Paper field map

This is the authoritative placement guide for delegated paper work.

Given:

```text
data/shards/osdi/2025/papers/<paper-id>/
```

use the following JSON pointers. Never add an ad hoc seventh stage file or move
a field to another stage to make a task convenient.

## Collection and identity — `metadata.json`

| Information | JSON pointer |
| --- | --- |
| Stable paper ID | `/paper_id` |
| Exact title | `/title` |
| Published abstract and the source it was read from | `/abstract`, `/abstract_source_id` |
| Conference and year | `/venue`, `/year` |
| Track or session | `/track_or_session` |
| Main-track/other paper type | `/paper_type` |
| Publication date | `/publication_date` |
| Page count | `/page_count` |
| Venue-year completeness copied at collection time | `/venue_year_completion_status` |
| Published-order authors | `/authors` |
| Author name and position | `/authors/*/name`, `/authors/*/position` |
| Resolved author ID and ORCID | `/authors/*/author_id`, `/authors/*/orcid` |
| Publication-time affiliations | `/authors/*/affiliations` |
| Published and normalized institution names | `/authors/*/affiliations/*/name_as_published`, `/normalized_name` |
| Institution type and publication-time geography | `/authors/*/affiliations/*/institution_type`, `/city`, `/state_or_province`, `/country`, `/region` |
| DOI | `/links/doi` |
| Official page and PDF | `/links/official_page`, `/links/official_pdf` |
| Artifact, code, project, slides, video | `/links/artifact`, `/links/code`, `/links/project`, `/links/slides`, `/links/video` |
| Official BibTeX | `/links/bibtex` |
| arXiv/journal/other linked versions | `/alternate_versions` |
| Sources used for these facts | `/sources` |
| Collection and verification metadata | `/workflow`, `/verification` |

The conference version remains canonical. Do not replace its title or date with
an arXiv or journal version.

## Relevance and taxonomy — `relevance.json`

| Information | JSON pointer |
| --- | --- |
| Relevance class | `/class` |
| Relevance-fit score, never quality | `/score` |
| One-sentence rationale | `/rationale` |
| Top-level and subtopic IDs | `/topics` |
| System layers | `/system_layers` |
| Contribution types | `/contribution_types` |
| Methods | `/methods` |
| Classification evidence | `/sources` |
| Classifier confidence | `/verification/confidence` |

Use only IDs from `config/taxonomy.v1.json`.

## Beginner story — `summary.json`

| Required story component | JSON pointer |
| --- | --- |
| A. One-line takeaway, at most 35 words | `/takeaway` |
| B. Background | `/background` |
| C. Villain/problem | `/villain` |
| D. How they beat the villain | `/approach` |
| E. So what | `/impact` |
| F. Evaluation overview | `/evaluation/overview` |
| What was implemented | `/evaluation/implemented` |
| Where/how it was tested | `/evaluation/setting` |
| Baselines | `/evaluation/baselines` |
| One to three important results | `/evaluation/results` |
| Evaluation form | `/evaluation/study_types` |
| G. Limitations and assumptions | `/limitations` |
| H. PhD-group signal | `/phd_group_signal` |
| I. Three to eight beginner concepts | `/beginner_concepts` |
| Sections actually read | `/reading_coverage` |
| Other important factual claims | `/claims` |
| Paper/PDF/project sources read | `/sources` |

Every result object contains `/source_ids` and `/locator`. Put the paper's
reported claim in `/text`; do not rewrite it as independently established fact.

Taxonomy tags live in `relevance.json`, not in summary prose.

## Faculty and group attribution — `groups.json`

| Information | JSON pointer |
| --- | --- |
| All supported paper-group/PI links | `/associations` |
| Research group reference | `/associations/*/group_id` |
| Faculty reference | `/associations/*/faculty_id` |
| Historical institution reference | `/associations/*/publication_institution_id` |
| Current institution reference | `/associations/*/current_institution_id` |
| Type of official evidence | `/associations/*/association_type` |
| Exact source IDs supporting the link | `/associations/*/evidence_source_ids` |
| Attribution rationale and confidence | `/associations/*/rationale`, `/confidence` |
| Authors whose groups cannot be resolved | `/unresolved_authors` |

Resolved entity facts live separately:

| Entity | Path |
| --- | --- |
| Author identity | `data/entities/authors/<author-id>.json` |
| Current faculty record and recruiting statement | `data/entities/faculty/<faculty-id>.json` |
| Research/industrial group | `data/entities/research-groups/<group-id>.json` |
| Normalized institution and geography | `data/entities/institutions/<institution-id>.json` |

Do not put a current faculty institution into the publication-time affiliation
field. Do not infer a PI from author order.

## Awards — `awards.json`

| Information | JSON pointer |
| --- | --- |
| One record per official paper-level award | `/awards` |
| Exact official name | `/awards/*/official_name` |
| Separate award category | `/awards/*/category` |
| Award venue/year | `/awards/*/venue`, `/year` |
| Official award source reference | `/awards/*/official_source_id` |
| Verification time | `/awards/*/last_verified_at` |
| Official award-page sources checked | `/sources` |

An approved empty `/awards` array still needs the official award page used to
check it. A `NOT_STARTED` empty array is not evidence that the paper won
nothing.

## Independent factual review — `review.json`

| Information | JSON pointer |
| --- | --- |
| Independent reviewer | `/reviewer` |
| Stage owners the reviewer is independent from | `/independent_from` |
| Metadata/relevance/summary/PI/award/link checks | `/checks` |
| Actionable factual issues | `/issues` |
| Final review decision | `/decision` |

Corrections that cross ownership boundaries begin as `/issues`; the owner of
the affected stage applies the correction. The reviewer then closes the issue.

## Venue/year coverage — sibling `venue-year.json`

This file is beside `papers/`, not inside a paper directory:

```text
data/shards/<venue>/<year>/venue-year.json
```

It stores official conference/program URLs, dates, completeness status,
main-track and relevance counts, sources, and last verification. Only the venue
collector edits it.

## Workflow state is not paper data

Every stage has `/workflow` and `/verification`. These distinguish:

- not checked;
- in progress;
- ready for independent review;
- approved;
- blocked; and
- unresolved evidence.

Never encode workflow uncertainty by changing a scientific or bibliographic
field to a guessed value.
