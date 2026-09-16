package org.systemsphd.explorer.web;

import org.systemsphd.explorer.model.Paper;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Corpus-wide counts shown in the page footer. Computed once at startup. */
public final class CorpusStatistics {
    private final int papers;
    private final int excluded;
    private final int conferences;
    private final int venueYears;
    private final int withAbstract;
    private final int withSummary;
    private final int withReview;
    private final int withCode;
    private final int withArtifact;

    public CorpusStatistics(List<Paper> papers) {
        Set<String> conferenceSlugs = new HashSet<>();
        Set<String> venueYearKeys = new HashSet<>();
        int excluded = 0;
        int withAbstract = 0;
        int withSummary = 0;
        int withReview = 0;
        int withCode = 0;
        int withArtifact = 0;
        for (Paper paper : papers) {
            conferenceSlugs.add(paper.getVenue());
            venueYearKeys.add(paper.getVenue() + "/" + paper.getYear());
            if ("EXCLUDED".equals(paper.getRelevanceClass())) {
                excluded++;
            }
            if (paper.isHasAbstract()) {
                withAbstract++;
            }
            if (paper.isHasSummary()) {
                withSummary++;
            }
            if (paper.isHasReview()) {
                withReview++;
            }
            if (paper.isHasCode()) {
                withCode++;
            }
            if (paper.isHasArtifact()) {
                withArtifact++;
            }
        }
        this.papers = papers.size();
        this.excluded = excluded;
        this.conferences = conferenceSlugs.size();
        this.venueYears = venueYearKeys.size();
        this.withAbstract = withAbstract;
        this.withSummary = withSummary;
        this.withReview = withReview;
        this.withCode = withCode;
        this.withArtifact = withArtifact;
    }

    public int getPapers() {
        return papers;
    }

    public int getExcluded() {
        return excluded;
    }

    public int getConferences() {
        return conferences;
    }

    public int getVenueYears() {
        return venueYears;
    }

    public int getWithAbstract() {
        return withAbstract;
    }

    public int getWithSummary() {
        return withSummary;
    }

    public int getWithReview() {
        return withReview;
    }

    public int getWithCode() {
        return withCode;
    }

    public int getWithArtifact() {
        return withArtifact;
    }

    /** Whole-number percentage of papers that have the given count. */
    public int percent(int count) {
        return papers == 0 ? 0 : (int) Math.round(100.0 * count / papers);
    }

    public int getAbstractPercent() {
        return percent(withAbstract);
    }

    public int getSummaryPercent() {
        return percent(withSummary);
    }

    public int getReviewPercent() {
        return percent(withReview);
    }
}
