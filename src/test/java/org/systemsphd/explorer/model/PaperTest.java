package org.systemsphd.explorer.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.systemsphd.explorer.support.ExportBuilder;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaperTest {
    @Test
    void onlyHttpAndHttpsLinksAreKept() {
        Paper paper = new ExportBuilder()
                .paper("p", "osdi", 2025)
                .officialPage("javascript:alert(1)")
                .officialPdf("HTTPS://example.org/paper.pdf")
                .code("ftp://example.org/code")
                .artifact("http://example.org/artifact")
                .add()
                .snapshot().getPapers().get(0);

        assertNull(paper.getOfficialPage());
        assertFalse(paper.isHasOfficialPage());
        assertEquals("HTTPS://example.org/paper.pdf", paper.getOfficialPdf());
        assertNull(paper.getCodeUrl());
        assertEquals("http://example.org/artifact", paper.getArtifactUrl());
        assertTrue(paper.isHasLinks());
    }

    @Test
    void malformedLinksAreDropped() {
        Paper paper = new ExportBuilder()
                .paper("p", "osdi", 2025)
                .officialPage("http://exa mple.org/with space")
                .officialPdf("/relative/path.pdf")
                .code("https://")
                .artifact("   ")
                .add()
                .snapshot().getPapers().get(0);

        assertNull(paper.getOfficialPage());
        assertNull(paper.getOfficialPdf());
        assertNull(paper.getCodeUrl());
        assertNull(paper.getArtifactUrl());
        assertFalse(paper.isHasLinks());
    }

    @Test
    void codeAvailabilityFollowsTheExportFlagOrAValidLink() {
        List<Paper> papers = new ExportBuilder()
                .paper("flag-only", "osdi", 2025).code("not a url").add()
                .paper("neither", "osdi", 2025).add()
                .snapshot().getPapers();

        assertTrue(papers.get(0).isHasCode());
        assertNull(papers.get(0).getCodeUrl());
        assertFalse(papers.get(1).isHasCode());
    }

    @Test
    void shortAuthorListsAreShownInFull() {
        Paper paper = new ExportBuilder()
                .paper("p", "osdi", 2025).authors("A One", "B Two", "C Three").add()
                .snapshot().getPapers().get(0);
        assertEquals("A One, B Two, C Three", paper.getAuthorLine());
    }

    @Test
    void longAuthorListsKeepLeadingAuthorsAndTheLastAuthor() {
        String[] authors = IntStream.rangeClosed(1, 12)
                .mapToObj(index -> "Author " + index)
                .toArray(String[]::new);
        Paper paper = new ExportBuilder()
                .paper("p", "osdi", 2025).authors(authors).add()
                .snapshot().getPapers().get(0);

        assertEquals(
                "Author 1, Author 2, Author 3, Author 4, Author 5, Author 6, … 5 more …, Author 12",
                paper.getAuthorLine()
        );
    }

    @Test
    void topicLabelsAreLimitedToTwoAndHumanized() {
        Paper paper = new ExportBuilder()
                .paper("p", "osdi", 2025)
                .topics("operating-systems-and-kernels", "memory_safety", "third-topic")
                .add()
                .snapshot().getPapers().get(0);

        assertEquals(List.of("Operating systems and kernels", "Memory safety"), paper.getTopicLabels());
    }

    @Test
    void relevanceTagIsOnlyShownForNonCorePapers() {
        List<Paper> papers = new ExportBuilder()
                .paper("core", "osdi", 2025).relevance("CORE_SYSTEMS").add()
                .paper("adjacent", "osdi", 2025).relevance("SYSTEMS_ADJACENT").add()
                .paper("review", "osdi", 2025).relevance("NEEDS_HUMAN_REVIEW").add()
                .snapshot().getPapers();

        assertFalse(papers.get(0).isShowRelevanceTag());
        assertTrue(papers.get(1).isShowRelevanceTag());
        assertEquals("Systems-adjacent", papers.get(1).getRelevanceLabel());
        assertEquals("Needs review", papers.get(2).getRelevanceLabel());
    }

    @Test
    void takeawayPresenceIsExposedForTheView() {
        List<Paper> papers = new ExportBuilder()
                .paper("with", "osdi", 2025).takeaway("One line.").add()
                .paper("without", "osdi", 2025).add()
                .snapshot().getPapers();

        assertTrue(papers.get(0).isHasTakeaway());
        assertFalse(papers.get(1).isHasTakeaway());
    }

    @Test
    void summaryAndReviewAreParsedFromTheExport() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode summary = mapper.createObjectNode()
                .put("status", "READY_FOR_REVIEW")
                .put("written_by", "agent-e4")
                .put("updated_at", "2026-07-30T03:44:52Z")
                .put("confidence", "HIGH")
                .put("background", "Slow hashes are expensive.")
                .put("villain", "Origins get overloaded.")
                .put("approach", "Split the check.")
                .put("impact", "Deployable today.")
                .put("limitations", "Assumes an honest CDN.")
                .put("phd_group_signal", "Protocols plus deployment.");
        summary.putArray("beginner_concepts").add("content delivery networks");
        ObjectNode evaluation = summary.putObject("evaluation");
        evaluation.put("overview", "Testbed plus Internet deployment.");
        evaluation.putArray("baselines").add("Forward everything");
        evaluation.putArray("results").addObject().put("text", "97 logins/s survive").put("locator", "Table 1");
        evaluation.putArray("study_types").add("prototype").add("measurement-study");
        summary.putObject("reading_coverage").put("abstract", "READ").put("introduction", "SKIMMED");

        ObjectNode review = mapper.createObjectNode()
                .put("status", "CHANGES_REQUESTED")
                .put("decision", "CHANGES_REQUESTED")
                .put("reviewer", "agent-h")
                .put("updated_at", "2026-07-30T04:09:26Z");
        review.putArray("issues").addObject()
                .put("severity", "ERROR").put("stage", "summary").put("description", "Too many results.").put("status", "OPEN");

        Paper paper = new ExportBuilder()
                .paper("p", "nsdi", 2025).abstractText("The abstract.").summary(summary).review(review).add()
                .snapshot().getPapers().get(0);

        assertTrue(paper.isHasAbstract());
        assertTrue(paper.isHasSummary());
        PaperSummary parsed = paper.getSummary();
        assertEquals("Ready for review", parsed.getStatusLabel());
        assertTrue(parsed.isUnreviewed());
        assertEquals("2026-07-30", parsed.getUpdatedDate());
        assertEquals("High", parsed.getConfidenceLabel());
        assertEquals("Split the check.", parsed.getApproach());
        assertTrue(parsed.isHasEvaluation());
        assertEquals(List.of("Forward everything"), parsed.getBaselines());
        assertEquals("97 logins/s survive", parsed.getResults().get(0).getText());
        assertEquals("Table 1", parsed.getResults().get(0).getLocator());
        assertEquals(List.of("Prototype", "Measurement study"), parsed.getStudyTypes());
        assertEquals("Read", parsed.getReadingCoverage().get("Abstract"));

        assertTrue(paper.isHasReview());
        assertEquals("Changes requested", paper.getReview().getDecisionLabel());
        assertEquals("agent-h", paper.getReview().getReviewer());
        assertEquals(1, paper.getReview().getIssues().size());
        assertEquals("ERROR", paper.getReview().getIssues().get(0).getSeverity());

        assertTrue(paper.getSearchText().contains("the abstract."));
        assertTrue(paper.getSearchText().contains("split the check."));
        assertTrue(paper.getSearchText().contains("content delivery networks"));
    }

    @Test
    void papersWithoutSummaryOrReviewExposeNulls() {
        Paper paper = new ExportBuilder().paper("p", "osdi", 2025).add().snapshot().getPapers().get(0);
        assertFalse(paper.isHasAbstract());
        assertFalse(paper.isHasSummary());
        assertNull(paper.getSummary());
        assertFalse(paper.isHasReview());
        assertNull(paper.getReview());
        assertEquals("", paper.getTrack());
    }
}
