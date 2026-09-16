package org.systemsphd.explorer.web;

import org.systemsphd.explorer.catalog.ConferenceCategory;
import org.systemsphd.explorer.catalog.VenueYearCoverage;

import java.util.List;
import java.util.Map;

/**
 * View model for the proceedings page: either the overview of every
 * conference and year, or one conference-year grouped by session.
 */
public final class ProceedingsPage {
    private final String venue;
    private final String venueName;
    private final String venueFullName;
    private final Integer year;
    private final VenueYearCoverage coverage;
    private final List<ProceedingsSession> sessions;
    private final int totalPapers;
    private final List<NavigationCategory> overview;
    private final List<NavigationYear> venueYears;
    private final List<ConferenceCategory> categories;
    private final Map<String, String> venueNames;
    private final String searchUrl;

    public ProceedingsPage(
            String venue,
            String venueName,
            String venueFullName,
            Integer year,
            VenueYearCoverage coverage,
            List<ProceedingsSession> sessions,
            List<NavigationCategory> overview,
            List<NavigationYear> venueYears,
            List<ConferenceCategory> categories,
            Map<String, String> venueNames,
            String searchUrl
    ) {
        this.venue = venue;
        this.venueName = venueName;
        this.venueFullName = venueFullName;
        this.year = year;
        this.coverage = coverage;
        this.sessions = List.copyOf(sessions);
        this.totalPapers = sessions.stream().mapToInt(ProceedingsSession::getCount).sum();
        this.overview = List.copyOf(overview);
        this.venueYears = List.copyOf(venueYears);
        this.categories = List.copyOf(categories);
        this.venueNames = Map.copyOf(venueNames);
        this.searchUrl = searchUrl;
    }

    /** True when no conference-year is selected and the overview is shown. */
    public boolean isShowingOverview() {
        return venue.isBlank();
    }

    public String getVenue() {
        return venue;
    }

    public String getVenueName() {
        return venueName;
    }

    public String getVenueFullName() {
        return venueFullName;
    }

    public Integer getYear() {
        return year;
    }

    public String getHeading() {
        return isShowingOverview() ? "Proceedings" : venueName + " " + year;
    }

    public VenueYearCoverage getCoverage() {
        return coverage;
    }

    public boolean isNoProgram() {
        return coverage != null && coverage.isNoProgram() && sessions.isEmpty();
    }

    public List<ProceedingsSession> getSessions() {
        return sessions;
    }

    /** True when the program has named sessions rather than one unnamed group. */
    public boolean isHasSessions() {
        return sessions.size() > 1 || (sessions.size() == 1 && !sessions.get(0).getName().isBlank());
    }

    public int getTotalPapers() {
        return totalPapers;
    }

    /** Every catalogued conference with its years, for the overview and the switcher. */
    public List<NavigationCategory> getOverview() {
        return overview;
    }

    /** Years available for the selected conference, newest first. */
    public List<NavigationYear> getVenueYears() {
        return venueYears;
    }

    public List<ConferenceCategory> getCategories() {
        return categories;
    }

    public Map<String, String> getVenueNames() {
        return venueNames;
    }

    /** The search page filtered to the same conference-year. */
    public String getSearchUrl() {
        return searchUrl;
    }
}
