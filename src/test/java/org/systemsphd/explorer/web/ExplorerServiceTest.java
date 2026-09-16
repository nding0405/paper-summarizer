package org.systemsphd.explorer.web;

import org.junit.jupiter.api.Test;
import org.systemsphd.explorer.catalog.ConferenceCatalog;
import org.systemsphd.explorer.catalog.Taxonomy;
import org.systemsphd.explorer.model.Paper;
import org.systemsphd.explorer.support.CoverageFixture;
import org.systemsphd.explorer.support.ExportBuilder;
import org.systemsphd.explorer.support.Filters;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExplorerServiceTest {
    /** A small corpus spanning every category, several years, and every relevance class. */
    private static ExplorerService corpus() {
        return new ExportBuilder()
                .paper("osdi-2025-a", "osdi", 2025).title("Zeta kernel isolation")
                        .authors("Ada Lovelace", "Grace Hopper").takeaway("Safety for kernel memory extensions.")
                        .topics("kernel-extensibility").methods("implementation").layers("kernel")
                        .institutions("North University").locations("Vancouver, Canada")
                        .facultyIds("faculty-hopper").groupIds("group-safe-kernels")
                        .code("https://example.org/code").add()
                .paper("osdi-2025-b", "osdi", 2025).title("alpha memory safety")
                        .authors("Linus Example").topics("memory-safety")
                        .artifact("https://example.org/artifact").add()
                .paper("sosp-2025-a", "sosp", 2025).title("Distributed debugging")
                        .authors("Barbara Liskov").relevance("SYSTEMS_ADJACENT").add()
                .paper("osdi-2024-a", "osdi", 2024).title("Older kernel paper")
                        .authors("Ada Lovelace").relevance("NEEDS_HUMAN_REVIEW").add()
                .paper("nsdi-2024-a", "nsdi", 2024).title("Congestion control revisited")
                        .authors("Van Jacobson").code("https://example.org/cc").artifact("https://example.org/cc-artifact").add()
                .paper("ccs-2024-a", "ccs", 2024).title("Isolation for enclaves")
                        .relevance("CORE_SYSTEMS").add()
                .paper("asplos-2023-a", "asplos", 2023).title("Accelerator scheduling").add()
                .paper("sp-2025-x", "sp", 2025).title("Excluded crypto paper")
                        .relevance("EXCLUDED").add()
                .paper("osdi-2025-x", "osdi", 2025).title("Excluded kernel paper")
                        .relevance("EXCLUDED").topics("kernel-extensibility").add()
                .paper("isca-2025-a", "isca", 2025).title("Unknown venue paper").add()
                .service();
    }

    private static List<String> ids(ExplorerPage page) {
        return page.getPapers().stream().map(Paper::getId).toList();
    }

    @Test
    void excludedPapersAreHiddenByDefault() {
        ExplorerPage page = corpus().page(Filters.of());

        assertEquals(8, page.getTotalResults());
        assertFalse(ids(page).contains("sp-2025-x"));
        assertFalse(ids(page).contains("osdi-2025-x"));
        assertEquals("All papers", page.getHeading());
        assertTrue(page.getActiveFilters().isEmpty());
    }

    @Test
    void excludedPapersCanBeRequestedExplicitly() {
        ExplorerPage excludedOnly = corpus().page(Filters.of("relevance", "EXCLUDED"));
        assertEquals(List.of("osdi-2025-x", "sp-2025-x"), ids(excludedOnly));

        ExplorerPage everything = corpus().page(Filters.of("relevance", "ALL"));
        assertEquals(10, everything.getTotalResults());
        assertEquals("All classes, including excluded", everything.getActiveFilters().get(0).getValue());
    }

    @Test
    void relevanceClassFilterMatchesExactly() {
        assertEquals(List.of("sosp-2025-a"), ids(corpus().page(Filters.of("relevance", "SYSTEMS_ADJACENT"))));
        assertEquals(List.of("osdi-2024-a"), ids(corpus().page(Filters.of("relevance", "NEEDS_HUMAN_REVIEW"))));
        assertEquals(6, corpus().page(Filters.of("relevance", "CORE_SYSTEMS")).getTotalResults());
    }

    @Test
    void resultsSortByYearDescendingThenVenueThenTitleIgnoringCase() {
        // The unknown-venue paper is still listed; it simply has no sidebar entry.
        assertEquals(
                List.of("isca-2025-a", "osdi-2025-b", "osdi-2025-a", "sosp-2025-a", "ccs-2024-a",
                        "nsdi-2024-a", "osdi-2024-a", "asplos-2023-a"),
                ids(corpus().page(Filters.of()))
        );
    }

    @Test
    void categoryFilterCoversEveryConferenceInTheCategory() {
        ExplorerPage page = corpus().page(Filters.of("category", "operating-systems"));

        assertEquals(List.of("osdi-2025-b", "osdi-2025-a", "sosp-2025-a", "osdi-2024-a"), ids(page));
        assertEquals("Operating systems", page.getHeading());
        assertEquals("Category", page.getActiveFilters().get(0).getName());
        assertEquals("/index.jsp", page.getActiveFilters().get(0).getRemoveHref());
    }

    @Test
    void venueFilterNarrowsWithinTheCategory() {
        ExplorerPage page = corpus().page(Filters.of("category", "operating-systems", "venue", "sosp"));

        assertEquals(List.of("sosp-2025-a"), ids(page));
        assertEquals("SOSP", page.getHeading());
        assertEquals("Conference", page.getActiveFilters().get(0).getName());
        assertEquals("/index.jsp?category=operating-systems", page.getActiveFilters().get(0).getRemoveHref());
    }

    @Test
    void yearFilterCombinesWithVenue() {
        ExplorerPage page = corpus().page(Filters.of("venue", "osdi", "year", "2024"));

        assertEquals(List.of("osdi-2024-a"), ids(page));
        assertEquals("OSDI 2024", page.getHeading());
        assertEquals(List.of("Conference", "Year"),
                page.getActiveFilters().stream().map(ActiveFilter::getName).toList());

        assertEquals("2024", corpus().page(Filters.of("year", "2024")).getHeading());
        assertEquals(3, corpus().page(Filters.of("year", "2024")).getTotalResults());
    }

    @Test
    void availabilityFilters() {
        assertEquals(List.of("osdi-2025-a", "nsdi-2024-a"), ids(corpus().page(Filters.of("availability", "code"))));
        assertEquals(List.of("osdi-2025-b", "nsdi-2024-a"), ids(corpus().page(Filters.of("availability", "artifact"))));
        assertEquals(List.of("osdi-2025-b", "osdi-2025-a", "nsdi-2024-a"),
                ids(corpus().page(Filters.of("availability", "either"))));
        assertEquals("Has code", corpus().page(Filters.of("availability", "code")).getActiveFilters().get(0).getValue());
    }

    @Test
    void searchTermsAreAndedAndCaseInsensitive() {
        assertEquals(List.of("osdi-2025-a", "osdi-2024-a"), ids(corpus().page(Filters.of("q", "KERNEL"))));
        assertEquals(List.of("osdi-2025-a"), ids(corpus().page(Filters.of("q", "kernel zeta"))));
        assertTrue(ids(corpus().page(Filters.of("q", "kernel congestion"))).isEmpty());
    }

    @Test
    void quotedPhrasesMustMatchContiguously() {
        assertEquals(List.of("osdi-2025-b"), ids(corpus().page(Filters.of("q", "\"memory safety\""))));
        assertEquals(List.of("osdi-2025-b", "osdi-2025-a"), ids(corpus().page(Filters.of("q", "memory safety"))));
        assertTrue(ids(corpus().page(Filters.of("q", "\"safety memory\""))).isEmpty());
        assertEquals(List.of("osdi-2025-a"), ids(corpus().page(Filters.of("q", "\"kernel isolation\" Hopper"))));
    }

    @Test
    void searchCoversPeopleTopicsMethodsLayersInstitutionsLocationsFacultyAndGroups() {
        for (String term : List.of("lovelace", "kernel-extensibility", "implementation", "vancouver",
                "north university", "faculty-hopper", "group-safe-kernels", "kernel memory extensions")) {
            assertTrue(ids(corpus().page(Filters.of("q", term))).contains("osdi-2025-a"), term);
        }
    }

    @Test
    void searchWithOnlyQuotesOrWhitespaceMatchesEverything() {
        assertEquals(8, corpus().page(Filters.of("q", "\"\"")).getTotalResults());
        assertEquals(8, corpus().page(Filters.of("q", "\"")).getTotalResults());
        assertEquals(8, corpus().page(Filters.of("q", "\" \"")).getTotalResults());
        assertEquals(8, corpus().page(Filters.of("q", "   ")).getTotalResults());
        assertEquals(List.of("osdi-2025-a", "osdi-2024-a"), ids(corpus().page(Filters.of("q", "kernel\""))));
    }

    @Test
    void searchFilterChipRemovesOnlyTheQuery() {
        ExplorerPage page = corpus().page(Filters.of("q", "kernel", "venue", "osdi"));
        ActiveFilter search = page.getActiveFilters().get(0);
        assertEquals("Search", search.getName());
        assertEquals("kernel", search.getValue());
        assertEquals("/index.jsp?category=operating-systems&venue=osdi", search.getRemoveHref());
    }

    @Test
    void paginationSplitsResultsIntoPagesOfForty() {
        ExportBuilder builder = new ExportBuilder();
        for (int index = 0; index < 95; index++) {
            builder.paper(String.format("osdi-2025-%03d", index), "osdi", 2025).add();
        }
        ExplorerService service = builder.service();

        ExplorerPage first = service.page(Filters.of("venue", "osdi"));
        assertEquals(95, first.getTotalResults());
        assertEquals(3, first.getTotalPages());
        assertEquals(40, first.getPapers().size());
        assertEquals(1, first.getFirstResult());
        assertEquals(40, first.getLastResult());
        assertFalse(first.isHasPrevious());
        assertEquals("/index.jsp?category=operating-systems&venue=osdi&page=2", first.getNextUrl());

        ExplorerPage second = service.page(Filters.of("venue", "osdi", "page", "2"));
        assertEquals(41, second.getFirstResult());
        assertEquals(80, second.getLastResult());
        assertEquals("/index.jsp?category=operating-systems&venue=osdi", second.getPreviousUrl());
        assertEquals("/index.jsp?category=operating-systems&venue=osdi&page=3", second.getNextUrl());

        ExplorerPage last = service.page(Filters.of("venue", "osdi", "page", "3"));
        assertEquals(15, last.getPapers().size());
        assertEquals(95, last.getLastResult());
        assertNull(last.getNextUrl());
    }

    @Test
    void pageNumbersBeyondTheEndClampToTheLastPage() {
        ExplorerPage page = corpus().page(Filters.of("page", "99"));
        assertEquals(8, page.getPapers().size());
        assertEquals(1, page.getTotalPages());
        assertNull(page.getNextUrl());
        assertNull(page.getPreviousUrl());
    }

    @Test
    void emptyResultsStillProduceOnePage() {
        ExplorerPage page = corpus().page(Filters.of("q", "nonexistent-term"));
        assertTrue(page.isEmpty());
        assertEquals(0, page.getTotalResults());
        assertEquals(0, page.getFirstResult());
        assertEquals(0, page.getLastResult());
        assertEquals(1, page.getTotalPages());
    }

    @Test
    void navigationCountsIgnoreExcludedAndUnknownVenuePapersAndTheCurrentSearch() {
        ExplorerPage page = corpus().page(Filters.of("q", "kernel"));
        List<NavigationCategory> navigation = page.getNavigation();

        assertEquals(List.of("Operating systems", "Computer networks", "Computer security", "Computer architecture"),
                navigation.stream().map(NavigationCategory::getName).toList());
        // Seven default-visible papers belong to catalogued conferences; the
        // unknown-venue paper is listed but not navigable.
        assertEquals(7, page.getNavigationTotal());

        NavigationCategory operatingSystems = navigation.get(0);
        assertEquals(4, operatingSystems.getCount());
        NavigationVenue osdi = operatingSystems.getVenues().get(0);
        assertEquals("OSDI", osdi.getName());
        assertEquals(3, osdi.getCount());
        assertEquals(List.of(2025, 2024), osdi.getYears().stream().map(NavigationYear::getYear).toList());
        assertEquals(List.of(2L, 1L), osdi.getYears().stream().map(NavigationYear::getCount).toList());

        NavigationVenue fast = operatingSystems.getVenues().get(4);
        assertEquals(0, fast.getCount());
        assertTrue(fast.getYears().isEmpty());

        assertEquals(1, navigation.get(1).getCount());
        assertEquals(1, navigation.get(2).getCount());
        assertEquals(1, navigation.get(3).getCount());
    }

    @Test
    void navigationLinksPreserveSearchAndFiltersButNotThePage() {
        ExplorerPage page = corpus().page(Filters.of("q", "kernel", "relevance", "ALL", "page", "2"));
        NavigationCategory operatingSystems = page.getNavigation().get(0);
        NavigationVenue osdi = operatingSystems.getVenues().get(0);

        assertEquals("/index.jsp?q=kernel&category=operating-systems&relevance=ALL", operatingSystems.getHref());
        assertEquals("/index.jsp?q=kernel&category=operating-systems&venue=osdi&relevance=ALL", osdi.getHref());
        assertEquals("/index.jsp?q=kernel&category=operating-systems&venue=osdi&year=2025&relevance=ALL",
                osdi.getYears().get(0).getHref());
    }

    @Test
    void navigationMarksTheSelectedScope() {
        ExplorerPage categoryPage = corpus().page(Filters.of("category", "operating-systems"));
        assertTrue(categoryPage.getNavigation().get(0).isSelected());
        assertFalse(categoryPage.getNavigation().get(0).getVenues().get(0).isSelected());

        ExplorerPage venuePage = corpus().page(Filters.of("venue", "osdi"));
        assertFalse(venuePage.getNavigation().get(0).isSelected());
        assertTrue(venuePage.getNavigation().get(0).getVenues().get(0).isSelected());
        assertFalse(venuePage.getNavigation().get(0).getVenues().get(0).getYears().get(0).isSelected());

        ExplorerPage yearPage = corpus().page(Filters.of("venue", "osdi", "year", "2024"));
        assertFalse(yearPage.getNavigation().get(0).getVenues().get(0).isSelected());
        assertTrue(yearPage.getNavigation().get(0).getVenues().get(0).getYears().get(1).isSelected());
    }

    @Test
    void venueNamesAreExposedForRendering() {
        ExplorerPage page = corpus().page(Filters.of());
        assertEquals("USENIX ATC", page.getVenueNames().get("atc"));
        assertEquals("IEEE S&P", page.getVenueNames().get("sp"));
        assertEquals(12, page.getVenueNames().size());
    }

    private static ExplorerService coveredCorpus() {
        return new ExplorerService(new ExportBuilder()
                .paper("osdi-2025-a", "osdi", 2025).add()
                .paper("sosp-2026-a", "sosp", 2026).add()
                .snapshot(), new ConferenceCatalog(), CoverageFixture.load(), Taxonomy.empty());
    }

    @Test
    void sidebarListsCoveredVenueYearsEvenWithoutPapers() {
        ExplorerPage page = coveredCorpus().page(Filters.of());
        NavigationCategory operatingSystems = page.getNavigation().get(0);

        NavigationVenue osdi = operatingSystems.getVenues().get(0);
        assertEquals(List.of(2026, 2025), osdi.getYears().stream().map(NavigationYear::getYear).toList());
        assertEquals(List.of(0L, 1L), osdi.getYears().stream().map(NavigationYear::getCount).toList());
        // Complete coverage with zero visible papers still shows its count.
        assertTrue(osdi.getYears().get(0).isShowCount());
        assertEquals("", osdi.getYears().get(0).getMark());
        assertEquals(1, osdi.getCount());

        NavigationVenue atc = operatingSystems.getVenues().get(3);
        assertEquals(1, atc.getYears().size());
        NavigationYear atc2026 = atc.getYears().get(0);
        assertEquals(2026, atc2026.getYear());
        assertFalse(atc2026.isShowCount());
        assertEquals("unavailable", atc2026.getMark());
        assertEquals("/index.jsp?category=operating-systems&venue=atc&year=2026", atc2026.getHref());

        NavigationYear sosp2026 = operatingSystems.getVenues().get(1).getYears().get(0);
        assertEquals(1, sosp2026.getCount());
        assertTrue(sosp2026.isShowCount());
        assertEquals("accepted only", sosp2026.getMark());

        NavigationYear ccs2026 = page.getNavigation().get(2).getVenues().get(0).getYears().get(0);
        assertEquals("upcoming", ccs2026.getMark());
        assertFalse(ccs2026.isShowCount());
    }

    @Test
    void selectingAnUpcomingVenueYearExplainsInsteadOfReportingZeroPapers() {
        ExplorerPage page = coveredCorpus().page(Filters.of("venue", "ccs", "year", "2026"));

        assertTrue(page.isEmpty());
        assertTrue(page.isNoProgram());
        assertEquals("Program not published yet", page.getCoverage().getLabel());
        assertEquals("ACM CCS 2026", page.getHeading());
    }

    @Test
    void coverageNoticeOnlyAppearsForASingleVenueYear() {
        assertNull(coveredCorpus().page(Filters.of("venue", "sosp")).getCoverage());
        assertNull(coveredCorpus().page(Filters.of("year", "2026")).getCoverage());
        assertNull(coveredCorpus().page(Filters.of("venue", "osdi", "year", "2024")).getCoverage());

        ExplorerPage sosp = coveredCorpus().page(Filters.of("venue", "sosp", "year", "2026"));
        assertEquals("Accepted papers only", sosp.getCoverage().getLabel());
        assertFalse(sosp.isNoProgram());
        assertEquals(1, sosp.getTotalResults());

        ExplorerPage osdi = coveredCorpus().page(Filters.of("venue", "osdi", "year", "2025"));
        assertTrue(osdi.getCoverage().isComplete());
        assertFalse(osdi.isNoProgram());
    }

    @Test
    void withoutAManifestTheSidebarOnlyListsYearsWithPapers() {
        NavigationVenue osdi = corpus().page(Filters.of()).getNavigation().get(0).getVenues().get(0);
        assertTrue(osdi.getYears().stream().allMatch(year -> year.getCount() > 0));
        assertTrue(osdi.getYears().stream().allMatch(year -> year.getCoverage() == null));
    }

    private static ExplorerService rankedCorpus() {
        return new ExportBuilder()
                .paper("title-hit", "osdi", 2023).title("Fast RDMA kernels").add()
                .paper("abstract-hit", "osdi", 2025).title("Unrelated title")
                        .abstractText("We study RDMA offload in the kernel.").add()
                .paper("other-hit", "osdi", 2026).title("Another title").topics("rdma").add()
                .paper("excluded-hit", "osdi", 2026).title("RDMA excluded").relevance("EXCLUDED").add()
                .service();
    }

    @Test
    void queryResultsRankTitleMatchesAboveAbstractMatchesAboveTheRest() {
        ExplorerPage page = rankedCorpus().page(Filters.of("q", "rdma"));
        assertEquals(List.of("title-hit", "abstract-hit", "other-hit"), ids(page));

        // Without a query the newest paper comes first.
        assertEquals(List.of("other-hit", "abstract-hit", "title-hit"), ids(rankedCorpus().page(Filters.of())));
    }

    @Test
    void suggestionsAreRankedDefaultVisibleMatches() {
        List<String> suggested = rankedCorpus().suggest("rdma").stream().map(Paper::getId).toList();
        assertEquals(List.of("title-hit", "abstract-hit", "other-hit"), suggested);
        assertTrue(rankedCorpus().suggest("   ").isEmpty());
        assertTrue(rankedCorpus().suggest("\"\"").isEmpty());
    }

    @Test
    void suggestionsAreLimited() {
        ExportBuilder builder = new ExportBuilder();
        for (int index = 0; index < 30; index++) {
            builder.paper("p" + index, "osdi", 2025).title("Common word " + index).add();
        }
        assertEquals(ExplorerService.SUGGESTION_LIMIT, builder.service().suggest("common").size());
    }

    @Test
    void searchCoversAbstractAndSummaryText() {
        ExplorerService service = new ExportBuilder()
                .paper("with-abstract", "osdi", 2025).abstractText("An abstract about tail latency.").add()
                .paper("with-summary", "osdi", 2025)
                        .summary(new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode()
                                .put("status", "READY_FOR_REVIEW")
                                .put("approach", "The approach shards the coordinator."))
                        .add()
                .paper("plain", "osdi", 2025).add()
                .service();
        assertEquals(List.of("with-abstract"), ids(service.page(Filters.of("q", "\"tail latency\""))));
        assertEquals(List.of("with-summary"), ids(service.page(Filters.of("q", "shards coordinator"))));
    }

    @Test
    void papersCanBeLookedUpById() {
        assertEquals("osdi-2025-a", corpus().find("osdi-2025-a").getId());
        assertNull(corpus().find("missing"));
        assertNull(corpus().find(null));
    }

    private static ExplorerService facetedCorpus() {
        return new ExportBuilder()
                .paper("ml-gpu", "osdi", 2025).topics("ml-systems", "gpu-scheduling").layers("runtime").methods("implementation").add()
                .paper("ml-only", "osdi", 2025).topics("ml-systems").layers("kernel").methods("simulation").add()
                .paper("gpu-only", "osdi", 2025).topics("gpu-scheduling").layers("runtime").add()
                .paper("neither", "osdi", 2025).topics("networking").add()
                .paper("excluded-ml", "osdi", 2025).topics("ml-systems").relevance("EXCLUDED").add()
                .service();
    }

    @Test
    void facetIndexCountsDefaultVisiblePapers() {
        FacetIndex facets = facetedCorpus().getFacets();
        FacetIndex.Facet topics = facets.get(FacetIndex.TOPIC);
        assertEquals(List.of("gpu-scheduling", "ml-systems", "networking"),
                topics.getOptions().stream().map(FacetIndex.Option::getId).toList());
        assertEquals(2, topics.getOptions().get(1).getCount());
        assertEquals("ML systems", topics.labelFor("ml-systems"));
        assertTrue(facets.isValid(FacetIndex.LAYER, "runtime"));
        assertFalse(facets.isValid(FacetIndex.LAYER, "nonsense"));
        assertEquals(2, facets.get(FacetIndex.METHOD).getOptions().size());
    }

    @Test
    void keywordFacetsMatchAnyByDefaultAndAllOnRequest() {
        ExplorerService service = facetedCorpus();
        FacetIndex facets = service.getFacets();

        ExplorerPage any = service.page(Filters.with(facets, "topic", "ml-systems", "topic", "gpu-scheduling"));
        assertEquals(List.of("gpu-only", "ml-gpu", "ml-only"), ids(any));
        assertEquals(List.of("Topic", "Topic"), any.getActiveFilters().stream().map(ActiveFilter::getName).toList());
        assertEquals("/index.jsp?topic=gpu-scheduling", any.getActiveFilters().get(0).getRemoveHref());

        ExplorerPage all = service.page(Filters.with(facets, "topic", "ml-systems", "topic", "gpu-scheduling", "match", "all"));
        assertEquals(List.of("ml-gpu"), ids(all));
        assertEquals("Keywords", all.getActiveFilters().get(2).getName());
        assertEquals("/index.jsp?topic=ml-systems&topic=gpu-scheduling", all.getActiveFilters().get(2).getRemoveHref());
    }

    @Test
    void facetsCombineWithEachOtherUsingAnd() {
        ExplorerService service = facetedCorpus();
        FacetIndex facets = service.getFacets();
        assertEquals(List.of("ml-gpu"), ids(service.page(Filters.with(facets, "topic", "ml-systems", "layer", "runtime"))));
        assertEquals(List.of("ml-only"), ids(service.page(Filters.with(facets, "method", "simulation"))));
        assertTrue(ids(service.page(Filters.with(facets, "topic", "networking", "method", "simulation"))).isEmpty());
    }

    @Test
    void unknownKeywordsAreIgnored() {
        ExplorerService service = facetedCorpus();
        ExplorerPage page = service.page(Filters.with(service.getFacets(), "topic", "made-up", "topic", "ml-systems"));
        assertEquals(List.of("ml-gpu", "ml-only"), ids(page));
        assertEquals(1, page.getFilterCount());
    }

    @Test
    void paginationOffersMultiPageJumps() {
        ExportBuilder builder = new ExportBuilder();
        for (int index = 0; index < 40 * 30; index++) {
            builder.paper(String.format("p%04d", index), "osdi", 2025).add();
        }
        ExplorerService service = builder.service();

        ExplorerPage first = service.page(Filters.of());
        assertEquals(30, first.getTotalPages());
        assertEquals(1, first.getCurrentPage());
        assertTrue(first.getBackwardJumps().isEmpty());
        assertEquals(List.of("+5", "+10"), first.getForwardJumps().stream().map(PageJump::getLabel).toList());
        assertEquals("/index.jsp?page=11", first.getForwardJumps().get(1).getHref());

        ExplorerPage middle = service.page(Filters.of("page", "12"));
        assertEquals(List.of(2, 7), middle.getBackwardJumps().stream().map(PageJump::getPage).toList());
        assertEquals(List.of(17, 22), middle.getForwardJumps().stream().map(PageJump::getPage).toList());

        ExplorerPage nearEnd = service.page(Filters.of("page", "28"));
        assertEquals(List.of(18, 23), nearEnd.getBackwardJumps().stream().map(PageJump::getPage).toList());
        assertTrue(nearEnd.getForwardJumps().isEmpty());

        ExplorerPage clamped = service.page(Filters.of("page", "500"));
        assertEquals(30, clamped.getCurrentPage());
    }

    private static ExplorerService programCorpus() {
        return new ExportBuilder()
                .paper("a", "asplos", 2025).title("Zebra paper").track("Session 10A: GPGPU").add()
                .paper("b", "asplos", 2025).title("apple paper").track("Session 2D: ML Inference Systems").add()
                .paper("c", "asplos", 2025).title("Banana paper").track("Session 2D: ML Inference Systems").add()
                .paper("d", "asplos", 2025).title("Excluded but listed").track("Session 1A: ML Acceleration").relevance("EXCLUDED").add()
                .paper("e", "asplos", 2025).title("No session").add()
                .paper("f", "asplos", 2024).title("Older").track("Session 1A").add()
                .paper("g", "eurosys", 2026).title("Untracked venue").add()
                .service();
    }

    @Test
    void proceedingsGroupEveryPaperBySessionInProgramOrder() {
        ProceedingsPage page = programCorpus().proceedings("asplos", "2025", "/proceedings.jsp", "/index.jsp");

        assertFalse(page.isShowingOverview());
        assertEquals("ASPLOS 2025", page.getHeading());
        assertEquals(5, page.getTotalPapers());
        assertEquals(
                List.of("Session 1A: ML Acceleration", "Session 2D: ML Inference Systems", "Session 10A: GPGPU", ""),
                page.getSessions().stream().map(ProceedingsSession::getName).toList()
        );
        assertEquals(List.of("b", "c"), page.getSessions().get(1).getPapers().stream().map(Paper::getId).toList());
        assertEquals("session-session-2d-ml-inference-systems", page.getSessions().get(1).getAnchor());
        assertEquals("papers", page.getSessions().get(3).getAnchor());
        assertTrue(page.isHasSessions());
        assertEquals(List.of(2025, 2024), page.getVenueYears().stream().map(NavigationYear::getYear).toList());
        assertTrue(page.getVenueYears().get(0).isSelected());
        assertEquals("/proceedings.jsp?venue=asplos&year=2024", page.getVenueYears().get(1).getHref());
        assertEquals("/index.jsp?category=computer-architecture&venue=asplos&year=2025&relevance=ALL", page.getSearchUrl());
    }

    @Test
    void proceedingsFallBackToTheLatestYearAndToTheOverview() {
        ProceedingsPage latest = programCorpus().proceedings("asplos", "1999", "/proceedings.jsp", "/index.jsp");
        assertEquals(2025, latest.getYear());

        ProceedingsPage untracked = programCorpus().proceedings("eurosys", null, "/proceedings.jsp", "/index.jsp");
        assertEquals(1, untracked.getSessions().size());
        assertFalse(untracked.isHasSessions());

        ProceedingsPage overview = programCorpus().proceedings("isca", null, "/proceedings.jsp", "/index.jsp");
        assertTrue(overview.isShowingOverview());
        assertEquals("Proceedings", overview.getHeading());
        assertEquals(4, overview.getOverview().size());
    }

    @Test
    void sessionNamesSortNaturally() {
        assertTrue(ExplorerService.compareNaturally("Session 2D", "Session 10A") < 0);
        assertTrue(ExplorerService.compareNaturally("Session 10A", "Session 9B") > 0);
        assertTrue(ExplorerService.compareNaturally("Cloud", "Storage") < 0);
        assertTrue(ExplorerService.compareNaturally("", "Anything") > 0);
        assertEquals(0, ExplorerService.compareNaturally("Same", "same"));
    }

    @Test
    void availabilityCanRequireASummaryOrAnAbstract() {
        ExplorerService service = new ExportBuilder()
                .paper("summarized", "osdi", 2025)
                        .summary(new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode().put("status", "READY_FOR_REVIEW"))
                        .add()
                .paper("abstracted", "osdi", 2025).abstractText("Text.").add()
                .paper("plain", "osdi", 2025).add()
                .service();
        assertEquals(List.of("summarized"), ids(service.page(Filters.of("availability", "summary"))));
        assertEquals(List.of("abstracted"), ids(service.page(Filters.of("availability", "abstract"))));
        assertEquals("Has LLM summary", service.page(Filters.of("availability", "summary")).getActiveFilters().get(0).getValue());
    }

    @Test
    void statisticsCountCorpusWideCoverage() {
        ExplorerService service = new ExportBuilder()
                .paper("a", "osdi", 2025).abstractText("An abstract that is long enough to count here.").code("https://x/c").add()
                .paper("b", "osdi", 2024)
                        .summary(new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode().put("status", "READY_FOR_REVIEW"))
                        .review(new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode().put("status", "CHANGES_REQUESTED").put("decision", "CHANGES_REQUESTED"))
                        .add()
                .paper("c", "nsdi", 2025).relevance("EXCLUDED").artifact("https://x/a").add()
                .paper("d", "nsdi", 2025).add()
                .service();
        CorpusStatistics stats = service.getStatistics();
        assertEquals(4, stats.getPapers());
        assertEquals(1, stats.getExcluded());
        assertEquals(2, stats.getConferences());
        assertEquals(3, stats.getVenueYears());
        assertEquals(1, stats.getWithAbstract());
        assertEquals(25, stats.getAbstractPercent());
        assertEquals(1, stats.getWithSummary());
        assertEquals(1, stats.getWithReview());
        assertEquals(1, stats.getWithCode());
        assertEquals(1, stats.getWithArtifact());
    }
}
