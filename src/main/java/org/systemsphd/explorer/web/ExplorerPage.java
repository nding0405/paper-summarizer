package org.systemsphd.explorer.web;

import org.systemsphd.explorer.catalog.ConferenceCategory;
import org.systemsphd.explorer.catalog.ConferenceDefinition;
import org.systemsphd.explorer.catalog.VenueYearCoverage;
import org.systemsphd.explorer.model.Paper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ExplorerPage {
    private final String heading;
    private final VenueYearCoverage coverage;
    private final List<Paper> papers;
    private final List<NavigationCategory> navigation;
    private final List<ActiveFilter> activeFilters;
    private final List<ConferenceCategory> categories;
    private final List<ConferenceDefinition> conferences;
    private final List<Integer> years;
    private final FilterState filters;
    private final int totalResults;
    private final int firstResult;
    private final int lastResult;
    private final int totalPages;
    private final int currentPage;
    private final String previousUrl;
    private final String nextUrl;
    private final List<PageJump> backwardJumps;
    private final List<PageJump> forwardJumps;
    private final boolean fixture;

    public ExplorerPage(
            String heading,
            VenueYearCoverage coverage,
            List<Paper> papers,
            List<NavigationCategory> navigation,
            List<ActiveFilter> activeFilters,
            List<ConferenceCategory> categories,
            List<ConferenceDefinition> conferences,
            List<Integer> years,
            FilterState filters,
            int totalResults,
            int firstResult,
            int lastResult,
            int totalPages,
            int currentPage,
            String previousUrl,
            String nextUrl,
            List<PageJump> backwardJumps,
            List<PageJump> forwardJumps,
            boolean fixture
    ) {
        this.heading = heading;
        this.coverage = coverage;
        this.papers = List.copyOf(papers);
        this.navigation = List.copyOf(navigation);
        this.activeFilters = List.copyOf(activeFilters);
        this.categories = List.copyOf(categories);
        this.conferences = List.copyOf(conferences);
        this.years = List.copyOf(years);
        this.filters = filters;
        this.totalResults = totalResults;
        this.firstResult = firstResult;
        this.lastResult = lastResult;
        this.totalPages = totalPages;
        this.currentPage = currentPage;
        this.previousUrl = previousUrl;
        this.nextUrl = nextUrl;
        this.backwardJumps = List.copyOf(backwardJumps);
        this.forwardJumps = List.copyOf(forwardJumps);
        this.fixture = fixture;
    }

    /** Short description of the selected conference scope, e.g. "OSDI 2025". */
    public String getHeading() {
        return heading;
    }

    /** Coverage of the selected venue-year, or null when no single venue-year is selected. */
    public VenueYearCoverage getCoverage() {
        return coverage;
    }

    /** True when the selected venue-year has no program yet, so "0 papers" would mislead. */
    public boolean isNoProgram() {
        return coverage != null && coverage.isNoProgram() && papers.isEmpty();
    }

    public List<Paper> getPapers() {
        return papers;
    }

    public List<ActiveFilter> getActiveFilters() {
        return activeFilters;
    }

    /** Active filters other than the text search, shown on the Filters button. */
    public long getFilterCount() {
        return activeFilters.stream().filter(filter -> !"Search".equals(filter.getName())).count();
    }

    public List<NavigationCategory> getNavigation() {
        return navigation;
    }

    public List<ConferenceCategory> getCategories() {
        return categories;
    }

    public List<ConferenceDefinition> getConferences() {
        return conferences;
    }

    /** Display names keyed by venue slug, for rendering each paper's venue line. */
    public Map<String, String> getVenueNames() {
        Map<String, String> names = new LinkedHashMap<>();
        for (ConferenceDefinition conference : conferences) {
            names.put(conference.getSlug(), conference.getName());
        }
        return names;
    }

    /** Default-visible papers across every conference in the sidebar. */
    public long getNavigationTotal() {
        return navigation.stream().mapToLong(NavigationCategory::getCount).sum();
    }

    public List<Integer> getYears() {
        return years;
    }

    public FilterState getFilters() {
        return filters;
    }

    public int getTotalResults() {
        return totalResults;
    }

    public int getFirstResult() {
        return firstResult;
    }

    public int getLastResult() {
        return lastResult;
    }

    public int getTotalPages() {
        return totalPages;
    }

    /** The page actually shown, after clamping the requested page into range. */
    public int getCurrentPage() {
        return currentPage;
    }

    /** Multi-page jumps before the current page, e.g. −10 then −5. */
    public List<PageJump> getBackwardJumps() {
        return backwardJumps;
    }

    /** Multi-page jumps after the current page, e.g. +5 then +10. */
    public List<PageJump> getForwardJumps() {
        return forwardJumps;
    }

    public String getPreviousUrl() {
        return previousUrl;
    }

    public String getNextUrl() {
        return nextUrl;
    }

    public boolean isHasPrevious() {
        return previousUrl != null;
    }

    public boolean isHasNext() {
        return nextUrl != null;
    }

    public boolean isFixture() {
        return fixture;
    }

    public boolean isEmpty() {
        return papers.isEmpty();
    }
}
