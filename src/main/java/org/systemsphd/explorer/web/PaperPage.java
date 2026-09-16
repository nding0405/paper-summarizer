package org.systemsphd.explorer.web;

import org.systemsphd.explorer.model.Paper;

import java.util.Map;

/** View model for one paper's page. */
public final class PaperPage {
    private final Paper paper;
    private final String venueName;
    private final String venueFullName;
    private final String proceedingsUrl;
    private final String searchUrl;
    private final Map<String, String> venueNames;

    public PaperPage(
            Paper paper,
            String venueName,
            String venueFullName,
            String proceedingsUrl,
            String searchUrl,
            Map<String, String> venueNames
    ) {
        this.paper = paper;
        this.venueName = venueName;
        this.venueFullName = venueFullName;
        this.proceedingsUrl = proceedingsUrl;
        this.searchUrl = searchUrl;
        this.venueNames = Map.copyOf(venueNames);
    }

    public Paper getPaper() {
        return paper;
    }

    public String getVenueName() {
        return venueName;
    }

    public String getVenueFullName() {
        return venueFullName;
    }

    /** The paper's conference-year in the proceedings view. */
    public String getProceedingsUrl() {
        return proceedingsUrl;
    }

    /** The search page scoped to the paper's conference-year. */
    public String getSearchUrl() {
        return searchUrl;
    }

    public Map<String, String> getVenueNames() {
        return venueNames;
    }
}
