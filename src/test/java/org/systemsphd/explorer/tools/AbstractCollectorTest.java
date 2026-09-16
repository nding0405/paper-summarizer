package org.systemsphd.explorer.tools;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbstractCollectorTest {
    private static final String LONG =
            "This sentence is long enough to count as a real abstract for testing purposes, honestly.";

    @Test
    void cleaningStripsTagsEntitiesAndLeadingLabel() {
        assertEquals("We & they build\n\nSecond.",
                AbstractCollector.cleanText("<jats:p>Abstract: We &amp; they   build</jats:p><jats:p>Second.</jats:p>"));
        assertEquals("café — done", AbstractCollector.cleanText("caf&eacute; &mdash; done".replace("&eacute;", "&#233;")));
    }

    @Test
    void shortOrPlaceholderAbstractsAreRejected() {
        assertNull(AbstractCollector.usableAbstract(null));
        assertNull(AbstractCollector.usableAbstract("Too short."));
        assertNull(AbstractCollector.usableAbstract("No abstract available for this paper at this time, sorry about that."));
        assertEquals(LONG, AbstractCollector.usableAbstract(LONG));
    }

    @Test
    void usenixPagesYieldTheDescriptionBlockWithParagraphs() {
        String page = "<div class=\"field field-name-field-paper-description field-type-text-long\">"
                + "<div class=\"field-items\"><div class=\"field-item even\"><p>" + LONG + "</p><p>" + LONG + "</p></div></div></div>";
        assertEquals(LONG + "\n\n" + LONG, AbstractCollector.extractUsenix(page));
        assertNull(AbstractCollector.extractUsenix("<html><body>no abstract here</body></html>"));
    }

    @Test
    void ndssPagesKeepEveryAbstractParagraphAndDropAuthorsAndButtons() {
        String page = "<div class=\"paper-data\"> <p><strong> <p>Author One (Uni), Author Two (Uni)</p> </strong></p> "
                + "<p><p>" + LONG + "</p> <p>" + LONG + "<br /> After the break.</p> </p> </div> "
                + "<div class=\"paper-buttons\"><a>Paper</a></div></div><h2>View More Papers</h2><p>" + LONG + "</p>";
        assertEquals(LONG + "\n\n" + LONG + "\n\nAfter the break.", AbstractCollector.extractNdss(page));
        assertNull(AbstractCollector.extractNdss("<div>nothing</div>"));
    }

    @Test
    void openAlexInvertedIndexIsReordered() throws Exception {
        ObjectNode index = JsonFiles.mapper().createObjectNode();
        String[] words = LONG.split(" ");
        for (int position = 0; position < words.length; position++) {
            index.withArray(words[position]).add(position);
        }
        assertEquals(LONG, AbstractCollector.openAlexAbstract(index));
        assertNull(AbstractCollector.openAlexAbstract(null));
        assertNull(AbstractCollector.openAlexAbstract(JsonFiles.mapper().createObjectNode()));
    }

    @Test
    void titleMatchingIgnoresCaseAndPunctuation() {
        assertTrue(AbstractCollector.titlesMatch("Fast: A Storage System!", "fast a storage system"));
        assertTrue(AbstractCollector.titlesMatch("Fast: A Storage System", "Fast: A Storage Systems"));
        assertFalse(AbstractCollector.titlesMatch("Fast: A Storage System", "Slow: A Network Protocol"));
        assertFalse(AbstractCollector.titlesMatch("Fast", null));
    }

    @Test
    void doisAndLanesAreNormalized() {
        assertEquals("10.1145/1.2", AbstractCollector.normalizeDoi("https://doi.org/10.1145/1.2"));
        assertEquals("10.1145/1.2", AbstractCollector.normalizeDoi("doi:10.1145/1.2"));
        assertNull(AbstractCollector.normalizeDoi(null));
        assertEquals("usenix", AbstractCollector.laneFor(metadata("https://www.usenix.org/x")));
        assertEquals("ndss", AbstractCollector.laneFor(metadata("https://www.ndss-symposium.org/x")));
        assertEquals("api", AbstractCollector.laneFor(metadata("https://dl.acm.org/doi/10.1145/1")));
        assertEquals("api", AbstractCollector.laneFor(metadata(null)));
    }

    @Test
    void abstractIsInsertedAfterTheTitleAndReplacesAnEarlierSource() {
        ObjectNode metadata = metadata("https://www.usenix.org/x");
        metadata.put("paper_id", "osdi-2025-example-01234567").put("title", "Example").put("venue", "osdi").put("year", 2025);
        metadata.put("abstract", "old").put("abstract_source_id", "src-abstract-osdi-2025-01234567");
        metadata.putArray("sources")
                .add(JsonFiles.mapper().createObjectNode().put("id", "src-program"))
                .add(JsonFiles.mapper().createObjectNode().put("id", "src-abstract-osdi-2025-01234567"));

        assertEquals("src-abstract-osdi-2025-01234567", AbstractCollector.uniqueSourceId(metadata));
        ObjectNode source = AbstractCollector.sourceRecord("src-abstract-osdi-2025-01234567", "https://x", "paper-page",
                "Example", "USENIX", true, "body".getBytes());
        ObjectNode updated = AbstractCollector.withAbstract(metadata, LONG, source);

        List<String> keys = new java.util.ArrayList<>();
        updated.fieldNames().forEachRemaining(keys::add);
        assertEquals(List.of("links", "paper_id", "title", "abstract", "abstract_source_id", "venue", "year", "sources"), keys);
        assertEquals(LONG, updated.path("abstract").asText());
        assertEquals(2, updated.path("sources").size());
        assertEquals("src-program", updated.path("sources").get(0).path("id").asText());
        assertEquals("src-abstract-osdi-2025-01234567", updated.path("sources").get(1).path("id").asText());
        assertTrue(updated.path("sources").get(1).path("content_hash").asText().startsWith("sha256:"));
        assertEquals("old", metadata.path("abstract").asText(), "the input is not modified");
    }

    private static ObjectNode metadata(String officialPage) {
        ObjectNode metadata = JsonFiles.mapper().createObjectNode();
        ObjectNode links = metadata.putObject("links");
        if (officialPage == null) {
            links.putNull("official_page");
        } else {
            links.put("official_page", officialPage);
        }
        return metadata;
    }
}
