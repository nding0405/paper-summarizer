package org.systemsphd.explorer.catalog;

/**
 * Collection status of one venue-year from the generated coverage manifest.
 * Coverage describes how much of a program has been collected; it says
 * nothing about paper quality or relevance.
 */
public final class VenueYearCoverage {
    private final String venue;
    private final int year;
    private final String status;
    private final String lastVerifiedAt;

    public VenueYearCoverage(String venue, int year, String status, String lastVerifiedAt) {
        this.venue = venue;
        this.year = year;
        this.status = status;
        this.lastVerifiedAt = lastVerifiedAt;
    }

    public String getVenue() {
        return venue;
    }

    public int getYear() {
        return year;
    }

    public String getStatus() {
        return status;
    }

    /** Verification timestamp reduced to its calendar date, or empty. */
    public String getLastVerifiedDate() {
        return lastVerifiedAt.length() >= 10 ? lastVerifiedAt.substring(0, 10) : lastVerifiedAt;
    }

    public boolean isComplete() {
        return "COMPLETE_PROCEEDINGS".equals(status);
    }

    /** True when no program exists to search yet, so a zero count would mislead. */
    public boolean isNoProgram() {
        return "UPCOMING".equals(status) || "UNAVAILABLE".equals(status);
    }

    public String getLabel() {
        return switch (status) {
            case "COMPLETE_PROCEEDINGS" -> "Complete proceedings";
            case "ACCEPTED_PAPERS_ONLY" -> "Accepted papers only";
            case "PARTIAL" -> "Partial coverage";
            case "UPCOMING" -> "Program not published yet";
            case "UNAVAILABLE" -> "Program unavailable";
            case "UNVERIFIED" -> "Coverage not verified";
            default -> Taxonomy.humanize(status);
        };
    }

    /** Short form for the sidebar, empty for complete coverage. */
    public String getMark() {
        return switch (status) {
            case "COMPLETE_PROCEEDINGS" -> "";
            case "ACCEPTED_PAPERS_ONLY" -> "accepted only";
            case "PARTIAL" -> "partial";
            case "UPCOMING" -> "upcoming";
            case "UNAVAILABLE" -> "unavailable";
            case "UNVERIFIED" -> "unverified";
            default -> Taxonomy.humanize(status).toLowerCase(java.util.Locale.ROOT);
        };
    }
}
