package org.systemsphd.explorer.web;

import org.junit.jupiter.api.Test;
import org.systemsphd.explorer.support.ExportBuilder;
import org.systemsphd.explorer.support.Filters;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FilterStateTest {
    @Test
    void missingParametersMeanNoFilter() {
        FilterState filters = Filters.of();

        assertEquals("", filters.getQuery());
        assertEquals("", filters.getCategory());
        assertEquals("", filters.getVenue());
        assertNull(filters.getYear());
        assertEquals("", filters.getYearValue());
        assertEquals("", filters.getRelevance());
        assertEquals("", filters.getAvailability());
        assertEquals(1, filters.getPage());
        assertFalse(filters.isFiltered());
    }

    @Test
    void unknownValuesAreIgnoredInsteadOfFailing() {
        FilterState filters = Filters.of(
                "category", "astrology",
                "venue", "isca",
                "year", "1999",
                "relevance", "MAYBE",
                "availability", "sometimes",
                "page", "-4"
        );

        assertEquals("", filters.getCategory());
        assertEquals("", filters.getVenue());
        assertNull(filters.getYear());
        assertEquals("", filters.getRelevance());
        assertEquals("", filters.getAvailability());
        assertEquals(1, filters.getPage());
        assertFalse(filters.isFiltered());
    }

    @Test
    void nonNumericYearAndPageFallBack() {
        FilterState filters = Filters.of("year", "next", "page", "last");
        assertNull(filters.getYear());
        assertEquals(1, filters.getPage());
    }

    @Test
    void validValuesAreAccepted() {
        FilterState filters = Filters.of(
                "q", "  kernel  ",
                "category", "computer-networks",
                "year", "2025",
                "relevance", "SYSTEMS_ADJACENT",
                "availability", "either",
                "page", "3"
        );

        assertEquals("kernel", filters.getQuery());
        assertEquals("computer-networks", filters.getCategory());
        assertEquals(2025, filters.getYear());
        assertEquals("2025", filters.getYearValue());
        assertEquals("SYSTEMS_ADJACENT", filters.getRelevance());
        assertEquals("either", filters.getAvailability());
        assertEquals(3, filters.getPage());
        assertTrue(filters.isFiltered());
    }

    @Test
    void aVenueAlwaysImpliesItsOwnCategory() {
        FilterState filters = Filters.of("category", "computer-security", "venue", "osdi");
        assertEquals("osdi", filters.getVenue());
        assertEquals("operating-systems", filters.getCategory());
    }

    @Test
    void overlongQueriesAreTruncated() {
        String query = "x".repeat(FilterState.MAX_QUERY_LENGTH + 50);
        assertEquals(FilterState.MAX_QUERY_LENGTH, Filters.of("q", query).getQuery().length());
    }

    @Test
    void sidebarUrlsKeepSearchRelevanceAndAvailabilityButResetPaging() {
        FilterState filters = Filters.of(
                "q", "\"memory safety\" kernel",
                "venue", "sosp",
                "year", "2024",
                "relevance", "ALL",
                "availability", "code",
                "page", "4"
        );

        assertEquals(
                "/index.jsp?q=%22memory+safety%22+kernel&category=operating-systems&venue=osdi&year=2025&relevance=ALL&availability=code",
                filters.urlFor("operating-systems", "osdi", 2025)
        );
        assertEquals(
                "/index.jsp?q=%22memory+safety%22+kernel&category=computer-networks&relevance=ALL&availability=code",
                filters.urlFor("computer-networks", "", null)
        );
        assertEquals(
                "/index.jsp?q=%22memory+safety%22+kernel&relevance=ALL&availability=code",
                filters.urlFor("", "", null)
        );
    }

    @Test
    void pageUrlsKeepEveryFilter() {
        FilterState filters = Filters.of("q", "a&b", "venue", "nsdi", "year", "2023", "availability", "artifact");

        assertEquals(
                "/index.jsp?q=a%26b&category=computer-networks&venue=nsdi&year=2023&availability=artifact&page=2",
                filters.pageUrl(2)
        );
        assertEquals(
                "/index.jsp?q=a%26b&category=computer-networks&venue=nsdi&year=2023&availability=artifact",
                filters.pageUrl(1)
        );
    }

    @Test
    void removingOneFilterKeepsTheOthers() {
        FilterState filters = Filters.of("q", "rdma", "venue", "sigcomm", "year", "2025", "page", "3");

        assertEquals("/index.jsp?category=computer-networks&venue=sigcomm&year=2025", filters.urlWithout("q"));
        assertEquals("/index.jsp?q=rdma&category=computer-networks&year=2025", filters.urlWithout("venue"));
        assertEquals("/index.jsp?q=rdma&category=computer-networks&venue=sigcomm", filters.urlWithout("year"));
        assertEquals("/index.jsp", filters.clearUrl());
    }

    @Test
    void urlsStartWithTheConfiguredBasePath() {
        FilterState filters = Filters.at("/explorer/", "q", "tail latency");

        assertEquals("/explorer/", filters.getBasePath());
        assertEquals("/explorer/", filters.clearUrl());
        assertEquals("/explorer/?q=tail+latency&page=2", filters.pageUrl(2));
        assertEquals("/explorer/", Filters.at("/explorer/").urlFor("", "", null));
    }

    @Test
    void keywordParametersAreValidatedDedupedAndPreservedInUrls() {
        FacetIndex facets = new ExportBuilder()
                .paper("a", "osdi", 2025).topics("ml-systems", "gpu-scheduling").layers("runtime").methods("fuzzing").add()
                .service().getFacets();

        FilterState filters = Filters.with(facets,
                "topic", "ml-systems", "topic", "unknown", "topic", "ml-systems", "topic", "gpu-scheduling",
                "layer", "runtime", "method", "fuzzing", "match", "all", "page", "3");

        assertEquals(List.of("ml-systems", "gpu-scheduling"), filters.getTopics());
        assertEquals(List.of("runtime"), filters.getLayers());
        assertEquals(List.of("fuzzing"), filters.getMethods());
        assertTrue(filters.isMatchAll());
        assertEquals(4, filters.getKeywordCount());
        assertTrue(filters.isFiltered());
        assertEquals(
                "/index.jsp?topic=ml-systems&topic=gpu-scheduling&layer=runtime&method=fuzzing&match=all&page=3",
                filters.pageUrl(3)
        );
        assertEquals("/index.jsp?topic=gpu-scheduling&layer=runtime&method=fuzzing&match=all",
                filters.urlWithout("topic", "ml-systems"));
        assertEquals("/index.jsp?layer=runtime&method=fuzzing&match=all", filters.urlWithout("topic"));
        assertEquals(List.of("topic", "topic", "layer", "method", "match"),
                filters.getHiddenParameters().stream().map(java.util.Map.Entry::getKey).toList());
    }

    @Test
    void matchDefaultsToAnyAndIsNotEmittedInUrls() {
        FilterState filters = Filters.of("match", "sometimes");
        assertFalse(filters.isMatchAll());
        assertEquals("any", filters.getMatch());
        assertEquals("/index.jsp", filters.pageUrl(1));
    }
}
