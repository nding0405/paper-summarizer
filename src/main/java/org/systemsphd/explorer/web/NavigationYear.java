package org.systemsphd.explorer.web;

import org.systemsphd.explorer.catalog.VenueYearCoverage;

public final class NavigationYear {
    private final int year;
    private final long count;
    private final String href;
    private final boolean selected;
    private final VenueYearCoverage coverage;

    public NavigationYear(int year, long count, String href, boolean selected, VenueYearCoverage coverage) {
        this.year = year;
        this.count = count;
        this.href = href;
        this.selected = selected;
        this.coverage = coverage;
    }

    public int getYear() {
        return year;
    }

    public long getCount() {
        return count;
    }

    public String getHref() {
        return href;
    }

    public boolean isSelected() {
        return selected;
    }

    /** Coverage status from the manifest, or null when unknown. */
    public VenueYearCoverage getCoverage() {
        return coverage;
    }

    /** Short status mark shown beside incomplete venue-years; empty otherwise. */
    public String getMark() {
        return coverage == null ? "" : coverage.getMark();
    }

    /** A zero count is only meaningful when a program exists to search. */
    public boolean isShowCount() {
        return count > 0 || coverage == null || !coverage.isNoProgram();
    }
}
