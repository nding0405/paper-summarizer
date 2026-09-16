package org.systemsphd.explorer.model;

import java.util.List;

public final class RepositorySnapshot {
    private final List<Paper> papers;
    private final boolean fixture;
    private final String sourceLabel;

    public RepositorySnapshot(List<Paper> papers, boolean fixture, String sourceLabel) {
        this.papers = List.copyOf(papers);
        this.fixture = fixture;
        this.sourceLabel = sourceLabel;
    }

    public List<Paper> getPapers() {
        return papers;
    }

    public boolean isFixture() {
        return fixture;
    }

    public String getSourceLabel() {
        return sourceLabel;
    }
}
